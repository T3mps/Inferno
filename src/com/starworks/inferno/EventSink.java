package com.starworks.inferno;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public final class EventSink {

	public static enum ListenerType {
		ON_COMPONENT_ADD,
		ON_COMPONENT_REPLACE,
		ON_COMPONENT_REMOVE;
	}

	public static final class Any {
		private Any() {}
	}

	public static final Class<Any> ANY = Any.class;
	
	private final LinkedSignal m_rootSignal;
	private final Map<Class<?>, List<ComponentEvent>> m_onComponentAddListeners;
	private final Map<Class<?>, List<ComponentEvent>> m_onComponentReplaceListeners;
	private final Map<Class<?>, List<ComponentEvent>> m_onComponentRemoveListeners;

	EventSink() {
		this.m_rootSignal = new LinkedSignal();
		this.m_onComponentAddListeners = new ConcurrentHashMap<Class<?>, List<ComponentEvent>>();
		m_onComponentAddListeners.put(Any.class, new CopyOnWriteArrayList<ComponentEvent>());
		this.m_onComponentReplaceListeners = new ConcurrentHashMap<Class<?>, List<ComponentEvent>>();
		m_onComponentReplaceListeners.put(Any.class, new CopyOnWriteArrayList<ComponentEvent>());
		this.m_onComponentRemoveListeners = new ConcurrentHashMap<Class<?>, List<ComponentEvent>>();
		m_onComponentRemoveListeners.put(Any.class, new CopyOnWriteArrayList<ComponentEvent>());
	}

	public <S extends Enum<S>> void register(Entity entity, S signal, ComponentEvent event) {
		if (entity == null || !entity.isEnabled() || signal == null || event == null) {
			return;
		}
		if ((m_rootSignal.type() == null ? m_rootSignal.setType(signal) : false) || m_rootSignal.type() == signal) {
			m_rootSignal.linkEvent(event, entity);
			return;
		}
		LinkedSignal current = m_rootSignal;
		do {
			if (current.type() == signal) {
				current.linkEvent(event, entity);
				break;
			}
			if (current.getNext() == null) {
				LinkedSignal next = new LinkedSignal(signal);
				current.setNext(next);
				next.linkEvent(event, entity);
				break;
			}

			current = current.getNext();
		} while (current != null);
	}

	public <T, S extends Enum<S>> void register(View.Of1<T> view, S signal, ComponentEvent event) {
		view.stream().forEach(v -> register(v.entity(), signal, event));
	}

	public <T1, T2, S extends Enum<S>> void register(View.Of2<T1, T2> view, S signal, ComponentEvent event) {
		view.stream().forEach(v -> register(v.entity(), signal, event));
	}

	public <T1, T2, T3, S extends Enum<S>> void register(View.Of3<T1, T2, T3> view, S signal, ComponentEvent event) {
		view.stream().forEach(v -> register(v.entity(), signal, event));
	}

	public <T1, T2, T3, T4, S extends Enum<S>> void register(View.Of4<T1, T2, T3, T4> view, S signal, ComponentEvent event) {
		view.stream().forEach(v -> register(v.entity(), signal, event));
	}

	public <T1, T2, T3, T4, T5, S extends Enum<S>> void register(View.Of5<T1, T2, T3, T4, T5> view, S signal, ComponentEvent event) {
		view.stream().forEach(v -> register(v.entity(), signal, event));
	}

	public <T1, T2, T3, T4, T5, T6, S extends Enum<S>> void register(View.Of6<T1, T2, T3, T4, T5, T6> view, S signal, ComponentEvent event) {
		view.stream().forEach(v -> register(v.entity(), signal, event));
	}

	public <T1, T2, T3, T4, T5, T6, T7, S extends Enum<S>> void register(View.Of7<T1, T2, T3, T4, T5, T6, T7> view, S signal, ComponentEvent event) {
		view.stream().forEach(v -> register(v.entity(), signal, event));
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, S extends Enum<S>> void register(View.Of8<T1, T2, T3, T4, T5, T6, T7, T8> view, S signal, ComponentEvent event) {
		view.stream().forEach(v -> register(v.entity(), signal, event));
	}

	public <S extends Enum<S>> void unregister(Entity entity, S signal) {
		if (entity == null || !entity.isEnabled() || signal == null) {
			return;
		}
		LinkedSignal current = m_rootSignal;
		do {
			if (current.type() == signal) {
				current.unlinkEntity(entity);
				break;
			}
		} while ((current = current.getNext()) != null);
	}

	public <T, S extends Enum<S>> void unregister(View.Of1<T> view, S signal) {
		view.stream().forEach(v -> unregister(v.entity(), signal));
	}

	public <T1, T2, S extends Enum<S>> void unregister(View.Of2<T1, T2> view, S signal) {
		view.stream().forEach(v -> unregister(v.entity(), signal));
	}

	public <T1, T2, T3, S extends Enum<S>> void unregister(View.Of3<T1, T2, T3> view, S signal) {
		view.stream().forEach(v -> unregister(v.entity(), signal));
	}

	public <T1, T2, T3, T4, S extends Enum<S>> void unregister(View.Of4<T1, T2, T3, T4> view, S signal) {
		view.stream().forEach(v -> unregister(v.entity(), signal));
	}

	public <T1, T2, T3, T4, T5, S extends Enum<S>> void unregister(View.Of5<T1, T2, T3, T4, T5> view, S signal) {
		view.stream().forEach(v -> unregister(v.entity(), signal));
	}

	public <T1, T2, T3, T4, T5, T6, S extends Enum<S>> void unregister(View.Of6<T1, T2, T3, T4, T5, T6> view, S signal) {
		view.stream().forEach(v -> unregister(v.entity(), signal));
	}

	public <T1, T2, T3, T4, T5, T6, T7, S extends Enum<S>> void unregister(View.Of7<T1, T2, T3, T4, T5, T6, T7> view, S signal) {
		view.stream().forEach(v -> unregister(v.entity(), signal));
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, S extends Enum<S>> void unregister(View.Of8<T1, T2, T3, T4, T5, T6, T7, T8> view, S signal) {
		view.stream().forEach(v -> unregister(v.entity(), signal));
	}

	public <S extends Enum<S>> void unregister(S signal, ComponentEvent event) {
		if (signal == null || event == null) {
			return;
		}
		LinkedSignal current = m_rootSignal;
		do {
			if (current.type() == signal) {
				current.unlinkEvent(event);
				break;
			}
		} while ((current = current.getNext()) != null);
	}

	public <S extends Enum<S>> void unregister(S signal) {
		if (signal == null) {
			return;
		}
		LinkedSignal current = m_rootSignal;
		LinkedSignal previous = null;
		do {
			if (current.type() == signal) {
				LinkedSignal next = current.getNext();
				if (previous != null) previous.setNext(next == null ? null : next);
				break;
			}

			previous = current;
		} while ((current = current.getNext()) != null);
	}

	public <S extends Enum<S>> void receive(S signal, Object data) {
		LinkedSignal current = m_rootSignal;
		do {
			if (current.type() == signal) {
				current.emit(data);
				break;
			}
		} while ((current = current.getNext()) != null);
	}

	public <S extends Enum<S>> void receiveAll(Object data) {
		m_rootSignal.linkedEmit(data);
	}

	public void connect(ListenerType type, Class<?> componentType, ComponentEvent listener) {
		if (type == null || componentType == null || listener == null) {
			return;
		}

		List<ComponentEvent> listeners;
		switch (type) {
		case ON_COMPONENT_ADD -> m_onComponentAddListeners.putIfAbsent(componentType, listeners = m_onComponentAddListeners.getOrDefault(componentType, new CopyOnWriteArrayList<ComponentEvent>()));
		case ON_COMPONENT_REPLACE -> m_onComponentReplaceListeners.putIfAbsent(componentType, listeners = m_onComponentReplaceListeners.getOrDefault(componentType, new CopyOnWriteArrayList<ComponentEvent>()));
		case ON_COMPONENT_REMOVE -> m_onComponentRemoveListeners.putIfAbsent(componentType, listeners = m_onComponentRemoveListeners.getOrDefault(componentType, new CopyOnWriteArrayList<ComponentEvent>()));
		default -> throw new IllegalArgumentException("Unknown listener type `" + type + "` connection attempted");
		}

		listeners.add(listener);
	}

	public void disconnect(ListenerType type, Class<?> componentType, ComponentEvent listener) {
		if (type == null || componentType == null || listener == null) {
			return;
		}

		List<ComponentEvent> listeners = switch (type) {
		case ON_COMPONENT_ADD -> m_onComponentAddListeners.getOrDefault(componentType, null);
		case ON_COMPONENT_REPLACE -> m_onComponentReplaceListeners.getOrDefault(componentType, null);
		case ON_COMPONENT_REMOVE -> m_onComponentAddListeners.getOrDefault(componentType, null);
		default -> throw new IllegalArgumentException("Unknown listener type `" + type + "` disconnection attempted");
		};
		if (listeners == null || listeners.isEmpty()) {
			return;
		}

		listeners.remove(listener);
	}

	protected void emit(ListenerType type, Class<?> componentType, Entity entity, Object data) {
		Iterator<ComponentEvent> specializedIterator;
		Iterator<ComponentEvent> anyIterator;
		List<ComponentEvent> listeners;
		switch (type) {
		case ON_COMPONENT_ADD:
			listeners = m_onComponentAddListeners.getOrDefault(componentType, null);
			specializedIterator = listeners == null ? null : listeners.iterator();
			anyIterator = m_onComponentAddListeners.get(Any.class).iterator();
			break;
		case ON_COMPONENT_REPLACE:
			listeners = m_onComponentReplaceListeners.getOrDefault(componentType, null);
			specializedIterator = listeners == null ? null : listeners.iterator();
			anyIterator = m_onComponentReplaceListeners.get(Any.class).iterator();
			break;
		case ON_COMPONENT_REMOVE:
			listeners = m_onComponentRemoveListeners.getOrDefault(componentType, null);
			specializedIterator = listeners == null ? null : listeners.iterator();
			anyIterator = m_onComponentRemoveListeners.get(Any.class).iterator();
			break;
		default:
			throw new IllegalArgumentException("Unknown listener type: " + type);
		}

		if (specializedIterator != null) {
			while (specializedIterator.hasNext()) {
				var listener = specializedIterator.next();
				listener.accept(entity, data);
			}
		}
		while (anyIterator.hasNext()) {
			var listener = anyIterator.next();
			listener.accept(entity, data);
		}
	}

	public void clear() {
		m_rootSignal.clear();
		m_onComponentAddListeners.clear();
		m_onComponentReplaceListeners.clear();
		m_onComponentRemoveListeners.clear();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("EventSink[");
		sb.append("signals={");
		Iterator<LinkedSignal> iterator = m_rootSignal.iterator();
		boolean comma = iterator.hasNext();
		while (iterator.hasNext()) {
			sb.append(iterator.next());
			sb.append(", ");
		}
		if (comma) sb.setLength(sb.length() - 2);
		sb.append("}, componentListeners={");
		sb.append("ON_COMPONENT_ADD={");
		for (var entry : m_onComponentAddListeners.entrySet()) {
			sb.append("[");
			sb.append(entry.getKey().getSimpleName());
			sb.append(", ");
			entry.getValue().forEach(e -> {

			});
			int lambda = 0;
			for (var event : entry.getValue()) {
				String name = event.getClass().getSimpleName();
				if (name.isEmpty() || name.contains("$")) {
					name = "LambdaEvent" + lambda++;
				}
				sb.append(name);
				sb.append(", ");
			}
			if (sb.charAt(sb.length() - 2) == ',') {
				sb.delete(sb.length() - 2, sb.length());
			}
			sb.append("], ");
		}
		if (sb.charAt(sb.length() - 2) == ',') {
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append("}, ON_COMPONENT_REPLACE={");
		for (var entry : m_onComponentReplaceListeners.entrySet()) {
			sb.append("[");
			sb.append(entry.getKey().getSimpleName());
			sb.append(", ");
			entry.getValue().forEach(e -> {

			});
			int lambda = 0;
			for (var event : entry.getValue()) {
				String name = event.getClass().getSimpleName();
				if (name.isEmpty() || name.contains("$")) {
					name = "LambdaEvent" + lambda++;
				}
				sb.append(name);
				sb.append(", ");
			}
			if (sb.charAt(sb.length() - 2) == ',') {
				sb.delete(sb.length() - 2, sb.length());
			}
			sb.append("], ");
		}
		if (sb.charAt(sb.length() - 2) == ',') {
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append("}, ON_COMPONENT_REMOVE={");
		for (var entry : m_onComponentRemoveListeners.entrySet()) {
			sb.append("[");
			sb.append(entry.getKey().getSimpleName());
			sb.append(", ");
			entry.getValue().forEach(e -> {

			});
			int lambda = 0;
			for (var event : entry.getValue()) {
				String name = event.getClass().getSimpleName();
				if (name.isEmpty() || name.contains("$")) {
					name = "LambdaEvent" + lambda++;
				}
				sb.append(name);
				sb.append(", ");
			}
			if (sb.charAt(sb.length() - 2) == ',') {
				sb.delete(sb.length() - 2, sb.length());
			}
			sb.append("], ");
		}
		if (sb.charAt(sb.length() - 2) == ',') {
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append("}]");
		return sb.toString();
	}

	public static interface ComponentEvent extends BiConsumer<Entity, Object> {

		@Override
		public abstract void accept(Entity entity, Object data);
	}

	private static final class LinkedSignal implements Iterable<LinkedSignal> {

		private Enum<? extends Enum<?>> m_signalType;
		private final Map<ComponentEvent, List<Entity>> m_eventMap;
		private LinkedSignal m_next;
		private int m_index;

		private LinkedSignal() {
			this(null);
		}

		private LinkedSignal(Enum<? extends Enum<?>> signalType) {
			this.m_signalType = signalType;
			this.m_eventMap = new ConcurrentHashMap<ComponentEvent, List<Entity>>();
			this.m_next = null;
			this.m_index = 0;
		}

		private void linkEvent(ComponentEvent event, Entity entity) {
			List<Entity> eventGroup = m_eventMap.get(event);
			m_eventMap.putIfAbsent(event, (eventGroup == null ? (eventGroup = new ArrayList<Entity>()) : eventGroup));
			eventGroup.add(entity);
		}

		private void unlinkEntity(Entity entity) {
			Iterator<Map.Entry<ComponentEvent, List<Entity>>> iterator = m_eventMap.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<ComponentEvent, List<Entity>> entry = iterator.next();
				List<Entity> entities = entry.getValue();
				entities.removeIf(e -> e.equals(entity));
				if (entities.isEmpty()) {
					iterator.remove();
				}
			}
		}

		private void unlinkEvent(ComponentEvent event) {
			m_eventMap.remove(event);
		}

		private void emit(Object data) {
			m_eventMap.entrySet().forEach(entry -> entry.getValue().stream().parallel().forEach(e -> entry.getKey().accept(e, data)));
		}

		private LinkedSignal linkedEmit(Object data) {
			LinkedSignal signal = this;
			do {
				signal.emit(data);
			} while ((signal = signal.m_next) != null);
			return this;
		}

		private void clear() {
			m_signalType = null;
			m_eventMap.clear();
			m_next = null;
		}

		@Override
		public Iterator<LinkedSignal> iterator() {
			return new LinkedSignalIterator(this);
		}

		private Enum<? extends Enum<?>> type() {
			return m_signalType;
		}

		private boolean setType(Enum<? extends Enum<?>> type) {
			this.m_signalType = type;
			return true;
		}

		private LinkedSignal getNext() {
			return m_next;
		}

		private LinkedSignal setNext(LinkedSignal next) {
			this.m_next = next;
			this.m_next.m_index = m_index + 1;
			return next;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("LinkedSignal[signalType=");
			sb.append(m_signalType);
			sb.append(", events={");
			int lambda = 0;
			for (var entry : m_eventMap.entrySet()) {
				String name = entry.getKey().getClass().getSimpleName();
				if (name.isEmpty() || name.contains("$")) {
					name = "LambdaEvent" + lambda++;
				}
				sb.append(name);
				sb.append(": ");
				sb.append(entry.getValue().size());
				sb.append(", ");
			}
			int index;
			if (sb.charAt(index = sb.length() - 2) == ',') {
				sb.delete(index, sb.length());
			}
			sb.append("}, index=");
			sb.append(m_index).append("]");
			return sb.toString();
		}
	}

	public static final class LinkedSignalIterator implements Iterator<LinkedSignal> {

		private LinkedSignal m_current;

		public LinkedSignalIterator(LinkedSignal head) {
			this.m_current = head;
		}

		@Override
		public boolean hasNext() {
			return m_current != null;
		}

		@Override
		public LinkedSignal next() {
			LinkedSignal result = m_current;
			m_current = m_current.getNext();
			return result;
		}
	}
}
