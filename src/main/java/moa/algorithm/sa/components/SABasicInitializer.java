package moa.algorithm.sa.components;

import moa.problem.continuous.ContinuousProblem;
import moa.algorithm.sa.api.SAInitializer;
import moa.algorithm.sa.api.SAState;
import moa.problem.continuous.components.RandomInitializer;

public final class SABasicInitializer  extends RandomInitializer<SAState<double[]>> implements SAInitializer<double[],ContinuousProblem> {
    private double initialTemp;
    
    public SABasicInitializer(double initialTemp) {
        this.initialTemp = initialTemp;
    }

    @Override
    public double initialTemperature() {
        return initialTemp;
    }
}