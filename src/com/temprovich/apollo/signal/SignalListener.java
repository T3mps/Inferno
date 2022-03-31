package com.temprovich.apollo.signal;

public interface SignalListener<T> {

    public void receive(T data);
    
}
