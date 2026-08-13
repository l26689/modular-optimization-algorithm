package sa.components.basiccomponents;

import oa.examples.continuousproblem.ContinuousProblem;
import oa.examples.continuousproblem.RandomInitializer;
import sa.core.SAInitializer;
import sa.core.SAState;

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