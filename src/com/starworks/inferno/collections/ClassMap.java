package com.starworks.inferno.collections;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.starworks.inferno.util.UnsafeSupport;

import sun.misc.Unsafe;

/**
 * A hash map that stores class indices and assigns them to objects using a
 * {@link ClassValue} field.
 *
 * <p>
 * The `ClassMap` class is a hash map that stores class indices and assigns them
 * to objects using a {@link ClassValue} field. It is used to create a mapping
 * between objects and their corresponding class indices, which can be used to
 * identify objects and group them by type.
 *
 * <p>
 * The `ClassMap` class implements its own hash map using native memory access
 * and the `Unsafe` class from the `sun.misc` package. It uses the identity hash
 * code of an object as the key for the hash map and reduces the significant
 * bits of the hash code using the `m_hashBits` field to reduce the probability
 * of collisions. The class indices are assigned to objects using a
 * {@link ClassValue} field and stored at the appropriate index in the hash map
 * using the `Unsafe.putIntVolatile` method.
 *
 * <p>
 * The `ClassMap` class provides several methods for adding and retrieving
 * objects from the hash map. It also implements the {@link Closeable} interface
 * to allow the native memory used by the hash map to be released when the
 * `ClassMap` instance is no longer needed.
 *
 * <p>
 * It is important to note that the `ClassMap` class is not thread-safe and does
 * not provide sufficient synchronization for concurrent access and modification
 * by multiple threads. If the ClassMap instance is accessed and modified
 * concurrently by multiple threads, it is recommended to use the getVolatile
 * and getOrIndex methods to ensure thread safety.
 * 
 * <p>
 * This implementation has a fixed m_capacity that is determined by the
 * m_hashBits field and the INT_BYTES_SHIFT constant. The m_capacity is equal to
 * <code> 2 ^ m_hashBits * INT_BYTES_SHIFT</code>. The m_hashBits field is
 * initialized with a default value of 20 bits if no hash bits are specified in
 * the constructor, which results in a m_capacity of approximately 1MB. The
 * INT_BYTES_SHIFT constant is set to 2, which means that the m_capacity is
 * multiplied by 4.
 * <p>
 * The ClassMap class also has a m_atCapacity field that is used to indicate
 * when the class has run out of memory for caching class indices. If the
 * m_atCapacity field is set to true, new objects are not added to the hash map
 * and their previously assigned values are returned instead.
 * <p>
 * Example usage:
 * 
 * <pre>
 * ClassMap classMap = new ClassMap();
 * int index1 = classMap.add(String.class);
 * int index2 = classMap.add(Integer.class);
 * int index3 = classMap.getOrAdd(new Object());
 * System.out.println(index1); // prints 1
 * System.out.println(index2); // prints 2
 * System.out.println(index3); // prints 3
 * classMap.close();
 * </pre>
 * 
 * @author Enrico Stara
 * @author Ethan Temprovich
 * @see Unsafe
 * @see ClassValue
 */
public final class ClassMap implements Closeable {

	/**
	 * 
	 * The number of bits to shift an integer value left by when calculating the
	 * memory address of an object in the hash map.
	 */
	public final static int INT_BYTES_SHIFT = Integer.BYTES >> 1;

	/**
	 * 
	 * The default number of bits to use for the hash map m_capacity and for
	 * reducing the significant bits of an object's identity hash code.
	 */
	public static final int DEFAULT_HASH_BITS = 20;

	/**
	 * 
	 * The minimum number of bits to use for the hash map m_index.
	 */
	public static final int MIN_HASH_BITS = 14;

	/**
	 * 
	 * The maximum number of bits to use for the hash map m_index.
	 */
	public static final int MAX_HASH_BITS = 24;

	private static final Unsafe unsafe = UnsafeSupport.getUnsafe();

	/*
	 * The value of DEFAULT_HASH_BITS is set to 20, which means that the hash map
	 * has a m_capacity of 2 to the power of 20, or approximately 1 million entries,
	 * and that the significant bits of an object's identity hash code are reduced
	 * to 20 bits when adding the object to the hash map or retrieving its value
	 * from the hash map. This results in a hash map with a m_capacity of
	 * approximately 1MB and a reduced collision probability.
	 */
	private final int m_hashBits;
	private int m_index;
	private final AtomicBoolean m_atCapacity;
	private final int m_capacity;
	private final long m_memoryAddress;
	private final Map<Object, Integer> m_objectCache;
	private final ClassValue<Integer> m_classValue;

