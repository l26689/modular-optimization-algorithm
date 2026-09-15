package oa.examples.continuousproblem.sphereproblem;

import oa.components.Recoders.*;
import oa.components.terminationcondition.MaxCallTerminationCondition;
import oa.examples.continuousproblem.ContinuousUniformSearch;
import sa.BasicSA.SimulatedAnnealing;
import sa.components.continuousproblem.*;

public class SphereProblemDemo {
    void main() {
        SphereProblem prob = new SphereProblem(2);
        SimulatedAnnealing<double[],SphereProblem> msa = 
        new SimulatedAnnealing<>(
            prob,
            new SABasicInitializer(100),
            new ContinuousUniformSearch(),
            new SABasicCoolingSchedule<>(0.99,100),
            new MaxCallTerminationCondition<double[]>(10000)
        );
        BestRecorder<double[]> recorder = new BestRecorder<>();
        msa.solve(recorder);
        System.out.println(prob.evaluate(recorder.getBestX()));
        
    }
}