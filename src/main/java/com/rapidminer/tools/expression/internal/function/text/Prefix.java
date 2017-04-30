/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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

import com.rapidminer.tools.expression.Function;


/**
 * A {@link Function} which computes the prefix of a given length from a nominal value
 *
 * @author Thilo Kamradt
 *
 */
public class Prefix extends AbstractStringIntegerInputStringOutputFunction {

	/**
	 * Creates a function which computes the prefix of a given length from a nominal value
	 */
	public Prefix() {
		super("text_transformation.prefix", 2);
	}

	/**
	 * Computes the result.
	 *
	 * @param text
	 * @param index
	 * @return the result of the computation.
	 */
	@Override
	protected String compute(String text, double index) {
		if (text == null) {
			return null;
		} else if (Double.isNaN(index)) {
			// for compatibility reasons
			index = 0;
		} else if (index < 0 || index >= text.length()) {
			return text;
		}
		return text.substring(0, Math.min(text.length(), (int) index));

	}

}
