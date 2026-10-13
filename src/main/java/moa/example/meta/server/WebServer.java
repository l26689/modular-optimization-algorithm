package moa.example.meta.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import moa.problem.continuous.AckleyProblem;
import moa.problem.continuous.ContinuousProblem;
import moa.problem.continuous.GriewankProblem;
import moa.problem.continuous.RastriginProblem;
import moa.problem.continuous.RosenbrockProblem;
import moa.problem.continuous.SphereProblem;

/**
 * 元优化界面后端，用 JDK 自带的 {@link HttpServer} 实现（零额外依赖）。
 * <p>
 * 职责：
 * <ul>
 *   <li>把项目根 {@code web/} 下的静态文件（html/css/js）发给浏览器；</li>
 *   <li>提供三个 API：预设列表、代码模板、跑一次元搜索；</li>
 *   <li>对输入做硬性上限校验（界面是开放的，一次误输入就能把机器跑死）；</li>
 *   <li><b>只绑定 127.0.0.1</b>——服务能让任意访问者触发大量 CPU 计算，绝不能暴露到局域网。</li>
 * </ul>
 * <p>
 * 运行：先 {@code mvn compile}，再 {@code java -cp target/classes moa.example.meta.server [端口]}，
 * 默认 8080。浏览器打开 {@code http://127.0.0.1:8080}。
 */
public final class WebServer {

    // 输入上限（防呆，见类注释）
    private static final int MAX_DIMENSION = 1000;
    private static final int MAX_META_ITERATIONS = 5000;
    private static final int MAX_SWARM_SIZE = 100;
    private static final int MAX_INNER_BUDGET = 1_000_000;

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        HttpServer server = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/", WebServer::handle);
        server.start();

