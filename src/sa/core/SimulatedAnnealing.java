package sa.core;

import java.util.Random;

import oa.api.OptimizationAlgorithm;
import oa.api.Problem;
import oa.api.Recorder;

/**
 * 模块化模拟退火算法的主协调器。
 * <p>
 * 负责将初始化器、扰动器、冷却策略和终止条件组装为完整的优化流程。
 * 内部主循环遵循标准模拟退火模板：
 * <ol>
 *   <li>获取初始解与初始温度</li>
 *   <li>迭代：扰动 → 评估 → Metropolis 接受准则 → 冷却 → 检查终止</li>
 *   <li>通过 {@link oa.api.Recorder} 输出优化结果</li>
 * </ol>
 *
 * <h3>问题类型绑定</h3>
 * 与基类 {@link oa.api.OptimizationAlgorithm} 不同，本类在<b>类级别</b>仅声明
 * {@code <X, Y>}，具体的 {@code Prob} 类型参数被推迟到<b>构造函数级别</b>声明。
 * 这样设计的好处是：同一个 {@code SimulatedAnnealing<X, Y>} 实例在理论上可以
 * 被不同类型的问题复用（配合 {@link oa.api.Reusable} 接口），
 * 而无需在类声明时就将问题类型固化。
 *
 * <h3>最少信息原则</h3>
 * 本控制器仅向组件传递它们无法自行推导的原子事实：
 * <ul>
 *   <li>当前解 {@code X}</li>
 *   <li>系统温度</li>
 *   <li>上一次接受标志 {@code isAccepted}</li>
 * </ul>
 * 目标函数值、是否改进等冗余信息不通过接口传递，
 * 组件若需要可自行通过持有的 {@link Problem#evaluate} 获取。
 *
 * <h3>冷启动</h3>
 * 首次迭代前 {@code isAccepted} 被初始化为 {@code false}，
 * 所有组件必须能正确处理这种"无历史"状态（详见各组件文档）。
 *
 * <h3>组件类型的通配符设计</h3>
 * 构造函数接受 {@code SA*<X, Y, ? super Prob>} 而非精确的 {@code SA*<X, Y, Prob>}，
 * 使用下界通配符（{@code ? super Prob}）放宽了对组件问题类型的要求。
 * 这意味着：一个声明为 {@code SATerminationCondition<double[], Double, Problem<double[], Double>>}
 * 的通用终止条件，可以被传入以 {@code ContinuousProblem} 构造的
 * {@code SimulatedAnnealing}——因为 {@code Problem<double[], Double>} 是 {@code ContinuousProblem} 的父类型。
 * <p>
 * 此设计遵循 PECS 原则（Producer Extends, Consumer Super）：
 * 主算法通过 {@code init(Prob, Random)} 向组件"写入"问题实例，
 * 因此使用 {@code ? super Prob}（消费者需要下界通配符）。
 * 不依赖问题特有方法（如 {@code getDimension()}）的组件可以用更泛化的
 * {@code Prob} 参数声明，实现跨问题类型的复用。
 *
 * <h3>随机数生成器绑定</h3>
 * 本类持有 {@link Random} 实例，并在构造函数中通过各组件的
 * {@code init(Prob, Random)} 方法统一注入。注入的随机数生成器
 * 在以下两个层面保证结果的可复现性：
 * <ul>
 *   <li><b>外部注入</b>：调用方可通过双参构造函数传入带固定种子的 {@code Random}，
 *       使整个优化过程完全可复现（相同输入 → 相同输出）。</li>
 *   <li><b>内部创建</b>：单参构造函数内部创建 {@code new Random()}，
 *       适用于不需要精确复现的日常使用场景。</li>
 * </ul>
 * 所有组件（初始化器、扰动器、冷却策略、终止条件）共享同一 {@code Random} 实例，
 * 组件内部不应再自行创建独立的随机数生成器，否则将破坏序列的可复现性。
 *
 * @param <X> 解的表示类型（例如 {@code double[]}、{@code int[]}）
 * @param <Y> 目标函数返回值的类型（例如 {@code Double}、{@code double[]}）
 */
public class SimulatedAnnealing<X,Y> extends OptimizationAlgorithm<X,Y,Problem<X,Y>,SAState<X>> {
    private SAInitializer<X,Y,? extends Problem<X,Y>> initializer;//初始化器
    private SAPerturbation<X,Y,? extends Problem<X,Y>> perturbation;//扰动器
    private SACoolingSchedule<X,Y,? extends Problem<X,Y>> coolingSchedule;//冷却器
    private SATerminationCondition<X,Y,? extends Problem<X,Y>> terminationCondition;//终止条件
    private Random random;//随机数生成器，由外部或内部创建，统一注入到所有组件，确保随机性可复现

