package oa.api.spi;

import oa.api.optimizationalgorithm.State;
import oa.api.problem.Problem;

/**
 * 搜索算子的统一接口，定义了优化算法中"从当前状态生成候选解"的契约。
 * <p>
 * 本接口继承自 {@link Component}，在继承 {@code init} 统一初始化能力的基础上，
 * 新增了 {@link #search(State)} 方法，用于执行搜索空间中的一步状态转移。
 * 所有搜索算子组件（如 SA 扰动器、PSO 速度更新、DE 变异等）均应实现本接口。
 *
 * <h3>设计定位</h3>
 * {@code SearchOperator} 与 {@link Initializer}、{@link TerminationCondition}
 * 三者共同构成 SPI 层的核心算法组件三角：
 * <ul>
 *   <li>{@link Initializer} —— 生成初始解（搜索起点）；</li>
 *   <li>{@code SearchOperator} —— 执行状态转移（搜索步进）；</li>
 *   <li>{@link TerminationCondition} —— 判断搜索终止（搜索终点）。</li>
 * </ul>
 *
 * <h3>命名缘由</h3>
 * 选择 {@code SearchOperator} 而非 {@code Perturbation}，是因为：
 * <ul>
 *   <li>"Perturbation" 在优化文献中暗示"无倾向的随机扰动"，而实际搜索算子
 *       可能包含确定性方向（如 PSO 的速度向量、DE 的差分向量）；</li>
 *   <li>"SearchOperator" 中性描述搜索空间中的一步转移操作，不预设随机性、
 *       方向性或步长策略，适用于 SA、PSO、爬山、DE 等多种算法。</li>
 * </ul>
 *
 * <h3>最小单位搜索</h3>
 * 每次调用 {@code search()} 只返回<b>一个</b>候选解（最小步进单位）。
 * 对于群体算法，主循环负责多次调用本方法以生成整个种群的新位置。
 * 这种设计保持了接口的原子性，避免将种群编排逻辑耦合到搜索算子中。
 *
 * <h3>与 GA 的关系</h3>
 * GA 不使用本接口。GA 的搜索操作拆分为选择（Selection）、交叉（Crossover）、
 * 变异（Mutation）三个独立步骤，各自实现 {@link Component}，由 GA 主循环编排。
 *
 * <h3>实现要求</h3>
 * <ul>
 *   <li>必须返回全新创建的对象，不得原地修改输入状态中的解；</li>
 *   <li>若需要自适应策略（如根据历史接受率调整步长），
 *       可在类内部维护私有状态；</li>
 *   <li>随机操作必须使用 {@code init} 阶段注入的 {@link java.util.Random} 实例，
 *       不得自行创建新的随机源。</li>
 * </ul>
 *
 * @param <X>    解的表示类型（例如 {@code double[]}、{@code int[]} 或自定义数据结构）
 * @param <Prob> 问题类型，必须是 {@link Problem} 的子类型
 * @param <S>    状态类型，必须是 {@link State} 的子类型
 *
 * @see Component
 * @see Initializer
 * @see TerminationCondition
 */
public interface SearchOperator<X, Prob extends Problem<X>, S extends State<X>>
        extends Component<X, Prob, S> {

    /**
     * 从当前搜索状态生成一个新的候选解（最小步进单位）。
     * <p>
     * 由主算法在每次迭代中调用，传入封装了当前搜索状态的 {@code S} 对象。
     * 实现类应从中获取当前解信息，生成一个候选解并返回。
     * 对于群体算法，主循环负责多次调用本方法以生成完整种群。
     *
     * @param state 封装了当前搜索状态的对象，包含当前解等信息（只读，不可原地修改）
     * @return 全新的候选解，必须与输入解相互独立
     */
    X search(S state);
}