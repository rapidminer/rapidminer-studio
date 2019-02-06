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
package com.rapidminer.studio.concurrency.internal.util;

/**
 * Interface which defines a listener which is used for background process execution.
 * <p>
 * Note that this part of the API is only temporary and might be removed in future versions again.
 * </p>
 *
 * @author Sebastian Land
 * @since 7.4
 */
public interface BackgroundExecutionServiceListener {

	/**
	 * This Method is being called every time a ProcessBackgroundExecution is removed. The
	 * respective ProcessBackgroundExecution is returned.
	 */
	public void processRemoved(BackgroundExecution execution);

	/**
	 * This Method is being called every time a ProcessBackgroundExecution is added. The respective
	 * ProcessBackgroundExecution is returned.
	 */
	public void processAdded(BackgroundExecution execution);

}
