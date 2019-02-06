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

import java.util.Locale;

import com.rapidminer.tools.expression.Function;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.function.AbstractArbitraryStringInputStringOutputFunction;


/**
 * A {@link Function} which transforms a nominal value to its upper case representation.
 *
 * @author Thilo Kamradt
 *
 */
public class Upper extends AbstractArbitraryStringInputStringOutputFunction {

	/**
	 * Creates a function that transforms a nominal value to its upper case representation
	 */
	public Upper() {
		super("text_transformation.upper", 1);
	}

	@Override
	protected String compute(String... values) {
		if (values[0] == null) {
			return null;
		}
		return values[0].toUpperCase(Locale.ENGLISH);
	}

	@Override
	protected void checkNumberOfInputs(int length) {
		if (length != 1) {
			throw new FunctionInputException("expression_parser.function_wrong_input", getFunctionName(), 1, length);
		}
	}

}
