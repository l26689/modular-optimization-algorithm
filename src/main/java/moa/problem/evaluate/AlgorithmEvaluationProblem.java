package moa.problem.evaluate;

import java.util.Random;

import moa.components.recorder.BestRecorder;
import moa.framework.engine.OptimizationAlgorithm;
import moa.framework.problem.Evaluable;
import moa.framework.problem.Problem;

/**
 * 算法评估问题 —— 将算法本身作为优化对象。
 *
 * <p>实现 Problem<AlgorithmBuilder> 和 Evaluable<AlgorithmBuilder, Double>。是否为最终类：否。
 *
 * <p>以固定种子运行多轮，返回平均性能。用于超参数调优和算法选型。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>可复现 —— 按固定 SEED 预先生成种子数组。</li>
 * </ul>
 *
 * <p><b>参见文档 6</b>
 */
public class AlgorithmEvaluationProblem<X, Prob extends Problem<X> & Evaluable<X, Double>>
        implements Problem<AlgorithmBuilder<X, Prob>>, Evaluable<AlgorithmBuilder<X, Prob>, Double> {

    public static final long SEED = 0;
    private final Prob problem;
    private final int runs;
    private final long[] seeds;

    public AlgorithmEvaluationProblem(Prob problem, int runs) {
        this.problem = problem;
        this.runs = runs;
        Random r = new Random(SEED);
        this.seeds = new long[runs];
        for (int i = 0; i < runs; i++) {
            seeds[i] = r.nextLong();
        }
    }

    @Override
    public Double evaluate(AlgorithmBuilder<X, Prob> builder) {
        double sum = 0.0;
        for (int i = 0; i < runs; i++) {
            OptimizationAlgorithm<X, Prob, ?> algo = builder.build(problem, new Random(seeds[i]));
            BestRecorder<X> recorder = new BestRecorder<>();
            algo.solve(recorder);
            X bestX = recorder.getBestX();
            sum += problem.evaluate(bestX);
        }
        return sum / runs;
    }

    @Override
    public AlgorithmBuilder<X, Prob> copyX(AlgorithmBuilder<X, Prob> x) {
        return x;
    }

    @Override
    public double compare(AlgorithmBuilder<X, Prob> b1, AlgorithmBuilder<X, Prob> b2) {
        double v1 = evaluate(b1);
        double v2 = evaluate(b2);
        return v2 - v1;
    }
}