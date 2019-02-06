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
package com.rapidminer.operator.preprocessing.transformation.aggregation;

import java.util.HashSet;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.table.DataRow;


/**
 * This is an implementation of a Aggregator for numerical attributes. It takes over the handling of
 * missing values.
 * 
 * @author Sebastian Land
 */
public abstract class NumericalAggregator implements Aggregator {

	private Attribute sourceAttribute;
	private boolean ignoreMissings;
	private boolean isMissing = false;
	private boolean isCountingOnlyDistinct = false;
	private HashSet<Double> distinctValueSet = null;


	public NumericalAggregator(AggregationFunction function) {
		if (function != null) {
			this.sourceAttribute = function.getSourceAttribute();
			this.ignoreMissings = function.isIgnoringMissings();
			this.isCountingOnlyDistinct = function.isCountingOnlyDistinct();
			if (isCountingOnlyDistinct) {
				distinctValueSet = new HashSet<>();
			}
		}
	}


	/**
	 * Set whether missing values should be ignored when calling {@link #count(double)}.
	 *
	 * @param ignoreMissings
	 * 		{@code true} if missing values ({@link Double#NaN}) should be skipped; {@code false} otherwise
	 * @since 9.2.0
	 */
	public void setIgnoreMissing(boolean ignoreMissings) {
		this.ignoreMissings = ignoreMissings;
	}


	@Override
	public final void count(Example example) {
		// check whether we have to count at all
		if (!isMissing || ignoreMissings) {
			double value = example.getValue(sourceAttribute);
			if (isMissing && !ignoreMissings || Double.isNaN(value)) {
				isMissing = true;
			} else {
				if (!isCountingOnlyDistinct || distinctValueSet.add(value)) {
					count(value);
				}
			}
		}
	}

	@Override
	public final void count(Example example, double weight) {
		// check whether we have to count at all
		if (!isMissing || ignoreMissings) {
			double value = example.getValue(sourceAttribute);
			if (isMissing && !ignoreMissings || Double.isNaN(value)) {
				isMissing = true;
			} else {
				if (!isCountingOnlyDistinct || distinctValueSet.add(value)) {
					count(value, weight);
				}
			}
		}
	}

	/**
	 * Counts the given value directly, does only respect the ignoreMissings flag, but does not check for distinctness.
	 *
	 * @param value
	 * 		the value
	 * @since 9.2.0
	 */
	public void countDirectly(double value) {
		if (ignoreMissings && Double.isNaN(value)) {
			return;
		}

		count(value);
	}

	/**
	 * Counts the given value directly, does only respect the ignoreMissings flag, but does not check for distinctness.
	 *
	 * @param value
	 * 		the value
	 * @param weight
	 * 		the weight
	 * @since 9.2.0
	 */
	public void countDirectlyWithWeight(double value, double weight) {
		if (ignoreMissings && Double.isNaN(value)) {
			return;
		}

		count(value, weight);
	}

	/**
	 * This method will count the given numerical value. This method will not be called in cases,
	 * where the examples value for the given source Attribute is unknown. Subclasses of this class
	 * will in this cases return either NaN if ignoreMissings is false, or will return the value as
	 * if the examples with the missing aren't present at all.
	 * 
	 * Please see {@link #count(double, double)} for taking weights into account. You may not mix
	 * both methods within one aggregation run, as subclasses might implement more memory efficient
	 * data structures when not using weights.
	 */
	protected abstract void count(double value);

	/**
	 * Same as {@link #count(double)}, but taking the weight into account. You may not mix both
	 * methods within one aggregation run, as subclasses might implement more memory efficient data
	 * structures when not using weights.
	 */
	protected abstract void count(double value, double weight);

	@Override
	public final void set(Attribute attribute, DataRow row) {
		if (isMissing && !ignoreMissings) {
			row.set(attribute, Double.NaN);
		} else {
			row.set(attribute, getValue());
		}
	}

	/**
	 * This method has to return the numerical value of this aggregator.
	 */
	protected abstract double getValue();

	/**
	 * Gets the aggregation result value.
	 *
	 * @return the value.
	 * @since 9.2.0
	 */
	public double getAggregationValue() {
		return getValue();
	}

	/**
	 * Explicitly sets the value of this aggregator. The only place where it makes sense to use this
	 * function is in {@link AggregationFunction#postProcessing(java.util.List)}.
	 * 
	 * The default implementation does nothing.
	 * 
	 * @param value
	 */
	protected void setValue(double value) {
		// do nothing
	}
}
