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
package com.rapidminer.operator.meta.branch;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * This condition tests if the provided example set has a minimum number of regular attributes.
 *
 * @author Ingo Mierswa
 */
public class MinNumberOfAttributesCondition implements ProcessBranchCondition {

	/**
	 * Constructor used by reflection.
	 */
	public MinNumberOfAttributesCondition() {}

	@Override
	public boolean check(ProcessBranch operator, String value) throws OperatorException {
		if (value == null) {
			throw new UndefinedParameterError(ProcessBranch.PARAMETER_CONDITION_VALUE, operator);
		}

		int minNumber = 0;
		try {
			minNumber = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new UserError(operator, 207, new Object[] { value, ProcessBranch.PARAMETER_CONDITION_VALUE, e });
		}

		ExampleSet exampleSet = operator.getConditionInput(ExampleSet.class);
		return exampleSet.getAttributes().size() >= minNumber;
	}
}
