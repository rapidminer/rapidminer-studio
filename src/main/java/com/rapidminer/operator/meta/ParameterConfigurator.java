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

import java.util.List;

import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.value.ParameterValues;


/**
 * An interface collecting common methods for {@link OperatorChain OperatorChains} that set
 * parameter for their inner operators.
 *
 * @author Jan Czogalla
 * @since 8.0
 */
public interface ParameterConfigurator {

	/**
	 * The parameter name for &quot;Parameters to optimize in the format
	 * OPERATORNAME.PARAMETERNAME.&quot;
	 */
	String PARAMETER_PARAMETERS = "parameters";

	/** A specification of the parameter values for a parameter.&quot; */
	String PARAMETER_VALUES = "values";

	/**
	 * Means that the parameter iteration scheme can only handle discrete parameter values (i.e.
	 * lists or numerical grids).
	 */
	int VALUE_MODE_DISCRETE = 0;

	/** Means that the parameter iteration scheme can only handle intervals of numerical values. */
	int VALUE_MODE_CONTINUOUS = 1;

	/**
	 * Has to return one of the predefined modes which indicate whether the operator takes discrete
	 * values or intervals as basis for optimization. The first option is to be taken for all
	 * strategies that iterate over the given parameters. The latter option is to be taken for
	 * strategies such as an evolutionary one in which allowed ranges of parameters have to be
	 * specified.
	 */
	int getParameterValueMode();

	/**
	 * Parses a parameter list and creates the corresponding data structures.
	 */
	List<ParameterValues> parseParameterValues(List<String[]> parameterList) throws OperatorException;

}
