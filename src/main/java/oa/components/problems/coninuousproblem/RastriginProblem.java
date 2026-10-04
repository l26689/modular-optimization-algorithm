package oa.components.problems.coninuousproblem;

public final class RastriginProblem extends ContinuousProblem {

    public RastriginProblem(int dimension) {
        super(createBounds(dimension, -5.12), createBounds(dimension, 5.12));
    }

    @Override
    public Double evaluate(double[] x) {
        double sum = 10.0 * x.length;
        for (int i = 0; i < x.length; i++) {
            sum += x[i] * x[i] - 10.0 * Math.cos(2.0 * Math.PI * x[i]);
        }
        return sum;
    }
}