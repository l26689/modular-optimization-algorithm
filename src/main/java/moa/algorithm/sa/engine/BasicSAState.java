package moa.algorithm.sa.engine;

import moa.algorithm.sa.api.SAState;

public final class BasicSAState<X> extends SAState<X> {

    public  BasicSAState(X currentX, double temperature, boolean isAccepted) {
        super(currentX, temperature, isAccepted);
    }
    
}
