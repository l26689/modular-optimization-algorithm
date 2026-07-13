package sa.components.basiccomponents;

import java.util.Random;

import sa.core.Perturbation;
import sa.examples.ContinuousProblem;

public class BasicPerturbation extends Perturbation<double[],ContinuousProblem> {

    private double[] lowerBounds;
    private double[] upperBounds;
    
    @Override
    protected void init(ContinuousProblem problem) {
        this.lowerBounds = problem.getLowerBounds();
        this.upperBounds = problem.getUpperBounds();
    }
    
    @Override
    protected double[] perturb(double temperature, double[] x,boolean isAccepted) {
        Random random = new Random();
        double[] newX = x.clone();
        
        for(int i = 0; i < newX.length; i++) {
            double delta = (upperBounds[i] - lowerBounds[i]) * 0.1;
            newX[i] += (random.nextDouble() - 0.5) * delta * 2;
            if(newX[i] < lowerBounds[i]) newX[i] = lowerBounds[i];
            if(newX[i] > upperBounds[i]) newX[i] = upperBounds[i];
        }
        
        return newX;
    }
}