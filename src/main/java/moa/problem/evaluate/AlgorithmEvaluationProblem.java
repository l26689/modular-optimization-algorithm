package moa.problem.evaluate;

import java.util.Random;

import moa.components.recorder.BestRecorder;
import moa.framework.engine.OptimizationAlgorithm;
import moa.framework.problem.Comparision;
import moa.framework.problem.Evaluable;
import moa.framework.problem.Problem;

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

    /**
     * 比较两个待评估的算法配置。
     * <p>
     * 语义与 {@link moa.problem.continuous.ContinuousProblem#compare} 保持一致：
     * {@code GREATER} = b1 更优（平均表现更小），{@code LESS} = b1 更劣。
     * 这个方向必须和 BestRecorder / StandardPSOParticle / DEParticle 等调用方一致，
     * 否则整个搜索会去追"最差"的配置。
     */
    @Override
    public Comparision compare(AlgorithmBuilder<X, Prob> b1, AlgorithmBuilder<X, Prob> b2) {
        double v1 = evaluate(b1);
        double v2 = evaluate(b2);
        if (v1 < v2) {
            return new Comparision(Comparision.Relation.GREATER, v2 - v1);
        }
        else if (v1 > v2) {
            return new Comparision(Comparision.Relation.LESS, v1 - v2);
        }
        return new Comparision(Comparision.Relation.EQUAL, 0.0);
    }
}