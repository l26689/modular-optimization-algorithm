package oa.components.Recoders;

import oa.api.Recorder;
import java.util.Iterator;

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
        if(state.isArraySupported()) {
            X[] currentXs = state.getCurrentXs();
            lastX = currentXs[currentXs.length - 1];
        }
        else {
            Iterator<X> currentXIterator = state.getCurrentXIterator();
            while(currentXIterator.hasNext()) {
                lastX = currentXIterator.next();
            }
        }

    }
    public X getLastX() {
        return prob.copyX(lastX);
    }
}
