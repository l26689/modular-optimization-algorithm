package sa.components.basiccomponents;

import java.util.Random;

import oa.examples.continuousproblem.ContinuousProblem;
import sa.core.SAInitializer;

public class SABasicInitializer extends SAInitializer<double[],Double,ContinuousProblem> {
    private double initialTemp;
    private int dim;
    private double[] lowerBounds;
    private double[] upperBounds;
    private Random random;
    
    public SABasicInitializer(double initialTemp) {
        this.initialTemp = initialTemp;
    }
    
    @Override
    protected void init(ContinuousProblem problem,Random random) {
        this.dim = problem.getDimension();
        this.lowerBounds = problem.getLowerBounds();
        this.upperBounds = problem.getUpperBounds();
        this.random = random;
    }
    
    @Override
    protected double[] initialX() {
        double[] x = new double[dim];
        for(int i = 0; i < dim; i++) {
            double range = upperBounds[i] - lowerBounds[i];
            x[i] = lowerBounds[i] + random.nextDouble() * range;
        }
        return x;
    }
    
    @Override
    protected double initialTemperature() {
        return initialTemp;
    }
}