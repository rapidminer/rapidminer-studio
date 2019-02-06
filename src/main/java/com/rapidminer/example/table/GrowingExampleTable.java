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
package com.rapidminer.example.table;

/**
 * {@link ExampleTable} to which rows can be added.
 *
 * @author Gisa Schaefer
 * @since 7.3
 */
public interface GrowingExampleTable extends ExampleTable {

	/**
	 * Adds the given row at the end of the table
	 *
	 * @param row
	 *            the row to be added
	 * @throws RuntimeException
	 *             May be thrown if the data row does not fit the attributes of the underlying
	 *             table, depending on the data row implementation.
	 */
	void addDataRow(DataRow row);
}
