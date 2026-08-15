package oa.api.optimizationalgorithm;


/**
 * 优化算法迭代状态的基类接口。
 * <p>
 * 本接口定义了访问当前解的统一契约：通过 {@link #getCurrentXs()} 获取当前解的数组。
 * 采用数组模式的原因：
 * <ul>
 *   <li><b>统一性</b>—— 无论是单解算法（SA）还是群体算法（PSO），
 *       都通过同一数组接口暴露解，组件无需区分访问模式。</li>
 *   <li><b>高性能</b>—— 避免了迭代器装箱开销，支持按索引直接访问解元素。</li>
 *   <li><b>简洁性</b>—— 遍历方式统一为 {@code for (X x : state.getCurrentXs())}，
 *       无需引入迭代器状态管理。</li>
 * </ul>
 *
 * <h3>SA 组件的特殊约定</h3>
 * {@code SAState} 的数组始终只包含一个元素（当前解），
 * 因此 SA 组件可直接通过 {@code getCurrentXs()[0]} 获取当前解。
 * 但编写同时支持 SA 和其他算法（如群体算法）的通用组件时，
 * 应使用 for-each 循环遍历数组，因为其他算法的数组可能包含多个元素。
 * <p>
 * 具体算法可通过实现此接口扩展额外的状态字段（如温度、接受标志等），
 * 同时保持对当前解的统一访问方式。
 *
 * <h3>使用约束</h3>
 * State 实例在求解过程中被复用，其内部状态（包括解引用）会随
 * 迭代推进而变化。各组件<b>不应</b>保存对 State 实例或其内部数组的引用
 * 跨迭代使用。如需持久化保存当前解，必须通过 {@link oa.api.problem.Problem#copyX}
 * 对解对象进行<b>深拷贝</b>，而非保存引用。
 *
 * @param <X> 解的表示类型（例如 {@code double[]}、{@code int[]} 或自定义数据结构）
 */
public interface State<X> {
    public X[] getCurrentXs();
}