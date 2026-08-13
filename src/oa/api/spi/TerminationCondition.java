package oa.api.spi;

import oa.api.optimizationalgorithm.State;
import oa.api.problem.Problem;

/**
 * 终止条件的统一接口，定义了优化算法中判断搜索何时停止的契约。
 * <p>
 * 本接口继承自 {@link Component}，在继承 {@code init} 统一初始化能力的基础上，
 * 新增了 {@link #check()} 方法，用于判定当前搜索是否应终止。
 * 所有终止条件组件（如最大迭代次数、温度阈值、停滞检测等）均应实现本接口。
 *
 * <h3>调用时机</h3>
 * {@code check()} 由主算法在每次迭代的关键节点调用（通常为迭代末尾），
 * 返回 {@code true} 时主循环立即退出。具体调用频率由各算法自行决定：
 * <ul>
 *   <li>模拟退火：每轮迭代（扰动 → 评估 → 接受/拒绝 → 冷却）结束后调用一次；</li>
 *   <li>遗传算法：每代演化完成后调用；</li>
 *   <li>其他算法：由各自的主循环约定。</li>
 * </ul>
 *
 * <h3>状态信息获取</h3>
 * 终止判断所需的状态信息（如当前温度、迭代计数、解的改进幅度等）
 * 应通过以下途径获取：
 * <ul>
 *   <li><b>内部维护</b>：迭代次数等可推导信息由组件内部计数器自行维护，
 *       每次 {@code check()} 调用时自增，无需外部传入；</li>
 *   <li><b>通过 {@code init} 绑定的问题实例</b>：如需获取问题维度等元数据，
 *       通过 {@code init} 阶段注入的 {@link Problem} 引用获取；</li>
 *   <li><b>通过 {@code State} 参数</b>：若子接口或实现类需要当前搜索状态
 *       （如当前解、温度、接受标志等），应在方法签名中添加 {@code S} 类型参数，
 *       遵循 {@link Component} 中定义的功能方法参数约定。</li>
 * </ul>
 *
 * <h3>实现要求</h3>
 * <ul>
 *   <li>{@code check()} 必须是幂等的——多次调用不应产生副作用，
 *       因为主算法可能在一次迭代中多次检查终止条件；</li>
 *   <li>返回 {@code true} 后，组件应能通过 {@link Reusable#reset()} 重置状态，
 *       以支持同一实例的多次运行；</li>
 *   <li>终止条件应尽可能轻量，避免在 {@code check()} 中执行耗时的目标函数评估。</li>
 * </ul>
 *
 * <h3>典型实现</h3>
 * <ul>
 *   <li><b>最大迭代次数</b>：内部维护计数器，达到预设上限时返回 {@code true}；</li>
 *   <li><b>温度阈值</b>：通过 {@code State} 参数获取当前温度，低于阈值时终止；</li>
 *   <li><b>停滞检测</b>：记录连续未接受新解的次数，超过容忍上限时终止；</li>
 *   <li><b>组合条件</b>：以上任意条件满足即终止。</li>
 * </ul>
 *
 * @param <X>    解的表示类型（例如 {@code double[]}、{@code int[]} 或自定义数据结构）
 * @param <Prob> 问题类型，必须是 {@link Problem} 的子类型
 * @param <S>    状态类型，必须是 {@link State} 的子类型
 *
 * @see Component
 * @see Reusable
 */
public interface TerminationCondition<X,Prob extends Problem<X>,S extends State<X>> extends Component<X,Prob,S> {
    /**
     * 检查当前是否满足终止条件。
     * <p>
     * 由主算法在每次迭代的适当节点调用。实现类应根据内部状态
     * （如迭代计数器、温度阈值等）判断搜索是否应结束。
     *
     * @return {@code true} 表示满足终止条件，搜索应停止；
     *         {@code false} 表示继续搜索
     */
    boolean check(S state);
}