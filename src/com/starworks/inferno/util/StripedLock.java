package com.starworks.inferno.util;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.LockSupport;

/**
 * A striped lock is a concurrency control mechanism that allows for multiple
 * threads to acquire locks on different keys concurrently. The lock is
 * constructed with a specified concurrency level, which determines the number
 * of stripes or locks that are available.
 *
 * <p>
 * A thread can acquire a lock on a specific key by calling the
 * {@link #lock(int)} method, passing in the key as a parameter. If the key is
 * already locked, the calling thread will be blocked until the lock becomes
 * available. The thread can release the lock on a key by calling the
 * {@link #unlock(int)} method and passing in the key.
 *
 * <p>
 * The {@link #stripedReadWriteLock()} and {@link #stripedReadWriteLock(int)}
 * methods return an instance of a {@link StripedReadWriteLock}, which is a
 * subclass of {@code StripedLock} that allows for read and write locks to be
 * acquired on a key.
 *
 * @author Ethan Temprovich
 * @see StripedReadWriteLock
 * @sealed
 */
public sealed class StripedLock permits StripedLock.StripedReadWriteLock {

	protected final int m_concurrencyLevel;
	protected final Thread[] m_owners;
	protected final AtomicIntegerArray m_counts;
	protected final AtomicIntegerArray m_waitingThreads;

	/**
	 * Constructs a striped lock with the default concurrency level.
	 *
	 * <p>
	 * The default concurrency level is equal to the number of available processors,
	 * as returned by {@link Runtime#availableProcessors()}.
	 */
	public StripedLock() {
		this(Runtime.getRuntime().availableProcessors());
	}

	/**
	 * Constructs a striped lock with the specified concurrency level.
	 *
	 * @param concurrencyLevel the number of stripes in the lock
	 * @throws IllegalArgumentException if the concurrency level is less than or
	 *                                  equal to 1
	 */
	public StripedLock(int concurrencyLevel) {
		if (concurrencyLevel <= 1) throw new IllegalArgumentException("Concurrency level may not be less than 2.");
		this.m_concurrencyLevel = nextPowerOfTwo(concurrencyLevel);
		this.m_owners = new Thread[m_concurrencyLevel];
		this.m_counts = new AtomicIntegerArray(m_concurrencyLevel);
		this.m_waitingThreads = new AtomicIntegerArray(m_concurrencyLevel);
	}

	public static StripedReadWriteLock stripedReadWriteLock() {
		return new StripedReadWriteLock();
	}

	public static StripedReadWriteLock stripedReadWriteLock(int concurrencyLevel) {
		return new StripedReadWriteLock(concurrencyLevel);
	}

	/**
	 * Acquires a stripe based on the key, and attempts to lock. If the key is
	 * already locked, the calling thread will be blocked until the lock becomes
	 * available.
	 * 
	 * @param key the key to lock
	 * @throws InterruptedException if the calling thread is interrupted while
	 *                              waiting for the lock
	 */
	public void lock(int key) throws InterruptedException {
		int stripe = hash(key);
		Thread currentThread = Thread.currentThread();
		if (currentThread == m_owners[stripe]) {
			m_counts.incrementAndGet(stripe);
			return;
		}
		m_waitingThreads.incrementAndGet(stripe);
		try {
			for (;;) {
				if (m_owners[stripe] == null) {
					m_owners[stripe] = currentThread;
					m_waitingThreads.decrementAndGet(stripe);
					m_counts.incrementAndGet(stripe);
					return;
				}
				if (Thread.interrupted()) {
					throw new InterruptedException();
				}
				while (m_owners[stripe] != null || m_waitingThreads.get(stripe) > 1) {
					if (Thread.interrupted()) {
						throw new InterruptedException();
					}
					LockSupport.park();
				}
			}
		} finally {
			if (Thread.interrupted()) {
				m_waitingThreads.decrementAndGet(stripe);
				LockSupport.unpark(m_owners[stripe]);
				throw new InterruptedException();
			}
		}
	}

	/**
	 * Releases a lock on the specified key. If the calling thread does not own the
	 * lock for the key, an IllegalMonitorStateException is thrown.
	 * 
	 * @param key the key to unlock
	 * @throws IllegalMonitorStateException if the calling thread does not own the
	 *                                      lock for the key
	 */
	public void unlock(int key) {
		int stripe = hash(key);
		Thread currentThread = Thread.currentThread();
		if (currentThread != m_owners[stripe]) {
			throw new IllegalMonitorStateException();
		}
		int count = m_counts.decrementAndGet(stripe);
		if (count == 0) {
			LockSupport.unpark(m_owners[stripe]);
			m_owners[stripe] = null;
		}
	}

	/**
	 * Returns true if the specified key is locked.
	 * 
	 * @param key the key to check
	 * @return true if the key is locked, false otherwise
	 */
	public boolean isLocked(int key) {
		int stripe = hash(key);
		return m_owners[stripe] != null;
	}

