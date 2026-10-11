package moa.algorithm.ga.api;

import moa.framework.problem.Problem;

/**
 * GA 变异算子契约 —— 对子代个体施加随机扰动。
 *
 * <p>继承 PopOperator<X, Prob, S, Gene, G>。是否为最终类：否。
 *
 * <p>以指定概率翻转种群中个体的基因位，增加种群多样性。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>可复现 —— 使用 init 注入的 Random 实例。</li>
 * </ul>
 *
 * <p><b>参见文档 2</b>
 */
public interface Mutation<X,Prob extends Problem<X>,S extends GAState<X>,Gene,G extends GeneType<X,Gene,Prob>> extends PopOperator<X,Prob,S,Gene,G> {
    
}
