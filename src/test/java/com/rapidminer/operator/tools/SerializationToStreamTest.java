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
package com.rapidminer.operator.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;

import com.rapidminer.RapidMiner;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.column.ColumnTypes;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.core.concurrency.ExecutionStoppedException;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.test_utils.RapidAssert;
import com.rapidminer.tools.Ontology;


/**
 * Tests the the {@link IOObjectSerializer} w.r.t. {@link ExampleSet}s and {@link IOTable}s which are both streamed via
 * {@link ExampleSetToStream}.
 *
 * @author Gisa Meier
 */
public class SerializationToStreamTest {

	private ExampleSet testSet;

	@Before
	public void setup() {
		Attribute numeric = AttributeFactory.createAttribute("numeric", Ontology.NUMERICAL);
		Attribute real = AttributeFactory.createAttribute("real", Ontology.REAL);
		Attribute integer = AttributeFactory.createAttribute("integer", Ontology.INTEGER);
		Attribute dateTime = AttributeFactory.createAttribute("date_time", Ontology.DATE_TIME);
		Attribute date = AttributeFactory.createAttribute("date", Ontology.DATE);
		Attribute time = AttributeFactory.createAttribute("time", Ontology.TIME);
		Attribute nominal = AttributeFactory.createAttribute("nominal", Ontology.NOMINAL);
		Attribute string = AttributeFactory.createAttribute("string", Ontology.STRING);
		Attribute polynominal = AttributeFactory.createAttribute("polynominal", Ontology.POLYNOMINAL);
		Attribute binominal = AttributeFactory.createAttribute("binominal", Ontology.BINOMINAL);
		Attribute path = AttributeFactory.createAttribute("path", Ontology.FILE_PATH);
		List<Attribute> attributes =
				Arrays.asList(numeric, real, integer, dateTime, date, time, nominal, string, polynominal, binominal,
						path);
		for (int i = 0; i < 5; i++) {
			nominal.getMapping().mapString("nominalValue" + i);
		}
		for (int i = 0; i < 14; i++) {
			string.getMapping().mapString("veryVeryLongStringValue" + i);
		}
		for (int i = 0; i < 6; i++) {
			polynominal.getMapping().mapString("polyValue" + i);
		}
		for (int i = 0; i < 2; i++) {
			binominal.getMapping().mapString("binominalValue" + i);
		}
		for (int i = 0; i < 3; i++) {
			path.getMapping().mapString("//folder/sufolder/subsubfolder/file" + i);
		}
		Random random = new Random();
		testSet = ExampleSets.from(attributes).withBlankSize(100)
				.withColumnFiller(numeric, i -> random.nextDouble() > 0.7 ? Double.NaN : random.nextDouble())
				.withColumnFiller(real, i -> random.nextDouble() > 0.7 ? Double.NaN : 42 + random.nextDouble())
				.withColumnFiller(integer,
						i -> random.nextDouble() > 0.7 ? Double.NaN : random.nextInt(100))
				.withColumnFiller(dateTime, i -> random.nextDouble() > 0.7 ? Double.NaN : (i % 3 == 0 ? -1 : 1)
						* 1515410698d + random.nextInt(1000))
				.withColumnFiller(date, i -> random.nextDouble() > 0.7 ? Double.NaN : (i % 3 == 0 ? -1 : 1) *
						230169600000d + random.nextInt(100) * 1000d * 60 * 60 * 24)
				.withColumnFiller(time, i -> random.nextDouble() > 0.7 ? Double.NaN :
						(i % 3 == 0 ? -1 : 1) * Math.floor(Math.random() * 60 * 60 * 24 * 1000))
				.withColumnFiller(nominal, i -> random.nextDouble() > 0.7 ? Double.NaN : random.nextInt(5))
				.withColumnFiller(string, i -> random.nextDouble() > 0.7 ? Double.NaN : random.nextInt(4))
				.withColumnFiller(polynominal, i -> random.nextDouble() > 0.7 ? Double.NaN : random.nextInt(6))
				.withColumnFiller(binominal, i -> random.nextDouble() > 0.7 ? Double.NaN : random.nextInt(2))
				.withColumnFiller(path, i -> random.nextDouble() > 0.7 ? Double.NaN : random.nextInt(3))
				.build();

		RapidMiner.initAsserters();
	}


