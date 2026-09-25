package sa.components;

import sa.core.SACoolingSchedule;
import sa.core.SAState;
import java.util.Random;
import oa.api.problem.Problem;

/**
 * 自适应智能冷却策略，根据搜索过程中的解接受率动态调整降温速度与方向。
 *
 * <h2>原理</h2>
 * 模拟退火的核心矛盾在于：温度下降太快则早熟收敛到局部最优，下降太慢则浪费计算资源。
 * 理想的冷却曲线应当与搜索的"实际进展"同步——当搜索在广阔区域游走时缓慢降温以充分探索，
 * 当搜索已聚焦到有希望的区域时加速降温以精细收敛。
 *
 * <p>本策略通过<b>接受率</b>（accepted / total）来判断搜索当前所处的阶段：
 * <ul>
 *   <li><b>高接受率</b>（> 目标值的 1.5 倍）→ 候选解大量被接受，搜索过于"发散"，
 *       说明当前温度偏高，需要加速降温以收缩搜索范围</li>
 *   <li><b>正常接受率</b>（目标值的 0.5 ~ 1.5 倍区间）→ 搜索处于健康状态，
 *       按基准速率稳步降温，维持探索与利用的平衡</li>
 *   <li><b>低接受率</b>（< 目标值的 0.5 倍）→ 候选解几乎全被拒绝，搜索已经"卡死"，
 *       说明温度过低或已陷入局部最优，需要主动回火升温以跳出陷阱</li>
 * </ul>
 *
 * <h2>与标准几何冷却的对比</h2>
 * 标准几何冷却 T_{k+1} = α × T_k（α ∈ (0,1)）在整个搜索过程中使用固定的衰减因子，
 * 无法感知搜索状态。本策略通过实时反馈形成了<b>闭环控制</b>：温度不再只降不升，
 * 而是根据搜索的实际表现进行双向调节，类似于 PID 控制器中的比例调节。
 *
 * <h2>参数建议</h2>
 * <pre>
 * targetAcceptanceRatio = 0.44   // 经典文献中的"最优"接受率（对于许多问题）
 * baseCoolingRate       = 0.95   // 标准几何冷却速率
 * aggressiveCoolingRate = 0.85   // 加速冷却时的速率（乘数更小 = 降温更快）
 * reheatingFactor       = 1.5    // 回火时的升温倍数
 * windowSize            = 100    // 统计窗口大小
 * stepsPerTemperature   = 50     // 每个温度下的迭代步数（内循环长度）
 * </pre>
 * 回火温度上限<b>无需手动指定</b>：首次调用 {@link #cool} 时自动捕获当前温度作为初温，
 * 后续回火时温度不会超过此初温。
 *
 * <h3>冷启动</h3>
 * 首次调用时 {@code state.getIsAccepted()} 为 {@code false}，窗口统计从零开始累积，
 * 在首个窗口填满之前温度保持不变，不触发自适应逻辑。
 *
 * <h3>设计约束</h3>
 * <ul>
 *   <li>不修改传入的 {@link SAState} 或其内部解对象</li>
 *   <li>使用框架通过 {@link #init} 注入的 {@link Random} 实例（本策略无需随机采样）</li>
 *   <li>仅依赖 {@link SAState} 提供的温度与接受标志，遵循最少信息原则</li>
 * </ul>
 *
 * @param <X> 解的表示类型
 */
public final class SAAdaptiveCoolingSchedule<X> implements SACoolingSchedule<X, Problem<X>, SAState<X>> {

    /**
     * 目标接受率，取值范围 (0, 1)。
     * 经典文献中建议的"最优"接受率约为 0.44（即 44% 的候选解被接受），
     * 此时搜索在探索与利用之间取得最佳平衡。
     * 实际应用中可根据问题特征调整：多峰问题建议偏低（0.2~0.3），
     * 单峰/凸问题可偏高（0.5~0.6）。
     */
    private final double targetAcceptanceRatio;

    /**
     * 基准冷却速率，取值范围 (0, 1)。
     * 当接受率处于正常区间时使用此速率降温。典型值为 0.90~0.99，
     * 越接近 1 则降温越慢、搜索越充分但耗时越长。
     * 新温度 = 当前温度 × baseCoolingRate。
     */
    private final double baseCoolingRate;