    /**
     * 构造一个完全组装的模拟退火实例，使用内部随机数生成器。
     * <p>
     * 构造函数内部会调用各组件的 {@code init(problem, random)}
     * 完成问题绑定，因此传入的组件无需在外部预先调用 {@code init}。
     *
     * <h3>通配符类型参数</h3>
     * 四个组件参数均声明为 {@code ? super Prob}（下界通配符），
     * 允许组件使用比 {@code Prob} 更泛化的问题类型声明。
     * 例如，一个不依赖 {@code ContinuousProblem} 特有方法的终止条件
     * 可以声明为 {@code SATerminationCondition<double[], Double, Problem<double[], Double>>}，
     * 仍能传入以 {@code ContinuousProblem} 构造的 {@code SimulatedAnnealing}。
     * 这种设计提升了组件的跨领域复用性。
     *
     * <h3>随机数生成器</h3>
     * 本构造函数内部创建 {@code new Random()} 并注入到所有组件。
     * 如需可复现的优化结果，请使用双参构造函数并传入带固定种子的 {@link Random}。
     *
     * @param <Prob>              问题类型，必须实现 {@link Problem}{@code <X, Y>}；
     *                            此泛型参数在构造函数级别声明，而非类级别
     * @param problem              待优化问题，不为 {@code null}
     * @param initializer          初始化器，负责生成初始解和初始温度
     * @param perturbation         扰动器，负责生成邻域候选解
     * @param coolingSchedule      冷却调度，负责更新温度
     * @param terminationCondition 终止条件，负责判断算法是否结束
     * @throws NullPointerException 如果任何参数为 {@code null}
     */
    public <Prob extends Problem<X,Y>>SimulatedAnnealing(
        Prob problem ,
        SAInitializer<X,Y,? super Prob> initializer,
        SAPerturbation<X,Y,? super Prob> perturbation,
        SACoolingSchedule<X,Y,? super Prob> coolingSchedule,
        SATerminationCondition<X,Y,? super Prob> terminationCondition){
            this.problem = problem;
            this.initializer = initializer;
            this.perturbation = perturbation;
            this.coolingSchedule = coolingSchedule;
            this.terminationCondition = terminationCondition;
            this.random = new Random();
            initializer.init(problem,random);
            perturbation.init(problem,random);
            coolingSchedule.init(problem,random);
            terminationCondition.init(problem,random);
        }

    /**
     * 构造一个完全组装的模拟退火实例，使用外部指定的随机数生成器。
     * <p>
     * 与单参构造函数功能相同，但允许调用方传入自定义的 {@link Random} 实例
     * （例如带固定种子 {@code new Random(42)}），使优化过程完全可复现。
     * 构造函数内部同样会调用各组件的 {@code init(problem, random)} 完成问题绑定。
     *
     * @param <Prob>              问题类型，必须实现 {@link Problem}{@code <X, Y>}；
     *                            此泛型参数在构造函数级别声明，而非类级别
     * @param random               随机数生成器，被注入到所有组件；传固定种子可实现结果复现
     * @param problem              待优化问题，不为 {@code null}
     * @param initializer          初始化器，负责生成初始解和初始温度
     * @param perturbation         扰动器，负责生成邻域候选解
     * @param coolingSchedule      冷却调度，负责更新温度
     * @param terminationCondition 终止条件，负责判断算法是否结束
     * @throws NullPointerException 如果任何参数为 {@code null}
     */
    public <Prob extends Problem<X,Y>>SimulatedAnnealing(
        Random random,
        Prob problem ,
        SAInitializer<X,Y,? super Prob> initializer,
        SAPerturbation<X,Y,? super Prob> perturbation,
        SACoolingSchedule<X,Y,? super Prob> coolingSchedule,
        SATerminationCondition<X,Y,? super Prob> terminationCondition){
            this.problem = problem;
            this.initializer = initializer;
            this.perturbation = perturbation;
            this.coolingSchedule = coolingSchedule;
            this.terminationCondition = terminationCondition;
            this.random = random;
            initializer.init(problem,random);
            perturbation.init(problem,random);
            coolingSchedule.init(problem,random);
            terminationCondition.init(problem,random);
        }

