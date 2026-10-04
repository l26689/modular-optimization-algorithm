package ga.core;

import oa.api.problem.Problem;
import oa.api.spi.component.*;

public interface GeneType<X,Gene,Prob extends Problem<X>> extends Component<X,Prob,GAState<X>> {
    public Gene[] encode(X individual);

    public X decode(Gene[] gene);

    public Gene[] copyGene(Gene[] gene);

    public double fitness(X individual);
}
