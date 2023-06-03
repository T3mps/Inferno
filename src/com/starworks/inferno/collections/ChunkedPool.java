package com.starworks.inferno.collections;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;


/**
 * <p>
 * The ChunkedPool class is designed to store a large number of objects in a
 * way that optimizes memory. It does this by dividing the objects into smaller
 * chunks and only allocating memory for the chunks that are currently in use.
 * This can help to avoid the overhead of constantly allocating and deallocating
 * large blocks of memory, as well as reduce the risk of running out of memory
 * due to a sudden increase in the number of objects being stored.
 *
 * <p>
 * Implementation includes using a minimal-locking, chunked data structure that
 * allows multiple threads to safely and concurrently access and modify the pool.
 * It also provides a number of utility methods for managing the objects in the
 * pool, such as adding, removing, and iterating over the
 * objects.
 *
 * @param <T> the type of objects in the pool.
 * @author Enrico Stara
 * @author Ethan Temprovich
 * @see ChunkedPool.Allocator
 */
public final class ChunkedPool<T extends ChunkedPool.Poolable> implements Closeable {

	/**
	 * 
	 * The maximum capacity of the ID stack.
	 */
	public static final int ID_STACK_CAPACITY = 1 << 16;
	/**
	 * 
	 * The default number of bits to use for the chunk ID.
	 */
	public static final int DEFAULT_CHUNK_BIT = 14;
	/**
	 * 
	 * The default number of bits to use for representing the chunk count.
	 */
	public static final int DEFAULT_CHUNK_COUNT_BIT = 16;

	private final IDFactory m_idFactory;
	private final Chunk<T>[] m_chunks;
	private int m_currentChunkIndex;
	private final StampedLock m_lock;
	private final List<Allocator<T>> m_allocators;

	/**
	 * Constructs a new ChunkedPool with default chunk size and chunk count.
	 */
	public ChunkedPool() {
		this(DEFAULT_CHUNK_BIT, DEFAULT_CHUNK_COUNT_BIT);
	}

	/**
	 * Constructs a new ChunkedPool with the specified chunk size and chunk count.
	 *
	 * <p>
	 * This constructor constructs a new ChunkedPool with the specified chunk size
	 * and chunk count. The chunk size is determined by the specified chunk bit,
	 * which specifies the size of each chunk in terms of the number of bits
	 * required to represent the maximum ID that can be allocated in the chunk. The
	 * chunk count is determined by the specified chunk count bit, which specifies
	 * the number of chunks in the pool in terms of the number of bits required to
	 * represent the maximum number of chunks in the pool.
	 *
	 * @param chunkBit      the chunk bit.
	 * @param chunkCountBit the chunk count bit.
	 */
	@SuppressWarnings("unchecked")
	public ChunkedPool(int chunkBit, int chunkCountBit) {
		this.m_idFactory = new IDFactory(chunkBit, chunkCountBit);
		this.m_chunks = new Chunk[m_idFactory.getChunkCount()];
		this.m_currentChunkIndex = -1;
		this.m_lock = new StampedLock();
		this.m_allocators = new ArrayList<Allocator<T>>();
	}

	/**
	 * Returns the object in the pool with the specified ID.
	 *
	 * @param id the ID of the object to return.
	 * @return the object in the pool with the specified ID.
	 */
	public T get(int id) {
		return getChunk(id).get(id);
	}

	/**
	 * Creates and returns a new allocator for this pool.
	 *
	 * @return a new allocator for this pool.
	 */
	public Allocator<T> newAllocator() {
		Allocator<T> allocator = new Allocator<T>(this, m_chunks);
		m_allocators.add(allocator);
		return allocator;
	}

