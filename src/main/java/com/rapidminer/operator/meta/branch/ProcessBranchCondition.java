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

import com.rapidminer.operator.OperatorException;


/**
 * This is the interface for all ProcessBranchConditions. Classes of this type may be used for the
 * ProcessBranch operator to create an if then else statement.
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public interface ProcessBranchCondition {

	/**
	 * This method checks if the actual condition is met for the given value .
	 * 
	 * @param operator
	 *            the operator which checks this condition
	 * @param value
	 *            the value to check if meets condition
	 * @return true if value meets condition, false otherwise
	 */
	public boolean check(ProcessBranch operator, String value) throws OperatorException;

}
