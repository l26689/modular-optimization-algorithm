package oa.examples.Recoders;

import oa.api.Recorder;
import oa.api.Problem;


public class LastRecorder<X,Y> implements Recorder<X,Y,Problem<X,Y>> {
    private X lastX;
    private Problem<X,Y> prob;

    public LastRecorder(Problem<X,Y> prob) {
        this.prob = prob;
    }
    @Override
    public void record(X x) {
        this.lastX = x;
    }
    public X getLastX() {
        return prob.copyX(lastX);
    }
    public Y getLastY() {
        return prob.evaluate(lastX);
    }
}
