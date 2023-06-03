package com.starworks.inferno;

import java.io.Closeable;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.starworks.inferno.collections.ClassMap.ClassIndex;
import com.starworks.inferno.ArchetypeList.Node;
import com.starworks.inferno.collections.ChunkedPool;
import com.starworks.inferno.collections.ChunkedPool.IDFactory;
import com.starworks.inferno.util.StripedLock;
import com.starworks.inferno.util.StripedLock.StripedReadWriteLock;

public final class Registry implements Closeable {

	public static final int DEFAULT_SYSTEM_TIMEOUT_SECONDS = 3;

	private final ChunkedPool<Entity> m_entityPool;
	private final ArchetypeList m_archetypeList;
	private final EventSink m_eventSink;
	private final Deque<Scheduler> m_schedulers;
	private final int m_systemTimeoutSeconds;
	private final StripedReadWriteLock m_lock;
	private final GsonBuilder m_gsonBuilder;

	public Registry() {
		this(DEFAULT_SYSTEM_TIMEOUT_SECONDS);
	}

	public Registry(int systemTimeoutSeconds) {
		this(systemTimeoutSeconds, new GsonBuilder().setPrettyPrinting());
	}

	public Registry(GsonBuilder gsonBuilder) {
		this(DEFAULT_SYSTEM_TIMEOUT_SECONDS, gsonBuilder);
	}

	public Registry(int systemTimeoutSeconds, GsonBuilder gsonBuilder) {
		this.m_entityPool = new ChunkedPool<Entity>();
		this.m_archetypeList = new ArchetypeList(this);
		this.m_eventSink = new EventSink();
		this.m_schedulers = new ConcurrentLinkedDeque<Scheduler>();
		this.m_systemTimeoutSeconds = systemTimeoutSeconds;
		this.m_lock = StripedLock.stripedReadWriteLock();
		this.m_gsonBuilder = gsonBuilder;
	}

	public Entity create() {
		Archetype archetype = m_archetypeList.getBaseArchetype();
		Entity entity = archetype.createEntity(this);
		return entity;
	}

	public Entity emplace(Object... components) {
		Object[] componentArray = components.length == 0 ? null : components;
		Archetype archetype = m_archetypeList.getOrCreate(componentArray);
		Entity entity = archetype.createEntity(this, componentArray);
		int size = components.length;
		for (int i = 0; i < size; ++i) {
			m_eventSink.emit(EventSink.ListenerType.ON_COMPONENT_ADD, components[i].getClass(), entity, components[i]);
		}
		return entity;
	}

	public Entity emulate(Entity entity, Object... addedComponents) {
		Object[] originComponents = entity.getComponents();
		int originComponentsLength = originComponents.length;
		if (originComponents == null || originComponentsLength == 0) {
			return emplace(addedComponents);
		}
		Object[] targetComponents = new Object[originComponentsLength + addedComponents.length];
		System.arraycopy(originComponents, 0, targetComponents, 0, originComponentsLength);
		System.arraycopy(addedComponents, 0, targetComponents, originComponentsLength, addedComponents.length);
		return emplace(targetComponents);
	}

	public String serialize(Entity entity) {
		Gson gson = m_gsonBuilder.create();
		Object[] components = entity.getComponents();
		int componentsLength = components.length;
		Object[] entityData = new Object[componentsLength << 1];
		for (int i = 0, j = 0; j < componentsLength; ++i, ++j) {
			Object component = components[j];
			Class<?> componentClass = component.getClass();
			entityData[i] = componentClass.getTypeName();
			entityData[++i] = component;
		}
		return gson.toJson(entityData);
	}

	public String[] parallelSerialize(Entity... entities) {
		int numThreads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);

