package pso.basicpso;

import java.lang.reflect.Array;
import java.util.Random;

import oa.api.optimizationalgorithm.OptimizationAlgorithm;
import oa.api.problem.Problem;
import oa.api.spi.Recorder;
import oa.api.spi.TerminationCondition;
import pso.core.Particle;

/**
 * 通用粒子群优化算法（Particle Swarm Optimization），采用<b>同步更新</b>模型。
 * <p>
 * 本类是 PSO 算法的主循环控制器，继承自 {@link OptimizationAlgorithm}，
 * 负责协调粒子群体的初始化、迭代搜索和终止判断。
 * 与 SA 的 {@code SimulatedAnnealing} 类似，本类本身不包含任何搜索逻辑，
 * 所有行为由注入的粒子组件决定。
 *
 * <h3>同步 PSO vs 异步 PSO</h3>
 * 本类实现的是<b>同步 PSO</b>：
 * <ul>
 *   <li>每一轮迭代中，所有粒子基于<b>同一个稳定的群体状态</b>执行搜索；</li>
 *   <li>所有粒子的新位置收集完毕后，一次性地替换整个群体状态；</li>
 *   <li>这保证了每轮迭代中所有粒子看到的是相同的 gBest。</li>
 * </ul>
 * 异步 PSO（粒子搜索后立即更新状态）需要不同的实现，不在本类范围内。
 *
 * <h3>最少信息原则</h3>
 * 本类只负责迭代控制，不维护任何粒子内部状态（位置、速度、pBest 等）。
 * 粒子通过 {@link Particle#search(pso.core.PSOState)} 接收当前群体状态，
 * 自行从中评估 gBest 并执行速度-位置更新。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * ParticleSwarmOptimization<double[]> pso = new ParticleSwarmOptimization<>(
 *     problem,
 *     new Particle[] {
 *         new StandardPSOParticle(0.729, 1.49445, 1.49445),
 *         new StandardPSOParticle(0.729, 1.49445, 1.49445),
 *         // ... 更多粒子
 *     },
 *     new MaxCallTerminationCondition(10000)
 * );
 *
 * BestRecorder<double[]> recorder = new BestRecorder<>();
 * pso.solve(recorder);
 * }</pre>
 *
 * <h3>设计约束</h3>
 * <ul>
 *   <li>粒子数组长度至少为 1（空数组在构造时抛异常）。</li>
 *   <li>所有粒子共享同一 {@link Random} 实例（无参构造自动创建；
 *       有参构造使用用户提供的实例）。</li>
 *   <li>粒子数组中的元素类型可以不同（异构），只要它们都实现
 *       {@code Particle<X, ? super Prob, BasicPSOState<X>>}。</li>
 *   <li>本类自身是 {@code final} 的，不期望被继承。</li>
 * </ul>
 *
 * <h3>数组交换策略</h3>
 * {@link #solve(Recorder)} 使用三变量交换（temp = currentPositions;
 * currentPositions = newPositions; newPositions = temp）避免每轮分配新数组。
 * 这确保了 {@link BasicPSOState} 始终引用稳定的当前位置数组。
 *
 * @param <X> 解的表示类型（例如 {@code double[]}、{@code int[]}）
 *
 * @see Particle
 * @see BasicPSOState
 * @see pso.core.PSOState
 * @see pso.components.continuousproblem.StandardPSOParticle
 */
public final class ParticleSwarmOptimization<X,Prob extends Problem<X>> extends OptimizationAlgorithm<X,Prob,BasicPSOState<X>> {
    private Particle<X,? super Prob,BasicPSOState<X>>[] particles;
    private TerminationCondition<X,? super Prob,? super BasicPSOState<X>> terminationCondition;

    private Random random;

