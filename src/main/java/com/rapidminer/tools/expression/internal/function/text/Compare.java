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
package com.rapidminer.tools.expression.internal.function.text;



/**
 * A {@link Function} for comparing two Strings.
 *
 * @author David Arnu
 *
 */
public class Compare extends Abstract2StringInputIntegerOutputFunction {

	/**
	 * Creates a function for comparing two Strings
	 */
	public Compare() {
		super("text_information.compare", 2);
	}

	@Override
	protected double compute(String value1, String value2) {

		// if one of the two strings is a missing value, we also return missing
		if (value1 == null || value2 == null) {
			return Double.NaN;
		}
		return value1.compareTo(value2);
	}

}
