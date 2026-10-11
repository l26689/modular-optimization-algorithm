package moa.algorithm.ga.api;

import moa.framework.problem.Problem;

/**
 * GA 替换算子契约 —— 用子代替换父代形成新一代种群。
 *
 * <p>继承 PopOperator<X, Prob, S, Gene, G>。是否为最终类：否。
 *
 * <p>合并父代与子代种群，按策略选择下一代个体。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>安全性 —— 写入 newPopulation 时必须通过 copyX() 深拷贝。</li>
 * </ul>
 *
 * <p><b>参见文档 2</b>
 */
public interface Replacement<X,Prob extends Problem<X>,S extends GAState<X>,Gene,G extends GeneType<X,Gene,Prob>> extends PopOperator<X,Prob,S,Gene,G> {
    
}
