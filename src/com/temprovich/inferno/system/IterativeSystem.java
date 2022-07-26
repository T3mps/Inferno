package com.temprovich.inferno.system;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.temprovich.inferno.Entity;
import com.temprovich.inferno.Family;
import com.temprovich.inferno.Registry;

public abstract class IterativeSystem extends AbstractEntitySystem implements Iterable<Entity> {

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
        var view = registry.view(family);
        
        for (var entity : view) {
            entities.add(entity);
        }
    }

    @Override
    public void onUnbind(Registry registry) {
        entities = null;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    @Override
    public Iterator<Entity> iterator() {
        return entities.iterator();
    }
}
