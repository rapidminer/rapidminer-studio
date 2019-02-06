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

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorException;


/**
 * This condition tests if an IOObject exists.
 * 
 * @author Ingo Mierswa
 */
public class InputExistsCondition implements ProcessBranchCondition {

	/**
	 * Constructor used by reflection.
	 */
	public InputExistsCondition() {}

	/**
	 * This method checks if the file with pathname value exists.
	 */
	@Override
	public boolean check(ProcessBranch operator, String value) throws OperatorException {
		Class<? extends IOObject> clazz = operator.getSelectedClass();
		if (clazz != null) {
			IOObject object = operator.getConditionInputOrNull(clazz);
			return (object != null);
		}
		return false;
	}
}
