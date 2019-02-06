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
package com.rapidminer.gui;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.rapidminer.Process;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ProgressThreadStateListener;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.UserData;
import com.rapidminer.operator.io.AbstractReader;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.documentation.OperatorDocumentation;


/**
 * Test class for {@link MetaDataUpdateQueue}. Tests the new functionality of generating {@link MetaData}
 * in the background for {@link AbstractReader AbstractReaders}.
 *
 * @author Jan Czogalla
 * @since 9.2.0
 */
public class MetaDataUpdateQueueTest {

	private static final int TIMES_TO_CHECK_STOP_CONDITION = 10;
	private static final int WAIT_TIME_BETWEEN_CHECKS = 50;
	private static final int DELAY_FOR_MD_GENERATION = 200;

	/** Test meta data generation in the background */
	@Test
	public void testRevalidationMechanism() {

		AtomicInteger validateCount = new AtomicInteger();
		AtomicInteger validateSpawned = new AtomicInteger();
		AtomicInteger revalidateSpawned = new AtomicInteger();

		// listener to monitor porgress threads; counts finished validation threads
		ProgressThreadStateListener counter = new ProgressThreadStateListener() {
			@Override
			public synchronized void progressThreadStarted(ProgressThread pg) {
				log("Starting", pg);
			}

			@Override
			public synchronized void progressThreadQueued(ProgressThread pg) {
				log("Queuing", pg);
				String id = pg.getID();
				if (id.equals(MetaDataUpdateQueue.REVALIDATE_PROCESS_KEY)) {
					revalidateSpawned.getAndIncrement();
				}
			}

			@Override
			public synchronized void progressThreadCancelled(ProgressThread pg) {
				log("Canceling", pg);
			}

			@Override
			public synchronized void progressThreadFinished(ProgressThread pg) {
				log("Finishing", pg);
				String id = pg.getID();
				if (id.equals(MetaDataUpdateQueue.VALIDATE_PROCESS_KEY)) {
					validateSpawned.getAndDecrement();
					validateCount.getAndIncrement();
				}
				if (id.equals(MetaDataUpdateQueue.REVALIDATE_PROCESS_KEY)) {
					revalidateSpawned.getAndDecrement();
				}
			}

			private void log(String status, ProgressThread pg) {
				LogService.getRoot().info(status + " \"" + pg + "\"");
			}
		};
		ProgressThread.addProgressThreadStateListener(counter);

		// dummy docu/desc for ProcessRootOperator
		OperatorDocumentation docu = mock(OperatorDocumentation.class);
		OperatorDescription desc = mock(OperatorDescription.class);
		doReturn(docu).when(desc).getOperatorDocumentation();

		// preparation for calling new Process()
		doReturn("root").when(docu).getShortName();
		doReturn("process").when(desc).getKey();
		doReturn(true).when(desc).isIconDefined();
		doReturn("").when(desc).getGroup();
		doReturn(ProcessRootOperator.class).when(desc).getOperatorClass();
		try {
			doReturn(new ProcessRootOperator(desc)).when(desc).createOperatorInstance();
		} catch (OperatorCreationException e) {
			e.printStackTrace();
		}
		try {
			OperatorService.registerOperator(desc, null);
		} catch (OperatorCreationException e) {
			e.printStackTrace();
		}
		Process p = new Process();
		ExecutionUnit subprocess = p.getRootOperator().getSubprocess(0);

		MetaDataUpdateQueue[] queue = {createUpdateQueue(validateSpawned, p)};
		queue[0].start();

		// runnable that waits for validations to run out
		Runnable waitForFinish = () -> {
			for (int i = 0; i < TIMES_TO_CHECK_STOP_CONDITION; i++) {
				if (validateSpawned.get() == 0 && revalidateSpawned.get() == 0) {
					return;
				}
				try {
					Thread.sleep(WAIT_TIME_BETWEEN_CHECKS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			// queue is stuck in endless loop; reset it; the assertion should kill the test afterwards
			queue[0].shutdown();
			queue[0] = createUpdateQueue(validateSpawned, p);
			queue[0].start();
			validateSpawned.set(0);
			revalidateSpawned.set(0);
		};

		// test with one non-caching operator
		LogService.getRoot().info("Starting short running test");
		AbstractReader<IOObject> nonCaching = createDummyReader("Non Caching", false, 0);
		subprocess.addOperator(nonCaching);
		queue[0].validate(p, true);
		waitForFinish.run();
		Assert.assertEquals("Incorrect validation runs", 1, validateCount.get());
		Assert.assertEquals("Incorrect md generation invocations (non)", 1, ((CountUserData) nonCaching.getUserData("mdCount")).counter.get());
		LogService.getRoot().info("Finishing short running test.\n--------------------");

		// test with one non-caching and one fast-caching operator
		LogService.getRoot().info("Starting fast running test with one generation.");
		validateCount.set(0);
		AbstractReader<IOObject> fastCaching = createDummyReader("Fast Caching", true, 0);
		subprocess.addOperator(fastCaching);
		// make all operator caches dirty again
		subprocess.getAllInnerOperators().forEach(op -> op.setParameter("test", "test"));
		queue[0].validate(p, true);
		waitForFinish.run();
		Assert.assertEquals("Incorrect validation runs", 2, validateCount.get());
		Assert.assertEquals("Incorrect md generation invocations (non)", 3, ((CountUserData) nonCaching.getUserData("mdCount")).counter.get());
		Assert.assertEquals("Incorrect md generation invocations (fast)", 1, ((CountUserData) fastCaching.getUserData("mdCount")).counter.get());
		LogService.getRoot().info("Finishing fast running test with one generation.\n--------------------");

		// test with one operator each (non, fast, delayed)
		LogService.getRoot().info("Starting long running test with two generations.");
		validateCount.set(0);
		AbstractReader<IOObject> slowCaching = createDummyReader("Slow Caching", true, DELAY_FOR_MD_GENERATION);
		subprocess.addOperator(slowCaching);
		// make all operator caches dirty again
		subprocess.getAllInnerOperators().forEach(op -> op.setParameter("test", "test"));
		queue[0].validate(p, true);
		waitForFinish.run();
		Assert.assertEquals("Incorrect validation runs", 2, validateCount.get());
		Assert.assertEquals("Incorrect md generation invocations (non)", 5, ((CountUserData) nonCaching.getUserData("mdCount")).counter.get());
		Assert.assertEquals("Incorrect md generation invocations (fast)", 2, ((CountUserData) fastCaching.getUserData("mdCount")).counter.get());
		Assert.assertEquals("Incorrect md generation invocations (slow)", 1, ((CountUserData) slowCaching.getUserData("mdCount")).counter.get());
		LogService.getRoot().info("Finishing long running test with two generations.\n--------------------");

		ProgressThread.removeProgressThreadStateListener(counter);
		queue[0].shutdown();
	}

	/** Creates a {@link MetaDataUpdateQueue} that increases a counter for each validation call */
	private MetaDataUpdateQueue createUpdateQueue(AtomicInteger validateSpawned, Process p) {
		MainFrame mf = mock(MainFrame.class);
		when(mf.getProcess()).thenReturn(p);
		return new MetaDataUpdateQueue(mf){
				@Override
				public void validate(Process process, boolean force) {
					validateSpawned.getAndIncrement();
					super.validate(process, force);
				}
			};
	}

	/** Creates a dummy {@link AbstractReader} that can be non-caching, caching, or delayed caching */
	private static AbstractReader<IOObject> createDummyReader(String shortName, boolean isCaching, int delay) {
		OperatorDocumentation docu = mock(OperatorDocumentation.class);
		doReturn(shortName).when(docu).getShortName();
		OperatorDescription desc = mock(OperatorDescription.class);
		doReturn(docu).when(desc).getOperatorDocumentation();
		AbstractReader<IOObject> reader;
		AtomicInteger mdCount = new AtomicInteger();
		if (!isCaching) {
			reader = new AbstractReader<IOObject>(desc, IOObject.class) {

				@Override
				public IOObject read() throws OperatorException {return null;}

				@Override
				public MetaData getGeneratedMetaData() throws OperatorException {
					mdCount.getAndIncrement();
					return super.getGeneratedMetaData();
				}
			};
		} else if (delay > 0) {
			reader = new AbstractReader<IOObject>(desc, IOObject.class) {

				@Override
				public MetaData getGeneratedMetaData() throws OperatorException {
					mdCount.getAndIncrement();
					try {
						Thread.sleep(delay);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					return super.getGeneratedMetaData();
				}

				@Override
				protected boolean isMetaDataCacheable() {return true;}

				@Override
				public IOObject read() throws OperatorException {return null;}
			};
		} else {
			reader = new AbstractReader<IOObject>(desc, IOObject.class) {

				@Override
				protected boolean isMetaDataCacheable() {
					return true;
				}

				@Override
				public IOObject read() throws OperatorException {
					return null;
				}

				@Override
				public MetaData getGeneratedMetaData() throws OperatorException {
					mdCount.getAndIncrement();
					return super.getGeneratedMetaData();
				}
			};
		}
		reader.setUserData("mdCount", new CountUserData(mdCount));
		return reader;
	}

	/** Simple {@link UserData} container for an {@link AtomicInteger} counter */
	private static class CountUserData implements UserData<Object> {

		private AtomicInteger counter;

		CountUserData(AtomicInteger counter) {
			this.counter = counter;
		}

		@Override
		public CountUserData copyUserData(Object newParent) {
			return this;
		}
	}
}