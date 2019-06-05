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
package com.rapidminer.connection.util;

import java.util.function.BooleanSupplier;

import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ProgressThreadStoppedException;
import com.rapidminer.tools.ProgressListener;


/**
 * Wrapper for test subjects that should be tested in a {@link TestExecutionContext}.
 *
 * @param <T>
 * 		the type of the test subject
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class TestExecutionContextImpl<T> implements TestExecutionContext<T> {

	/** The test subject */
	private final T subject;
	/** The test progress */
	private final ProgressListener progressListener;
	/** Returns {@code true} if test should be stopped */
	private final BooleanSupplier isCancelled;

	/**
	 * Creates a new test execution context without a progress thread.
	 *
	 * @param subject
	 * 		the test subject
	 */
	public TestExecutionContextImpl(T subject) {
		this(subject, null);
	}

	/**
	 * Creates a new test execution context with the given progress thread.
	 *
	 * @param subject
	 * 		the test subject
	 * @param progressThread
	 * 		the progress thread in which the test is executed
	 */
	public TestExecutionContextImpl(T subject, ProgressThread progressThread) {
		this.subject = subject;
		if (progressThread != null) {
			this.progressListener = progressThread.getProgressListener();
			this.isCancelled = progressThread::isCancelled;
		} else {
			this.progressListener = new ProgressAdapter() {
				// does nothing
			};
			this.isCancelled = () -> false;
		}
	}

	@Override
	public T getSubject() {
		checkCancelled();
		return subject;
	}

	@Override
	public void checkCancelled() {
		if (isCancelled.getAsBoolean()) {
			throw new ProgressThreadStoppedException();
		}
	}

	@Override
	public ProgressListener getProgressListener() {
		checkCancelled();
		return progressListener;
	}
}