	/**
	 * Returns true if the calling thread owns a lock on the specified key.
	 * 
	 * @param key the key to check
	 * @return true if the calling thread owns a lock on the specified key
	 */
	public boolean hasLock(int key) {
		int stripe = hash(key);
		return m_owners[stripe] == Thread.currentThread();
	}

	/**
	 * Returns true if the current thread holds the lock for the specified key.
	 * 
	 * @param key the key to check
	 * @return true if the current thread holds the lock for the key, false
	 *         otherwise
	 */
	public boolean isHeldByCurrentThread(int key) {
		int stripe = hash(key);
		return Thread.holdsLock(this) && m_owners[stripe] == Thread.currentThread();
	}

	/**
	 * This implementation is based on MurmurHash.
	 * 
	 * @param key the key to hash
	 * @return the hashed value of the key, modulo the concurrency level
	 */
	protected int hash(int key) {
		// The magic constants are the two 32-bit Mersenne primes, 2^31-1 and 2^32-5.
		int h = (key ^ (key >>> 16)) * 0x85ebca6b;
		h ^= h >>> 13;
		h *= 0xc2b2ae35;
		h ^= h >>> 16;
		// Return the absolute value of h modulo the concurrency level
		return Math.abs(h % m_concurrencyLevel); 
	}

	/**
	 * Returns the number of stripes in the lock.
	 * 
	 * @return the number of stripes in the lock
	 */
	public int size() {
		return m_concurrencyLevel;
	}

