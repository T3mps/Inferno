package com.temprovich.inferno.system;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.temprovich.inferno.Entity;
import com.temprovich.inferno.Family;
import com.temprovich.inferno.Registry;

public abstract class IterativeIntervalSystem extends IntervalSystem implements Iterable<Entity> {

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
        var view = registry.view(family);
        
        for (var entity : view) {
            entities.add(entity);
        }
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

    public Family getFamily() {
        return family;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    @Override
    public Iterator<Entity> iterator() {
        return entities.iterator();
    }
}
