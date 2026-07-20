package oa.examples.continuousproblem.rosenbrock;

import oa.examples.continuousproblem.ContinuousProblem;

// Rosenbrock 函数：经典的非凸函数，又称“香蕉函数”
// 公式：f(x) = Σ[100*(x_{i+1} - x_i²)² + (1 - x_i)²]
// 全局最优在 x=(1,1,...,1)，f(x)=0，山谷狭长难以收敛
public class RosenbrockProblem extends ContinuousProblem {

    public RosenbrockProblem(int dimension) {
        // 标准边界：[-2.048, 2.048]
        super(createBounds(dimension, -2.048), createBounds(dimension, 2.048));
    }

    @Override
    public Double evaluate(double[] x) {
        double sum = 0.0;
        for (int i = 0; i < x.length - 1; i++) {
            double a = x[i + 1] - x[i] * x[i];
            double b = 1.0 - x[i];
            sum += 100.0 * a * a + b * b;
        }
        return sum;
    }
}