	private static int nextPowerOfTwo(int value) {
		return 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(value - 1));
	}

	/**
	 * A striped lock that supports read-write lock semantics which supports
	 * interruptible lock acquisition and fairness policies.
	 * 
	 * @author Ethan Temprovich
	 */
	public static final class StripedReadWriteLock extends StripedLock {

		private final AtomicIntegerArray m_readLocks;
		private final AtomicIntegerArray m_writeLocks;

		/**
		 * Constructs a striped read-write lock with the default concurrency level.
		 */
		private StripedReadWriteLock() {
			this(Runtime.getRuntime().availableProcessors());
		}

		/**
		 * Constructs a striped read-write lock with the specified concurrency level.
		 * 
		 * @param concurrencyLevel the concurrency level of the lock
		 */
		public StripedReadWriteLock(int concurrencyLevel) {
			super(concurrencyLevel);
			this.m_readLocks = new AtomicIntegerArray(concurrencyLevel);
			this.m_writeLocks = new AtomicIntegerArray(concurrencyLevel);
		}

		/*
		 * (non-Javadoc)
		 */
		@Override
		public void lock(int key) {
			throw new UnsupportedOperationException();
		}

		/*
		 * (non-Javadoc)
		 */
		@Override
		public void unlock(int key) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Acquires a read lock on the specified key. If the key is already locked for
		 * writing, the calling thread will be blocked until the lock becomes available.
		 * 
		 * @param key the key to lock
		 * @throws InterruptedException if the calling thread is interrupted while
		 *                              waiting for the lock
		 */
		public void readLock(int key) throws InterruptedException {
			int stripe = hash(key);
			Thread currentThread = Thread.currentThread();
			if (currentThread == m_owners[stripe]) {
				m_counts.incrementAndGet(stripe);
				return;
			}
			m_waitingThreads.incrementAndGet(stripe);
			try {
				for (;;) {
					if (m_owners[stripe] == null && m_writeLocks.get(stripe) == 0) {
						m_owners[stripe] = currentThread;
						m_waitingThreads.decrementAndGet(stripe);
						m_readLocks.incrementAndGet(stripe);
						return;
					}
					if (Thread.interrupted()) {
						throw new InterruptedException();
					}
					while (m_owners[stripe] != null || m_writeLocks.get(stripe) > 0) {
						if (Thread.interrupted()) {
							throw new InterruptedException();
						}
						LockSupport.park();
					}
				}
			} finally {
				if (Thread.interrupted()) {
					m_waitingThreads.decrementAndGet(stripe);
					LockSupport.unpark(m_owners[stripe]);
					throw new InterruptedException();
				}
			}
		}

		/**
		 * Releases a read lock on the specified key. If the calling thread does not own
		 * the lock for the key, an IllegalMonitorStateException is thrown.
		 * 
		 * @param key the key to unlock
		 * @throws IllegalMonitorStateException if the calling thread does not own the
		 *                                      lock for the key
		 */
		public void unlockRead(int key) {
			int stripe = hash(key);
			Thread currentThread = Thread.currentThread();
			if (currentThread != m_owners[stripe]) {
				throw new IllegalMonitorStateException();
			}
			int count = m_readLocks.decrementAndGet(stripe);
			if (count == 0) {
				m_owners[stripe] = null;
				LockSupport.unpark(m_owners[stripe]);
			}
		}

		/**
		 * Acquires a write lock on the specified key. If the key is already locked for
		 * reading or writing, the calling thread will be blocked until the lock becomes
		 * available.
		 * 
		 * @param key the key to lock
		 * @throws InterruptedException if the calling thread is interrupted while
		 *                              waiting for the lock
		 */
		public void writeLock(int key) throws InterruptedException {
			int stripe = hash(key);
			Thread currentThread = Thread.currentThread();
			if (currentThread == m_owners[stripe]) {
				m_counts.incrementAndGet(stripe);
				return;
			}
			m_waitingThreads.incrementAndGet(stripe);
			try {
				for (;;) {
					if (m_owners[stripe] == null && m_readLocks.get(stripe) == 0 && m_writeLocks.get(stripe) == 0) {
						m_owners[stripe] = currentThread;
						m_waitingThreads.decrementAndGet(stripe);
						m_writeLocks.incrementAndGet(stripe);
						return;
					}
					if (Thread.interrupted()) {
						throw new InterruptedException();
					}
					while (m_owners[stripe] != null || m_readLocks.get(stripe) > 0 || m_writeLocks.get(stripe) > 0) {
						if (Thread.interrupted()) {
							throw new InterruptedException();
						}
						LockSupport.park();
					}
				}
			} finally {
				if (Thread.interrupted()) {
					m_waitingThreads.decrementAndGet(stripe);
					LockSupport.unpark(m_owners[stripe]);
					throw new InterruptedException();
				}
			}
		}

		/**
		 * Releases a write lock on the specified key. If the calling thread does not
		 * own the lock for the key, an IllegalMonitorStateException is thrown.
		 * 
		 * @param key the key to unlock
		 * @throws IllegalMonitorStateException if the calling thread does not own the
		 *                                      lock for the key
		 */
		public void unlockWrite(int key) {
			int stripe = hash(key);
			Thread currentThread = Thread.currentThread();
			if (currentThread != m_owners[stripe]) {
				throw new IllegalMonitorStateException();
			}
			int count = m_writeLocks.decrementAndGet(stripe);
			if (count == 0) {
				m_owners[stripe] = null;
				LockSupport.unpark(m_owners[stripe]);
			}
		}

		/**
		 * Upgrades a read lock to a write lock. If the key is already locked for
		 * writing, the calling thread will be blocked until the lock becomes available.
		 * 
		 * @param key the key to upgrade
		 * @throws InterruptedException if the calling thread is interrupted while
		 *                              waiting for the lock
		 */
		public void upgrade(int key) throws InterruptedException {
			int stripe = hash(key);
			Thread currentThread = Thread.currentThread();
			if (currentThread == m_owners[stripe]) {
				m_counts.incrementAndGet(stripe);
				return;
			}
			m_waitingThreads.incrementAndGet(stripe);
			try {
				for (;;) {
					if (m_owners[stripe] == null && m_readLocks.get(stripe) == 1 && m_writeLocks.get(stripe) == 0) {
						m_owners[stripe] = currentThread;
						m_waitingThreads.decrementAndGet(stripe);
						m_readLocks.decrementAndGet(stripe);
						m_writeLocks.incrementAndGet(stripe);
						return;
					}
					if (Thread.interrupted()) {
						throw new InterruptedException();
					}
					while (m_owners[stripe] != null || m_readLocks.get(stripe) > 1 || m_writeLocks.get(stripe) > 0) {
						if (Thread.interrupted()) {
							throw new InterruptedException();
						}
						LockSupport.park();
					}
				}
			} finally {
				if (Thread.interrupted()) {
					m_waitingThreads.decrementAndGet(stripe);
					LockSupport.unpark(m_owners[stripe]);
					throw new InterruptedException();
				}
			}
		}

		/**
		 * Downgrades a write lock to a read lock.
		 * 
		 * @param key the key to downgrade
		 * @throws IllegalMonitorStateException if the calling thread does not own the
		 *                                      lock for the key
		 */
		public void downgrade(int key) {
			int stripe = hash(key);
			Thread currentThread = Thread.currentThread();
			if (currentThread != m_owners[stripe]) {
				throw new IllegalMonitorStateException();
			}
			int count = m_writeLocks.decrementAndGet(stripe);
			if (count == 0) {
				m_owners[stripe] = null;
				LockSupport.unpark(m_owners[stripe]);
			}
			m_readLocks.incrementAndGet(stripe);
		}

		/**
		 * Returns true if the calling thread owns a read lock on the specified key.
		 * 
		 * @param key the key to check
		 * @return true if the calling thread owns a read lock on the specified key
		 */
		public boolean hasReadLock(int key) {
			int stripe = hash(key);
			return m_owners[stripe] == Thread.currentThread() && m_readLocks.get(stripe) > 0;
		}

		/**
		 * Returns true if the calling thread owns a write lock on the specified key.
		 * 
		 * @param key the key to check
		 * @return true if the calling thread owns a write lock on the specified key
		 */
		public boolean hasWriteLock(int key) {
			int stripe = hash(key);
			return m_owners[stripe] == Thread.currentThread() && m_writeLocks.get(stripe) > 0;
		}
	}
}