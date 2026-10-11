package moa.problem.continuous;

// Rosenbrock 函数：经典的非凸函数，又称“香蕉函数”
// 公式：f(x) = Σ[100*(x_{i+1} - x_i²)² + (1 - x_i)²]
// 全局最优在 x=(1,1,...,1)，f(x)=0，山谷狭长难以收敛
/**
 * Rosenbrock 香蕉函数 —— 经典非凸优化基准。
 *
 * <p>继承 ContinuousProblem。是否为最终类：是。
 *
 * <p>全局最优在 x = (1,...,1)，f(x) = 0。搜索范围为 [-30, 30]^n，具有狭长弯曲的谷底。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>纯函数 —— evaluate() 无副作用。</li>
 * </ul>
 *
 * <p><b>参见文档 2</b>
 */
public final class RosenbrockProblem extends ContinuousProblem {

    public RosenbrockProblem(int dimension) {
        // 标准边界：[-2.048, 2.048]
        super(createBounds(dimension, -30), createBounds(dimension, 30));
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