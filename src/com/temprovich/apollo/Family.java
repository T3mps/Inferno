package com.temprovich.apollo;

import java.util.HashSet;
import java.util.Set;

import com.temprovich.apollo.component.Component;

public final class Family {

    private final Set<Class<?>> types = new HashSet<Class<?>>();

    @SafeVarargs
    private Family(final Class<?>... types) {
        if (types.length == 0) throw new IllegalArgumentException("Family must have at least one type");
        for (Class<?> type : types) this.types.add(type);
    }

    private Family(final Family family) {
        this.types.addAll(family.types);
    }

    @SafeVarargs
    public static Family define(final Class<?>... types) {
        return new Family(types);
    }

    public static Family define(final Family family) {
        Family result = new Family(family);
        return result;
    }

    public static Family define(final Entity entity) {
        Family family = new Family();
        for (var component : entity.getComponents()) family.types.add(component.getClass());
        return family;
    }

    @SafeVarargs
    public static Family define(final Family original, final Class<? extends Component>... types) {
        Family family = new Family(original);
        for (var type : types) family.types.add(type);
        return family;
    }

    public final boolean isMember(final Entity entity) {
        for (var type : types) if (!entity.has(type)) {
            return false;
        }

        return true;
    }

    public final boolean isRelated(final Entity entity) {
        for (var type : types) if (entity.has(type)) {
            return true;
        }

        return false;
    }

    public final boolean isSubsetOf(final Family family) {
        for (var type : types) if (!family.types.contains(type)) {
            return false;
        }

        return true;
    }

    public final boolean isSupersetOf(final Family family) {
        for (var type : family.types) if (!types.contains(type)) {
            return false;
        }

        return true;
    }

    public final boolean isDisjointFrom(final Family family) {
        for (var type : types) if (family.types.contains(type)) {
            return false;
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

}
