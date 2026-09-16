package oa.examples.deploymentproblem;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 部署问题 HTTP 通信工具类，封装与 JS 评估服务器的交互。
 *
 * <p>
 * 本类负责将 actionId 数组发送到 JS 端的 {@code /api/evaluate} 端点，
 * 并解析返回的覆盖指标。使用 {@link HttpClient} 并强制 HTTP/1.1 模式，
 * 内置连接池和重试机制以应对高频率评估调用。
 *
 * <h3>线程安全</h3>
 * 本类的所有字段均为 {@code final}，构造后不可变。
 * {@link HttpClient} 本身是线程安全的，可被多个线程共享。
 * 纯函数方法 {@code evaluate()} 无内部可变状态，天然线程安全。
 *
 * <h3>设计约束</h3>
 * <ul>
 *   <li>本类实例化后不可变，可被多个组件安全共享。</li>
 *   <li>内部复用 {@link HttpClient} 实例，利用其内置连接池避免端口耗尽。</li>
 *   <li>采用简单的 JSON 字符串拼接构造请求体，避免引入第三方 JSON 库。</li>
 * </ul>
 */
public final class DeploymentUtil {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 50;

    private final String serverUrl;
    private final HttpClient httpClient;

    public DeploymentUtil(String serverUrl) {
        this.serverUrl = serverUrl;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * 评估一个解，向 JS 服务器发送 actionIds 并获取覆盖指标。
     * 内置重试机制，在连接失败时自动重试。
     *
     * @param sessionId 会话 ID，JS 端用于查找缓存的场景数据
     * @param actionIds 选中的部署动作 ID 列表
     * @return 包含检测覆盖率、打击覆盖率、设备数量等指标的评估结果
     * @throws RuntimeException 如果网络通信失败或 JS 端返回错误
     */
    public EvaluationResult evaluate(String sessionId, int[] actionIds) {
        String requestBody = buildRequestBody(sessionId, actionIds);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/api/evaluate"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        IOException lastException = null;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    String errorMsg = extractJsonString(response.body(), "error");
                    throw new RuntimeException("JS 评估失败 (HTTP " + response.statusCode() + "): "
                            + (errorMsg != null ? errorMsg : response.body()));
                }

                return parseResult(response.body());

            } catch (ConnectException e) {
                throw new RuntimeException(
                        "无法连接到 JS 评估服务器 (" + serverUrl + ")。请确保 deployment-simulator 已启动。",
                        e);
            } catch (IOException e) {
                lastException = e;
                if (attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("评估请求被中断", ie);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("评估请求被中断", e);
            }
        }

        throw new RuntimeException("HTTP 通信异常（重试 " + MAX_RETRIES + " 次后仍失败）: "
                + (lastException != null ? lastException.getMessage() : "未知错误"), lastException);
    }

    private String buildRequestBody(String sessionId, int[] actionIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"sessionId\":\"").append(sessionId).append("\",\"actionIds\":[");
        for (int i = 0; i < actionIds.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(actionIds[i]);
        }
        sb.append("]}");
        return sb.toString();
    }

    private EvaluationResult parseResult(String json) {
        EvaluationResult r = new EvaluationResult();
        r.detectionPercentage = extractJsonNumber(json, "detectionPercentage");
        r.attackPercentage = extractJsonNumber(json, "attackPercentage");
        r.jointPercentage = extractJsonNumber(json, "jointPercentage");
        r.totalCount = (int) extractJsonNumber(json, "totalCount");
        r.detectorCount = (int) extractJsonNumber(json, "detectorCount");
        r.strikerCount = (int) extractJsonNumber(json, "strikerCount");
        return r;
    }

    static double extractJsonNumber(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start == -1) return 0;
        start += searchKey.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end))
                || json.charAt(end) == '.' || json.charAt(end) == '-' || json.charAt(end) == 'e'
                || json.charAt(end) == 'E')) {
            end++;
        }
        if (end <= start) return 0;
        return Double.parseDouble(json.substring(start, end));
    }

    static String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) return null;
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    /**
     * 评估结果数据结构，包含解的覆盖指标。
     */
    public static class EvaluationResult {
        public double detectionPercentage;
        public double attackPercentage;
        public double jointPercentage;
        public int totalCount;
        public int detectorCount;
        public int strikerCount;

        @Override
        public String toString() {
            return String.format(
                    "[detection=%.1f%%, attack=%.1f%%, joint=%.1f%%, total=%d (detector=%d, striker=%d)]",
                    detectionPercentage, attackPercentage, jointPercentage,
                    totalCount, detectorCount, strikerCount);
        }
    }
}