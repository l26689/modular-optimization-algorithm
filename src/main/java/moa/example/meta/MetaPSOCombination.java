package moa.example.meta;

import java.util.Arrays;
import java.util.Random;

import moa.algorithm.pso.api.PSOState;
import moa.algorithm.pso.api.Particle;
import moa.algorithm.pso.engine.ParticleSwarmOptimization;
import moa.components.recorder.BestRecorder;
import moa.components.terminator.MaxCallTerminationCondition;
import moa.framework.engine.OptimizationAlgorithm;
import moa.problem.continuous.ContinuousProblem;
import moa.problem.continuous.SphereProblem;
import moa.problem.evaluate.AlgorithmEvaluationProblem;

/**
 * 元优化器入口：用"伪 PSO"在算法配置空间里搜索，找出在该问题上表现最好的算法组合。
 *
 * <h3>这个程序到底在优化什么</h3>
 * 注意区分两个层次，别搞混：
 * <ul>
 *   <li><b>内层</b>：一个普通优化算法（SA / PSO / DE），目标是解用户给定的问题
 *       （这里是 {@link SphereProblem}），产出的是"问题的解"（一个 double[]）；</li>
 *   <li><b>外层</b>：{@link TransferPSOParticle} 组成的伪 PSO，目标是在"算法配置"
 *       空间里搜索，产出的是"最优算法配置"（一组参数坐标）。</li>
 * </ul>
 * 所以 <b>外层 {@code getBestX()} 返回的是坐标，不是问题的解</b>——这是元优化
 * 最容易犯的错。要拿到那个配置真正的解，必须拿坐标再跑一次内层算法，
 * 见 {@link #main} 最后一段。
 *
 * <h3>运行</h3>
 * <pre>
 *   mvn compile
 *   java -cp target/classes moa.example.meta.MetaPSOCombination
 * </pre>
 * 所有可调参数都在 {@link MetaPSOConfig} 里，改那个文件就够了。
 *
 * <h3>已知限制（v1 不解决，改的时候留意）</h3>
 * <ol>
 *   <li><b>预算口径不公平</b>：内层终止条件数的是"迭代轮数"，但 SA 一轮约 4 次
 *       目标函数评估，PSO 一轮约 2N² 次。三支用相同的轮数预算跑，PSO 实际拿到的
 *       评估次数多得多，所以比出来的"最优"是偏向 PSO 的。
 *       要真正公平，得改成按"目标函数评估次数"对齐（写一个计数装饰 Problem +
 *       读计数的终止条件）。</li>
 *   <li>DE 策略固定为 rand/1/bin，没有把策略本身做成坐标。</li>
 *   <li>GA 支是占位，恒返回惩罚值。</li>
 * </ol>
 */
public class MetaPSOCombination {

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        // =================================================================
        // 1) 内层：待优化的问题 + 评估器
        // =================================================================
        // AlgorithmEvaluationProblem 已经写好了"跑 RUNS 遍、固定种子、取平均"
        // 这套公平评估逻辑，直接复用。
        // 换成别的问题（RosenbrockProblem / RastriginProblem ...）只要改这一行。
        ContinuousProblem target = new SphereProblem(2);

        AlgorithmEvaluationProblem<double[], ContinuousProblem> inner =
                new AlgorithmEvaluationProblem<>(target, MetaPSOConfig.RUNS);

        // 满意度函数：坐标 → 该配置的平均表现
        ParameterSpaceProblem space = new ParameterSpaceProblem(inner);

        // =================================================================
        // 2) 外层：伪 PSO
        // =================================================================
        TransferPSOParticle[] swarm =
                new TransferPSOParticle[MetaPSOConfig.META_SWARM_SIZE];
        for (int i = 0; i < swarm.length; i++) {
            swarm[i] = new TransferPSOParticle();
        }

        ParticleSwarmOptimization<double[], ParameterSpaceProblem> metaPso =
                new ParticleSwarmOptimization<double[], ParameterSpaceProblem>(
                        new Random(MetaPSOConfig.SEED),   // 固定种子 → 结果可复现
                        space,
                        asParticleArray(swarm),
                        new MaxCallTerminationCondition<double[]>(MetaPSOConfig.META_ITERATIONS));

        BestRecorder<double[]> recorder = new BestRecorder<>();
        metaPso.solve(recorder);

        // =================================================================
        // 3) 结果 1：最优"配置"（坐标）
        // =================================================================
        double[] bestCoord = recorder.getBestX();
        printCoordinate(bestCoord);
        System.out.printf("该配置的平均表现 = %.6f   （越小越好）%n", space.evaluate(bestCoord));
        System.out.printf("搜索期间实际评估过的不同配置数 = %d%n", space.cacheSize());

        int totalJumps = 0;
        for (TransferPSOParticle p : swarm) {
            totalJumps += p.getJumpCount();
        }
        System.out.printf("全部粒子累计跳槽次数 = %d%n%n", totalJumps);

        // =================================================================
        // 4) 结果 2：拿最优坐标真跑一次，得到"问题的解"
        // =================================================================
        // 这一步不能省：上一步拿到的只是"哪个配置最好"，不是解本身。
        OptimizationAlgorithm<double[], ContinuousProblem, ?> bestAlgorithm =
                new BranchAlgorithmBuilder(bestCoord).build(target, new Random(MetaPSOConfig.SEED));

        BestRecorder<double[]> finalRecorder = new BestRecorder<>();
        bestAlgorithm.solve(finalRecorder);

        System.out.println("--- 用最优配置真跑一次 ---");
        System.out.println("问题的解 = " + Arrays.toString(finalRecorder.getBestX()));
        System.out.printf("目标值   = %.10f%n", target.evaluate(finalRecorder.getBestX()));

        System.out.printf("%n耗时 %d ms%n", System.currentTimeMillis() - startTime);
    }

    // =====================================================================
    // 工具
    // =====================================================================

    /** 逐维打印坐标，带名字，便于对照 MetaPSOConfig 里的坐标布局。 */
    private static void printCoordinate(double[] coord) {
        int branch = MetaPSOConfig.branchOf(coord);
        System.out.println("--- 最优算法配置 ---");
        System.out.printf("算法种类 = %s%n", MetaPSOConfig.branchName(branch));
        for (int i = 0; i < coord.length; i++) {
            System.out.printf("  [%d] %-14s = %.6f%n", i, MetaPSOConfig.DIM_NAMES[i], coord[i]);
        }
        // 提示：只有属于当前分支的维是有意义的，其余维可以被忽略
        System.out.print("  本支实际生效的维：");
        for (int i : MetaPSOConfig.paramsOf(branch)) {
            System.out.print(MetaPSOConfig.DIM_NAMES[i] + " ");
        }
        System.out.println();
    }

    /**
     * 把 {@link TransferPSOParticle} 数组转成引擎要的类型。
     * <p>
     * 引擎构造器要的是 {@code Particle<double[],? super Prob,? super BasicPSOState<double[]>>[]}，
     * 而 Java 的泛型数组不支持直接创建带泛型参数的数组，只能造通配符数组再强转。
     */
    @SuppressWarnings("unchecked")
    private static Particle<double[], ParameterSpaceProblem, PSOState<double[]>>[] asParticleArray(
            TransferPSOParticle[] swarm) {
        Particle<double[], ParameterSpaceProblem, PSOState<double[]>>[] result =
                (Particle<double[], ParameterSpaceProblem, PSOState<double[]>>[])
                        new Particle<?, ?, ?>[swarm.length];
        for (int i = 0; i < swarm.length; i++) {
            result[i] = swarm[i];
        }
        return result;
    }
}
