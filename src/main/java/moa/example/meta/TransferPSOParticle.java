package moa.example.meta;

import java.util.Arrays;
import java.util.Random;

import moa.algorithm.pso.api.PSOState;
import moa.algorithm.pso.api.Particle;

/**
 * 带"跳槽"机制的 PSO 粒子——本项目的核心机制就在这里。
 * <p>
 * 前半段和标准 PSO 粒子完全一样（速度-位置更新 + pBest 记录），
 * 差别只在最后多了一步判定：算出自己当前配置有多差，按差的程度决定
 * 要不要换一个算法支重新开始。
 *
 * <h3>它在一轮迭代里的完整流程</h3>
 * <ol>
 *   <li>从群体状态里找出 gBest（和 {@code StandardPSOParticle} 一样的做法）；</li>
 *   <li>标准速度-位置更新（对<b>所有</b>维都更新，包括当前用不到的支的参数维）；</li>
 *   <li>解出当前位置落在哪个算法支上；</li>
 *   <li>算自己当前位置的性能；</li>
 *   <li><b>跳槽判定</b>：按"混合性能"算出跳槽概率，掷骰子；</li>
 *   <li>命中则<b>局部重启</b>（换支 + 目标支参数重采样 + 速度清零 + pBest 清空）；
 *       未命中则照常更新 pBest。</li>
 * </ol>
 *
 * <h3>性能怎么算（按用户要求：pBest 权重大）</h3>
 * <pre>
 *   混合性能 = W_CURRENT × 当前位置性能 + W_PBEST × pBest 性能
 * </pre>
 * 用混合值而不是纯当前位置，是为了减轻"一次倒霉的位置就把粒子踢走"的抖动；
 * pBest 占大头（0.8）意味着粒子的历史成绩是主要依据。
 *
 * <h3>概率怎么算</h3>
 * 不能直接拿目标值当概率（尺度随问题变化，没法定阈值），所以用<b>群内排名</b>：
 * 数一数群里有几个粒子的当前性能比自己好，比例越大说明自己越差，
 * 跳槽概率就越高。概率被钳在 [P_JUMP_MIN, P_JUMP_MAX]（见 MetaPSOConfig）：
 * <ul>
 *   <li>下限保证最好的粒子也会偶尔探索别的支（否则"跨算法联系"就没了）；</li>
 *   <li>上限保证差的粒子不会每轮都跳（否则速度更新积累不起来，
 *       PSO 退化成随机重启）。</li>
 * </ul>
 *
 * <h3>已知问题：正反馈风险</h3>
 * "差的都跳向表现好的支"是个贪心个体策略，会自我强化——早期某支稍领先，
 * 差粒子可能全涌过去，导致群坍缩成一支、多样性丢失（这在文献里叫
 * 过早利用 / early exploitation）。目前靠概率上下限缓解。
 * 若要进一步抑制，可以考虑：把"随机挑一支"改成"偏向但不必选最优支"，
 * 或者给"刚刚跳过的支"加一个冷却期（本类没有实现）。
 *
 * @see MetaPSOConfig
 * @see ParameterSpaceProblem
 */
