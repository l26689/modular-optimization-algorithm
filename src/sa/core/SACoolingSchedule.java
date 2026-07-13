package sa.core;

import oa.Problem;

/**
 * 冷却调度策略，定义温度如何随迭代逐步降低。
 * <p>
 * 本组件在每一次迭代结束后被调用（注意：本框架不区分“外循环”和“内循环”，
 * 每个温度下仅产生一个候选解，因此 {@link #cool} 的调用次数即为算法总迭代次数）。
 * 实现类可基于当前温度、当前解和上一轮接受状态自适应调整降温速度，
 * 但最简单的几何冷却（每次乘以一个略小于1的常数）通常已足够使用。
 *
 * <h3>最少信息原则</h3>
 * 本组件仅接收主算法维护的原子信息：温度、当前解、上一次接受标志。
 * 目标函数值、改进幅度等冗余数据不在参数中，若需要可自行通过持有的
 * {@link Problem#evaluate} 获取。
 *
 * @param <X>   解的表示类型（例如 {@code double[]}）
 * @param <Prob> 问题类型，必须实现 {@link Problem}{@code <X>}
 */
public abstract class SACoolingSchedule<X, Prob extends Problem<X>> {

    /**
     * 绑定问题实例，使冷却策略可获取问题的维度等元数据（多数冷却策略无需此信息，
     * 但作为组件统一契约保留）。
     * <p>
     * 此方法由框架在构造阶段自动调用，使用者无需手动处理。
     *
     * @param problem 待求解问题
     * @throws NullPointerException 如果 problem 为 null
     */
    protected abstract void init(Prob problem);

    /**
     * 计算下一轮的系统温度。
     * <p>
     * 本方法每次迭代后调用一次，调用次数等于算法总迭代次数（非传统意义上的外循环）。
     * 传入的 {@code isAccepted} 反映的是刚结束的本次迭代的接受结果，其值始终真实
     * （包括首次调用时），实现可直接据此调整温度。
     *
     * <h3>关于温度约束</h3>
     * 经典实现通常使温度单调不增并保持正数，但本接口不强制此约定。
     * 需要“回火”甚至非传统温度策略的实现类可以自由决定温度值，
     * 只需确保返回值与自身设计的冷却逻辑自洽即可。
     *
     * @param temperature 当前温度
     * @param x           当前解（只读，不可原地修改）
     * @param isAccepted  刚结束的本次迭代的接受结果
     * @return 新的温度值，其合理性由实现类自行保证
     */
    protected abstract double cool(SAState<X> state);
}