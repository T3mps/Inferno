package com.temprovich.apollo.system;

import java.util.ArrayList;
import java.util.List;

import com.temprovich.apollo.Entity;
import com.temprovich.apollo.EntityListener;
import com.temprovich.apollo.Family;
import com.temprovich.apollo.Registry;

public abstract class IterativeIntervalSystem extends IntervalSystem implements EntityListener {

    private Family family;
    private List<Entity> entities;

    public IterativeIntervalSystem(Family family, float interval) {
        this(family, interval, 0);
    }

    public IterativeIntervalSystem(Family family, float interval, int priority) {
        super(interval, priority);
        this.family = family;
        this.entities = new ArrayList<Entity>();
    }

    @Override
    public void onBind(Registry registry) {
        this.entities = registry.view(family);
        registry.register(this, family);
    }

    @Override
    public void onUnbind(Registry registry) {
        this.entities = null;
    }

    @Override
    protected void intervalUpdate() {
        push();

        for (int i = 0; i < entities.size(); i++) processEntity(entities.get(i));

        pop();
    }

    protected void push() {}

    protected abstract void processEntity(Entity entity);

    protected void pop() {}

    @Override
    public void onEntityAdd(Entity entity) {
        entities.add(entity);
    }

    @Override
    public void onEntityRemove(Entity entity) {
        entities.remove(entity);
    }

    public Family getFamily() {
        return family;
    }

    public List<Entity> getEntities() {
        return entities;
    }
    
}
