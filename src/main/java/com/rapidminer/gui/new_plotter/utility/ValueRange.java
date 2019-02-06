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
package com.rapidminer.gui.new_plotter.utility;

import com.rapidminer.datatable.DataTableFilterCondition;
import com.rapidminer.gui.new_plotter.listener.ValueRangeListener;


/**
 * Interface for value ranges. A value range is a collection of values. How these are specified
 * depends on the implementation. Possibilities are: a single value, a list of discrete or nominal
 * values, or a value range with x <= value <= y.
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public interface ValueRange extends DataTableFilterCondition {

	/**
	 * Returns a representative value for the value range which is used for sorting and for color
	 * mapping.
	 */
	public double getValue();

	@Override
	public String toString();

	public ValueRange clone();

	/**
	 * Implementations of ValueRange which define an upper and a lower bound should return true in
	 * this function.
	 */
	public boolean definesUpperLowerBound();

	/**
	 * Returns the upper bound if the implementation of ValueRange supports bound, or Double.NaN
	 * otherwise.
	 */
	public double getUpperBound();

	/**
	 * Returns the lower bound if the implementation of ValueRange supports bound, or Double.NaN
	 * otherwise.
	 */
	public double getLowerBound();

	public void addValueRangeListener(ValueRangeListener l);

	public void removeValueRangeListener(ValueRangeListener l);
}