	/**
	 * Creates and returns a new chunk with the specified owner and last chunk.
	 *
	 * @param owner     the owner of the new chunk.
	 * @param lastChunk the last chunk.
	 * @return a new chunk with the specified owner and last chunk.
	 */
	private Chunk<T> createChunk(Allocator<T> owner, Chunk<T> lastChunk) {
		long stamp = m_lock.writeLock();
		try {
			int id = ++m_currentChunkIndex;
			if (id > m_idFactory.getChunkCount() - 1) {
				throw new OutOfMemoryError(ChunkedPool.class.getName() + ": cannot create a new memory chunk");
			}
			var newChunk = new Chunk<T>(id, m_idFactory);
			if (lastChunk != null) {
				lastChunk.setNext(newChunk);
			}
			m_chunks[id] = newChunk;
			return newChunk;
		} finally {
			m_lock.unlockWrite(stamp);
		}
	}

	/**
	 * Returns the specified chunk in the pool.
	 *
	 * @param id the ID of the chunk to return.
	 * @return the chunk in the pool with the specified ID.
	 */
	private Chunk<T> getChunk(int id) {
		long stamp = m_lock.tryOptimisticRead();
		var chunk = m_chunks[m_idFactory.getChunkID(id)];
		if (!m_lock.validate(stamp)) {
			stamp = m_lock.readLock();
			try {
				chunk = m_chunks[m_idFactory.getChunkID(id)];
			} finally {
				m_lock.unlockRead(stamp);
			}
		}
		return chunk;
	}

	/**
	 * Returns the number of objects in the pool.
	 *
	 * @return the number of objects in the pool.
	 */
	public int size() {
		int size = 0;
		for (int i = 0; i < m_chunks.length; i++) {
			var chunk = m_chunks[i];
			size += chunk != null ? chunk.size() : 0;
		}
		return size;
	}

	/**
	 * Closes the pool and releases all resources.
	 *
	 * <p>
	 * This method closes all allocators associated with the pool, and releases any
	 * other resources used by the pool. After calling this method, the pool can no
	 * longer be used.
	 */
	@Override
	public void close() {
		m_allocators.forEach(Allocator::close);
	}

	/**
	 * Returns the ID factory used by the pool.
	 *
	 * @return the ID factory used by the pool.
	 */
	public IDFactory getIDFactory() {
		return m_idFactory;
	}

	/*
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ChunkedPool[");
		sb.append("chunkCount=").append(m_idFactory.getChunkCount()).append(", ");
		sb.append("chunkCapacity=").append(m_idFactory.getChunkCapacity()).append(", ");
		sb.append("chunks={");
		int size = m_chunks.length;
		for (int i = 0; i < size; i++) {
			var chunk = m_chunks[i];
			if (chunk != null) {
				sb.append(chunk).append(", ");
			}
		}
		if (size > 0) {
			sb.setLength(sb.length() - 2);
		}
		sb.append("}]");
		return sb.toString();
	}

	/**
	 * An Allocator provides a thread-safe way to add and remove objects from a
	 * ChunkedPool. It acts as a wrapper around the pool and a stack of 32-bit
	 * integer IDs. It provides methods for registering objects to the pool, via an
	 * ID, and generating the next available ID. These IDs are produced based on the
	 * parameters set within the IDFactory object in the owning pool.
	 * 
	 * <p>
	 * A {@link ChunkedPool chunked pool} instance can have multiple allocators
	 * associated with it. Each allocator is associated with a specific chain of
	 * chunks in the pool. When an allocator is closed, it will release all IDs
	 * associated with the objects it has allocated.
	 *
	 * @param <T> the type of objects in the pool.
	 * @author Ethan Temprovich
	 * @see ChunkedPool
	 * @see IntStack
	 * @see IDFactory
	 * @apiNote An Allocator is associated with a specific ChunkedPool
	 */
	public static final class Allocator<T extends Poolable> implements Closeable {

		private final ChunkedPool<T> m_owner;
		private final IntStack m_idStack;
		private final Chunk<T> m_firstChunk;
		private Chunk<T> m_currentChunk;
		private final StampedLock m_lock;
		private int m_nextID;

