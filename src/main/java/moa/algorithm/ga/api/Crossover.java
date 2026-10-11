package moa.algorithm.ga.api;

import moa.framework.problem.Problem;

/**
 * GA 交叉算子契约 —— 重组父代基因生成子代。
 *
 * <p>继承 PopOperator<X, Prob, S, Gene, G>。是否为最终类：否。
 *
 * <p>从 newPopulation 中读取父代个体，生成子代替换。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>可复现 —— 使用 init 注入的 Random 实例。</li>
 * </ul>
 *
 * <p><b>参见文档 2</b>
 */
public interface Crossover<X,Prob extends Problem<X>,S extends GAState<X>,Gene,G extends GeneType<X,Gene,Prob>> extends PopOperator<X,Prob,S,Gene,G> {
    
}
