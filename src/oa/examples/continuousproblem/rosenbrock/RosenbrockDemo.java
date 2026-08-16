package oa.examples.continuousproblem.rosenbrock;

import oa.components.Recoders.*;
import oa.components.terminationcondition.MaxCallTerminationCondition;
import oa.examples.continuousproblem.ContinuousUniformSearch;
import sa.BasicSA.SimulatedAnnealing;
import sa.components.continuousproblem.*;

public class RosenbrockDemo {
    void main() {
        RosenbrockProblem prob = new RosenbrockProblem(2);
        SimulatedAnnealing<double[]> msa = 
        new <RosenbrockProblem>SimulatedAnnealing<double[]>(
            prob,
            new SABasicInitializer(100),
            new ContinuousUniformSearch(),
            new SABasicCoolingSchedule(0.99,100),
            new MaxCallTerminationCondition<double[]>(10000)
        );
        LastRecorder<double[],Double> recorder = new LastRecorder<>(prob);
        msa.solve(recorder);
        System.out.println(prob.evaluate(recorder.getLastX()));
        
    }
    
}
