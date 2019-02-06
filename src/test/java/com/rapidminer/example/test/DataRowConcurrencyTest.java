/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
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
package com.rapidminer.example.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.DataRowReader;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.test_utils.RapidAssert;
import com.rapidminer.tools.Ontology;


/**
 * Tests concurrent modifications of data rows.
 *
 * @author Marcel Michel
 */
public class DataRowConcurrencyTest {

	/** needs to be greater than the number of attributes (in this case 4) */
	private static final int THREAD_COUNT = 20;

	/** the number of used examples */
	private static final int EXAMPLE_COUNT = 10_000;

	/** the amount of test runs */
	private static final int LOOP_COUNT = 20;

	private CountDownLatch startSignal;

	private ExecutorService executorService;

	@Before
	public void setup() {
		RapidMiner.initAsserters();
		startSignal = new CountDownLatch(1);
		executorService = Executors.newFixedThreadPool(THREAD_COUNT);
	}

	@After
	public void tearDown() throws InterruptedException {
		executorService.shutdown();
		executorService.awaitTermination(5, TimeUnit.SECONDS);
	}

	@Test
	public void concurrentWriteValueTest() {
		int count = LOOP_COUNT;
		while (count > 0) {
			for (int i = DataRowFactory.FIRST_TYPE_INDEX; i <= DataRowFactory.LAST_TYPE_INDEX; i++) {
				// make sure the random seed is different to create different example sets
				ExampleSet sourceSet = createMemoryExampleTable(EXAMPLE_COUNT, i, 1).createExampleSet();
				ExampleSet targetSet = createMemoryExampleTable(EXAMPLE_COUNT, i, 2).createExampleSet();

				Attribute[] attributes = sourceSet.getExampleTable().getAttributes();
				List<Future<Void>> tasks = new ArrayList<>(THREAD_COUNT);
				for (int j = 0; j < THREAD_COUNT; j++) {
					Callable<Void> task = prepareCopyValueTask(sourceSet, targetSet, attributes[j % attributes.length]);
					tasks.add(executorService.submit(task));
				}

				// start computation
				startSignal.countDown();

				for (Future<Void> task : tasks) {
					try {
						// wait for computation
						task.get();
					} catch (InterruptedException | ExecutionException e) {
						Assert.fail(e.getMessage());
					}
				}
				// finally compare the example sets, at this point they should be equal
				RapidAssert.assertEquals("ExampleSets are not equal", sourceSet, targetSet);
				count--;
			}
		}
	}

	@Test
	public void concurrentAddAttributeTest() {
		int count = LOOP_COUNT;
		while (count > 0) {
			for (int i = DataRowFactory.FIRST_TYPE_INDEX; i <= DataRowFactory.LAST_TYPE_INDEX; i++) {
				// make sure the random seed is different to create different example sets
				MemoryExampleTable expectedTable = createMemoryExampleTable(EXAMPLE_COUNT, i, 1);
				MemoryExampleTable actualTable = createMemoryExampleTable(EXAMPLE_COUNT, i, 2);

				Attribute[] originalAttributes = expectedTable.getAttributes();

				int newAttributeSize = THREAD_COUNT - 1;
				List<Attribute> newAttributes = new ArrayList<>(newAttributeSize);
				for (int j = 0; j < newAttributeSize; j++) {
					newAttributes.add(AttributeFactory.createAttribute("real-" + j, Ontology.REAL));
				}
				expectedTable.addAttributes(newAttributes);

				List<Future<Void>> tasks = new ArrayList<>(THREAD_COUNT);
				for (int j = 0; j < newAttributeSize; j++) {
					Callable<Void> task = prepareAddAttributeTask(actualTable, newAttributes.get(j));
					tasks.add(executorService.submit(task));
				}
				// only one thread is modifying the data values
				tasks.add(executorService.submit(prepareCopyValueTask(expectedTable, actualTable, originalAttributes)));

				// start computation
				startSignal.countDown();

				for (Future<Void> task : tasks) {
					try {
						// wait for computation
						task.get();
					} catch (InterruptedException | ExecutionException e) {
						Assert.fail(e.getMessage());
					}
				}
				for (Attribute a : newAttributes) {
					expectedTable.removeAttribute(a);
					actualTable.removeAttribute(a);
				}

				// finally compare the example sets, at this point they should be equal
				RapidAssert.assertEquals("ExampleSets are not equal", expectedTable.createExampleSet(),
						actualTable.createExampleSet());
				count--;
			}
		}
	}

