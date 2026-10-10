package meta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

import oa.api.optimizationalgorithm.OptimizationAlgorithm;
import oa.components.evaluate.AlgorithmBuilder;
import oa.components.problems.coninuousproblem.ContinuousProblem;
import oa.components.recoders.BestRecorder;

/**
 * 元问题 —— <b>把「选一个算法组合」变成一个可以被优化的连续问题</b>。
 *
 * <h3>核心思想：用优化算法优化优化算法</h3>
 * 本类实现 {@link ContinuousProblem}，其"解"是一段 6 维的配置向量
 * （编码方式见 {@link ConfigCodec}），其"目标值"是<b>该配置在真实问题上
 * 跑出来的平均满意度</b>。于是框架自身的模拟退火算法就可以用来搜索
 * 最优的算法组合——框架被用来优化它自己的配置空间。
 *
 * <pre>
 *   元层：  SA 搜索 6 维配置空间  ──evaluate──▶  MetaProblem
 *                                                    │ 解码
 *                                                    ▼
 *   内层：  用该配置组装 SA，在真实问题上跑 N 次 ──▶ 平均满意度
 * </pre>
 *
 * <h3>⚠️ 评估代价极高，缓存是必需的</h3>
 * 一次 {@code evaluate} 会触发 {@code repeats × innerBudget} 次真实的目标函数
 * 评估。而主算法在每次 {@code compare} 中都会调用两次 {@code evaluate}
 * （一次候选解、一次当前解），若不缓存，开销会迅速失控。
 * 本类用 {@link ConfigCodec.Config} 作为键缓存全部已评估配置，
 * record 的值相等性保证了相同配置必然命中。
 *
 * <h3>⚠️ 返回值是成本而非满意度</h3>
 * 与 {@link RuntimeProblem} 同理，框架的最小化语义要求
 * {@code evaluate} 返回"越小越好"的值，故此处返回
 * {@code -meanSatisfaction}。对外展示满意度请使用 {@link #results()}。
 *
 * <h3>统计可信度</h3>
 * 优化算法是随机的，同一配置多次运行结果不同。{@code repeats} 就是
 * 为了压低这一噪声——<b>若只跑一次就排名，排的其实是随机噪声</b>。
 * 建议 {@code repeats >= 3}。
 */
public class MetaProblem extends ContinuousProblem {

    /** 内层重复实验的随机种子基数，保证整体可复现 */
    private static final long SEED_BASE = 20261002L;

    private final RuntimeProblem innerProblem;
    private final int innerBudget;
    private final int repeats;

    /** 已评估配置的缓存。LinkedHashMap 保持插入顺序，便于按评估顺序导出。 */
    private final Map<ConfigCodec.Config, EvaluationResult> cache = new LinkedHashMap<>();

    /** 实际执行的内层运行总次数（不含缓存命中） */
    private int totalInnerRuns;
    /** 元层评估的累计耗时（纳秒，不含缓存命中）；用纳秒累计以避免快速运行的精度丢失 */
    private long totalNanos;

    /** 每评估出一个新配置时的回调，用于进度显示；可为 null */
    private Consumer<EvaluationResult> onEvaluated;

    /**
     * 构造元问题。
     *
     * @param innerProblem 待优化的真实问题，不得为 null
     * @param innerBudget  单次内层运行的迭代预算，必须为正；
     *                     直接作为 {@code MaxCallTerminationCondition} 的上限
     * @param repeats      每个配置重复运行的次数，必须为正；建议 ≥ 3
     * @throws NullPointerException     内层问题为 null
     * @throws IllegalArgumentException 预算或重复次数非正
     */
    public MetaProblem(RuntimeProblem innerProblem, int innerBudget, int repeats) {
        // 元层的搜索空间是归一化后的配置空间 [0,1]^6
        super(createBounds(ConfigCodec.DIM, 0.0), createBounds(ConfigCodec.DIM, 1.0));

        if (innerProblem == null) {
            throw new NullPointerException("内层问题不得为 null");
        }
        if (innerBudget <= 0) {
            throw new IllegalArgumentException("迭代预算必须为正，当前为 " + innerBudget);
        }
        if (repeats <= 0) {
            throw new IllegalArgumentException("重复次数必须为正，当前为 " + repeats);
        }

        this.innerProblem = innerProblem;
        this.innerBudget = innerBudget;
        this.repeats = repeats;
    }

    /**
     * 评估一个配置向量。
     * <p>
     * <b>返回的是成本（越小越优）</b>，即平均满意度的相反数。
     * 结果会被缓存，重复评估同一配置不会重新运行实验。
     *
     * @param rawConfig 配置向量，长度须为 {@link ConfigCodec#DIM}
     * @return 成本 = -平均满意度
     */
    @Override
    public Double evaluate(double[] rawConfig) {
        if (rawConfig == null || rawConfig.length != ConfigCodec.DIM) {
            throw new IllegalArgumentException(
                    "配置向量长度必须为 " + ConfigCodec.DIM + "，实际为 "
                            + (rawConfig == null ? "null" : rawConfig.length));
        }

        ConfigCodec.Config config = ConfigCodec.decode(rawConfig);
        EvaluationResult result = cache.get(config);
        if (result == null) {
            result = runExperiment(config);
            cache.put(config, result);
            if (onEvaluated != null) {
                onEvaluated.accept(result);
            }
        }
        return -result.meanSatisfaction();
    }

