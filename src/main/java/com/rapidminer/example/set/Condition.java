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
import com.rapidminer.operator.tools.ExpressionEvaluationException;

import java.io.Serializable;


/**
 * Objects implementing this interface are used by
 * {@link com.rapidminer.example.set.ConditionedExampleSet}s, a special sub class of
 * {@link com.rapidminer.example.ExampleSet} that skips all examples that do not fulfill this
 * condition. In order for the
 * {@link com.rapidminer.example.set.ConditionedExampleSet#createCondition(String, ExampleSet, String)}
 * factory method to be able to create instances of an implementation of Condition, it must
 * implement a two argument constructor taking an {@link ExampleSet} and a parameter String. The
 * meaning of the parameter string is dependent on the implementation and may even be ignored,
 * although it would be nice to print a warning.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public interface Condition extends Serializable {

	/**
	 * Should return true if the given example does fulfill this condition.
	 * 
	 * @param example
	 * @return
	 * @throws ExpressionEvaluationException
	 *             if the condition cannot be evaluated
	 */
	public boolean conditionOk(Example example) throws ExpressionEvaluationException;

	/**
	 * Returns a duplicate of this condition. Subclasses which cannot dynamically changed can also
	 * return the same object.
	 * 
	 * @deprecated Conditions should not be able to be changed dynamically and hence there is no
	 *             need for a copy
	 */
	@Deprecated
	public Condition duplicate();

}
