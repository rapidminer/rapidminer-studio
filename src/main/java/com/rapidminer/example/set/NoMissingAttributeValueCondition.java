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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;


/**
 * This subclass of {@link Condition} serves to excludes all examples containing missing values
 * within specified attributes from an example set. The parameters might be specified using a
 * regular expression as parameter string
 * 
 * @author Sebastian Land ingomierswa Exp $
 */
public class NoMissingAttributeValueCondition implements Condition {

	private static final long serialVersionUID = -6043772701857922762L;

	private Collection<Attribute> checkedAttributes = new LinkedList<Attribute>();

	public NoMissingAttributeValueCondition(ExampleSet exampleSet, String parameterString) {
		Iterator<Attribute> iterator = exampleSet.getAttributes().allAttributes();
		while (iterator.hasNext()) {
			Attribute attribute = iterator.next();
			if (attribute.getName().matches(parameterString)) {
				checkedAttributes.add(attribute);
			}
		}
	}

	/** Returns true if the example does not contain missing values within regarded attributes. */
	@Override
	public boolean conditionOk(Example example) {
		boolean isOk = true;
		for (Attribute attribute : checkedAttributes) {
			isOk &= !Double.isNaN(example.getValue(attribute));
		}
		return isOk;
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

}
