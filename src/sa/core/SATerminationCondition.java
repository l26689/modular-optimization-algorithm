package sa.core;

import java.util.Random;

import oa.api.problem.Problem;
import oa.api.spi.TerminationCondition;
import sa.BasicSA.SimulatedAnnealing;

/**
 * SA 终止条件接口，判断模拟退火算法何时停止迭代。
 * <p>
 * 本接口继承自 SPI 层的通用 {@link TerminationCondition}，将状态类型绑定为
 * {@link SAState}，使实现类可以直接通过 {@code SAState} 获取温度、接受标志等
 * SA 特有的状态信息，而无需自行向下转型。
 *
 * <h3>冷启动约定</h3>
 * 首次调用 {@link #check(SAState)} 时，
 * {@code isAccepted} 固定为 {@code false}。这并非一次真实的接受事件，
 * 仅表示"尚无历史记录"。实现类不应据此决定是否终止。
 *
 * <h3>实现要求</h3>
 * <ul>
 *   <li>{@code check} 必须是幂等的——多次调用不应产生副作用。</li>
 *   <li>返回 {@code true} 后，组件应能通过 {@link oa.api.spi.Reusable#reset()} 重置状态。</li>
 *   <li>终止条件应尽可能轻量，避免在 {@code check} 中执行耗时的目标函数评估。</li>
 * </ul>
 *
 * <h3>最少信息原则</h3>
 * 本组件仅接收主算法维护的原子信息：当前温度、当前解、上一次接受标志。
 * 目标函数值、迭代次数等可推导信息不在参数中，
 * 若需要可自行通过内部计数器推导。
 *
 * <h3>通配符兼容性</h3>
 * {@link SimulatedAnnealing} 构造函数以 {@code SATerminationCondition<X, ? super Prob>}
 * 的形式接收本组件（下界通配符）。这意味着实现类可以将 {@code Prob} 声明为
 * 比实际使用的问题类型更泛化的父类型。例如，一个仅依赖调用次数的终止条件
 * 可以声明为 {@code SATerminationCondition<double[], Problem<double[]>>}，
 * 并被传入 {@code SimulatedAnnealing<double[]>} 中。
 *
 * @param <X>   解的表示类型（例如 {@code double[]}）
 * @param <Prob> 问题类型，必须实现 {@link Problem}{@code <X>}
 */
public interface SATerminationCondition<X, Prob extends Problem<X>>
        extends TerminationCondition<X, Prob, SAState<X>> {

    /**
     * 绑定问题实例，使终止条件可获取问题的维度等元数据（多数终止条件无需此信息，
     * 但作为组件统一契约保留）。
     * <p>
     * 此方法由框架在构造阶段自动调用，使用者无需手动处理。
     *
     * @param problem 待求解问题
     * @param random 随机数生成器，由主算法统一创建并注入，组件应使用此实例进行所有随机操作
     *               （如随机终止策略），以保证结果可复现；不应自行创建新的 {@link Random} 实例
     * @throws NullPointerException 如果 problem 为 null
     */
    @Override
    void init(Prob problem, Random random);

    /**
     * 检查当前是否满足终止条件。
     * <p>
     * 由主算法在每次迭代末尾调用一次。传入的 {@link SAState} 封装了当前迭代的
     * 关键状态信息：
     * <ul>
     *   <li>{@code state.getCurrentXs()[0]} - 当前解（只读，不可原地修改）</li>
 *   <li>{@code state.getTemperature()} - 当前温度</li>
 *   <li>{@code state.getIsAccepted()} - 刚结束的本次迭代的接受结果；
 *       首次调用时为 {@code false}（冷启动），不应据此决定是否终止</li>
     * </ul>
     *
     * @param state 当前迭代状态，包含当前解、温度和接受标志
     * @return {@code true} 表示满足终止条件，搜索应停止；
     *         {@code false} 表示继续搜索
     */
    @Override
    boolean check(SAState<X> state);
}