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
package com.rapidminer.tools.expression.internal.function.process;

import com.rapidminer.MacroHandler;
import com.rapidminer.Process;
import com.rapidminer.tools.expression.FunctionDescription;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.function.AbstractArbitraryStringInputStringOutputFunction;


/**
 * A {@link Function} that looks for a macro value and can deliver a default value if the macro does
 * not exist.
 *
 * @author Gisa Schaefer
 *
 */
public class MacroValue extends AbstractArbitraryStringInputStringOutputFunction {

	private final MacroHandler handler;

	/**
	 * Creates a function that can look up a macro value.
	 *
	 * @param process
	 *            the process with the {@link MacroHandler} that should be used for finding the
	 *            macro
	 */
	public MacroValue(Process process) {
		super("process.macro", FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS);
		handler = process.getMacroHandler();
	}

	/**
	 * Creates a function that can look up a macro value.
	 *
	 * @param handler
	 *            the {@link MacroHandler} that should be used for finding the macro
	 */
	public MacroValue(MacroHandler handler) {
		super("process.macro", FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS);
		this.handler = handler;
	}

	@Override
	protected void checkNumberOfInputs(int length) {
		if (length != 1 && length != 2) {
			throw new FunctionInputException("expression_parser.function_wrong_input_two", getFunctionName(), 1, 2, length);
		}
	}

	@Override
	protected String compute(String... values) {
		String macro = handler.getMacro(values[0]);
		if (values.length == 1) {
			if (macro == null) {
				throw new FunctionInputException("expression_parser.unknown_macro", values[0]);
			}
			return macro;
		} else {
			if (macro == null) {
				return values[1];
			} else {
				return macro;
			}
		}
	}

}
