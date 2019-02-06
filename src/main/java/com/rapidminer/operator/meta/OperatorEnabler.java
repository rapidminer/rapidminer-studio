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

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.SimpleOperatorChain;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInnerOperator;

import java.util.List;


/**
 * This operator can be used to enable and disable other operators. The operator which should be
 * enabled or disabled must be a child operator of this one. Together with one of the parameter
 * optimizing or iterating operators this operator can be used to dynamically change the process
 * setup which might be useful in order to test different layouts, e.g. the gain by using different
 * preprocessing steps.
 * 
 * @author Ingo Mierswa
 */
public class OperatorEnabler extends SimpleOperatorChain {

	/**
	 * The parameter name for &quot;The name of the operator which should be disabled or
	 * enabled&quot;
	 */
	public static final String PARAMETER_OPERATOR_NAME = "operator_name";

	/**
	 * The parameter name for &quot;Indicates if the operator should be enabled (true) or disabled
	 * (false)&quot;
	 */
	public static final String PARAMETER_ENABLE = "enable";

	public OperatorEnabler(OperatorDescription description) {
		super(description, "Subprocess");
	}

	@Override
	public void doWork() throws OperatorException {
		String operatorName = getParameterAsString(PARAMETER_OPERATOR_NAME);
		Operator operator = lookupOperator(operatorName);
		if (operator == null) {
			throw new UserError(this, 109, operatorName);
		}
		operator.setEnabled(getParameterAsBoolean(PARAMETER_ENABLE));
		getSubprocess(0).execute();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInnerOperator(PARAMETER_OPERATOR_NAME,
				"The name of the operator which should be disabled or enabled");
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_ENABLE,
				"Indicates if the operator should be enabled (true) or disabled (false)", false);
		type.setExpert(false);
		types.add(type);
		return types;
	}
}