    /**
     * 用给定配置在内层问题上重复运行，收集统计量。
     *
     * <p>每次都<b>重新创建</b>全部组件与算法实例——因为
     * {@code SABasicCoolingSchedule} 等组件内部持有可变状态，
     * 跨运行复用会导致计数残留、结果错误。
     *
     * @param config 待评估的组合
     * @return 该组合的统计结果
     */
    private EvaluationResult runExperiment(ConfigCodec.Config config) {
        double sum = 0.0;
        double sumSq = 0.0;
        double best = Double.NEGATIVE_INFINITY;
        double worst = Double.POSITIVE_INFINITY;
        double[] bestSolution = null;

        long startNanos = System.nanoTime();

        // 用框架的 AlgorithmBuilder 抽象组装算法组合。
        // 每次 build 都产出全新的组件实例——SABasicCoolingSchedule 等内部
        // 持有可变状态，跨运行复用会导致计数残留、结果错误。
        AlgorithmBuilder<double[], ContinuousProblem> builder =
                ComponentRegistry.createAlgorithmBuilder(config, innerBudget);

        for (int run = 0; run < repeats; run++) {
            // 固定种子，使结果可复现；不同 run 用不同种子以体现随机性
            Random random = new Random(SEED_BASE + run);

            OptimizationAlgorithm<double[], ContinuousProblem, ?> algo =
                    builder.build(innerProblem, random);

            BestRecorder<double[]> recorder = new BestRecorder<>();
            algo.solve(recorder);

            // RuntimeProblem.evaluate 返回成本，取负得到满意度。
            // 顺便留下解本身——对排列这类问题，它才是真正的「答案」；
            // 只保留分数的话，结果里就只剩一个数字，看不出解长什么样。
            double[] solution = recorder.getBestX().clone();
            double satisfaction = -innerProblem.evaluate(solution);

            sum += satisfaction;
            sumSq += satisfaction * satisfaction;
            if (satisfaction > best) {
                best = satisfaction;
                bestSolution = solution;
            }
            if (satisfaction < worst) worst = satisfaction;
        }

        long elapsedNanos = System.nanoTime() - startNanos;

        double mean = sum / repeats;
        double variance = repeats > 1
                ? (sumSq - repeats * mean * mean) / (repeats - 1)
                : 0.0;
        double stdDev = variance > 0.0 ? Math.sqrt(variance) : 0.0;
        // 用纳秒换算，避免"快于 1ms 的运行为 0"的精度丢失
        double meanMillis = elapsedNanos / 1e6 / repeats;

        totalInnerRuns += repeats;
        totalNanos += elapsedNanos;

        return new EvaluationResult(config, mean, stdDev, best, worst, meanMillis, repeats,
                bestSolution);
    }

    /**
     * 返回迄今为止评估过的全部组合，按平均满意度降序排列。
     * <p>
     * 这是元搜索的<b>真正产出</b>——比起"最终找到的那一个最优配置"，
     * 一份完整的排名表更有价值：它揭示了哪些选择真正重要、
     * 以及"稍差一点但快很多"的备选方案。
     *
     * @return 按满意度降序排列的结果列表（副本，可安全修改）
     */
    public List<EvaluationResult> results() {
        List<EvaluationResult> list = new ArrayList<>(cache.values());
        list.sort(null);
        return list;
    }

    /** 返回已评估的不同配置数量（即缓存大小）。 */
    public int evaluatedConfigCount() {
        return cache.size();
    }

    /** 返回实际执行的内层运行总次数（不含缓存命中）。 */
    public int totalInnerRuns() {
        return totalInnerRuns;
    }

    /** 返回内层实验的累计耗时（毫秒）。 */
    public double totalMillis() {
        return totalNanos / 1e6;
    }

    /** 返回单次内层运行的迭代预算。 */
    public int innerBudget() {
        return innerBudget;
    }

    /** 返回每个配置的重复运行次数。 */
    public int repeats() {
        return repeats;
    }

    /** 返回被优化的真实问题。 */
    public RuntimeProblem innerProblem() {
        return innerProblem;
    }

    /**
     * 注册一个进度回调，每评估出一个<b>新</b>配置时触发一次（缓存命中不触发）。
     * <p>
     * 元搜索可能持续数十秒，没有反馈的静默等待体验很差。
     * 回调在主线程内同步执行，因此实现必须<b>快速返回</b>，不要做耗时操作。
     *
     * @param listener 回调；传 {@code null} 可取消注册
     */
    public void setOnEvaluated(Consumer<EvaluationResult> listener) {
        this.onEvaluated = listener;
    }
}
