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

import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;


/**
 * Thread that disables GUI components that interfere with process editing while running.
 * 
 * @author Simon Fischer
 * 
 */
public abstract class EditBlockingProgressThread extends ProgressThread {

	private Object lock = new Object();
	private boolean isComplete = false;
	private boolean mustReenable = false;
	private static AtomicInteger pendingThreads = new AtomicInteger(0);

	public EditBlockingProgressThread(String i18nKey) {
		super(i18nKey);
	}

	/** Implement this method rather than {@link #run()} to perform the actual task. */
	public abstract void execute();

	@Override
	public final void run() {
		try {
			execute();
		} finally {
			synchronized (lock) {
				lock.notify();
				isComplete = true;
				if ((pendingThreads.decrementAndGet() == 0) && mustReenable) {
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							SwingTools.setProcessEditorsEnabled(true);
						}
					});
				}
			}
		}
	}

	@Override
	public void start() {
		super.start();
		// We are on the EDT. BLock GUI a few milliseconds and see if we are complete
		synchronized (lock) {
			pendingThreads.incrementAndGet();
			try {
				lock.wait(200);
			} catch (InterruptedException e) {
			}
			if (!isComplete) {
				mustReenable = true;
				SwingTools.setProcessEditorsEnabled(false);
			}
		}
	}

	public static boolean isEditing() {
		return pendingThreads.get() > 0;
	}
}
