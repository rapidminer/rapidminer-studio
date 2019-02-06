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
package com.rapidminer.example.table.internal;

import java.io.Serializable;


/**
 * This interface is the basis for the columns used in the {@link ColumnarExampleTable}.
 * Implementing classes provide their own internal data structure.
 *
 * @author Jan Czogalla
 * @see ColumnarExampleTable
 * @since 7.3
 *
 */
interface Column extends Serializable {

	/**
	 * Gets the value at the specified row.
	 *
	 * @param row
	 *            the row that should be looked up
	 * @return the value at the specified row
	 */
	double get(int row);

	/**
	 * Sets the value at the specified row to the given value. It does the same as
	 * {@link #set(int, double)} but with the additional information that the highest row that was
	 * set before is smaller than row. This additional information is used for autodetection of
	 * column densitiy. Note that this operation is unchecked, make sure to {@link #ensure(int)} a
	 * sufficient size before.
	 *
	 * @param row
	 *            the row that should be set
	 * @param value
	 *            the value that should be set at the row
	 */
	void setLast(int row, double value);

	/**
	 * Sets the value at the specified row to the given value.
	 *
	 * @param row
	 *            the row that should be set
	 * @param value
	 *            the value that should be set at the row
	 */
	void set(int row, double value);

	/**
	 * Ensures that the internal data structure can hold up to {@code size} values.
	 *
	 * @param size
	 *            the size that should be ensured
	 */
	void ensure(int size);

	/**
	 * Completes the column (optional). Invoking this method signals that no further calls to
	 * {@link #ensure(int)} and {@link #append(double)} will be made.
	 */
	default void complete() {};

}
