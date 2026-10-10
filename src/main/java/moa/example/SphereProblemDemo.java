package moa.example;

import moa.problem.continuous.SphereProblem;
import moa.algorithm.sa.components.*;
import moa.algorithm.sa.engine.BasicSA;
import moa.problem.continuous.components.ContinuousUniformSearch;
import moa.components.recorder.*;
import moa.components.terminator.MaxCallTerminationCondition;

public class SphereProblemDemo {
    void main() {
        SphereProblem prob = new SphereProblem(2);
        BasicSA<double[],SphereProblem> msa = 
        new BasicSA<>(
            prob,
            new SABasicInitializer(100),
            new ContinuousUniformSearch(),
            new SABasicCoolingSchedule<>(0.99,100),
            new MaxCallTerminationCondition<double[]>(10000)
        );
        BestRecorder<double[]> recorder = new BestRecorder<>();
        msa.solve(recorder);
        System.out.println(prob.evaluate(recorder.getBestX()));
        
    }
}