		/**
		 * Constructs a new allocator for the specified pool.
		 *
		 * @param pool   the pool to allocate objects from.
		 * @param chunks the chunks in the pool.
		 */
		private Allocator(ChunkedPool<T> pool, Chunk<T>[] chunks) {
			this.m_owner = pool;
			this.m_idStack = new IntStack(ID_STACK_CAPACITY);
			this.m_currentChunk = pool.createChunk(this, null);
			this.m_firstChunk = m_currentChunk;
			this.m_nextID = IDFactory.DETACHED_BIT;
			this.m_lock = new StampedLock();
			nextID();
		}

		/**
		 * Registers the specified object with the specified ID in the pool. If the ID
		 * is already in use, the previously registered object is overridden..
		 *
		 * @param id    the ID to register the object with.
		 * @param entry the object to register.
		 * @return the newly registered object
		 */
		public T register(int id, T entry) {
			return m_owner.getChunk(id).set(id, entry);
		}

		/**
		 * Returns the next available ID for an object in the pool.
		 *
		 * <p>
		 * It first checks if there are any recycled IDs available, and if not, it
		 * creates a new ID using the ID factory of the pool.
		 *
		 * @return the next available ID for an object in the pool.
		 */
		public int nextID() {
			long stamp = m_lock.tryOptimisticRead();
			int nextID = m_idStack.pop();
			if (!m_lock.validate(stamp)) {
				stamp = m_lock.readLock();
				try {
					nextID = m_idStack.pop();
				} finally {
					m_lock.unlockRead(stamp);
				}
			}
			if (nextID != IDFactory.DETACHED_BIT) {
				m_owner.getChunk(nextID).m_index.incrementAndGet();
				return nextID;
			}

			stamp = m_lock.writeLock();
			try {
				nextID = m_nextID;
				if (m_currentChunk.m_index.get() < m_owner.getIDFactory().getChunkCapacity() - 1) {
					m_nextID = m_owner.m_idFactory.createID(m_currentChunk.m_id, m_currentChunk.m_index.incrementAndGet());
					return nextID;
				}
				m_currentChunk = m_owner.createChunk(this, m_currentChunk);
				m_nextID = m_owner.m_idFactory.createID(m_currentChunk.m_id, m_currentChunk.m_index.incrementAndGet());
				return nextID;
			} finally {
				m_lock.unlockWrite(stamp);
			}
		}

		/**
		 * Frees the specified ID for reuse.
		 *
		 * @param id the ID to free.
		 * @return the freed ID.
		 * @throws IllegalArgumentException if the specified ID is invalid.
		 */
		public int freeID(int id) {
			Chunk<T> chunk = m_owner.getChunk(id);
			if (chunk == null) {
				throw new IllegalArgumentException("Invalid ID: " + id);
			}
			long stamp = m_lock.readLock();
			try {
				if (chunk.isEmpty()) {
					return id;
				}
				int freeID = chunk.remove(id);
				if (freeID == IDFactory.DETACHED_BIT) {
					return freeID;
				}
				if (chunk != m_currentChunk) {
					m_idStack.push(freeID);
					return freeID;
				}

				stamp = m_lock.tryConvertToWriteLock(stamp);
				return m_nextID = freeID;
			} finally {
				m_lock.unlock(stamp);
			}
		}

		/**
		 * Closes the allocator and releases any resources it is holding. It is
		 * important to close an allocator when it is no longer needed to ensure that
		 * all allocated objects are properly deallocated.
		 */
		@Override
		public void close() {
			m_idStack.close();
		}

		/**
		 * Returns an ChunkedPoolIterator instance with the firstChunk of this
		 * allocator as the construct parameter.
		 *
		 * @return an iterator over the objects in the pool
		 * @see ChunkedPoolIterator
		 */
		public Iterator<T> iterator() {
			return new ChunkedPoolIterator<T>(m_firstChunk);
		}

		/**
		 * Returns the pool that the allocator is associated with.
		 *
		 * @return the pool that the allocator is associated with.
		 */
		public ChunkedPool<T> getPool() {
			return m_owner;
		}

