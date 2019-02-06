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
package com.rapidminer.gui.tools;

import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;

import java.util.logging.Level;


/**
 * A queue of runnables in which only execution of the last is relevant. Older runnables become
 * obsolete as soon as a new is inserted.
 * 
 * @author Simon Fischer
 * 
 */
public class UpdateQueue extends Thread {

	private Runnable pending;

	public UpdateQueue(String name) {
		super("UpdateQueue-" + name);
		setDaemon(true);
	}

	private Object lock = new Object();
	private boolean shutdownRequested = false;

	/**
	 * Queues runnable for execution. Will be executed as soon as the current runnable has
	 * terminated. If there is no current executable, will be executed immediately (in the thread
	 * created by this instance). If this method is called again before the current runnable is
	 * executed, runnable will be discarded in favor of the new.
	 */
	public void execute(Runnable runnable) {
		synchronized (lock) {
			pending = runnable;
			lock.notifyAll();
		}
	}

	/**
	 * Executes the given progress thread and waits for it, so only one will be enqueued at a time.
	 * (The calling thread will *not* wait, only the queue waits!
	 */
	public void executeBackgroundJob(final ProgressThread progressThread) {
		execute(new Runnable() {

			@Override
			public void run() {
				progressThread.startAndWait();
			}
		});
	}

	@Override
	public void run() {
		while (!shutdownRequested) {
			final Runnable target;
			synchronized (lock) {
				target = pending;
				pending = null;
			}
			if (target != null) {
				try {
					target.run();
				} catch (Exception e) {
					// LogService.getRoot().log(Level.WARNING,
					// "Error executing task in "+getName()+": "+e, e);
					LogService.getRoot().log(
							Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(),
									"com.rapidminer.gui.tools.UpdateQueue.error_executing_task", getName(), e), e);

				}
			}
			synchronized (lock) {
				if (pending == null) {
					try {
						lock.wait();
					} catch (InterruptedException e) {
					}
				}
			}
		}
	}

	public void shutdown() {
		synchronized (lock) {
			pending = null;
			shutdownRequested = true;
			lock.notifyAll();
		}
	}
}
