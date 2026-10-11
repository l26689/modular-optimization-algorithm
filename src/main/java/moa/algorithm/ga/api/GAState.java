package moa.algorithm.ga.api;

import moa.framework.engine.State;

/**
 * GA 状态契约 —— 遗传算法种群状态的统一抽象。
 *
 * <p>继承 State<X>。是否为最终类：否。
 *
 * <p>通过 getCurrentXs() 返回当前种群中所有个体。无额外方法，保持与框架层 State 的最小接口兼容。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>安全性 —— 实例在迭代间复用，组件不得持有引用跨迭代使用。详见文档 5。</li>
 * </ul>
 *
 * <p><b>参见文档 5</b>
 */
public interface GAState<X> extends State<X> {
}
