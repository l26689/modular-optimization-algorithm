package moa.problem.evaluate;

import java.util.Random;

import moa.components.recorder.BestRecorder;
import moa.framework.engine.OptimizationAlgorithm;
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

    @Override
    public double compare(AlgorithmBuilder<X, Prob> b1, AlgorithmBuilder<X, Prob> b2) {
        double v1 = evaluate(b1);
        double v2 = evaluate(b2);
        return v2 - v1;
    }
}