package oa.examples.continuousproblem.myproblem;

import sa.components.basiccomponents.*;
import sa.core.SimulatedAnnealing;
import oa.examples.Recoders.*;

public class MyProblemDemo {
    void main() {
        SimulatedAnnealing<double[],Double> msa = 
        new <MyProblem>SimulatedAnnealing<double[],Double>(
            new MyProblem(2),
            new SABasicInitializer(100),
            new SABasicPerturbation(),
            new SABasicCoolingSchedule(0.99,100),
            new SABasicTerminationCondition(10000)
        );
        BestRecorder<double[],Double> recorder = new BestRecorder<double[],Double>(new MyProblem(2));
        msa.solve(recorder);
        System.out.println(recorder.getBestY());
        
    }
}
