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
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;


/**
 * Returns true if the value of the desired attribute is greater then a given threshold.
 *
 * @author Ingo Mierswa
 */
public class GreaterSplitCondition extends AbstractSplitCondition {

	private static final long serialVersionUID = 7094464803196955502L;

	private double value;
	private final int attValueType;

	public GreaterSplitCondition(Attribute attribute, double value) {
		super(attribute.getName());
		this.value = value;
		this.attValueType = attribute.getValueType();
	}

	@Override
	public boolean test(Example example) {
		return example.getValue(example.getAttributes().get(getAttributeName())) > value;
	}

	@Override
	public String getRelation() {
		return ">";
	}

	public double getValue() {
		return value;
	}

	@Override
	public String getValueString() {
		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attValueType, Ontology.DATE)) {
			return Tools.createDateAndFormat(value);
		} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attValueType, Ontology.TIME)) {
			return Tools.createTimeAndFormat(value);
		} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attValueType, Ontology.DATE_TIME)) {
			return Tools.createDateTimeAndFormat(value);
		} else {
			return Tools.formatIntegerIfPossible(this.value);
		}
	}
}
