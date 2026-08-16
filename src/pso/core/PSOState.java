package pso.core;

import oa.api.optimizationalgorithm.State;

/**
 * PSO 算法的迭代状态封装，是最少信息原则在群体算法中的核心体现。
 * <p>
 * 本类实现自 {@link State}，仅维护一个字段：所有粒子的当前位置数组。
 * 与 SA 的 {@code SAState}（额外包含温度、接受标志）不同，
 * PSO 的状态不需要温度或接受/拒绝信息——粒子速度更新是确定性的，
 * 搜索结果总是被接受。
 *
 * <h3>状态字段</h3>
 * {@code currentPositions} 是一个长度为粒子数的数组，其中
 * {@code currentPositions[i]} 对应第 i 个粒子的当前位置。
 * 通过 {@link #getCurrentXs()} 获取此数组的引用。
 *
 * <h3>最少信息原则</h3>
 * 本类刻意不包含以下信息，因为它们可由组件自行推导：
 * <ul>
 *   <li><b>全局最优（gBest）</b>——粒子可从 {@code getCurrentXs()} 中评估所有位置得出；</li>
 *   <li><b>个体最优（pBest）</b>——每个粒子内部维护自己的历史最优；</li>
 *   <li><b>粒子速度</b>——每个粒子内部维护自己的速度向量；</li>
 *   <li><b>目标函数值</b>——组件可通过持有的 {@code Problem} 引用自行评估。</li>
 * </ul>
 *
 * <h3>可变性</h3>
 * 本实例在求解过程中被复用：{@link #set(Object[])} 直接替换内部数组引用，
 * 不进行防御性拷贝。这是出于性能考虑（避免每轮迭代分配新数组），
 * 与 {@code SAState} 的行为一致。
 *
 * <h3>使用约束</h3>
 * <ul>
 *   <li>组件<b>不应</b>保存对 {@link #getCurrentXs()} 返回数组的引用跨迭代使用，
 *       因为该数组在下轮迭代中会被交换。</li>
 *   <li>{@link #getCurrentXs()} 返回的数组内容为只读——
 *       组件不应原地修改数组中的解。</li>
 *   <li>如需持久化保存当前解，必须通过 {@code Problem.copyX} 进行深拷贝。</li>
 * </ul>
 *
 * <h3>子类化</h3>
 * 本类不声明为 {@code final}，允许子类扩展额外字段（如迭代计数器、
 * 多样性度量等）。{@link pso.basicpso.BasicPSOState} 是本类的
 * {@code final} 子类，用于触发 JIT 去虚拟化优化。
 *
 * @param <X> 解的表示类型（例如 {@code double[]}、{@code int[]}）
 *
 * @see State
 * @see pso.basicpso.BasicPSOState
 */
public class PSOState<X> implements State<X> {
    private X[] currentPositions;

    /**
     * 构造一个 PSO 迭代状态对象。
     * <p>
     * 传入的数组引用被直接存储（不拷贝），调用方应确保传入后不再修改该数组。
     *
     * @param positions 所有粒子的当前位置数组，不为 {@code null}，长度至少为 1
     */
    public PSOState(X[] positions) {
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