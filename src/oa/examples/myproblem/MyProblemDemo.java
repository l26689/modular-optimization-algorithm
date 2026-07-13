package oa.examples.myproblem;

import oa.examples.ContinuousProblem;
import sa.components.basiccomponents.*;
import sa.core.SimulatedAnnealing;

public class MyProblemDemo {
    void main() {
        ContinuousProblem problem = new MyProblem(2);
        SimulatedAnnealing<double[],ContinuousProblem> msa = 
        new SimulatedAnnealing<double[],ContinuousProblem>(
            problem,
            new SABasicInitializer(100),
            new SABasicPerturbation(),
            new SABasicCoolingSchedule(0.99,100),
            new SABasicTerminationCondition(10000)
        );
        double[] x = msa.solve();
        System.out.println(problem.evaluate(x));
    }
}
