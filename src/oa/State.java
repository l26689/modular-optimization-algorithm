package oa;

public class State<X> {
    private final X currentX;

    public State(X currentX) {
        this.currentX = currentX;
    }

    public X currentX() { return currentX; }
}