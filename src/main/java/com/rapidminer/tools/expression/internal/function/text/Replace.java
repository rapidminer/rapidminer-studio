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
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.function.AbstractArbitraryStringInputStringOutputFunction;


/**
 * A {@link Function} which replaces all parts of a nominal value which match a specific string.
 *
 * @author Thilo Kamradt
 *
 */
public class Replace extends AbstractArbitraryStringInputStringOutputFunction {

	/**
	 * Creates a function that replaces all parts of a nominal value which match a specific string
	 */
	public Replace() {
		super("text_transformation.replace", 3);
	}

	private String compute(String text, String target, String replacement) {

		// missing values are not changed
		if (text == null || target == null || replacement == null) {
			return null;
		}
		if (target.length() == 0) {
			throw new FunctionInputException("expression_parser.function_missing_arguments", "search", getFunctionName());
		}
		return text.replace(target, replacement);
	}

	@Override
	protected String compute(String... values) {
		return compute(values[0], values[1], values[2]);
	}

	/**
	 * Checks if the number of input arguments is exactly 3.
	 */
	@Override
	protected void checkNumberOfInputs(int inputLength) {
		if (inputLength != 3) {
			throw new FunctionInputException("expression_parser.function_wrong_input", getFunctionName(), 3, inputLength);
		}
	}

}
