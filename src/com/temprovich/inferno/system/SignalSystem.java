package com.temprovich.inferno.system;

import com.temprovich.inferno.Entity;
import com.temprovich.inferno.signal.Signal;
import com.temprovich.inferno.signal.SignalListener;

public abstract class SignalSystem extends AbstractEntitySystem implements SignalListener<Entity> {

    public SignalSystem(Signal<Entity> signal) {
        this(signal, 0);
    }

    public SignalSystem(Signal<Entity> signal, int priority) {
        super(priority);
        signal.register(this);
    }

    @Override
    public void receive(Entity t) {
        push();

        process(t);

        pop();
    }

    protected void push() {}

    protected abstract void process(Entity entity);
    
    protected void pop() {}

}
