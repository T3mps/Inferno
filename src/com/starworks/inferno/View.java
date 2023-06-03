package com.starworks.inferno;

import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.starworks.inferno.collections.ClassMap.ClassIndex;

public abstract class View<T> implements Iterable<T> {

	private final ArchetypeList m_archetypeList;
	private final Map<ClassIndex, ArchetypeList.Node> m_nodeMap;

	private View(ArchetypeList archetypeList, Map<ClassIndex, ArchetypeList.Node> nodeMap) {
		this.m_archetypeList = archetypeList;
		this.m_nodeMap = nodeMap;
	}

	public final View<T> include(Class<?>... componentTypes) {
		m_archetypeList.include(m_nodeMap, componentTypes);
		return this;
	}
	
	public final View<T> exclude(Class<?>... componentTypes) {
		m_archetypeList.exclude(m_nodeMap, componentTypes);
		return this;
	}

	protected abstract Iterator<T> archetypeIterator(Archetype archetype);

	@Override
	public final Iterator<T> iterator() {
		Iterator<ArchetypeList.Node> iterator;
		return m_nodeMap != null ?
					(m_nodeMap.size() > 1 ?
						new EntityTupleIterator<T>(this, m_nodeMap.values().iterator()) :
							(iterator = m_nodeMap.values().iterator()).hasNext() ?
								archetypeIterator(iterator.next().getArchetype()) :
									new IteratorWith0<T>()) :
										new IteratorWith0<T>();
	}

	public final Stream<T> stream() {
		return StreamSupport.stream(spliterator(), false);
	}
	
