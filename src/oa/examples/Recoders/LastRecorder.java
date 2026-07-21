package oa.examples.Recoders;

import oa.api.Recorder;
import oa.api.Problem;
import oa.api.State;


public class LastRecorder<X,Y> implements Recorder<X,Y,Problem<X,Y>,State<X>> {
    private X lastX;
    private Problem<X,Y> prob;

    public LastRecorder(Problem<X,Y> prob) {
        this.prob = prob;
    }
    @Override
    public void record(State<X> state) {
        this.lastX = state.currentX();
    }
    public X getLastX() {
        return prob.copyX(lastX);
    }
    public Y getLastY() {
        return prob.evaluate(lastX);
    }
}
