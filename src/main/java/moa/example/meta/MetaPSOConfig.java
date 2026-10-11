package moa.example.meta;

import java.util.Random;

/**
 * 元优化器的<b>唯一调参入口</b>。
 * <p>
 * 这个类只放常量和纯工具方法，不含任何逻辑。想调行为就改这里的常量，
 * 大部分都不需要动别处的代码（个别常量改完要连带调整坐标范围，见各自注释）。
 * <p>
 * 坐标布局：外层"伪 PSO"在 8 维空间里飞，一个坐标 = 一整套算法配置。
 * 下标 0 是算法种类，其余下标是各算法的参数。选定了某一支之后，
 * 其它支的参数维会被忽略（但仍在被 PSO 演化，跳槽时可能用得上）。
 */
public final class MetaPSOConfig {

    private MetaPSOConfig() {
        // 纯常量类，不实例化
    }

    // =====================================================================
    // 一、坐标布局：下标 → 参数
    // =====================================================================
    // 改坐标维度时，必须同步修改：DIM、LOWER[]、UPPER[]、下面的 *_PARAMS 数组、
    // 以及 BranchAlgorithmBuilder 里对应的解码代码。四处要一致。
    // =====================================================================

    /** 算法种类（离散）：0=SA, 1=PSO, 2=DE, 3=GA。 */
    public static final int I_TYPE = 0;
    /** SA 初始温度。 */
    public static final int I_SA_TEMP = 1;
    /** SA 降温率（每 SA_LADDER 轮乘一次）。 */
    public static final int I_SA_COOL_RATE = 2;
    /** PSO 惯性权重 ω。 */
    public static final int I_PSO_OMEGA = 3;
    /** PSO 认知系数 c1。 */
    public static final int I_PSO_C1 = 4;
    /** PSO 社会系数 c2。 */
    public static final int I_PSO_C2 = 5;
    /** DE 缩放因子 F。 */
    public static final int I_DE_F = 6;
    /** DE 交叉概率 CR。 */
    public static final int I_DE_CR = 7;
    /** 坐标总维度。 */
    public static final int DIM = 8;

    // =====================================================================
    // 二、坐标取值范围
    // =====================================================================
    // 注意：SA 的组件【不做任何参数校验】（SABasicCoolingSchedule / SABasicInitializer
    // 构造函数里没有检查），所以语义边界完全由这两个数组守住。比如降温率必须 < 1，
    // 初始温度必须 > 0，否则 SA 会安静地给出错误结果而不报错。
    // =====================================================================

    /** 各维下界，下标与上面 I_* 常量一一对应。 */
    public static final double[] LOWER = {
            0.0,     // I_TYPE      算法编号，0..3
            0.1,     // I_SA_TEMP   初始温度必须为正
            0.001,   // I_SA_COOL_RATE  必须 (0,1)，等于 1 表示不降温
            0.4,     // I_PSO_OMEGA 经验区间 0.4~0.9
            0.5,     // I_PSO_C1
            0.5,     // I_PSO_C2
            0.1,     // I_DE_F      F 太小则变异步长不足
            0.0      // I_DE_CR
    };

    /** 各维上界，下标与上面 I_* 常量一一对应。 */
    public static final double[] UPPER = {
            3.0,     // I_TYPE
            1000.0,  // I_SA_TEMP
            0.999,   // I_SA_COOL_RATE  必须 < 1
            0.9,     // I_PSO_OMEGA
            2.5,     // I_PSO_C1
            2.5,     // I_PSO_C2
            2.0,     // I_DE_F      经典取值 0.5 左右，上限给 2 留探索空间
            1.0      // I_DE_CR
    };

    // =====================================================================
    // 三、算法分支编号
    // =====================================================================

    public static final int BRANCH_SA = 0;
    public static final int BRANCH_PSO = 1;
    public static final int BRANCH_DE = 2;
    public static final int BRANCH_GA = 3;
    /** 分支总数（= I_TYPE 的合法取值个数）。 */
    public static final int BRANCH_COUNT = 4;

