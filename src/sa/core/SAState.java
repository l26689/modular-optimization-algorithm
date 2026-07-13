package sa.core;

import oa.State;

public class SAState<X> extends State<X> {
    private final double temperature;
    private final boolean isAccepted;

    public SAState(X currentX, double temperature, boolean isAccepted) {
        super(currentX);
        this.temperature = temperature;
        this.isAccepted = isAccepted;
    }

    public double temperature() { return temperature; }
    public boolean isAccepted() { return isAccepted; }
}