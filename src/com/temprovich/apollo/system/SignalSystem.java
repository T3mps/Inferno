package com.temprovich.apollo.system;

import com.temprovich.apollo.Entity;
import com.temprovich.apollo.signal.Signal;
import com.temprovich.apollo.signal.SignalListener;

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
