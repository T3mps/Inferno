package com.starworks.inferno;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Scheduler implements Closeable {

	private final Registry m_registry;
	private final int m_timeoutSeconds;
	private final Map<Runnable, Single> m_taskMap;
	private final List<Task> m_mainTasks;
	private final ExecutorService m_mainExecutor;
	private final ForkJoinPool m_workStealExecutor;
	private final ScheduledExecutorService m_updateExecutor;
	private final StampedLock m_scheduleLock;
	private final ReentrantLock m_updateLock;
	private long m_time;
	private long m_elapsedTime;

	protected Scheduler(Registry registry, int timeoutSeconds) {
		this.m_registry = registry;
		this.m_timeoutSeconds = timeoutSeconds;
		this.m_taskMap = new HashMap<Runnable, Single>();
		this.m_mainTasks = new ArrayList<Task>();
		var threadFactory = new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				SchedulerThread schedulerThread = new SchedulerThread(r);
				return schedulerThread;
			}
		};
		this.m_mainExecutor = Executors.newSingleThreadExecutor(threadFactory);
		int nThreads = Runtime.getRuntime().availableProcessors();
		this.m_workStealExecutor = (ForkJoinPool) Executors.newWorkStealingPool(nThreads);
		this.m_updateExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
		this.m_scheduleLock = new StampedLock();
		this.m_updateLock = new ReentrantLock();
		this.m_time = System.nanoTime();
		this.m_elapsedTime = 1;
	}

	public Runnable submit(Runnable system) {
		long stamp = m_scheduleLock.writeLock();
		try {
			m_taskMap.computeIfAbsent(system, sys -> {
				Single single = new Single(sys);
				m_mainTasks.add(single);
				return single;
			});
			return system;
		} finally {
			m_scheduleLock.unlockWrite(stamp);
		}
	}

	public Runnable[] scheduleParallel(Runnable... systems) {
		long stamp = m_scheduleLock.writeLock();
		try {
			switch (systems.length) {
			case 0:
				return systems;
			case 1:
				submit(systems[0]);
			default: {
				var cluster = new Cluster(systems);
				m_mainTasks.add(cluster);
				m_taskMap.putAll(cluster.m_taskMap);
			}
			}
			return systems;
		} finally {
			m_scheduleLock.unlockWrite(stamp);
		}
	}

	public void update() {
		m_updateLock.lock();
		try {
			long prevTime = m_time;
			m_time = System.nanoTime();
			m_elapsedTime = m_time - prevTime;
			var futures = m_mainExecutor.invokeAll(m_mainTasks);
			if (futures.isEmpty()) return;
			futures.get(0).get(m_timeoutSeconds, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			System.err.println("Scheduler interrupted during update: " + e.getLocalizedMessage());
		} catch (ExecutionException e) {
			System.err.println("Failed to execute system: " + e.getLocalizedMessage());
		} catch (TimeoutException e) {
			System.err.println("System failed to execute within " + m_timeoutSeconds + "seconds: " + e.getLocalizedMessage());
		} finally {
			m_updateLock.unlock();
		}
	}

	public void forkAndJoin(Runnable subsystem) {
		Thread currentThread = Thread.currentThread();
		if (!(currentThread instanceof SchedulerThread || currentThread instanceof ForkJoinWorkerThread)) {
			throw new IllegalCallerException("Cannot invoke the forkAndJoin() method from outside other systems.");
		}
		try {
			m_workStealExecutor.invoke(new RecursiveAction() {
				private static final long serialVersionUID = 1L;

				@Override
				protected void compute() {
					subsystem.run();
				}
			});
		} catch (RuntimeException e) {
//			LOGGER.error("Failed to execute subsystem", e);
		}
	}

	public void suspend(Runnable system) {
		Single singleTask = m_taskMap.get(system);
		if (singleTask == null) {
			return;
		}
		singleTask.setEnabled(false);
	}

	public void resume(Runnable system) {
		Single singleTask = m_taskMap.get(system);
		if (singleTask == null) {
			return;
		}
		singleTask.setEnabled(true);
	}

	public double deltaTime() {
		return m_elapsedTime / 1_000_000_000.0;
	}

	public boolean shutdown() {
		m_updateExecutor.shutdown();
		m_mainExecutor.shutdown();
		m_workStealExecutor.shutdown();
		try {
			m_workStealExecutor.awaitTermination(m_timeoutSeconds, TimeUnit.SECONDS);
			m_updateExecutor.awaitTermination(m_timeoutSeconds, TimeUnit.SECONDS);
			m_mainExecutor.awaitTermination(m_timeoutSeconds, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			System.err.println("Scheduler failed to shutdown within " + m_timeoutSeconds + "seconds: " + e.getLocalizedMessage());
			m_registry.getSchedulers().remove(this);
			return false;
		}
		return true;
	}

	@Override
	public void close() {
		shutdown();
	}

	private interface Task extends Callable<Void> {
	}

	private final class Single implements Task {

		private final Runnable m_system;
		private final AtomicBoolean m_enabled;

		public Single(Runnable system) {
			this.m_system = system;
			this.m_enabled = new AtomicBoolean(true);
		}

		public Runnable getSystem() {
			return m_system;
		}

		public boolean isEnabled() {
			return m_enabled.get();
		}

		public void setEnabled(boolean enabled) {
			this.m_enabled.set(enabled);
		}

		@Override
		public Void call() {
			if (isEnabled()) {
				forkAndJoin(m_system);
			}
			return null;
		}

		private void run() {
			if (isEnabled()) {
				m_system.run();
			}
		}
	}

	private final class Cluster implements Task {

		private final List<Single> m_tasks;
		private final Map<Runnable, Single> m_taskMap;

		private Cluster(Runnable[] systems) {
			this.m_tasks = Arrays.stream(systems).map(Single::new).toList();
			this.m_taskMap = m_tasks.stream().collect(Collectors.toMap(Single::getSystem, Function.identity()));
		}

		@Override
		public Void call() {
			forkAndJoin(() -> ForkJoinTask.invokeAll(m_tasks.stream().map(single -> new RecursiveAction() {
				private static final long serialVersionUID = 1L;

				@Override
				protected void compute() {
					single.run();
				}
			}).toArray(ForkJoinTask[]::new)));
			return null;
		}
	}

	private static final class SchedulerThread extends Thread {
		private static final AtomicInteger counter = new AtomicInteger(0);

		public SchedulerThread(Runnable runnable) {
			super(runnable, "Scheduler" + counter.getAndIncrement());
		}
	}
}
