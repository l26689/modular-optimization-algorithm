package oa.examples.continuousproblem.myproblem;

import oa.examples.continuousproblem.ContinuousProblem;
import sa.components.basiccomponents.*;
import sa.core.SimulatedAnnealing;
import oa.examples.Recoders.*;

public class MyProblemDemo {
    void main() {
        SimulatedAnnealing<double[],Double,ContinuousProblem> msa = 
        new SimulatedAnnealing<double[],Double,ContinuousProblem>(
            new MyProblem(2),
            new SABasicInitializer(100),
            new SABasicPerturbation(),
            new SABasicCoolingSchedule(0.99,100),
            new SABasicTerminationCondition(10000)
        );
        BestRecorder<double[],Double,MyProblem> recorder = new BestRecorder<double[],Double,MyProblem>(new MyProblem(2));
        msa.solve(recorder);
        System.out.println(recorder.getBestY());
        
    }
}
