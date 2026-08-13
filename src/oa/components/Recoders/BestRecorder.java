package oa.components.Recoders;

import java.util.Iterator;

import oa.api.Problem;
import oa.api.Recorder;
import oa.api.State;

public class BestRecorder<X> implements Recorder<X,Problem<X>,State<X>> {
    private X bestX;
    private Problem<X> prob;

    public BestRecorder(Problem<X> prob) {
        this.prob = prob;
    }
    @Override
    public void record(State<X> state) {
        if(state.isArraySupported()) {
            X[] currentXs = state.getCurrentXs();
            for(X x : currentXs) {
                if (bestX == null || prob.compare(x, bestX) >= 0) {
                    bestX = prob.copyX(x);
                }
            }
        }
        else {
            Iterator<X> currentXIterator = state.getCurrentXIterator();
            while(currentXIterator.hasNext()) {
                X x = currentXIterator.next();
                if (bestX == null || prob.compare(x, bestX) >= 0) {
                    bestX = prob.copyX(x);
                }
            }
        }
    }
    public X getBestX() { return bestX;}
}