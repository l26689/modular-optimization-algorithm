package oa.components.problems.coninuousproblem;

public final class GriewankProblem extends ContinuousProblem {

    public GriewankProblem(int dimension) {
        super(createBounds(dimension, -32.0), createBounds(dimension, 32.0));
    }

    @Override
    public Double evaluate(double[] x) {
        double sum = 0.0;
        double product = 1.0;
        for (int i = 0; i < x.length; i++) {
            sum += x[i] * x[i] / 4000.0;
            product *= Math.cos(x[i] / Math.sqrt(i + 1));
        }
        return sum - product + 1.0;
    }
}