package com.temprovich.inferno.hierarchy;

import com.temprovich.inferno.Entity;

public class Hierarchy {
    
    private Member root;

    public Hierarchy(Entity entity) {
        this.root = new Member(entity);
    }

    public Hierarchy(Hierarchy hierarchy) {
        this.root = new Member(hierarchy.root);
    }

    public boolean has(Entity key) {
        // return find(root, key);
        return false;
    }
}
