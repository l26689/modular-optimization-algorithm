package moa.example.meta;

import java.util.HashMap;
import java.util.Map;

import moa.problem.continuous.ContinuousProblem;
import moa.problem.evaluate.AlgorithmEvaluationProblem;

/**
 * <b>满意度函数</b>：把一个"算法配置坐标"映射成该配置的性能分数（越小越好）。
 * <p>
 * 继承 {@link ContinuousProblem} 是为了白拿三样东西：
 * <ul>
 *   <li>维度与边界（外层 PSO 的 {@code StandardPSOParticle} 只认 ContinuousProblem）；</li>
 *   <li>{@code copyX}（深拷贝，PSO 引擎要用来保存 gBest）；</li>
 *   <li>{@code compare}（{@code evaluate(x2) - evaluate(x1)}，正值表示 x1 更优，
 *       方向正好是"性能值越小越好"，不需要额外翻转）。</li>
 * </ul>
 * 只需要实现 {@link #evaluate}。
 *
 * <h3>为什么必须缓存</h3>
 * 内层 PSO 每个粒子每轮都要遍历全群找 gBest，而每次比较都要调两次
 * {@code evaluate}——一轮就是 O(N²) 次。每一次 {@code evaluate} 又意味着
 * "完整跑一遍被评估的算法"。没有缓存的话整个程序跑不动。
 * <p>
 * 缓存<b>是安全的</b>（不会失真），因为 {@link AlgorithmEvaluationProblem}
 * 内部用固定种子（其 {@code SEED} 常量）跑每一遍，所以"同一个坐标 → 同一个分数"
 * 是纯函数，重复求值结果必然相同。这也意味着你换用别的评估方式时，
 * 要先确认它同样确定性。
 *
 * @see BranchAlgorithmBuilder
 */
public final class ParameterSpaceProblem extends ContinuousProblem {

    /**
     * 非法/跑不起来的配置的惩罚值。
     * <p>
     * 取 1e12 而不是 {@code Double.MAX_VALUE}，是为了给 {@code compare}
     * 里的相减留出空间（{@code evaluate(x2) - evaluate(x1)} 不会溢出成
     * Infinity 或 NaN）。相对真实目标值（个位到几千）足够大，
     * 保证这种配置永远排在最后。
     */
    private static final double PENALTY = 1.0e12;

    /** 内层评估器：对给定的配置跑 RUNS 遍目标问题，返回平均最优值。 */
    private final AlgorithmEvaluationProblem<double[], ContinuousProblem> inner;

    /** 内层算法的迭代预算，透传给 {@link BranchAlgorithmBuilder}。 */
    private final int innerBudget;

    /** 坐标 → 分数 的缓存。key 是量化后的坐标字符串。 */
    private final Map<String, Double> cache = new HashMap<>();

    public ParameterSpaceProblem(AlgorithmEvaluationProblem<double[], ContinuousProblem> inner) {
        this(inner, MetaPSOConfig.INNER_BUDGET);
    }

    public ParameterSpaceProblem(AlgorithmEvaluationProblem<double[], ContinuousProblem> inner, int innerBudget) {
        super(MetaPSOConfig.LOWER, MetaPSOConfig.UPPER);
        this.inner = inner;
        this.innerBudget = innerBudget;
    }

    /**
     * 求"这一组坐标对应的算法配置"的性能。
     *
     * @param coord 算法配置坐标（外层 PSO 的一个位置）
     * @return 平均最优目标值，越小越好；跑不起来的配置返回 {@link #PENALTY}
     */
    @Override
    public Double evaluate(double[] coord) {
        String key = keyOf(coord);
        Double cached = cache.get(key);
        if (cached != null) {
            return cached;      // 命中缓存，直接返回（这是性能的关键路径）
        }

        double value;
        try {
            value = inner.evaluate(new BranchAlgorithmBuilder(coord, innerBudget));
        } catch (RuntimeException e) {
            // 目前只有 GA 占位分支会走到这里（抛 UnsupportedOperationException）。
            // 用惩罚值而不是让异常冒泡：异常会终止整个元搜索，而惩罚值能让
            // GA 支自然被淘汰（它的分数永远最差，不会被选为 gBest）。
            //
            // 注意：SA 的组件【不做参数校验】，所以坐标采到语义上不合理的值
            // （降温率过大、初始温度为负）不会抛异常，只会安静地给出差分数。
            // 边界靠 MetaPSOConfig 的 LOWER/UPPER 守住。
            value = PENALTY;
        }

        cache.put(key, value);
        return value;
    }

    /**
     * 把坐标量化成缓存 key。
     * <p>
     * 量化到 1e-6 是为了让浮点末位抖动不产生"新配置"——PSO 的位置更新是
     * 连续浮点运算，同一个配置在数值上几乎不可能精确重复，不量化的话
     * 缓存命中率会很低。
     */
    private static String keyOf(double[] coord) {
        StringBuilder sb = new StringBuilder(coord.length * 12);
        for (double v : coord) {
            sb.append(Math.round(v * 1e6)).append('|');
        }
        return sb.toString();
    }

    /** 已缓存的配置数量，用于观察搜索是否真的在探索新配置。 */
    public int cacheSize() {
        return cache.size();
    }
}
