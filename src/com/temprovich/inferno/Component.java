package com.temprovich.inferno;

public abstract class Component {

    private Entity parent;
    private boolean enabled;

    public Component() {
        this.enabled = false;
    }
    
    public final Entity getParent() {
        return parent;
    }
    
    public final void setParent(Entity parent) {
        this.parent = parent;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        if (enabled) return;
        enabled = true;
        onEnable();
    }

    public void onEnable() {}
    
    public void disable() {
        if (!enabled) return;
        enabled = false;
        onDisable();
    }

    public void onDisable() {}
}
