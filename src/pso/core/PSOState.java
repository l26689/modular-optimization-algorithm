package pso.core;

import java.util.Collection;

import oa.api.State;

class PSOState<X> extends State<Collection<X>> {

    public PSOState(Collection<X> particles) {
        super(particles);
    }
}