    /**
     * 加速冷却速率，取值范围 (0, 1) 且必须小于 {@link #baseCoolingRate}。
     * 当接受率过高（搜索过于发散）时使用此速率快速降温以加速收敛。
     * 乘数越小降温越快，典型值为 0.80~0.90。
     * 新温度 = 当前温度 × aggressiveCoolingRate。
     */
    private final double aggressiveCoolingRate;

    /**
     * 回火因子，取值必须大于 1。
     * 当接受率过低（搜索陷入停滞）时，将当前温度乘以该因子进行升温，
     * 使算法重新获得跳出局部最优的能力。典型值为 1.2~2.0，
     * 过大的回火因子可能导致搜索发散。
     * 新温度 = 当前温度 × reheatingFactor。
     */
    private final double reheatingFactor;

    /**
     * 初始温度，在首次 {@link #cool} 调用时自动捕获。
     * 作为回火温度上限，防止回火时温度超过搜索起点。
     * 初始值为 -1 表示尚未捕获；每次 {@link #init} 后重置为 -1，
     * 确保新一轮求解时重新读取当前初温。
     */
    private double initialTemperature = -1;

    /**
     * 统计窗口大小，即用于计算接受率的最近 N 次迭代。
     * 窗口越大，接受率估计越稳定但响应越迟钝；
     * 窗口越小，响应越快但波动越大。典型值为 50~200。
     */
    private final int windowSize;

    /**
     * 每个温度下的迭代步数（内循环长度）。
     * 温度仅在每 stepsPerTemperature 次迭代后才可能被调整，
     * 这模拟了传统 SA 中"内循环"的概念——每个温度下进行多次扰动尝试。
     * 典型值为 10~100，取决于问题维度。
     */
    private final int stepsPerTemperature;

    /**
     * 当前统计窗口内被接受的候选解数量。
     * 每次 {@link #cool} 被调用时，若 {@link SAState#getIsAccepted()} 为 true 则加 1。
     * 窗口重置时清零。
     */
    private int acceptedCount;

    /**
     * 当前统计窗口内的总迭代次数。
     * 每次 {@link #cool} 被调用时加 1。窗口重置时清零。
     */
    private int totalCount;

    /**
     * 自 {@link #init} 或构造以来的总迭代次数计数器。
     * 用于判断当前是否到达温度调整点（每 stepsPerTemperature 次迭代调整一次）。
     */
    private int iterationCount;

    /**
     * 构造一个自适应冷却策略实例。
     *
     * @param targetAcceptanceRatio 目标接受率，典型值 0.44，范围 (0, 1)
     * @param baseCoolingRate       基准冷却速率，典型值 0.95，范围 (0, 1)
     * @param aggressiveCoolingRate 加速冷却速率，典型值 0.85，必须小于 baseCoolingRate
     * @param reheatingFactor       回火因子，典型值 1.5，必须 > 1
     * @param windowSize            统计窗口大小，典型值 100，用于计算接受率
     * @param stepsPerTemperature   每温度步数，典型值 50，控制温度调整频率
     * @throws IllegalArgumentException 如果任何参数不在合法范围内
     */
    public SAAdaptiveCoolingSchedule(double targetAcceptanceRatio, double baseCoolingRate,
                                      double aggressiveCoolingRate, double reheatingFactor,
                                      int windowSize, int stepsPerTemperature) {
        if (targetAcceptanceRatio <= 0 || targetAcceptanceRatio >= 1) {
            throw new IllegalArgumentException("目标接受率必须在 (0, 1) 区间内");
        }
        if (baseCoolingRate <= 0 || baseCoolingRate >= 1) {
            throw new IllegalArgumentException("基准冷却速率必须在 (0, 1) 区间内");
        }
        if (aggressiveCoolingRate <= 0 || aggressiveCoolingRate >= 1) {
            throw new IllegalArgumentException("加速冷却速率必须在 (0, 1) 区间内");
        }
        if (aggressiveCoolingRate >= baseCoolingRate) {
            throw new IllegalArgumentException("加速冷却速率必须小于基准冷却速率（乘数更小 = 降温更快）");
        }
        if (reheatingFactor <= 1) {
            throw new IllegalArgumentException("回火因子必须大于 1");
        }
        if (windowSize <= 0) {
            throw new IllegalArgumentException("窗口大小必须为正整数");
        }
        if (stepsPerTemperature <= 0) {
            throw new IllegalArgumentException("每温度步数必须为正整数");
        }
        this.targetAcceptanceRatio = targetAcceptanceRatio;
        this.baseCoolingRate = baseCoolingRate;
        this.aggressiveCoolingRate = aggressiveCoolingRate;
        this.reheatingFactor = reheatingFactor;
        this.windowSize = windowSize;
        this.stepsPerTemperature = stepsPerTemperature;
        this.acceptedCount = 0;
        this.totalCount = 0;
        this.iterationCount = 0;
    }

