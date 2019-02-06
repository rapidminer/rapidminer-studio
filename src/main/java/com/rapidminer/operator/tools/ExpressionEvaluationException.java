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
package com.rapidminer.operator.tools;

import com.rapidminer.operator.OperatorException;


/**
 * This exception indicates an error during the evaluation of expressions.
 * 
 * @author Marco Boeck
 */
public class ExpressionEvaluationException extends OperatorException {

	private static final long serialVersionUID = 2654691902442722376L;

	public ExpressionEvaluationException(String str) {
		super(str);
	}

	public ExpressionEvaluationException(String str, Exception e) {
		super(str, e);
	}

}
