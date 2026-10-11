package moa.algorithm.ga.api;

import moa.framework.api.Component;
import moa.framework.problem.Problem;

/**
 * GA 基因翻译器契约 —— 负责解的编码与解码。
 *
 * <p>实现 Component<X, Prob, State<X>>。是否为最终类：否。
 *
 * <p>将算法内部基因表示与问题层解表示相互转换，是 GA 特有的抽象层。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>纯函数 —— encode() 和 decode() 应为纯函数，无副作用。</li>
 * </ul>
 *
 * <p><b>参见文档 2</b>
 */
public interface GeneType<X,Gene,Prob extends Problem<X>> extends Component<X,Prob,GAState<X>> {
    public Gene[] encode(X individual);

    public X decode(Gene[] gene);

    public Gene[] copyGene(Gene[] gene);

    public double fitness(X individual);
}
