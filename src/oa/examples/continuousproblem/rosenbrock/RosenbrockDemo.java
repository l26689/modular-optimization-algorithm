package oa.examples.continuousproblem.rosenbrock;

import oa.components.Recoders.*;
import sa.components.basiccomponents.*;
import sa.core.SimulatedAnnealing;

public class RosenbrockDemo {
    void main() {
        RosenbrockProblem prob = new RosenbrockProblem(2);
        SimulatedAnnealing<double[]> msa = 
        new <RosenbrockProblem>SimulatedAnnealing<double[]>(
            prob,
            new SABasicInitializer(100),
            new SABasicPerturbation(),
            new SABasicCoolingSchedule(0.99,100),
            new SABasicTerminationCondition(10000)
        );
        LastRecorder<double[],Double> recorder = new LastRecorder<double[],Double>(prob);
        msa.solve(recorder);
        System.out.println(prob.evaluate(recorder.getLastX()));
        
    }
    
}
