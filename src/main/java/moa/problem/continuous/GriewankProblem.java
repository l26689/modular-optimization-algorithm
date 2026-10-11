package moa.problem.continuous;

/**
 * Griewank 基准函数 —— 多峰但全局结构简单的问题。
 *
 * <p>继承 ContinuousProblem。是否为最终类：是。
 *
 * <p>全局最优在 x = (0,...,0)，f(x) = 0。搜索范围为 [-600, 600]^n。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>纯函数 —— evaluate() 无副作用。</li>
 * </ul>
 *
 * <p><b>参见文档 2</b>
 */
public final class GriewankProblem extends ContinuousProblem {

    public GriewankProblem(int dimension) {
        super(createBounds(dimension, -32.0), createBounds(dimension, 32.0));
    }

    @Override
    public Double evaluate(double[] x) {
        double sum = 0.0;
        double product = 1.0;
        for (int i = 0; i < x.length; i++) {
            sum += x[i] * x[i] / 4000.0;
            product *= Math.cos(x[i] / Math.sqrt(i + 1));
        }
        return sum - product + 1.0;
    }
}