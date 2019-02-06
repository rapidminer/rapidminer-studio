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
package com.rapidminer.operator.preprocessing.filter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * Replaces positive and negative infinite values in examples by one of the functions
 * &quot;none&quot;, &quot;zero&quot;, &quot;max_byte&quot;, &quot;max_int&quot;,
 * &quot;max_double&quot;, and &quot;missing&quot;. &quot;none&quot; means, that the value is not
 * replaced. The max_xxx functions replace plus infinity by the upper bound and minus infinity by
 * the lower bound of the range of the Java type xxx. &quot;missing&quot; means, that the value is
 * replaced by nan (not a number), which is internally used to represent missing values. A
 * {@link MissingValueReplenishment} operator can be used to replace missing values by average (or
 * the mode for nominal attributes), maximum, minimum etc. afterwards.<br/>
 * For each attribute, the function can be selected using the parameter list <code>columns</code>.
 * If an attribute's name appears in this list as a key, the value is used as the function name. If
 * the attribute's name is not in the list, the function specified by the <code>default</code>
 * parameter is used.
 *
 * @author Simon Fischer, Ingo Mierswa
 */
public class InfiniteValueReplenishment extends ValueReplenishment {

	public static final String PARAMETER_REPLENISHMENT_VALUE = "replenishment_value";

	public static final String PARAMETER_REPLENISHMENT_WHAT = "replenish_what";

	private static final int NONE = 0;

	private static final int ZERO = 1;

	private static final int MAX_BYTE = 2;

	private static final int MAX_INT = 3;

	private static final int MAX_DOUBLE = 4;

	private static final int MISSING = 5;

	private static final int VALUE = 6;

	private static final String[] REP_NAMES = { "none", "zero", "max_byte", "max_int", "max_double", "missing", "value" };
	private static final String[] WHAT_NAMES = { "positive_infinity", "negative_infinity" };

	public InfiniteValueReplenishment(OperatorDescription description) {
		super(description);
	}

	@Override
	protected Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd)
			throws UndefinedParameterError {
		return Collections.singletonList(amd);
	}

	@Override
	public double getReplacedValue() {
		try {
			int chosen = getParameterAsInt(PARAMETER_REPLENISHMENT_WHAT);
			if (chosen == 0) {
				return Double.POSITIVE_INFINITY;
			}
		} catch (Exception e) {
		}

		return Double.NEGATIVE_INFINITY;
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.NUMERICAL };
	}

	@Override
	public String[] getFunctionNames() {
		return REP_NAMES;
	}

	@Override
	public int getDefaultFunction() {
		return MAX_DOUBLE;
	}

	@Override
	public int getDefaultColumnFunction() {
		return ZERO;
	}

	/**
	 * Replaces the values
	 *
	 * @throws UndefinedParameterError
	 */
	@Override
	public double getReplenishmentValue(int functionIndex, ExampleSet exampleSet, Attribute attribute)
			throws UndefinedParameterError {
		int chosen = getParameterAsInt(PARAMETER_REPLENISHMENT_WHAT);
		switch (functionIndex) {
			case NONE:
				return Double.POSITIVE_INFINITY;
			case ZERO:
				return 0.0;
			case MAX_BYTE:
				return (chosen == 0) ? Byte.MAX_VALUE : Byte.MIN_VALUE;
			case MAX_INT:
				return (chosen == 0) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
			case MAX_DOUBLE:
				return (chosen == 0) ? Double.MAX_VALUE : -Double.MAX_VALUE;
			case MISSING:
				return Double.NaN;
			case VALUE:
				return getParameterAsDouble(PARAMETER_REPLENISHMENT_VALUE);
			default:
				throw new RuntimeException("Illegal value functionIndex: " + functionIndex);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeCategory(PARAMETER_REPLENISHMENT_WHAT,
				"Decides if positive or negative infite values will be replaced.", WHAT_NAMES, 0, false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_REPLENISHMENT_VALUE, "This value will be inserted instead of infinity.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_DEFAULT, getFunctionNames(), true, VALUE));
		types.add(type);
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		// the model takes care of materialization
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				InfiniteValueReplenishment.class, attributeSelector);
	}

}
