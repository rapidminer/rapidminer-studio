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
package com.rapidminer.operator;

/**
 * Will be thrown if an operator chain has the wrong number of inner operators. Should be thrown
 * during validation.
 * 
 * @author Ingo Mierswa, Simon Fischer 15:35:42 ingomierswa Exp $
 */
public class IllegalNumberOfInnerOperatorsException extends Exception {

	private static final long serialVersionUID = 8042272058326397126L;

	private transient OperatorChain operatorChain;

	public IllegalNumberOfInnerOperatorsException(String message, OperatorChain operatorChain) {
		super(message);
		this.operatorChain = operatorChain;
	}

	public OperatorChain getOperatorChain() {
		return operatorChain;
	}
}
