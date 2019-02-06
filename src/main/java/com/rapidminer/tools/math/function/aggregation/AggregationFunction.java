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
package com.rapidminer.tools.math.function.aggregation;

import com.rapidminer.example.Attribute;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.tools.Ontology;


/**
 * An aggregation function which calculates the value for a given value array.
 * 
 * @author Tobias Malbrecht, Ingo Mierswa
 * 
 */
public interface AggregationFunction {

	/**
	 * Returns the name of the aggregation function.
	 */
	public String getName();

	/**
	 * Consider a new value and a corresponding weight by updating counters.
	 */
	public void update(double value, double weight);

	/**
	 * Consider a new value by updating counters.
	 */
	public void update(double value);

	/**
	 * Returns the function value.
	 */
	public double getValue();

	/**
	 * Calculate function value for given values.
	 * 
	 * ATTENTION: counters might be reset and hence value history might be lost!
	 */
	public double calculate(double[] values);

	/**
	 * Calculate function value for given values and weights.
	 * 
	 * ATTENTION: counters might be reset and hence value history might be lost!
	 */
	public double calculate(double[] values, double[] weights);

	/**
	 * Returns whether this function supports the given attribute.
	 */
	public boolean supportsAttribute(Attribute attribute);

	/**
	 * Returns whether this function supports the given attribute tested on the meta data.
	 */
	public boolean supportsAttribute(AttributeMetaData amd);

	/**
	 * Returns whether this function supports attributes of the given type, where valueType is one
	 * of the static value types defined in {@link Ontology}.
	 */
	public boolean supportsValueType(int valueType);

	/**
	 * Returns the result type of this {@link AggregationFunction} when applied on data of type
	 * inputType as one of the static value types defined in {@link Ontology}.
	 */
	public int getValueTypeOfResult(int inputType);

	/**
	 * Sets the target attribute for this aggregation function.
	 * Allows for nominal target attributes to set up the mapping.
	 *
	 * @param attribute
	 * 		the target attribute
	 * @since 9.0.0
	 */
	default void setTargetAttribute(Attribute attribute) {}
}
