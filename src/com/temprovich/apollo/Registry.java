package com.temprovich.apollo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingDeque;

import com.temprovich.apollo.component.Component;
import com.temprovich.apollo.system.EntitySystem;
import com.temprovich.apollo.util.Array;

public class Registry {

    private static final int DEFAULT_INITIAL_CAPACITY = 48;

    private final List<Entity> entities;
    private final Map<Family, List<Entity>> views;

    private final Deque<Task> tasks;
    private final List<EntitySystem> systems;
    private final List<EntityListener> listeners;
    private final Map<Family, List<EntityListener>> filteredListeners;

    private boolean updating;

    public Registry() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    public Registry(int capacity) {
        this.entities = new ArrayList<Entity>(capacity);
        this.views = new HashMap<Family, List<Entity>>(capacity);
        this.tasks = new LinkedBlockingDeque<Task>();
        this.systems = new ArrayList<EntitySystem>();
        this.listeners = new ArrayList<EntityListener>();
        this.filteredListeners = new HashMap<Family, List<EntityListener>>();
        this.updating = false;
    }

    public final Entity create() {
        Entity e = new Entity();
        add(e);
        return e;
    }

    public final Array<Entity> create(int n) {
        Array<Entity> entities = new Array<Entity>(n);
        for (int i = 0; i < n; i++) entities.add(create());
        return entities;
    }

    @SafeVarargs
    public final <T extends Component> Entity emplace(final T... components) {
        Entity e = new Entity().addAll(components);
        add(e);
        return e;
    }

    @SafeVarargs
    public final <T extends Component> Array<Entity> emplace(int n, final T... components) {
        Array<Entity> entities = new Array<Entity>(n);
        for (int i = 0; i < n; i++) entities.add(emplace(components));
        return entities;
    }
    
    public Entity add(Entity e) {
        if (updating) tasks.add(() -> { addInternal(e); });
        else addInternal(e);
        return e;
    }
    
    private void addInternal(Entity e) {
        if (e.getEngine() != null || entities.contains(e)) throw new IllegalArgumentException("Entity already added to a registry");
        if (e.isEnabled()) throw new IllegalArgumentException("Entity is already enabled");
        
        entities.add(e);
        e.setEngine(this);
        e.enable();
        
        for (Family family : views.keySet()) if (family.isMember(e)) {
            views.get(family).add(e);
        }

        for (EntityListener l : listeners) l.onEntityAdd(e);
        
        for (Entry<Family, List<EntityListener>> entry : filteredListeners.entrySet()) {
        	if (entry.getKey().isMember(e)) {
                for (EntityListener l : entry.getValue()) { l.onEntityAdd(e); }
        	}
        }
    }

    public void destroy(Entity e) {
        if (updating) tasks.add(() -> { removeInternal(e); });
        else removeInternal(e);
    }

    public void destroy(List<Entity> entities) {
        for (Entity e : entities) destroy(e);
    }

    private void removeInternal(Entity e) {
        if (e.getEngine() != this) return;
        if (!e.isEnabled()) throw new IllegalArgumentException("Entity is not enabled");
        if (!entities.contains(e)) throw new IllegalArgumentException("Entity is not in this registry");

        // inform listeners as long as the entity is still active
        for (EntityListener l : listeners) l.onEntityRemove(e);
        
        for (Entry<Family, List<EntityListener>> entry : filteredListeners.entrySet()) {
        	if (entry.getKey().isMember(e)) {
                for (EntityListener l : entry.getValue()) { l.onEntityRemove(e); }
        	}
        }
        
        // actually remove entity
        e.disable();
        e.removeEngine();
        entities.remove(e);
        e.flush();
       
        for (List<Entity> view : views.values()) view.remove(e);
    }

    public void destroyAll() {
        if (updating) tasks.add(() -> { removeAllInternal(); });
        else removeAllInternal();
    }

    private void removeAllInternal() {
        while (!entities.isEmpty()) removeInternal(entities.get(0));
    }

    public Entity release(Entity e) {
        if (updating) tasks.add(() -> { releaseInternal(e); });
        else releaseInternal(e);
        return e;
    }

    public Entity release(List<Entity> entities) {
        for (Entity e : entities) release(e);
        return entities.get(0);
    }

    private void releaseInternal(Entity e) {
        if (e.getEngine() != this) throw new IllegalArgumentException("Entity is not in this registry");
        if (!e.isEnabled()) throw new IllegalArgumentException("Entity is not enabled");
        
        e.disable();
        e.removeEngine();
        entities.remove(e);
        
        for (Family family : views.keySet()) if (family.isMember(e)) {
            views.get(family).remove(e);
        }

        for (EntityListener l : listeners) l.onEntityRemove(e);
        
        for (Entry<Family, List<EntityListener>> entry : filteredListeners.entrySet()) {
        	if (entry.getKey().isMember(e)) {
                for (EntityListener l : entry.getValue()) { l.onEntityRemove(e); }
        	}
        }
    }

