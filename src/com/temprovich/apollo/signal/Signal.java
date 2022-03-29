package com.temprovich.apollo.signal;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Signal<T> {

    private List<SignalListener<T>> listeners = new CopyOnWriteArrayList<SignalListener<T>>();
    
    public void register(SignalListener<T> l) {
        if (listeners.contains(l)) return;
        listeners.add(l);
    }
    
    public void unregister(SignalListener<T> l) {
        listeners.remove(l);
    }
    
    public void dispatch(T t) {
        for (SignalListener<T> l : listeners) l.receive(t);
    }
    
}