	/**
	 * Constructs a new `ClassMap` instance with a default m_capacity of
	 * approximately 1MB.
	 *
	 * @see #ClassMap(int)
	 */
	public ClassMap() {
		this(DEFAULT_HASH_BITS);
	}

	/**
	 * Constructs a new `ClassMap` instance with a specified m_capacity.
	 *
	 * <p>
	 * The m_capacity is determined by the `hashBits` argument and the
	 * `INT_BYTES_SHIFT` constant. The m_capacity is equal to 2 to the power of
	 * `hashBits` multiplied by the `INT_BYTES_SHIFT` constant, which is set to 2.
	 * The `hashBits` argument is constrained to the range [14, 24] by the
	 * `MIN_HASH_BITS` and `MAX_HASH_BITS` constants.
	 * 
	 * @param hashBits the number of bits to consider when hashing keys for indices
	 */
	public ClassMap(int hashBits) {
		if (hashBits < MIN_HASH_BITS || hashBits > MAX_HASH_BITS)
			throw new IllegalArgumentException(
					"Hash cannot be less than " + MIN_HASH_BITS + " or greater than " + MAX_HASH_BITS + " bits");
		this.m_hashBits = Math.min(Math.max(hashBits, MIN_HASH_BITS), MAX_HASH_BITS);
		this.m_index = 1;
		this.m_atCapacity = new AtomicBoolean(false);
		this.m_capacity = (1 << hashBits) << INT_BYTES_SHIFT;
		this.m_memoryAddress = unsafe.allocateMemory(m_capacity);
		unsafe.setMemory(m_memoryAddress, m_capacity, (byte) 0);
		this.m_objectCache = new ConcurrentHashMap<>(1 << 10);
		this.m_classValue = new ClassValue<Integer>() {
			@Override
			protected Integer computeValue(Class<?> type) {
				return m_index++;
			}
		};
	}

	/**
	 * Calculates an index for the type provided and stores the value for fast
	 * lookup.
	 *
	 * @param type the {@link Class} to be added to the hash map
	 * @return the index assigned to the object and stored in the hash map, or the
	 *         previously assigned index if the class has run out of memory for
	 *         caching class indices
	 * @see {@link #index(Object)} method for implementation details.
	 */
	public int add(Class<?> newClass) {
		return addObject(newClass);
	}

	/**
	 * Calculates a progressive index* for the object provided and indexes the
	 * index*—based on its {@link #reduce(int, int) reduced} identity m_hashCode—and
	 * stores the value for fast lookup.
	 *
	 * <p>
	 * This method adds the specified object to the hash map by first computing the
	 * index by using the object's hash code and {@link #reduce(int) reducing} the
	 * significant bits. It then uses the {@code Unsafe.putIntVolatile} method to
	 * store the object's assigned value at the calculated index in memory.
	 *
	 * <p>
	 * If the object has already been added to the hash map, its previously assigned
	 * value is returned. If the object has not yet been added to the hash map, a
	 * new value is assigned to the object using the {@link ClassValue} field and
	 * stored at the appropriate index; the assigned value is returned.
	 *
	 * <p>
	 * If the class has run out of memory for caching class indices, as indicated by
	 * the {@code m_atCapacity} field, the object is not added to the hash map and
	 * its previously assigned value is returned.
	 *
	 * <p>
	 * It is important to note that this method may not provide sufficient thread
	 * safety in cases where the hash map is concurrently accessed and modified by
	 * multiple threads. In such cases, the {@link #getOrIndexObject(Object)} method
	 * should be used instead to ensure that the object is added to the hash map in
	 * a thread-safe manner.
	 *
	 * @param object the object to be added to the hash map
	 * @return the index assigned to the object and stored in the hash map, or the
	 *         previously assigned index if the class has run out of memory for
	 *         caching class indices
	 */
	public int addObject(Object newClass) {
		if (m_atCapacity.get()) {
			return m_classValue.get((Class<?>) newClass);
		}
		int identityHashCode = reduce(System.identityHashCode(newClass));
		long i = getAddress(identityHashCode);
		int currentIndex = unsafe.getInt(i);
		if (currentIndex == 0) {
			int idx = m_classValue.get((Class<?>) newClass);
			unsafe.putIntVolatile(null, i, idx);
			m_objectCache.put(newClass, idx);
			return idx;
		} else {
			if (!m_objectCache.containsKey(newClass)) {
				int idx = m_classValue.get((Class<?>) newClass);
				m_atCapacity.set(true);
				return idx;
			}
		}
		return currentIndex;
	}

