package oa.api.spi;

import oa.api.optimizationalgorithm.State;
import oa.api.problem.Problem;

import java.util.Random;
/**
 * 优化算法组件的统一接口，定义了所有算法组件应遵循的基本契约。
 * <p>
 * 在模块化优化框架中，优化算法由多个可替换的组件协作完成，例如：
 * <ul>
 *   <li>扰动器（负责生成候选解）</li>
 *   <li>冷却策略（控制温度衰减）</li>
 *   <li>终止条件（判断搜索是否结束）</li>
 *   <li>接受准则（决定是否接受新解）</li>
 * </ul>
 * 本接口为这些组件提供了统一的初始化入口，使框架可以在不关心具体组件类型
 * 的前提下，以一致的方式完成组件的初始化工作。
 *
 * <h3>设计意图</h3>
 * 所有参与优化算法运行的组件都应实现本接口。通过统一的 {@link #init(Problem, Random)}
 * 方法，框架在算法启动前为每个组件注入问题实例和随机数生成器，确保组件
 * 能够获取必要的上下文信息（如问题维度、变量边界、目标函数）并具备可控的
 * 随机行为。
 *
 * <h3>与 {@link Reusable} 的关系</h3>
 * 本接口关注组件的<b>初始化</b>，{@link Reusable} 关注组件的<b>复用</b>。
 * 二者的职责互不重叠：
 * <ul>
 *   <li>{@code init} 在组件首次使用前调用，绑定问题上下文和随机源；</li>
 *   <li>{@code reset} 在多次运行之间调用，清除累积的内部状态但不改变问题绑定。</li>
 * </ul>
 * 一个组件通常同时实现本接口和 {@link Reusable}：{@code init} 负责"出生"，
 * {@code reset} 负责"轮回"。
 *
 * <h3>功能方法的参数约定</h3>
 * 本接口仅定义了统一的初始化方法 {@link #init(Problem, Random)}，组件特有的
 * 功能方法（如终止条件的 {@code check}、扰动器的 {@code perturb}、冷却策略的
 * {@code cool}、接受准则的 {@code accept} 等）由各子接口或抽象类自行声明。
 * 这些功能方法应遵循以下约定：
 * <ul>
 *   <li>方法参数中应包含 {@link State} 或其子类（即本接口的类型参数 {@code S}），
 *       以便组件获取当前搜索状态（如当前解、温度、迭代次数等）；</li>
 *   <li>不应将问题实例或随机数生成器作为功能方法的参数——
 *       这些已在 {@code init} 中绑定，组件应在内部字段中持有引用。</li>
 * </ul>
 * 例如，{@code SATerminationCondition} 的 {@code check(SAState)} 方法
 * 接收 {@code SAState}（{@code State} 的子类）作为参数，从中获取当前温度
 * 和接受标志，而非通过额外参数传递这些信息。
 *
 * <h3>实现要求</h3>
 * <ul>
 *   <li>{@code init} 应在组件首次使用前被调用，且仅调用一次。
 *       若需在同一问题实例上重复运行，应使用 {@code reset} 而非再次调用 {@code init}。</li>
 *   <li>{@code init} 中的 {@code random} 参数应被存储为组件内部字段，
 *       组件中的所有随机操作均应使用该生成器，以保证算法整体的可重复性。</li>
 * </ul>
 *
 * @param <X>    解的表示类型（例如 {@code double[]}、{@code int[]} 或自定义数据结构）
 * @param <Prob> 问题类型，必须是 {@link Problem} 的子类型
 * @param <S>    状态类型，必须是 {@link State} 的子类型
 *
 * @see Reusable
 * @see Recorder
 */
public interface Component<X,Prob extends Problem<X>,S extends State<X>> {      
    /**
     * 初始化组件，传入问题实例和随机数生成器，用于设置组件运行所需的上下文。
     *
     * @param prob   待优化的问题实例
     * @param random 随机数生成器，用于保证算法中随机操作的可重复性
     */
    void init(Prob prob,Random random);
}