public final class TransferPSOParticle
        implements Particle<double[], ParameterSpaceProblem, PSOState<double[]>> {

    // ---- 框架注入的上下文 ----
    private ParameterSpaceProblem problem;
    private Random random;
    private int dim;
    private double[] lowerBounds;
    private double[] upperBounds;

    // ---- 粒子自身状态 ----
    /** 当前位置。 */
    private double[] position;
    /** 当前速度。跳槽时会被清零。 */
    private double[] velocity;
    /** 个体历史最优位置（可能是另一个算法支上的配置）。 */
    private double[] pBest;
    /** pBest 对应的性能，避免重复求值。 */
    private double pBestPerf;
    /** 当前位置落在哪个算法支上（由坐标解出，不作为独立状态维护）。 */
    private int branch;

    /** 跳槽次数，仅供观察机制是否在工作。 */
    private int jumpCount;

    @Override
    public void init(ParameterSpaceProblem problem, Random random) {
        this.problem = problem;
        this.random = random;
        this.dim = problem.getDimension();
        this.lowerBounds = problem.getLowerBounds();
        this.upperBounds = problem.getUpperBounds();
        this.velocity = new double[dim];
    }

    /**
     * 生成初始位置：所有维在范围内均匀采样，算法支随机。
     * <p>
     * 返回值是原始数组（所有权交给调用方，和框架其他粒子的约定一致）；
     * 内部保存的是副本，以免外部改动影响粒子状态。
     */
    @Override
    public double[] initialX() {
        double[] x = MetaPSOConfig.randomCoord(random);
        this.position = x.clone();
        this.branch = MetaPSOConfig.branchOf(x);
        this.pBest = x.clone();
        this.pBestPerf = problem.evaluate(pBest);
        return x;
    }

    /**
     * 执行一步：标准 PSO 更新 + 跳槽判定。
     *
     * @param state 当前群体状态（含所有粒子的位置）
     * @return 新位置（副本，外部拥有所有权）
     */
    @Override
    public double[] search(PSOState<double[]> state) {
        double[] gBest = findGBest(state);

        // ---- 第一步：标准速度-位置更新 ----
        // 对所有维都更新，包括当前用不到的支的参数维。这不是浪费：
        // 那些维在跳槽过去时就是"预置"的初始值（虽然是不受约束的漂移值，
        // 所以跳槽时还会重新采样一次，见 jump()）。
        for (int d = 0; d < dim; d++) {
            double r1 = random.nextDouble();
            double r2 = random.nextDouble();
            velocity[d] = MetaPSOConfig.META_OMEGA * velocity[d]
                    + MetaPSOConfig.META_C1 * r1 * (pBest[d] - position[d])
                    + MetaPSOConfig.META_C2 * r2 * (gBest[d] - position[d]);
            position[d] += velocity[d];
            // 位置越界就裁剪（速度不限制，靠 ω 自然衰减）
            if (position[d] < lowerBounds[d]) position[d] = lowerBounds[d];
            if (position[d] > upperBounds[d]) position[d] = upperBounds[d];
        }

        // ---- 第二步：看看现在落在哪一支、表现如何 ----
        branch = MetaPSOConfig.branchOf(position);
        double currentPerf = problem.evaluate(position);

        // ---- 第三步：跳槽判定 ----
        double pJump = jumpProbability(state, currentPerf);
        if (random.nextDouble() < pJump) {
            jump();
        } else if (problem.compare(position, pBest) > 0) {
            // 没跳槽就正常更新个体历史最优
            pBest = problem.copyX(position);
            pBestPerf = currentPerf;
        }

        return problem.copyX(position);
    }

    // =====================================================================
    // 跳槽机制
    // =====================================================================

    /**
     * 算跳槽概率。
     * <p>
     * 想改判定规则的话，主要就改这个方法。这里把"排名"直接取成
     * "群里有多少粒子的当前性能比我好"的占比，你也可以换成：
     * <ul>
     *   <li>只用 pBest 性能排（更稳，但反应慢）；</li>
     *   <li>用绝对阈值（简单，但换个问题就要重调）；</li>
     *   <li>加个温度做 logistic 映射（和 SA 的 Metropolis 呼应，更优雅）。</li>
     * </ul>
     *
     * @param state       当前群体状态
     * @param currentPerf 当前位置的性能
     * @return 跳槽概率，已钳在 [P_JUMP_MIN, P_JUMP_MAX]
     */
    private double jumpProbability(PSOState<double[]> state, double currentPerf) {
        // 混合性能：pBest 占大头，减少单次坏位置的抖动
        double blended = MetaPSOConfig.W_CURRENT * currentPerf
                       + MetaPSOConfig.W_PBEST * pBestPerf;

        // 群内排名：数有多少粒子的当前配置严格优于我的混合值
        // （这里的每次 evaluate 都命中缓存，因为找 gBest 时已经把这些位置算过了）
        double[][] positions = state.getCurrentXs();
        int better = 0;
        for (double[] p : positions) {
            if (problem.evaluate(p) < blended) {
                better++;
            }
        }

        int n = positions.length;
        // better 最多是 n-1（自己的当前位置不会优于 blended，因为 blended 在
        // currentPerf 和 pBestPerf 之间，而 pBestPerf 不劣于 currentPerf）
        double ratio = (n <= 1) ? 0.0 : (double) better / (n - 1);

        return MetaPSOConfig.P_JUMP_MIN
             + (MetaPSOConfig.P_JUMP_MAX - MetaPSOConfig.P_JUMP_MIN) * ratio;
    }

    /**
     * 跳槽 = <b>局部重启</b>。
     * <p>
     * 四件事一起做，缺一不可：
     * <ol>
     *   <li><b>换支 + 目标支参数重采样</b>：不能继承旧值。PSO 对所有维都在更新，
     *       旧值是没有适应度约束的漂移值，很可能落在目标支的坏区间
     *       （比如降温率 1.7），直接继承会导致跳一次就崩、然后被踢回来。</li>
     *   <li><b>速度清零</b>：不清的话，旧支积累的动量会推着粒子在新支里继续冲，
     *       语义上不成立（新支的参数含义完全不同）。</li>
     *   <li><b>pBest 清空为新位置</b>（按用户选择）：真正的"局部重启"，
     *       彻底忘记旧支的历史。</li>
     *   <li>更新 pBestPerf，避免下次判定用到过期的分数。</li>
     * </ol>
     * <p>
     * 注意：其它支的参数维<b>保留不动</b>，以后跳回去还能用。
     */
    private void jump() {
        int target = randomOtherBranch();
        MetaPSOConfig.resampleBranchDims(position, target, random);
        Arrays.fill(velocity, 0.0);
        branch = target;
        pBest = problem.copyX(position);
        pBestPerf = problem.evaluate(pBest);
        jumpCount++;
    }

    /**
     * 随机挑一个"别的"可运行支。
     * <p>
     * 只从 {@link MetaPSOConfig#RUNNABLE_BRANCHES} 里挑（目前不含 GA，
     * 因为 GA 装配不起来、跳过去只会拿到恒定的惩罚值）。
     * <p>
     * 可调点：目前是等概率随机。一个可能的改进是"偏向当前 gBest 所在的支"，
     * 但那会显著加强正反馈（群更容易坍缩成一支），慎用。
     */
    private int randomOtherBranch() {
        int[] candidates = MetaPSOConfig.RUNNABLE_BRANCHES;
        int alternatives = 0;
        for (int b : candidates) {
            if (b != branch) alternatives++;
        }
        if (alternatives == 0) {
            return branch;      // 没有别的支可跳，原地不动
        }
        int pick = random.nextInt(alternatives);
        for (int b : candidates) {
            if (b != branch) {
                if (pick == 0) return b;
                pick--;
            }
        }
        return branch;          // 理论上到不了这里
    }

    // =====================================================================
    // 与 StandardPSOParticle 相同的部分
    // =====================================================================

    /**
     * 从群体状态里找全局最优位置。
     * <p>
     * 和 {@code StandardPSOParticle} 一样，gBest 是从<b>当前位置</b>里现算的
     * （不含各粒子的 pBest），且每个粒子每轮都独立算一遍。
     * 因为有缓存，重复求值不额外花钱，所以这里不做优化。
     */
    private double[] findGBest(PSOState<double[]> state) {
        double[][] positions = state.getCurrentXs();
        double[] best = positions[0];
        for (int i = 1; i < positions.length; i++) {
            if (problem.compare(positions[i], best) > 0) {
                best = positions[i];
            }
        }
        return best;
    }

    // =====================================================================
    // 仅供观察
    // =====================================================================

    /** 该粒子累计跳槽了多少次。 */
    public int getJumpCount() {
        return jumpCount;
    }

    /** 该粒子当前所在的算法支（外部观察用，可能已经过时——每轮都会被重新解出）。 */
    public int getBranch() {
        return branch;
    }
}