	/**
	 * Returns the index stored for the specified object in the hash map.
	 *
	 * @param object the object whose index is to be retrieved from the hash map
	 * @return the index stored for the specified object in the hash map, or 0 if no
	 *         index is stored
	 * @see {@link #indexObject(Object)} method for implementation details.
	 */
	public int indexOf(Class<?> type) {
		return indexOfObject(type);
	}

	/**
	 * Returns the index stored for the specified object in the hash map.
	 *
	 * <p>
	 * This method retrieves the index stored in the hash map for the specified
	 * object. The index is calculated by {@link #reduce(int, int) reducing} the
	 * object's hash code and then invoking {@code Unsafe.getIntVolatile} method to
	 * retrieve the index stored at the calculated hash.
	 *
	 * <p>
	 * It is important to note that this method may not provide sufficient thread
	 * safety in cases where the hash map is concurrently accessed and modified by
	 * multiple threads. In such cases, the {@link #getVolatile(Object)} method
	 * should be used instead to ensure that the returned value reflects the latest
	 * value stored in the hash map.
	 *
	 * @param object the object whose index is to be retrieved from the hash map
	 * @return the index stored for the specified object in the hash map, or 0 if no
	 *         index is stored
	 */
	public int indexOfObject(Object object) {
		if (m_atCapacity.get()) {
			return m_classValue.get((Class<?>) object);
		}
		int identityHashCode = reduce(System.identityHashCode(object));
		return unsafe.getInt(getAddress(identityHashCode));
	}

	/**
	 * Returns the index stored for the specified object in the hash map in a
	 * thread-safe manner.
	 *
	 * <p>
	 * This method retrieves the index stored in the hash map for the specified
	 * object. The index is calculated by {@link #reduce(int, int) reducing} the
	 * object's hash code and then invoking {@code Unsafe.getIntVolatile} method to
	 * retrieve the index stored at the calculated hash. This method should be used
	 * in cases where the {@link #find(Object)} method may not provide sufficient
	 * thread safety
	 *
	 * <p>
	 * The {@code Unsafe.getIntVolatile} method uses volatile memory access
	 * semantics, which ensures that the value returned by the method reflects the
	 * latest value stored in memory at the specified index. This is important in a
	 * multi-threaded environment, as it ensures that the value returned by this
	 * method is not cached in a processor register or local memory, but is always
	 * read directly from main memory.
	 *
	 * @param object the object whose value is to be retrieved from the hash map
	 * @return the index stored for the specified object in the hash map, or 0 if no
	 *         index is stored
	 */
	public int indexOfVolatile(Object object) {
		if (m_atCapacity.get()) {
			return m_classValue.get((Class<?>) object);
		}
		int identityHashCode = reduce(System.identityHashCode(object));
		return unsafe.getIntVolatile(null, getAddress(identityHashCode));
	}

	/**
	 * Retrieves the index in the hash map pertaining to the given type, or adds the
	 * type to the hash map if not already indexed.
	 *
	 * @param type the type whose index is to be retrieved or added to the hash map
	 * @return the index stored for the specified type in the hash map, or 0 if no
	 *         index is stored
	 * @see {@link #find(Class)} method for implementation details.
	 * @see {@link #index(Class)} method for implementation details.
	 */
	public int indexOfOrAdd(Class<?> type) {
		return indexOfOrAddObject(type);
	}

	/**
	 * Retrieves the index in the hash map pertaining to the given object, or adds
	 * the object to the hash map if not already indexed.
	 *
	 * @param object the object whose index is to be retrieved or added to the hash
	 *               map
	 * @return the index stored for the specified object in the hash map, or 0 if no
	 *         index is stored
	 * @see {@link #find(Object)} method for implementation details.
	 * @see {@link #indexObject(Object)} method for implementation details.
	 */
	public int indexOfOrAddObject(Object object) {
		int value = indexOfVolatile(object);
		if (value != 0) {
			return value;
		}
		return addObject(object);
	}

	public ClassIndex getClassIndex(int hash) {
		return new ClassIndex(hash);
	}

