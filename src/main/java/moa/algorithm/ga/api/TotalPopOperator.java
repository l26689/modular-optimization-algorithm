package moa.algorithm.ga.api;

import moa.framework.problem.Problem;

/**
 * GA 全种群算子契约 —— 组合所有子算子为一个整体操作。
 *
 * <p>继承 PopOperator<X, Prob, S, Gene, G>。是否为最终类：否。
 *
 * <p>内部按序调用 Selection -> Crossover -> Mutation -> Replacement。额外提供基因类型信息供算法引擎进行编译期校验。
 *
 * <p><b>参见文档 2</b>
 */
public interface TotalPopOperator<X,Prob extends Problem<X>,S extends GAState<X>,Gene,G extends GeneType<X,Gene,Prob>> extends PopOperator<X,Prob,S,Gene,G> {

}
