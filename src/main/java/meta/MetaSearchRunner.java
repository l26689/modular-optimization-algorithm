package meta;

import java.util.List;
import java.util.Random;

import oa.components.problems.coninuousproblem.ContinuousProblem;
import oa.components.recoders.BestRecorder;
import oa.components.terminationcondition.MaxCallTerminationCondition;
import sa.basicsa.SimulatedAnnealing;

/**
 * 元搜索驱动器 —— 用框架自身的模拟退火去搜索最优算法组合。
 *
 * <h3>为什么用框架自己的 SA 而不是另写一个搜索器</h3>
 * 这既是"用优化算法优化优化算法"的直接体现，也是一次对框架通用性的验证：
 * {@link MetaProblem}（配置空间）与 {@link RuntimeProblem}（真实问题）在
 * 框架眼中毫无区别，同一套 {@code SimulatedAnnealing} 与同一批组件
 * 可以不加修改地作用于两者。
 *
 * <h3>元层参数为何这样取</h3>
 * <ul>
 *   <li><b>初始温度 1.0</b> —— 配置空间已归一化到 [0,1]^6，
 *       温度 1.0 意味着初期几乎无条件接受，符合探索需求。</li>
 *   <li><b>扰动 σ=0.15</b> —— 相对整个 [0,1] 的跨度，单步约移动 15%，
 *       既能在配置空间中产生有意义的跳变，又不至于退化为纯随机。</li>
 *   <li><b>冷却率由预算反算</b> —— 使温度在预算耗尽时恰好降到终值，
 *       避免"跑完了温度还很高"（未收敛）或"很早就冻结"（早熟）。</li>
 * </ul>
 *
 * <h3>产出是排名，不只是一个赢家</h3>
 * {@link #run()} 返回<b>全部</b>评估过的配置的排名表。这比"最终那一个最优
 * 配置"更有价值：可以看到哪些选择真正拉开差距，以及是否存在
 * "稍差一点但快很多"的备选方案。
 *
 * <h3>关于预算与可信度</h3>
 * 元搜索预算是<b>尝试多少个配置</b>的上限。配置空间是 6 维连续空间，
 * 预算较小时元搜索接近随机采样——这并不影响排名表的有效性
 * （表中每一项都是真实跑出来的），只是无法保证找到全局最优。
 */
public class MetaSearchRunner {

    /** 元层退火的初始温度（配置空间已归一化到 [0,1]） */
    private static final double META_INITIAL_TEMP = 1.0;
    /** 元层退火的终止温度，用于反算冷却率 */
    private static final double META_FINAL_TEMP = 1e-2;
    /** 元层扰动的标准差比例（相对 [0,1] 跨度） */
    private static final double META_PERTURBATION_SCALE = 0.15;

    private final MetaProblem metaProblem;
    private final int metaBudget;
    private final long seed;

    /**
     * 构造元搜索驱动器。
     *
     * @param metaProblem 待搜索的元问题
     * @param metaBudget  元搜索的迭代预算（约等于尝试的配置数上限），必须为正
     * @param seed        元层随机种子，固定后可复现整次搜索
     * @throws IllegalArgumentException 预算非正
     */
    public MetaSearchRunner(MetaProblem metaProblem, int metaBudget, long seed) {
        if (metaProblem == null) {
            throw new NullPointerException("元问题不得为 null");
        }
        if (metaBudget <= 0) {
            throw new IllegalArgumentException("元搜索预算必须为正，当前为 " + metaBudget);
        }
        this.metaProblem = metaProblem;
        this.metaBudget = metaBudget;
        this.seed = seed;
    }

    /**
     * 执行元搜索并返回完整排名表。
     *
     * @return 全部已评估组合，按平均满意度降序排列
     */
    public List<EvaluationResult> run() {
        // 反算冷却率：使温度在 metaBudget 次迭代后从初温降到终温
        double coolingRate = Math.pow(META_FINAL_TEMP / META_INITIAL_TEMP, 1.0 / metaBudget);

        SimulatedAnnealing<double[], ContinuousProblem> metaSearch = new SimulatedAnnealing<>(
                new Random(seed),
                metaProblem,
                ComponentRegistry.createInitializer(META_INITIAL_TEMP),
                ComponentRegistry.createPerturbation(
                        ComponentRegistry.PERTURBATION_GAUSSIAN, META_PERTURBATION_SCALE),
                ComponentRegistry.createCooling(
                        ComponentRegistry.COOLING_GEOMETRIC, coolingRate, 1),
                new MaxCallTerminationCondition<double[]>(metaBudget));

        // 元层不需要从 Recorder 取结果——MetaProblem 自己缓存了全部评估记录
        BestRecorder<double[]> sink = new BestRecorder<>();
        metaSearch.solve(sink);

        return metaProblem.results();
    }

    /** 返回本次搜索使用的元层预算。 */
    public int metaBudget() {
        return metaBudget;
    }
}
