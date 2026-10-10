package meta.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import meta.EvaluationResult;
import meta.MetaProblem;
import meta.MetaSearchRunner;
import meta.ObjectiveFunction;
import meta.RuntimeProblem;
import meta.code.CodeTemplates;
import meta.code.UserCodeCompiler;

/**
 * 元优化的人机交互界面服务端。
 *
 * <h3>零依赖</h3>
 * 使用 JDK 自带的 {@code com.sun.net.httpserver}，不引入任何第三方库，
 * 与项目"无构建工具、javac 直接编译"的现状一致。
 *
 * <h3>只监听本机回环地址</h3>
 * 服务绑定在 {@code 127.0.0.1} 而非 {@code 0.0.0.0}。
 * 本服务可以让任意访问者触发大量 CPU 计算，绝不应暴露到局域网或公网。
 *
 * <h3>单任务模型</h3>
 * 同一时刻只允许一个搜索任务。元搜索是 CPU 密集的长时间任务，
 * 并发多个只会互相拖慢；单任务模型也让进度查询接口无需任务 ID。
 *
 * <h3>接口</h3>
 * <table border="1">
 *   <caption>HTTP 接口</caption>
 *   <tr><th>方法</th><th>路径</th><th>说明</th></tr>
 *   <tr><td>GET</td><td>/</td><td>交互页面</td></tr>
 *   <tr><td>GET</td><td>/style.css</td><td>样式表</td></tr>
 *   <tr><td>GET</td><td>/app.js</td><td>前端脚本</td></tr>
 *   <tr><td>GET</td><td>/api/templates</td><td>满意度函数代码模板列表</td></tr>
 *   <tr><td>POST</td><td>/api/search</td>
 *       <td>启动搜索，立即返回，不阻塞。参数在请求体里（含用户代码，太长塞不进 URL）</td></tr>
 *   <tr><td>GET</td><td>/api/status</td><td>查询进度与结果，供前端轮询</td></tr>
 * </table>
 *
 * <h3>为什么用轮询而不是 SSE</h3>
 * 元搜索可能持续数分钟，必须有进度反馈，否则用户会以为卡死。
 * 轮询（每 400ms）实现简单、无需处理断线重连，对本场景足够。
 *
 * <h3>启动方式</h3>
 * <pre>{@code
 * ./build.sh                       # 会把 web 静态文件复制到 bin/meta/web/
 * java -cp bin meta.web.WebServer  # 默认 8080 端口
 * java -cp bin meta.web.WebServer 9000
 * }</pre>
 */
public final class WebServer {

    private static final int DEFAULT_PORT = 8080;

    // 输入上限：界面是开放的，必须有硬性保护，否则一次误操作就能把机器跑死
    private static final int MAX_DIMENSION = 1000;
    private static final int MAX_BUDGET = 1_000_000;
    private static final int MAX_META_BUDGET = 5_000;
    private static final int MAX_REPEATS = 50;

    private final HttpServer server;

    /** 当前任务状态。所有字段跨线程可见性由 volatile 保证。 */
    private final JobState job = new JobState();

    private WebServer(int port) throws IOException {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("端口非法：" + port);
        }
        InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
        this.server = HttpServer.create(address, 0);
        this.server.setExecutor(Executors.newFixedThreadPool(4));

