package oa.components.problems.coninuousproblem;

public final class AckleyProblem extends ContinuousProblem {

    public AckleyProblem(int dimension) {
        super(createBounds(dimension, -32.0), createBounds(dimension, 32.0));
    }

    @Override
    public Double evaluate(double[] x) {
        int n = x.length;
        double sumSq = 0.0;
        double sumCos = 0.0;
        for (int i = 0; i < n; i++) {
            sumSq += x[i] * x[i];
            sumCos += Math.cos(2.0 * Math.PI * x[i]);
        }
        double term1 = -20.0 * Math.exp(-0.2 * Math.sqrt(sumSq / n));
        double term2 = -Math.exp(sumCos / n);
        return term1 + term2 + 20.0 + Math.E;
    }
}