        System.out.println("元优化界面已启动：http://127.0.0.1:" + port);
        System.out.println("（仅监听本机回环地址，按 Ctrl+C 停止）");
    }

    private static void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();

            if ("GET".equals(method)) {
                handleGet(ex, path);
            } else if ("POST".equals(method) && "/api/search".equals(path)) {
                handleSearch(ex);
            } else {
                sendText(ex, 404, "Not Found");
            }
        } finally {
            ex.close();
        }
    }

    // =====================================================================
    // GET：静态资源 + 只读 API
    // =====================================================================

    private static void handleGet(HttpExchange ex, String path) throws IOException {
        switch (path) {
            case "/" -> serveFile(ex, "/web/index.html", "text/html");
            case "/style.css" -> serveFile(ex, "/web/style.css", "text/css");
            case "/app.js" -> serveFile(ex, "/web/app.js", "application/javascript");
            case "/api/presets" -> sendJson(ex, 200, presetsJson());
            case "/api/templates" -> sendJson(ex, 200, templatesJson());
            default -> serveFile(ex, "/web" + path, contentType(path));
        }
    }

    // =====================================================================
    // POST /api/search：解析表单，跑元搜索，返回结果
    // =====================================================================

    private static void handleSearch(HttpExchange ex) throws IOException {
        try {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> p = parseForm(body);

            int dimension = parseInt(p.get("dimension"), "维度");
            int metaIterations = parseInt(p.getOrDefault("metaIterations", "10"), "元搜索轮数");
            int swarmSize = parseInt(p.getOrDefault("swarmSize", "6"), "粒子数");
            int innerBudget = parseInt(p.getOrDefault("innerBudget", "2000"), "内层评估次数");

            if (dimension < 1 || dimension > MAX_DIMENSION) {
                throw new IllegalArgumentException("维度必须在 1.." + MAX_DIMENSION);
            }
            if (metaIterations < 1 || metaIterations > MAX_META_ITERATIONS) {
                throw new IllegalArgumentException("元搜索轮数必须在 1.." + MAX_META_ITERATIONS);
            }
            if (swarmSize < 1 || swarmSize > MAX_SWARM_SIZE) {
                throw new IllegalArgumentException("粒子数必须在 1.." + MAX_SWARM_SIZE);
            }
            if (innerBudget < 1 || innerBudget > MAX_INNER_BUDGET) {
                throw new IllegalArgumentException("内层评估次数必须在 1.." + MAX_INNER_BUDGET);
            }

            ContinuousProblem target = buildTarget(p, dimension);

            MetaSearchRunner.SearchResult r = MetaSearchRunner.run(target, metaIterations, swarmSize, innerBudget);
            sendJson(ex, 200, resultJson(r));

        } catch (IllegalArgumentException e) {
            sendJson(ex, 400, errorJson(e.getMessage()));
        } catch (Exception e) {
            sendJson(ex, 500, errorJson("服务器内部错误: " + e));
        }
    }

    /** 按表单里的 type 分支，构造目标问题（预设或用户代码）。 */
    private static ContinuousProblem buildTarget(Map<String, String> p, int dimension) {
        String type = p.getOrDefault("type", "");
        if ("preset".equals(type)) {
            Preset preset = Preset.byId(p.get("preset"));
            return preset.create(dimension);
        }
        if ("code".equals(type)) {
            double lower = parseDouble(p.get("lower"), "下界");
            double upper = parseDouble(p.get("upper"), "上界");
            if (lower >= upper) {
                throw new IllegalArgumentException("下界必须小于上界");
            }
            String code = p.get("code");
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("请粘贴代码");
            }
            double[] lowArr = new double[dimension];
            double[] upArr = new double[dimension];
            Arrays.fill(lowArr, lower);
            Arrays.fill(upArr, upper);
            ObjectiveFunction fn = UserCodeCompiler.compileAndCheck(code, dimension, lowArr, upArr);
            return new RuntimeProblem(dimension, lowArr, upArr, fn);
        }
        throw new IllegalArgumentException("未知的问题类型: " + type);
    }

    // =====================================================================
    // 预设问题
    // =====================================================================

    /** 5 个现成的连续问题，边界各自内置（与各问题类构造器一致）。 */
    private enum Preset {
        SPHERE("sphere", "Sphere（平方和）", -100, 100),
        ROSENBROCK("rosenbrock", "Rosenbrock（香蕉函数）", -30, 30),
        RASTRIGIN("rastrigin", "Rastrigin（多峰）", -5.12, 5.12),
        GRIEWANK("griewank", "Griewank（多峰）", -32, 32),
        ACKLEY("ackley", "Ackley（多峰）", -32, 32);

        final String id;
        final String name;
        final double lower;
        final double upper;

        Preset(String id, String name, double lower, double upper) {
            this.id = id;
            this.name = name;
            this.lower = lower;
            this.upper = upper;
        }

        ContinuousProblem create(int dim) {
            return switch (this) {
                case SPHERE -> new SphereProblem(dim);
                case ROSENBROCK -> new RosenbrockProblem(dim);
                case RASTRIGIN -> new RastriginProblem(dim);
                case GRIEWANK -> new GriewankProblem(dim);
                case ACKLEY -> new AckleyProblem(dim);
            };
        }

        static Preset byId(String id) {
            for (Preset p : values()) {
                if (p.id.equals(id)) return p;
            }
            throw new IllegalArgumentException("未知预设: " + id);
        }
    }

    // =====================================================================
    // JSON 输出（无第三方库，手动拼）
    // =====================================================================

    private static String presetsJson() {
        StringBuilder sb = new StringBuilder("[");
        Preset[] presets = Preset.values();
        for (int i = 0; i < presets.length; i++) {
            Preset p = presets[i];
            if (i > 0) sb.append(',');
            sb.append("{\"id\":").append(quote(p.id))
              .append(",\"name\":").append(quote(p.name))
              .append(",\"lower\":").append(p.lower)
              .append(",\"upper\":").append(p.upper).append('}');
        }
        return sb.append(']').toString();
    }

    private static String templatesJson() {
        StringBuilder sb = new StringBuilder("[");
        var templates = CodeTemplates.ALL;
        for (int i = 0; i < templates.size(); i++) {
            var t = templates.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"id\":").append(quote(t.id()))
              .append(",\"name\":").append(quote(t.name()))
              .append(",\"description\":").append(quote(t.description()))
              .append(",\"code\":").append(quote(t.code())).append('}');
        }
        return sb.append(']').toString();
    }

    private static String resultJson(MetaSearchRunner.SearchResult r) {
        return "{\"branch\":" + quote(r.branch())
                + ",\"coord\":" + doubleArrayJson(r.coord())
                + ",\"dimNames\":" + stringArrayJson(r.dimNames())
                + ",\"avgCost\":" + r.avgCost()
                + ",\"bestX\":" + doubleArrayJson(r.bestX())
                + ",\"evalCount\":" + r.evalCount()
                + ",\"elapsedMs\":" + r.elapsedMs() + '}';
    }

    private static String errorJson(String message) {
        return "{\"error\":" + quote(message) + '}';
    }

    private static String doubleArrayJson(double[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(arr[i]);
        }
        return sb.append(']').toString();
    }

    private static String stringArrayJson(String[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(quote(arr[i]));
        }
        return sb.append(']').toString();
    }

    private static String quote(String s) {
        return '"' + escapeJson(s) + '"';
    }

    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    // =====================================================================
    // 请求 / 响应 辅助
    // =====================================================================

    private static Map<String, String> parseForm(String body) {
        Map<String, String> map = new HashMap<>();
        for (String pair : body.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            String key = urlDecode(pair.substring(0, eq));
            String value = urlDecode(pair.substring(eq + 1));
            map.put(key, value);
        }
        return map;
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static int parseInt(String raw, String label) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException(label + "必须是整数");
        }
    }

    private static double parseDouble(String raw, String label) {
        try {
            return Double.parseDouble(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException(label + "必须是数字");
        }
    }

    private static String contentType(String path) {
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        return "text/html";
    }

    private static void serveFile(HttpExchange ex, String resourcePath, String contentType) throws IOException {
        try (InputStream in = WebServer.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                sendText(ex, 404, "Not Found: " + resourcePath);
                return;
            }
            byte[] bytes = in.readAllBytes();
            ex.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
        }
    }

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
    }

    private static void sendText(HttpExchange ex, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
