package oa.components.Recoders;

import oa.api.Problem;
import oa.api.Recorder;
import oa.api.State;

public class BestRecorder<X,Y> implements Recorder<X,Y,Problem<X,Y>,State<X>> {
    private X bestX;
    private Y bestY;
    private Problem<X,Y> prob;

    public BestRecorder(Problem<X,Y> prob) {
        this.prob = prob;
    }
    @Override
    public void record(State<X> state) {
        Y y = prob.evaluate(state.currentX());
        if (bestY == null || prob.compare(y, bestY) >= 0) {
            bestX = prob.copyX(state.currentX());
            bestY = y;
        }
    }
    public X getBestX() { return bestX;}
    public Y getBestY() { return bestY;}
}
