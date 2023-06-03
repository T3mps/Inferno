package com.starworks.inferno;

import com.starworks.inferno.collections.ChunkedPool;
import com.starworks.inferno.collections.ChunkedPool.IDFactory;

public final class Archetype {

	public static final int COMPONENT_INDEX_CAPACITY = 1 << 10;

	private final ArchetypeList m_list;
	private final ChunkedPool.Allocator<Entity> m_allocator;
	private final Class<?>[] m_componentTypes;
	private final int[] m_componentIndices;

	protected Archetype(ArchetypeList archetypeList, ChunkedPool.Allocator<Entity> allocator, Class<?>... componentTypes) {
		this.m_list = archetypeList;
		this.m_allocator = allocator;
		this.m_componentTypes = componentTypes;
		if (m_componentTypes.length < 1) {
			this.m_componentIndices = null;
			return;
		}
		this.m_componentIndices = new int[COMPONENT_INDEX_CAPACITY];
		for (int i = 0; i < componentTypes.length; ++i) {
			m_componentIndices[archetypeList.getClassMap().indexOf(componentTypes[i])] = i + 1;
		}
	}

	public Entity createEntity(Registry registry, Object... components) {
		int id = m_allocator.nextID();
		return m_allocator.register(id, new Entity(id, registry, this, sort(components)));
	}

	public boolean destroy(Entity entity) {
		int id = entity.getID();
		m_allocator.freeID(id);
		entity.setID(id | IDFactory.DETACHED_BIT);
		entity.setArchetype(null);
		entity.setComponents(null);
		return true;
	}

	public Entity attach(Entity entity, Object... components) {
		entity.setID(m_allocator.nextID());
		entity = m_allocator.register(entity.getID(), switch (m_componentTypes.length) {
		case 00 -> entity.setArchetype(this).setComponents(null);
		case 01 -> entity.setArchetype(this).setComponents(components);
		default -> entity.setArchetype(this).setComponents(sort(components));
		});
		entity.setRegistry(m_list.getRegistry());
		return entity;
	}

	public Entity detach(Entity entity) {
		m_allocator.freeID(entity.getID());
		entity.setID(entity.getID() | IDFactory.DETACHED_BIT);
		return entity;
	}
	
	public Entity reattach(Entity entity) {
		entity.setID(m_allocator.nextID());
		m_allocator.register(entity.getID(), entity);
		entity.setRegistry(m_list.getRegistry());
		return entity;
	}
	
	private Object[] sort(Object[] components) {
		if (components.length == 1) {
			return components;
		}
		
		int nextIndex;
		for (int i = 0; i < components.length; i++) {
			nextIndex = indexOf(components[i].getClass());
			if (nextIndex != i) {
				var temp = components[nextIndex];
				components[nextIndex] = components[i];
				components[i] = temp;
			}
		}
		nextIndex = indexOf(components[0].getClass());
		if (nextIndex > 0) {
			Object temp = components[nextIndex];
			components[nextIndex] = components[0];
			components[0] = temp;;
		}
		return components;
	}

	public int indexOf(Class<?> componentType) {
		if (m_componentIndices == null) return 0;
		return m_componentIndices[m_list.getClassMap().indexOf(componentType)] - 1;
	}

	public int length() {
		return m_componentTypes.length;
	}

	public ArchetypeList getParentList() {
		return m_list;
	}

	public ChunkedPool.Allocator<Entity> getAllocator() {
		return m_allocator;
	}

	public Class<?>[] getComponentTypes() {
		return m_componentTypes;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Archetype[");
		for (int i = 0; i < m_componentTypes.length; i++) {
			sb.append(m_componentTypes[i].getSimpleName());
			if (i < m_componentTypes.length - 1) {
				sb.append(",");
			}
		}
		sb.append("]");
		return sb.toString();
	}
}
