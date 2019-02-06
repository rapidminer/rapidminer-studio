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
package com.rapidminer.gui.tools.autocomplete;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;


/**
 * Test for SuccessiveExecutionTimer
 *
 * @author Jonas Wilms-Pfau
 * @since 8.1.2
 */
public class SuccessiveExecutionTimerTest {

	private AtomicInteger executionCount = new AtomicInteger(0);
	private int timeout = 10;
	private int triggerCount = 0;
	Runnable increaseCounter = () -> executionCount.incrementAndGet();
	private SuccessiveExecutionTimer timer = new SuccessiveExecutionTimer(timeout, increaseCounter);

	@Test
	public void shouldNotStartImmediately() throws InterruptedException {
		Thread.sleep(timeout * 5);
		Assert.assertEquals(0, executionCount.get());
	}

	@Test
	public void shouldResetCounter() throws InterruptedException {
		restartTimerAndSleep(1);
		timer.restart();
		timer.restart();
		timer.runNow();
		//for the run now
		triggerCount += 1;
		Thread.sleep(timeout * 5);
		Assert.assertEquals(triggerCount, executionCount.get());
	}

	@Test
	public void canceledTimerShouldNotExecute() throws InterruptedException {
		restartTimerAndSleep(1);
		timer.stop();
		Thread.sleep(timeout * 5);
		Assert.assertEquals(triggerCount, executionCount.get());
	}

	private void restartTimerAndSleep(long milis) throws InterruptedException {
		timer.restart();
		long before = System.currentTimeMillis();
		Thread.sleep(milis);
		if (System.currentTimeMillis() - before >= timeout) {
			triggerCount += 1;
		}
	}
}
