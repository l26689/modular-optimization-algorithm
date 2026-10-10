package moa.algorithm.ga.components;

import java.util.Random;

import moa.algorithm.ga.api.GAState;
import moa.algorithm.ga.api.GeneType;
import moa.algorithm.ga.api.PopOperator;
import moa.framework.problem.Problem;
import moa.algorithm.ga.api.*;

public final class CustomPopOperator<X,Prob extends Problem<X>,S extends GAState<X>,Gene,G extends GeneType<X,Gene,Prob>> 
implements PopOperator<X,Prob,S,Gene,G> {
    Selection<X,? super Prob,? super S,? super Gene,G> selection;
    Crossover<X,? super Prob,? super S,? super Gene,G> crossover;
    Mutation<X,? super Prob,? super S,? super Gene,G> mutation;
    Replacement<X,? super Prob,? super S,? super Gene,G> replacement;

    public CustomPopOperator(Selection<X,? super Prob,? super S,? super Gene,G> selection,
                              Crossover<X,? super Prob,? super S,? super Gene,G> crossover,
                              Mutation<X,? super Prob,? super S,? super Gene,G> mutation,
                              Replacement<X,? super Prob,? super S,? super Gene,G> replacement) {
        this.selection = selection;
        this.crossover = crossover;
        this.mutation = mutation;
        this.replacement = replacement;
    }
    @Override
    public void init(Prob prob, Random random) {
        selection.init(prob, random);
        crossover.init(prob, random);
        mutation.init(prob, random);
        replacement.init(prob, random);
    }

    @Override
    public void operate(S state, X[] newPopulation) {
        selection.operate(state, newPopulation);
        crossover.operate(state, newPopulation);
        mutation.operate(state, newPopulation);
        replacement.operate(state, newPopulation);
    }
    
}
