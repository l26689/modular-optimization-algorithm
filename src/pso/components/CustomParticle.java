package pso.components;

import java.util.Random;

import oa.api.optimizationalgorithm.State;
import oa.api.problem.Problem;
import oa.api.spi.Initializer;
import oa.api.spi.SearchOperator;
import pso.core.Particle;

/**
 * 组合式粒子，通过包装任意 {@link Initializer} 和 {@link SearchOperator} 实现 {@link Particle} 接口。
 * <p>
 * 本类是 {@link Particle} 接口的组合实现方式，将初始化逻辑和搜索逻辑
 * 分别委托给独立的组件。这使得可以实现<b>异构粒子群</b>——
 * 同一轮迭代中，不同粒子可以采用完全不同的搜索策略。
 *
 * <h3>设计动机</h3>
 * SPI 层的 {@link Initializer} 和 {@link SearchOperator} 是独立接口，
 * 分别定义了"生成初始解"和"执行一步搜索"的能力。通过本类将它们组合，
 * 任何一组兼容的初始化和搜索组件都可以作为一个完整的粒子使用。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 粒子 0：标准 PSO 初始化 + 经典 PSO 速度更新
 * Particle<double[], ContinuousProblem, BasicPSOState<double[]>> p0 =
 *     new CustomParticle<>(
 *         new UniformInitializer(),
 *         new StandardPSOSearch(0.729, 1.49445, 1.49445)
 *     );
 *
 * // 粒子 1：同样初始化 + 自适应 PSO 速度更新
 * Particle<double[], ContinuousProblem, BasicPSOState<double[]>> p1 =
 *     new CustomParticle<>(
 *         new UniformInitializer(),
 *         new AdaptivePSOSearch()
 *     );
 * }</pre>
 *
 * <h3>生命周期</h3>
 * {@link #init(Problem, Random)} 同时初始化内部的 initializer 和 searchOperator，
 * 确保两者共享同一组问题元数据和随机源。
 *
 * @param <X>    解的表示类型
 * @param <Prob> 问题类型，必须是 {@link Problem} 的子类型
 * @param <S>    状态类型，必须是 {@link State} 的子类型
 *
 * @see Particle
 * @see pso.components.continuousproblem.StandardPSOParticle
 */
public final class CustomParticle<X,Prob extends Problem<X>,S extends State<X>> implements Particle<X,Prob,S> {
    private final Initializer<X, ? super Prob, ? super S> initializer;
    private final SearchOperator<X,? super Prob,? super S> searchOperator;

    /**
     * 构造一个组合式粒子。
     * <p>
     * 使用 PECS 通配符（{@code ? super}），使粒子可以接受比自身声明
     * 更宽泛的问题类型和状态类型的组件，提升复用性。
     *
     * @param initializer    初始化器，负责生成该粒子的初始位置
     * @param searchOperator 搜索算子，负责从当前状态出发执行一步搜索
     */
    public CustomParticle(
        Initializer<X,? super Prob,? super S> initializer, 
        SearchOperator<X,? super Prob,? super S> searchOperator) {
        this.initializer = initializer;
        this.searchOperator = searchOperator;
    }

    @Override
    public void init(Prob prob, Random random) {
        initializer.init(prob, random);
        searchOperator.init(prob, random);
    }

    @Override
    public X search(S state) {
        return searchOperator.search(state);
    }

    @Override
    public X initialX() {
        return initializer.initialX();
    }
}