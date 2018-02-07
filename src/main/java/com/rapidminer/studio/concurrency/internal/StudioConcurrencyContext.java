/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.studio.concurrency.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

import com.rapidminer.Process;
import com.rapidminer.RapidMiner;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.core.concurrency.ExecutionStoppedException;
import com.rapidminer.studio.internal.ParameterServiceRegistry;
import com.rapidminer.studio.internal.ProcessStoppedRuntimeException;
import com.rapidminer.tools.LogService;


/**
 * Simple {@link ConcurrencyContext} to be used with a single {@link Process}.
 * <p>
 * The context does not implement the submission methods for {@link ForkJoinTask}s.
 *
 * @author Gisa Schaefer, Michael Knopf
 * @since 6.2.0
 */
public class StudioConcurrencyContext implements ConcurrencyContext {

	/**
	 * The current ForkJoinPool implementation restricts the maximum number of running threads to
	 * 32767. Attempts to create pools with greater than the maximum number result in
	 * IllegalArgumentException.
	 */
	private static final int FJPOOL_MAXIMAL_PARALLELISM = 32767;

	/** Locks to handle access from different threads */
	private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock(true);
	private static final Lock READ_LOCK = LOCK.readLock();
	private static final Lock WRITE_LOCK = LOCK.writeLock();

	/** The fork join pool all task are submitted to. */
	private static ForkJoinPool pool = null;

	/** The corresponding process. */
	private final Process process;



	/**
	 * Creates a new {@link ConcurrencyContext} for the given {@link Process}.
	 * <p>
	 * The context assumes that only operators that belong to the corresponding process submit tasks
	 * to this context.
	 *
	 * @param process
	 *            the corresponding process
	 */
	public StudioConcurrencyContext(Process process) {
		if (process == null) {
			throw new IllegalArgumentException("process must not be null");
		}
		this.process = process;
	}

	@Override
	public void run(List<Runnable> runnables) throws ExecutionException, ExecutionStoppedException {
		if (runnables == null) {
			throw new IllegalArgumentException("runnables must not be null");
		}

		// nothing to do if list is empty
		if (runnables.isEmpty()) {
			return;
		}

		// check for null runnables
		for (Runnable runnable : runnables) {
			if (runnable == null) {
				throw new IllegalArgumentException("runnables must not contain null");
			}
		}

		// wrap runnables in callables
		List<Callable<Void>> callables = new ArrayList<>(runnables.size());
		for (final Runnable runnable : runnables) {
			callables.add(() -> {
				runnable.run();
				return null;
			});
		}

		// submit callables without further checks
		call(callables);
	}

	@Override
	public <T> List<T> call(List<Callable<T>> callables)
			throws ExecutionException, ExecutionStoppedException, IllegalArgumentException {
		if (callables == null) {
			throw new IllegalArgumentException("callables must not be null");
		}

		// nothing to do if list is empty
		if (callables.isEmpty()) {
			return Collections.emptyList();
		}

		// check for null tasks
		for (Callable<T> callable : callables) {
			if (callable == null) {
				throw new IllegalArgumentException("callables must not contain null");
			}
		}

		ForkJoinPool forkJoinPool = AccessController.doPrivileged(
				(PrivilegedAction<ForkJoinPool>) this::getForkJoinPool);
		// handle submissions from inside and outside the pool differently
		Thread currentThread = Thread.currentThread();
		if (currentThread instanceof ForkJoinWorkerThread
				&& ((ForkJoinWorkerThread) currentThread).getPool() == forkJoinPool) {
			return RecursiveWrapper.call(callables);
		} else {
			final List<Future<T>> futures = new ArrayList<>(callables.size());
			for (Callable<T> callable : callables) {
				futures.add(forkJoinPool.submit(callable));
			}
			return collectResults(futures);
		}
	}

	@Override
	public <T> List<Future<T>> submit(List<Callable<T>> callables) throws IllegalArgumentException {
		if (callables == null) {
			throw new IllegalArgumentException("callables must not be null");
		}

		// nothing to do if list is empty
		if (callables.isEmpty()) {
			return Collections.emptyList();
		}

		// check for null tasks
		for (Callable<T> callable : callables) {
			if (callable == null) {
				throw new IllegalArgumentException("callables must not contain null");
			}
		}

		ForkJoinPool forkJoinPool = AccessController.doPrivileged(
				(PrivilegedAction<ForkJoinPool>) this::getForkJoinPool);
		// handle submissions from inside and outside the pool differently
		Thread currentThread = Thread.currentThread();
		if (currentThread instanceof ForkJoinWorkerThread
				&& ((ForkJoinWorkerThread) currentThread).getPool() == forkJoinPool) {
			final List<Future<T>> futures = new ArrayList<>(callables.size());
			for (Callable<T> callable : callables) {
				futures.add(ForkJoinTask.adapt(callable).fork());
			}
			return futures;
		} else {
			// submit callables without further checks
			final List<Future<T>> futures = new ArrayList<>(callables.size());
			for (Callable<T> callable : callables) {
				futures.add(forkJoinPool.submit(callable));
			}
			return futures;
		}
	}