		/**
		 * Returns the stack of recycled IDs used by the allocator.
		 *
		 * @return the stack of recycled IDs used by the allocator.
		 */
		public IntStack getStack() {
			return m_idStack;
		}
	}

	/**
	 * 
	 * The Chunk class represents a partition of the ChunkedPool and is responsible
	 * for storing a group of Identifiable objects. The Chunk class is used in
	 * conjunction with the IDFactory class to efficiently store these objects.
	 * 
	 * <p>
	 * The Chunk class is not meant for direct use, and rather should be
	 * interfaced with via the {@link Allocator} class. The Chunk expects items
	 * stored to already have an ID.
	 * 
	 * @param <T> the type of objects in the chunk.
	 * @author Ethan Temprovich
	 * @see Allocator
	 */
	public static final class Chunk<T extends Poolable> {

		private final T[] m_data;
		private final int m_id;
		private Chunk<T> m_next;
		private final AtomicInteger m_index;
		private final IDFactory m_idFactory;
		private final StampedLock m_lock;

		/**
		 * 
		 * Constructs a new Chunk with the given id and IDFactory.
		 * 
		 * @param id        the id of the chunk.
		 * @param idFactory the IDFactory used to create IDs for objects in the chunk.
		 */
		@SuppressWarnings("unchecked")
		public Chunk(int id, IDFactory idFactory) {
			this.m_data = (T[]) new Poolable[idFactory.getChunkCapacity()];
			this.m_id = id;
			this.m_next = null;
			this.m_index = new AtomicInteger(-1);
			this.m_idFactory = idFactory;
			this.m_lock = new StampedLock();
		}

		/**
		 * 
		 * Returns the object stored at the specified id in this chunk.
		 * 
		 * @param id the id of the object to retrieve
		 * @return the object stored at the specified id in this chunk, or null if no
		 *         object is stored at the id
		 */
		public T get(int id) {
			long stamp = m_lock.readLock();
			try {
				return m_data[m_idFactory.getObjectID(id)];
			} finally {
				m_lock.unlockRead(stamp);
			}
		}

		/**
		 * Sets the element at the specified position in this chunk to the specified
		 * value.
		 *
		 * @param id    the position of the element in this chunk
		 * @param value the element to be stored
		 * @return the stored element at the specified position
		 */
		public T set(int id, T value) {
			long stamp = m_lock.writeLock();
			try {
				return (m_data[m_idFactory.getObjectID(id)] = value);
			} finally {
				m_lock.unlockWrite(stamp);
			}
		}

		/**
		 * 
		 * Removes the object with the given ID from the Chunk. This method first
		 * finds the index of the object in the Chunk using the associated
		 * {@link IDFactory}, and then sets the element in the data array at that index
		 * to null. If the object being removed is not the last element in the data
		 * array, the method then sets the ID of the last element to the ID of the
		 * removed element and moves the last element to the removed element's index.
		 * The method then decrements the size of the Chunk by 1. If the last index is
		 * out of bounds or the Chunk is empty, the method returns
		 * {@link IDFactory#DETACHED_BIT}. Otherwise, the method returns the ID of the
		 * removed object.
		 * 
		 * @param id the ID of the object to be removed
		 * @return the ID of the removed object, or {@link IDFactory#DETACHED_BIT} if
		 *         the Chunk is empty or the last index is out of bounds
		 */
		public int remove(int id) {
			long stamp = m_lock.writeLock();
			try {
				int removedIndex = m_idFactory.getObjectID(id);
				int lastIndex = m_index.decrementAndGet() + (m_next == null ? 0 : 1);
				if (lastIndex < 0 || lastIndex >= m_idFactory.getChunkCapacity()) {
					return IDFactory.DETACHED_BIT;
				}
				T last = m_data[lastIndex];
				T removed = m_data[removedIndex];
				if (last != null && last != removed) {
					last.setID(id);
					m_data[removedIndex] = m_data[lastIndex];
				} else {
					m_data[removedIndex] = null;
				}
				return m_idFactory.mergeChunkIDs(id, lastIndex);
			} finally {
				m_lock.unlockWrite(stamp);
			}
		}

