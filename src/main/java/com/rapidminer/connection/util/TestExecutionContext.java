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

import com.rapidminer.gui.tools.ProgressThreadStoppedException;
import com.rapidminer.tools.ProgressListener;


/**
 * This context is passed when an object should be tested. This is a generic interface not tied to a particular test
 * case.
 *
 * <ul>
 * <li>Call {@link #checkCancelled()} between expensive operations</li>
 * <li>Update the {@link #getProgressListener() test progress} if possible</li>
 * </ul>
 *
 * @param <T>
 * 		the type of the test subject
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public interface TestExecutionContext<T> {

	/**
	 * The test subject that should be tested
	 *
	 * @return the test subject
	 * @throws ProgressThreadStoppedException in case the test was cancelled
	 */
	T getSubject();

	/**
	 * Checks if the user has requested to cancel the test,
	 * if yes throws a {@link ProgressThreadStoppedException} which doesn't have to be handled.
	 *
	 * @throws ProgressThreadStoppedException in case the test was cancelled
	 */
	void checkCancelled() throws ProgressThreadStoppedException;

	/**
	 * Returns a Progress Listener that can be updated with the current test progress
     *
	 * <p>Every method of the returned {@link ProgressListener} might throw a {@link ProgressThreadStoppedException}.</p>
	 *
	 * @return the updatable progress listener, never {@code null}
	 * @throws ProgressThreadStoppedException in case the test was cancelled
	 */
	ProgressListener getProgressListener();
}
