// MOA 元优化界面 —— 前端逻辑（无框架，原生 fetch）

const $ = (id) => document.getElementById(id);

// 状态：预设/模板数据缓存
let presets = [];
let templates = [];

// =====================================================================
// 初始化
// =====================================================================

async function init() {
  bindEvents();
  try {
    [presets, templates] = await Promise.all([
      fetchJson('/api/presets'),
      fetchJson('/api/templates'),
    ]);
    renderPresets();
    renderTemplates();
  } catch (e) {
    showError('加载预设/模板失败：' + e.message);
  }
}

function bindEvents() {
  // 类型切换
  document.querySelectorAll('input[name="type"]').forEach((radio) => {
    radio.addEventListener('change', onTypeChange);
  });
  // 模板填充
  $('template-select').addEventListener('change', onTemplateChange);
  // 预设切换时刷新边界提示
  $('preset-select').addEventListener('change', renderPresetBounds);
  // 搜索
  $('search-btn').addEventListener('click', onSearch);
}

// =====================================================================
// 渲染下拉
// =====================================================================

function renderPresets() {
  const sel = $('preset-select');
  sel.innerHTML = '';
  for (const p of presets) {
    const opt = document.createElement('option');
    opt.value = p.id;
    opt.textContent = p.name;
    sel.appendChild(opt);
  }
  renderPresetBounds();
}

function renderPresetBounds() {
  const p = presets.find((x) => x.id === $('preset-select').value);
  if (p) {
    $('preset-bounds').textContent =
      `该预设的变量范围：${p.lower} ~ ${p.upper}（由预设自带，无需填）`;
  }
}

function renderTemplates() {
  const sel = $('template-select');
  for (const t of templates) {
    const opt = document.createElement('option');
    opt.value = t.id;
    opt.textContent = t.name + ' —— ' + t.description;
    sel.appendChild(opt);
  }
}

// =====================================================================
// 交互
// =====================================================================

function onTypeChange() {
  const type = document.querySelector('input[name="type"]:checked').value;
  const isPreset = type === 'preset';
  $('opt-preset').classList.toggle('active', isPreset);
  $('opt-code').classList.toggle('active', !isPreset);
  $('preset-panel').classList.toggle('hidden', !isPreset);
  $('code-panel').classList.toggle('hidden', isPreset);
}

function onTemplateChange() {
  const id = $('template-select').value;
  if (!id) return;
  const t = templates.find((x) => x.id === id);
  if (t) $('code').value = t.code;
}

// =====================================================================
// 搜索
// =====================================================================

async function onSearch() {
  const type = document.querySelector('input[name="type"]:checked').value;
  const params = new URLSearchParams();
  params.append('type', type);

  if (type === 'preset') {
    params.append('preset', $('preset-select').value);
    params.append('dimension', $('dimension-preset').value);
  } else {
    params.append('dimension', $('dimension-code').value);
    params.append('lower', $('lower').value);
    params.append('upper', $('upper').value);
    params.append('code', $('code').value);
  }

  params.append('metaIterations', $('metaIterations').value);
  params.append('swarmSize', $('swarmSize').value);
  params.append('innerBudget', $('innerBudget').value);

  // 进入搜索状态
  const btn = $('search-btn');
  btn.disabled = true;
  btn.textContent = '搜索中…';
  $('status').textContent = '正在进行元搜索，可能需要几秒…';
  hideResult();

  try {
    const resp = await fetch('/api/search', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
      body: params.toString(),
    });
    const data = await resp.json();
    if (!resp.ok) {
      showError(data.error || ('请求失败，状态码 ' + resp.status));
    } else {
      renderResult(data);
    }
  } catch (e) {
    showError('请求出错：' + e.message);
  } finally {
    btn.disabled = false;
    btn.textContent = '开始搜索';
    $('status').textContent = '';
  }
}

// =====================================================================
// 结果渲染
// =====================================================================

function hideResult() {
  $('result').classList.add('hidden');
  $('error-box').classList.add('hidden');
}

function showError(message) {
  $('result').classList.remove('hidden');
  $('error-box').classList.remove('hidden');
  $('error-box').textContent = message;
}

function renderResult(r) {
  $('error-box').classList.add('hidden');
  $('result').classList.remove('hidden');

  $('branch').textContent = r.branch;

  // 坐标表：按最优算法种类，只显示该算法实际用到的参数（参数名后括号内是中文名）
  const tbody = $('coord-table').querySelector('tbody');
  tbody.innerHTML = '';

  let idxs, names;
  switch (r.branch) {
    case 'SA':
      idxs = [1, 2];
      names = ['初始温度', '降温率'];
      break;
    case 'PSO':
      idxs = [3, 4, 5];
      names = ['惯性权重 ω', '认知系数 c1', '社会系数 c2'];
      break;
    case 'DE':
      idxs = [6, 7];
      names = ['缩放因子 F', '交叉概率 CR'];
      break;
    default: // GA 等未实现：兜底显示全部（跳过算法种类）
      idxs = r.coord.slice(1).map((_, i) => i + 1);
      names = idxs.map(() => '');
  }

  for (let k = 0; k < idxs.length; k++) {
    const i = idxs[k];
    const tr = document.createElement('tr');
    const tdName = document.createElement('td');
    const tdVal = document.createElement('td');
    tdName.textContent = r.dimNames[i] + '（' + names[k] + '）';
    tdVal.textContent = formatNum(r.coord[i]);
    tr.appendChild(tdName);
    tr.appendChild(tdVal);
    tbody.appendChild(tr);
  }

  $('best-x').textContent = r.bestX.map(formatNum).join(', ');
  $('avg-cost').textContent = formatNum(r.avgCost);
  $('best-x-desc').textContent = '';

  $('evals').textContent = r.evalCount;
  $('elapsed').textContent = r.elapsedMs + ' ms';
}

function formatNum(v) {
  if (Number.isInteger(v)) return String(v);
  return v.toFixed(6);
}

// =====================================================================
// 工具
// =====================================================================

async function fetchJson(url) {
  const resp = await fetch(url);
  if (!resp.ok) throw new Error('HTTP ' + resp.status);
  return resp.json();
}

init();
