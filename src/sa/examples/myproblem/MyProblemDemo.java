package sa.examples.myproblem;

import sa.components.basiccomponents.*;
import sa.core.ModularSimulatedAnnealing;
import sa.examples.ContinuousProblem;

public class MyProblemDemo {
    void main() {
        ContinuousProblem problem = new MyProblem(2);
        ModularSimulatedAnnealing<double[],ContinuousProblem> msa = 
        new ModularSimulatedAnnealing<double[],ContinuousProblem>(
            problem,
            new BasicInitializer(100),
            new BasicPerturbation(),
            new BasicCoolingSchedule(0.99,100),
            new BasicTerminationCondition(10000)
        );
        double[] x = msa.solve();
        System.out.println(problem.evaluate(x));
    }
}
