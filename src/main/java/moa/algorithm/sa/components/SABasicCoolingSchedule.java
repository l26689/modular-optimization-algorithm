package moa.algorithm.sa.components;

import java.util.Random;

import moa.algorithm.sa.api.SACoolingSchedule;
import moa.algorithm.sa.api.SAState;
import moa.framework.problem.Problem;

public final class SABasicCoolingSchedule<X> implements SACoolingSchedule<X,Problem<X>,SAState<X>> {
    private double coolingRate;
    private int currentIteration;
    private int maxIterations;
    
    public SABasicCoolingSchedule(double coolingRate, int maxIterations) {
        this.coolingRate = coolingRate;
        this.maxIterations = maxIterations;
        this.currentIteration = 0;
    }
    
    @Override
    public void init(Problem<X> problem,Random random) {
        // 初始化操作
    }
    
    @Override
    public double cool(SAState<X> state) {
        double temperature = state.getTemperature();
        currentIteration++;
        if(currentIteration > maxIterations) {
            currentIteration = 0;
            return temperature * coolingRate;
        }
        return temperature;
    }
}