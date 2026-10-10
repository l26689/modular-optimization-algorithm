package moa.problem.continuous.components;

import java.util.Random;

import moa.framework.api.Initializer;
import moa.framework.engine.State;
import moa.problem.continuous.ContinuousProblem;

public class RandomInitializer<S extends State<double[]>> implements Initializer<double[],ContinuousProblem,S> {
    protected int dim;
    protected double[] lowerBounds;
    protected double[] upperBounds;
    protected Random random;
    
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