	@Test
	public void testExampleSet() throws IOException {
		ExampleSet set = testSet;
		ExampleSet backAndForth = (ExampleSet) readFromArray(writeToArray(set));
		RapidAssert.assertEquals("ExampleSets are not equal", set, backAndForth);
	}

	@Test
	public void testTable() throws IOException {
		ExampleSet set = testSet;
		IOTable table = BeltConverter.convert(set, CONTEXT);

		ExampleSet backAndForth = (ExampleSet) readFromArray(writeToArray(table));
		RapidAssert.assertEquals("ExampleSets are not equal", set, backAndForth);
	}

	@Test
	public void testTableWithOutDatetime() throws IOException {
		ExampleSet set = (ExampleSet) testSet.clone();
		set.getAttributes().remove(set.getAttributes().get("date_time"));
		set.getAttributes().remove(set.getAttributes().get("time"));
		set.getAttributes().remove(set.getAttributes().get("date"));
		IOTable table = BeltConverter.convert(set, CONTEXT);

		ExampleSet backAndForth = (ExampleSet) readFromArray(writeToArray(table));
		RapidAssert.assertEquals("ExampleSets are not equal", set, backAndForth);
	}

	@Test(expected = InvalidObjectException.class)
	public void testTableWithCustomColumn() throws IOException {
		ColumnType<Double> customType = ColumnTypes.objectType("com.rapidminer.custom.double", Double.class, null);
		ColumnType<Integer> customType2 = ColumnTypes.categoricalType("com.rapidminer.custom.integer", Integer.class,
				null);
		Table table = Builders.newTableBuilder(11).addReal("real", i -> 3 * i / 5.0)
				.addObject("custom", i -> (double) i, customType).addInt("int", i -> 5 * i)
				.addCategorical("custom2", i -> i, customType2)
				.build(Belt.defaultContext());

		writeToArray(new IOTable(table));
	}

	private byte[] writeToArray(Object object) throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		IOObjectSerializer.getInstance().serialize(stream, object);
		return stream.toByteArray();
	}

	private Object readFromArray(byte[] array) throws IOException {
		ByteArrayInputStream stream = new ByteArrayInputStream(array);
		return IOObjectSerializer.getInstance().deserialize(stream);
	}


	private static final ConcurrencyContext CONTEXT = new ConcurrencyContext() {

		private ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

		@Override
		public <T> List<Future<T>> submit(List<Callable<T>> callables) throws IllegalArgumentException {
			List<Future<T>> futures = new ArrayList<>();
			for (Callable<T> callable : callables) {
				futures.add(pool.submit(callable));
			}
			return futures;
		}

		@Override
		public <T> List<T> call(List<Callable<T>> callables)
				throws ExecutionException, ExecutionStoppedException, IllegalArgumentException {
			List<Future<T>> futures = submit(callables);
			List<T> results = new ArrayList<>();
			for (Future<T> future : futures) {
				try {
					results.add(future.get());
				} catch (InterruptedException e) {
					throw new RuntimeException("must not happen");
				}
			}
			return results;
		}

		@Override
		public void run(List<Runnable> runnables)
				throws ExecutionException, ExecutionStoppedException, IllegalArgumentException {
		}

		@Override
		public <T> List<T> invokeAll(List<ForkJoinTask<T>> tasks)
				throws ExecutionException, ExecutionStoppedException, IllegalArgumentException {
			return null;
		}

		@Override
		public <T> T invoke(ForkJoinTask<T> task)
				throws ExecutionException, ExecutionStoppedException, IllegalArgumentException {
			return null;
		}

		@Override
		public int getParallelism() {
			return pool.getParallelism();
		}

		@Override
		public <T> List<T> collectResults(List<Future<T>> futures)
				throws ExecutionException, ExecutionStoppedException, IllegalArgumentException {
			return null;
		}

		@Override
		public void checkStatus() throws ExecutionStoppedException {
		}

	};

}