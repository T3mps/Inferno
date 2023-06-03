package com.starworks.inferno;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.starworks.inferno.collections.ClassMap;
import com.starworks.inferno.collections.ClassMap.ClassIndex;

public final class ArchetypeList implements Closeable {

	private final Registry m_registry;
	private final ClassMap m_classMap;
	private final ConcurrentMap<ClassIndex, Node> m_nodes;
	private final Archetype m_baseArchetype;

	public ArchetypeList(Registry registry) {
		this.m_registry = registry;
		this.m_classMap = new ClassMap();
		this.m_nodes = new ConcurrentHashMap<ClassIndex, Node>();
		this.m_baseArchetype = new Archetype(this, registry.getEntityPool().newAllocator());
	}

	public Archetype getOrCreate(Object[] components) {
		int componentsLength = components == null ? 0 : components.length;
		Node node;
		switch (componentsLength) {
		case 0:
			return m_baseArchetype;
		case 1:
			Class<?> componentType = components[0].getClass();
			node = m_nodes.get(m_classMap.getClassIndex(m_classMap.indexOf(componentType)));
			if (node != null) {
				node.link(m_classMap.getClassIndex(m_classMap.indexOf(componentType)), node);
				break;
			}
			var key = m_classMap.getClassIndex(m_classMap.indexOfOrAdd(componentType));
			node = m_nodes.get(key);
			if (node == null) {
				node = getOrCreateNode(key, componentType);
			}
			break;
		default:
			var classIndex = m_classMap.getClassIndex(components);
			node = m_nodes.get(classIndex);
			if (node == null) {
				Class<?>[] componentTypes = new Class<?>[components.length];
				for (int i = 0; i < components.length; i++) {
					componentTypes[i] = components[i].getClass();
				}
				node = getOrCreateNode(classIndex, componentTypes);
			}
			break;
		}

		return node.getArchetype();
	}

	public Map<ClassIndex, Node> find(Class<?>... componentTypes) {
		switch (componentTypes.length) {
		case 0:
			return null;
		case 1:
			Node node = m_nodes.get(m_classMap.getClassIndex(m_classMap.indexOf(componentTypes[0])));
			return node == null ? null : node.getLinkedNodes();
		default:
			Map<ClassIndex, Node> currentArchetypes = null;
			int size = componentTypes.length;
			for (int i = 0; i < size; ++i) {
				node = m_nodes.get(m_classMap.getClassIndex(m_classMap.indexOf(componentTypes[i])));
				if (node == null) continue;
				currentArchetypes = currentArchetypes == null ? node.getLinkedNodes() : intersect(currentArchetypes, node.m_linkedNodes);
			}
			return currentArchetypes;
		}
	}

	void include(Map<ClassIndex, Node> nodeMap, Class<?>... componentTypes) {
		if (componentTypes.length == 0) return;
		for (var componentType : componentTypes) {
			Node node = m_nodes.get(m_classMap.getClassIndex(m_classMap.indexOf(componentType)));
			if (node == null) continue;
			intersect(nodeMap, node.m_linkedNodes);
		}
	}

	void exclude(Map<ClassIndex, Node> nodeMap, Class<?>... componentTypes) {
		if (componentTypes.length == 0) return;
		for (var componentType : componentTypes) {
			var classIndex = m_classMap.getClassIndex(m_classMap.indexOf(componentType));
			nodeMap.remove(classIndex);
			Node node = m_nodes.get(classIndex);
			if (node != null) {
				for (var linkedNodeKey : node.m_linkedNodes.keySet()) {
					nodeMap.remove(linkedNodeKey);
				}
			}
		}
	}

	private Map<ClassIndex, Node> intersect(Map<ClassIndex, Node> subject, Map<ClassIndex, Node> other) {
		Set<ClassIndex> indexKeySet = subject.keySet();
		Iterator<ClassIndex> iterator = indexKeySet.iterator();
		while (iterator.hasNext()) {
			if (!other.containsKey(iterator.next())) {
				iterator.remove();
			}
		}
		return subject;
	}

