package oa.examples.Recoders;

import oa.api.Recorder;


public class LastRecorder<X,Y> implements Recorder<X,Y> {
    private X lastX;
    private Y lastY;
    @Override
    public void record(X x, Y y) {
        this.lastX = x;
        this.lastY = y;
    }
    public X getLastX() {
        return lastX;
    }
    public Y getLastY() {
        return lastY;
    }
}
