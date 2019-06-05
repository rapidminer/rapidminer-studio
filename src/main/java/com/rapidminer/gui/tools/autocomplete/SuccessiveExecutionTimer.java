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

import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.util.concurrent.Future;
import javax.swing.Timer;

import com.rapidminer.gui.tools.MultiSwingWorker;


/**
 * Wrapper class for {@link Timer}, that ensures successive execution of given Runnable on the SwingWorker ThreadPool.
 *
 * @since 8.1.2
 * @author Jonas Wilms-Pfau
 */
class SuccessiveExecutionTimer {

	/** Reference to the last execution */
	private volatile WeakReference<Future<Void>> lastExecutionReference = new WeakReference<>(null);

	/** Synchronization lock for the execution */
	private final Object executionLock = new Object();

	/** The runnable to execute */
	private final Runnable runnable;

	/** Timer used to trigger the execution */
	private final Timer timer;

	/**
	 * Creates a new SuccessiveExecutionTimer
	 *
	 * @param delayMs
	 * 		milliseconds for the initial and between-event delay
	 * @param runnable
	 * 		the runnable to run
	 */
	SuccessiveExecutionTimer(int delayMs, Runnable runnable) {
		if (runnable == null) {
			throw new IllegalArgumentException("runnable must not be null");
		}
		this.runnable = runnable;
		timer = new Timer(delayMs, this::execute);
		timer.setRepeats(false);
	}

	/**
	 * Restarts the timer
	 *
	 * @see Timer#restart
	 */
	void restart() {
		timer.restart();
	}

	/**
	 * Stops already scheduled, but not started executions
	 *
	 * @see Timer#stop
	 */
	void stop() {
		timer.stop();
	}

	/**
	 * Executes the given Runnable immediately on the callers thread.
	 *
	 * @see Runnable#run
	 */
	void run() {
		runnable.run();
	}

	/**
	 * Stops the Timer and runs the Runnable immediately on the callers thread.
	 *
	 * @see #stop
	 * @see #run
	 */
	void runNow(){
		stop();
		run();
	}

	/**
	 * Executes the runnable on the SwingWorker thread pool.
	 * <p>
	 * If the runnable is already executing at the moment it will restart the timer.
	 * </p>
	 *
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	private void execute(ActionEvent ae) {
		//Do not stack endless waiting threads
		Future<Void> lastExecution = lastExecutionReference.get();
		if (lastExecution != null && !lastExecution.isDone()) {
			restart();
		} else {
			MultiSwingWorker<Void, Void> newExecution = new MultiSwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() {
					synchronized (executionLock) {
						runnable.run();
					}
					return null;
				}
			};
			lastExecutionReference = new WeakReference<>(newExecution);
			newExecution.start();
		}
	}
}

