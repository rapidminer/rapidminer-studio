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

import com.rapidminer.tools.Tools;
import com.rapidminer.tools.expression.Function;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.function.AbstractArbitraryStringInputStringOutputFunction;


/**
 * A {@link Function} which escapse all HTML tags of a nominal value.
 *
 * @author Thilo Kamradt
 *
 */
public class EscapeHTML extends AbstractArbitraryStringInputStringOutputFunction {

	/**
	 * Creates a function which escapse all HTML tags of a nominal value.
	 */
	public EscapeHTML() {
		super("text_transformation.escape_html", 1);
	}

	@Override
	protected String compute(String... values) {
		return Tools.escapeHTML(values[0]);
	}

	@Override
	protected void checkNumberOfInputs(int length) {
		if (length != 1) {
			throw new FunctionInputException("expression_parser.function_wrong_input", getFunctionName(), 1, length);
		}
	}

}
