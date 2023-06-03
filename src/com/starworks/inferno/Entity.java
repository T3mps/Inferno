package com.starworks.inferno;

import com.starworks.inferno.collections.ChunkedPool.IDFactory;
import com.starworks.inferno.collections.ChunkedPool.Poolable;

public final class Entity implements Poolable {

	private int m_id;
	private Registry m_registry;
	private Archetype m_archetype;
	private Object[] m_components;

	Entity(int id, Registry registry, Archetype archetype, Object... components) {
		this.setID(id);
		this.m_registry = registry;
		this.m_archetype = archetype;
		this.m_components = components;
	}

	public Entity add(Object... components) {
		if (!isEnabled()) return null;
		return m_registry.add(this, components);
	}

	public Object replace(Object component) {
		if (component == null || !isEnabled()) return null;
		int index = m_archetype.indexOf((Class<?>) component.getClass());
		var oldComponent = m_components[index];
		if (oldComponent == null) return null;
		m_components[index] = component;
		return oldComponent;
	}

	public Object remove(Object component) {
		if (!isEnabled() || !contains(component)) return null;
		return m_registry.remove(this, component.getClass());
	}

	public Object remove(Class<?> componentType) {
		if (!isEnabled() || !contains(componentType)) return null;
		return m_registry.remove(this, componentType);
	}

	public <T> T get(Class<T> componentType) {
		int index = m_archetype.indexOf(componentType);
		if (index < 0) return null;
		var component = m_components[index];
		if (componentType.isInstance(component)) return componentType.cast(component);
		return null;
	}

	public boolean contains(Class<?> componentType) {
		if (componentType == null || m_components == null) return false;
		if (m_archetype.length() == 1) return m_components[0].getClass().equals(componentType);
		return m_archetype.indexOf(componentType) > -1;
	}

	public boolean contains(Object component) {
		if (component == null || m_components == null) return false;
		if (m_archetype.length() == 1) m_components[0].equals(component);
		int index = m_archetype.indexOf(component.getClass());
		if (index > -1) return m_components[index].equals(component);
		return false;
	}

	public <S extends Enum<S>> void register(S signal, EventSink.ComponentEvent event) {
		m_registry.eventSink().register(this, signal, event);
	}

	public <S extends Enum<S>> void unregister(S signal) {
		m_registry.eventSink().unregister(this, signal);
	}

	public String serialize() {
		return m_registry.serialize(this);
	}
	
	@Override
	public int getID() {
		return m_id;
	}

	@Override
	public void setID(int id) {
		m_id = id;
	}

	public String getFormattedID() {
		return m_registry.getEntityPool().getIDFactory().idToString(m_id);
	}

	public boolean isEnabled() {
		return m_registry.validate(this);
	}

	public Entity setEnabled(boolean enabled) {
		if (enabled && !isEnabled()) return m_archetype.reattach(this);
		else if (!enabled && isEnabled()) return m_archetype.detach(this);
		return this;
	}

	public boolean isReleased() {
		return (m_id & IDFactory.RELEASED_BIT) == IDFactory.RELEASED_BIT;
	}

	public Registry getRegistry() {
		return m_registry;
	}
	
	Entity setRegistry(Registry registry) {
		m_registry = registry;
		return this;
	}
	
	public Archetype getArchetype() {
		return m_archetype;
	}
	
	Entity setArchetype(Archetype archetype) {
		m_archetype = archetype;
		return this;
	}

	public Object[] getComponents() {
		return m_components;
	}

	Entity setComponents(Object[] components) {
		m_components = components;
		return this;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + m_id;
		result = prime * result + ((m_components == null) ? 0 : m_components.hashCode());
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Entity[");
		sb.append("id=[");
		sb.append(m_registry.getEntityPool().getIDFactory().idToString(m_id));
		sb.append("], components={");
		if (m_components != null && m_components.length > 0) {
			for (int i = 0; i < m_components.length; i++) {
				if (i > 0) sb.append(", ");
				sb.append(m_components[i].toString());
			}
		}
		sb.append("}]");
		return sb.toString();
	}
}
