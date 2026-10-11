package moa.problem.continuous.components;

import java.util.Random;

import moa.framework.api.Initializer;
import moa.framework.engine.State;
import moa.problem.continuous.ContinuousProblem;

/**
 * 随机初始化器 —— 在问题边界内均匀随机采样生成初始解。
 *
 * <p>实现 Initializer<X:double[], Prob:ContinuousProblem, S:State<double[]>>。是否为最终类：否。
 *
 * <p>每个维度在 [lowerBounds[i], upperBounds[i]) 内独立采样。字段声明为 protected，允许子类扩展。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>可复现 —— 使用 init 注入的 Random 实例。</li>
 * </ul>
 *
 * <p><b>参见文档 2</b>
 */
public class RandomInitializer<S extends State<double[]>> implements Initializer<double[],ContinuousProblem,S> {
    protected int dim;
    protected double[] lowerBounds;
    protected double[] upperBounds;
    protected Random random;
    
    @Override
    public void init(ContinuousProblem problem,Random random) {
        this.dim = problem.getDimension();
        this.lowerBounds = problem.getLowerBounds();
        this.upperBounds = problem.getUpperBounds();
        this.random = random;
    }
    
    @Override
    public double[] initialX() {
        double[] x = new double[dim];
        for(int i = 0; i < dim; i++) {
            double range = upperBounds[i] - lowerBounds[i];
            x[i] = lowerBounds[i] + random.nextDouble() * range;
        }
        return x;
    }
    
}