    /**
     * 初始化（重置）组件内部状态。
     * <p>
     * 由框架在算法启动前调用，将接受计数、总计数和迭代计数全部归零。
     * 注意：此方法<b>不改变</b>构造时设定的策略参数（冷却速率、窗口大小等），
     * 仅重置运行时累积的统计状态，使得同一实例可以在多次求解中复用。
     * <p>
     * 本策略不使用随机数，但保留 random 参数以符合 {@link SACoolingSchedule} 的接口契约。
     *
     * @param problem 待求解问题（本策略不直接使用，保留以符合接口契约）
     * @param random  框架注入的随机数生成器（本策略不直接使用）
     */
    @Override
    public void init(Problem<X> problem, Random random) {
        this.initialTemperature = -1;
        this.acceptedCount = 0;
        this.totalCount = 0;
        this.iterationCount = 0;
    }

    /**
     * 计算下一轮迭代的系统温度，实现自适应冷却逻辑。
     *
     * <h3>执行流程</h3>
     * <ol>
     *   <li>首次调用时自动捕获当前温度作为初温（回火上限）</li>
     *   <li>从 state 中读取当前温度和上轮接受标志（只读，不修改 state）</li>
     *   <li>更新接受计数和总计数（滑动窗口统计）</li>
     *   <li>若未到达温度调整点（iterationCount % stepsPerTemperature ≠ 0），
     *       返回当前温度不变——模拟"内循环"中温度恒定的行为</li>
     *   <li>若窗口未填满（totalCount < windowSize），返回当前温度不变——
     *       等待足够样本后再做统计决策（冷启动保护）</li>
     *   <li>计算窗口内的接受率 = acceptedCount / totalCount</li>
     *   <li>根据接受率落入的区间选择冷却模式：
     *     <ul>
     *       <li>接受率 < 目标的 50% → 回火升温（温度 × reheatingFactor，上限为初温）</li>
     *       <li>接受率 > 目标的 150% → 加速冷却（温度 × aggressiveCoolingRate）</li>
     *       <li>接受率在目标区间内  → 基准冷却（温度 × baseCoolingRate）</li>
     *     </ul>
     *   </li>
     *   <li>重置窗口统计，开始新一轮观测</li>
     * </ol>
     *
     * <h3>温度下限保护</h3>
     * 返回温度最低为 1e-10，防止温度降为零导致后续 Metropolis 准则中
     * 出现除零或 {@code exp(-∞) = 0} 的数值问题。
     *
     * @param state 封装了当前解、当前温度和接受标志的迭代状态对象
     * @return 新的温度值，保证大于 0
     */
    @Override
    public double cool(SAState<X> state) {
        double temperature = state.getTemperature();

        if (initialTemperature < 0) {
            initialTemperature = temperature;
        }

        iterationCount++;

        if (state.getIsAccepted()) {
            acceptedCount++;
        }
        totalCount++;

        if (iterationCount % stepsPerTemperature != 0) {
            return temperature;
        }

        if (totalCount < windowSize) {
            return temperature;
        }

        double acceptanceRatio = (double) acceptedCount / totalCount;

        double newTemperature;
        if (acceptanceRatio < targetAcceptanceRatio * 0.5) {
            newTemperature = Math.min(temperature * reheatingFactor, initialTemperature);
        } else if (acceptanceRatio > targetAcceptanceRatio * 1.5) {
            newTemperature = temperature * aggressiveCoolingRate;
        } else {
            newTemperature = temperature * baseCoolingRate;
        }

        acceptedCount = 0;
        totalCount = 0;

        return Math.max(newTemperature, 1e-10);
    }
}