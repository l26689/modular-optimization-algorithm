package sa.components.basiccomponents;

import sa.core.TerminationCondition;
import sa.examples.ContinuousProblem;

public class BasicTerminationCondition extends TerminationCondition<double[],ContinuousProblem>{
    private int maxIterations;
    private int currentIteration;
    
    public BasicTerminationCondition(int maxIterations) {
        this.maxIterations = maxIterations;
        this.currentIteration = 0;
    }
    
    @Override
    public void init(ContinuousProblem problem) {
    }
    
    @Override
    public boolean check(double temperature, double[] x,boolean isAccepted) {
        currentIteration++;
        return currentIteration > maxIterations;
    }
}