    public void releaseAll() {
        if (updating) tasks.add(() -> { releaseAllInternal(); });
        else releaseAllInternal();
    }

    private void releaseAllInternal() {
        while (!entities.isEmpty()) releaseInternal(entities.get(0));
    }

    public void update(float dt) {
        if (updating) return;
        updating = true;
        
        // update systems
        for (EntitySystem p : systems) if (p.isEnabled()) {
            p.update(dt);
        }
        
        // execute pending commands
        while (!tasks.isEmpty()) {
            Task t = tasks.poll();
            t.execute();
        }
        
        updating = false;
    }

    public void dispose() {
        if (updating) return;

        for (int i = 0; i < entities.size(); i++) destroy(entities.get(i));

        entities.clear();
        views.clear();

        for (int i = systems.size() - 1; i >= 0; i--) {
            EntitySystem p = systems.get(i);
            p.disable();
            p.onUnbind(this);
            p.unbind();
        }
        
        systems.clear();
    }

    public final List<Entity> view(final Family family) {
        List<Entity> view = views.get(family);

        if (view == null) {
            view = new ArrayList<Entity>();
            views.put(family, view);

            initView(family, view);
        }

        return Collections.unmodifiableList(view);
    }

    @SafeVarargs
    public final List<Entity> view(final Class<? extends Component>... components) {
        return view(Family.define(components));
    }

    public Entity get(int index) {
        return entities.get(index);
    }

    public boolean has(Entity e) {
        return entities.contains(e);
    }
    
    private void initView(Family family, List<Entity> view) {
        if (!view.isEmpty()) return;

        for (Entity e : entities) if (family.isMember(e)) {
            view.add(e);
        }
    }

    // TODO: implement sorting
    public final <T extends Component> List<Entity> sort(final Class<T> clazz) {
        throw new UnsupportedOperationException("Sorting is not yet supported.");
    }

    public final <T extends Component> List<Entity> sort(final Class<T> clazz, final Comparator<Entity> comparator) {
        throw new UnsupportedOperationException("Sorting is not yet supported.");
    }

    public void register(EntityListener listener, Family family) {
        List<EntityListener> listeners = filteredListeners.get(family);
        if (listeners == null) {
            listeners = new ArrayList<EntityListener>();
            filteredListeners.put(family, listeners);
        }

        if (listeners.contains(listener)) return;
        listeners.add(listener);
    }

    public void register(EntityListener listener) {
        if (listeners.contains(listener)) return;
        listeners.add(listener);
    }

    public void unregister(EntityListener listener, Family family) {
        List<EntityListener> listeners = filteredListeners.get(family);
        if (listeners == null) return;
        listeners.remove(listener);
    }
    
    public void unregister(EntityListener listener) {
        listeners.remove(listener);
    }

    public void bind(EntitySystem system) throws IllegalStateException, IllegalArgumentException {
        if (updating) throw new IllegalStateException("cannot add system while updating");
        
        EntitySystem old = getSystem(system.getClass());
        
        if (old != null) removeSystem(old);
        
        system.bind(this);
        systems.add(system);
        systems.sort(EntitySystem.getComparator());
        system.onBind(this);
    }

    public void unbind(EntitySystem system) throws IllegalStateException, IllegalArgumentException {
        if (updating) throw new IllegalStateException("cannot remove system while updating");
        
        if (!systems.contains(system)) throw new IllegalArgumentException("system is unknown");
        
        system.onUnbind(this);
        systems.remove(system);
        system.unbind();        
    }

    public <T extends EntitySystem> T getSystem(Class<T> clazz) throws IllegalArgumentException {
        for (EntitySystem p : systems) if (clazz.isInstance(p)) {
            return clazz.cast(p);
        }
        
       return null;
    }
    
    public boolean hasSystem(Class<?> clazz) {
        for (EntitySystem p : systems) if (p.getClass() == clazz) {
            return true;
        }

        return false;
    }

    public <T extends EntitySystem> T removeSystem(T system) {
        if (updating) throw new IllegalStateException("cannot remove system while updating");
        
        if (!systems.contains(system)) throw new IllegalArgumentException("system is unknown");
        
        systems.remove(system);
        system.unbind();
        return system;
    }

    public EntitySystem getSystem(int index) {
        return systems.get(index);
    }

    public boolean hasSystem(EntitySystem p) {
        return systems.contains(p);
    }
    
    public int systemCount() {
        return systems.size();
    }

    public int entityCount() {
        return entities.size();
    }

    private interface Task {

        void execute();

    }

}