    /**
     * 【重要】目前真正能跑起来的分支。
     * <p>
     * GA 不在其中：框架里 GA 的引擎 BasicGA 存在，但 Selection / Crossover /
     * Mutation / Replacement / GeneType 全都只有接口、没有任何实现类，
     * BasicGA 因此装配不起来（见 BranchAlgorithmBuilder 里的占位分支）。
     * <p>
     * 跳槽的目标支只从这个数组里挑，避免粒子白白跳到跑不了的 GA 支上、
     * 拿一个恒定的惩罚值再跳回来，浪费迭代。等你补上 GA 组件后，
     * 把 BRANCH_GA 加进来即可。
     */
    public static final int[] RUNNABLE_BRANCHES = { BRANCH_SA, BRANCH_PSO, BRANCH_DE };

    /** 各分支用到的坐标下标，跳槽重采样和范围钳制都靠它。 */
    private static final int[] SA_PARAMS = { I_SA_TEMP, I_SA_COOL_RATE };
    private static final int[] PSO_PARAMS = { I_PSO_OMEGA, I_PSO_C1, I_PSO_C2 };
    private static final int[] DE_PARAMS = { I_DE_F, I_DE_CR };
    private static final int[] GA_PARAMS = { };

    // =====================================================================
    // 四、跳槽机制的可调参数
    // =====================================================================

    /**
     * 跳槽概率下限。必须有下限：否则表现最好的粒子永远不会探索别的支，
     * "跨算法联系"就退化成各支孤立搜索。典型 0.001~0.01。
     */
    public static final double P_JUMP_MIN = 0.001;

    /**
     * 跳槽概率上限。必须有上限：否则差的粒子每轮都跳，
     * 整个 PSO 退化成随机重启（速度更新积累不起来）。典型 0.1~0.3。
     */
    public static final double P_JUMP_MAX = 0.3;

    /**
     * 混合性能里"当前位置性能"的权重。<b>必须与 W_PBEST 之和为 1。</b>
     * 取小了会让跳槽判断更看重历史（更稳、更不容易被一次坏位置踢走）。
     */
    public static final double W_CURRENT = 0.2;

    /**
     * 混合性能里"pBest 性能"的权重。按用户要求 pBest 占大头（0.8）。
     * 把 W_PBEST 调大 → 粒子更"念旧"，跳槽更少。
     */
    public static final double W_PBEST = 0.8;

    // =====================================================================
    // 五、外层元 PSO 的参数（调参器自己的参数，固定值即可）
    // =====================================================================

    /** 外层惯性权重。 */
    public static final double META_OMEGA = 0.729;
    /** 外层认知系数。 */
    public static final double META_C1 = 1.49445;
    /** 外层社会系数。 */
    public static final double META_C2 = 1.49445;
    /** 外层粒子数（= 每轮最多评估的配置数）。 */
    public static final int META_SWARM_SIZE = 50;
    /** 外层迭代轮数（= MaxCallTerminationCondition 的 maxCalls）。 */
    public static final int META_ITERATIONS = 100;

    // =====================================================================
    // 六、内层被评估算法的参数（固定值，不参与搜索）
    // =====================================================================
    // 这些量【直接决定评估成本】，一旦进了坐标，不同配置的"预算"就不同，
    // 性能之间失去可比性，"最优组合"这个结论也就不成立。所以固定在这里。
    // =====================================================================

    /** 内层群规模（PSO / DE 用）。代价约是它的平方，别设大。 */
    public static final int INNER_POP = 5;
    /** 内层算法的评估次数预算（= 目标函数 evaluate 被调用的总次数）。
     *  三种算法按此对齐算力：SA 每轮约 4 次、DE 约 20 次、PSO 约 60 次，
     *  同样的预算下它们各自跑的轮数不同，这才公平。 */
    public static final int INNER_BUDGET = 2000;
    /** SA 的冷却阶梯长度：每多少轮降一次温。 */
    public static final int SA_LADDER = 10;
    /** 每组配置重复跑几遍取平均（AlgorithmEvaluationProblem 的 runs）。 */
    public static final int RUNS = 3;

