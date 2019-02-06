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

import com.rapidminer.operator.DefaultIODescription;
import com.rapidminer.operator.IllegalInputException;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.WrongNumberOfInnerOperatorsException;


/**
 * This condition can be used to define a output condition for the last operator in a chain (e.g.
 * the last operator of a chain must deliver a performance vector). This also means that all
 * operators before can handle their predecessors output. It is therefore not necessary to add a
 * {@link SimpleChainInnerOperatorCondition} additionally to this condition to the list of
 * conditions.
 * 
 * @author Ingo Mierswa ingomierswa Exp $
 */
@SuppressWarnings("deprecation")
public class LastInnerOperatorCondition implements InnerOperatorCondition {

	/**
	 * The array of classes which must be delivered by the inner chain of operators described by
	 * this InnerOpDesc object.
	 */
	private final Class<?>[] mustDeliver;

	private final Class<?>[] willGet;

	private boolean allowEmptyChains = false;

	/**
	 * Creates an inner operator condition. The first operator in the chain gets the input of the
	 * operator chain. Each operator must be able to handle the output of the predecessor. The last
	 * operator must provide all classes in the given <code>mustDeliver</code> class array. Empty
	 * chains are not allowed.
	 */
	public LastInnerOperatorCondition(Class<?>[] mustDeliver) {
		this(null, mustDeliver);
	}

	/**
	 * Creates an inner operator condition. The first operator in the chain gets the input of the
	 * operator chain and additionally the given classes <code>willGet</code>. Each operator must be
	 * able to handle the output of the predecessor. The last operator must provide all classes in
	 * the given <code>mustDeliver</code> class array. Empty chains are not allowed.
	 */
	public LastInnerOperatorCondition(Class<?>[] willGet, Class<?>[] mustDeliver) {
		this(willGet, mustDeliver, false);
	}

	/**
	 * Creates an inner operator condition. The first operator in the chain gets the input of the
	 * operator chain and additionally the given classes <code>willGet</code>. Each operator must be
	 * able to handle the output of the predecessor. The last operator must provide all classes in
	 * the given <code>mustDeliver</code> class array.
	 */
	public LastInnerOperatorCondition(Class<?>[] willGet, Class<?>[] mustDeliver, boolean allowEmptyChains) {
		this.willGet = willGet;
		this.mustDeliver = mustDeliver;
		this.allowEmptyChains = allowEmptyChains;
	}

	/** */
	@Override
	public Class<?>[] checkIO(OperatorChain chain, Class<?>[] input)
			throws IllegalInputException, WrongNumberOfInnerOperatorsException {
		if (!allowEmptyChains && chain.getNumberOfOperators() == 0) {
			throw new WrongNumberOfInnerOperatorsException(chain, chain.getMinNumberOfInnerOperators(),
					chain.getMaxNumberOfInnerOperators(), 0);
		}

		Class<?>[] output = input;
		if (willGet != null) {
			output = new Class[input.length + willGet.length];
			System.arraycopy(input, 0, output, 0, input.length);
			System.arraycopy(willGet, 0, output, input.length, willGet.length);
		}

		for (int i = 0; i < chain.getNumberOfOperators(); i++) {
			Operator operator = chain.getOperator(i);
			if (operator.isEnabled()) {
				output = operator.checkIO(output);
			}
		}
		for (int i = 0; i < mustDeliver.length; i++) {
			if (!DefaultIODescription.containsClass(mustDeliver[i], output)) {
				throw new IllegalInputException(chain, chain.getOperator(chain.getNumberOfOperators() - 1), mustDeliver[i]);
			}
		}

		return output;
	}

	@Override
	public String toHTML() {
		StringBuffer result = new StringBuffer("The inner operators ");
		if (willGet != null) {
			result.append("must be able to handle [");
			for (int i = 0; i < willGet.length; i++) {
				if (i != 0) {
					result.append(", ");
				}
				result.append(willGet[i].getSimpleName());
			}
			result.append("]");
			if (mustDeliver != null) {
				result.append(" and ");
			} else {
				result.append(".");
			}
		}
		if (mustDeliver != null) {
			result.append("must deliver [");
			for (int i = 0; i < mustDeliver.length; i++) {
				if (i != 0) {
					result.append(", ");
				}
				result.append(mustDeliver[i].getSimpleName());
			}
			result.append("].");
		}
		return result.toString();
	}
}
