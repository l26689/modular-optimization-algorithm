package oa.examples.Recoders;

import oa.api.Problem;
import oa.api.Recorder;

public class BestRecorder<X,Y> implements Recorder<X,Y,Problem<X,Y>> {
    private X bestX;
    private Y bestY;
    private Problem<X,Y> prob;

    public BestRecorder(Problem<X,Y> prob) {
        this.prob = prob;
    }
    @Override
    public void record(X x) {
        Y y = prob.evaluate(x);
        if (bestY == null || prob.compare(y, bestY) >= 0) {
            bestX = x;
            bestY = y;
        }
    }
    public X getBestX() { return bestX;}
    public Y getBestY() { return bestY;}
}
