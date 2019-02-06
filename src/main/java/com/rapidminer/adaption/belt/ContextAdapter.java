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
package com.rapidminer.adaption.belt;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.execution.ExecutionAbortedException;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.core.concurrency.ExecutionStoppedException;


/**
 * Adapts a {@link ConcurrencyContext} to a belt {@link Context}.
 *
 * Please note that this class is not part of any public API and might be modified or removed in future releases without
 * prior warning.
 *
 * @author Gisa Meier
 * @since 9.0.0
 */
public final class ContextAdapter implements Context {

	private final ConcurrencyContext studioContext;

	private ContextAdapter(ConcurrencyContext studioContext) {
		this.studioContext = studioContext;
	}

	@Override
	public boolean isActive() {
		try {
			studioContext.checkStatus();
			return true;
		} catch (ExecutionStoppedException e) {
			return false;
		}
	}

	@Override
	public void requireActive() {
		try {
			studioContext.checkStatus();
		} catch (ExecutionStoppedException e) {
			throw new ExecutionAbortedException("Execution was aborted");
		}
	}

	@Override
	public int getParallelism() {
		return studioContext.getParallelism();
	}

	@Override
	public <T> List<T> call(List<Callable<T>> list) throws ExecutionException {
		// ensure that NPE is thrown instead of IllegalArgumentException by ConcurrencyContext#call
		if (list == null) {
			throw new NullPointerException("callables must not be null");
		}
		// check for null tasks
		for (Callable<T> callable : list) {
			if (callable == null) {
				throw new NullPointerException("callables must not contain null");
			}
		}
		try {
			return studioContext.call(list);
		} catch (ExecutionStoppedException e) {
			throw new ExecutionAbortedException("Execution was aborted", e);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof ExecutionAbortedException) {
				throw (ExecutionAbortedException) e.getCause();
			}
			throw e;
		}
	}

	/**
	 * Creates a belt {@link Context} from the given studio context.
	 * 
	 * @param studioContext
	 *            a {@link ConcurrencyContext}
	 * @return a belt context
	 */
	public static Context adapt(ConcurrencyContext studioContext) {
		return new ContextAdapter(studioContext);
	}
}
