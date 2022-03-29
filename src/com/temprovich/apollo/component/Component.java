package com.temprovich.apollo.component;

import com.temprovich.apollo.Entity;

public interface Component {

    public abstract Entity getParent();

    public abstract void setParent(Entity parent);

    public abstract boolean isEnabled();

    public abstract void enable();

    public abstract void onEnable();

    public abstract void disable();

    public abstract void onDisable();
    
}