	private Node getOrCreateNode(ClassIndex key, Class<?>... componentTypes) {
		Node node = m_nodes.computeIfAbsent(key, k -> new Node(componentTypes));
		int size = componentTypes.length;
		if (size <= 1) {
			node.link(key, node);
			return node;
		}
		for (int i = 0; i < size; i++) {
			Class<?> componentType = componentTypes[i];
			var typeKey = m_classMap.getClassIndex(m_classMap.indexOf(componentType));
			Node singleTypeNode = m_nodes.computeIfAbsent(typeKey, k -> new Node(componentType));
			singleTypeNode.link(key, node);
		}
		return node;
	}

	@Override
	public void close() {
		m_nodes.clear();
		m_classMap.close();
	}

	public Registry getRegistry() {
		return m_registry;
	}

	public ClassMap getClassMap() {
		return m_classMap;
	}

	protected Archetype getBaseArchetype() {
		return m_baseArchetype;
	}

	@Override
	public String toString() {
		return m_nodes.values().stream().map(Node::toString).collect(Collectors.joining("\n"));
	}

	public String toString(String prefix) {
		StringBuilder sb = new StringBuilder();
		for (Node n : m_nodes.values()) {
			sb.append(n.toString(prefix)).append("\n");
		}
		boolean empty = m_nodes.values().size() == 0;
		if (!empty) {
			sb.setLength(sb.length() - 1);
			sb.delete(0, prefix.length());
			sb.insert(0, "ArchetypeList\u251c\u2500\u2510\n" + prefix + "\u250c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2518\n" + prefix);
			sb.replace(sb.length() - 1, sb.length(), "\u2534");
			return sb.toString();
		}
		sb.append("ArchetypeList\u251c\u2524");
		return sb.toString();
	}

	final class Node {

		private final Map<ClassIndex, Node> m_linkedNodes;
		private Archetype m_archetype;

		private Node(Class<?>... componentTypes) {
			this.m_linkedNodes = new ConcurrentHashMap<ClassIndex, Node>();
			this.m_archetype = componentTypes.length == 0 ? null : new Archetype(ArchetypeList.this, ArchetypeList.this.m_registry.getEntityPool().newAllocator(), componentTypes);
		}

		public void link(ClassIndex key, Node node) {
			m_linkedNodes.putIfAbsent(key, node);
		}

		public Map<ClassIndex, Node> getLinkedNodes() {
			return new ConcurrentHashMap<ClassIndex, Node>(m_linkedNodes);
		}

		public Archetype getArchetype() {
			return m_archetype;
		}

		@Override
		public String toString() {
			Set<Node> visitedNodes = new HashSet<Node>();
			StringBuilder sb = new StringBuilder();
			toString(sb, "", visitedNodes, "\u251C\u2500\u2500");
			return sb.toString();
		}

		public String toString(String prefix) {
			Set<Node> visitedNodes = new HashSet<Node>();
			StringBuilder sb = new StringBuilder();
			toString(sb, prefix, visitedNodes, "\u251C\u2500\u2500");
			return sb.toString();
		}

		private void toString(StringBuilder sb, String prefix, Set<Node> visitedNodes, String treePrefix) {
			if (visitedNodes.contains(this)) {
				return;
			}
			visitedNodes.add(this);
			sb.append(prefix).append(treePrefix).append(m_linkedNodes.isEmpty() ? "Node" : "LinkedNode").append("[archetype={");
			if (m_archetype != null) {
				sb.append(m_archetype.toString().substring("Archetype[".length()));
				sb.setLength(sb.length() - 1);
			}
			sb.append("}]").append(System.lineSeparator()).append(prefix).append("\u2502");
			if (m_linkedNodes.isEmpty()) return;
			Iterator<Node> iterator = m_linkedNodes.values().iterator();
			while (iterator.hasNext()) {
				Node child = iterator.next();
				if (iterator.hasNext()) {
					child.toString(sb, prefix, visitedNodes, "\u251C\u2500\u2500");
				} else {
					child.toString(sb, prefix, visitedNodes, "\u2514\u2500\u2500");
				}
			}
		}
	}
}
