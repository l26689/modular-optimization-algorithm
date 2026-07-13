package sa.core;

import java.util.Random;

/**
 * 模块化模拟退火算法的主协调器。
 * <p>
 * 负责将初始化器、扰动器、冷却策略和终止条件组装为完整的优化流程。
 * 内部主循环遵循标准模拟退火模板：
 * <ol>
 *   <li>获取初始解与初始温度</li>
 *   <li>迭代：扰动 → 评估 → Metropolis 接受准则 → 冷却 → 检查终止</li>
 *   <li>返回搜索过程中发现的最优解</li>
 * </ol>
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
 * 所有组件必须能正确处理这种“无历史”状态（详见各组件文档）。
 *
 * @param <X>    解的表示类型（例如 {@code double[]}、{@code int[]}）
 * @param <Prob> 问题类型，必须实现 {@link Problem}{@code <X>}
 */
public class ModularSimulatedAnnealing<X,Prob extends Problem<X>> {
    private Initializer<X,Prob> initializer;//初始化器
    private Perturbation<X,Prob> perturbation;//扰动器
    private CoolingSchedule<X,Prob> coolingSchedule;//冷却器
    private TerminationCondition<X,Prob> terminationCondition;//终止条件
    private Random random = new Random();//随机数生成器
    private Prob problem;//优化问题

    /**
     * 构造一个完全组装的模拟退火实例。
     * <p>
     * 构造函数内部会调用各组件（扰动器、冷却、终止条件）的 {@code init(problem)}
     * 完成问题绑定，因此传入的组件无需在外部预先调用 {@code init}。
     *
     * @param problem      待优化问题，不为 {@code null}
     * @param initializer  初始化器，负责生成初始解和初始温度
     * @param perturbation 扰动器，负责生成邻域候选解
     * @param cooling      冷却调度，负责更新温度
     * @param termination  终止条件，负责判断算法是否结束
     * @throws NullPointerException 如果任何参数为 {@code null}
     */
    public ModularSimulatedAnnealing(
        Prob problem ,
        Initializer<X,Prob> initializer,
        Perturbation<X,Prob> perturbation,
        CoolingSchedule<X,Prob> coolingSchedule,
        TerminationCondition<X,Prob> terminationCondition){
            this.problem = problem;
            this.initializer = initializer;
            this.perturbation = perturbation;
            this.coolingSchedule = coolingSchedule;
            this.terminationCondition = terminationCondition;
            initializer.init(problem);
            perturbation.init(problem);
            coolingSchedule.init(problem);
            terminationCondition.init(problem);
        }

    /**
     * 启动模拟退火优化过程，返回搜索到的最优解。
     * <p>
     * 算法流程：
     * <ol>
     *   <li>调用初始化器获得初始解 {@code currentX} 和初始温度。</li>
     *   <li>计算初始解的目标值，并将其记录为最优解。</li>
     *   <li>在每一轮迭代中：
     *     <ul>
     *       <li>调用扰动器生成候选解 {@code newX}。</li>
     *       <li>评估候选解的目标值。</li>
     *       <li>若候选解优于当前解，则无条件接受；否则根据 Metropolis 准则以概率接受。</li>
     *       <li>更新最优解记录（若当前解被接受且优于历史最优）。</li>
     *       <li>调用冷却策略降低温度。</li>
     *       <li>检查终止条件是否满足。</li>
     *     </ul>
     *   </li>
     *   <li>返回优化过程中发现的最优解（深拷贝，安全独立）。</li>
     * </ol>
     *
     * <h3>冷启动细节</h3>
     * 第一次迭代中，传递给各组件的 {@code isAccepted} 固定为 {@code false}。
     * 各组件（特别是扰动器和终止条件）需正确处理此初始状态。
     *
     * <h3>线程安全</h3>
     * 本方法未做任何同步，默认在单线程下使用。如果在多线程环境中调用，
     * 需由外部保证互斥。
     *
     * @return 优化过程中发现的最优解（独立拷贝，调用者可安全修改）
     */
    public X solve(){
        double temperature= initializer.initialTemperature();
        X currentX = initializer.initialX();
        double currentValue = problem.evaluate(currentX);

        X bestX = problem.copyX(currentX);
        double bestValue = currentValue;

        X newX = problem.copyX(currentX);
        double newValue;

        boolean isAccepted = false;

        while (!terminationCondition.check(temperature, currentX,isAccepted)) {
            
            newX = perturbation.perturb(temperature,currentX,isAccepted);

            newValue = problem.evaluate(newX);

            if(newValue<currentValue){
                currentX = newX;
                currentValue = newValue;
                isAccepted = true;

                if(currentValue<bestValue){
                    bestX = problem.copyX(currentX);
                    bestValue = currentValue;
                }
            }
            else{
                isAccepted = random.nextDouble()<Math.exp((currentValue-newValue)/temperature);
                if(isAccepted){
                    currentX = newX;
                    currentValue = newValue;
                }
            }
            temperature = coolingSchedule.cool(temperature,currentX,isAccepted);
        }
        
        return bestX;
    }
}