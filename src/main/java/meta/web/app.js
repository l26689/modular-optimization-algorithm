'use strict';

/**
 * MOA 元优化界面前端。
 *
 * 流程：加载代码模板 → 用户填代码与参数 → POST /api/search 启动 → 轮询 /api/status → 渲染排名。
 *
 * 之所以用轮询而不是 SSE：元搜索可能持续数分钟，轮询实现简单、
 * 无需处理断线重连，对本场景完全够用。
 */

const POLL_INTERVAL_MS = 400;
const MAX_TABLE_ROWS = 20;

const el = (id) => document.getElementById(id);
const form = el('form');
const templateSelect = el('template');
const codeArea = el('code');
const statusPanel = el('statusPanel');
const resultPanel = el('resultPanel');

let templates = [];
let pollTimer = null;

/**
 * 带诊断信息的 JSON 请求。
 *
 * 直接调用 res.json() 在服务端返回 HTML 时只会抛出
 * "Unexpected token '<'"，完全看不出问题出在哪。
 * 这里改为先取文本再解析，失败时把实际拿到的内容与地址一并报出来，
 * 并针对最常见的两种误用（file:// 直开、端口不对）给出明确提示。
 */
async function fetchJson(url, options) {
  if (location.protocol === 'file:') {
    throw new Error(
      '检测到你是用 file:// 直接打开了 HTML 文件。\n' +
      '本页面必须由服务端提供：先运行 ./build.sh web，' +
      '然后在浏览器打开 http://127.0.0.1:8080'
    );
  }

  const res = await fetch(url, options);
  const text = await res.text();

  let data;
  try {
    data = JSON.parse(text);
  } catch (err) {
    const head = text.trim().slice(0, 50).replace(/\s+/g, ' ');
    throw new Error(
      `${url} 返回的不是 JSON（HTTP ${res.status}）。\n` +
      `响应开头：${head}\n` +
      `当前页面地址：${location.href}\n\n` +
      '这个地址不是本服务的 Java 后端在提供页面。\n' +
      '本页面必须由 Java 服务端提供（它才有 /api 接口）：\n' +
      '  1. 在项目根目录运行  ./build.sh web\n' +
      '  2. 在浏览器打开  http://127.0.0.1:8080\n\n' +
      '注意：VS Code 的 Live Server、python -m http.server 等静态服务器\n' +
      '只能提供 HTML，无法提供本页面依赖的 /api 接口，请不要用它们打开。'
    );
  }
  return data;
}

/** 拉取代码模板列表并初始化表单。 */
async function loadTemplates() {
  templates = await fetchJson('/api/templates');
  templateSelect.innerHTML = '';
  for (const t of templates) {
    const opt = document.createElement('option');
    opt.value = t.id;
    opt.textContent = t.name;
    templateSelect.appendChild(opt);
  }
  applyTemplate();
}

/** 选中模板后，把它的代码、推荐维度与范围填进表单。 */
function applyTemplate() {
  const t = currentTemplate();
  if (!t) return;
  codeArea.value = t.code;
  el('dim').value = t.suggestedDimension;
  el('lower').value = t.defaultLower;
  el('upper').value = t.defaultUpper;
  el('templateNote').textContent = t.description;
  updateEstimate();
}

function currentTemplate() {
  return templates.find((t) => t.id === templateSelect.value);
}

/** 实时显示预估的内层实验量，让用户对耗时有个预期。 */
function updateEstimate() {
  const budget = Number(el('budget').value) || 0;
  const meta = Number(el('meta').value) || 0;
  const repeats = Number(el('repeats').value) || 0;
  if (!budget || !meta || !repeats) {
    el('hint').textContent = '';
    return;
  }
  const total = meta * repeats * budget;
  el('hint').textContent =
    `预估最多 ${total.toLocaleString('en-US')} 次目标函数调用` +
    `（${meta} 组合 × ${repeats} 次重复 × ${budget} 次迭代，实际因缓存而更少）`;
}

/** 表单提交：启动搜索并开始轮询。 */
async function onSubmit(event) {
  event.preventDefault();
  stopPolling();

  if (!codeArea.value.trim()) {
    fail('请先填写满意度函数代码（可从上方模板选择，或自己编写）。');
    return;
  }

  // 用户代码含换行与特殊字符，必须放在请求体里——塞进 URL 会超长且转义易错。
  // 用 URLSearchParams 编码成 form-encoded，服务端复用同一套查询解析。
  const params = new URLSearchParams({
    code: codeArea.value,
    dim: el('dim').value,
    lower: el('lower').value,
    upper: el('upper').value,
    budget: el('budget').value,
    meta: el('meta').value,
    repeats: el('repeats').value,
    seed: el('seed').value,
  });

  setBusy(true);
  statusPanel.hidden = false;
  resultPanel.hidden = true;
  el('errorText').hidden = true;
  setStatus('正在编译并启动……', 0);

  let body;
  try {
    body = await fetchJson('/api/search', { method: 'POST', body: params });
  } catch (err) {
    fail(err.message);
    return;
  }
  if (body.error) {
    fail(body.error);
    return;
  }
  pollTimer = setInterval(pollStatus, POLL_INTERVAL_MS);
  pollStatus();
}

