package moa.problem.continuous;

/**
 * Rastrigin 基准函数 —— 基于余弦调制的高度多峰问题。
 *
 * <p>继承 ContinuousProblem。是否为最终类：是。
 *
 * <p>全局最优在 x = (0,...,0)，f(x) = 0。搜索范围为 [-5.12, 5.12]^n。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>纯函数 —— evaluate() 无副作用。</li>
 * </ul>
 *
 * <p><b>参见文档 2</b>
 */
public final class RastriginProblem extends ContinuousProblem {

    public RastriginProblem(int dimension) {
        super(createBounds(dimension, -5.12), createBounds(dimension, 5.12));
    }

    @Override
    public Double evaluate(double[] x) {
        double sum = 10.0 * x.length;
        for (int i = 0; i < x.length; i++) {
            sum += x[i] * x[i] - 10.0 * Math.cos(2.0 * Math.PI * x[i]);
        }
        return sum;
    }
}