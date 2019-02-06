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

/**
 * A sparse chunk that stores double values.
 *
 * @author Gisa Schaefer
 * @since 7.4
 */
interface DoubleSparseChunk {

	/**
	 * Sets the value for the given row. Returns {@code true} if after this set the sparse chunk is
	 * too full. Note that the density check only works if the total size was {@link #ensure}d
	 * before.
	 *
	 * @param row
	 *            the row for which to set the value
	 * @param value
	 *            the value to store
	 * @return {@code true} if the maximal density is reached
	 */
	boolean set(int row, double value);

	/**
	 * Returns the value stored for this row.
	 *
	 * @param row
	 *            the row for which to obtain the stored value
	 * @return the value stored for row
	 */
	double get(int row);

	/**
	 * Sets the total size.
	 *
	 * @param size
	 *            the expected size
	 */
	void ensure(int size);

}
