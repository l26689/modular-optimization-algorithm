package oa.examples.Recoders;

import oa.core.Problem;
import oa.core.Recorder;

public class BestRecorder<X,Y,Prob extends Problem<X,Y>> implements Recorder<X,Y> {
    private X bestX;
    private Y bestY;
    private Prob prob;

    public BestRecorder(Prob prob) {
        this.prob = prob;
    }
    @Override
    public void record(X x, Y y) {
        if (bestY == null || prob.compare(y, bestY) >= 0) {
            bestX = x;
            bestY = y;
        }
    }
    public X getBestX() { return bestX;}
    public Y getBestY() { return bestY;}
}
