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
package com.rapidminer.operator.ports.metadata;

import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.Port;


/**
 * Transforms the meta data by applying the meta data transformer of a subprocess. Remember to add
 * this rule at the correct place, i.e. after the rules that ensure that the inner sources receive
 * their meta data.
 * 
 * @author Simon Fischer
 * 
 */
public class SubprocessTransformRule implements MDTransformationRule {

	private final ExecutionUnit subprocess;

	public SubprocessTransformRule(ExecutionUnit subprocess) {
		this.subprocess = subprocess;
	}

	@Override
	public void transformMD() {
		for (Operator op : subprocess.getAllInnerOperators()) {
			op.clear(Port.CLEAR_META_DATA_ERRORS);
		}
		subprocess.transformMetaData();
	}
}
