package sa.components.basiccomponents;

import sa.core.SACoolingSchedule;
import sa.core.SAState;
import java.util.Random;

import oa.examples.continuousproblem.ContinuousProblem;

public class SABasicCoolingSchedule extends SACoolingSchedule<double[],ContinuousProblem> {
    private double coolingRate;
    private int currentIteration;
    private int maxIterations;
    
    public SABasicCoolingSchedule(double coolingRate, int maxIterations) {
        this.coolingRate = coolingRate;
        this.maxIterations = maxIterations;
        this.currentIteration = 0;
    }
    
    @Override
    public void init(ContinuousProblem problem,Random random) {
        // 初始化操作
    }
    
    @Override
    public double cool(SAState<double[]> state) {
        double temperature = state.temperature();
        currentIteration++;
        if(currentIteration > maxIterations) {
            currentIteration = 0;
            return temperature * coolingRate;
        }
        return temperature;
    }
}