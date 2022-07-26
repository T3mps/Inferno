package com.temprovich.inferno;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.temprovich.inferno.signal.Signal;
import com.temprovich.inferno.util.Bag;

public class Entity {

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

    Entity(Entity entity) {
        this.flags = entity.flags;
        this.registry = entity.registry;
        this.onComponentAdd = entity.onComponentAdd;
        this.onComponentRemove = entity.onComponentRemove;
        this.components = new Bag<Component>(entity.components.size());
        this.componentMap = new HashMap<Class<?>, Component>(entity.componentMap.size());

        for (var component : entity.components) {
            this.components.add(component);
            this.componentMap.put(component.getClass(), component);
        }

        this.enabled = entity.enabled;
    }

    public final Entity add(final Component component) {
        if (component == null) {
            throw new NullPointerException("component");
        }
        if (enabled || registry != null) {
            throw new IllegalStateException("Entity is enabled or already added to a registry");
        }
        if (component.getParent() != null) {
            throw new IllegalArgumentException("Component already added to an entity");
        }
        
        components.add(component);
        component.setParent(this);
        onComponentAdd();

        if (enabled && !component.isEnabled()) {
            component.enable();
        }

        return this;
    }

    public final Entity addAll(final Component... components) {
        for (var c : components) {
            add(c);
        }
        
        return this;
    }

    public final Entity addAll(final Entity entity) {
        for (var c : entity.components) {
            add(c);
        }

        return this;
    }

    public final boolean has(final Class<?> componentClass) {
        if (componentMap.containsKey(componentClass)) {
            return true;
        }

        for (var component : components) {
            if (componentClass.isInstance(component)) {
                return true;
            }
        }

        return false;
    }

    public final boolean has(final Class<?>... componentClasses) {
        for (var c : componentClasses) {
            if (!has(c)) {
                return false;
            }
        }

        return true;
    }

    public final <T extends Component> boolean has(final T component) {
        if (component == null) {
            throw new NullPointerException("component");
        }
        if (componentMap.containsKey(component.getClass())) {
            return true;
        }

        for (var c : components) {
            if (component.equals(c)) {
                return true;
            }
        }

        return false;
    }

    public final <T extends Component> T get(final Class<T> componentClass) {
        Component com = componentMap.get(componentClass);
        if (com != null) {
            return componentClass.cast(com);
        }
        
        for (var component : components) {
            if (componentClass.isInstance(component)) {
                componentMap.put(componentClass, component);
                return componentClass.cast(component);
            }
        }

        return null;
    }

    @SafeVarargs
    public final <T extends Component> List<T> get(final Class<T>... componentClasses) {
        return get(Family.define(componentClasses));
    }

    @SuppressWarnings("unchecked")
    public final <T extends Component> List<T> get(final Family family) {
        List<T> result = new ArrayList<T>();

        for (var componentClass : family.getTypes()) {
            T com = get((Class<T>) componentClass);
            if (com != null) {
                result.add(com);
            }
        }

        return result;
    }

    public final <T extends Component> T remove(final Class<T> componentClass) {
        if (enabled || registry != null) {
            throw new IllegalStateException("Entity is enabled or already added to a registry");
        }

        T component = get(componentClass);

        components.remove(component);
        component.setParent(null);
        onComponentRemove();

        if (enabled && component.isEnabled()) {
            component.disable();
        }

        return componentClass.cast(component);
    }

    public final <T extends Component> T remove(final T component) {
        if (component == null) {
            throw new NullPointerException("component");
        }
        if (enabled || registry != null) {
            throw new IllegalStateException("Entity is enabled or already added to a registry");
        }
        if (component.getParent() != this || !components.contains(component)) {
            throw new IllegalArgumentException("Component not added to this entity");
        }

        components.remove(component);
        component.setParent(null);
        onComponentRemove();

        if (enabled && component.isEnabled()) {
            component.disable();
        }

        return component;
    }

    void flush() {
        for (var component : components) {
            component.setParent(null);
            component.disable();
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

    public Registry getRegistry() {
        return registry;
    }

    public Entity setRegistry(Registry registry) {
        this.registry = registry;
        return this;
    }

    public Entity removeRegistry() {
        this.registry = null;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Entity enable() {
        if (enabled) {
            return this;
        }
        
        for (Component component : components) {
            if (!component.isEnabled()) {
                component.enable();
            }
        }

        enabled = true;
        return this;
    }

    public Entity disable() {
        if (!enabled) {
            return this;
        }

        for (var component : components) {
            if (component.isEnabled()) {
                component.disable();
            }
        }

        enabled = false;
        return this;
    }

    public Bag<Component> getComponents() {
        return components;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result << 5) - result) + ((componentMap == null) ? 0 : componentMap.hashCode());
        result = ((result << 5) - result) + ((components == null) ? 0 : components.hashCode());
        result = ((result << 5) - result) + (enabled ? 1231 : 1237);
        result = ((result << 5) - result) + flags;
        result = ((result << 5) - result) + ((onComponentAdd == null) ? 0 : onComponentAdd.hashCode());
        result = ((result << 5) - result) + ((onComponentRemove == null) ? 0 : onComponentRemove.hashCode());
        result = ((result << 5) - result) + ((registry == null) ? 0 : registry.hashCode());
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

    @Override
    public String toString() {
        return new StringBuilder()
        .append("Entity [components=")
        .append(components)
        .append(", flags=")
        .append(flags)
        .append(", enabled=")
        .append(enabled)
        .toString();
    }
}