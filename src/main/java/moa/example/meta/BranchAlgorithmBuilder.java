package moa.example.meta;

import java.util.Random;

import moa.algorithm.de.components.DERand1Particle;
import moa.algorithm.pso.api.PSOState;
import moa.algorithm.pso.api.Particle;
import moa.algorithm.pso.components.StandardPSOParticle;
import moa.algorithm.pso.engine.ParticleSwarmOptimization;
import moa.algorithm.sa.components.SABasicCoolingSchedule;
import moa.algorithm.sa.components.SABasicInitializer;
import moa.algorithm.sa.engine.BasicSA;
import moa.framework.engine.OptimizationAlgorithm;
import moa.problem.continuous.ContinuousProblem;
import moa.problem.continuous.components.ContinuousUniformSearch;
import moa.problem.evaluate.AlgorithmBuilder;

/**
 * <b>解码器</b>：把"一组坐标"翻译成"一个组装好的算法"。
 * <p>
 * 这是元优化的关键一环——外层 PSO 只认坐标，是这里把坐标变成真正能跑的算法。
 * 每个分支的装配方式都照搬框架示例的写法（见
 * {@code moa.example.SphereProblemDemo}），不修改任何框架类。
 *
 * <h3>各分支的"切换粒度"不一样，这点要理解</h3>
 * <ul>
 *   <li><b>SA</b>：换<b>引擎</b>——用 {@link BasicSA}，状态类型是 BasicSAState。</li>
 *   <li><b>PSO</b>：同一个 {@link ParticleSwarmOptimization} 引擎，换<b>粒子组</b>
 *       （{@link StandardPSOParticle}[]）。</li>
 *   <li><b>DE</b>：同样是 PSO 引擎，换粒子组（{@link DERand1Particle}[]）。
 *       DE 在这个框架里没有独立引擎，它就是"PSO 引擎 + DE 粒子"。</li>
 *   <li><b>GA</b>：占位，见 {@link #build} 里的说明。</li>
 * </ul>
 *
 * <h3>泛型数组</h3>
 * PSO 引擎的构造器要的是 {@code Particle<...>[]}，而 Java 不允许直接
 * {@code new Particle<double[],...>[n]}（泛型数组创建非法），所以只能先造
 * 一个通配符数组再强转，见 {@link #newParticleArray}。
 *
 * @see ParameterSpaceProblem
 * @see MetaPSOConfig
 */
public final class BranchAlgorithmBuilder implements AlgorithmBuilder<double[], ContinuousProblem> {

    /** 这一组坐标（组件之外的人不应修改它）。 */
    private final double[] coord;

    /** 内层算法的迭代预算（解目标问题时跑多少轮）。 */
    private final int innerBudget;

    public BranchAlgorithmBuilder(double[] coord) {
        this(coord, MetaPSOConfig.INNER_BUDGET);
    }

    public BranchAlgorithmBuilder(double[] coord, int innerBudget) {
        this.coord = coord;
        this.innerBudget = innerBudget;
    }

    /**
     * 按坐标里的算法种类组装对应的算法。
     *
     * @param problem 内层算法要解的问题
     * @param random  随机源（由 AlgorithmEvaluationProblem 提供固定种子，保证可复现）
     * @return 可运行的算法实例
     * @throws UnsupportedOperationException 当坐标指向 GA 支时
     */
    @Override
    public OptimizationAlgorithm<double[], ContinuousProblem, ?> build(
            ContinuousProblem problem, Random random) {

        int branch = MetaPSOConfig.branchOf(coord);
        switch (branch) {
            case MetaPSOConfig.BRANCH_SA:
                return buildSA(problem, random);
            case MetaPSOConfig.BRANCH_PSO:
                return buildPSO(problem, random);
            case MetaPSOConfig.BRANCH_DE:
                return buildDE(problem, random);
            default:
                // GA 占位分支。
                // 为什么装配不起来：BasicGA 的构造器需要 TotalPopOperator，
                // 而 ga/components 里只有一个 CustomPopOperator，它又需要
                // Selection / Crossover / Mutation / Replacement 四个组件——
                // 这四个接口在 ga/api 里都存在，但【没有任何实现类】。
                // 同理 GeneType 也没有实现，连续问题的基因编码也缺。
                // 所以这里只能抛异常，由 ParameterSpaceProblem 接住并给惩罚值，
                // 使 GA 支自然被淘汰。
                //
                // 等框架里补上这些组件后，把下面这段换成真正的装配（照 buildPSO 写），
                // 并把 MetaPSOConfig.RUNNABLE_BRANCHES 里加上 BRANCH_GA 即可。
                throw new UnsupportedOperationException(
                        "GA 分支尚未实现：ga/api 下的 Selection/Crossover/Mutation/"
                        + "Replacement/GeneType 都还没有实现类，BasicGA 装配不起来。");
        }
    }

    // =====================================================================
    // SA 支：换引擎
    // =====================================================================

