package oa;

/**
 * 优化算法的抽象基类。
 * <p>
 * 本类定义了所有优化算法的统一契约：持有一个待求解的 {@link Problem} 实例，
 * 并通过 {@link #solve()} 方法启动优化过程。具体算法（如模拟退火、遗传算法等）
 * 应继承此类并实现各自的求解逻辑。
 *
 * <h3>最少信息原则</h3>
 * 本基类仅维护问题实例这一核心引用，不引入任何算法特有的状态或参数。
 * 各子算法的组件（如扰动器、冷却策略等）通过独立的接口与主算法交互，
 * 保持框架的模块化和可扩展性。
 *
 * @param <X>    解的表示类型（例如 {@code double[]}、{@code int[]} 或自定义数据结构）
 * @param <Prob> 问题类型，必须实现 {@link Problem}{@code <X>}
 */
public abstract class OptimizationAlgorithm<X,Y, Prob extends Problem<X,Y>> {
    /** 待求解的优化问题，由子类在构造阶段绑定 */
    protected Prob problem;

    /**
     * 启动优化过程，返回搜索到的最优解。
     * <p>
     * 具体实现由子类根据各自算法逻辑完成（如模拟退火的迭代搜索、
     * 遗传算法的种群演化等）。返回的解应为独立拷贝，调用者可安全修改
     * 而不影响算法内部状态。
     *
     * @return 优化过程中发现的最优解（独立拷贝）
     */
    public abstract void solve();
}