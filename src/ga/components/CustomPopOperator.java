package ga.components;

import java.util.Random;

import ga.core.GAState;
import ga.core.GeneType;
import ga.core.PopOperator;
import ga.core.singlepopopeartor.*;
import oa.api.problem.Problem;

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
