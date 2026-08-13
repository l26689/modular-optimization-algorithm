package oa.components.terminationcondition;

import java.util.Random;

import oa.api.optimizationalgorithm.State;
import oa.api.problem.Problem;
import oa.api.spi.Reusable;
import oa.api.spi.TerminationCondition;

/**
 * 基于总调用次数的终止条件，在 {@link #check()} 被调用指定次数后停止搜索。
 * <p>
 * 本组件内部维护一个调用计数器，每次 {@code check()} 被调用时自增。
 * 当累计调用次数超过预设上限时，返回 {@code true} 通知主算法停止迭代。
 *
 * <h3>适用场景</h3>
 * 适用于需要严格控制目标函数评估总次数的场景，例如：
 * <ul>
 *   <li>计算资源受限时的公平算法对比（统一评估次数上限）；</li>
 *   <li>实时系统中需要严格的时间预算控制；</li>
 *   <li>与其他以评估次数为指标的终止条件组合使用。</li>
 * </ul>
 *
 * <h3>与迭代次数的区别</h3>
 * "总调用次数"不等同于"迭代次数"——某些算法可能在一次迭代中多次调用
 * {@code check()}（如内层循环和外层循环各检查一次），或一次迭代中不调用
 * {@code check()}。本组件以实际的 {@code check()} 调用次数为准，
 * 而非算法层面的迭代轮次。
 *
 * <h3>复用支持</h3>
 * 本类同时实现 {@link Reusable} 接口，通过 {@link #reset()} 可将计数器归零，
 * 使同一实例可用于多次独立的优化运行，无需重新创建。
 *
 * @param <X>    解的表示类型
 * @param <Prob> 问题类型，必须是 {@link Problem} 的子类型
 * @param <S>    状态类型，必须是 {@link State} 的子类型
 */
public class MaxCallTerminationCondition<X>
        implements TerminationCondition<X,Problem<X>,State<X>>, Reusable {

    private final int maxCalls;
    private int callCount;

    /**
     * 构造一个以总调用次数为上限的终止条件。
     *
     * @param maxCalls 允许的最大 {@code check()} 调用次数，必须为正整数
     * @throws IllegalArgumentException 如果 {@code maxCalls <= 0}
     */
    public MaxCallTerminationCondition(int maxCalls) {
        if (maxCalls <= 0) {
            throw new IllegalArgumentException("maxCalls 必须为正整数，当前值: " + maxCalls);
        }
        this.maxCalls = maxCalls;
        this.callCount = 0;
    }

    @Override
    public void init(Problem<X> prob, Random random) {
    }

    @Override
    public boolean check(State<X> state) {
        callCount++;
        return callCount > maxCalls;
    }

    @Override
    public void reset() {
        callCount = 0;
    }
}