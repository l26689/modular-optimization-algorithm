package oa.api.spi;

import oa.api.optimizationalgorithm.State;
import oa.api.problem.Problem;

/**
 * 初始化器的统一接口，定义了优化算法中生成初始解的契约。
 * <p>
 * 本接口继承自 {@link Component}，在继承 {@code init} 统一初始化能力的基础上，
 * 新增了 {@link #initializeX()} 方法，用于生成搜索的起始解。
 * 所有初始化器组件（如随机初始化、启发式构造、贪心构造等）均应实现本接口。
 *
 * <h3>调用时机</h3>
 * {@code initializeX()} 由主算法在优化过程启动时调用，且在整个优化过程中
 * <b>仅调用一次</b>。调用前，框架已通过 {@code init(Prob, Random)} 完成
 * 问题实例和随机数生成器的绑定，因此初始化器可直接使用内部持有的
 * {@link Problem} 和 {@link Random} 引用。
 *
 * <h3>解的可行性</h3>
 * 初始化器生成的解<b>必须是可行解</b>（满足问题的所有约束）。
 * 如果问题允许不可行解作为起点（如某些约束优化算法），
 * 应在具体实现类的文档中明确声明，并由主算法配合修复或惩罚机制处理。
 *
 * <h3>实现策略</h3>
 * 常见初始解生成策略包括：
 * <ul>
 *   <li><b>随机初始化</b>：在变量边界内随机采样，最简单通用；</li>
 *   <li><b>启发式构造</b>：利用问题领域知识构造高质量初始解，加速收敛；</li>
 *   <li><b>贪心构造</b>：逐步构建解，每步选择当前最优的局部决策；</li>
 *   <li><b>外部注入</b>：从外部数据源（如历史最优解、预训练结果）加载初始解。</li>
 * </ul>
 *
 * <h3>与 {@code init} 的职责分离</h3>
 * {@code init} 负责绑定问题上下文和随机源（"准备工具"），
 * {@code initializeX} 负责产生具体初始解（"开始干活"）。
 * 二者的分离使初始化器可以在不重新绑定问题的情况下，
 * 通过 {@link Reusable#reset()} 重置后反复生成不同的初始解。
 *
 * @param <X>    解的表示类型（例如 {@code double[]}、{@code int[]} 或自定义数据结构）
 * @param <Prob> 问题类型，必须是 {@link Problem} 的子类型
 * @param <S>    状态类型，必须是 {@link State} 的子类型
 *
 * @see Component
 * @see Reusable
 */
public interface Initializer<X,Prob extends Problem<X>,S extends State<X>> extends Component<X,Prob,S> {
    /**
     * 生成一个初始解，作为优化搜索的起点。
     * <p>
     * 调用前已通过 {@code init(Prob, Random)} 绑定问题实例和随机数生成器，
     * 实现类应使用内部持有的引用获取问题边界、维度等信息，并通过
     * 统一的随机源保证结果可复现。
     *
     * @return 初始解，必须是可行解（满足问题约束），不为 {@code null}
     */
    X initialX();
}