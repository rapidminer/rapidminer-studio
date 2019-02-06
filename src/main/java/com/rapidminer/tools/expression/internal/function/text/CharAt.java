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

import com.rapidminer.tools.expression.Function;


/**
 * A {@link Function} which delivers the character at a specific position of a nominal value.
 *
 * @author Thilo Kamradt
 *
 */
public class CharAt extends AbstractStringIntegerInputStringOutputFunction {

	/**
	 * Creates a function which delivers the character at a specific position of a nominal value
	 */
	public CharAt() {
		super("text_transformation.char", 2);
	}

	/**
	 * Delivers the character at the specified index.
	 *
	 * @param text
	 *            the text from which you want to separate the character
	 * @param index
	 *            the index of the character you want to separate
	 * @return the character at the specified index.
	 */
	@Override
	protected String compute(String text, double index) {
		if (text == null || index >= text.length() || index < 0) {
			return null;
		}
		return "" + text.charAt((int) index);
	}

}
