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
package com.rapidminer.operator.learner.tree;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;


/**
 * A split condition for numerical missing values (equals).
 *
 * @author Gisa Schaefer
 */
public class NumericalMissingSplitCondition extends AbstractSplitCondition {

	private static final long serialVersionUID = 3883155435836330171L;

	/** the symbol displayed in the decision tree for numerical missing values */
	private static final String SYMBOL_MISSING_NUMERICAL = "?";

	public NumericalMissingSplitCondition(Attribute attribute) {
		super(attribute.getName());
	}

	@Override
	public boolean test(Example example) {
		double currentValue = example.getValue(example.getAttributes().get(getAttributeName()));
		return Double.isNaN(currentValue);
	}

	@Override
	public String getRelation() {
		return "=";
	}

	@Override
	public String getValueString() {
		return SYMBOL_MISSING_NUMERICAL;
	}
}
