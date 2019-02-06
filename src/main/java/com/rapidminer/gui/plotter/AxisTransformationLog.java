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
package com.rapidminer.gui.plotter;

import java.text.DecimalFormat;


/**
 * Transforms the given value by applying a log function.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class AxisTransformationLog implements AxisTransformation {

	private DecimalFormat format = new DecimalFormat("0.00E0");

	/** Transforms the given value by applying a log function. */
	@Override
	public double transform(double value) {
		if (value <= 0.0d) {
			throw new IllegalArgumentException("Cannot apply log scaling to a value less than or equal to zero.");
		}
		return Math.log10(value);
	}

	/** Transforms the given value by applying an exponential function. */
	@Override
	public double inverseTransform(double value) {
		return Math.pow(10, value);
	}

	/** Returns the formatted value. Returns null if the value does not start with 1. */
	@Override
	public String format(double value, int formatNumber) {
		if (formatNumber % 2 == 0) {
			return format.format(value);
		} else {
			return null;
		}
	}

	/** Adapts the minimum corresponding to the given tic size. */
	@Override
	public double adaptTicsMin(double min, double ticSize) {
		return min;
	}

	/** Adapts the maximum corresponding to the given tic size. */
	@Override
	public double adaptTicsMax(double max, double ticSize) {
		return max;
	}
}