    /** 全局随机种子，固定以保证结果可复现。 */
    public static final long SEED = 42L;

    // =====================================================================
    // 七、工具方法
    // =====================================================================

    /**
     * 从坐标里解出算法分支编号。
     * <p>
     * 离散维做了<b>钳制</b>而不是报错：PSO 会把这一维飞到 1.7、-0.3 之类的值，
     * 四舍五入后再夹到 [0, BRANCH_COUNT-1]。钳制比抛异常好——抛异常会让
     * 粒子越界一次就被判死刑（见 ParameterSpaceProblem 的惩罚值），
     * 白白浪费搜索力。
     */
    public static int branchOf(double[] coord) {
        int rounded = (int) Math.round(coord[I_TYPE]);
        if (rounded < 0) return 0;
        if (rounded > BRANCH_COUNT - 1) return BRANCH_COUNT - 1;
        return rounded;
    }

    /** 各维的显示名，下标与 I_* 常量一一对应（只用于打印，不影响逻辑）。 */
    public static final String[] DIM_NAMES = {
            "algorithmType", "saInitTemp", "saCoolingRate",
            "psoOmega", "psoC1", "psoC2", "deF", "deCR"
    };

    /** 分支编号 → 名字（只用于打印）。 */
    public static String branchName(int branch) {
        switch (branch) {
            case BRANCH_SA:  return "SA";
            case BRANCH_PSO: return "PSO";
            case BRANCH_DE:  return "DE";
            case BRANCH_GA:  return "GA";
            default:         return "未知(" + branch + ")";
        }
    }

    /** 取某一分支占用的坐标下标。返回的数组不要改。 */
    public static int[] paramsOf(int branch) {
        switch (branch) {
            case BRANCH_SA:  return SA_PARAMS;
            case BRANCH_PSO: return PSO_PARAMS;
            case BRANCH_DE:  return DE_PARAMS;
            default:         return GA_PARAMS;
        }
    }

    /**
     * 生成一个全新的随机坐标（所有维都在范围内均匀采样，算法支随机）。
     * <p>
     * 注意：被忽略的支的参数维也照样采样，因为 PSO 对所有维都做速度更新，
     * 留着随机的初值比留 0 好（跳槽过去时至少不是个退化的值）。
     */
    public static double[] randomCoord(Random random) {
        double[] coord = new double[DIM];
        for (int i = 0; i < DIM; i++) {
            coord[i] = LOWER[i] + random.nextDouble() * (UPPER[i] - LOWER[i]);
        }
        coord[I_TYPE] = RUNNABLE_BRANCHES[random.nextInt(RUNNABLE_BRANCHES.length)];
        return coord;
    }

    /**
     * 跳槽用的<b>局部重启</b>：把坐标改成目标支，并把该支的参数维在范围内重新采样。
     * <p>
     * 为什么必须重采样、而不能沿用旧值：PSO 对所有维都在做速度更新，
     * 所以一个粒子在 PSO 支跑着的时候，它的 SA 参数维也被悄悄演化过，
     * 但那是不受适应度约束的"漂移值"，很可能落在目标支的坏区间里
     * （比如降温率 1.7）。直接继承过去会导致跳一次表现就崩，
     * 然后又被踢回来，来回震荡。
     * <p>
     * 其他支的参数维保持不动（以后跳回来还能用）。
     *
     * @param coord  原地修改的坐标
     * @param branch 要跳到的目标支
     */
    public static void resampleBranchDims(double[] coord, int branch, Random random) {
        coord[I_TYPE] = branch;
        for (int i : paramsOf(branch)) {
            coord[i] = LOWER[i] + random.nextDouble() * (UPPER[i] - LOWER[i]);
        }
    }
}