    /**
     * 装配 SA。
     * <p>
     * 可调点：这里固定用了最朴素的组合（基础降温 + 均匀邻域扰动）。
     * 框架里还有 {@code SAAdaptiveCoolingSchedule}（自适应降温，参数更多）可以换，
     * 换的话记得在 MetaPSOConfig 里给它加坐标维。注意自适应降温的构造器
     * <b>会做参数校验并抛异常</b>，与下面两个组件不同。
     * <p>
     * 已知限制：扰动的幅度被 {@link ContinuousUniformSearch} 硬编码成
     * 边界跨度的 10%（该类第 70 行），<b>不可调</b>。想让 SA 支多一个
     * "扰动幅度"坐标，需要自己写一个带参的搜索算子。
     */
    private OptimizationAlgorithm<double[], ContinuousProblem, ?> buildSA(
            ContinuousProblem problem, Random random) {

        CountingProblem counting = new CountingProblem(problem);

        double initTemp = coord[MetaPSOConfig.I_SA_TEMP];
        double coolRate = coord[MetaPSOConfig.I_SA_COOL_RATE];

        return new BasicSA<double[], ContinuousProblem>(
                random,
                counting,
                new SABasicInitializer(initTemp),
                new ContinuousUniformSearch(),
                new SABasicCoolingSchedule<double[]>(coolRate, MetaPSOConfig.SA_LADDER),
                new EvaluationBudgetTerminationCondition(counting, innerBudget));
    }

    // =====================================================================
    // PSO 支：换粒子组
    // =====================================================================

    /** 装配标准 PSO。群规模固定为 {@link MetaPSOConfig#INNER_POP}，不进坐标。 */
    private OptimizationAlgorithm<double[], ContinuousProblem, ?> buildPSO(
            ContinuousProblem problem, Random random) {

        CountingProblem counting = new CountingProblem(problem);

        double omega = coord[MetaPSOConfig.I_PSO_OMEGA];
        double c1 = coord[MetaPSOConfig.I_PSO_C1];
        double c2 = coord[MetaPSOConfig.I_PSO_C2];

        int n = MetaPSOConfig.INNER_POP;
        Particle<double[], ContinuousProblem, PSOState<double[]>>[] particles = newParticleArray(n);
        for (int i = 0; i < n; i++) {
            particles[i] = new StandardPSOParticle(omega, c1, c2);
        }

        return new ParticleSwarmOptimization<double[], ContinuousProblem>(
                random,
                counting,
                particles,
                new EvaluationBudgetTerminationCondition(counting, innerBudget));
    }

    // =====================================================================
    // DE 支：同样是 PSO 引擎，换 DE 粒子
    // =====================================================================

    /**
     * 装配 DE（用 PSO 引擎承载 DE 粒子）。
     * <p>
     * 可调点：这里固定用 {@link DERand1Particle}（rand/1/bin 策略）。
     * 框架里还有 {@code DEParticle}、{@code DEBest1Particle}、
     * {@code DECurrentToBest1Particle}、{@code DERand2Particle} 四种策略可选，
     * 想让它们也参与搜索的话，可以再加一个"DE 策略"的离散坐标维，
     * 在这里 switch 选择即可。
     * <p>
     * 注意构造器第三个参数 {@code myIndex} 是粒子在群里的下标，
     * 属于结构信息，<b>不是</b>可调参数——DE 的差分向量要靠它排除自己，
     * 给错了会导致变异用到自身位置。
     */
    private OptimizationAlgorithm<double[], ContinuousProblem, ?> buildDE(
            ContinuousProblem problem, Random random) {

        CountingProblem counting = new CountingProblem(problem);

        double f = coord[MetaPSOConfig.I_DE_F];
        double cr = coord[MetaPSOConfig.I_DE_CR];

        int n = MetaPSOConfig.INNER_POP;
        Particle<double[], ContinuousProblem, PSOState<double[]>>[] particles = newParticleArray(n);
        for (int i = 0; i < n; i++) {
            particles[i] = new DERand1Particle(f, cr, i);
        }

        return new ParticleSwarmOptimization<double[], ContinuousProblem>(
                random,
                counting,
                particles,
                new EvaluationBudgetTerminationCondition(counting, innerBudget));
    }

    // =====================================================================
    // 工具
    // =====================================================================

    /**
     * 创建泛型粒子数组。
     * <p>
     * Java 不允许 {@code new Particle<double[],...>[n]}（泛型数组创建非法），
     * 标准做法是先创建一个通配符数组（它是 reifiable 的，合法），再强转。
     * 强转会有未检查警告，所以标了 {@code @SuppressWarnings}。
     * <p>
     * 数组元素类型 {@code Particle<double[], ContinuousProblem, PSOState<double[]>>}
     * 同时兼容 StandardPSOParticle 和所有 DEParticle 子类（它们的声明完全一致），
     * 所以 PSO 支和 DE 支能共用这一个方法。
     */
    @SuppressWarnings("unchecked")
    private static Particle<double[], ContinuousProblem, PSOState<double[]>>[] newParticleArray(int n) {
        return (Particle<double[], ContinuousProblem, PSOState<double[]>>[]) new Particle<?, ?, ?>[n];
    }
}
