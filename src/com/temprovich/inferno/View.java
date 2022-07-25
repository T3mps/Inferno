package com.temprovich.inferno;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class View implements Iterable<Entity> {

    private Family family;
    private Entity[] entities;

    View(Family family, List<Entity> entities) {
        this.family = family;
        this.entities = new Entity[entities.size()];
        
        for (int i = 0; i < entities.size(); i++) {
            this.entities[i] = entities.get(i);
        }
    }

    public Entity get(int index) {
        return entities[index];
    }

    public boolean contains(Entity entity) {
        for (int i = 0; i < entities.length; i++) {
            if (entities[i] == entity) {
                return true;
            }
        }

        return false;
    }

    public boolean isEmpty() {
        if (entities.length == 0) {
            return true;
        }

        // check for null entities
        for (int i = 0; i < entities.length; i++) {
            if (entities[i] != null) {
                return false;
            }
        }

        return true;
    }

    public Family getFamily() {
        return family;
    }

    public Entity[] getEntities() {
        return entities;
    }

    public int size() {
        return entities.length;
    }

    @Override
    public Iterator<Entity> iterator() {
        return new ViewIterator(this);
    }

    public void each(Consumer<Entity> consumer) {
        for (var entity : this) {
            consumer.accept(entity);
        }
    }

    public void each(Consumer<Entity> consumer, int start, int end) {
        for (int i = start; i < end; i++) {
            consumer.accept(entities[i]);
        }
    }

    public Entity[] toArray() {
        return entities;
    }

    public Entity[] toArray(Entity[] array) {
        if (array.length < entities.length) {
            array = new Entity[entities.length];
        }
        
        System.arraycopy(entities, 0, array, 0, entities.length);
        return array;
    }

    public List<Entity> asList() {
        return Arrays.asList(entities);
    }

    public Stream<Entity> stream() {
        return Arrays.stream(entities);
    }

    public Stream<Entity> parallelStream() {
        return Arrays.stream(entities).parallel();
    }

    private static class ViewIterator implements Iterator<Entity> {

        private View view;
        private int index;

        public ViewIterator(View view) {
            this.view = view;
            this.index = 0;
        }

        @Override
        public boolean hasNext() {
            return index < view.size();
        }

        @Override
        public Entity next() {
            return view.get(index++);
        }
    }
}
