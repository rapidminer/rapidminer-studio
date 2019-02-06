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

import java.io.File;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * This condition tests if a file exists.
 *
 * @author Sebastian Land
 */
public class FileExistsCondition implements ProcessBranchCondition {

	/**
	 * Constructor used by reflection.
	 */
	public FileExistsCondition() {}

	/**
	 * This method checks if the file with pathname value exists.
	 */
	@Override
	public boolean check(ProcessBranch operator, String value) throws OperatorException {
		if (value == null) {
			throw new UndefinedParameterError(ProcessBranch.PARAMETER_CONDITION_VALUE, operator);
		}
		File file = new File(value);
		return file.exists();
	}
}
