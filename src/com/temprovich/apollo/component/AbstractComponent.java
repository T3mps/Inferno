package com.temprovich.apollo.component;

import com.temprovich.apollo.Entity;

public abstract class AbstractComponent implements Component {

    private Entity parent;
    private boolean enabled;

    public AbstractComponent() {
        this.enabled = false;
    }
    
    @Override
    public final Entity getParent() {
        return parent;
    }
    
    @Override
    public final void setParent(Entity parent) {
        this.parent = parent;
    }

    @Override
    public final boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        if (enabled) return;
        enabled = true;
        onEnable();
    }

    @Override
    public void onEnable() {}
    
    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;
        onDisable();
    }

    @Override
    public void onDisable() {}

}
