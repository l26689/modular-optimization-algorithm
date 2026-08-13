package oa.api.optimizationalgorithm;

import java.util.Iterator;

import oa.api.problem.Problem;

/**
 * 优化算法迭代状态的基类接口。
 * <p>
 * 本接口定义了访问当前解的统一契约，提供两种访问模式：
 * <ul>
 *   <li><b>迭代器模式</b>（通用）—— 通过 {@link #getCurrentXIterator()} 获取迭代器，
 *       常规用法为 {@code while (it.hasNext()) { X x = it.next(); }}，
 *       适用于所有解的表示类型。迭代器返回的元素个数取决于具体实现：
 *       SA 的 {@code SAState} 将整个 {@code X} 作为单一元素返回，
 *       群体算法可将每个个体作为独立元素暴露。</li>
 *   <li><b>数组模式</b>（高性能）—— 通过 {@link #getCurrentXs()} 直接获取解数组，
 *       仅在 {@link #isArraySupported()} 返回 {@code true} 时可用。
 *       适合需要频繁按索引访问解元素的场景，避免了迭代器装箱开销。</li>
 * </ul>
 *
 * <h3>SA 组件的特殊约定</h3>
 * {@code SAState} 的迭代器始终只包含一个元素（当前解），且每次迭代创建新实例，
 * 因此 SA 组件可直接调用 {@code getCurrentXIterator().next()} 获取当前解，
 * 无需 {@code hasNext()} 循环。但编写同时支持 SA 和其他算法（如群体算法）的
 * 通用组件时，必须按迭代器标准方式遍历，因为其他算法的迭代器可能包含多个元素。
 * <p>
 * 具体算法可通过继承此接口扩展额外的状态字段（如温度、接受标志等），
 * 同时保持对当前解的统一访问方式。
 * 如需修改或保存当前解，应先通过 {@link Problem#copyX} 创建副本后再操作。
 *
 * @param <X> 解的表示类型（例如 {@code double[]}、{@code int[]} 或自定义数据结构）
 */
public interface State<X> {
    public Iterator<X> getCurrentXIterator();

    public X[] getCurrentXs();

    public boolean isArraySupported();
}