		/**
		 * 
		 * Returns whether or not the chunk has any empty spaces left in which an object
		 * can be stored.
		 * 
		 * @return true if there are empty spaces left in the chunk, false otherwise
		 */
		public boolean hasCapacity() {
			return m_index.get() < m_idFactory.getChunkCapacity() - 1;
		}

		/**
		 * 
		 * Sets the next chunk in the linked list of chunks.
		 * 
		 * @param next the next chunk in the linked list
		 */
		private void setNext(Chunk<T> next) {
			this.m_next = next;
		}

		/**
		 * Returns the number of objects managed by this chunk.
		 * 
		 * @return The number of objects in this chunk.
		 */
		public int size() {
			return m_index.get() + (m_next == null ? 0 : 1);
		}

		/**
		 * Returns whether this chunk manages zero objects.
		 * 
		 * @return true if this chunk has no objects, false otherwise.
		 */
		public boolean isEmpty() {
			return size() == 0;
		}

		/*
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Chunk[");
			sb.append("id=").append(m_id).append(", ");
			sb.append("capacity=").append(m_data.length).append(", ");
			sb.append("size=").append(m_index.get()).append(", ");
			sb.append("entries={");
			int size = m_index.get();
			for (int i = 0; i < size; i++) {
				if (i > 0) {
					sb.append(", ");
				}
				sb.append(m_idFactory.idToString(m_data[i].getID()));
			}
			sb.append("}]");
			return sb.toString();
		}
	}

	/**
	 * An iterator for the elements in a chunked pool.
	 *
	 * @param <T> the type of elements in the chunked pool.
	 * @author Ethan Temprovich
	 */
	public static class ChunkedPoolIterator<T extends Poolable> implements Iterator<T> {

		private int m_next;
		private Chunk<T> m_currentChunk;

		/**
		 * Constructs a new chunked pool iterator.
		 *
		 * @param currentChunk the current chunk to iterate over.
		 */
		private ChunkedPoolIterator(Chunk<T> currentChunk) {
			this.m_next = 0;
			this.m_currentChunk = currentChunk;
		}

		/**
		 * Returns {@code true} if there are more elements to iterate over in the
		 * current chunk, or if there are more chunks to iterate.
		 *
		 * @return {@code true} if there are more elements/chunks to iterate over,
		 *         {@code false} otherwise.
		 */
		@Override
		public boolean hasNext() {
			return m_currentChunk.size() > m_next || ((m_next = 0) == 0 && (m_currentChunk = m_currentChunk.m_next) != null);
		}

		/**
		 * Returns the next element in the iteration.
		 *
		 * @return the next element in the iteration.
		 * @throws NoSuchElementException if there are no more elements to iterate over.
		 */
		@Override
		public T next() {
			return m_currentChunk.m_data[m_next++];
		}
	}

	/**
	 * An interface for objects which have a unique identifier, allowing for pooling.
	 *
	 * @author Ethan Temprovich
	 */
	public interface Poolable {

		/**
		 * Returns the unique identifier for this object.
		 *
		 * @return the unique identifier for this object.
		 */
		public int getID();

		/**
		 * Sets the unique identifier for this object.
		 *
		 * @param id the new unique identifier for this object.
		 * @return depends on implementation.
		 */
		public void setID(int id);
	}

	/**
	 * A class that generates unique IDs by combining a chunk ID and an object ID.
	 * The chunk ID represents the group that the object belongs to, and the object
	 * ID represents the specific object within that group. The generated ID is a
	 * 32-bit integer with the following format:
	 *
	 * DETACHED:FLAG:CHUNK_ID:OBJECT_ID
	 *
	 * @author Ethan Temprovich
	 */
	public static final class IDFactory {

