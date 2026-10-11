package moa.components.state;

import moa.framework.engine.State;

/**
 * 长生命周期状态实现 —— 通过替换引用实现零分配更新。
 *
 * <p>实现 State<X>。是否为最终类：是。
 *
 * <p>传入的数组引用被直接存储（不拷贝），调用方通过 set() 替换引用即可更新状态。适用于群体算法。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>安全性 —— 不进行防御性拷贝，数组内容为只读。组件不得保存 getCurrentXs() 返回的引用跨迭代使用。详见文档 5。</li>
 * </ul>
 *
 * <p><b>参见文档 5</b>
 */
public class LongLifeState<X> implements State<X> {
    private X[] currentPositions;

    /**
     * 构造一个 LongLife 状态对象。
     * <p>
     * 传入的数组引用被直接存储（不拷贝），调用方应确保传入后不再修改该数组。
     *
     * @param positions 所有粒子的当前位置数组，不为 {@code null}，长度至少为 1
     */
    public LongLifeState(X[] positions) {
        currentPositions = positions;
    }

    @Override
    public X[] getCurrentXs() {
        return currentPositions;
    }

    /**
     * 替换内部位置数组引用。
     * <p>
     * 不进行防御性拷贝，直接替换引用。调用方应确保传入后不再修改该数组，
     * 或在下轮迭代中通过交换数组引用实现零分配更新。
     *
     * @param positions 新的位置数组，不为 {@code null}
     */
    public void set(X[] positions) {
        currentPositions = positions;
    }
}
