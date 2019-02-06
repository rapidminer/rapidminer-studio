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
import com.rapidminer.parameter.ParameterTypeTupel;


/**
 * Allows the specification of parameter values as a basis of e.g. optimization.
 * 
 * @author Tobias Malbrecht
 */
public abstract class ParameterValues {

	protected transient Operator operator;

	protected transient ParameterType type;

	protected String key;

	public ParameterValues(Operator operator, ParameterType type) {
		this.operator = operator;
		this.type = type;
	}

	public Operator getOperator() {
		return operator;
	}

	public ParameterType getParameterType() {
		return type;
	}

	public String getKey() {
		return ParameterTypeTupel.transformTupel2String(operator.getName(), type.getKey());
	}

	public abstract int getNumberOfValues();

	public abstract String getValuesString();

	public abstract void move(int index, int direction);

	public String[] getValuesArray() {
		return null;
	}

	public static boolean isValidNumericalParameter(String value) {
		if (value.startsWith("%{") && value.endsWith("}")) {
			return true;
		}
		try {
			Double.valueOf(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

}
