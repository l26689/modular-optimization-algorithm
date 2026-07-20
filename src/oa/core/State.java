package oa.core;

/**
 * 优化算法迭代状态的基类封装。
 * <p>
 * 本类作为所有优化算法状态对象的抽象基类，仅持有当前解这一最基础的信息。
 * 具体算法（如模拟退火）可通过继承此类扩展额外的状态字段（如温度、接受标志等），
 * 同时保持对当前解的统一访问方式。
 *
 * <h3>不可变性</h3>
 * 本类的 {@code currentX} 字段为 {@code final}，创建后不可修改，
 * 确保状态对象在单次迭代内保持一致，避免意外修改。
 *
 * @param <X> 解的表示类型（例如 {@code double[]}、{@code int[]} 或自定义数据结构）
 */
public class State<X> {
    /** 当前解，由子类通过构造函数传入 */
    private final X currentX;

    /**
     * 构造一个优化算法的状态对象。
     *
     * @param currentX 当前解，不为 {@code null}
     */
    public State(X currentX) {
        this.currentX = currentX;
    }

    /**
     * 获取当前解。
     * <p>
     * 返回的解为只读状态，调用者不应直接修改其内容。
     * 若需要修改，应先通过 {@link Problem#copyX} 创建副本后再操作。
     *
     * @return 当前解
     */
    public X currentX() { return currentX; }
}