		int length = entities.length;
		String[] serializedEntities = new String[length];
		for (int i = 0; i < length; ++i) {
			int index = i;
			Entity entity = entities[i];
			executor.submit(() -> {
				try {
					m_lock.writeLock(index);
					try {
						serializedEntities[index] = serialize(entity);
					} finally {
						m_lock.unlockWrite(index);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
		}
		executor.shutdown();
		return serializedEntities;
	}

	public Entity deserialize(String json) {
		Gson gson = m_gsonBuilder.create();
		Object[] entityData = gson.fromJson(json, Object[].class);
		int componentsLength = entityData.length >> 1;
		Object[] components = new Object[componentsLength];
		for (int i = 0, j = 0; j < componentsLength; ++i, ++j) {
			String fullyQualifiedClassName = (String) entityData[i];
			String serializedComponent = gson.toJson(entityData[++i]);
			try {
				Class<?> componentClass = Class.forName(fullyQualifiedClassName);
				Object component = gson.fromJson((String) serializedComponent, componentClass);
				components[j] = component;
			} catch (ClassNotFoundException e) {
				throw new IllegalStateException("Unable to deserialize component " + fullyQualifiedClassName + " due to class location being ambiguous");
			}
		}
		return emplace(components);
	}

	public Entity[] parallelDeserialize(String... serializedEntities) {
		int numThreads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);

		int length = serializedEntities.length;
		Entity[] entities = new Entity[length];
		for (int i = 0; i < length; ++i) {
			int index = i;
			String serializedEntity = serializedEntities[i];
			executor.submit(() -> {
				try {
					m_lock.readLock(index);
					try {
						entities[index] = deserialize(serializedEntity);
					} finally {
						m_lock.unlockRead(index);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
		}
		executor.shutdown();
		return entities;
	}

	public boolean destroy(Entity entity) {
		if (!validate(entity)) {
			return false;
		}
		return entity.getArchetype().destroy(entity);
	}

	public void release(Entity entity) {
		if (!validate(entity)) {
			return;
		}
		entity.getArchetype().detach(entity);
		entity.setID(IDFactory.RELEASED_BIT);
	}

	public boolean validate(Entity entity) {
		return !((entity.getID() & IDFactory.DETACHED_BIT) == IDFactory.DETACHED_BIT);
	}

	public Entity add(Entity entity, Object... components) {
		if (components.length == 0) {
			return entity;
		}
		int componentsLength = components.length;
		Archetype prevArchetype = entity.getArchetype();
		Object[] entityComponents = entity.getComponents();
		int prevComponentsLength = prevArchetype.length();
		if (prevComponentsLength == 0) {
			Archetype archetype = m_archetypeList.getOrCreate(components);
			Entity result = archetype.attach(prevArchetype.detach(entity), components);
			for (int i = 0; i < componentsLength; ++i) {
				m_eventSink.emit(EventSink.ListenerType.ON_COMPONENT_ADD, components[i].getClass(), result, components[i]);
			}
			return result;
		}
		Object[] newComponentArray = new Object[prevComponentsLength + componentsLength];
		if (prevComponentsLength == 1) {
			newComponentArray[0] = entityComponents[0];
		} else {
			System.arraycopy(entityComponents, 0, newComponentArray, 0, prevComponentsLength);
		}
		if (componentsLength == 1) {
			newComponentArray[prevComponentsLength] = components[0];
		} else {
			System.arraycopy(components, 0, newComponentArray, prevComponentsLength, componentsLength);
		}
		Archetype archetype = m_archetypeList.getOrCreate(newComponentArray);
		prevArchetype.detach(entity);
		Entity result = archetype.attach(entity, newComponentArray);
		for (int i = 0; i < components.length; ++i) {
			m_eventSink.emit(EventSink.ListenerType.ON_COMPONENT_ADD, components[i].getClass(), result, components[i]);
		}
		return result;
	}

	public Object replace(Entity entity, Object component) {
		if (entity == null) return null;
		Object result = entity.replace(component);
		m_eventSink.emit(EventSink.ListenerType.ON_COMPONENT_REPLACE, component.getClass(), entity, component);
		return result;
	}

	public Object remove(Entity entity, Class<?> componentType) {
		if (entity == null || componentType == null) return null;
		
		Archetype prevArchetype = entity.getArchetype();
		Object[] entityComponents = entity.getComponents();
		int prevComponentsLength = prevArchetype.length();
		if (prevComponentsLength == 0) return null;

		Object[] newComponentArray;
		Object removed;
		if (prevComponentsLength == 1) {
			newComponentArray = null;
			removed = entityComponents[0];
		} else {
			newComponentArray = new Object[prevComponentsLength - 1];
			int removedIndex = prevArchetype.indexOf(componentType);
			removed = entityComponents[removedIndex];
			if (removedIndex > 0) {
				System.arraycopy(entityComponents, 0, newComponentArray, 0, removedIndex);
			}
			if (removedIndex < prevComponentsLength - 1) {
				System.arraycopy(entityComponents, removedIndex + 1, newComponentArray, removedIndex, prevComponentsLength - (removedIndex + 1));
			}
		}
		Archetype archetype = m_archetypeList.getOrCreate(newComponentArray);
		prevArchetype.detach(entity);
		archetype.attach(entity, newComponentArray);
		m_eventSink.emit(EventSink.ListenerType.ON_COMPONENT_REMOVE, componentType, entity, removed);
		return removed;
	}

	public Object get(Entity entity, Class<?> componentType) {
		if (entity == null) return null;
		return entity.get(componentType);
	}

	public boolean contains(Entity entity, Class<?> componentType) {
		if (entity == null) return false;
		return entity.contains(componentType);
	}

	public boolean contains(Entity entity, Object component) {
		if (entity == null) return false;
		return entity.contains(component);
	}

	public <T> View<View.With1<T>> view(Class<T> componentType) {
		Map<ClassIndex, Node> nodes = m_archetypeList.find(componentType);
		if (nodes == null) return null;
		return new View.Of1<T>(m_archetypeList, nodes, componentType);
	}

	public <T1, T2> View<View.With2<T1, T2>> view(Class<T1> componentType1, Class<T2> componentType2) {
		Map<ClassIndex, Node> nodes = m_archetypeList.find(componentType1, componentType2);
		if (nodes == null) return null;
		return new View.Of2<T1, T2>(m_archetypeList, nodes, componentType1, componentType2);
	}

	public <T1, T2, T3> View<View.With3<T1, T2, T3>> view(Class<T1> componentType1, Class<T2> componentType2, Class<T3> componentType3) {
		Map<ClassIndex, Node> nodes = m_archetypeList.find(componentType1, componentType2, componentType3);
		if (nodes == null) return null;
		return new View.Of3<T1, T2, T3>(m_archetypeList, nodes, componentType1, componentType2, componentType3);
	}

	public <T1, T2, T3, T4> View<View.With4<T1, T2, T3, T4>> view(Class<T1> componentType1, Class<T2> componentType2, Class<T3> componentType3, Class<T4> componentType4) {
		Map<ClassIndex, Node> nodes = m_archetypeList.find(componentType1, componentType2, componentType3, componentType4);
		if (nodes == null) return null;
		return new View.Of4<T1, T2, T3, T4>(m_archetypeList, nodes, componentType1, componentType2, componentType3, componentType4);
	}

	public <T1, T2, T3, T4, T5> View<View.With5<T1, T2, T3, T4, T5>> view(Class<T1> componentType1, Class<T2> componentType2, Class<T3> componentType3, Class<T4> componentType4, Class<T5> componentType5) {
		Map<ClassIndex, Node> nodes = m_archetypeList.find(componentType1, componentType2, componentType3, componentType4, componentType5);
		if (nodes == null) return null;
		return new View.Of5<T1, T2, T3, T4, T5>(m_archetypeList, nodes, componentType1, componentType2, componentType3, componentType4, componentType5);
	}

	public <T1, T2, T3, T4, T5, T6> View<View.With6<T1, T2, T3, T4, T5, T6>> view(Class<T1> componentType1, Class<T2> componentType2, Class<T3> componentType3, Class<T4> componentType4, Class<T5> componentType5, Class<T6> componentType6) {
		Map<ClassIndex, Node> nodes = m_archetypeList.find(componentType1, componentType2, componentType3, componentType4, componentType5, componentType6);
		if (nodes == null) return null;
		return new View.Of6<T1, T2, T3, T4, T5, T6>(m_archetypeList, nodes, componentType1, componentType2, componentType3, componentType4, componentType5, componentType6);
	}

	public <T1, T2, T3, T4, T5, T6, T7> View<View.With7<T1, T2, T3, T4, T5, T6, T7>> view(Class<T1> componentType1, Class<T2> componentType2, Class<T3> componentType3, Class<T4> componentType4, Class<T5> componentType5, Class<T6> componentType6, Class<T7> componentType7) {
		Map<ClassIndex, Node> nodes = m_archetypeList.find(componentType1, componentType2, componentType3, componentType4, componentType5, componentType6, componentType7);
		if (nodes == null) return null;
		return new View.Of7<T1, T2, T3, T4, T5, T6, T7>(m_archetypeList, nodes, componentType1, componentType2, componentType3, componentType4, componentType5, componentType6, componentType7);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8> View<View.With8<T1, T2, T3, T4, T5, T6, T7, T8>> view(Class<T1> componentType1, Class<T2> componentType2, Class<T3> componentType3, Class<T4> componentType4, Class<T5> componentType5, Class<T6> componentType6, Class<T7> componentType7, Class<T8> componentType8) {
		Map<ClassIndex, Node> nodes = m_archetypeList.find(componentType1, componentType2, componentType3, componentType4, componentType5, componentType6, componentType7, componentType8);
		if (nodes == null) return null;
		return new View.Of8<T1, T2, T3, T4, T5, T6, T7, T8>(m_archetypeList, nodes, componentType1, componentType2, componentType3, componentType4, componentType5, componentType6, componentType7, componentType8);
	}

	public <T> void clear(View.Of1<T> view) {
		view.stream().forEach(v -> remove(v.entity(), v.component().getClass()));
	}

	public <T1, T2> void clear(View.Of2<T1, T2> view) {
		view.stream().forEach(v -> {
			remove(v.entity(), v.component1().getClass());
			remove(v.entity(), v.component2().getClass());
		});
	}

	public <T1, T2, T3> void clear(View.Of3<T1, T2, T3> view) {
		view.stream().forEach(v -> {
			remove(v.entity(), v.component1().getClass());
			remove(v.entity(), v.component2().getClass());
			remove(v.entity(), v.component3().getClass());
		});
	}

	public <T1, T2, T3, T4> void clear(View.Of4<T1, T2, T3, T4> view) {
		view.stream().forEach(v -> {
			remove(v.entity(), v.component1().getClass());
			remove(v.entity(), v.component2().getClass());
			remove(v.entity(), v.component3().getClass());
			remove(v.entity(), v.component4().getClass());
		});
	}

	public <T1, T2, T3, T4, T5> void clear(View.Of5<T1, T2, T3, T4, T5> view) {
		view.stream().forEach(v -> {
			remove(v.entity(), v.component1().getClass());
			remove(v.entity(), v.component2().getClass());
			remove(v.entity(), v.component3().getClass());
			remove(v.entity(), v.component4().getClass());
			remove(v.entity(), v.component5().getClass());
		});
	}

	public <T1, T2, T3, T4, T5, T6> void clear(View.Of6<T1, T2, T3, T4, T5, T6> view) {
		view.stream().forEach(v -> {
			remove(v.entity(), v.component1().getClass());
			remove(v.entity(), v.component2().getClass());
			remove(v.entity(), v.component3().getClass());
			remove(v.entity(), v.component4().getClass());
			remove(v.entity(), v.component5().getClass());
			remove(v.entity(), v.component6().getClass());
		});
	}

	public <T1, T2, T3, T4, T5, T6, T7> void clear(View.Of7<T1, T2, T3, T4, T5, T6, T7> view) {
		view.stream().forEach(v -> {
			remove(v.entity(), v.component1().getClass());
			remove(v.entity(), v.component2().getClass());
			remove(v.entity(), v.component3().getClass());
			remove(v.entity(), v.component4().getClass());
			remove(v.entity(), v.component5().getClass());
			remove(v.entity(), v.component6().getClass());
			remove(v.entity(), v.component7().getClass());
		});
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8> void clear(View.Of8<T1, T2, T3, T4, T5, T6, T7, T8> view) {
		view.stream().forEach(v -> {
			remove(v.entity(), v.component1().getClass());
			remove(v.entity(), v.component2().getClass());
			remove(v.entity(), v.component3().getClass());
			remove(v.entity(), v.component4().getClass());
			remove(v.entity(), v.component5().getClass());
			remove(v.entity(), v.component6().getClass());
			remove(v.entity(), v.component7().getClass());
			remove(v.entity(), v.component8().getClass());
		});
	}

	public <T> boolean destroy(View.Of1<T> view) {
		view.stream().forEach(v -> destroy(v.entity()));
		return true;
	}

	public <T1, T2> boolean destroy(View.Of2<T1, T2> view) {
		view.stream().forEach(v -> destroy(v.entity()));
		return true;
	}

	public <T1, T2, T3> boolean destroy(View.Of3<T1, T2, T3> view) {
		view.stream().forEach(v -> destroy(v.entity()));
		return true;
	}

	public <T1, T2, T3, T4> boolean destroy(View.Of4<T1, T2, T3, T4> view) {
		view.stream().forEach(v -> destroy(v.entity()));
		return true;
	}

	public <T1, T2, T3, T4, T5> boolean destroy(View.Of5<T1, T2, T3, T4, T5> view) {
		view.stream().forEach(v -> destroy(v.entity()));
		return true;
	}

	public <T1, T2, T3, T4, T5, T6> boolean destroy(View.Of6<T1, T2, T3, T4, T5, T6> view) {
		view.stream().forEach(v -> destroy(v.entity()));
		return true;
	}

	public <T1, T2, T3, T4, T5, T6, T7> boolean destroy(View.Of7<T1, T2, T3, T4, T5, T6, T7> view) {
		view.stream().forEach(v -> destroy(v.entity()));
		return true;
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8> boolean destroy(View.Of8<T1, T2, T3, T4, T5, T6, T7, T8> view) {
		view.stream().forEach(v -> destroy(v.entity()));
		return true;
	}

	public <T> boolean release(View.Of1<T> view) {
		view.stream().forEach(v -> release(v.entity()));
		return true;
	}

	public <T1, T2> boolean release(View.Of2<T1, T2> view) {
		view.stream().forEach(v -> release(v.entity()));
		return true;
	}

	public <T1, T2, T3> boolean release(View.Of3<T1, T2, T3> view) {
		view.stream().forEach(v -> release(v.entity()));
		return true;
	}

	public <T1, T2, T3, T4> boolean release(View.Of4<T1, T2, T3, T4> view) {
		view.stream().forEach(v -> release(v.entity()));
		return true;
	}

	public <T1, T2, T3, T4, T5> boolean release(View.Of5<T1, T2, T3, T4, T5> view) {
		view.stream().forEach(v -> release(v.entity()));
		return true;
	}

	public <T1, T2, T3, T4, T5, T6> boolean release(View.Of6<T1, T2, T3, T4, T5, T6> view) {
		view.stream().forEach(v -> release(v.entity()));
		return true;
	}

	public <T1, T2, T3, T4, T5, T6, T7> boolean release(View.Of7<T1, T2, T3, T4, T5, T6, T7> view) {
		view.stream().forEach(v -> release(v.entity()));
		return true;
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8> boolean release(View.Of8<T1, T2, T3, T4, T5, T6, T7, T8> view) {
		view.stream().forEach(v -> release(v.entity()));
		return true;
	}

	public EventSink eventSink() {
		return m_eventSink;
	}

	public Scheduler createScheduler() {
		Scheduler scheduler = new Scheduler(this, m_systemTimeoutSeconds);
		m_schedulers.push(scheduler);
		return scheduler;
	}

	@Override
	public void close() {
		m_schedulers.stream().forEach(Scheduler::close);
		m_eventSink.clear();
		m_archetypeList.close();
		m_entityPool.close();
	}

	public ChunkedPool<Entity> getEntityPool() {
		return m_entityPool;
	}

	public ArchetypeList getArchetypeList() {
		return m_archetypeList;
	}

	public Deque<Scheduler> getSchedulers() {
		return m_schedulers;
	}

	public int getSystemTimeoutSeconds() {
		return m_systemTimeoutSeconds;
	}

	public GsonBuilder getGsonBuilder() {
		return m_gsonBuilder;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String tab = "    ";
		sb.append("Registry {\n");
		sb.append(tab).append(m_entityPool.toString()).append('\n');
		sb.append(tab).append(m_archetypeList.toString(tab)).append('\n');
		sb.append(tab).append(m_eventSink.toString()).append('\n');
		sb.append(tab).append("Schedulers[");
		Iterator<Scheduler> iterator = m_schedulers.iterator();
		boolean comma = iterator.hasNext();
		while (iterator.hasNext()) {
			var scheduler = iterator.next();
			sb.append(scheduler.toString());
			sb.append(", ");
		}
		if (comma) {
			sb.setLength(sb.length() - 2);
		}
		sb.append("]\n");
		sb.append("}");
		return sb.toString();
	}
}