	/**
	 * Creates a {@link Callable} which copies the values of the selectedAttribute from the source
	 * to the target {@link ExampleSet}.
	 */
	private Callable<Void> prepareCopyValueTask(ExampleSet source, ExampleSet target, Attribute selectedAttribute) {
		return new Callable<Void>() {

			@Override
			public Void call() {
				try {
					startSignal.await();
					Iterator<Example> sourceIterator = source.iterator();
					Iterator<Example> targetIterator = target.iterator();
					Example sourceRow = sourceIterator.next();
					Example targetRow = targetIterator.next();
					while (sourceRow != null && targetRow != null) {
						targetRow.setValue(selectedAttribute, sourceRow.getValue(selectedAttribute));
						sourceRow = sourceIterator.next();
						targetRow = targetIterator.next();
					}
				} catch (InterruptedException e) {
					Assert.fail(Thread.currentThread().getName() + " " + e.getMessage());
				}
				return null;
			}
		};
	}

	/**
	 * Creates a {@link Callable} which copies the values from the source to the target
	 * {@link ExampleTable} by using the {@link DataRowReader}, only the values of the defined
	 * {@link Attribute}s will be copied.
	 */
	private Callable<Void> prepareCopyValueTask(ExampleTable source, ExampleTable target, Attribute[] attributes) {
		return new Callable<Void>() {

			@Override
			public Void call() {
				try {
					startSignal.await();
					DataRowReader sourceReader = source.getDataRowReader();
					DataRowReader targetReader = target.getDataRowReader();
					DataRow sourceRow = null;
					DataRow targetRow = null;
					while (sourceReader.hasNext() && targetReader.hasNext()) {
						sourceRow = sourceReader.next();
						targetRow = targetReader.next();
						for (Attribute a : attributes) {
							targetRow.set(a, sourceRow.get(a));
						}
					}
				} catch (InterruptedException e) {
					Assert.fail(Thread.currentThread().getName() + " " + e.getMessage());
				}
				return null;
			}
		};
	}

	/**
	 * Creates a {@link Callable} which adds the {@link Attribute} to the target
	 * {@link ExampleTable}.
	 */
	private Callable<Void> prepareAddAttributeTask(ExampleTable target, Attribute attribute) {
		return new Callable<Void>() {

			@Override
			public Void call() {
				try {
					startSignal.await();
					target.addAttribute(attribute);
				} catch (InterruptedException e) {
					Assert.fail(Thread.currentThread().getName() + " " + e.getMessage());
				}
				return null;
			}
		};
	}

	/**
	 * Creates a {@link MemoryExampleTable} with random values.
	 *
	 * @param size
	 *            the number of rows
	 * @param dataManagement
	 *            the data management strategy (see {@link DataRowFactory} for more information)
	 * @return the created example set as {@link MemoryExampleTable}
	 */
	private static MemoryExampleTable createMemoryExampleTable(int size, int dataManagement, int seed) {
		Attribute[] attributes = ExampleTestTools.createFourAttributes();
		MemoryExampleTable exampleTable = new MemoryExampleTable(attributes);

		DataRowFactory rowFactory = new DataRowFactory(dataManagement, '.');
		Random random = new Random(seed);
		for (int i = 0; i < size; i++) {
			DataRow row = rowFactory.create(attributes.length);
			for (int j = 0; j < attributes.length; j++) {
				if (attributes[j].isNominal()) {
					row.set(attributes[j], random.nextInt(attributes[j].getMapping().getValues().size()));
				} else if (attributes[j].getValueType() == Ontology.INTEGER) {
					row.set(attributes[j], random.nextInt(200) - 100);
				} else {
					row.set(attributes[j], 20.0 * random.nextDouble() - 10.0);
				}
			}
			exampleTable.addDataRow(row);
		}
		return exampleTable;
	}
}
