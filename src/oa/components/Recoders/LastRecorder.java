package oa.components.Recoders;

import java.util.Random;

import oa.api.optimizationalgorithm.State;

import oa.api.problem.Problem;
import oa.api.spi.Recorder;

/**
 * 最近解记录器，保存最后一次记录的状态中的最后一个解。
 * <p>
 * 本类仅在 {@link #record(State)} 中保存状态解数组末尾的解的引用，
 * 不进行拷贝。在 {@link #getLastX()} 时通过 {@link Problem#copyX(Object)}
 * 返回防御性副本，确保调用方获得独立的解对象。
 *
 * <h3>生命周期</h3>
 * <ol>
 *   <li>构造：无参构造，不绑定任何 Problem</li>
 *   <li>{@link #init(Problem, Random)}：由算法在 {@code solve()} 入口调用，
 *       注入 Problem 引用（用于 {@code copyX()}）</li>
 *   <li>{@link #record(State)}：由算法在每次迭代后调用，保存末尾解引用</li>
 *   <li>{@link #getLastX()}：在 {@code solve()} 返回后由调用方获取最近解</li>
 * </ol>
 *
 * <h3>使用场景</h3>
 * 适用于只需要最终解、不关心历史轨迹的简单场景。
 * 由于 SA 的 {@code SAState} 数组仅含一个元素，该元素即为最近接受的解。
 *
 * @param <X> 解的表示类型
 * @param <Y> 目标值类型（保留参数，供未来扩展，当前未使用）
 */
public class LastRecorder<X,Y> implements Recorder<X,Problem<X>,State<X>> {
    private X lastX;
    private Problem<X> prob;

    /**
     * 记录状态，保存末尾解的引用。
     *
     * @param state 当前算法状态
     */
    @Override
    public void record(State<X> state) {
        X[] currentXs = state.getCurrentXs();
        lastX = currentXs[currentXs.length - 1];
    }

    /**
     * 获取最近一次记录的解（防御性拷贝）。
     *
     * @return 最近解的独立副本
     */
    public X getLastX() {
        return prob.copyX(lastX);
    }

    /**
     * 绑定问题实例。
     * <p>
     * 由算法在 {@code solve()} 入口调用，不由此类的使用者直接调用。
     *
     * @param prob   待优化问题，用于 {@code copyX()} 创建防御性副本
     * @param random 随机数生成器（此实现不使用）
     */
    @Override
    public void init(Problem<X> prob, Random random) {
        this.prob = prob;
    }
}