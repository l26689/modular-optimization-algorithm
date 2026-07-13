package sa.components.basiccomponents;

import oa.examples.ContinuousProblem;
import sa.core.SATerminationCondition;
import sa.core.SAState;

public class SABasicTerminationCondition extends SATerminationCondition<double[],ContinuousProblem>{
    private int maxIterations;
    private int currentIteration;
    
    public SABasicTerminationCondition(int maxIterations) {
        this.maxIterations = maxIterations;
        this.currentIteration = 0;
    }
    
    @Override
    public void init(ContinuousProblem problem) {
    }
    
    @Override
    public boolean check(SAState<double[]> state) {
        currentIteration++;
        return currentIteration > maxIterations;
    }
}