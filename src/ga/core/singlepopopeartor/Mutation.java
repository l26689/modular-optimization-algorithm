package ga.core.singlepopopeartor;

import ga.core.GAState;
import ga.core.GeneType;
import ga.core.PopOperator;
import oa.api.problem.Problem;

public interface Mutation<X,Prob extends Problem<X>,S extends GAState<X>,Gene,G extends GeneType<X,Gene,Prob>> extends PopOperator<X,Prob,S,Gene,G> {
    
}
