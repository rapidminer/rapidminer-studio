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
package com.rapidminer.example.set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * The condition is fulfilled if an attribute has a value equal to, not equal to, less than, ... a
 * given value. This filter can be constructed from several conditions of the class
 * {@link AttributeValueFilterSingleCondition} which must all be fulfilled.
 * 
 * @author Ingo Mierswa
 */
public class AttributeValueFilter implements Condition {

	private static final long serialVersionUID = 6977275837081172924L;

	private static final int AND = 0;
	private static final int OR = 1;

	/** The list of all single conditions. */
	private List<AttributeValueFilterSingleCondition> conditions = new LinkedList<AttributeValueFilterSingleCondition>();

	private int combinationMode = AND;

	/**
	 * Creates a new AttributeValueFilter. If attribute is not nominal, value must be a number.
	 */
	public AttributeValueFilter(Attribute attribute, int comparisonType, String value) {
		addCondition(attribute, comparisonType, value);
	}

	/**
	 * Constructs an AttributeValueFilter for a given {@link ExampleSet} from a parameter string
	 * 
	 * @param parameterString
	 *            Must be of the form attribute1 R1 value1 RR attribute2 R2 value2 RR ..., where Ri
	 *            is one out of =, != or &lt;&gt;, &lt;, &gt;, &lt;=, and &gt;= and all RR must be
	 *            either || for OR or && for AND.
	 */
	public AttributeValueFilter(ExampleSet exampleSet, String parameterString) {
		if ((parameterString == null) || (parameterString.length() == 0)) {
			throw new IllegalArgumentException("Parameter string must not be empty!");
		}

		String[] splitted = parameterString.split("\\|\\|");
		if (splitted.length > 1) {
			for (String condition : splitted) {
				condition = condition.trim();
				addCondition(new AttributeValueFilterSingleCondition(exampleSet, condition));
			}
			this.combinationMode = OR;
		} else {
			splitted = parameterString.split("\\&\\&");
			if (splitted.length > 1) {
				for (String condition : splitted) {
					condition = condition.trim();
					addCondition(new AttributeValueFilterSingleCondition(exampleSet, condition));
				}
				this.combinationMode = AND;
			} else {
				addCondition(new AttributeValueFilterSingleCondition(exampleSet, parameterString));
				this.combinationMode = AND;
			}
		}
	}

	private void addCondition(Attribute attribute, int comparisonType, String value) {
		addCondition(new AttributeValueFilterSingleCondition(attribute, comparisonType, value));
	}

	private void addCondition(AttributeValueFilterSingleCondition condition) {
		conditions.add(condition);
	}

	/**
	 * Since the condition cannot be altered after creation we can just return the condition object
	 * itself.
	 * 
	 * @deprecated Conditions should not be able to be changed dynamically and hence there is no
	 *             need for a copy
	 */
	@Override
	@Deprecated
	public Condition duplicate() {
		return this;
	}

	@Override
	public String toString() {
		return conditions.toString();
	}

	/** Returns true if all conditions are fulfilled for the given example. */
	@Override
	public boolean conditionOk(Example e) {
		Iterator<AttributeValueFilterSingleCondition> i = conditions.iterator();
		while (i.hasNext()) {
			AttributeValueFilterSingleCondition condition = i.next();
			if (combinationMode == AND) {
				if (!condition.conditionOk(e)) {
					return false;
				}
			} else {
				if (condition.conditionOk(e)) {
					return true;
				}
			}
		}
		if (combinationMode == AND) {
			return true;
		} else {
			return false;
		}
	}
}