        this.server.createContext("/", this::handleIndex);
        this.server.createContext("/app.js",
                ex -> serveResource(ex, "/meta/web/app.js",
                        "application/javascript; charset=utf-8"));
        this.server.createContext("/style.css",
                ex -> serveResource(ex, "/meta/web/style.css",
                        "text/css; charset=utf-8"));
        this.server.createContext("/api/templates", this::handleTemplates);
        this.server.createContext("/api/search", this::handleSearch);
        this.server.createContext("/api/status", this::handleStatus);
    }

    /**
     * 程序入口。
     *
     * @param args 可选：第一个参数为端口号，默认 {@value #DEFAULT_PORT}
     * @throws IOException 端口被占用等
     */
    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("端口参数不是整数：" + args[0]);
                System.exit(2);
            }
        }

        WebServer web;
        try {
            web = new WebServer(port);
        } catch (java.net.BindException e) {
            // 端口占用是启动失败最常见的原因，给出可操作的中文提示而不是一堆堆栈
            System.err.println("启动失败：端口 " + port + " 已被占用。");
            System.err.println("可能是上一次的服务没关干净，或别的程序正在使用该端口。");
            System.err.println("换一个端口再试，例如：./build.sh web 8081");
            System.exit(1);
            return;
        }
        web.start();
    }

    private void start() {
        server.start();
        System.out.println("元优化界面已启动：http://127.0.0.1:" + server.getAddress().getPort());
        System.out.println("（仅监听本机回环地址，按 Ctrl+C 停止）");
    }

    // ==================================================================
    // 静态资源
    // ==================================================================

    private void handleIndex(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (!"/".equals(path) && !"/index.html".equals(path)) {
            sendText(ex, 404, "未找到：" + path);
            return;
        }
        serveResource(ex, "/meta/web/index.html", "text/html; charset=utf-8");
    }

    /**
     * 从 classpath 读取静态资源并返回。
     * <p>
     * 资源文件由 {@code build.sh} 从 {@code src/meta/web/} 复制到
     * {@code bin/meta/web/}，因此运行时通过 classpath 定位。
     */
    private void serveResource(HttpExchange ex, String resourcePath, String contentType)
            throws IOException {
        try (InputStream in = WebServer.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                sendText(ex, 500, "静态资源缺失：" + resourcePath
                        + "\n请先执行 ./build.sh（它会复制 web 资源到 bin/）");
                return;
            }
            byte[] body = in.readAllBytes();
            ex.getResponseHeaders().set("Content-Type", contentType);
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream out = ex.getResponseBody()) {
                out.write(body);
            }
        }
    }

    // ==================================================================
    // API
    // ==================================================================

    /** GET /api/templates —— 返回全部代码模板（含源码），供前端快捷填充。 */
    private void handleTemplates(HttpExchange ex) throws IOException {
        StringBuilder sb = new StringBuilder("[");
        List<CodeTemplates.Template> all = CodeTemplates.all();
        for (int i = 0; i < all.size(); i++) {
            CodeTemplates.Template t = all.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append('{')
                    .append("\"id\":").append(jsonString(t.id())).append(',')
                    .append("\"name\":").append(jsonString(t.name())).append(',')
                    .append("\"description\":").append(jsonString(t.description())).append(',')
                    .append("\"suggestedDimension\":").append(t.suggestedDimension()).append(',')
                    .append("\"defaultLower\":").append(num(t.defaultLower())).append(',')
                    .append("\"defaultUpper\":").append(num(t.defaultUpper())).append(',')
                    .append("\"code\":").append(jsonString(t.code()))
                    .append('}');
        }
        sb.append(']');
        sendJson(ex, 200, sb.toString());
    }

    /** POST /api/search —— 校验参数、编译用户代码后在后台线程启动搜索，立即返回。 */
    private void handleSearch(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            sendText(ex, 405, "请使用 POST");
            return;
        }
        if (job.running) {
            sendJson(ex, 409, "{\"error\":" + jsonString("已有搜索正在进行，请等待完成") + "}");
            return;
        }

        try {
            // 用户代码含换行与特殊字符，必须走请求体；前端以 form-encoded 提交，
            // 因此这里可以直接复用 parseQuery（它内部会做 URL 解码）。
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> q = parseQuery(body);

            String code = required(q, "code");
            int dimension = (int) parseLong(q.getOrDefault("dim", "2"), "dim", 1, MAX_DIMENSION);
            double lower = parseDouble(q.getOrDefault("lower", "-100"), "lower");
            double upper = parseDouble(q.getOrDefault("upper", "100"), "upper");
            int budget = (int) parseLong(q.getOrDefault("budget", "5000"), "budget", 1, MAX_BUDGET);
            int metaBudget = (int) parseLong(q.getOrDefault("meta", "200"), "meta", 1, MAX_META_BUDGET);
            int repeats = (int) parseLong(q.getOrDefault("repeats", "3"), "repeats", 1, MAX_REPEATS);
            long seed = parseLong(q.getOrDefault("seed", "42"), "seed", Long.MIN_VALUE, Long.MAX_VALUE);

            if (lower >= upper) {
                throw new IllegalArgumentException("下界必须小于上界");
            }

            ObjectiveFunction objective = compileAndCheck(code, dimension, lower, upper);
            startJob(objective, dimension, lower, upper, budget, metaBudget, repeats, seed);
            sendJson(ex, 200, "{\"started\":true}");

        } catch (IllegalArgumentException e) {
            sendJson(ex, 400, "{\"error\":" + jsonString(e.getMessage()) + "}");
        } catch (RuntimeException e) {
            sendJson(ex, 500, "{\"error\":" + jsonString("启动失败：" + e) + "}");
        }
    }

    /** GET /api/status —— 返回进度与（完成后的）排名结果。 */
    private void handleStatus(HttpExchange ex) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append('{')
                .append("\"running\":").append(job.running).append(',')
                .append("\"evaluated\":").append(job.evaluated).append(',')
                .append("\"totalInnerRuns\":").append(job.totalInnerRuns).append(',')
                .append("\"elapsedMillis\":").append(num(job.elapsedMillis)).append(',')
                .append("\"summary\":").append(job.summary == null ? "null" : jsonString(job.summary))
                .append(',')
                .append("\"error\":").append(job.error == null ? "null" : jsonString(job.error))
                .append(',')
                .append("\"results\":[");

        List<EvaluationResult> ranking = job.ranking;
        if (ranking != null) {
            for (int i = 0; i < ranking.size(); i++) {
                EvaluationResult r = ranking.get(i);
                if (i > 0) {
                    sb.append(',');
                }
                sb.append('{')
                        .append("\"rank\":").append(i + 1).append(',')
                        .append("\"meanCost\":").append(num(-r.meanSatisfaction())).append(',')
                        .append("\"stdDev\":").append(num(r.stdDevSatisfaction())).append(',')
                        .append("\"bestCost\":").append(num(-r.bestSatisfaction())).append(',')
                        .append("\"meanMillis\":").append(num(r.meanMillis())).append(',')
                        .append("\"describe\":").append(jsonString(r.config().describe()));
                if (i == 0) {
                    // 只有第一名才展示它找到的解：那才是用户要的最终答案；
                    // 全量输出会让响应体积成倍增长，而其余名次的解意义不大。
                    sb.append(",\"solution\":").append(jsonString(describeSolution(r)));
                }
                sb.append('}');
            }
        }
        sb.append("]}");
        sendJson(ex, 200, sb.toString());
    }

    /**
     * 把第一名的配置找到的解翻译成人类可读的文字。
     * <p>
     * 解码规则由用户的满意度函数提供（{@link ObjectiveFunction#describe}）——
     * 框架手里只有一串连续分量，无从得知它代表什么（排列？坐标？）。
     */
    private String describeSolution(EvaluationResult result) {
        double[] solution = result.bestX();
        if (solution == null || job.objective == null) {
            return "";
        }
        try {
            return job.objective.describe(solution);
        } catch (RuntimeException e) {
            // 用户重写的 describe 有 bug 时，不能让它拖垮整个状态接口
            return "（无法描述该解：" + e + "）";
        }
    }

    /**
     * 在后台线程执行元搜索。
     * <p>
     * 每个配置都重新构造全部组件——{@code SABasicCoolingSchedule} 等组件
     * 内部持有可变状态，跨运行复用会导致计数残留。这一点由
     * {@link MetaProblem} 保证，此处只需正常调用。
     */
    private void startJob(ObjectiveFunction objective, int dimension, double lower, double upper,
            int budget, int metaBudget, int repeats, long seed) {

        job.reset();
        job.running = true;
        job.objective = objective;
        job.summary = String.format(Locale.ROOT,
                "%d 维 | [%s, %s] | 预算 %,d | 重复 %d 次 | 元搜索 %d",
                dimension, trim(lower), trim(upper),
                budget, repeats, metaBudget);

        Thread worker = new Thread(() -> {
            try {
                RuntimeProblem problem = new RuntimeProblem(dimension, lower, upper, objective);
                MetaProblem metaProblem = new MetaProblem(problem, budget, repeats);
                metaProblem.setOnEvaluated(result -> {
                    job.evaluated = metaProblem.evaluatedConfigCount();
                    job.totalInnerRuns = metaProblem.totalInnerRuns();
                    job.elapsedMillis = metaProblem.totalMillis();
                });

                List<EvaluationResult> ranking =
                        new MetaSearchRunner(metaProblem, metaBudget, seed).run();

                job.ranking = ranking;
                job.evaluated = metaProblem.evaluatedConfigCount();
                job.totalInnerRuns = metaProblem.totalInnerRuns();
                job.elapsedMillis = metaProblem.totalMillis();
            } catch (Throwable t) {
                job.error = String.valueOf(t.getMessage() != null ? t.getMessage() : t);
            } finally {
                job.running = false;
            }
        }, "meta-search");

        // 守护线程：主程序退出时不会因为搜索未完成而挂住
        worker.setDaemon(true);
        worker.start();
    }

    // ==================================================================
    // 工具方法
    // ==================================================================

    /**
     * 编译用户提交的满意度代码，并在正式搜索前做一次轻量体检。
     * <p>
     * 体检用几个代表性解试调用，把三类会让搜索失败或静默跑偏的问题挡在搜索之前：
     * <ul>
     *   <li>返回值非有限值（除零、log 非正数、sqrt 负数等）</li>
     *   <li>调用时抛异常</li>
     *   <li><b>非纯函数</b>——同一输入两次调用结果不同。这一类最难发现，
     *       因为它不报错，只是让优化算法静默失去意义</li>
     * </ul>
     *
     * @throws IllegalArgumentException 编译失败或体检不通过，消息面向用户
     */
    private static ObjectiveFunction compileAndCheck(String code, int dimension,
            double lower, double upper) {
        ObjectiveFunction objective;
        try {
            objective = UserCodeCompiler.compile(code);
        } catch (UserCodeCompiler.CompilationException e) {
            throw new IllegalArgumentException("代码编译失败：\n" + e.getMessage());
        }

        // 用边界与中点三种解探测，覆盖最容易出问题（除零、越界）的区域
        double[][] probes = {
            filled(dimension, lower),
            filled(dimension, upper),
            filled(dimension, (lower + upper) / 2.0),
        };

        for (int i = 0; i < probes.length; i++) {
            double first;
            try {
                first = objective.evaluate(probes[i].clone());
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(
                        "满意度函数在第 " + (i + 1) + " 个测试解上抛出了异常：" + e
                        + "\n请确保它对任何取值都能返回一个有限结果"
                        + "（退化情形应在函数内部返回一个有限的大惩罚值）。");
            }
            if (!Double.isFinite(first)) {
                throw new IllegalArgumentException(
                        "满意度函数返回了非有限值（" + first + "）。"
                        + "\n请检查除零、log 非正数、sqrt 负数等情形，改返回一个有限的大惩罚值。");
            }

            double second = objective.evaluate(probes[i].clone());
            if (Double.compare(first, second) != 0) {
                throw new IllegalArgumentException(
                        "满意度函数不是纯函数：同一个输入两次调用返回了不同结果（"
                        + first + " 与 " + second + "）。"
                        + "\n请不要在 evaluate 里使用 Math.random()、System.currentTimeMillis()"
                        + " 或可变的静态状态——优化算法依赖「相同输入必得相同输出」。");
            }
        }
        return objective;
    }

    /** 构造一个各分量都等于 {@code value} 的解。 */
    private static double[] filled(int dimension, double value) {
        double[] values = new double[dimension];
        java.util.Arrays.fill(values, value);
        return values;
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return map;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                map.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));
            }
        }
        return map;
    }

    private static String decode(String s) {
        return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static String required(Map<String, String> q, String key) {
        String v = q.get(key);
        if (v == null || v.isEmpty()) {
            throw new IllegalArgumentException("缺少参数：" + key);
        }
        return v;
    }

    private static long parseLong(String value, String name, long min, long max) {
        long v;
        try {
            v = Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " 需要整数，实际为：" + value);
        }
        if (v < min || v > max) {
            throw new IllegalArgumentException(name + " 超出允许范围 [" + min + ", " + max + "]，实际为：" + v);
        }
        return v;
    }

    private static double parseDouble(String value, String name) {
        double v;
        try {
            v = Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " 需要数字，实际为：" + value);
        }
        if (!Double.isFinite(v)) {
            throw new IllegalArgumentException(name + " 必须为有限值，实际为：" + value);
        }
        return v;
    }

    /** 去掉整数的多余小数位，仅用于展示。 */
    private static String trim(double v) {
        if (v == Math.rint(v)) {
            return String.format(Locale.ROOT, "%.0f", v);
        }
        return String.valueOf(v);
    }

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        send(ex, status, json, "application/json; charset=utf-8");
    }

    private static void sendText(HttpExchange ex, int status, String text) throws IOException {
        send(ex, status, text, "text/plain; charset=utf-8");
    }

    private static void send(HttpExchange ex, int status, String body, String contentType)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", contentType);
        // 禁止缓存，否则轮询状态接口会拿到过期数据
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    /** 数字直接输出；非有限值降级为 null，避免产出非法 JSON（NaN/Infinity 不是合法 JSON）。 */
    private static String num(double v) {
        if (!Double.isFinite(v)) {
            return "null";
        }
        return String.valueOf(v);
    }

    /** 生成一个合法的 JSON 字符串字面量（含转义）。 */
    private static String jsonString(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /** 搜索任务的可变状态，供工作线程写入、HTTP 线程读取。 */
    private static final class JobState {
        volatile boolean running;
        volatile int evaluated;
        volatile int totalInnerRuns;
        volatile double elapsedMillis;
        volatile String summary;
        volatile String error;
        volatile List<EvaluationResult> ranking;
        /** 当前任务的满意度函数，用于把最优解翻译成人类可读文字 */
        volatile ObjectiveFunction objective;

        void reset() {
            evaluated = 0;
            totalInnerRuns = 0;
            elapsedMillis = 0;
            error = null;
            ranking = null;
            summary = null;
            objective = null;
        }
    }
}
