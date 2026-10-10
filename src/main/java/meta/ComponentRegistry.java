package meta;

import oa.api.optimizationalgorithm.State;
import oa.api.problem.Problem;
import oa.api.spi.component.SearchOperator;
import oa.components.evaluate.AlgorithmBuilder;
import oa.components.problems.coninuousproblem.ContinuousProblem;
import oa.components.searchoperators.ContinuousUniformSearch;
import oa.components.searchoperators.GaussianPerturbation;
import oa.components.terminationcondition.MaxCallTerminationCondition;
import sa.basicsa.SimulatedAnnealing;
import sa.components.GeometricCoolingSchedule;
import sa.components.SABasicCoolingSchedule;
import sa.components.continuousproblem.SABasicInitializer;
import sa.core.SACoolingSchedule;
import sa.core.SAInitializer;
import sa.core.SAState;

/**
 * 组件注册表 —— <b>算法组合空间的唯一定义处</b>。
 * <p>
 * 元优化层能搜索哪些组件、各有什么参数，全部由本类集中声明。
 * 想让搜索空间包含新组件，只需在此登记，{@link ConfigCodec} 与
 * {@link MetaProblem} 的消费逻辑无需改动。
 *
 * <h3>当前可搜索的组件</h3>
 * <table border="1">
 *   <caption>注册表内容</caption>
 *   <tr><th>类别</th><th>编号</th><th>实现</th><th>参数</th></tr>
 *   <tr><td rowspan="2">冷却策略</td><td>0</td><td>GeometricCoolingSchedule</td>
 *       <td>每次调用温度乘以固定衰减率</td></tr>
 *   <tr><td>1</td><td>SABasicCoolingSchedule</td>
 *       <td>每 N 次调用降温一次（分段冷却）</td></tr>
 *   <tr><td rowspan="2">扰动器</td><td>0</td><td>ContinuousUniformSearch</td>
 *       <td>±10% 边界跨度内均匀采样（幅度不可调）</td></tr>
 *   <tr><td>1</td><td>GaussianPerturbation</td>
 *       <td>正态分布，标准差 = 比例 × 边界跨度</td></tr>
 * </table>
 *
 * <h3>⚠️ 每次运行都必须创建新实例</h3>
 * {@code SABasicCoolingSchedule} 与 {@code MaxCallTerminationCondition} 等组件
 * 内部持有可变状态（迭代计数、调用计数），<b>不可跨多次运行复用</b>。
 * 因此本类的工厂方法每次都返回全新实例，{@link MetaProblem} 在重复实验时
 * 也必须重新调用它们，而不是缓存组件对象。
 *
 * <h3>为什么扰动器有两个</h3>
 * 项目原有实现只有 {@code ContinuousUniformSearch} 一个扰动器。
 * 若组合空间中该维度只有一个取值，"搜索最优组合"在这一维上就是退化的，
 * 排名结果会失去意义。因此新增 {@code GaussianPerturbation}，
 * 使该维度成为真实的算法取舍（小步精修 vs 大步跳变）。
 */
public final class ComponentRegistry {

    /** 冷却策略：几何冷却，每次调用降温 */
    public static final int COOLING_GEOMETRIC = 0;
    /** 冷却策略：分段冷却，每 N 次调用降温一次 */
    public static final int COOLING_STEPPED = 1;
    /** 冷却策略可取值个数 */
    public static final int COOLING_TYPE_COUNT = 2;

    /** 扰动器：均匀扰动（幅度固定为 10% 边界跨度） */
    public static final int PERTURBATION_UNIFORM = 0;
    /** 扰动器：高斯扰动（幅度可调） */
    public static final int PERTURBATION_GAUSSIAN = 1;
    /** 扰动器可取值个数 */
    public static final int PERTURBATION_TYPE_COUNT = 2;

    private ComponentRegistry() {
    }

    /**
     * 创建初始化器。
     *
     * @param initialTemp 初始温度，必须为正
     * @return 全新的初始化器实例
     */
    public static SAInitializer<double[], ContinuousProblem> createInitializer(double initialTemp) {
        return new SABasicInitializer(initialTemp);
    }

    /**
     * 创建扰动器。
     *
     * @param type  扰动器类型，见本类常量
     * @param scale 高斯扰动的标准差比例；均匀扰动忽略此参数
     * @return 全新的扰动器实例
     * @throws IllegalArgumentException 类型编号非法
     */
    public static SearchOperator<double[], ContinuousProblem, State<double[]>> createPerturbation(
            int type, double scale) {
        switch (type) {
            case PERTURBATION_UNIFORM:
                return new ContinuousUniformSearch();
            case PERTURBATION_GAUSSIAN:
                return new GaussianPerturbation(scale);
            default:
                throw new IllegalArgumentException("未知的扰动器类型: " + type);
        }
    }

    /**
     * 创建冷却策略。
     *
     * @param type     冷却策略类型，见本类常量
     * @param rate     冷却率，取值 (0,1)
     * @param stepSize 分段冷却的步长；几何冷却忽略此参数
     * @return 全新的冷却策略实例
     * @throws IllegalArgumentException 类型编号非法
     */
    public static SACoolingSchedule<double[], Problem<double[]>, SAState<double[]>> createCooling(
            int type, double rate, int stepSize) {
        switch (type) {
            case COOLING_GEOMETRIC:
                return new GeometricCoolingSchedule(rate);
            case COOLING_STEPPED:
                return new SABasicCoolingSchedule<>(rate, stepSize);
            default:
                throw new IllegalArgumentException("未知的冷却策略类型: " + type);
        }
    }

    /**
     * 把配置组装成一个算法构造器（复用框架的 {@link AlgorithmBuilder} 抽象）。
     * <p>
     * 返回的构造器在每次被调用时才真正创建算法实例，因此每个重复实验都拿到
     * <b>全新的组件</b>——这是必须的，因为 {@code SABasicCoolingSchedule} 等
     * 组件内部持有可变状态，跨运行复用会导致计数残留、结果错误。
     *
     * @param config      待评估的算法组合
     * @param innerBudget 单次内层运行的迭代预算
     * @return 算法构造器，接受（问题, 随机源）并产出组装好的算法
     */
    public static AlgorithmBuilder<double[], ContinuousProblem> createAlgorithmBuilder(
            ConfigCodec.Config config, int innerBudget) {
        return (problem, random) -> new SimulatedAnnealing<double[], ContinuousProblem>(
                random,
                problem,
                createInitializer(config.initialTemp()),
                createPerturbation(config.perturbationType(), config.perturbationScale()),
                createCooling(config.coolingType(), config.coolingRate(), config.stepSize()),
                new MaxCallTerminationCondition<double[]>(innerBudget));
    }

    /**
     * 返回冷却策略的展示名。
     *
     * @param type 类型编号
     * @return 展示名
     */
    public static String coolingName(int type) {
        switch (type) {
            case COOLING_GEOMETRIC:
                return "几何冷却";
            case COOLING_STEPPED:
                return "分段冷却";
            default:
                return "未知冷却(" + type + ")";
        }
    }

    /**
     * 返回扰动器的展示名。
     *
     * @param type 类型编号
     * @return 展示名
     */
    public static String perturbationName(int type) {
        switch (type) {
            case PERTURBATION_UNIFORM:
                return "均匀扰动";
            case PERTURBATION_GAUSSIAN:
                return "高斯扰动";
            default:
                return "未知扰动(" + type + ")";
        }
    }
}
