package oa.examples.continuousproblem.rosenbrock;

import oa.examples.continuousproblem.ContinuousProblem;
import sa.components.basiccomponents.*;
import sa.core.SimulatedAnnealing;
import oa.examples.Recoders.*;

public class RosenbrockDemo {
    void main() {
        SimulatedAnnealing<double[],Double,ContinuousProblem> msa = 
        new SimulatedAnnealing<double[],Double,ContinuousProblem>(
            new RosenbrockProblem(2),
            new SABasicInitializer(100),
            new SABasicPerturbation(),
            new SABasicCoolingSchedule(0.99,100),
            new SABasicTerminationCondition(10000)
        );
        LastRecorder<double[],Double> recorder = new LastRecorder<double[],Double>();
        msa.solve(recorder);
        System.out.println(recorder.getLastY());
        
    }
    
}
