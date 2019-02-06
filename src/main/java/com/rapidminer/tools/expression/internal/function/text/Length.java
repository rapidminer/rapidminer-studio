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
 * A {@link Function} which calculates the length of a nominal value.
 *
 * @author Thilo Kamradt
 *
 */
public class Length extends Abstract1StringInputIntegerOutputFunction {

	/**
	 * Creates a function which calculates the length of a nominal value.
	 */
	public Length() {
		super("text_information.length");
	}

	@Override
	protected double compute(String value1) {
		if (value1 == null) {
			return Double.NaN;
		}
		return value1.length();
	}

}
