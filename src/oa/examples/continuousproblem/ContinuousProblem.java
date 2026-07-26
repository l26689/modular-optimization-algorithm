package oa.examples.continuousproblem;

import oa.api.Problem;

public abstract class ContinuousProblem implements Problem<double[],Double>{
    protected double[] lowerBounds;
    protected double[] upperBounds;

    public ContinuousProblem(double[] lower, double[] upper) {
        if (lower.length != upper.length) {
            throw new IllegalArgumentException("边界维度不一致");
        }
        this.lowerBounds = lower.clone();
        this.upperBounds = upper.clone();
    }

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
    public  double compare(Double y1, Double y2){
        return y2-y1;
    }
}