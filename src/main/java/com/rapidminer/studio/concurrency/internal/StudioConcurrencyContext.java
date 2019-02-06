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
package com.rapidminer.studio.concurrency.internal;

import com.rapidminer.Process;
import com.rapidminer.RapidMiner;


/**
 * Simple {@link com.rapidminer.core.concurrency.ConcurrencyContext} to be used with a single {@link Process}.
 * <p>
 * The context does not implement the submission methods for {@link java.util.concurrent.ForkJoinTask}s.
 *
 * @author Gisa Schaefer, Michael Knopf
 * @since 6.2.0
 */
public class StudioConcurrencyContext extends AbstractConcurrencyContext {

	/**
	 * The pool used by this context
	 */
	private static final LazyPool FOREGROUND_POOL = new LazyPool(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_NUMBER_OF_THREADS);

	/**
	 * Creates a new {@link StudioConcurrencyContext} for the given {@link Process}.
	 * <p>
	 * The context assumes that only operators that belong to the corresponding process submit tasks to this context.
	 *
	 * @param process
	 * 		the corresponding process
	 */
	public StudioConcurrencyContext(Process process) {
		super(process, FOREGROUND_POOL);
	}
}
