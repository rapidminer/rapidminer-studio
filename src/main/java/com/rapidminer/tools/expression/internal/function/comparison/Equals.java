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
package com.rapidminer.tools.expression.internal.function.comparison;

import java.util.Date;

import com.rapidminer.tools.expression.FunctionDescription;


/**
 * Class for the EQUALS function that has 2 arbitrary inputs
 *
 * @author Sabrina Kirstein
 *
 */
public class Equals extends AbstractEqualityFunctionWith2Inputs {

	/**
	 * Constructs a EQUALS Function with 2 parameters with {@link FunctionDescription}
	 */
	public Equals() {
		super("comparison.equals");
	}

	@Override
	protected Boolean compute(double left, double right) {
		if (Double.isNaN(left) && Double.isNaN(right)) {
			return true;
		}
		if (Double.isNaN(left) || Double.isNaN(right)) {
			return false;
		}
		return left == right;
	}

	@Override
	protected Boolean compute(double left, Boolean right) {
		if (Double.isNaN(left) || right == null) {
			return false;
		}
		double rightValue = right ? 1 : 0;
		return left == rightValue;
	}

	@Override
	protected Boolean compute(Boolean left, Boolean right) {
		if (left == null && right == null) {
			return true;
		}
		if (left == null || right == null) {
			return false;
		}
		return left == right;
	}

	@Override
	protected Boolean compute(double left, String right) {
		if (right == null || Double.isNaN(left)) {
			return false;
		}
		try {
			return left == Double.parseDouble(right);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	protected Boolean compute(double left, Date right) {
		if (right == null || Double.isNaN(left)) {
			return false;
		}
		return right.getTime() == left;
	}

	@Override
	protected Boolean compute(Boolean left, String right) {
		if (left == null || right == null) {
			return false;
		}
		return Boolean.toString(left).equals(right);
	}

	@Override
	protected Boolean compute(Boolean left, Date right) {
		return false;
	}

	@Override
	protected Boolean compute(String left, String right) {
		if (left == null && right == null) {
			return true;
		}
		if (left == null || right == null) {
			return false;
		}
		return left.equals(right);
	}

	@Override
	protected Boolean compute(String left, Date right) {
		return false;
	}

	@Override
	protected Boolean compute(Date left, Date right) {
		if (left == null && right == null) {
			return true;
		}
		if (left == null || right == null) {
			return false;
		}
		return left.equals(right);
	}
}
