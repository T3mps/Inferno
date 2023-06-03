package com.starworks.inferno.collections;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;

import com.starworks.inferno.util.UnsafeSupport;

import sun.misc.Unsafe;

/**
 * A thread-safe stack implementation that stores integers.
 * 
 * <p>
 * The stack is based on a dynamically resizing array stored in off-heap memory,
 * using the {@link Unsafe} class to access and manipulate the memory directly.
 *
 * @author Ethan Temprovich
 */
public final class IntStack implements Closeable {

	private static final Unsafe unsafe = UnsafeSupport.getUnsafe();

	private final AtomicInteger m_index;
	private final StampedLock m_lock;
	private int m_capacity;
	private long m_address;

	/**
	 * Constructs a new stack with the given initial capacity.
	 *
	 * @param initialCapacity the initial capacity of the stack
	 */
	public IntStack(int initialCapacity) {
		this.m_index = new AtomicInteger(-Integer.BYTES);
		this.m_lock = new StampedLock();
		this.m_capacity = initialCapacity;
		this.m_address = unsafe.allocateMemory(initialCapacity);
	}

	/**
	 * Removes and returns the top element from the stack, or
	 * {@code Integer.MIN_VALUE} if the stack is empty.
	 *
	 * @return the top element from the stack, or {@code Integer.MIN_VALUE} if the
	 *         stack is empty
	 */
	public int pop() {
		int i = m_index.get();
		if (i < 0) {
			return Integer.MIN_VALUE;
		}
		int returnValue = unsafe.getInt(m_address + i);
		returnValue = m_index.compareAndSet(i, i - Integer.BYTES) ? returnValue : Integer.MIN_VALUE;
		return returnValue;
	}

	/**
	 * Pushes the given element onto the top of the stack.
	 *
	 * @param id the element to be pushed onto the stack
	 * @return {@code true} if the element was successfully pushed onto the stack,
	 *         {@code false} otherwise
	 */
	public boolean push(int id) {
		long offset = m_index.addAndGet(Integer.BYTES);
		if (offset >= m_capacity) {
			long l = m_lock.writeLock();
			try {
				int currentCapacity;
				if (offset >= (currentCapacity = m_capacity)) {
					int newCapacity = currentCapacity + (currentCapacity >>> 1);
					long newAddress = unsafe.allocateMemory(newCapacity);
					unsafe.copyMemory(m_address, newAddress, currentCapacity);
					m_capacity = newCapacity;
					m_address = newAddress;
				}
			} finally {
				m_lock.unlock(l);
			}
		}
		unsafe.putInt(m_address + offset, id);
		return true;
	}

	/**
	 * Removes all elements from the stack.
	 */
	public void clear() {
		m_index.set(-Integer.BYTES);
	}

	/**
	 * Returns the number of elements in the stack.
	 * 
	 * @return the number of elements in the stack
	 */
	public int size() {
		return (m_index.get() >> 2) + 1;
	}

	/**
	 * Returns the current capacity of the stack.
	 *
	 * @return the current capacity of the stack
	 */
	public int capacity() {
		return m_capacity;
	}

	/**
	 * Releases the memory used by the stack.
	 */
	@Override
	public void close() {
		unsafe.freeMemory(m_address);
	}
}