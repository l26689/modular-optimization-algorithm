package ga.basicga;

import ga.core.GAState;
import oa.components.state.LongLifeState;


public final class BasicGAState<X> extends LongLifeState<X> implements GAState<X> {

    public BasicGAState(X[] population) {
        super(population);
    }
}
