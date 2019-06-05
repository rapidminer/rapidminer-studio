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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.SwingWorker;


/**
 * A {@link SwingWorker} with an extra {@link #start()} method that allows to start the execution of the swing worker
 * immediately in a cached thread pool with unlimited threads. This is in contrast to the usual {@link #execute()}
 * method which allows only {@code 10} swing workers to be running at the same time and puts additional swing workers in
 * a waiting queue.
 *
 * @param <T>
 * 		the result type returned by this {@code SwingWorker's} {@code doInBackground} and {@code get} methods
 * @param <V>
 * 		the type used for carrying out intermediate results by this {@code SwingWorker's} {@code publish} and {@code
 * 		process} methods
 * @author Gisa Meier
 * @since 9.3
 */
public abstract class MultiSwingWorker<T, V> extends SwingWorker<T, V> {

	/**
	 * this is the {@link ExecutorService} from which all {@link MultiSwingWorker}s are started
	 */
	private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
		Thread thread = new Thread(r, "BackgroundWorker");
		thread.setDaemon(true);
		return thread;
	});

	/**
	 * Starts this {@code SwingWorker} for execution on a <i>background worker</i> thread. There are unlimited
	 * <i>background worker</i> threads available. This is in contrast to {@link #execute()}, which schedules the
	 * worker for execution for one of the {@code 10} <i>swing worker</i> threads.
	 *
	 * <p>
	 * Note: {@code SwingWorker} is only designed to be executed once. Starting a {@code SwingWorker} more than once
	 * will not result in invoking the {@code doInBackground} method twice.
	 */
	public final void start() {
		EXECUTOR.execute(this);
	}

}
