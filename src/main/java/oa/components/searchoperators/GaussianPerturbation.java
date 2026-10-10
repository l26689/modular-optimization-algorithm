package oa.components.searchoperators;

import java.util.Random;

import oa.api.optimizationalgorithm.State;
import oa.api.spi.component.SearchOperator;
import oa.components.problems.coninuousproblem.ContinuousProblem;

/**
 * 高斯邻域扰动算子。
 * <p>
 * 与 {@link ContinuousUniformSearch}（在 ±10% 边界跨度内<b>均匀</b>采样）相对，
 * 本类在每维施加服从正态分布的偏移，标准差为边界跨度的 {@code scaleFactor} 倍。
 * 高斯分布的性质是<b>小步长高概率、大步长低概率</b>，在模拟退火低温阶段
 * 更倾向于在邻域内做精细搜索，而均匀分布则会以相同概率产生大步跳变。
 * 两者属于真实的算法设计取舍，因此本类被注册进元优化框架的组件注册表，
 * 供"自动搜索最优算法组合"时选用。
 *
 * <h3>参数</h3>
 * <ul>
 *   <li>{@code scaleFactor} —— 标准差相对边界跨度的比例。
 *       取值越大，探索范围越广；越小则越接近局部精修。</li>
 * </ul>
 *
 * <h3>设计约束</h3>
 * <ul>
 *   <li>必须返回全新创建的对象，不得原地修改输入解。</li>
 *   <li>使用注入的 {@link Random} 实例，不得自行创建随机数生成器。</li>
 *   <li>越界坐标被裁剪回合法区间。</li>
 * </ul>
 *
 * @see ContinuousUniformSearch
 * @see SearchOperator
 */
public final class GaussianPerturbation
        implements SearchOperator<double[], ContinuousProblem, State<double[]>> {

    private final double scaleFactor;

    private ContinuousProblem problem;
    private double[] lowerBounds;
    private double[] upperBounds;
    private Random random;

    /**
     * 构造一个高斯扰动算子。
     *
     * @param scaleFactor 标准差相对边界跨度的比例，必须为正
     * @throws IllegalArgumentException 若 {@code scaleFactor <= 0}
     */
    public GaussianPerturbation(double scaleFactor) {
        if (!(scaleFactor > 0.0)) {
            throw new IllegalArgumentException("scaleFactor 必须为正，当前为 " + scaleFactor);
        }
        this.scaleFactor = scaleFactor;
    }

    /** 返回本扰动器的标准差比例，供结果展示使用。 */
    public double scaleFactor() {
        return scaleFactor;
    }

    @Override
    public void init(ContinuousProblem problem, Random random) {
        this.problem = problem;
        this.lowerBounds = problem.getLowerBounds();
        this.upperBounds = problem.getUpperBounds();
        this.random = random;
    }

    /**
     * 在当前解附近按正态分布生成候选解。
     *
     * @param state 封装当前迭代状态的对象
     * @return 全新的候选解，与输入解相互独立
     */
    @Override
    public double[] search(State<double[]> state) {
        double[] x = state.getCurrentXs()[0];
        double[] newX = problem.copyX(x);

        for (int i = 0; i < newX.length; i++) {
            double sigma = (upperBounds[i] - lowerBounds[i]) * scaleFactor;
            newX[i] += random.nextGaussian() * sigma;
            if (newX[i] < lowerBounds[i]) newX[i] = lowerBounds[i];
            if (newX[i] > upperBounds[i]) newX[i] = upperBounds[i];
        }

        return newX;
    }
}
