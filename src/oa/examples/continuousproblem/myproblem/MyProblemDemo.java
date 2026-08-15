package oa.examples.continuousproblem.myproblem;

import oa.components.Recoders.*;
import oa.components.terminationcondition.MaxCallTerminationCondition;
import oa.examples.continuousproblem.ContinuousUniformSearch;
import sa.BasicSA.SimulatedAnnealing;
import sa.components.basiccomponents.*;

public class MyProblemDemo {
    void main() {
        MyProblem prob = new MyProblem(2);
        SimulatedAnnealing<double[]> msa = 
        new <MyProblem>SimulatedAnnealing<double[]>(
            prob,
            new SABasicInitializer(100),
            new ContinuousUniformSearch(),
            new SABasicCoolingSchedule(0.99,100),
            new MaxCallTerminationCondition<double[]>(10000)
        );
        BestRecorder<double[]> recorder = new BestRecorder<>(prob);

        msa.solve(recorder);
        System.out.println(prob.evaluate(recorder.getBestX()));
        
    }
}