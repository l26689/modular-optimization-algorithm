package oa.api;

import java.util.Iterator;

/**
 * 优化算法迭代状态的基类封装。
 * <p>
 * 本类作为所有优化算法状态对象的抽象基类，仅持有当前解这一最基础的信息。
 * 具体算法（如模拟退火）可通过继承此类扩展额外的状态字段（如温度、接受标志等），
 * 同时保持对当前解的统一访问方式。
 * 访问解时，默认使用 {@link #currentXIterator} 迭代器，
 * 如有性能需求要直接访问数组元素，可设置 {@link #isArraySupported} 为 {@code true}，
 * 并通过 {@link #currentXs} 访问数组。
 * 如需修改或保存当前解，应先通过 {@link Problem#copyX} 创建副本后再操作。
 *
 * @param <X> 解的表示类型（例如 {@code double[]}、{@code int[]} 或自定义数据结构）
 */
public interface State<X> {
    public Iterator<X> getCurrentXIterator();

    public X[] getCurrentXs();

    public boolean isArraySupported();
}