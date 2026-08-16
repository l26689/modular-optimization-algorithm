package pso.basicpso;

import pso.core.PSOState;

/**
 * 基础 PSO 状态类，是 {@link PSOState} 的 {@code final} 子类。
 * <p>
 * 本类不添加任何新字段或方法，其唯一目的是通过 {@code final} 修饰
 * 触发 JIT 编译器的<b>类层次分析（CHA）去虚拟化</b>优化。
 * 当 JIT 确定一个方法调用的接收者类型为 {@code final} 类时，
 * 可以将虚方法调用内联为直接调用，消除方法派发开销。
 *
 * <h3>为什么需要这个类</h3>
 * {@link PSOState} 不能声明为 {@code final}，因为用户可能需要
 * 扩展它来添加自定义字段（如迭代计数器、多样性度量等）。
 * 但在 {@link ParticleSwarmOptimization} 这类通用算法中，
 * 状态类型已知不会被子类化，使用 {@code BasicPSOState} 即可
 * 获得去虚拟化优化。
 *
 * <h3>使用方式</h3>
 * 通常不需要手动创建本类实例——{@link ParticleSwarmOptimization}
 * 内部自动使用。如果用户自定义了 {@link PSOState} 的子类，
 * 则无法使用 {@link ParticleSwarmOptimization}，需自行实现
 * 算法主循环。
 *
 * @param <X> 解的表示类型
 *
 * @see PSOState
 * @see ParticleSwarmOptimization
 */
public final class BasicPSOState<X> extends PSOState<X> {

    /**
     * 构造一个基础 PSO 状态对象。
     *
     * @param positions 所有粒子的当前位置数组，不为 {@code null}，长度至少为 1
     */
    public BasicPSOState(X[] positions) {
        super(positions);
    }

}