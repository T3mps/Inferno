package com.temprovich.apollo.system;

import java.util.ArrayList;
import java.util.List;

import com.temprovich.apollo.Entity;
import com.temprovich.apollo.EntityListener;
import com.temprovich.apollo.Family;
import com.temprovich.apollo.Registry;

public abstract class IterativeSystem extends AbstractEntitySystem implements EntityListener {

    private Family family;

    private List<Entity> entities;

    public IterativeSystem(Family family) {
        this(family, 0);
    }

    public IterativeSystem(Family family, int priority) {
        super(priority);
        this.family = family;
        this.entities = new ArrayList<Entity>();
    }

    @Override
    public void update(float dt) {
        for (int i = 0; i < entities.size(); i++) process(entities.get(i), dt);
    }

    protected abstract void process(Entity entity, float dt);

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
    public void onEntityAdd(Entity entity) {
        entities.add(entity);
    }

    @Override
    public void onEntityRemove(Entity entity) {
        entities.remove(entity);
    }

    public List<Entity> getEntities() {
        return entities;
    }
    
}
