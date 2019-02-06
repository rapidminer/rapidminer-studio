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

import com.rapidminer.tools.expression.FunctionDescription;
import com.rapidminer.tools.expression.internal.function.AbstractArbitraryStringInputStringOutputFunction;


/**
 * A {@link Function} for concatenating Strings.
 *
 * @author David Arnu
 *
 */
public class Concat extends AbstractArbitraryStringInputStringOutputFunction {

	/**
	 * Creates a function for concatenating Strings.
	 */
	public Concat() {
		super("text_transformation.concat", FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS);
	}

	@Override
	protected String compute(String... values) {

		StringBuilder builder = new StringBuilder();

		for (String value : values) {
			// missing values are ignored
			if (value != null) {
				builder.append(value);
			}
		}
		return builder.toString();
	}

	@Override
	protected void checkNumberOfInputs(int length) {
		// we do not care about the number of attributes
	}

}
