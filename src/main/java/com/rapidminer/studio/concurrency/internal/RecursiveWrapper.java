/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.studio.concurrency.internal;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

import com.rapidminer.core.concurrency.ExecutionStoppedException;
import com.rapidminer.studio.internal.ProcessStoppedRuntimeException;


/**
 * Wraps an arbitrary list of {@link Callable}s to allow for efficient work-stealing (load distribution) inside a {@link
 * ForkJoinPool}.
 *
 * @author Michael Knopf, Gisa Meier
 * @since 8.0
 */
class RecursiveWrapper<T> extends RecursiveAction {

	private static final long serialVersionUID = 1L;

	private final transient List<Callable<T>> callables;
	private final transient T[] results;
	private final AtomicBoolean alive;
	private final int from;
	private final int to;

	private RecursiveWrapper<T> next;

	RecursiveWrapper(List<Callable<T>> callables, T[] results, int from, int to, RecursiveWrapper<T> next,
					 AtomicBoolean alive) {
		this.callables = callables;
		this.results = results;
		this.from = from;
		this.to = to;
		this.next = next;
		this.alive = alive;
	}

	@Override
	protected void compute() {
		if (alive.get()) {
			int start = this.from;
			int end = this.to;

			RecursiveWrapper<T> nextPartition = null;

			while (end - start > 1) {
				int middle = start + end >>> 1;
				nextPartition = new RecursiveWrapper<>(callables, results, middle, end, nextPartition, alive);
				nextPartition.fork();
				end = middle;
			}

			try {
				results[start] = callables.get(start).call();
				while (nextPartition != null) {
					if (nextPartition.tryUnfork()) {
						nextPartition.compute();
					} else {
						nextPartition.join();
					}
					nextPartition = nextPartition.next;
				}
				// do the same error handling as ForkJoinTask$AdaptedCallable and set sentinel to false
			} catch (Error | RuntimeException e) {
				alive.set(false);
				throw e;
			} catch (Exception ex) {
				alive.set(false);
				throw new RuntimeException(ex);
			}
		}
	}

	/**
	 * Calls the given callables using {@link RecursiveAction}s. Can only be called from inside a {@link ForkJoinPool}.
	 * 
	 * @param callables
	 *            the callables to call
	 * @return a list containing the results of the callables
	 * @throws ExecutionException
	 *             if an exception occurred while computing
	 * @throws ExecutionStoppedException
	 *             if the execution was stopped
	 */
	static <T> List<T> call(List<Callable<T>> callables) throws ExecutionException {
		@SuppressWarnings("unchecked")
		T[] resultArray = (T[]) new Object[callables.size()];
		RecursiveWrapper<T> action = new RecursiveWrapper<>(callables, resultArray, 0, callables.size(), null,
				new AtomicBoolean(true));
		try {
			action.compute();
			return Arrays.asList(resultArray);
		} catch (ProcessStoppedRuntimeException e) {
			// handle ProcessStoppedRuntimeException as done by StudioConcurrencyContext#collectResults
			throw (ExecutionStoppedException) e.getCause();
		} catch (Throwable e) {
			// do same wrapping as ForkJoinTask#get
			throw new ExecutionException(e);
		}
	}

}