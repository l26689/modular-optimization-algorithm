package moa.example;

import moa.problem.continuous.RosenbrockProblem;
import moa.algorithm.sa.components.*;
import moa.algorithm.sa.engine.BasicSA;
import moa.problem.continuous.components.ContinuousUniformSearch;
import moa.components.recorder.*;
import moa.components.terminator.MaxCallTerminationCondition;

public class RosenbrockDemo {
    void main() {
        RosenbrockProblem prob = new RosenbrockProblem(2);
        BasicSA<double[],RosenbrockProblem> msa = 
        new BasicSA<>(
            prob,
            new SABasicInitializer(100),
            new ContinuousUniformSearch(),
            new SABasicCoolingSchedule<>(0.99,100),
            new MaxCallTerminationCondition<double[]>(10000)
        );
        LastRecorder<double[],Double> recorder = new LastRecorder<>();
        msa.solve(recorder);
        System.out.println(prob.evaluate(recorder.getLastX()));
        
    }
    
}
