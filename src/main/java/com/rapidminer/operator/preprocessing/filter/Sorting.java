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

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SortedExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * <p>
 * This operator sorts the given example set according to a single attribute. The example set is
 * sorted according to the natural order of the values of this attribute either in increasing or in
 * decreasing direction.
 * </p>
 *
 * @author Ingo Mierswa
 */
public class Sorting extends AbstractDataProcessing {

	/**
	 * The parameter name for &quot;Indicates the attribute which should be used for determining the
	 * sorting.&quot;
	 */
	public static final String PARAMETER_ATTRIBUTE_NAME = "attribute_name";

	/** The parameter name for &quot;Indicates the direction of the sorting.&quot; */
	public static final String PARAMETER_SORTING_DIRECTION = "sorting_direction";

	public Sorting(OperatorDescription description) {
		super(description);

		getExampleSetInputPort().addPrecondition(new AttributeSetPrecondition(getExampleSetInputPort(),
				AttributeSetPrecondition.getAttributesByParameter(this, PARAMETER_ATTRIBUTE_NAME)));
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		int sortingDirection = getParameterAsInt(PARAMETER_SORTING_DIRECTION);
		Attribute sortingAttribute = exampleSet.getAttributes().get(getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
		// some checks
		if (sortingAttribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE_NAME, getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
		}

		ExampleSet result = new SortedExampleSet(exampleSet, sortingAttribute, sortingDirection, getProgress());

		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_ATTRIBUTE_NAME,
				"Indicates the attribute which should be used for determining the sorting.", getExampleSetInputPort(),
				false));
		types.add(new ParameterTypeCategory(PARAMETER_SORTING_DIRECTION, "Indicates the direction of the sorting.",
				SortedExampleSet.SORTING_DIRECTIONS, SortedExampleSet.INCREASING, false));
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), Sorting.class, null);
	}
}