    /**
     * 启动模拟退火优化过程，通过 {@link Recorder} 输出优化结果。
     * <p>
     * 算法流程：
     * <ol>
     *   <li>调用初始化器获得初始解 {@code currentX} 和初始温度。</li>
     *   <li>计算初始解的目标值。</li>
     *   <li>在每一轮迭代中：
     *     <ul>
     *       <li>调用扰动器生成候选解 {@code newX}。</li>
     *       <li>评估候选解的目标值。</li>
     *       <li>通过 {@link Problem#compare} 统一计算 Metropolis 接受概率，
     *           不区分"更优/更差"分支（详见下方说明）。</li>
     *       <li>若候选解被接受，调用 {@code recorder.record(newX, newValue)} 记录。</li>
     *       <li>调用冷却策略降低温度。</li>
     *       <li>检查终止条件是否满足。</li>
     *     </ul>
     *   </li>
     *   <li>优化结果通过 {@code recorder} 对外提供（如 {@code recorder.getBestX()}）。</li>
     * </ol>
     *
     * <h3>compare 与 Metropolis 接受准则</h3>
     * 本方法使用统一表达式计算接受概率，不显式分支：
     * <pre>{@code
     *   P = exp(problem.compare(newX, currentX) / temperature)
     * }</pre>
     * 其中 {@link Problem#compare} 定义的是<b>偏序关系</b>（partial order）：
     * <ul>
     *   <li>{@code compare(newX, currentX) > 0} → 候选解的目标值优于当前解的目标值</li>
     *   <li>{@code compare(newX, currentX) < 0} → 候选解的目标值劣于当前解的目标值</li>
     *   <li>{@code compare(newX, currentX) = 0} → 候选解的目标值等优 <b>或</b> 互不支配</li>
     * </ul>
     * 对应的接受行为：
     * <ul>
     *   <li>候选解更优时：{@code exp(正值 / T) > 1}，确定性接受（{@code random < P} 必然成立）</li>
     *   <li>候选解更差时：{@code exp(负值 / T) ∈ (0, 1)}，以概率接受，差距越大概率越低</li>
     *   <li>等优或不可比时：{@code exp(0) = 1}，确定性接受</li>
     * </ul>
     *
     * <h3>设计意图：多目标优化中的充分探索</h3>
     * 本方法刻意不将"候选解更优时无条件接受"作为显式分支写出，
     * 而是依赖 {@code compare} 的符号语义自然达到同样效果。这一设计的核心原因在于
     * <b>多目标优化</b>：当两个解互不支配时，{@code compare} 返回零，
     * 接受概率为 {@code 1}，算法始终接受不可比的新解，从而在 Pareto 前沿上
     * 进行充分的无偏随机游走，避免因人为偏好而遗漏前沿区域。
     * <p>
     * <b>单目标场景下的差异：</b>在全序关系中，{@code compare = 0} 仅表示等优，
     * 确定性接受等优解不会引入额外开销。然而，若希望对等优解也施加 Metropolis 试探
     * （例如避免在平坦区域停滞），需在 {@code compare} 实现中将等优映射为小正值
     * 而非零，使接受概率略低于 1。这与当前框架的偏序设计完全兼容。
     *
     * <h3>Recorder 记录策略</h3>
     * 模拟退火的 {@link oa.api.Recorder} 遵循<b>仅记录被接受解</b>的策略：
     * 每轮迭代中，只有当候选解被接受（无论是因更优而确定性接受，还是因 Metropolis
     * 准则而概率性接受）时，才会调用 {@code recorder.record(newX, newValue)}。
     * 被拒绝的候选解不会触发记录。这一策略确保记录的历史序列完整反映了
     * 算法在解空间中的实际搜索轨迹（即马尔可夫链的每一步状态）。
     *
     * <h3>冷启动细节</h3>
     * 第一次迭代中，传递给各组件的 {@code isAccepted} 固定为 {@code false}。
     * 各组件（特别是扰动器和终止条件）需正确处理此初始状态。
     *
     * <h3>SAState 说明</h3>
     * 算法通过 {@link SAState} 对象向组件传递状态信息，该对象封装了三个核心字段：
     * <ul>
     *   <li>{@code currentX} - 当前解</li>
     *   <li>{@code temperature} - 当前系统温度</li>
     *   <li>{@code isAccepted} - 上一轮迭代是否接受了新解</li>
     * </ul>
     * 组件应通过 {@code state.currentX()}、{@code state.temperature()}、{@code state.isAccepted()} 访问这些信息。
     *
     * <h3>线程安全</h3>
     * 本方法未做任何同步，默认在单线程下使用。如果在多线程环境中调用，
     * 需由外部保证互斥。
     *
     * @param recorder 记录器，负责接收每一个被接受的解及其目标值；
     *                 优化结果通过 {@code recorder} 对外提供（如 {@code getBestX()}、{@code getHistory()} 等）
     */
    @Override
    public void solve(Recorder<X,Y,? extends Problem<X,Y>,? super SAState<X>> recorder){
        double temperature= initializer.initialTemperature();
        X currentX = initializer.initialX();
         X newX = problem.copyX(currentX);

        boolean isAccepted = false;

        while (!terminationCondition.check(new SAState<X>(currentX,temperature,isAccepted))) {

            newX = perturbation.perturb(new SAState<X>(currentX,temperature,isAccepted));

            isAccepted = random.nextDouble()<Math.exp(problem.compare(newX,currentX)/temperature);
            if(isAccepted){
                currentX = newX;
            }
            recorder.record(new SAState<X>(currentX,temperature,isAccepted));
            temperature = coolingSchedule.cool(new SAState<X>(currentX,temperature,isAccepted));
        }
    }
}