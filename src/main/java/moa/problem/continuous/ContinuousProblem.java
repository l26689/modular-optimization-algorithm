package moa.problem.continuous;

import moa.framework.problem.Comparision;
import moa.framework.problem.Evaluable;
import moa.framework.problem.Problem;

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
    /**
     * 比较两个解。
     * <p>
     * <b>约定（与 Comparision 的枚举注释、以及全部调用方一致）：</b>
     * {@code GREATER} 表示 x1 优于 x2，{@code LESS} 表示 x1 劣于 x2。
     * 所以最小化问题里 v1 &lt; v2（x1 目标值更小）要返回 {@code GREATER}。
     * <p>
     * {@code gap} 是两者目标值之差的绝对值，方向由 {@code relation} 表达；
     * 需要"x1 比 x2 好多少"时直接用 gap 即可（正值）。
     */
    @Override
    public Comparision compare(double[] x1, double[] x2){
        double v1 = evaluate(x1);
        double v2 = evaluate(x2);
        if (v1 < v2) {
            // x1 的目标值更小 → x1 更优
            return new Comparision(Comparision.Relation.GREATER, v2 - v1);
        }
        else if (v1 > v2) {
            // x1 的目标值更大 → x1 更劣
            return new Comparision(Comparision.Relation.LESS, v1 - v2);
        }
        return new Comparision(Comparision.Relation.EQUAL, 0.0);
    }
}