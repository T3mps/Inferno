package com.temprovich.inferno;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Family implements Iterable<Class<?>>, Comparable<Family> {
    
    private final Set<Class<?>> types;

    @SafeVarargs
    private Family(final Class<?>... types) {
        if (types.length == 0) {
            throw new IllegalArgumentException("Family must have at least one type");
        }
        this.types = new HashSet<Class<?>>();
        for (var type : types) {
            this.types.add(type);
        }
    }

    private Family(final Family family) {
        this.types = new HashSet<Class<?>>();
        this.types.addAll(family.types);
    }

    @SafeVarargs
    public static Family define(final Class<?>... types) {
        return new Family(types);
    }

    public static Family define(final Family family) {
        return new Family(family);
    }

    public static Family define(final Entity entity) {
        Family family = new Family();
        for (var component : entity.getComponents()) {
            family.types.add(component.getClass());
        }

        return family;
    }

    @SafeVarargs
    public static Family define(final Family original, final Class<? extends Component>... types) {
        Family family = new Family(original);
        for (var type : types) {
            family.types.add(type);
        }
        
        return family;
    }

    public final boolean isMember(final Entity entity) {
        for (var type : types) {
            if (!entity.has(type)) {
                return false;
            }
        }

        return true;
    }

    public final boolean isRelated(final Entity entity) {
        for (var type : types) {
            if (entity.has(type)) {
                return true;
            }
        }

        return false;
    }

    public final boolean isSubsetOf(final Family family) {
        for (var type : types) {
            if (!family.types.contains(type)) {
                return false;
            }
        }

        return true;
    }

    public final boolean isSupersetOf(final Family family) {
        for (var type : family.types) {
            if (!types.contains(type)) {
                return false;
            }
        }

        return true;
    }

    public final boolean isDisjointFrom(final Family family) {
        for (var type : types) {
            if (family.types.contains(type)) {
                return false;
            }
        }

        return true;
    }

    public final boolean has(final Class<?> type) {
        return types.contains(type);
    }

    public final Class<?>[] getTypes() {
        return types.toArray(new Class<?>[0]);
    }

    @Override
    public int compareTo(Family o) {
        return types.hashCode() - o.types.hashCode();
    }

    @Override
    public Iterator<Class<?>> iterator() {
        return types.iterator();
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((types == null) ? 0 : types.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Family)) return false;
        Family other = (Family) obj;
        if (types == null) {
            if (other.types != null) return false;
        } else if (!types.equals(other.types)) return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Family [types=");
        for (var type : types) {
            builder.append(type.getSimpleName());
            builder.append(", ");
        }
        builder.append("]");
        return builder.toString();
    }

    private class FamilyIterator implements Iterator<Class<?>> {
            
            private int index = 0;
            
            @Override
            public boolean hasNext() {
                return index < types.size();
            }
    
            @Override
            public Class<?> next() {
                return types.toArray(new Class<?>[0])[index++];
            }
            
    }
}
