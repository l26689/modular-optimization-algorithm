package sa.core;

/**
 * 终止条件，决定算法何时停止迭代。
 * <p>
 * 本组件在每次迭代结束后被调用，根据当前温度、当前解以及上一轮接受状态
 * 判断是否满足停止条件。常见实现包括：达到最大迭代次数、温度低于阈值、
 * 连续若干次未接受新解（停滞）等。
 *
 * <h3>冷启动约定</h3>
 * 首次调用 {@link #check(double, Object, boolean)} 时，
 * {@code isAccepted} 固定为 {@code false}。这并非一次真实的接受事件，
 * 仅表示“尚无历史”。实现类应将此视为冷启动信号，使用默认判断逻辑，
 * 不应依赖该标志决定是否终止。
 *
 * <h3>最少信息原则</h3>
 * 本组件仅接收主算法维护的原子信息：温度、当前解、上一次接受标志。
 * 迭代次数等可推导信息不在参数中，若需要（如实现最大迭代终止），
 * 可在类内部维护私有计数器，通过本方法的调用次数自行推导。
 *
 * <h3>实现自由</h3>
 * 本接口不对“终止”的语义做任何硬性约束。实现类可以基于温度、解的质量、
 * 搜索历史等任意标准组合来决定是否停止，框架仅要求返回 {@code true} 时结束循环。
 *
 * @param <X>   解的表示类型（例如 {@code double[]}）
 * @param <Prob> 问题类型，必须实现 {@link Problem}{@code <X>}
 */
public abstract class TerminationCondition<X, Prob extends Problem<X>> {

    /**
     * 绑定问题实例，使终止条件可获取问题的维度等元数据（多数实现无需此信息，
     * 但作为组件统一契约保留）。
     * <p>
     * 此方法由框架在构造阶段自动调用，使用者无需手动处理。
     *
     * @param problem 待求解问题
     * @throws NullPointerException 如果 problem 为 null
     */
    protected abstract void init(Prob problem);

    /**
     * 判断算法是否应当终止。
     * <p>
     * 本方法在每次迭代结束时被调用一次（与冷却策略调用时机相同），
     * 传入的 {@code isAccepted} 反映的是刚结束的本次迭代的接受结果，
     * 其值始终真实（包括首次调用后的第一轮结果）。
     * 首次调用时 {@code isAccepted} 为 {@code false}，表示“尚无历史”。
     *
     * @param temperature 当前温度
     * @param x           当前解（只读，不可原地修改）
     * @param isAccepted  刚结束的本次迭代的接受结果；首次调用时为 {@code false}
     * @return {@code true} 表示满足终止条件，算法将停止；{@code false} 表示继续迭代
     */
    protected abstract boolean check(double temperature, X x, boolean isAccepted);
}