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
package com.rapidminer.adaption.belt;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;

import org.junit.Test;

import com.rapidminer.adaption.belt.ContextAdapter;
import com.rapidminer.belt.Context;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.core.concurrency.ExecutionStoppedException;


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
		public <T> List<T> call(List<Callable<T>> arg0) {
			return null;
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

	@Test
	public void testSubmit() throws ExecutionException, InterruptedException {
		Context context = ContextAdapter.adapt(studioContext);
		Callable<Double> callable = () -> 42.0;
		Future<Double> future = context.submit(callable);
		assertEquals(42, future.get(), 0);
	}


	@Test
	public void testActive() throws ExecutionException, InterruptedException {
		Context context = ContextAdapter.adapt(studioContext);
		assertEquals(false,context.isActive());
	}

	@Test
	public void testParallelism() throws ExecutionException, InterruptedException {
		Context context = ContextAdapter.adapt(studioContext);
		assertEquals(4,context.getParallelism());
	}

}
