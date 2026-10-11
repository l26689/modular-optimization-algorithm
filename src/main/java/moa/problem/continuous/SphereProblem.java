package moa.problem.continuous;

/**
 * Sphere 基准函数 —— 最简单的连续优化测试函数。
 *
 * <p>继承 ContinuousProblem。是否为最终类：是。
 *
 * <p>全局最优在 x = (0,...,0)，f(x) = 0。搜索范围为 [-100, 100]^n，单峰凸函数。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>纯函数 —— evaluate() 无副作用。</li>
 * </ul>
 *
 * <p><b>参见文档 2</b>
 */
public final class SphereProblem extends ContinuousProblem {

    public SphereProblem(int dimension) {
        super(createBounds(dimension, -100), createBounds(dimension, 100));
    }

    /**
     * 目标函数：越小越优。
     * 实现必须为纯函数（相同输入 -> 相同输出）。
     */
    @Override
    public Double evaluate(double[] x) {
        double sum = 0.0;
        for (double v : x) {
            sum += v * v;   // 示例：简单的平方和函数
        }
        return sum;
    }

    /**
     * 深拷贝解。
     * 可简单使用 clone()（如果类型支持）或手动复制。
     */
    @Override
    public double[] copyX(double[] x) {
        return x.clone();
    }
}