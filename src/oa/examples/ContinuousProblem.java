package oa.examples;

import oa.Problem;

public abstract class ContinuousProblem implements Problem<double[]>{
    protected double[] lowerBounds;
    protected double[] upperBounds;

    public ContinuousProblem(double[] lower, double[] upper) {
        if (lower.length != upper.length) {
            throw new IllegalArgumentException("边界维度不一致");
        }
        this.lowerBounds = lower.clone();
        this.upperBounds = upper.clone();
    }

    public abstract double evaluate(double[] x);
    public int getDimension() { return lowerBounds.length;}
    public double[] getLowerBounds() { return lowerBounds.clone();}
    public double[] getUpperBounds() { return upperBounds.clone();}
    public double[] copyX(double[] x) { return x.clone();}
}
