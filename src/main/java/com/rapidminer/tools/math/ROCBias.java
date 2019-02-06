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
package com.rapidminer.tools.math;

import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * Specifies how roc plots are evaluate: - first count correct classifications, then count incorrect
 * ones - first count incorrect classifications, then count correct ones - distribute them evenly.
 * 
 * @author Simon Fischer
 * 
 */
public enum ROCBias {

	PESSIMISTIC,

	NEUTRAL,

	OPTIMISTIC;

	/** Parameter to select the bias type. */
	public static final String PARAMETER_NAME_ROC_BIAS = "roc_bias";

	public static ROCBias getROCBiasParameter(Operator operator) throws UndefinedParameterError {
		return ROCBias.values()[operator.getParameterAsInt(PARAMETER_NAME_ROC_BIAS)];
	}

	public static ParameterType makeParameterType() {
		String[] values = new String[ROCBias.values().length];
		for (int i = 0; i < values.length; i++) {
			values[i] = ROCBias.values()[i].toString().toLowerCase();
		}
		return new ParameterTypeCategory(PARAMETER_NAME_ROC_BIAS,
				"Determines how the ROC (and AUC) are evaluated: Count correct predictions first, last, or alternatingly",
				values, OPTIMISTIC.ordinal());
	}
}