	@Override
	public final Spliterator<T> spliterator() {
		return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED);
	}

	public record With1<T>(T component, Entity entity) {
	}

	public record With2<T1, T2>(T1 component1, T2 component2, Entity entity) {
	}

	public record With3<T1, T2, T3>(T1 component1, T2 component2, T3 component3, Entity entity) {
	}

	public record With4<T1, T2, T3, T4>(T1 component1, T2 component2, T3 component3, T4 component4, Entity entity) {
	}

	public record With5<T1, T2, T3, T4, T5>(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, Entity entity) {
	}

	public record With6<T1, T2, T3, T4, T5, T6>(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, Entity entity) {
	}

	public record With7<T1, T2, T3, T4, T5, T6, T7>(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7, Entity entity) {
	}

	public record With8<T1, T2, T3, T4, T5, T6, T7, T8>(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7, T8 component8, Entity entity) {
	}

	public static final class Of1<T> extends View<With1<T>> {
		
		private final Class<T> type;

		Of1(ArchetypeList archetypeList, Map<ClassIndex, ArchetypeList.Node> nodeMap, Class<T> type) {
			super(archetypeList, nodeMap);
			this.type = type;
		}

		@Override
		protected Iterator<With1<T>> archetypeIterator(Archetype archetype) {
			Iterator<Entity> iterator = archetype.getAllocator().iterator();
			return new View.IteratorWith1<T>(archetype.indexOf(type), iterator, archetype);
		}
	}

	public static final class Of2<T1, T2> extends View<With2<T1, T2>> {
		
		private final Class<T1> type1;
		private final Class<T2> type2;

		Of2(ArchetypeList archetypeList, Map<ClassIndex, ArchetypeList.Node> nodeMap, Class<T1> type1, Class<T2> type2) {
			super(archetypeList, nodeMap);
			this.type1 = type1;
			this.type2 = type2;
		}

		@Override
		protected Iterator<With2<T1, T2>> archetypeIterator(Archetype archetype) {
			Iterator<Entity> iterator = archetype.getAllocator().iterator();
			return new View.IteratorWith2<T1, T2>(archetype.indexOf(type1), archetype.indexOf(type2), iterator, archetype);
		}
	}

	public static final class Of3<T1, T2, T3> extends View<With3<T1, T2, T3>> {
		
		private final Class<T1> type1;
		private final Class<T2> type2;
		private final Class<T3> type3;

		Of3(ArchetypeList archetypeList, Map<ClassIndex, ArchetypeList.Node> nodeMap, Class<T1> type1, Class<T2> type2, Class<T3> type3) {
			super(archetypeList, nodeMap);
			this.type1 = type1;
			this.type2 = type2;
			this.type3 = type3;
		}

		@Override
		protected Iterator<With3<T1, T2, T3>> archetypeIterator(Archetype archetype) {
			Iterator<Entity> iterator = archetype.getAllocator().iterator();
			return new View.IteratorWith3<T1, T2, T3>(archetype.indexOf(type1), archetype.indexOf(type2), archetype.indexOf(type3), iterator, archetype);
		}
	}

	public static final class Of4<T1, T2, T3, T4> extends View<With4<T1, T2, T3, T4>> {
		
		private final Class<T1> type1;
		private final Class<T2> type2;
		private final Class<T3> type3;
		private final Class<T4> type4;

		Of4(ArchetypeList archetypeList, Map<ClassIndex, ArchetypeList.Node> nodeMap, Class<T1> type1, Class<T2> type2, Class<T3> type3, Class<T4> type4) {
			super(archetypeList, nodeMap);
			this.type1 = type1;
			this.type2 = type2;
			this.type3 = type3;
			this.type4 = type4;
		}

		@Override
		protected Iterator<With4<T1, T2, T3, T4>> archetypeIterator(Archetype archetype) {
			Iterator<Entity> iterator = archetype.getAllocator().iterator();
			return new View.IteratorWith4<T1, T2, T3, T4>(archetype.indexOf(type1), archetype.indexOf(type2), archetype.indexOf(type3), archetype.indexOf(type4), iterator, archetype);
		}
	}

	public static final class Of5<T1, T2, T3, T4, T5> extends View<With5<T1, T2, T3, T4, T5>> {
		
		private final Class<T1> type1;
		private final Class<T2> type2;
		private final Class<T3> type3;
		private final Class<T4> type4;
		private final Class<T5> type5;

		Of5(ArchetypeList archetypeList, Map<ClassIndex, ArchetypeList.Node> nodeMap, Class<T1> type1, Class<T2> type2, Class<T3> type3, Class<T4> type4, Class<T5> type5) {
			super(archetypeList, nodeMap);
			this.type1 = type1;
			this.type2 = type2;
			this.type3 = type3;
			this.type4 = type4;
			this.type5 = type5;
		}

		@Override
		protected Iterator<With5<T1, T2, T3, T4, T5>> archetypeIterator(Archetype archetype) {
			Iterator<Entity> iterator = archetype.getAllocator().iterator();
			return new View.IteratorWith5<T1, T2, T3, T4, T5>(archetype.indexOf(type1), archetype.indexOf(type2), archetype.indexOf(type3), archetype.indexOf(type4),
															  archetype.indexOf(type5), iterator, archetype);
		}
	}

	public static final class Of6<T1, T2, T3, T4, T5, T6> extends View<With6<T1, T2, T3, T4, T5, T6>> {
		
		private final Class<T1> type1;
		private final Class<T2> type2;
		private final Class<T3> type3;
		private final Class<T4> type4;
		private final Class<T5> type5;
		private final Class<T6> type6;

		Of6(ArchetypeList archetypeList, Map<ClassIndex, ArchetypeList.Node> nodeMap, Class<T1> type1, Class<T2> type2, Class<T3> type3, Class<T4> type4, Class<T5> type5, Class<T6> type6) {
			super(archetypeList, nodeMap);
			this.type1 = type1;
			this.type2 = type2;
			this.type3 = type3;
			this.type4 = type4;
			this.type5 = type5;
			this.type6 = type6;
		}

		@Override
		protected Iterator<With6<T1, T2, T3, T4, T5, T6>> archetypeIterator(Archetype archetype) {
			Iterator<Entity> iterator = archetype.getAllocator().iterator();
			return new View.IteratorWith6<T1, T2, T3, T4, T5, T6>(archetype.indexOf(type1), archetype.indexOf(type2), archetype.indexOf(type3), archetype.indexOf(type4),
																  archetype.indexOf(type5), archetype.indexOf(type6), iterator, archetype);
		}
	}

	public static final class Of7<T1, T2, T3, T4, T5, T6, T7> extends View<With7<T1, T2, T3, T4, T5, T6, T7>> {
		
		private final Class<T1> type1;
		private final Class<T2> type2;
		private final Class<T3> type3;
		private final Class<T4> type4;
		private final Class<T5> type5;
		private final Class<T6> type6;
		private final Class<T7> type7;

		Of7(ArchetypeList archetypeList, Map<ClassIndex, ArchetypeList.Node> nodeMap, Class<T1> type1, Class<T2> type2, Class<T3> type3, Class<T4> type4, Class<T5> type5, Class<T6> type6, Class<T7> type7) {
			super(archetypeList, nodeMap);
			this.type1 = type1;
			this.type2 = type2;
			this.type3 = type3;
			this.type4 = type4;
			this.type5 = type5;
			this.type6 = type6;
			this.type7 = type7;
		}

		@Override
		protected Iterator<With7<T1, T2, T3, T4, T5, T6, T7>> archetypeIterator(Archetype archetype) {
			Iterator<Entity> iterator = archetype.getAllocator().iterator();
			return new View.IteratorWith7<T1, T2, T3, T4, T5, T6, T7>(archetype.indexOf(type1), archetype.indexOf(type2), archetype.indexOf(type3), archetype.indexOf(type4),
																	  archetype.indexOf(type5), archetype.indexOf(type6), archetype.indexOf(type7), iterator, archetype);
		}
	}

	public static final class Of8<T1, T2, T3, T4, T5, T6, T7, T8> extends View<With8<T1, T2, T3, T4, T5, T6, T7, T8>> {
		
		private final Class<T1> type1;
		private final Class<T2> type2;
		private final Class<T3> type3;
		private final Class<T4> type4;
		private final Class<T5> type5;
		private final Class<T6> type6;
		private final Class<T7> type7;
		private final Class<T8> type8;

		Of8(ArchetypeList archetypeList, Map<ClassIndex, ArchetypeList.Node> nodeMap, Class<T1> type1, Class<T2> type2, Class<T3> type3, Class<T4> type4, Class<T5> type5, Class<T6> type6, Class<T7> type7, Class<T8> type8) {
			super(archetypeList, nodeMap);
			this.type1 = type1;
			this.type2 = type2;
			this.type3 = type3;
			this.type4 = type4;
			this.type5 = type5;
			this.type6 = type6;
			this.type7 = type7;
			this.type8 = type8;
		}

		@Override
		protected Iterator<With8<T1, T2, T3, T4, T5, T6, T7, T8>> archetypeIterator(Archetype archetype) {
			Iterator<Entity> iterator = archetype.getAllocator().iterator();
			return new View.IteratorWith8<T1, T2, T3, T4, T5, T6, T7, T8>(archetype.indexOf(type1), archetype.indexOf(type2), archetype.indexOf(type3), archetype.indexOf(type4),
																		  archetype.indexOf(type5), archetype.indexOf(type6), archetype.indexOf(type7), archetype.indexOf(type8), iterator, archetype);
		}
	}
	
	private static final class EntityTupleIterator<T> implements Iterator<T> {
		
		private final View<T> m_view;
		private final Iterator<ArchetypeList.Node> m_nodeIterator;
		private Iterator<T> m_tupleIterator;

		private EntityTupleIterator(View<T> owner, Iterator<ArchetypeList.Node> nodesIterator) {
			this.m_view = owner;
			this.m_nodeIterator = nodesIterator;
			this.m_tupleIterator = this.m_nodeIterator.hasNext() ?
							  	owner.archetypeIterator(this.m_nodeIterator.next().getArchetype()) :
								  new IteratorWith0<T>();
		}

		@Override
		public boolean hasNext() {
			return m_tupleIterator.hasNext() || (m_nodeIterator.hasNext() && (m_tupleIterator = m_view.archetypeIterator(m_nodeIterator.next().getArchetype())).hasNext());
		}

		@Override
		public T next() {
			return m_tupleIterator.next();
		}
	}

	static final class IteratorWith0<T> implements Iterator<T> {

		IteratorWith0() {
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public T next() {
			return null;
		}
	}

	static final class IteratorWith1<T> implements Iterator<View.With1<T>> {

		private final int m_index;
		private final Iterator<Entity> m_iterator;
		private final Archetype m_archetype;

		IteratorWith1(int index, Iterator<Entity> iterator, Archetype archetype) {
			this.m_index = index;
			this.m_iterator = iterator;
			this.m_archetype = archetype;
		}

		@Override
		public boolean hasNext() {
			return m_iterator.hasNext();
		}

		@Override
		@SuppressWarnings("unchecked")
		public View.With1<T> next() {
			Entity entity;
			while ((entity = m_iterator.next()).getArchetype() != m_archetype);
			Object[] components = entity.getComponents();
			return new View.With1<T>((T) components[m_index], entity);
		}
	}

	static final class IteratorWith2<T1, T2> implements Iterator<View.With2<T1, T2>> {

		private final int m_index1;
		private final int m_index2;
		private final Iterator<Entity> m_iterator;
		private final Archetype m_archetype;

		IteratorWith2(int index1, int index2, Iterator<Entity> iterator, Archetype archetype) {
			this.m_index1 = index1;
			this.m_index2 = index2;
			this.m_iterator = iterator;
			this.m_archetype = archetype;
		}

		@Override
		public boolean hasNext() {
			return m_iterator.hasNext();
		}

		@Override
		@SuppressWarnings("unchecked")
		public View.With2<T1, T2> next() {
			Entity entity;
			while ((entity = m_iterator.next()).getArchetype() != m_archetype);
			Object[] components = entity.getComponents();
			return new View.With2<T1, T2>((T1) components[m_index1],
										  (T2) components[m_index2], entity);
		}
	}

	static final class IteratorWith3<T1, T2, T3> implements Iterator<View.With3<T1, T2, T3>> {

		private final int m_index1;
		private final int m_index2;
		private final int m_index3;
		private final Iterator<Entity> m_iterator;
		private final Archetype m_archetype;

		IteratorWith3(int index1, int index2, int index3, Iterator<Entity> iterator, Archetype archetype) {
			this.m_index1 = index1;
			this.m_index2 = index2;
			this.m_index3 = index3;
			this.m_iterator = iterator;
			this.m_archetype = archetype;
		}

		@Override
		public boolean hasNext() {
			return m_iterator.hasNext();
		}

		@Override
		@SuppressWarnings("unchecked")
		public View.With3<T1, T2, T3> next() {
			Entity entity;
			while ((entity = m_iterator.next()).getArchetype() != m_archetype);
			Object[] components = entity.getComponents();
			return new View.With3<T1, T2, T3>((T1) components[m_index1],
											  (T2) components[m_index2],
											  (T3) components[m_index3], entity);
		}
	}

	static final class IteratorWith4<T1, T2, T3, T4> implements Iterator<View.With4<T1, T2, T3, T4>> {

		private final int m_index1;
		private final int m_index2;
		private final int m_index3;
		private final int m_index4;
		private final Iterator<Entity> m_iterator;
		private final Archetype m_archetype;

		IteratorWith4(int index1, int index2, int index3, int index4, Iterator<Entity> iterator, Archetype archetype) {
			this.m_index1 = index1;
			this.m_index2 = index2;
			this.m_index3 = index3;
			this.m_index4 = index4;
			this.m_iterator = iterator;
			this.m_archetype = archetype;
		}

		@Override
		public boolean hasNext() {
			return m_iterator.hasNext();
		}

		@Override
		@SuppressWarnings("unchecked")
		public View.With4<T1, T2, T3, T4> next() {
			Entity entity;
			while ((entity = m_iterator.next()).getArchetype() != m_archetype);
			Object[] components = entity.getComponents();
			return new View.With4<T1, T2, T3, T4>((T1) components[m_index1],
												  (T2) components[m_index2],
												  (T3) components[m_index3],
												  (T4) components[m_index4], entity);
		}
	}

	static final class IteratorWith5<T1, T2, T3, T4, T5> implements Iterator<View.With5<T1, T2, T3, T4, T5>> {

		private final int m_index1;
		private final int m_index2;
		private final int m_index3;
		private final int m_index4;
		private final int m_index5;
		private final Iterator<Entity> m_iterator;
		private final Archetype m_archetype;

		IteratorWith5(int index1, int index2, int index3, int index4, int index5, Iterator<Entity> iterator, Archetype archetype) {
			this.m_index1 = index1;
			this.m_index2 = index2;
			this.m_index3 = index3;
			this.m_index4 = index4;
			this.m_index5 = index5;
			this.m_iterator = iterator;
			this.m_archetype = archetype;
		}

		@Override
		public boolean hasNext() {
			return m_iterator.hasNext();
		}

		@Override
		@SuppressWarnings("unchecked")
		public View.With5<T1, T2, T3, T4, T5> next() {
			Entity entity;
			while ((entity = m_iterator.next()).getArchetype() != m_archetype);
			Object[] components = entity.getComponents();
			return new View.With5<T1, T2, T3, T4, T5>((T1) components[m_index1],
													  (T2) components[m_index2],
													  (T3) components[m_index3],
													  (T4) components[m_index4],
													  (T5) components[m_index5], entity);
		}
	}

	static final class IteratorWith6<T1, T2, T3, T4, T5, T6> implements Iterator<View.With6<T1, T2, T3, T4, T5, T6>> {

		private final int m_index1;
		private final int m_index2;
		private final int m_index3;
		private final int m_index4;
		private final int m_index5;
		private final int m_index6;
		private final Iterator<Entity> m_iterator;
		private final Archetype m_archetype;

		IteratorWith6(int index1, int index2, int index3, int index4, int index5, int index6, Iterator<Entity> iterator, Archetype archetype) {
			this.m_index1 = index1;
			this.m_index2 = index2;
			this.m_index3 = index3;
			this.m_index4 = index4;
			this.m_index5 = index5;
			this.m_index6 = index6;
			this.m_iterator = iterator;
			this.m_archetype = archetype;
		}

		@Override
		public boolean hasNext() {
			return m_iterator.hasNext();
		}

		@Override
		@SuppressWarnings("unchecked")
		public View.With6<T1, T2, T3, T4, T5, T6> next() {
			Entity entity;
			while ((entity = m_iterator.next()).getArchetype() != m_archetype);
			Object[] components = entity.getComponents();
			return new View.With6<T1, T2, T3, T4, T5, T6>((T1) components[m_index1],
														  (T2) components[m_index2],
														  (T3) components[m_index3],
														  (T4) components[m_index4],
														  (T5) components[m_index5],
														  (T6) components[m_index6], entity);
		}
	}

	static final class IteratorWith7<T1, T2, T3, T4, T5, T6, T7> implements Iterator<View.With7<T1, T2, T3, T4, T5, T6, T7>> {

		private final int m_index1;
		private final int m_index2;
		private final int m_index3;
		private final int m_index4;
		private final int m_index5;
		private final int m_index6;
		private final int m_index7;
		private final Iterator<Entity> m_iterator;
		private final Archetype m_archetype;

		IteratorWith7(int index1, int index2, int index3, int index4, int index5, int index6, int index7, Iterator<Entity> iterator, Archetype archetype) {
			this.m_index1 = index1;
			this.m_index2 = index2;
			this.m_index3 = index3;
			this.m_index4 = index4;
			this.m_index5 = index5;
			this.m_index6 = index6;
			this.m_index7 = index7;
			this.m_iterator = iterator;
			this.m_archetype = archetype;
		}

		@Override
		public boolean hasNext() {
			return m_iterator.hasNext();
		}

		@Override
		@SuppressWarnings("unchecked")
		public View.With7<T1, T2, T3, T4, T5, T6, T7> next() {
			Entity entity;
			while ((entity = m_iterator.next()).getArchetype() != m_archetype);
			Object[] components = entity.getComponents();
			return new View.With7<T1, T2, T3, T4, T5, T6, T7>((T1) components[m_index1],
															  (T2) components[m_index2],
															  (T3) components[m_index3],
															  (T4) components[m_index4],
															  (T5) components[m_index5],
															  (T6) components[m_index6],
															  (T7) components[m_index7], entity);
		}
	}

	static final class IteratorWith8<T1, T2, T3, T4, T5, T6, T7, T8> implements Iterator<View.With8<T1, T2, T3, T4, T5, T6, T7, T8>> {

		private final int m_index1;
		private final int m_index2;
		private final int m_index3;
		private final int m_index4;
		private final int m_index5;
		private final int m_index6;
		private final int m_index7;
		private final int m_index8;
		private final Iterator<Entity> m_iterator;
		private final Archetype m_archetype;

		IteratorWith8(int index1, int index2, int index3, int index4, int index5, int index6, int index7, int index8, Iterator<Entity> iterator, Archetype archetype) {
			this.m_index1 = index1;
			this.m_index2 = index2;
			this.m_index3 = index3;
			this.m_index4 = index4;
			this.m_index5 = index5;
			this.m_index6 = index6;
			this.m_index7 = index7;
			this.m_index8 = index8;
			this.m_iterator = iterator;
			this.m_archetype = archetype;
		}

		@Override
		public boolean hasNext() {
			return m_iterator.hasNext();
		}

		@Override
		@SuppressWarnings("unchecked")
		public View.With8<T1, T2, T3, T4, T5, T6, T7, T8> next() {
			Entity entity;
			while ((entity = m_iterator.next()).getArchetype() != m_archetype);
			Object[] components = entity.getComponents();
			return new View.With8<T1, T2, T3, T4, T5, T6, T7, T8>((T1) components[m_index1],
																  (T2) components[m_index2],
																  (T3) components[m_index3],
																  (T4) components[m_index4],
																  (T5) components[m_index5],
																  (T6) components[m_index6],
																  (T7) components[m_index7],
																  (T8) components[m_index8], entity);
		}
	}
}
