package pso.core;

import oa.api.optimizationalgorithm.State;
import oa.api.problem.Problem;
import oa.api.spi.Initializer;
import oa.api.spi.SearchOperator;

/**
 * PSO 粒子的统一接口，将初始化和搜索两种能力合二为一。
 * <p>
 * 本接口同时继承 {@link Initializer} 和 {@link SearchOperator}，
 * 使每个粒子成为一个自包含的搜索单元：既能生成自身的初始位置，
 * 又能从当前群体状态出发执行一步搜索转移。
 *
 * <h3>设计动机</h3>
 * 传统 PSO 实现中，初始化逻辑和速度-位置更新逻辑分散在不同模块中，
 * 粒子只是被动的数据容器。本接口打破这一模式，让每个粒子<b>拥有自己的行为</b>：
 * <ul>
 *   <li>粒子 0 可以用标准 PSO 更新（ω=0.9, c₁=c₂=2.0），</li>
 *   <li>粒子 1 可以用收缩因子 PSO（ω=0.729, c₁=c₂=1.49445），</li>
 *   <li>粒子 2 甚至可以用 DE 变异——完全异构。</li>
 * </ul>
 * 这种设计使粒子群优化从"同构群体的参数调优"提升为
 * "异构搜索策略的自由组合"，是框架最少信息原则在群体算法中的核心体现。
 *
 * <h3>与 SPI 层的关系</h3>
 * {@code Particle} 直接继承 SPI 层的两个通用接口，而非定义 PSO 特有方法。
 * 这意味着任何实现了 {@link Initializer} 和 {@link SearchOperator} 的组件
 * 都可以通过 {@link pso.components.CustomParticle} 包装为一个粒子，
 * 无需额外适配。
 *
 * <h3>实现方式</h3>
 * 有两种方式实现本接口：
 * <ul>
 *   <li><b>直接实现</b>：如 {@code StandardPSOParticle}，在单个类中完整实现
 *       初始化和搜索逻辑，适合标准 PSO 粒子。</li>
 *   <li><b>组合实现</b>：通过 {@link pso.components.CustomParticle} 将任意
 *       {@link Initializer} 和 {@link SearchOperator} 组合为一个粒子，
 *       适合快速原型和异构粒子群。</li>
 * </ul>
 *
 * <h3>最少信息原则</h3>
 * {@link #search(State)} 仅接收当前群体状态，粒子所需的个体信息
 * （自身位置、速度、个体历史最优）由粒子内部维护。gBest 由粒子自行
 * 从状态中评估得出，不依赖外部传入。
 *
 * @param <X>    解的表示类型（例如 {@code double[]}、{@code int[]}）
 * @param <Prob> 问题类型，必须是 {@link Problem} 的子类型
 * @param <S>    状态类型，必须是 {@link State} 的子类型；
 *               在 {@link pso.basicpso.ParticleSwarmOptimization} 中被固定为
 *               {@link pso.basicpso.BasicPSOState}{@code <X>}
 *
 * @see Initializer
 * @see SearchOperator
 * @see pso.components.CustomParticle
 * @see pso.components.continuousproblem.StandardPSOParticle
 */
public interface Particle<X,Prob extends Problem<X>,S extends State<X>> 
extends Initializer<X,Prob,S>,
SearchOperator<X,Prob,S> {
}