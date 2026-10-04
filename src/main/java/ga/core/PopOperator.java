package ga.core;

import oa.api.problem.*;
import oa.api.spi.component.*;


public interface PopOperator<X,Prob extends Problem<X>,S extends GAState<X>,Gene,G extends GeneType<X,Gene,Prob>> extends Component<X,Prob,S> {
    public void operate(S state, X[] newPopulation);
}
