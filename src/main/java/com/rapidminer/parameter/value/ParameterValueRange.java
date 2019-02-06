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
package com.rapidminer.parameter.value;

import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;


/**
 * Represents a range of numerical parameter values.
 * 
 * @author Tobias Malbrecht
 */
public class ParameterValueRange extends ParameterValues {

	private String min;

	private String max;

	public ParameterValueRange(Operator operator, ParameterType type, String min, String max) {
		super(operator, type);
		this.min = min;
		this.max = max;
	}

	public void setMin(String min) {
		this.min = min;
	}

	public String getMin() {
		return min;
	}

	public void setMax(String max) {
		this.max = max;
	}

	public String getMax() {
		return max;
	}

	/** Do nothing. */
	@Override
	public void move(int index, int direction) {}

	@Override
	public int getNumberOfValues() {
		return -1;
	}

	@Override
	public String getValuesString() {
		return "[" + min + ";" + max + "]";
	}

	@Override
	public String toString() {
		return "range: " + min + " - " + max;
	}
}
