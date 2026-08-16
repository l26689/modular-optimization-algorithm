package oa.components.Recoders;

import oa.api.optimizationalgorithm.State;

import oa.api.problem.Problem;
import oa.api.spi.Recorder;


public class LastRecorder<X,Y> implements Recorder<X,Problem<X>,State<X>> {
    private X lastX;
    private Problem<X> prob;

    public LastRecorder(Problem<X> prob) {
        this.prob = prob;
    }
    @Override
    public void record(State<X> state) {
        X[] currentXs = state.getCurrentXs();
        lastX = currentXs[currentXs.length - 1];
    }
    public X getLastX() {
        return prob.copyX(lastX);
    }
}
