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
 * A condition for a specific inner operator with a fixed index. Since in these cases often a
 * special name for this operator can be used, e.g. &quot;Training&quot; for the first operator of a
 * cross validation, this condition also allows the definition of a describing name.
 * 
 * @author Ingo Mierswa ingomierswa Exp $
 */
@SuppressWarnings("deprecation")
public class SpecificInnerOperatorCondition implements InnerOperatorCondition {

	/**
	 * A short name describing the purpose of this inner operator chain, e.g. &quot;training&quot;
	 * and &quot;applier chain&quot; for cross validation.
	 */
	private final String name;

	/**
	 * The operator index of the chain of operators for which this description object should be
	 * created.
	 */
	private final int index;

	/**
	 * The array of classes which is given to the inner chain of operators described by this
	 * InnerOpDesc object.
	 */
	private final Class<?>[] willGet;

	/**
	 * The array of classes which must be delivered by the inner chain of operators described by
	 * this InnerOpDesc object.
	 */
	private final Class<?>[] mustDeliver;

	/** Creates an inner operator condition. */
	public SpecificInnerOperatorCondition(String name, int index, Class<?>[] willGet, Class<?>[] mustDeliver) {
		this.name = name;
		this.index = index;
		this.willGet = willGet;
		this.mustDeliver = mustDeliver;
	}

	@Override
	public Class<?>[] checkIO(OperatorChain chain, Class<?>[] input)
			throws IllegalInputException, WrongNumberOfInnerOperatorsException {
		if (this.index < 0 || this.index >= chain.getNumberOfOperators()) {
			throw new WrongNumberOfInnerOperatorsException(chain, chain.getMinNumberOfInnerOperators(),
					chain.getMaxNumberOfInnerOperators(), index);
		}
		Operator operator = chain.getOperator(this.index);

		if (this.index == 0) {
			for (Class<?> c : willGet) {
				if (!DefaultIODescription.containsClass(c, input)) {
					throw new IllegalInputException(operator, c);
				}
			}
		}

		Class<?>[] output = operator.checkIO(willGet);
		for (int i = 0; i < mustDeliver.length; i++) {
			if (!DefaultIODescription.containsClass(mustDeliver[i], output)) {
				throw new IllegalInputException(chain, operator, mustDeliver[i]);
			}
		}
		return mustDeliver;
	}

	@Override
	public String toHTML() {
		StringBuffer result = new StringBuffer("Operator " + (index + 1) + " (" + name + ") must be able to handle [");
		for (int i = 0; i < willGet.length; i++) {
			if (i != 0) {
				result.append(", ");
			}
			result.append(willGet[i].getSimpleName());
		}
		result.append("]");
		if ((mustDeliver != null) && (mustDeliver.length > 0)) {
			result.append(" and must deliver [");
			for (int i = 0; i < mustDeliver.length; i++) {
				if (i != 0) {
					result.append(", ");
				}
				result.append(mustDeliver[i].getSimpleName());
			}
			result.append("].");
		} else {
			result.append(".");
		}
		return result.toString();
	}
}