	public ClassIndex getClassIndex(Object[] objects) {
		int size = objects.length;
		boolean[] checkArray = new boolean[m_index + size + 1];
		int min = Integer.MAX_VALUE, max = 0;
		for (int i = 0; i < size; i++) {
			int value = indexOf(objects[i].getClass());
			value = value == 0 ? indexOfOrAdd(objects[i].getClass()) : value;
			if (checkArray[value]) {
				throw new IllegalArgumentException("Duplicate types are not allowed");
			}
			checkArray[value] = true;
			min = Math.min(value, min);
			max = Math.max(value, max);
		}
		return new ClassIndex(checkArray, min, max, size);
	}

	/**
	 * Reduces the given hash code to a valid index. This has the effect of
	 * discarding the most significant bits of the hash code, leaving only the least
	 * significant bits that fall within the valid range of indices for the hash
	 * map.
	 * 
	 * <pre>
	 * e.g.
	 * 	HASH_BITS 		= 20 (default)
	 * ---------------------------------------------
	 * 	reduce(0x12345678)     -> 0x12345
	 * 	reduce(0xffffffff)     -> 0xfffff
	 * 	reduce(0xffffffffffff) -> 0xfffff
	 * </pre>
	 *
	 * @param m_hashCode the hash code to be reduced
	 * @return the reduced hash code, suitable for use as an index in this hash map
	 */
	private int reduce(int hashCode) {
		return hashCode >> (Integer.SIZE - m_hashBits);
	}

	/*
	 * Calculates the memory address of the specified object in the hash map.
	 * 
	 * @param identityHashCode the reduced identity hash code of the object whose
	 * memory address is to be calculated
	 * 
	 * @return the memory address of the object in the hash map
	 */
	private long getAddress(long identityHashCode) {
		return m_memoryAddress + (identityHashCode << INT_BYTES_SHIFT);
	}

	/*
	 * This method returns the number of objects that have been added to the hash
	 * map and assigned a class index. It does not return the m_capacity of the hash
	 * map.
	 * 
	 * @return the number of objects in the hash map
	 */
	public int size() {
		return m_index - 1;
	}

	/**
	 * Indicates if the hash table is full, at which point objects will no longer be
	 * cached. Their values will be returned but no longer stored.
	 * 
	 * @return `true` if the class has run out of memory for caching class indices,
	 *         `false` otherwise
	 */
	public boolean isFull() {
		return m_atCapacity.get();
	}

	/**
	 * Closes the ClassMap and releases the memory allocated for it.
	 *
	 * <p>
	 * This method closes the ClassMap and releases the memory allocated for it by
	 * calling the {@code Unsafe.freeMemory} method with the {@code m_memoryAddress}
	 * field as its argument.
	 *
	 * <p>
	 * It is important to note that this method should be called when the ClassMap
	 * is no longer needed to ensure that the memory allocated for it is released
	 * and can be garbage collected.
	 *
	 * @throws Exception if an error occurs while releasing the memory allocated for
	 *                   the ClassMap
	 */
	@Override
	public void close() {
		m_objectCache.clear();
		unsafe.freeMemory(m_memoryAddress);
	}

	public int getHashBit() {
		return m_hashBits;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ClassMap[");
		boolean first = true;
		for (var object : m_objectCache.values()) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(object);
			sb.append('=');
			sb.append(indexOfObject(object));
		}
		sb.append(']');
		return sb.toString();
	}

	public static final class ClassIndex {

		private final int m_hashCode;
		private final byte[] m_data;

		private ClassIndex(int value) {
			this.m_hashCode = value;
			m_data = null;
		}

		private ClassIndex(boolean[] checkArray, int min, int max, int length) {
			long result = 1;
			int index = 0;
			m_data = new byte[length];
			for (int i = min; i <= max; i++) {
				if (checkArray[i]) {
					result = result * 31 + i;
					m_data[index++] = (byte) (i ^ (i >>> 8));
				}
			}
			m_hashCode = (int) (result ^ (result >>> 32));
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			return Arrays.equals(((ClassIndex) o).m_data, m_data);
		}

		@Override
		public int hashCode() {
			return m_hashCode;
		}

		@Override
		public String toString() {
			return "ClassIndex[" + m_hashCode + ":" + Arrays.toString(m_data) + "]";
		}
	}
}