/** 轮询一次状态。 */
async function pollStatus() {
  let data;
  try {
    data = await fetchJson('/api/status');
  } catch (err) {
    return; // 单次失败不终止轮询，网络抖动属正常
  }

  if (data.error) {
    fail(data.error);
    return;
  }

  if (data.running) {
    const done = data.evaluated;
    // 进度条按"已评估配置数 / 元搜索预算"估算；预算不是硬上限，故最多显示到 99%
    const planned = Number(el('meta').value) || 1;
    setStatus(
      `正在搜索……已评估 ${done} 个组合，真实运行 ${data.totalInnerRuns} 次内层实验，` +
      `耗时 ${(data.elapsedMillis / 1000).toFixed(1)} 秒`,
      Math.min(99, (done / planned) * 100)
    );
    return;
  }

  stopPolling();
  setBusy(false);
  setStatus(`搜索完成：共评估 ${data.evaluated} 个组合，耗时 ${(data.elapsedMillis / 1000).toFixed(1)} 秒`, 100);
  renderResults(data);
}

/** 渲染排名表与摘要。 */
function renderResults(data) {
  const rows = data.results || [];
  if (rows.length === 0) {
    resultPanel.hidden = false;
    el('summary').textContent = '没有结果。';
    el('rows').innerHTML = '';
    el('solutionBox').hidden = true;
    return;
  }

  el('summary').textContent = (data.summary || '') + `　｜　共评估 ${rows.length} 个不同组合`;

  const tbody = el('rows');
  tbody.innerHTML = '';
  for (const r of rows.slice(0, MAX_TABLE_ROWS)) {
    const tr = document.createElement('tr');
    tr.appendChild(cell(r.rank, 'num'));
    tr.appendChild(cell(fmt(r.meanCost), 'num'));
    tr.appendChild(cell(fmt(r.stdDev), 'num'));
    tr.appendChild(cell(fmt(r.bestCost), 'num'));
    tr.appendChild(cell(r.meanMillis.toFixed(2) + ' ms', 'num'));
    tr.appendChild(cell(r.describe, ''));
    tbody.appendChild(tr);
  }
  if (rows.length > MAX_TABLE_ROWS) {
    const tr = document.createElement('tr');
    const td = cell(`…… 其余 ${rows.length - MAX_TABLE_ROWS} 项未显示`, '');
    td.colSpan = 6;
    td.style.color = '#6b7280';
    tr.appendChild(td);
    tbody.appendChild(tr);
  }

  // 只展示第一名找到的解——那才是用户要的最终答案。
  // 怎么把解翻译成人话由满意度函数决定（ObjectiveFunction.describe），前端原样显示。
  const best = rows[0];
  if (best && best.solution) {
    el('solutionText').textContent = best.solution;
    el('solutionBox').hidden = false;
  } else {
    el('solutionBox').hidden = true;
  }

  resultPanel.hidden = false;
}

function cell(text, cls) {
  const td = document.createElement('td');
  td.textContent = text;
  if (cls) td.className = cls;
  return td;
}

/**
 * 数字格式化：中等量级用常规表示，极小/极大值用科学计数法。
 * 目标值跨度可能从 1e-6 到 1e3，单一格式无法兼顾可读性。
 */
function fmt(v) {
  if (v === null || v === undefined || !Number.isFinite(v)) return '—';
  if (v === 0) return '0';
  const a = Math.abs(v);
  if (a >= 1e-2 && a < 1e6) return String(Number(v.toPrecision(6)));
  return v.toExponential(4);
}

function setStatus(text, percent) {
  el('statusText').textContent = text;
  el('barFill').style.width = Math.max(0, Math.min(100, percent)) + '%';
}

function setBusy(busy) {
  el('submit').disabled = busy;
  el('submit').textContent = busy ? '搜索中……' : '开始搜索';
}

function fail(message) {
  stopPolling();
  setBusy(false);
  statusPanel.hidden = false;
  el('statusText').textContent = '';
  el('errorText').hidden = false;
  el('errorText').textContent = '出错了：' + message;
}

function stopPolling() {
  if (pollTimer !== null) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
}

templateSelect.addEventListener('change', applyTemplate);
el('budget').addEventListener('input', updateEstimate);
el('meta').addEventListener('input', updateEstimate);
el('repeats').addEventListener('input', updateEstimate);
form.addEventListener('submit', onSubmit);

loadTemplates().catch((err) => fail('无法加载代码模板：' + err.message));
