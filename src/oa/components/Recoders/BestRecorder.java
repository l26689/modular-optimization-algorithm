package oa.components.Recoders;

import oa.api.optimizationalgorithm.State;
import oa.api.problem.Problem;
import oa.api.spi.Recorder;

public class BestRecorder<X> implements Recorder<X,Problem<X>,State<X>> {
    private X bestX;
    private Problem<X> prob;

    public BestRecorder(Problem<X> prob) {
        this.prob = prob;
    }

    @Override
    public void record(State<X> state) {
        X[] currentXs = state.getCurrentXs();
        for(X x : currentXs) {
            if (bestX == null || prob.compare(x, bestX) >= 0) {
                bestX = prob.copyX(x);
            }
        }
    }
    public X getBestX() { return bestX;}
}