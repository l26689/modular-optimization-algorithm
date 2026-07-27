package oa.components.Recoders;

import oa.api.Recorder;
import oa.api.Problem;
import oa.api.State;


public class LastRecorder<X,Y> implements Recorder<X,Problem<X>,State<X>> {
    private X lastX;
    private Problem<X> prob;

    public LastRecorder(Problem<X> prob) {
        this.prob = prob;
    }
    @Override
    public void record(State<X> state) {
            this.lastX = state.currentX();
    }
    public X getLastX() {
        return prob.copyX(lastX);
    }
}
