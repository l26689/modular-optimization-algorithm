package moa.algorithm.ga.api;

import moa.framework.api.Component;
import moa.framework.problem.Problem;


/**
 * GA 种群算子基契约 —— 对种群执行整体操作。
 *
 * <p>实现 Component<X, Prob, S>。是否为最终类：否。
 *
 * <p>算法引擎调用 operate() 将当前种群状态变换为新一代种群。TotalPopOperator 继承此接口，添加算子管线元信息。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>安全性 —— 对 newPopulation 的写入必须通过 copyX() 创建独立副本。</li>
 * </ul>
 *
 * <p><b>参见文档 2</b>
 */
public interface PopOperator<X,Prob extends Problem<X>,S extends GAState<X>,Gene,G extends GeneType<X,Gene,Prob>> extends Component<X,Prob,S> {
    public void operate(S state, X[] newPopulation);
}
