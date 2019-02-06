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
 * This implementation of {@link Column} is used to represent {@code null} attributes, i.e.
 * attributes that were removed and are not set in a {@link ColumnarExampleTable}. This makes
 * {@code null} checks unnecessary when iterating over all attribute indices.
 *
 * @author Jan Czogalla
 * @see Column
 * @see ColumnarExampleTable
 * @since 7.3
 *
 */
class NaNColumn implements Column {

	private static final long serialVersionUID = 1L;

	@Override
	public double get(int row) {
		return Double.NaN;
	}

	@Override
	public void set(int row, double value) {
		// do nothing
	}

	@Override
	public void ensure(int size) {
		// do nothing
	}

	@Override
	public void setLast(int row, double value) {
		// do nothing
	}

}
