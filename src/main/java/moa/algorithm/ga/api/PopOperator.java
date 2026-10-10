package moa.algorithm.ga.api;

import moa.framework.api.Component;
import moa.framework.problem.Problem;


public interface PopOperator<X,Prob extends Problem<X>,S extends GAState<X>,Gene,G extends GeneType<X,Gene,Prob>> extends Component<X,Prob,S> {
    public void operate(S state, X[] newPopulation);
}
