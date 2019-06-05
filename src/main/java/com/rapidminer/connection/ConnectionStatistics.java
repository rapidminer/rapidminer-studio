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
package com.rapidminer.connection;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * An interface for simple statistics about a connection. This keeps track of the last change to the connection,
 * the last successful test as well as the last test error. The statistics can be updated, internally using {@link ZonedDateTime#now()}
 * for the dates.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
@JsonDeserialize(as = ConnectionStatisticsImpl.class)
public interface ConnectionStatistics {

	/** Gets the time of last successful test */
	ZonedDateTime getLastSuccess();

	/** Gets the time of last recorded change */
	ZonedDateTime getLastChange();

	/** Gets the error of last failed test */
	String getLastError();

	/** Updates the time of last successful test with now */
	ConnectionStatistics updateSuccess();

	/** Updates the time of last change with now */
	ConnectionStatistics updateChange();

	/**
	 * Updates the error of last failed test
	 *
	 * @param error
	 * 		the error message
	 */
	ConnectionStatistics updateError(String error);

}