    /**
     * 构造一个 PSO 算法实例（自动创建随机源）。
     * <p>
     * 所有粒子和终止条件将共享同一 {@link Random} 实例（种子随机），
     * 结果不可复现。如需可复现结果，使用
     * {@link #ParticleSwarmOptimization(Random, Problem, Particle[], TerminationCondition)}。
     *
     * @param <Prob>               问题类型，至少是 {@link Problem}{@code <X>}
     * @param problem              待优化问题
     * @param particles            粒子数组，长度至少为 1
     * @param terminationCondition 终止条件
     * @throws IllegalArgumentException 如果 particles 为空数组
     */
    public ParticleSwarmOptimization(
        Prob problem,
        Particle<X,? super Prob,BasicPSOState<X>>[] particles,
        TerminationCondition<X,? super Prob,? super BasicPSOState<X>> terminationCondition) {
        if(particles.length == 0) {
            throw new IllegalArgumentException("particles.length must be greater than 0");
        }
        this.problem = problem;
        this.particles = particles;
        this.terminationCondition = terminationCondition;
        random = new Random();
        for (Particle<X,? super Prob,BasicPSOState<X>> particle : particles) {
            particle.init(problem,random);
        }
        terminationCondition.init(problem,random);
    }

    /**
     * 构造一个 PSO 算法实例（指定随机源）。
     * <p>
     * 所有粒子和终止条件将共享传入的 {@link Random} 实例。
     * 传入相同种子的 {@link Random} 可保证结果可复现。
     *
     * @param <Prob>               问题类型，至少是 {@link Problem}{@code <X>}
     * @param random               共享随机源，用于所有粒子的初始化和搜索
     * @param problem              待优化问题
     * @param particles            粒子数组，长度至少为 1
     * @param terminationCondition 终止条件
     * @throws IllegalArgumentException 如果 particles 为空数组
     */
    public ParticleSwarmOptimization(
        Random random,
        Prob problem,
        Particle<X,? super Prob,BasicPSOState<X>>[] particles,
        TerminationCondition<X,? super Prob,? super BasicPSOState<X>> terminationCondition) {
        if(particles.length == 0) {
            throw new IllegalArgumentException("particles.length must be greater than 0");
        }
        this.random = random;
        this.problem = problem;
        this.particles = particles;
        this.terminationCondition = terminationCondition;
        for (Particle<X,? super Prob,BasicPSOState<X>> particle : particles) {
            particle.init(problem,random);
        }
        terminationCondition.init(problem,random);
    }

    /**
     * 执行同步 PSO 优化。
     * <p>
     * 主循环流程：
     * <ol>
     *   <li>调用 {@code recorder.init(problem, random)} 完成 Recorder 的生命周期绑定；</li>
     *   <li>调用各粒子的 {@link Particle#initialX()} 收集初始位置，</li>
     *       构造初始 {@link BasicPSOState}；</li>
     *   <li>通过 {@link Recorder#record(oa.api.optimizationalgorithm.State)}
     *       记录初始状态；</li>
     *   <li>进入迭代循环：</li>
     *   <ol>
     *     <li>为每个粒子调用 {@link Particle#search(pso.core.PSOState)}，
     *         收集新位置到 {@code newPositions} 数组；</li>
     *     <li>通过三变量交换，将 {@code newPositions} 变为当前状态；</li>
     *     <li>更新 {@link BasicPSOState} 的引用；</li>
     *     <li>记录新状态；</li>
     *     <li>检查终止条件。</li>
     *   </ol>
     * </ol>
     * <p>
     * 数组交换策略确保 {@link BasicPSOState} 始终引用稳定的当前位置数组，
     * 避免粒子在搜索过程中读写不同步的问题。
     *
     * @param recorder 状态记录器，用于收集迭代过程中的群体状态
     */
    @Override
    public void solve(Recorder<X, ? super Prob, ? super BasicPSOState<X>> recorder) {
        recorder.init(problem, random);

        X tempX = particles[0].initialX();
        X[] currentPositions = (X[]) Array.newInstance(tempX.getClass(),particles.length);
        X[] newPositions = (X[]) Array.newInstance(tempX.getClass(),particles.length);
        X[] temp;
        currentPositions[0] = tempX;
            
        for (int i = 1; i < particles.length; i++) {
            currentPositions[i] = particles[i].initialX();
        }

        BasicPSOState<X> state = new BasicPSOState<X>(currentPositions);

        recorder.record(state);

        while (!terminationCondition.check(state)) {
            for (int i = 0; i < particles.length; i++) {
                newPositions[i] = particles[i].search(state);
            }

            temp = currentPositions;
            currentPositions = newPositions;
            newPositions = temp;
            state.set(currentPositions);

            recorder.record(state);
        }
    }
}