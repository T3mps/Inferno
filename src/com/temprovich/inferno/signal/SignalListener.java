package com.temprovich.inferno.signal;

public interface SignalListener<T> {

    public void receive(T data);
    
}
