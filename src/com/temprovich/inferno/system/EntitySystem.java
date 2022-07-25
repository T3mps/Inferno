package com.temprovich.inferno.system;

import java.util.Comparator;

import com.temprovich.inferno.Registry;

public interface EntitySystem {

    public abstract void update(float dt);

    public abstract void bind(Registry registry);

    public abstract void unbind();

    public abstract void onBind(Registry registry);

    public abstract void onUnbind(Registry registry);

    public abstract void onStateChange();

    public abstract int getPriority();

    public abstract Registry registry();
    
    public abstract boolean isProcessing();

    public abstract void setProcessing(boolean processing);

    public abstract boolean isEnabled();

    public abstract void enable();

    public abstract void disable();

    public static final class SystemComparator implements Comparator<EntitySystem> {

        @Override
        public int compare(EntitySystem a, EntitySystem b) {
            return a.getPriority() > b.getPriority() ? 1 : (a.getPriority() == b.getPriority()) ? 0 : -1;
        }
        
    }

}
