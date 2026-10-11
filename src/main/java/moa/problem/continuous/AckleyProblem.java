package moa.problem.continuous;

/**
 * Ackley 基准函数 —— 具有大量局部最优的多峰问题。
 *
 * <p>继承 ContinuousProblem。是否为最终类：是。
 *
 * <p>全局最优在 x = (0,...,0)，f(x) = 0。搜索范围为 [-32, 32]^n。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>纯函数 —— evaluate() 无副作用。</li>
 * </ul>
 *
 * <p><b>参见文档 2</b>
 */
public final class AckleyProblem extends ContinuousProblem {

    public AckleyProblem(int dimension) {
        super(createBounds(dimension, -32.0), createBounds(dimension, 32.0));
    }

    @Override
    public Double evaluate(double[] x) {
        int n = x.length;
        double sumSq = 0.0;
        double sumCos = 0.0;
        for (int i = 0; i < n; i++) {
            sumSq += x[i] * x[i];
            sumCos += Math.cos(2.0 * Math.PI * x[i]);
        }
        double term1 = -20.0 * Math.exp(-0.2 * Math.sqrt(sumSq / n));
        double term2 = -Math.exp(sumCos / n);
        return term1 + term2 + 20.0 + Math.E;
    }
}