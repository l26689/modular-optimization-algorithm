package oa.components.Recoders;

import oa.api.optimizationalgorithm.State;

import java.util.Iterator;
import java.util.Random;

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
