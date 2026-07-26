package oa.api;

/**
 * 优化算法的抽象基类。
 * <p>
 * 本类定义了所有优化算法的统一契约：持有一个待求解的 {@link Problem} 实例，
 * 并通过 {@link #solve(Recorder)} 方法启动优化过程。具体算法（如模拟退火、遗传算法等）
 * 应继承此类并实现各自的求解逻辑。
 *
 * <h3>最少信息原则</h3>
 * 本基类仅维护问题实例这一核心引用，不引入任何算法特有的状态或参数。
 * 各子算法的组件（如扰动器、冷却策略等）通过独立的接口与主算法交互，
 * 保持框架的模块化和可扩展性。
 *
 * <h3>优化结果获取</h3>
 * {@code solve()} 方法返回 {@code void}，优化结果不通过返回值传递，
 * 而是通过 {@link Recorder} 参数对外提供。调用方在构造算法实例后，
 * 需同时准备一个 {@code Recorder} 子类实例，在 {@code solve()} 返回后
 * 从中获取最优解、搜索历史等结果。
 *
 * @param <X>    解的表示类型（例如 {@code double[]}、{@code int[]} 或自定义数据结构）
 * @param <Y>    目标函数返回值的类型（例如 {@code Double}、{@code double[]}）
 * @param <Prob> 问题类型，必须实现 {@link Problem}{@code <X, Y>}
 * @param <S>    状态类型，根据具体优化算法，类型不同，需要继承 {@link State}{@code <X>}
 */
public abstract class OptimizationAlgorithm<X,Y, Prob extends Problem<X,Y>, S extends State<X>> {
    /** 待求解的优化问题，由子类在构造阶段绑定 */
    protected Prob problem;

    /**
     * 启动优化过程，通过 {@link Recorder} 输出优化结果。
     * <p>
     * 具体实现由子类根据各自算法逻辑完成（如模拟退火的迭代搜索、
     * 遗传算法的种群演化等）。优化结果不通过返回值传递，
     * 而是通过参数 {@code recorder} 对外提供。
     *
     * @param recorder 记录器，负责接收算法产生的解及其目标值；
     *                 优化结果通过 {@code recorder} 的特定方法（如 {@code getBestX()}）对外提供
     */
    public abstract void solve(Recorder<X,Y,? extends Prob,? super S> recorder);

}