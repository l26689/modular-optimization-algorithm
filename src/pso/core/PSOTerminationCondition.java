package pso.core;

import oa.api.problem.Problem;
import oa.api.spi.TerminationCondition;

/**
 * PSO 算法的终止条件接口。
 * <p>
 * 本接口继承 SPI 层的 {@link TerminationCondition}，将状态类型绑定为
 * {@link PSOState}。与 {@code SATerminationCondition} 不同，PSO 的
 * 终止条件不需要处理冷启动（PSO 状态始终包含完整的粒子位置信息）。
 *
 * <h3>实现方式</h3>
 * 终止条件通过 {@link PSOState#getCurrentXs()} 获取当前所有粒子位置，
 * 可基于以下策略判断：
 * <ul>
 *   <li>最大迭代次数（通过内部计数器统计调用次数）</li>
 *   <li>全局最优值收敛（连续若干轮无改进）</li>
 *   <li>粒子多样性低于阈值（所有粒子位置趋于一致）</li>
 * </ul>
 *
 * <h3>与通用终止条件的兼容性</h3>
 * 实现了 {@link TerminationCondition}{@code <X, Prob, PSOState<X>>} 的通用组件
 * 可直接作为 PSO 的终止条件使用，无需额外适配。
 * 例如 {@code oa.components.terminationcondition.MaxCallTerminationCondition}
 * 实现了 {@code TerminationCondition<X, Prob, ? super SAState<X>>}，
 * 由于 {@code PSOState} 和 {@code SAState} 均实现了 {@code State<X>}，
 * 该组件可同时用于 SA 和 PSO。
 *
 * @param <X>    解的表示类型（例如 {@code double[]}、{@code int[]}）
 * @param <Prob> 问题类型，必须是 {@link Problem} 的子类型
 *
 * @see TerminationCondition
 * @see PSOState
 */
public interface PSOTerminationCondition<X,Prob extends Problem<X>> extends TerminationCondition<X,Prob,PSOState<X>> {
}