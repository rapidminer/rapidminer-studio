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

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;


/**
 * This subclass of {@link Condition} serves to exclude examples with known labels from an example
 * set.
 * 
 * @author Ingo Mierswa ingomierswa Exp $
 */
public class MissingLabelsCondition implements Condition {

	private static final long serialVersionUID = 6559275828082706521L;

	/**
	 * Throws an exception since a parameter string is not allowed for this condition.
	 */
	public MissingLabelsCondition(ExampleSet exampleSet, String parameterString) {}

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

	/** Returns true if the label was not defined. */
	@Override
	public boolean conditionOk(Example example) {
		if (Double.isNaN(example.getValue(example.getAttributes().getLabel()))) {
			return true;
		} else {
			return false;
		}
	}
}
