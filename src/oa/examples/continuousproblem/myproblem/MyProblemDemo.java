package oa.examples.continuousproblem.myproblem;

import oa.components.Recoders.*;
import sa.components.basiccomponents.*;
import sa.core.SimulatedAnnealing;

public class MyProblemDemo {
    void main() {
        MyProblem prob = new MyProblem(2);
        SimulatedAnnealing<double[]> msa = 
        new <MyProblem>SimulatedAnnealing<double[]>(
            prob,
            new SABasicInitializer(100),
            new SABasicPerturbation(),
            new SABasicCoolingSchedule(0.99,100),
            new SABasicTerminationCondition(10000)
        );
        BestRecorder<double[]> recorder = new BestRecorder<>(new MyProblem(2));
        msa.solve(recorder);
        System.out.println(prob.evaluate(recorder.getBestX()));
        
    }
}