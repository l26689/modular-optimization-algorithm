package pso.components.continuousproblem;

import java.util.Random;

import oa.examples.continuousproblem.ContinuousProblem;
import pso.core.PSOState;
import pso.core.Particle;

/**
 * 标准 PSO 粒子，针对 {@link ContinuousProblem} 的经典实现。
 * <p>
 * 每个粒子独立维护自身的位置、速度和个体历史最优（pBest），
 * 在每次 {@link #search(PSOState)} 调用中执行标准 PSO 速度-位置更新：
 * <pre>
 *   v = ω·v + c₁·r₁·(pBest - x) + c₂·r₂·(gBest - x)
 *   x = x + v
 * </pre>
 * 其中 gBest 由粒子自行从 {@link PSOState#getCurrentXs()} 中评估所有粒子位置得出，
 * 遵循最少信息原则，不依赖外部传入的全局最优。
 *
 * <h3>参数说明</h3>
 * <ul>
 *   <li>{@code ω}（惯性权重）：控制粒子保持当前速度的倾向，
 *       较大值有利于全局探索，较小值有利于局部精化</li>
 *   <li>{@code c₁}（认知系数）：控制粒子向自身历史最优移动的倾向</li>
 *   <li>{@code c₂}（社会系数）：控制粒子向群体全局最优移动的倾向</li>
 * </ul>
 * 常用参数组合：ω=0.729, c₁=c₂=1.49445（Clerc & Kennedy 2002 收缩因子）。
 *
 * <h3>边界处理</h3>
 * 位置更新后若超出 {@link ContinuousProblem#getLowerBounds()} /
 * {@link ContinuousProblem#getUpperBounds()} 定义的边界，将被裁剪到合法区间。
 * 速度不做边界限制，依靠惯性权重自然衰减。
 *
 * <h3>设计约束</h3>
 * <ul>
 *   <li>使用 {@code init} 阶段注入的 {@link Random} 实例进行所有随机操作。</li>
 *   <li>通过 {@link ContinuousProblem#copyX(double[])} 进行防御性拷贝，
 *       确保返回的解与内部状态相互独立。</li>
 *   <li>pBest 更新使用 {@link ContinuousProblem#compare(double[], double[])}
 *       统一判断优劣，兼容最小化和最大化问题。</li>
 *   <li>本类是 {@code final} 的，不期望被继承。JIT 可通过 CHA 优化去虚拟化调用。</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * ParticleSwarmOptimization<double[]> pso = new ParticleSwarmOptimization<>(
 *     problem,
 *     new Particle[] {
 *         new StandardPSOParticle(0.729, 1.49445, 1.49445),
 *         new StandardPSOParticle(0.729, 1.49445, 1.49445),
 *         // ... 更多粒子
 *     },
 *     new MaxCallTerminationCondition(10000)
 * );
 * }</pre>
 *
 * @see Particle
 * @see PSOState
 * @see ContinuousProblem
 */
public final class StandardPSOParticle
        implements Particle<double[], ContinuousProblem, PSOState<double[]>> {

    private double[] position;
    private double[] velocity;
    private double[] pBest;

    private final double omega;
    private final double c1;
    private final double c2;

    private double[] lowerBounds;
    private double[] upperBounds;
    private int dim;
    private ContinuousProblem problem;
    private Random random;

    /**
     * 构造一个标准 PSO 粒子。
     * <p>
     * 构造时仅存储算法参数，实际的维度、边界等元数据在
     * {@link #init(ContinuousProblem, Random)} 中获取。
     *
     * @param omega 惯性权重，通常取值 0.4~0.9
     * @param c1    认知系数，控制个体经验的影响
     * @param c2    社会系数，控制群体经验的影响
     */
    public StandardPSOParticle(double omega, double c1, double c2) {
        this.omega = omega;
        this.c1 = c1;
        this.c2 = c2;
    }

    /**
     * 绑定问题实例和随机源，获取维度、边界等元数据，初始化速度为零向量。
     * <p>
     * 速度数组在此处分配（而非构造时），因为维度信息只有绑定了问题才能获取。
     * 初始速度为零向量——粒子在首次迭代中会朝 pBest 和 gBest 方向移动。
     *
     * @param problem 连续优化问题实例
     * @param random  共享随机源
     */
    @Override
    public void init(ContinuousProblem problem, Random random) {
        this.problem = problem;
        this.random = random;
        this.dim = problem.getDimension();
        this.lowerBounds = problem.getLowerBounds();
        this.upperBounds = problem.getUpperBounds();
        this.velocity = new double[dim];
    }

    /**
     * 生成粒子的随机初始位置，在边界范围内均匀采样。
     * <p>
     * 同时初始化内部状态：{@code position} 和 {@code pBest} 均设为初始位置
     * 的防御性拷贝。返回给调用方的是原始数组（非拷贝），调用方拥有所有权。
     *
     * @return 随机初始位置（调用方拥有所有权）
     */
    @Override
    public double[] initialX() {
        double[] x = new double[dim];
        for (int i = 0; i < dim; i++) {
            double range = upperBounds[i] - lowerBounds[i];
            x[i] = lowerBounds[i] + random.nextDouble() * range;
        }
        this.position = problem.copyX(x);
        this.pBest = problem.copyX(x);
        return x;
    }

    /**
     * 执行标准 PSO 速度-位置更新，将粒子移动到新位置。
     * <p>
     * 更新公式（按维度独立计算）：
     * <pre>
     *   v_d = ω·v_d + c₁·r₁·(pBest_d - x_d) + c₂·r₂·(gBest_d - x_d)
     *   x_d = x_d + v_d
     * </pre>
     * 其中 gBest 由 {@link #findGBest(PSOState)} 从群体状态中自行评估得出。
     * 位置直接原地更新，不分配临时数组。更新后若新位置优于 pBest，则更新 pBest。
     * <p>
     * 返回的是新位置的防御性拷贝，确保调用方无法修改粒子内部状态。
     *
     * @param state 当前群体状态，包含所有粒子的位置
     * @return 新位置（防御性拷贝，调用方拥有所有权）
     */
    @Override
    public double[] search(PSOState<double[]> state) {
        double[] gBest = findGBest(state);

        for (int d = 0; d < dim; d++) {
            double r1 = random.nextDouble();
            double r2 = random.nextDouble();
            velocity[d] = omega * velocity[d]
                    + c1 * r1 * (pBest[d] - position[d])
                    + c2 * r2 * (gBest[d] - position[d]);
            position[d] += velocity[d];
            if (position[d] < lowerBounds[d]) {
                position[d] = lowerBounds[d];
            }
            if (position[d] > upperBounds[d]) {
                position[d] = upperBounds[d];
            }
        }

        if (problem.compare(position, pBest) > 0) {
            this.pBest = problem.copyX(position);
        }

        return problem.copyX(position);
    }

    /**
     * 从当前群体状态中找出全局最优粒子。
     * <p>
     * 遍历 {@link PSOState#getCurrentXs()} 中的所有粒子位置，
     * 通过 {@link ContinuousProblem#compare(double[], double[])} 统一比较，
     * 返回目标值最优的位置引用。
     *
     * @param state 当前群体状态，包含所有粒子的位置
     * @return 全局最优位置（直接引用，非拷贝；调用方应只读使用）
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
}