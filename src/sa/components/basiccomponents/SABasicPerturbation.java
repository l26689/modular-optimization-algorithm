package sa.components.basiccomponents;

import java.util.Random;

import oa.examples.continuousproblem.ContinuousProblem;
import sa.core.SAPerturbation;
import sa.core.SAState;

public final class SABasicPerturbation implements SAPerturbation<double[],ContinuousProblem> {

    private double[] lowerBounds;
    private double[] upperBounds;
    private ContinuousProblem problem;
    private Random random;
    
    @Override
    public void init(ContinuousProblem problem,Random random) {
        this.lowerBounds = problem.getLowerBounds();
        this.upperBounds = problem.getUpperBounds();
        this.problem = problem;
        this.random = random;
    }
    
    @Override
    public double[] perturb(SAState<double[]> state) {

        double[] x = state.getCurrentXs()[0];
        
        double[] newX = problem.copyX(x);
        
        for(int i = 0; i < newX.length; i++) {
            double delta = (upperBounds[i] - lowerBounds[i]) * 0.1;
            newX[i] += (random.nextDouble() - 0.5) * delta * 2;
            if(newX[i] < lowerBounds[i]) newX[i] = lowerBounds[i];
            if(newX[i] > upperBounds[i]) newX[i] = upperBounds[i];
        }
        
        return newX;
    }
}