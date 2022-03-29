package com.temprovich.apollo.system;

import java.util.Comparator;

import com.temprovich.apollo.Registry;

public interface EntitySystem {

    public abstract void update(float dt);

    public abstract void bind(Registry registry);

    public abstract void unbind();

    public abstract void onBind(Registry engine);

    public abstract void onUnbind(Registry engine);

    public abstract void onStateChange();

    public abstract int getPriority();

    public abstract Registry registry();
    
    public abstract boolean isProcessing();

    public abstract void setProcessing(boolean processing);

    public abstract boolean isEnabled();

    public abstract void enable();

    public abstract void disable();

    public static Comparator<EntitySystem> getComparator() {
        return new Comparator<EntitySystem>() {
            
            @Override
            public int compare(EntitySystem a, EntitySystem b) {
                return a.getPriority() > b.getPriority() ? 1 : (a.getPriority() == b.getPriority()) ? 0 : -1;
            }
        };
    }

}
