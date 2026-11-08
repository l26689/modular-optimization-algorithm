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
    @Override
    public Comparision compare(double[] x1, double[] x2){
        double v1 = evaluate(x1);
        double v2 = evaluate(x2);
        if (v1 < v2) {
            return new Comparision(Comparision.Relation.LESS, v2 - v1);
        }
        else if (v1 > v2) {
            return new Comparision(Comparision.Relation.GREATER, v1 - v2);
        }
        return new Comparision(Comparision.Relation.EQUAL, 0.0);
    }
}