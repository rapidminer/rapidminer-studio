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

import java.util.regex.PatternSyntaxException;

import com.rapidminer.tools.expression.FunctionInputException;


/**
 * A {@link Function} for checking whether a nominal value matches a regular expression
 *
 * @author Thilo Kamradt
 *
 */
public class Matches extends Abstract2StringInputBooleanOutputFunction {

	/**
	 * Creates a function for checking whether a nominal value matches a regular expression
	 */
	public Matches() {
		super("text_information.matches");
	}

	@Override
	protected Boolean compute(String value1, String value2) {
		if (value1 == null || value2 == null) {
			return null;
		}
		try {
			return value1.matches(value2);
		} catch (PatternSyntaxException e) {
			throw new FunctionInputException("expression_parser.invalid_regex", value2, getFunctionName());
		}
	}

}
