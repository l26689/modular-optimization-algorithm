package oa.examples.continuousproblem;

import java.util.Random;

import oa.api.optimizationalgorithm.State;
import oa.api.spi.SearchOperator;

/**
 * 连续空间均匀邻域搜索算子，直接实现 SPI 层的 {@link SearchOperator} 接口。
 * <p>
 * 在当前解的邻域内以<b>均匀分布</b>随机采样一个候选解：对每个维度施加
 * ±10% 边界跨度的随机偏移，超出边界时裁剪到合法区间。
 * <p>
 * 本类不依赖任何算法特有接口（如 {@code SAPerturbation}），
 * 可被 SA、爬山法、随机搜索等任何需要邻域搜索的算法直接使用。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * SimulatedAnnealing<double[]> sa = new SimulatedAnnealing<>(
 *     problem,
 *     new RandomInitializer(new Random()),
 *     new ContinuousUniformSearch(),
 *     new SomeCoolingSchedule(),
 *     new SomeTerminationCondition()
 * );
 * }</pre>
 *
 * <h3>设计约束</h3>
 * <ul>
 *   <li>必须返回全新创建的对象，不得原地修改输入解。</li>
 *   <li>使用注入的 {@link Random} 实例进行所有随机操作，不得自行创建。</li>
 *   <li>扰动幅度为各维度边界跨度的 10%，可根据需要调整此比例。</li>
 * </ul>
 *
 * @see SearchOperator
 * @see ContinuousProblem
 */
public final class ContinuousUniformSearch implements SearchOperator<double[],ContinuousProblem,State<double[]>> {

    private double[] lowerBounds;
    private double[] upperBounds;
    private ContinuousProblem problem;
    private Random random;

    @Override
    public void init(ContinuousProblem problem,Random random) {
        this.lowerBounds = problem.getLowerBounds();
        this.upperBounds = problem.getUpperBounds();
        this.problem = problem;
        this.random = random;
    }

    /**
     * 在当前解的邻域内以均匀分布生成候选解。
     * <p>
     * 对每个维度施加 ±10% 边界跨度的随机偏移，超出边界时裁剪到合法区间。
     *
     * @param state 封装了当前迭代状态的 {@link State} 对象
     * @return 全新的候选解，与 {@code state.getCurrentXs()[0]} 相互独立
     */
    @Override
    public double[] search(State<double[]> state) {

        double[] x = state.getCurrentXs()[0];

        double[] newX = problem.copyX(x);

        for(int i = 0; i < newX.length; i++) {
            double delta = (upperBounds[i] - lowerBounds[i]) * 0.1;
            newX[i] += (random.nextDouble() - 0.5) * delta * 2;
            if(newX[i] < lowerBounds[i]) newX[i] = lowerBounds[i];
            if(newX[i] > upperBounds[i]) newX[i] = upperBounds[i];
        }

        return newX;
    }
}