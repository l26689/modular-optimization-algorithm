package oa.examples.continuousproblem.rosenbrock;

import sa.components.basiccomponents.*;
import sa.core.SimulatedAnnealing;
import oa.examples.Recoders.*;

public class RosenbrockDemo {
    void main() {
        RosenbrockProblem prob = new RosenbrockProblem(2);
        SimulatedAnnealing<double[],Double> msa = 
        new <RosenbrockProblem>SimulatedAnnealing<double[],Double>(
            prob,
            new SABasicInitializer(100),
            new SABasicPerturbation(),
            new SABasicCoolingSchedule(0.99,100),
            new SABasicTerminationCondition(10000)
        );
        LastRecorder<double[],Double> recorder = new LastRecorder<double[],Double>(prob);
        msa.solve(recorder);
        System.out.println(recorder.getLastY());
        
    }
    
}
