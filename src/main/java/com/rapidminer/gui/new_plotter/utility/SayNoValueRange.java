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

import com.rapidminer.datatable.DataTableRow;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class SayNoValueRange extends AbstractValueRange {

	@Override
	public double getValue() {
		return Double.NaN;
	}

	@Override
	public boolean definesUpperLowerBound() {
		return false;
	}

	@Override
	public double getUpperBound() {
		return Double.NaN;
	}

	@Override
	public double getLowerBound() {
		return Double.NaN;
	}

	@Override
	public boolean keepRow(DataTableRow row) {
		return false;
	}

	@Override
	public ValueRange clone() {
		return new SayNoValueRange();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof SayNoValueRange) {
			return true;
		}
		return false;
	}

}