	@Override
	public <T> List<T> collectResults(List<Future<T>> futures)
			throws ExecutionException, ExecutionStoppedException, IllegalArgumentException {
		if (futures == null) {
			throw new IllegalArgumentException("futures must not be null");
		}

		// nothing to do if list is empty
		if (futures.isEmpty()) {
			return Collections.emptyList();
		}

		// check for null tasks
		for (Future<T> future : futures) {
			if (future == null) {
				throw new IllegalArgumentException("futures must not contain null");
			}
		}

		List<T> results = new ArrayList<>(futures.size());
		for (Future<T> future : futures) {
			try {
				T result = future.get();
				results.add(result);
			} catch (InterruptedException | RejectedExecutionException e) {
				// The pool's invokeAll() method calls Future.get() internally. If the process is
				// stopped by the user, these calls might be interrupted before calls to
				// checkStatus() throw an ExecutionStoppedException. Thus, we need to check the
				// current status again.
				checkStatus();
				// InterruptedExceptions are very unlikely to happen at this point, since the above
				// calls to get() will return immediately. A RejectedExectutionException is an
				// extreme corner case as well. In both cases, there is no benefit for the API user
				// if the exception is passed on directly. Thus, we can wrap it within a
				// ExecutionException which is part of the API.
				throw new ExecutionException(e);
			} catch (ExecutionException e) {
				// A ProcessStoppedRuntimeException is an internal exception thrown if the user
				// requests the process to stop (see the checkStatus() implementation of this
				// class). This exception should not be wrapped or consumed here, since it is
				// handled by the operator implementation itself.
				if (e.getCause() instanceof ProcessStoppedRuntimeException) {
					throw (ExecutionStoppedException) e.getCause();
				} else {
					throw e;
				}
			}
		}

		return results;
	}

	@Override
	public int getParallelism() {
		if (pool != null) {
			return AccessController.doPrivileged((PrivilegedAction<ForkJoinPool>) this::getForkJoinPool)
					.getParallelism();
		} else {
			return getDesiredParallelismLevel();
		}
	}

	@Override
	public void checkStatus() throws ExecutionStoppedException {
		if (process.shouldStop()) {
			throw new ProcessStoppedRuntimeException();
		}
	}

	@Override
	public <T> T invoke(ForkJoinTask<T> task) throws ExecutionException, ExecutionStoppedException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> List<T> invokeAll(final List<ForkJoinTask<T>> tasks) throws ExecutionException,
			ExecutionStoppedException {
		throw new UnsupportedOperationException();
	}

	/**
	 * This method verifies and returns a JVM-wide, static FJPool. Override if a different pool behavior is needed.
	 *
	 * @return the ForkJoinPool to use for this context instance for execution.
	 */
	protected ForkJoinPool getForkJoinPool() {
		READ_LOCK.lock();
		try {
			if (!isPoolOutdated()) {
				// nothing to do
				return pool;
			}
		} finally {
			READ_LOCK.unlock();
		}
		WRITE_LOCK.lock();
		try {
			if (!isPoolOutdated()) {
				// pool has been updated in the meantime
				// no reason to re-create the pool once again
				return pool;
			}
			if (pool != null) {
				pool.shutdown();
			}
			int desiredParallelismLevel = getDesiredParallelismLevel();
			pool = new ForkJoinPool(desiredParallelismLevel);
			LogService.getRoot().log(Level.CONFIG,
					"com.rapidminer.concurrency.concurrency_context.pool_creation",
					desiredParallelismLevel);
			return pool;
		} finally {
			WRITE_LOCK.unlock();
		}
	}

	/**
	 * Checks if the JVM-wide, static pool needs to be re-created. Override if a different pool behavior is needed.
	 *
	 * @return {@code true} if the current pool is {@code null} or the
	 *         {@link #getDesiredParallelismLevel()} is not equal to the current {@link #pool}
	 *         parallelism otherwise {@code false}
	 */
	protected boolean isPoolOutdated() {
		return pool == null || getDesiredParallelismLevel() != pool.getParallelism();
	}

	/**
	 * Returns the desired number of cores to be used for concurrent computations for the JVM-wide, static pool. This
	 * number is always at least one and either bound by a license limit or by the user's configuration. Override if a
	 * different pool behavior is needed.
	 *
	 * @return the desired parallelism level
	 */
	protected int getDesiredParallelismLevel() {
		String numberOfThreads = ParameterServiceRegistry.INSTANCE
				.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_NUMBER_OF_THREADS);

		int userLevel = 0;

		if (numberOfThreads != null) {
			try {
				userLevel = Integer.parseInt(numberOfThreads);
				LogService.getRoot().log(Level.FINE, "com.rapidminer.concurrency.concurrency_context.parse_success",
						userLevel);
			} catch (NumberFormatException e) {
				// ignore and use default value
				LogService.getRoot().log(Level.FINE, "com.rapidminer.concurrency.concurrency_context.parse_failure",
						numberOfThreads);
			}
		}

		if (userLevel <= 0) {
			userLevel = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
		}

		// should not happen, but we want to avoid any exception during pool creation
		if (userLevel > FJPOOL_MAXIMAL_PARALLELISM) {
			userLevel = FJPOOL_MAXIMAL_PARALLELISM;
		}
		return userLevel;
	}

}
