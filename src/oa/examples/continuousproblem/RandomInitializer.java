package oa.examples.continuousproblem;

import java.util.Random;

import oa.api.optimizationalgorithm.State;
import oa.api.spi.Initializer;

public class RandomInitializer<S extends State<double[]>> implements Initializer<double[],ContinuousProblem,S> {
    private int dim;
    private double[] lowerBounds;
    private double[] upperBounds;
    private Random random;
    
    @Override
    public void init(ContinuousProblem problem,Random random) {
        this.dim = problem.getDimension();
        this.lowerBounds = problem.getLowerBounds();
        this.upperBounds = problem.getUpperBounds();
        this.random = random;
    }
    
    @Override
    public double[] initialX() {
        double[] x = new double[dim];
        for(int i = 0; i < dim; i++) {
            double range = upperBounds[i] - lowerBounds[i];
            x[i] = lowerBounds[i] + random.nextDouble() * range;
        }
        return x;
    }
    
}
