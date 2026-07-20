package sa.core;

import java.util.Random;

import oa.core.Problem;

/**
 * 初始化器，负责定义算法的起始状态：初始解和初始温度。
 * <p>
 * 在模拟退火正式开始前，主算法会通过本组件获取搜索的起点和初始温度。
 * 实现类应通过 {@link #init(Problem, Random)} 获取问题的维度、边界等元数据，
 * 以便在后续调用 {@link #initialX()} 和 {@link #initialTemperature()} 时使用。
 *
 * <h3>最少信息原则</h3>
 * 初始化器只接收问题实例本身（通过 {@code init}），不依赖任何迭代状态。
 * 初始解和初始温度的生成逻辑完全封装在子类内部，主算法只需调用即可。
 *
 * <h3>实现注意事项</h3>
 * <ul>
 *   <li>{@link #initialX()} 返回的解必须是独立新对象，避免后续迭代修改影响其他组件。</li>
 *   <li>{@link #initialTemperature()} 应返回一个足够高的温度值，使得算法早期能以较大概率接受劣解，
 *   从而增强全局搜索能力。典型做法是根据初始解的评估值或预期目标值范围来设定。</li>
 * </ul>
 *
 * <h3>通配符兼容性</h3>
 * {@link SimulatedAnnealing} 构造函数以 {@code SAInitializer<X, ? super Prob>}
 * 的形式接收本组件（下界通配符）。这意味着实现类可以将 {@code Prob} 声明为
 * 比实际使用的问题类型更泛化的父类型。例如，一个仅依赖 {@link Problem}{@code <double[]>}
 * 而不需要 {@code getDimension()} 等特有方法的初始化器，可以声明为
 * {@code SAInitializer<double[], Problem<double[]>>}，
 * 并被传入 {@code SimulatedAnnealing<double[], ContinuousProblem>} 中。
 *
 * @param <X>   解的表示类型（例如 {@code double[]}、{@code int[]}）
 * @param <Prob> 问题类型，必须实现 {@link Problem}{@code <X>}
 */
public abstract class SAInitializer<X, Prob extends Problem<X>> {

    /**
     * 绑定具体问题实例，使初始化器能访问问题的维度和边界等信息。
     * <p>
     * 此方法应在构造完成后、首次调用 {@link #initialX()} 或 {@link #initialTemperature()} 之前调用。
     * 通常由算法框架在构造阶段自动调用，实现者无需关心调用时机。
     *
     * @param problem 待求解问题，不为 {@code null}
     * @param random 随机数生成器，由主算法统一创建并注入，组件应使用此实例进行所有随机操作
     *               （如随机采样、随机初始化等），以保证结果可复现；不应自行创建新的 {@link Random} 实例
     */
    protected abstract void init(Prob problem,Random random);

    /**
     * 生成搜索的起始解。
     * <p>
     * 返回的解应当位于问题的合理区域内（例如连续问题需满足边界约束），
     * 并且是一个全新创建的对象，与任何内部状态无关。
     *
     * @return 初始解（独立新对象）
     */
    protected abstract X initialX();

    /**
     * 计算算法的起始温度。
     * <p>
     * 温度应设置得足够高，使得早期搜索具有较高的接受概率，避免过早陷入局部最优。
     * 一种常见策略是：随机采样若干解，根据其目标值方差或最大值来确定初始温度。
     *
     * @return 初始温度，通常为正数
     */
    protected abstract double initialTemperature();
}