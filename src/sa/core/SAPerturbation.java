package sa.core;

import java.util.Random;

import oa.api.problem.Problem;
import oa.api.spi.SearchOperator;
import sa.BasicSA.SimulatedAnnealing;

/**
 * 模拟退火扰动器，定义如何从当前解生成邻域候选解。
 * <p>
 * 本接口继承自 SPI 层的 {@link SearchOperator}，将 {@code search(SAState)}
 * 作为 SA 扰动操作的唯一入口，不再提供额外的别名方法。
 *
 * <h3>继承层次</h3>
 * <pre>
 *   {@link SearchOperator}{@code <X, Prob, SAState<X>>}  ← SPI 层通用接口
 *       └── SAPerturbation{@code <X, Prob>}               ← SA 层特化，无额外方法
 * </pre>
 * 实现类需重写 {@link SearchOperator#search(SAState)}。
 * 主算法（{@link SimulatedAnnealing}）通过 {@code perturbation.search(state)} 调用。
 *
 * <h3>SA 扰动特点：无倾向的随机扰动</h3>
 * 与其他搜索算子的一个重要区别是：SA 的扰动通常应当是<b>无倾向的</b>（unbiased）。
 * 即候选解在邻域中的分布应具有对称性，不偏向任何特定方向——因为 SA 依赖
 * Metropolis 准则来引导搜索，而非依赖扰动本身的方向性。如果扰动带有系统性偏差，
 * 会干扰 Metropolis 准则的概率平衡，破坏算法的理论收敛性保证。
 * <p>
 * 实现时应确保扰动在邻域内均匀或对称分布（如均匀扰动、高斯扰动等），
 * 避免使用梯度下降、动量等有向搜索策略——这些属于爬山法或 PSO 的范畴。
 *
 * <h3>冷启动约定</h3>
 * 首次调用时，{@code isAccepted} 固定为 {@code false}。这并非一次真实的接受事件，
 * 仅表示"尚无历史记录"。实现类应将此视为冷启动信号，采用默认的扰动幅度，
 * 不应依赖该标志做出自适应调整。
 *
 * <h3>实现要求</h3>
 * <ul>
 *   <li>必须返回全新创建的对象，不得原地修改输入解。</li>
 *   <li>若需要自适应扰动（如根据接受率调整步长），
 *   可在类内部维护私有状态（如滑动窗口记录历史接受情况）。</li>
 *   <li>扰动幅度或策略可自由设计，本接口不做任何硬性约束（例如不强制步长必须为正）。</li>
 * </ul>
 *
 * <h3>最少信息原则</h3>
 * 本组件仅接收主算法维护的原子信息：当前温度、当前解、上一次接受标志。
 * 目标函数值、改进幅度等冗余数据不在参数中，
 * 若需要可自行通过持有的 {@link Problem#evaluate} 获取。
 *
 * <h3>通配符兼容性</h3>
 * {@link SimulatedAnnealing} 构造函数以 {@code SAPerturbation<X, ? super Prob>}
 * 的形式接收本组件（下界通配符）。这意味着实现类可以将 {@code Prob} 声明为
 * 比实际使用的问题类型更泛化的父类型。例如，一个仅通过 {@link Problem#copyX}
 * 和边界信息工作的扰动器，可以声明为 {@code SAPerturbation<double[], Problem<double[]>>}，
 * 并被传入 {@code SimulatedAnnealing<double[]>} 中。
 *
 * @param <X>   解的表示类型（例如 {@code double[]}）
 * @param <Prob> 问题类型，必须实现 {@link Problem}{@code <X>}
 */
public interface SAPerturbation<X, Prob extends Problem<X>> extends SearchOperator<X, Prob, SAState<X>> {

    /**
     * 绑定问题实例，使扰动器获取问题的维度、边界等元数据。
     * <p>
     * 此方法由框架在构造阶段自动调用，使用者无需手动处理。
     *
     * @param problem 待求解问题
     * @param random 随机数生成器，由主算法统一创建并注入，组件应使用此实例进行所有随机操作
     *               （如生成随机扰动、随机选择维度等），以保证结果可复现；不应自行创建新的 {@link Random} 实例
     * @throws NullPointerException 如果 problem 为 null
     */
    void init(Prob problem, Random random);

    /**
     * 生成当前解的一个邻域候选解。
     * <p>
     * 继承自 {@link SearchOperator#search(oa.api.optimizationalgorithm.State)}，
     * 算法每次迭代会调用本方法一次，传入封装了当前迭代状态的 {@link SAState} 对象：
     * <ul>
     *   <li>{@code state.getCurrentXs()[0]} - 当前解（只读，不可原地修改）</li>
     *   <li>{@code state.getTemperature()} - 当前系统温度（可用于控制扰动幅度）</li>
     *   <li>{@code state.getIsAccepted()} - 上一轮迭代的接受结果；首次迭代时为 {@code false}，
     *       表示"尚无历史"，实现应使用默认扰动强度</li>
     * </ul>
     *
     * <p><b>SA 特别提醒：</b>SA 的扰动应是无倾向的随机扰动（unbiased random perturbation）。
     * 候选解在邻域中的分布应具有对称性，不偏向任何特定方向。
     * 避免使用梯度下降、动量等有向策略——这些属于爬山法或 PSO 的范畴。
     *
     * @param state 封装了当前迭代状态的 {@link SAState} 对象，包含当前解、温度和接受标志
     * @return 全新的候选解，必须与 {@code state.getCurrentXs()[0]} 相互独立
     */
    X search(SAState<X> state);
}