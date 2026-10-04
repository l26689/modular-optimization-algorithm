package oa.examples.deploymentproblem;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import oa.api.problem.Evaluable;
import oa.api.problem.Problem;

/**
 * 部署优化问题，将设备部署建模为子集选择问题。
 *
 * <p>
 * 解表示为一个 {@code int[]}，其中每个元素是 JS 端生成的 actionId。
 * 本类通过 {@link DeploymentUtil} 调用 JS 端的 {@code /api/evaluate} 端点
 * 获取解的覆盖指标，不直接理解场景语义（建筑、格点、mask 等）。
 *
 * <h3>多目标优化</h3>
 * 评估返回三个目标：检测覆盖率、打击覆盖率、设备总数（取负）。
 * {@link #compare(int[], int[])} 基于 Pareto 支配关系实现偏序比较：
 * <ul>
 *   <li>正结果：x1 支配 x2（在所有目标上不劣于 x2，且至少一个目标严格优于）</li>
 *   <li>负结果：x2 支配 x1</li>
 *   <li>零：互不支配或等优</li>
 * </ul>
 *
 * <h3>评估缓存</h3>
 * 由于 {@code evaluate()} 涉及 HTTP 网络调用（10-50ms），
 * 本类内置线程安全的 LRU 缓存。缓存键基于排序后的 actionId 集合（与顺序无关），
 * 最大容量 10000 条，超出时自动淘汰最久未访问的条目。
 * 该设计可显著降低 SA 主循环中 {@link #compare} 的重复评估开销。
 *
 * <h3>设计约束</h3>
 * <ul>
 *   <li>{@link #copyX(int[])} 返回防御性副本，遵循不可变性原则。</li>
 *   <li>{@link DeploymentUtil} 通过构造函数注入，本类不持有可变静态状态。</li>
 * </ul>
 */
public final class DeploymentProblem implements Problem<int[]>, Evaluable<int[], double[]> {

    private static final int MAX_CACHE_SIZE = 10000;

    private final String sessionId;
    private final String objective;
    private final int maxTotalCount;
    private final DeploymentUtil util;

    private final Map<String, double[]> cache;

    public DeploymentProblem(String sessionId, String objective, int maxTotalCount, DeploymentUtil util) {
        this.sessionId = sessionId;
        this.objective = objective;
        this.maxTotalCount = maxTotalCount;
        this.util = util;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<String, double[]>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, double[]> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        });
    }

    public String getObjective() {
        return objective;
    }

    public int getMaxTotalCount() {
        return maxTotalCount;
    }

    /**
     * 评估解，返回三目标向量。
     * 优先从缓存获取，缓存未命中时通过 {@link DeploymentUtil} 发起 HTTP 请求。
     *
     * <p>
     * 目标向量为 [detectionPercentage, attackPercentage, -totalCount]，
     * 全部沿最大化方向。第三个目标取负是为了统一比较方向：
     * 设备数量越少越好 → 取负后越大越好。
     *
     * <p>
     * 缓存键基于排序后的 actionId 集合，与数组中的元素顺序无关。
     * 返回的目标向量是每次新建的数组，确保调用方不会意外修改缓存内容。
     */
    @Override
    public double[] evaluate(int[] actionIds) {
        String key = toCacheKey(actionIds);
        synchronized (cache) {
            double[] cached = cache.get(key);
            if (cached != null) {
                return Arrays.copyOf(cached, cached.length);
            }
        }
        DeploymentUtil.EvaluationResult r = util.evaluate(sessionId, actionIds);
        double[] result = new double[] { r.detectionPercentage, r.attackPercentage, -(double) r.totalCount };
        synchronized (cache) {
            cache.putIfAbsent(key, result);
        }
        return result;
    }

    /**
     * 构建缓存键。
     * 对 actionIds 排序后取 {@link Arrays#toString}，确保键与数组元素顺序无关。
     */
    private static String toCacheKey(int[] actionIds) {
        int[] sorted = Arrays.copyOf(actionIds, actionIds.length);
        Arrays.sort(sorted);
        return Arrays.toString(sorted);
    }

    /**
     * 基于 Pareto 支配的偏序比较。
     */
    @Override
    public double compare(int[] x1, int[] x2) {
        double[] obj1 = evaluate(x1);
        double[] obj2 = evaluate(x2);
        return dominanceDistance(obj1, obj2);
    }

    /**
     * 计算 Pareto 支配距离。
     *
     * <p>
     * 支配定义：x1 支配 x2 当且仅当：
     * <ul>
     *   <li>x1 在所有目标上都不劣于 x2（≥）</li>
     *   <li>x1 在至少一个目标上严格优于 x2（>）</li>
     * </ul>
     * 若 x1 支配 x2，返回所有目标上优势之和（正值）；
     * 若 x2 支配 x1，返回所有目标上劣势之和（负值）；
     * 若互不支配，返回 0。
     */
    private static double dominanceDistance(double[] obj1, double[] obj2) {
        boolean x1Better = false;
        boolean x2Better = false;
        double sum = 0;
        for (int i = 0; i < obj1.length; i++) {
            sum += obj1[i] - obj2[i];
            if (obj1[i] > obj2[i]) x1Better = true;
            if (obj2[i] > obj1[i]) x2Better = true;
        }
        if (x1Better && !x2Better) return sum;
        if (x2Better && !x1Better) return sum;
        return 0;
    }

    /**
     * 创建解的防御性副本。
     */
    @Override
    public int[] copyX(int[] x) {
        return Arrays.copyOf(x, x.length);
    }

}