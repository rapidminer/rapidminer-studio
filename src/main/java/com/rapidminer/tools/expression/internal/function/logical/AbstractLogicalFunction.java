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
package com.rapidminer.tools.expression.internal.function.logical;

import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.FunctionDescription;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.function.AbstractFunction;


/**
 * Abstract class for a function that has arbitrary logical (numerical, true or false) inputs
 *
 * @author Sabrina Kirstein
 *
 */
public abstract class AbstractLogicalFunction extends AbstractFunction {

	/**
	 * Constructs a logical AbstractFunction with {@link FunctionDescription} generated from the
	 * arguments and the function name generated from the description.
	 *
	 * @param i18nKey
	 *            the key for the {@link FunctionDescription}. The functionName is read from
	 *            "gui.dialog.function.i18nKey.name", the helpTextName from ".help", the groupName
	 *            from ".group", the description from ".description" and the function with
	 *            parameters from ".parameters". If ".parameters" is not present, the ".name" is
	 *            taken for the function with parameters.
	 * @param numberOfArgumentsToCheck
	 *            the fixed number of parameters this functions expects or
	 *            {@link FunctionDescription#UNFIXED_NUMBER_OF_ARGUMENTS}
	 */
	public AbstractLogicalFunction(String i18nKey, int numberOfArgumentsToCheck) {
		super(i18nKey, numberOfArgumentsToCheck, Ontology.BINOMINAL);
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {

		if (inputTypes.length > 2 || inputTypes.length < 1) {
			throw new FunctionInputException("expression_parser.function_wrong_input_two", getFunctionName(), "1", "2",
					inputTypes.length);
		}
		for (ExpressionType inputType : inputTypes) {
			if (inputType != ExpressionType.INTEGER && inputType != ExpressionType.DOUBLE
					&& inputType != ExpressionType.BOOLEAN) {
				throw new FunctionInputException("expression_parser.function_wrong_type_two", getFunctionName(), "boolean",
						"numerical");
			}
		}
		// result is always boolean
		return ExpressionType.BOOLEAN;
	}

}
