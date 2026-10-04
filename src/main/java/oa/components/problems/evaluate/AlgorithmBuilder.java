package oa.components.problems.evaluate;

import java.util.Random;

import oa.api.optimizationalgorithm.OptimizationAlgorithm;
import oa.api.problem.Problem;

public interface AlgorithmBuilder<X, Prob extends Problem<X>> {
    OptimizationAlgorithm<X, Prob, ?> build(Prob problem, Random random);
}