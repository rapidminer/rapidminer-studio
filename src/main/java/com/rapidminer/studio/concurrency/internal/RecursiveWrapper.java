/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
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
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
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
class RecursiveWrapper<T> extends CountedCompleter<Void> {

	/**
	 * {@link RuntimeException} that temporarily wraps checked exception in order to unwrap them later.
	 *
	 * @since 9.2
	 */
	static final class WrapperRuntimeException extends RuntimeException {

		private static final long serialVersionUID = -5276047218418452356L;

		WrapperRuntimeException(Exception e){
			super(e);
		}
	}

	private static final long serialVersionUID = 1L;

	private final transient List<Callable<T>> callables;
	private final transient T[] results;
	private final AtomicBoolean alive;
	private final int from;
	private final int to;

	RecursiveWrapper(CountedCompleter<?> parent, List<Callable<T>> callables, T[] results, int from, int to,
					 AtomicBoolean alive) {
		super(parent);
		this.callables = callables;
		this.results = results;
		this.from = from;
		this.to = to;
		this.alive = alive;
	}

	@Override
	public void compute() {
		if (alive.get()) {
			int start = this.from;
			int end = this.to;

			while (end - start > 1) {
				int middle = start + end >>> 1;
				addToPendingCount(1);
				new RecursiveWrapper<>(this, callables, results, middle, end, alive).fork();
				end = middle;
			}

			try {
				results[start] = callables.get(start).call();
				// do the same error handling as ForkJoinTask$AdaptedCallable and set sentinel to false
			} catch (Error | RuntimeException e) {
				alive.set(false);
				throw e;
			} catch (Exception ex) {
				alive.set(false);
				//Use custom wrapper for easier unwrapping
				throw new WrapperRuntimeException(ex);
			}
		}
		propagateCompletion();
	}

	/**
	 * Calls the given callables using {@link CountedCompleter}s. Can only be called from inside a
	 * {@link ForkJoinPool}.
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
		RecursiveWrapper<T> action = new RecursiveWrapper<>(null, callables, resultArray, 0, callables.size(),
				new AtomicBoolean(true));
		try {
			action.invoke();
			return Arrays.asList(resultArray);
		} catch (ProcessStoppedRuntimeException e) {
			// handle ProcessStoppedRuntimeException as done by StudioConcurrencyContext#collectResults
			throw e;
		}catch (WrapperRuntimeException e){
			// unwrap own wrapped exceptions and wrap into ExecutionException
			throw new ExecutionException(e.getCause());
		} catch (Throwable e) {
			// do same wrapping as ForkJoinTask#get
			throw new ExecutionException(e);
		}
	}

}