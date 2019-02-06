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

/**
 * This class can be used to access the {@link ConcurrencyExecutionService} which in turn can be
 * used to parallelize operator tasks and thus enable making use of all available CPU cores while
 * running an operator.
 * <p>
 * Note that this part of the API is only temporary and might be removed in future versions again.
 * </p>
 *
 * @author Marco Boeck
 * @since 7.4
 */
public enum ConcurrencyExecutionServiceProvider {

	/** the singleton provider instance */
	INSTANCE;

	/** this is the concurrency execution service instance */
	private ConcurrencyExecutionService service;

	/**
	 * Gets the concurrency execution service which can be used to parallelize operator tasks.
	 *
	 * @return the service or {@code null} if {@link #isInitialized()} returns {@code false}
	 */
	public ConcurrencyExecutionService getService() {
		return service;
	}

	/**
	 * Can be used to check if the concurrency execution service is initialized and can be used.
	 *
	 * @return {@code true} if the concurrency execution service is available; {@code false}
	 *         otherwise
	 */
	public boolean isInitialized() {
		return service != null;
	}

	/**
	 * <strong>Attention: </strong> NOT PART OF THE PUBLIC API!
	 *
	 * @param service
	 *            the {@link ConcurrencyExecutionService} to set. Must not be {@code null}!
	 * @throws IllegalStateException
	 *             if the concurrency execution service has already been set
	 */
	public void setConcurrencyExecutionService(ConcurrencyExecutionService service) {
		if (this.service != null) {
			throw new IllegalStateException("ConcurrencyExecutionService is already set!");
		}
		if (service == null) {
			throw new IllegalArgumentException("service must not be null!");
		}

		this.service = service;
	}

}
