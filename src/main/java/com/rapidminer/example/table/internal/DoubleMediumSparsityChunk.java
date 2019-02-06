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
 * A grand sparse chunk that stores double values of medium sparsity.
 *
 * @author Jan Czogalla
 * @since 7.3.1
 */
final class DoubleMediumSparsityChunk extends AbstractMediumSparsityChunk implements DoubleSparseChunk {

	private static final long serialVersionUID = 1L;

	private double[] data = AutoColumnUtils.EMPTY_DOUBLE_ARRAY;

	DoubleMediumSparsityChunk(double defaultValue) {
		super(defaultValue);
	}

	@Override
	void removeValueIndex(int index, int length) {
		double[] tmp = data;
		if (length != tmp.length) {
			tmp = new double[length];
		}
		AutoColumnUtils.copy(data, tmp, index, index + 1, index, valueCount);
		data = tmp;
	}

	@Override
	void insertValueIndex(int index, int length) {
		double[] tmp = data;
		if (length != tmp.length) {
			tmp = new double[length];
		}
		AutoColumnUtils.copy(data, tmp, index, index, index + 1, valueCount);
		data = tmp;
	}

	@Override
	double getValue(int index) {
		return data[index];
	}

	@Override
	void setValue(int index, double value) {
		data[index] = value;
	}

	@Override
	protected double getMaximalDensity(int row) {
		if (row < AutoColumnUtils.THRESHOLD_CHECK_FOR_SPARSE) {
			return AutoColumnUtils.THRESHOLD_DOUBLE_MEDIUM_SPARSITY_DENSITY;
		}
		return AutoColumnUtils.THRESHOLD_DOUBLE_MEDIUM_SPARSITY_MAXIMAL_DENSITY;
	}

}
