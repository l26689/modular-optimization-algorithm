package moa.algorithm.sa.engine;

import moa.algorithm.sa.api.SAState;

/**
 * BasicSA 的内置状态实现 —— SAState 的 final 子类。
 *
 * <p>继承 SAState<X>。是否为最终类：是。
 *
 * <p>无额外字段或逻辑，作为 final 类触发 JIT 去虚拟化优化。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>安全性 —— 实例在迭代间复用，组件不得保存引用跨迭代使用。详见文档 5。</li>
 * </ul>
 *
 * <p><b>参见文档 5</b>
 */
public final class BasicSAState<X> extends SAState<X> {

    public  BasicSAState(X currentX, double temperature, boolean isAccepted) {
        super(currentX, temperature, isAccepted);
    }
    
}
