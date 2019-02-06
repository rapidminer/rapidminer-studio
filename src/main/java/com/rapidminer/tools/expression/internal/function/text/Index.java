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
 * A {@link Function} which checks if any part of a nominal value matches a specific string and
 * returns the index of the first matching letter.
 *
 * @author Thilo Kamradt
 *
 */
public class Index extends Abstract2StringInputIntegerOutputFunction {

	/**
	 * Creates a function which checks if any part of a nominal value matches a specific string and
	 * returns the index of the first matching letter.
	 */
	public Index() {
		super("text_information.index", 2);
	}

	@Override
	protected double compute(String value1, String value2) {
		if (value1 == null || value2 == null) {
			return Double.NaN;
		}
		return value1.indexOf(value2);
	}

}
