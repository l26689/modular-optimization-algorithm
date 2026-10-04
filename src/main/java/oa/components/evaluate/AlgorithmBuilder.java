package oa.components.evaluate;

import java.util.Random;

import oa.api.optimizationalgorithm.OptimizationAlgorithm;
import oa.api.problem.Problem;

@FunctionalInterface
public interface AlgorithmBuilder<X, Prob extends Problem<X>> {
    OptimizationAlgorithm<X, Prob, ?> build(Prob problem, Random random);
}