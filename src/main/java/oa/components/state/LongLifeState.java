package oa.components.state;

import oa.api.optimizationalgorithm.State;

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
