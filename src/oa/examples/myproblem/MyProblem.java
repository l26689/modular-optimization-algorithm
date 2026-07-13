package oa.examples.myproblem;

import oa.examples.ContinuousProblem;

public class MyProblem extends ContinuousProblem {

    public MyProblem(int dimension) {
        super(createBounds(dimension, -100), createBounds(dimension, 100));
    }

    /**
     * 目标函数：越小越优。
     * 实现必须为纯函数（相同输入 -> 相同输出）。
     */
    @Override
    public double evaluate(double[] x) {
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

    private static double[] createBounds(int dim, double value) {
        double[] bounds = new double[dim];
        java.util.Arrays.fill(bounds, value);
        return bounds;
    }
}