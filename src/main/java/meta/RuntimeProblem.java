package meta;

import oa.components.problems.coninuousproblem.ContinuousProblem;

/**
 * 运行时可构造的问题适配器 —— 把"用户的三种输入"变成框架认识的 {@code Problem}。
 * <p>
 * 用户输入：<b>维度 + 统一上下界 + 满意度函数</b>；
 * 本类把它们组装成一个 {@link ContinuousProblem}，从而可以直接接入
 * {@code SimulatedAnnealing}、{@code ParticleSwarmOptimization} 等任何算法。
 *
 * <h3>⚠️ 语义换算：本类的 {@code evaluate()} 返回的是「成本」而非「满意度」</h3>
 * {@link ObjectiveFunction} 约定"满意度越大越优"，而框架
 * {@link ContinuousProblem#compare} 采用最小化语义（{@code evaluate} 越小越优）。
 * 二者相差一个负号：
 * <pre>{@code
 *   evaluate(x) = -objective.evaluate(x) = cost(x)
 * }</pre>
 * 因此直接调用 {@code evaluate()} 得到的是<b>成本</b>（越小越优）。
 * 若要向用户展示满意度，请取负：{@code satisfaction = -evaluate(x)}。
 * <p>
 * 必须保持"只取负号"这一换算，不可换成 {@code 1/(1+cost)} 之类，
 * 否则会改变 {@code compare} 的数值尺度，破坏模拟退火的温度语义。
 * 详见 {@link ObjectiveFunction}。
 *
 * <h3>评估缓存</h3>
 * 模拟退火在主循环中会反复评估"当前解"（未接受新解时它保持不变），
 * 因此本类缓存最近一次评估结果，可显著减少目标函数调用次数。
 * 这与 README 中"为评估添加缓存"的建议一致。
 * 缓存的正确性由 {@link ObjectiveFunction} 的<b>纯函数</b>契约保证。
 */
public class RuntimeProblem extends ContinuousProblem {

    private final ObjectiveFunction objective;

    /** 最近一次评估的解（防御性拷贝），用于缓存命中判断 */
    private double[] lastX;
    /** 最近一次评估的成本，null 表示尚无缓存 */
    private Double lastCost;

    /**
     * 构造一个运行时问题。
     *
     * @param dimension 解向量维度，必须为正
     * @param lower     所有维度的统一下界
     * @param upper     所有维度的统一上界，必须大于 {@code lower}
     * @param objective 满意度函数，不得为 null；必须为纯函数
     * @throws IllegalArgumentException 维度或范围非法
     * @throws NullPointerException     {@code objective} 为 null
     */
    public RuntimeProblem(int dimension, double lower, double upper, ObjectiveFunction objective) {
        super(createBoundsChecked(dimension, lower), createBoundsChecked(dimension, upper));
        if (objective == null) {
            throw new NullPointerException("满意度函数不得为 null");
        }
        this.objective = objective;
    }

    /**
     * 评估一个解的成本，结果越小越优。
     * <p>
     * <b>注意返回的是成本（= 满意度的相反数）</b>，这是框架的最小化语义所要求的。
     *
     * @param x 待评估的解，长度必须等于问题维度
     * @return 成本值（越小越优），保证为有限值
     * @throws IllegalArgumentException 解的长度与维度不符，或函数返回非有限值
     */
    @Override
    public Double evaluate(double[] x) {
        if (x == null || x.length != getDimension()) {
            throw new IllegalArgumentException(
                    "解的长度必须为 " + getDimension() + "，实际为 "
                            + (x == null ? "null" : x.length));
        }
        // 缓存命中：SA 在未接受新解时会反复评估同一个当前解
        if (lastX != null && java.util.Arrays.equals(x, lastX)) {
            return lastCost;
        }

        double satisfaction = objective.evaluate(x);
        if (!Double.isFinite(satisfaction)) {
            throw new IllegalArgumentException(
                    "满意度函数返回了非有限值（" + satisfaction + "）。"
                            + "请检查函数是否出现除零、log 非正数或 sqrt 负数等退化情形，"
                            + "并在函数内部改返回一个有限的大惩罚值。");
        }

        double cost = -satisfaction;

        this.lastX = x.clone();
        this.lastCost = cost;
        return cost;
    }

    /**
     * 计算一个解的满意度（用户视角的分数，越大越优）。
     * <p>
     * 便捷方法，等价于 {@code -evaluate(x)}。
     *
     * @param x 待评估的解
     * @return 满意度分数，越大越优
     */
    public double satisfaction(double[] x) {
        return -evaluate(x);
    }

    /** 返回本问题使用的满意度函数。 */
    public ObjectiveFunction objective() {
        return objective;
    }

    private static double[] createBoundsChecked(int dimension, double value) {
        if (dimension <= 0) {
            throw new IllegalArgumentException("维度必须为正整数，当前为 " + dimension);
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("范围端点必须为有限值，当前为 " + value);
        }
        double[] bounds = new double[dimension];
        java.util.Arrays.fill(bounds, value);
        return bounds;
    }
}
