package com.temprovich.apollo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.temprovich.apollo.component.Component;
import com.temprovich.apollo.signal.Signal;
import com.temprovich.apollo.util.Bag;

public final class Entity {

    public int flags;
    
    private Registry registry;
    public final Signal<Entity> onComponentAdd;
    public final Signal<Entity> onComponentRemove;
    
    private Bag<Component> components;
    private Map<Class<?>, Component> componentMap;

    private boolean enabled;
    
    public Entity() {
        this.flags = 0;
        this.registry = null;
        this.onComponentAdd = new Signal<Entity>();
        this.onComponentRemove = new Signal<Entity>();
        this.components = new Bag<Component>();
        this.componentMap = new HashMap<Class<?>, Component>();
        this.enabled = false;
    }

    public final Entity add(final Component c) {
        if (enabled || registry != null) throw new IllegalStateException("Entity is enabled or has been added to a registry");
        if (c.getParent() != null) throw new IllegalArgumentException("Component already added to an entity");
        
        components.add(c);
        c.setParent(this);
        onComponentAdd();

        if (enabled && !c.isEnabled()) c.enable();

        return this;
    }

    public final Entity addAll(final Component... c) {
        for (Component comp : c) add(comp);
        return this;
    }

    public final boolean has(final Class<?> clazz) {
        if (componentMap.containsKey(clazz)) return true;

        for (Component c : components) if (clazz.isInstance(c)) {
            return true;
        }

        return false;
    }

    public final <T extends Component> T get(final Class<T> clazz) {
        Component com = componentMap.get(clazz);
        if (com != null) return clazz.cast(com);
        
        for (Component c : components) {
            if (clazz.isInstance(c)) {
                componentMap.put(clazz, c);
                return clazz.cast(c);
            }
        }

        return null;
    }

    @SafeVarargs
    public final <T extends Component> List<T> get(final Class<T>... clazz) {
        return get(Family.define(clazz));
    }

    @SuppressWarnings("unchecked")
    public final <T extends Component> List<T> get(final Family family) {
        List<T> result = new ArrayList<T>();

        for (Class<?> clazz : family.getTypes()) {
            T com = get((Class<T>) clazz);
            if (com != null) result.add(com);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public final <T extends Component> T replace(final Class<T> clazz, T c) {
        if (c.getParent() != null) throw new IllegalArgumentException("Component already added to an entity");
        
        Component old = componentMap.get(clazz);
        if (old != null) {
            components.remove(old);
            onComponentRemove();
        }

        components.add(c);
        c.setParent(this);
        onComponentAdd();

        if (enabled && !c.isEnabled()) c.enable();

        return (T) old;
    }

    public final <T extends Component> T remove(final Class<T> clazz) {
        T c = get(clazz);

        components.remove(c);
        c.setParent(null);
        onComponentRemove();

        if (enabled && c.isEnabled()) c.disable();

        return clazz.cast(c);
    }

    void flush() {
        for (Component c : components) {
            c.setParent(null);
            c.disable();
        }
        components.clear();
        componentMap.clear();
    }

    void onComponentAdd() {
        onComponentAdd.dispatch(this);
    }

    void onComponentRemove() {
        onComponentRemove.dispatch(this);
    }

    public Registry getEngine() {
        return registry;
    }

    public Entity setEngine(Registry registry) {
        this.registry = registry;
        return this;
    }

    public Entity removeEngine() {
        this.registry = null;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Entity enable() {
        if (enabled) return this;
        for (Component c : components) if (!c.isEnabled()) c.enable();
        enabled = true;
        return this;
    }

    public Entity disable() {
        if (!enabled) return this;
        for (Component c : components) if (c.isEnabled()) c.disable();
        enabled = false;
        return this;
    }

    Bag<Component> getComponents() {
        return components;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((componentMap == null) ? 0 : componentMap.hashCode());
        result = prime * result + ((components == null) ? 0 : components.hashCode());
        result = prime * result + (enabled ? 1231 : 1237);
        result = prime * result + flags;
        result = prime * result + ((onComponentAdd == null) ? 0 : onComponentAdd.hashCode());
        result = prime * result + ((onComponentRemove == null) ? 0 : onComponentRemove.hashCode());
        result = prime * result + ((registry == null) ? 0 : registry.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Entity)) return false;
        Entity other = (Entity) obj;
        if (componentMap == null) {
            if (other.componentMap != null) return false;
        } else if (!componentMap.equals(other.componentMap)) return false;
        if (components == null) {
            if (other.components != null) return false;
        } else if (!components.equals(other.components)) return false;
        if (enabled != other.enabled) return false;
        if (flags != other.flags) return false;
        if (onComponentAdd == null) {
            if (other.onComponentAdd != null) return false;
        } else if (!onComponentAdd.equals(other.onComponentAdd)) return false;
        if (onComponentRemove == null) {
            if (other.onComponentRemove != null) return false;
        } else if (!onComponentRemove.equals(other.onComponentRemove)) return false;
        if (registry == null) {
            if (other.registry != null) return false;
        } else if (!registry.equals(other.registry)) return false;
        return true;
    }

    

}
