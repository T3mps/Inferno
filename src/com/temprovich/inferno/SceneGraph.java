package com.temprovich.inferno;

import java.util.List;
import java.util.ArrayList;

public final class SceneGraph<T> {
    
    private SceneNode<T> root;

    public SceneGraph() {
        this.root = new SceneNode<T>();
    }

    public static final class SceneNode<T> {

        private T value;
        private SceneNode<T> parent;
        private final List<SceneNode<T>> children;

        public SceneNode() {
            this.value = null;
            this.parent = null;
            this.children = new ArrayList<SceneNode<T>>();
        }

        public SceneNode(T value) {
            this.value = value;
            this.parent = null;
            this.children = new ArrayList<SceneNode<T>>();
        }

        public SceneNode(T value, SceneNode<T> parent) {
            this.value = value;
            this.parent = parent;
            this.children = new ArrayList<SceneNode<T>>();
        }

        public void giveChild(T child) {
            SceneNode<T> node = new SceneNode<T>(child, this);
            children.add(node);
        }
        
        public void giveChild(SceneNode<T> child) {
            if (child.parent != null) {
                child.parent.children.remove(child);
            }

            child.parent = this;
            children.add(child);
        }
        
        public SceneNode<T> nthChild(int index) {
            return children.get(index);
        }

        public boolean hasChild(T child) {
            for (SceneNode<T> node : children) {
                if (node.value.equals(child)) {
                    return true;
                }
            }

            return false;
        }

        public boolean hasChild(SceneNode<T> child) {
            return children.contains(child);
        }

        public SceneNode<T> findChild(T child) {
            for (SceneNode<T> node : children) {
                if (node.value.equals(child)) {
                    return node;
                }
            }

            return null;
        }
        
        public SceneNode<T> removeChild(T child) {
            SceneNode<T> node = findChild(child);
            if (node != null) {
                node.parent = null;
                children.remove(node);
            }

            return node;
        }

        public SceneNode<T> removeChild(SceneNode<T> child) {
            if (child.parent != this) {
                return null;
            }

            child.parent = null;
            children.remove(child);

            return child;
        }

        public T getValue() {
            return value;
        }
        
        public void setValue(T value) {
            this.value = value;
        }

        public boolean isRoot() {
            return parent == null;
        }

        public SceneNode<T> getParent() {
            return parent;
        }

        public void setParent(SceneNode<T> parent) {
            this.parent = parent;
        }

        public List<SceneNode<T>> getChildren() {
            return children;
        }

        public boolean hasChildren() {
            return !children.isEmpty();
        }

        public int getChildCount() {
            return children.size();
        }
    }
}
