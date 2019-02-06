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
package com.rapidminer.adaption.belt;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.execution.ExecutionAbortedException;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.core.concurrency.ExecutionStoppedException;
import com.rapidminer.studio.internal.ProcessStoppedRuntimeException;

import junit.framework.TestCase;


/**
 * Tests the {@link ContextAdapter}.
 *
 * @author Gisa Meier
 */
public class ContextAdapterTest {

	private static final ConcurrencyContext studioContext = new ConcurrencyContext() {

		private ForkJoinPool pool = new ForkJoinPool(4);

		@Override
		public <T> List<Future<T>> submit(List<Callable<T>> callables) throws IllegalArgumentException {
			List<Future<T>> futures = new ArrayList<>();
			for (Callable<T> callable : callables) {
				futures.add(pool.submit(callable));
			}
			return futures;
		}

		@Override
		public <T> List<T> call(List<Callable<T>> callables) throws ExecutionException {
			List<Future<T>> futures = new ArrayList<>();
			for (Callable<T> callable : callables) {
				futures.add(pool.submit(callable));
			}
			List<T> results = new ArrayList<>();
			for(Future<T> future: futures){
				try {
					results.add(future.get());
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					//Do the same as in AbstractConcurrencyContext
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
		public void run(List<Runnable> arg0)
				throws ExecutionException, ExecutionStoppedException, IllegalArgumentException {
		}

		@Override
		public <T> List<T> invokeAll(List<ForkJoinTask<T>> arg0)
				throws ExecutionException, ExecutionStoppedException, IllegalArgumentException {
			return null;
		}

		@Override
		public <T> T invoke(ForkJoinTask<T> arg0)
				throws ExecutionException, ExecutionStoppedException, IllegalArgumentException {
			return null;
		}

		@Override
		public int getParallelism() {
			return 4;
		}

		@Override
		public <T> List<T> collectResults(List<Future<T>> arg0)
				throws ExecutionException, ExecutionStoppedException, IllegalArgumentException {
			return null;
		}

		@Override
		public void checkStatus() throws ExecutionStoppedException {
			throw new ExecutionStoppedException();
		}

	};

	@Test(expected = ExecutionAbortedException.class)
	public void testNotActive() throws ExecutionException, InterruptedException {
		Context context = ContextAdapter.adapt(studioContext);
		assertFalse(context.isActive());
		context.requireActive();
	}

	@Test
	public void testActive() throws ExecutionException, InterruptedException {
		Context context = ContextAdapter.adapt(new ConcurrencyContext() {
			@Override
			public void run(List<Runnable> list) throws ExecutionException, ExecutionStoppedException,
					IllegalArgumentException {

			}

			@Override
			public <T> List<T> call(List<Callable<T>> list) throws ExecutionException, ExecutionStoppedException,
					IllegalArgumentException {
				return null;
			}

			@Override
			public <T> List<Future<T>> submit(List<Callable<T>> list) throws IllegalArgumentException {
				return null;
			}

			@Override
			public <T> List<T> collectResults(List<Future<T>> list) throws ExecutionException,
					ExecutionStoppedException, IllegalArgumentException {
				return null;
			}

			@Override
			public <T> T invoke(ForkJoinTask<T> forkJoinTask) throws ExecutionException, ExecutionStoppedException,
					IllegalArgumentException {
				return null;
			}

			@Override
			public <T> List<T> invokeAll(List<ForkJoinTask<T>> list) throws ExecutionException,
					ExecutionStoppedException, IllegalArgumentException {
				return null;
			}

			@Override
			public void checkStatus() throws ExecutionStoppedException {

			}

			@Override
			public int getParallelism() {
				return 0;
			}
		});
		assertTrue(context.isActive());
		context.requireActive();
	}

	@Test
	public void testParallelism() throws ExecutionException, InterruptedException {
		Context context = ContextAdapter.adapt(studioContext);
		assertEquals(4,context.getParallelism());
	}

	@Test
	public void testCallables() throws ExecutionException {
		Context ctx = ContextAdapter.adapt(studioContext);
		List<Callable<String>> callables = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			String val = "" + i;
			callables.add(() -> val);
		}
		List<String> result = ctx.call(callables);
		List<String> expected = IntStream.range(0, 20).mapToObj(i -> "" + i).collect(Collectors.toList());
		TestCase.assertEquals(expected, result);
	}

	@Test(expected = NullPointerException.class)
	public void testNullCallables() throws ExecutionException {
		Context ctx = ContextAdapter.adapt(studioContext);
		ctx.call(null);
	}

	@Test(expected = NullPointerException.class)
	public void testNullContainingCallables() throws ExecutionException {
		Context ctx = ContextAdapter.adapt(studioContext);
		ctx.call(Arrays.asList(() -> "", null));
	}

	@Test(expected = ExecutionAbortedException.class)
	public void testExecutionAborted() throws ExecutionException {
		Context ctx = ContextAdapter.adapt(studioContext);
		ctx.call(Arrays.asList(() -> "", () -> {
			throw new ExecutionAbortedException("bla");
		}));
	}

	@Test(expected = ExecutionAbortedException.class)
	public void testProcessStopped() throws ExecutionException {
		Context ctx = ContextAdapter.adapt(studioContext);
		ctx.call(Arrays.asList(() -> "", () -> {
			throw new ProcessStoppedRuntimeException();
		}));
	}

	@Test(expected = ExecutionException.class)
	public void testRuntimeExceptions() throws ExecutionException {
		Context ctx = ContextAdapter.adapt(studioContext);
		ctx.call(Arrays.asList(() -> "", () -> {
			throw new RuntimeException();
		}));
	}

}
