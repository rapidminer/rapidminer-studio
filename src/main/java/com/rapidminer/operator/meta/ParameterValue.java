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
package com.rapidminer.operator.meta;

import com.rapidminer.Process;
import com.rapidminer.operator.Operator;

import java.io.Serializable;
import java.util.Map;


/**
 * The parameter values used by the class {@link ParameterSet}.
 * 
 * @author Ingo Mierswa
 */
public class ParameterValue implements Serializable {

	private static final long serialVersionUID = -6847818423564185071L;

	private final String operator;

	private final String parameterKey;

	private final String parameterValue;

	public ParameterValue(String operator, String parameterKey, String parameterValue) {
		this.operator = operator;
		this.parameterKey = parameterKey;
		this.parameterValue = parameterValue;
	}

	public String getOperator() {
		return operator;
	}

	public String getParameterKey() {
		return parameterKey;
	}

	public String getParameterValue() {
		return parameterValue;
	}

	@Override
	public String toString() {
		return operator + "." + parameterKey + "\t= " + parameterValue;
	}

	public void apply(Process process, Map<String, String> nameTranslation) {
		String opName = null;
		if (nameTranslation != null) {
			opName = nameTranslation.get(this.operator);
		}
		if (opName == null) {
			opName = this.operator;
		}
		process.getLogger().fine(
				"Setting parameter '" + parameterKey + "' of operator '" + opName + "' to '" + parameterValue + "'.");
		Operator operator = process.getOperator(opName);
		if (operator == null) {
			process.getLogger().warning("No such operator: '" + opName + "'.");
		} else {
			operator.getParameters().setParameter(parameterKey, parameterValue);
		}
	}
}