		/** The total number of bits in the generated ID. */
		public static final int BIT_LENGTH = 30;

		/** The minimum number of bits to use for the chunk ID. */
		public static final int MIN_CHUNK_BIT = 10;

		/** The minimum number of bits to use for representing the chunk count. */
		public static final int MIN_CHUNK_BIT_COUNT = 6;

		/** The maximum number of bits to use for the chunk ID. */
		public static final int MAX_CHUNK_BIT = BIT_LENGTH - MIN_CHUNK_BIT_COUNT;

		/** The max length of the chunk bits. */
		public static final int MAX_CHUNK_BIT_COUNT = BIT_LENGTH - MIN_CHUNK_BIT;

		/** The index of the RELEASED bit in the generated ID. */
		public static final int RELEASED_BIT_INDEX = 30;

		/** The value of the RELEASED bit in the generated ID. */
		public static final int RELEASED_BIT = 1 << RELEASED_BIT_INDEX;
		
		/** The index of the DETACHED bit in the generated ID. */
		public static final int DETACHED_BIT_INDEX = 31;

		/** The value of the DETACHED bit in the generated ID. */
		public static final int DETACHED_BIT = 1 << DETACHED_BIT_INDEX;

		private final int m_chunkBit;
		private final int m_chunkCountBit;
		private final int m_chunkCount;
		private final int m_chunkIDBitMask;
		private final int m_chunkIDBitMaskShifted;
		private final int m_chunkCapacity;
		private final int m_objectIDBitMask;

		public IDFactory(int chunkBit, int chunkCountBit) {
			this.m_chunkBit = chunkBit;
			this.m_chunkCountBit = chunkCountBit;
			this.m_chunkCount = 1 << chunkCountBit;
			this.m_chunkIDBitMask = (1 << (BIT_LENGTH - chunkBit)) - 1;
			this.m_chunkIDBitMaskShifted = ((1 << (BIT_LENGTH - chunkBit)) - 1) << chunkBit;
			this.m_chunkCapacity = 1 << Math.min(Math.max(MIN_CHUNK_BIT, chunkBit), MAX_CHUNK_BIT);
			this.m_objectIDBitMask = (1 << chunkBit) - 1;
		}

		public int createID(int chunkID, int objectID) {
			return (chunkID & m_chunkIDBitMask) << m_chunkBit | (objectID & m_objectIDBitMask);
		}

		public int mergeChunkIDs(int id, int objectID) {
			return (id & m_chunkIDBitMaskShifted) | objectID;
		}

		public int getDetachedID(int id) {
			return ((id & DETACHED_BIT) >>> DETACHED_BIT_INDEX);
		}
		
		public int getReleasedID(int id) {
			return ((id & RELEASED_BIT) >>> RELEASED_BIT_INDEX);
		}
		
		public int getChunkID(int id) {
			return (id >>> m_chunkBit) & m_chunkIDBitMask;
		}

		public int getObjectID(int id) {
			return id & m_objectIDBitMask;
		}

		public String idToString(int id) {
			StringBuilder sb = new StringBuilder();
			sb.append(getDetachedID(id));
			sb.append(':');
			sb.append(getReleasedID(id));
			sb.append(':');
			sb.append(getChunkID(id));
			sb.append(':');
			sb.append(getObjectID(id));
			return sb.toString();
		}

		public int getChunkBit() {
			return m_chunkBit;
		}

		public int getChunkCountBit() {
			return m_chunkCountBit;
		}

		public int getChunkCount() {
			return m_chunkCount;
		}

		public int getChunkIDBitMask() {
			return m_chunkIDBitMask;
		}

		public int getChunkIDBitMaskShifted() {
			return m_chunkIDBitMaskShifted;
		}

		public int getChunkCapacity() {
			return m_chunkCapacity;
		}

		public int getObjectIDBitMask() {
			return m_objectIDBitMask;
		}
	}
}

