package com.temprovich.inferno;

public final class Inferno {
    
    public static final Entity create() {
        return new Entity();
    }

    public static final Entity emulate(final Entity entity) {
        return new Entity(entity);
    }
}
