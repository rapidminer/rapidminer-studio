/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import com.rapidminer.Process;
import com.rapidminer.ProcessListener;
import com.rapidminer.RapidMiner;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.core.concurrency.ExecutionStoppedException;
import com.rapidminer.operator.Operator;
import com.rapidminer.studio.internal.ProcessStoppedRuntimeException;
import com.rapidminer.tools.ParameterService;


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

	/** The corresponding process. */
	private final Process process;

	/** The fork join pool all task are submitted to. */
	private volatile ForkJoinPool pool = null;

	/** The parallelism level. */
	private final int parallelismLevel;

	/**
	 * This enumeration reflects the possible states of the context. It is used to manage the pool
	 * and to prevent concurrent submissions. State transitions are handled via
	 * {@link StudioConcurrencyContext#transitionState(ContextState)}.
	 *
	 * @author Michael Knopf
	 */
	private enum ContextState {
		/** The worker pool is <strong>not initialized</strong> and no submissions are queued. */
		PASSIVE,
		/** The worker pool is <strong>initialized and processing</strong> a submitted task. */
		WORKING,
		/** The worker pool is <strong>initialized and idling</strong>, no submissions are queued. */
		IDLE;
	};

	/** Counter for concurrent submissions. */
	private AtomicInteger concurrentSubmissions = new AtomicInteger(0);

	/**
	 * The current status of the context. <strong>Do not manipulate this attribute outside of
	 * {@link #transitionState(ContextState)}!</strong>
	 */
	private ContextState state = ContextState.PASSIVE;

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

		// remember process
		this.process = process;

		// look up parallelism level
		String numberOfThreads = ParameterService
				.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_NUMBER_OF_THREADS);

		int userLevel = 0;
		if (numberOfThreads != null) {
			try {
				userLevel = Integer.parseInt(numberOfThreads);
			} catch (NumberFormatException e) {
				// ignore and use default value
			}
		}

		// zero is a placeholder for the default value
		if (userLevel == 0) {
			userLevel = Runtime.getRuntime().availableProcessors() - 1;
		}

		parallelismLevel = Math.min(Math.max(1, userLevel), FJPOOL_MAXIMAL_PARALLELISM);

		// listen to process state changes to shutdown pool when necessary
		process.getRootOperator().addProcessListener(new ProcessListener() {

			@Override
			public void processStarts(Process process) {
				// ignore
			}

			@Override
			public void processStartedOperator(Process process, Operator op) {
				// ignore
			}

			@Override
			public void processFinishedOperator(Process process, Operator op) {
				// ignore
			}

			@Override
			public void processEnded(Process process) {
				// Transition back to state PASSIVE to shut down the pool. Note that this does
				// not prevent the context to be used for further process executions (the pool
				// will be reinitialized).
				transitionState(ContextState.PASSIVE);
			}
		});
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

		// Change to state WORKING (if not already in that state).
		transitionState(ContextState.WORKING);

		// wrap runnables in callables
		List<Callable<Void>> callables = new ArrayList<>(runnables.size());
		for (final Runnable runnable : runnables) {
			callables.add(new Callable<Void>() {

				@Override
				public Void call() throws Exception {
					runnable.run();
					return null;
				}
			});
		}

		// submit callables without further checks
		try {
			callUnchecked(callables);
		} finally {
			transitionState(ContextState.IDLE);
		}
	}

	@Override
	public <T> List<T> call(List<Callable<T>> callables) throws ExecutionException, ExecutionStoppedException {
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

		// Change to state WORKING (if not already in that state).
		transitionState(ContextState.WORKING);

		// submit callables without further checks
		try {
			List<T> result = callUnchecked(callables);
			return result;
		} finally {
			transitionState(ContextState.IDLE);
		}
	}

	/**
	 * Utility method to submit a list of {@code Callable}s without further checks of the submission
	 * lock.
	 *
	 * @param callables
	 *            the callables
	 * @return the callables' results
	 * @throws ExecutionException
	 *             if the computation threw an exception
	 * @throws ExecutionStoppedException
	 *             if the computation was stopped
	 */
	private <T> List<T> callUnchecked(List<Callable<T>> callables) throws ExecutionException, ExecutionStoppedException {
		// the following line is blocking
		List<Future<T>> futures = pool.invokeAll(callables);
		List<T> results = new ArrayList<>(callables.size());
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
		return parallelismLevel;
	}

	/**
	 * Implements transitions between {@link ContextState}s. Supported transitions are:
	 * <ul>
	 * <li>{@code PASSIVE} &#8594; {@code PASSIVE} (no operation)</li>
	 * <li>{@code PASSIVE} &#8594; {@code WORKING} (initializes the pool)</li>
	 * <li>{@code WORKING} &#8594; {@code IDLE} (may not change state if there are concurrent
	 * submissions)</li>
	 * <li>{@code WORKING} &#8594; {@code WORKING} (concurrent submission)</li>
	 * <li>{@code IDLE} &#8594; {@code IDLE} (no operation)</li>
	 * <li>{@code IDLE} &#8594; {@code WORKING}</li>
	 * <li>{@code IDLE} &#8594; {@code PASSIVE} (shuts down the pool)</li>
	 * </ul>
	 * If an unsupported transition is triggered, an {@link IllegalStateException} is thrown.
	 *
	 *
	 * @param targetState
	 *            the target state
	 * @throws IllegalStateException
	 *             if the transition is not supported
	 */
	private synchronized void transitionState(ContextState targetState) {
		switch (targetState) {
			case PASSIVE:
				if (this.state == ContextState.WORKING) {
					throw new IllegalStateException("Unsupported transition.");
				} else if (this.state == ContextState.IDLE) {
					// shutdown current pool
					if (this.pool != null) {
						this.pool.shutdownNow();
						this.pool = null;
					}
				}
				this.state = ContextState.PASSIVE;
				break;
			case WORKING:
				if (this.state == ContextState.PASSIVE) {
					// initialize pool
					if (pool == null) {
						pool = new ForkJoinPool(parallelismLevel);
					}
				}
				// increase submission counter and change state
				this.concurrentSubmissions.incrementAndGet();
				this.state = ContextState.WORKING;
				break;
			case IDLE:
				if (this.state == ContextState.PASSIVE) {
					throw new IllegalStateException("Unsupported transition.");
				}
				// only transition to IDLE state of no more submissions are being processed
				if (this.concurrentSubmissions.decrementAndGet() == 0) {
					this.state = ContextState.IDLE;
				}
				break;
		}
	}

	@Override
	public void checkStatus() throws ExecutionStoppedException {
		if (process != null && process.shouldStop()) {
			throw new ProcessStoppedRuntimeException();
		}
	}

	@Override
	public <T> T invoke(ForkJoinTask<T> task) throws ExecutionException, ExecutionStoppedException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> List<T> invokeAll(final List<ForkJoinTask<T>> tasks) throws ExecutionException, ExecutionStoppedException {
		throw new UnsupportedOperationException();
	}

}
