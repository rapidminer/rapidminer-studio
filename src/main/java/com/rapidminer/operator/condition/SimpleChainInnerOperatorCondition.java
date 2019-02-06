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
package com.rapidminer.operator.condition;

import com.rapidminer.operator.IllegalInputException;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.WrongNumberOfInnerOperatorsException;


/**
 * This condition can be used to check if all inner operators can handle the output of their
 * predecessor.
 * 
 * @author Ingo Mierswa
 */
@SuppressWarnings("deprecation")
public class SimpleChainInnerOperatorCondition implements InnerOperatorCondition {

	private boolean deliverInput;

	public SimpleChainInnerOperatorCondition() {
		this(false);
	}

	public SimpleChainInnerOperatorCondition(boolean deliverInput) {
		this.deliverInput = deliverInput;
	}

	@Override
	public Class<?>[] checkIO(OperatorChain chain, Class<?>[] input)
			throws IllegalInputException, WrongNumberOfInnerOperatorsException {
		Class<?>[] output = input;
		for (int i = 0; i < chain.getNumberOfOperators(); i++) {
			Operator operator = chain.getOperator(i);
			if (operator.isEnabled()) {
				output = operator.checkIO(output);
			}
		}
		if (this.deliverInput) {
			return input;
		} else {
			return output;
		}
	}

	@Override
	public String toHTML() {
		return "All inner operators must be able to handle the output of their predecessor.";
	}
}
