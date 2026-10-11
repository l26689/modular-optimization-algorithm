package moa.algorithm.ga.engine;

import moa.algorithm.ga.api.GAState;
import moa.components.state.LongLifeState;


/**
 * 基础 GA 状态实现 —— GAState 的 final 子类。
 *
 * <p>继承 LongLifeState<X>，实现 GAState<X,Gene,G>。是否为最终类：是。
 *
 * <p>无额外字段，仅作为 final 包装类触发 JIT 去虚拟化优化。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>安全性 —— 不进行防御性拷贝。详见文档 5。</li>
 * </ul>
 *
 * <p><b>参见文档 5</b>
 */
public final class BasicGAState<X> extends LongLifeState<X> implements GAState<X> {

    public BasicGAState(X[] population) {
        super(population);
    }
}
