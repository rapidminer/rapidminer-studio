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
package com.rapidminer.example.set;

import static com.rapidminer.belt.column.ColumnTypes.NOMINAL;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.rapidminer.adaption.belt.ContextAdapter;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.core.concurrency.ExecutionStoppedException;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.UserError;
import com.rapidminer.studio.internal.ProcessStoppedRuntimeException;
import com.rapidminer.test.asserter.AsserterFactoryRapidMiner;
import com.rapidminer.test_utils.RapidAssert;


/**
 * Tests the {@link TableSplitter} by comparing it to {@link SplittedExampleSet}.
 *
 * @author Gisa Meier
 */
@RunWith(Enclosed.class)
public class TableSplitterTest {

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
			for (Future<T> future : futures) {
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
		}

	};

	@RunWith(Parameterized.class)
	public static class Comparison {

		@Parameterized.Parameter
		public String name;

		@Parameterized.Parameter(value = 1)
		public TableSplitter splitter;

		@Parameterized.Parameter(value = 2)
		public SplittedExampleSet splittedExampleSet;

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> parameters() throws UserError {
			List<Object[]> parameters = new ArrayList<>();

			Table table = Builders.newTableBuilder(6)
					.addReal("real", i -> Math.random())
					.addInt("int", i -> i)
					.build(ContextAdapter.adapt(studioContext));
			ExampleSet exampleSet = BeltConverter.convert(new IOTable(table), studioContext);

			parameters.add(new Object[]{"simple", new TableSplitter(table, new int[]{1, 0, 0, 2, 0, 2}, 3),
					new SplittedExampleSet(exampleSet, new Partition(new int[]{1, 0, 0, 2, 0, 2}, 3))});

			table = Builders.newTableBuilder(20)
					.addReal("real", i -> Math.random())
					.addInt("int", i -> i)
					.build(ContextAdapter.adapt(studioContext));
			exampleSet = BeltConverter.convert(new IOTable(table), studioContext);
			parameters.add(new Object[]{"splitByReal", TableSplitter.splitByAttribute(table, "real", 0.5),
					SplittedExampleSet.splitByAttribute(exampleSet, exampleSet.getAttributes().get("real"), 0.5)});

			table = Builders.newTableBuilder(25)
					.addNominal("nominal", i -> Math.random() > 0.3 ? (Math.random() > 0.5 ? "blue" : "green") : "red")
					.addInt("int", i -> i)
					.addMetaData("nominal", ColumnRole.LABEL)
					.build(ContextAdapter.adapt(studioContext));
			exampleSet = BeltConverter.convert(new IOTable(table), studioContext);
			parameters.add(new Object[]{"splitByNominal", TableSplitter.splitByAttribute(table, "nominal"),
					SplittedExampleSet.splitByAttribute(exampleSet, exampleSet.getAttributes().get("nominal"))});

			for (int i = 0; i < SplittedExampleSet.SAMPLING_NAMES.length; i++) {
				parameters.add(new Object[]{"ratio-" + SplittedExampleSet.SAMPLING_NAMES[i],
						new TableSplitter(table, 0.7, i, true, 42),
						new SplittedExampleSet(exampleSet, 0.7, i, true, 42, false)});
			}

			for (int i = 0; i < SplittedExampleSet.SAMPLING_NAMES.length; i++) {
				parameters.add(new Object[]{"ratios-" + SplittedExampleSet.SAMPLING_NAMES[i],
						new TableSplitter(table, new double[]{0.2, 0.4, 0.1, 0.3}, i, true, 42),
						new SplittedExampleSet(exampleSet, new double[]{0.2, 0.4, 0.1, 0.3}, i, true, 42, false)});
			}

			parameters.add(new Object[]{"subsetsNumber-" + SplittedExampleSet.SAMPLING_NAMES[SplittedExampleSet.STRATIFIED_SAMPLING],
					new TableSplitter(table, 3, SplittedExampleSet.STRATIFIED_SAMPLING, true, 42),
					new SplittedExampleSet(exampleSet, 3, SplittedExampleSet.STRATIFIED_SAMPLING, true, 42, false)});

			table = Builders.newTableBuilder(25)
					.addReal("real", i -> Math.random())
					.addInt("int", i -> i)
					.addMetaData("real", ColumnRole.LABEL)
					.build(ContextAdapter.adapt(studioContext));
			exampleSet = BeltConverter.convert(new IOTable(table), studioContext);
			for (int i : new int[]{SplittedExampleSet.LINEAR_SAMPLING, SplittedExampleSet.SHUFFLED_SAMPLING,
					SplittedExampleSet.AUTOMATIC}) {
				parameters.add(new Object[]{"subsetsNumber-" + SplittedExampleSet.SAMPLING_NAMES[i],
						new TableSplitter(table, 3, i, true, 42),
						new SplittedExampleSet(exampleSet, 3, i, true, 42, false)});
			}
			return parameters;
		}

		@BeforeClass
		public static void setup() {
			RapidAssert.ASSERTER_REGISTRY.registerAllAsserters(new AsserterFactoryRapidMiner());
		}


		@Test
		public void selectSingleSubset() {
			Table table = splitter.selectSingleSubset(0, ContextAdapter.adapt(studioContext));
			ExampleSet fromTable = BeltConverter.convert(new IOTable(table), studioContext);
			splittedExampleSet.clearSelection();
			splittedExampleSet.selectSingleSubset(0);
			RapidAssert.assertEquals(splittedExampleSet, fromTable);
		}

		@Test
		public void selectAllSubsetsBut() {
			Table table = splitter.selectAllSubsetsBut(0, ContextAdapter.adapt(studioContext));
			ExampleSet fromTable = BeltConverter.convert(new IOTable(table), studioContext);
			splittedExampleSet.clearSelection();
			splittedExampleSet.selectAllSubsetsBut(0);
			RapidAssert.assertEquals(splittedExampleSet, fromTable);
		}

		@Test
		public void selectAllSubsets() {
			Table table = splitter.selectAllSubsets();
			ExampleSet fromTable = BeltConverter.convert(new IOTable(table), studioContext);
			splittedExampleSet.clearSelection();
			splittedExampleSet.selectAllSubsets();
			RapidAssert.assertEquals(splittedExampleSet, fromTable);
		}

		@Test
		public void getNumberOfSubsets() {
			assertEquals(splittedExampleSet.getNumberOfSubsets(), splitter.getNumberOfSubsets());
		}
	}

	public static class Exceptions {

		@Test(expected = UserError.class)
		public void stratifiedNoLabel() throws UserError {
			Table table = Builders.newTableBuilder(6)
					.addReal("real", i -> Math.random())
					.addInt("int", i -> i)
					.build(ContextAdapter.adapt(studioContext));
			new TableSplitter(table, 2, SplittedExampleSet.STRATIFIED_SAMPLING, true, 42);
		}

		@Test(expected = UserError.class)
		public void stratifiedNotNominal() throws UserError {
			Table table = Builders.newTableBuilder(6)
					.addReal("real", i -> Math.random())
					.addInt("int", i -> i)
					.addMetaData("real", ColumnRole.LABEL)
					.build(ContextAdapter.adapt(studioContext));
			new TableSplitter(table, 2, SplittedExampleSet.STRATIFIED_SAMPLING, true, 42);
		}

		@Test(expected = UnsupportedOperationException.class)
		public void splitByRealNotReadable() {
			Table table = Builders.newTableBuilder(6)
					.addDateTime("date", i -> Instant.EPOCH)
					.addInt("int", i -> i)
					.build(ContextAdapter.adapt(studioContext));
			TableSplitter.splitByAttribute(table, "date", 0.5);
		}

		@Test(expected = UnsupportedOperationException.class)
		public void splitByCategoryNotReadable() {
			Table table = Builders.newTableBuilder(6)
					.addDateTime("date", i -> Instant.EPOCH)
					.addInt("int", i -> i)
					.build(ContextAdapter.adapt(studioContext));
			TableSplitter.splitByAttribute(table, "date");
		}

		@Test(expected = IllegalArgumentException.class)
		public void stratifiedBuilderNotNominal() {
			new ColumnStratifiedPartitionBuilder(Buffers.realBuffer(10).toColumn(), true, 42);
		}

		@Test(expected = IllegalArgumentException.class)
		public void stratifiedBuilderNotSize() {
			new ColumnStratifiedPartitionBuilder(Buffers.<String>categoricalBuffer(5).toColumn(NOMINAL),
					true, 42).createPartition(new double[]{0.5, 0.5}, 1000);
		}
	}
}