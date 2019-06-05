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
package com.rapidminer.tools.expression.internal.function;

import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.Function;
import com.rapidminer.tools.expression.FunctionDescription;


/**
 * Abstract {@link Function} that provides a constructor with i18n and helper methods to compute the
 * result type and if the result is constant.
 *
 * @author Gisa Schaefer
 *
 */
public abstract class AbstractFunction implements Function {

	private final String functionName;
	private final FunctionDescription description;

	/**
	 * Constructs an AbstractFunction with {@link FunctionDescription} generated from the arguments
	 * and the function name generated from the description.
	 *
	 * @param i18nKey
	 *            the key for the {@link FunctionDescription}. The functionName is read from
	 *            "gui.dialog.function.i18nKey.name", the helpTextName from ".help", the groupName
	 *            from ".group", the description from ".description" and the function with
	 *            parameters from ".parameters". If ".parameters" is not present, the ".name" is
	 *            taken for the function with parameters.
	 * @param numberOfArgumentsToCheck
	 *            the fixed number of parameters this functions expects or -1
	 * @param returnType
	 *            the {@link Ontology#ATTRIBUTE_VALUE_TYPE}
	 */
	public AbstractFunction(String i18nKey, int numberOfArgumentsToCheck, int returnType) {
		this.description = new FunctionDescription(i18nKey, numberOfArgumentsToCheck, returnType);
		functionName = description.getDisplayName().split("\\(")[0];
	}

	@Override
	public String getFunctionName() {
		return functionName;
	}

	@Override
	public FunctionDescription getFunctionDescription() {
		return description;
	}

	/**
	 * Whether this function returns the same result for the same input. Default implementation
	 * returns {@code true}.
	 *
	 * @return default implementation returns {@code true}
	 */
	protected boolean isConstantOnConstantInput() {
		return true;
	}

	/**
	 * Computes the result {@ExpressionType} from the types of the arguments
	 * inputTypes.
	 *
	 * @param inputTypes
	 *            the types of the inputs
	 * @return the result type
	 */
	protected abstract ExpressionType computeType(ExpressionType... inputTypes);

	/**
	 * Extracts the {@link ExpressionType}s from the inputEvaluators and calls
	 * {@link #computeType(ExpressionType...)}.
	 *
	 * @param inputEvaluators
	 *            the input evaluators
	 * @return the result type of the function when getting the given inputs
	 */
	protected ExpressionType getResultType(ExpressionEvaluator... inputEvaluators) {
		ExpressionType[] inputTypes = new ExpressionType[inputEvaluators.length];
		for (int i = 0; i < inputEvaluators.length; i++) {
			inputTypes[i] = inputEvaluators[i].getType();
		}
		return computeType(inputTypes);
	}

	/**
	 * Computes whether the result of this function is constant.
	 *
	 * @param inputEvaluators
	 *            the input arguments
	 * @return {@code true} if the result of this function is constant
	 */
	protected boolean isResultConstant(ExpressionEvaluator... inputEvaluators) {
		if (!isConstantOnConstantInput()) {
			return false;
		}
		for (ExpressionEvaluator inputEvaluator : inputEvaluators) {
			if (!inputEvaluator.isConstant()) {
				return false;
			}
		}
		return true;
	}

}
