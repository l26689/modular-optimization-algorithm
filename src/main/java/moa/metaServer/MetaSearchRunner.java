package moa.metaServer;

import java.util.Random;

import moa.algorithm.pso.api.PSOState;
import moa.algorithm.pso.api.Particle;
import moa.algorithm.pso.engine.ParticleSwarmOptimization;
import moa.components.recorder.BestRecorder;
import moa.components.terminator.MaxCallTerminationCondition;
import moa.framework.engine.OptimizationAlgorithm;
import moa.meta.BranchAlgorithmBuilder;
import moa.meta.MetaPSOConfig;
import moa.meta.ParameterSpaceProblem;
import moa.meta.TransferPSOParticle;
import moa.problem.continuous.ContinuousProblem;
import moa.problem.evaluate.AlgorithmEvaluationProblem;

/**
 * 元搜索门面：给定一个目标问题，用 {@code moa.meta} 的伪 PSO 跨算法搜索
 * 找出最优的「算法 + 参数组合」，并返回该组合真正求出的解。
 * <p>
 * 本类是 {@code moa.meta.MetaPSOCombination.main} 的<b>参数化版本</b>——逻辑照搬，
 * 只是把硬编码的 {@code SphereProblem} 换成调用方传入的 {@code target}。
 * 供 {@link WebServer} 的 {@code /api/search} 调用。
 */
public final class MetaSearchRunner {

    private MetaSearchRunner() {
    }

    /**
     * 一次元搜索的结果。
     *
     * @param branch      最优算法支的名字（SA / PSO / DE）
     * @param coord       最优配置坐标（8 维，下标含义见 MetaPSOConfig.DIM_NAMES）
     * @param dimNames    坐标各维的名字（和 coord 等长，前端逐项展示用）
     * @param avgCost     该配置的平均表现（越小越优）
     * @param bestX       用该配置真跑一次得到的问题的解
     * @param evalCount   搜索期间实际评估过的不同配置数
     * @param elapsedMs   总耗时（毫秒）
     */
    public record SearchResult(
            String branch,
            double[] coord,
            String[] dimNames,
            double avgCost,
            double[] bestX,
            int evalCount,
            long elapsedMs) {
    }

    /**
     * 跑一次元搜索。
     *
     * @param target         目标问题（预设的 ContinuousProblem 或 RuntimeProblem）
     * @param metaIterations 外层元 PSO 的迭代轮数
     * @param swarmSize      外层粒子数
     * @param innerBudget    内层算法的迭代轮数（解目标问题时跑多少轮）
     * @return 搜索结果
     */
    public static SearchResult run(ContinuousProblem target, int metaIterations, int swarmSize, int innerBudget) {
        long start = System.currentTimeMillis();

        // 内层评估器：对每组配置跑 RUNS 遍目标问题，取平均
        AlgorithmEvaluationProblem<double[], ContinuousProblem> inner =
                new AlgorithmEvaluationProblem<>(target, MetaPSOConfig.RUNS);

        // 满意度函数：坐标 -> 该配置的平均表现（带缓存）
        ParameterSpaceProblem space = new ParameterSpaceProblem(inner, innerBudget);

        // 外层伪 PSO：粒子在配置空间飞，可跨算法跳槽
        TransferPSOParticle[] swarm = new TransferPSOParticle[swarmSize];
        for (int i = 0; i < swarmSize; i++) {
            swarm[i] = new TransferPSOParticle();
        }

        ParticleSwarmOptimization<double[], ParameterSpaceProblem> metaPso =
                new ParticleSwarmOptimization<double[], ParameterSpaceProblem>(
                        new Random(MetaPSOConfig.SEED),
                        space,
                        asParticleArray(swarm),
                        new MaxCallTerminationCondition<double[]>(metaIterations));

        BestRecorder<double[]> recorder = new BestRecorder<>();
        metaPso.solve(recorder);

        // 结果 1：最优配置坐标（注意：这是坐标，不是问题的解）
        double[] bestCoord = recorder.getBestX();

        // 结果 2：拿最优坐标真跑一次，得到"问题的解"
        OptimizationAlgorithm<double[], ContinuousProblem, ?> bestAlgorithm =
                new BranchAlgorithmBuilder(bestCoord, innerBudget).build(target, new Random(MetaPSOConfig.SEED));
        BestRecorder<double[]> finalRecorder = new BestRecorder<>();
        bestAlgorithm.solve(finalRecorder);

        return new SearchResult(
                MetaPSOConfig.branchName(MetaPSOConfig.branchOf(bestCoord)),
                bestCoord,
                MetaPSOConfig.DIM_NAMES,
                space.evaluate(bestCoord),
                finalRecorder.getBestX(),
                space.cacheSize(),
                System.currentTimeMillis() - start);
    }

    /** 把具体粒子数组转成引擎要的接口数组（泛型数组创建的强制转换）。 */
    @SuppressWarnings("unchecked")
    private static Particle<double[], ParameterSpaceProblem, PSOState<double[]>>[] asParticleArray(
            TransferPSOParticle[] swarm) {
        Particle<double[], ParameterSpaceProblem, PSOState<double[]>>[] result =
                (Particle<double[], ParameterSpaceProblem, PSOState<double[]>>[]) new Particle<?, ?, ?>[swarm.length];
        for (int i = 0; i < swarm.length; i++) {
            result[i] = swarm[i];
        }
        return result;
    }
}
