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

import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.function.AbstractArbitraryStringInputStringOutputFunction;


/**
 * A {@link Function} for replacing parts of a string by an other. Which parts replaced parts is
 * defined by a regular expression
 *
 * @author David Arnu
 *
 */
public class ReplaceAll extends AbstractArbitraryStringInputStringOutputFunction {

	/**
	 * Creates a function for replacing parts of a string by an other
	 */
	public ReplaceAll() {
		super("text_transformation.replace_all", 3);
	}

	/**
	 * Replaces all occurences of <code>regex</code> in <code>text</code> with
	 * <code>replacement</code>
	 *
	 * @param text
	 * @param regex
	 * @param replacement
	 * @return the string with replacements
	 */
	protected String compute(String text, String regex, String replacement) {

		// missing values are not changed
		if (text == null || regex == null || replacement == null) {
			return null;
		} else {
			if (regex.length() == 0) {
				throw new FunctionInputException("expression_parser.function_missing_arguments", "regex", getFunctionName());
			}
			return text.replaceAll(regex, replacement);
		}
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
