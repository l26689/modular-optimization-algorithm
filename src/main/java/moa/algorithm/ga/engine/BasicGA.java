package moa.algorithm.ga.engine;

import java.lang.reflect.Array;
import java.util.Random;

import moa.algorithm.ga.api.*;
import moa.framework.api.Initializer;
import moa.framework.api.Recorder;
import moa.framework.api.TerminationCondition;
import moa.framework.engine.OptimizationAlgorithm;
import moa.framework.problem.Problem;

public final class BasicGA<X,Prob extends Problem<X>> 
    extends OptimizationAlgorithm<X,Prob,BasicGAState<X>> {
    Random random;
    Initializer<X,? super Prob,? super BasicGAState<X>>[] initializers;
    TotalPopOperator<X,? super Prob,? super BasicGAState<X>,?,?> totalPopOperator;
    TerminationCondition<X,? super Prob,? super BasicGAState<X>> terminationCondition;

    public BasicGA(
        Prob problem,
        Initializer<X,? super Prob,? super BasicGAState<X>>[] initializers,
        TotalPopOperator<X,? super Prob,? super BasicGAState<X>,?,?> totalPopOperator,
        TerminationCondition<X,? super Prob,? super BasicGAState<X>> terminationCondition) {
        this.random = new Random();
        this.problem = problem;
        this.initializers = initializers;
        this.totalPopOperator = totalPopOperator;
        this.terminationCondition = terminationCondition;
        for (Initializer<X,? super Prob,? super BasicGAState<X>> initializer : initializers) {
            initializer.init(problem, random);
        }
        totalPopOperator.init(problem, random);
        terminationCondition.init(problem, random);
    }

    public BasicGA(
        Random random,
        Prob problem,
        Initializer<X,? super Prob,? super BasicGAState<X>>[] initializers,
        TotalPopOperator<X,? super Prob,? super BasicGAState<X>,?,?> totalPopOperator,
        TerminationCondition<X,? super Prob,? super BasicGAState<X>> terminationCondition) {
        this.random = random;
        this.problem = problem;
        this.initializers = initializers;
        this.totalPopOperator = totalPopOperator;
        this.terminationCondition = terminationCondition;
        for (Initializer<X,? super Prob,? super BasicGAState<X>> initializer : initializers) {
            initializer.init(problem, random);
        }
        totalPopOperator.init(problem, random);
        terminationCondition.init(problem, random);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void solve(Recorder<X, ? super Prob, ? super BasicGAState<X>> recorder) {
        recorder.init(problem, random);

        X tempX = initializers[0].initialX();
        X[] currentPopulation = (X[]) Array.newInstance(tempX.getClass(),initializers.length);
        X[] newPopulation = (X[]) Array.newInstance(tempX.getClass(),initializers.length);
        X[] temp;
        currentPopulation[0] = tempX;

        for (int i = 1; i < initializers.length; i++) {
            currentPopulation[i] = initializers[i].initialX();
        }

        BasicGAState<X> state = new BasicGAState<X>(currentPopulation);

        recorder.record(state);

        while (!terminationCondition.check(state)) {
            totalPopOperator.operate(state, newPopulation);
            
            temp = currentPopulation;
            currentPopulation = newPopulation;
            newPopulation = temp;

            state.set(currentPopulation);

            recorder.record(state);
        }
    }
}
    
