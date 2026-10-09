package moa.algorithm.ga.api;

import moa.framework.problem.Problem;

public interface TotalPopOperator<X,Prob extends Problem<X>,S extends GAState<X>,Gene,G extends GeneType<X,Gene,Prob>> extends PopOperator<X,Prob,S,Gene,G> {

}
