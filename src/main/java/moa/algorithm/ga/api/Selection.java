package moa.algorithm.ga.api;

import moa.framework.problem.Problem;

/**
 * GA 选择算子契约 —— 从父代种群选择个体。
 *
 * <p>继承 PopOperator<X, Prob, S, Gene, G>。是否为最终类：否。
 *
 * <p>将选出的个体写入 newPopulation，通常基于适应度排名。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>可复现 —— 使用 init 注入的 Random 实例。</li>
 * </ul>
 *
 * <p><b>参见文档 2</b>
 */
public interface Selection<X,Prob extends Problem<X>,S extends GAState<X>,Gene,G extends GeneType<X,Gene,Prob>> extends PopOperator<X,Prob,S,Gene,G> {
}