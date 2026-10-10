package moa.algorithm.ga.engine;

import moa.algorithm.ga.api.GAState;
import moa.components.state.LongLifeState;


public final class BasicGAState<X> extends LongLifeState<X> implements GAState<X> {

    public BasicGAState(X[] population) {
        super(population);
    }
}
