package moa.problem.continuous;

import moa.framework.problem.Evaluable;
import moa.framework.problem.Problem;

/**
 * 连续优化问题 —— 针对 double[] 表示的解的抽象基类。
 *
 * <p>实现 Problem<double[]> 和 Evaluable<double[], Double>。是否为最终类：否。
 *
 * <p>提供维度查询、边界获取、解的深拷贝（clone）和默认 compare 实现。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>纯函数 —— evaluate() 应无副作用。</li>
 * </ul>
 *
 * <p><b>参见文档 2、5</b>
 */
public abstract class ContinuousProblem implements Problem<double[]>,Evaluable<double[],Double>{
    protected double[] lowerBounds;
    protected double[] upperBounds;

    public ContinuousProblem(double[] lower, double[] upper) {
        if (lower.length != upper.length) {
            throw new IllegalArgumentException("边界维度不一致");
        }
        this.lowerBounds = lower.clone();
        this.upperBounds = upper.clone();
    }
    @Override
    public abstract Double evaluate(double[] x);
    public int getDimension() { return lowerBounds.length;}
    public double[] getLowerBounds() { return lowerBounds.clone();}
    public double[] getUpperBounds() { return upperBounds.clone();}
    public double[] copyX(double[] x) { return x.clone();}
    protected static double[] createBounds(int dim, double value) {
        double[] bounds = new double[dim];
        java.util.Arrays.fill(bounds, value);
        return bounds;
    }
    @Override
    public double compare(double[] x1, double[] x2){
        return evaluate(x2) - evaluate(x1);
    }
}