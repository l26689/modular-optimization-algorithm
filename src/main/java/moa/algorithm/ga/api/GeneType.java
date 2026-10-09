package moa.algorithm.ga.api;

import moa.framework.api.Component;
import moa.framework.problem.Problem;

public interface GeneType<X,Gene,Prob extends Problem<X>> extends Component<X,Prob,GAState<X>> {
    public Gene[] encode(X individual);

    public X decode(Gene[] gene);

    public Gene[] copyGene(Gene[] gene);

    public double fitness(X individual);
}
