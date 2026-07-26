package oa.components.Recoders;

import oa.api.Problem;
import oa.api.Recorder;
import oa.api.State;

public class BestRecorder<X,Y> implements Recorder<X,Y,Problem<X,Y>,State<X>> {
    private X bestX;
    private Problem<X,Y> prob;

    public BestRecorder(Problem<X,Y> prob) {
        this.prob = prob;
    }
    @Override
    public void record(State<X> state) {
        if (bestX == null || prob.compare(state.currentX(), bestX) >= 0) {
            bestX = prob.copyX(state.currentX());
        }
    }
    public X getBestX() { return bestX;}
    public Y getBestY() { return prob.evaluate(bestX);}
}