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
package com.rapidminer.tools.expression.internal.function.logical;

import com.rapidminer.tools.expression.FunctionDescription;


/**
 * Class for the NOT function that has 1 logical (numerical, true or false) input
 *
 * @author Sabrina Kirstein
 *
 */
public class Not extends AbstractLogicalFunctionWith1Input {

	/**
	 * Constructs a NOT Function with 1 parameter with {@link FunctionDescription}
	 */
	public Not() {
		super("logical.not");
	}

	@Override
	protected Boolean compute(double value) {
		// if the given value is double missing -> return boolean missing
		if (Double.isNaN(value)) {
			return null;
		}
		// else if the value is zero (false) -> return true
		else if (Math.abs(value) < Double.MIN_VALUE * 2) {
			return true;
		} else {
			// if the given value is not zero (true) -> return false
			return false;
		}
	}

	@Override
	protected Boolean compute(Boolean value) {
		// if the given value is missing -> return missing
		if (value == null) {
			return null;
			// if the given value is true -> return false
		} else if (value) {
			return false;
			// if the given value is false -> return true
		} else {
			return true;
		}
	}

}
