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

import com.rapidminer.tools.expression.FunctionDescription;


/**
 * Class for the OR function that has 2 logical (numerical, true or false) inputs
 *
 * @author Sabrina Kirstein
 *
 */
public class Or extends AbstractLogicalFunctionWith2Inputs {

	/**
	 * Constructs a OR Function with 2 parameters with {@link FunctionDescription}
	 */
	public Or() {
		super("logical.or");
	}

	@Override
	protected Boolean compute(double left, double right) {
		if (Double.isNaN(left) || Double.isNaN(right)) {
			return null;
		}
		boolean leftValue = Math.abs(left) < Double.MIN_VALUE * 2 ? false : true;
		boolean rightValue = Math.abs(right) < Double.MIN_VALUE * 2 ? false : true;
		return leftValue || rightValue;
	}

	@Override
	protected Boolean compute(double left, Boolean right) {
		if (Double.isNaN(left) || right == null) {
			return null;
		}
		Boolean leftValue = Math.abs(left) < Double.MIN_VALUE * 2 ? false : true;
		return leftValue || right;
	}

	@Override
	protected Boolean compute(Boolean left, Boolean right) {
		if (left == null || right == null) {
			return null;
		}
		return left || right;
	}

}
