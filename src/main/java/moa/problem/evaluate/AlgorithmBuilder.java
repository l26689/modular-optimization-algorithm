package moa.problem.evaluate;

import java.util.Random;

import moa.framework.engine.OptimizationAlgorithm;
import moa.framework.problem.Problem;

public interface AlgorithmBuilder<X, Prob extends Problem<X>> {
    OptimizationAlgorithm<X, Prob, ?> build(Prob problem, Random random);
}