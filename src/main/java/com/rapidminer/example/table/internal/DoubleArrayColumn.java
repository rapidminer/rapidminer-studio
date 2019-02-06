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

import java.util.Arrays;

import com.rapidminer.tools.Ontology;


/**
 * This implementation of {@link Column} uses an internal double array to store values and is
 * usually used for non-nominal attributes (see {@link Ontology#NOMINAL},
 * {@link Ontology#BINOMINAL}, {@link Ontology#POLYNOMINAL}) .
 *
 * @author Jan Czogalla
 * @see Column
 * @see ColumnarExampleTable
 * @since 7.3
 *
 */
class DoubleArrayColumn implements Column {

	private static final long serialVersionUID = 1L;

	protected double[] data;

	/** Creates a new {@code DoubleArrayColumn} with a capacity for {@code size} double values. */
	DoubleArrayColumn(int size) {
		data = new double[size];
	}

	@Override
	public double get(int row) {
		return data[row];
	}

	@Override
	public void set(int row, double value) {
		data[row] = value;
	}

	@Override
	public void ensure(int size) {
		data = Arrays.copyOf(data, size);
	}

	@Override
	public void setLast(int row, double value) {
		set(row, value);
	}

}
