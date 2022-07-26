package com.temprovich.inferno.system;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.temprovich.inferno.Entity;
import com.temprovich.inferno.EntityListener;
import com.temprovich.inferno.Family;
import com.temprovich.inferno.Registry;

public abstract class SortedIterativeSystem extends AbstractEntitySystem implements EntityListener, Iterable<Entity> {

    private Family family;

    private final List<Entity> entities;
    private List<Entity> sortedEntities;
    private Comparator<Entity> entityComparator;
    
    private boolean sort = false;

    public SortedIterativeSystem(Family family, Comparator<Entity> entityComparator) {
        this(family, entityComparator, 0);
    }

    public SortedIterativeSystem(Family family, Comparator<Entity> entityComparator, int priority) {
        super(priority);
        this.family = family;
        this.entities = new ArrayList<Entity>();
        this.sortedEntities = new ArrayList<Entity>(16);
        this.sort = false;
        this.entityComparator = entityComparator;
    }

    public void force() {
        sort = true;
    }

    protected void sort() {
        if (sort) {
            sortedEntities.sort(entityComparator);
            sort = false;
        }
    }

    @Override
    public void update(float dt) {
        sort();

        push();
        
        for (int i = 0; i < sortedEntities.size(); i++) {
            processEntity(sortedEntities.get(i), dt);
        }

        pop();
    }

    protected void push() {}

    protected abstract void processEntity(Entity entity, float dt);

    protected void pop() {}

    @Override
    public void onBind(Registry registry) {
        var view = registry.view(family);

        for (var entity : view) {
            entities.add(entity);
        }
        sortedEntities.clear();

        if (!view.isEmpty()) {
            for (int i = 0; i < view.size(); i++) sortedEntities.add(view.get(i));
            sortedEntities.sort(entityComparator);
        }

        sort = false;
        registry.register(this, family);
    }

    @Override
    public void onUnbind(Registry registry) {
        registry.unregister(this);
        sortedEntities.clear();
        sort = false;
    }

    @Override
    public void onEntityAdd(Entity e) {
        sortedEntities.add(e);
        sort = true;
    }

    @Override
    public void onEntityRemove(Entity e) {
        sortedEntities.remove(e);
        sort = true;
    }

    public Family getFamily() {
        return family;
    }
    
    public List<Entity> getEntities() {
        return entities;
    }

    @Override
    public Iterator<Entity> iterator() {
        return sortedEntities.iterator();
    }
}
