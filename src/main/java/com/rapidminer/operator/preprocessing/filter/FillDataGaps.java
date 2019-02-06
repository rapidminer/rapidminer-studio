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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.AbstractExampleSetProcessing;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.math.MathFunctions;


/**
 * <p>
 * This operator fills gaps in the data based on the ID attribute of the data set. The ID attribute
 * must either have the value type &quot;integer&quot; or one of the data value types.
 * </p>
 *
 * <p>
 * The operator performs the following steps:
 * </p>
 * <ol>
 * <li>The data is sorted according to the ID attribute</li>
 * <li>All occurring distances between consecutive ID values are calculated</li>
 * <li>The greatest common divisor (GCD) of all distances is calculated</li>
 * <li>All rows which would have an ID value which is a multiple of the GCD but are missing are
 * added to the data set</li>
 * </ol>
 *
 * <p>
 * Please note that all values of attributes beside the ID attribute will have a missing value which
 * often must be replaced as a next step.
 * </p>
 *
 * @author Ingo Mierswa
 */
public class FillDataGaps extends AbstractExampleSetProcessing {

	public static final String PARAMETER_USE_GCD_FOR_STEP_SIZE = "use_gcd_for_step_size";

	public static final String PARAMETER_STEP_SIZE = "step_size";

	public static final String PARAMETER_START = "start";

	public static final String PARAMETER_END = "end";

	public FillDataGaps(OperatorDescription description) {
		super(description);

		getExampleSetInputPort().addPrecondition(
				new ExampleSetPrecondition(getExampleSetInputPort(), Ontology.VALUE_TYPE, Attributes.ID_NAME));
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		metaData.getNumberOfExamples().increaseByUnknownAmount();
		for (AttributeMetaData amd : metaData.getAllAttributes()) {
			if (amd.getRole() == null || !amd.getRole().equals(Attributes.ID_NAME)) {
				amd.getNumberOfMissingValues().increaseByUnknownAmount();
			}
		}
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet inputSet) throws OperatorException {
		// reject to operator on empty example sets.
		if (inputSet.size() == 0) {
			return inputSet;
		}

		// init and checks
		Attribute idAttribute = inputSet.getAttributes().getId();
		if (idAttribute == null) {
			throw new UserError(this, 129);
		}
		if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(idAttribute.getValueType(), Ontology.DATE_TIME)
				&& !Ontology.ATTRIBUTE_VALUE_TYPE.isA(idAttribute.getValueType(), Ontology.INTEGER)) {
			throw new UserError(this, 120, new Object[] { idAttribute.getName(),
					Ontology.VALUE_TYPE_NAMES[idAttribute.getValueType()],
					Ontology.VALUE_TYPE_NAMES[Ontology.DATE_TIME] + " or " + Ontology.VALUE_TYPE_NAMES[Ontology.INTEGER] });
		}

		// sort data according to ID attribute
		Sorting sorting = null;
		try {
			sorting = OperatorService.createOperator(Sorting.class);
		} catch (OperatorCreationException e) {
			throw new OperatorException(getName() + ": Cannot create discretization operator (" + e + ").");
		}
		sorting.setParameter(Sorting.PARAMETER_ATTRIBUTE_NAME, idAttribute.getName());
		ExampleSet sortedSet = sorting.apply(inputSet);

		// determine step size
		long stepSize = 1;
		if (!getParameterAsBoolean(PARAMETER_USE_GCD_FOR_STEP_SIZE)) {
			stepSize = getParameterAsInt(PARAMETER_STEP_SIZE);
		} else {
			// calculate all distances
			List<Long> distances = new LinkedList<Long>();
			boolean first = true;
			long lastValue = 0;

			// start value defined?
			if (isParameterSet(PARAMETER_START)) {
				first = false;
				lastValue = getParameterAsInt(PARAMETER_START);
			}

			// add data distances
			for (Example example : sortedSet) {
				long value = (long) example.getValue(idAttribute);
				if (first) {
					first = false;
				} else {
					if (value > lastValue) {
						distances.add(value - lastValue);
					}
				}
				lastValue = value;
			}

			// end value defined?
			if (isParameterSet(PARAMETER_END)) {
				int endValue = getParameterAsInt(PARAMETER_END);
				if (endValue > lastValue) {
					distances.add(endValue - lastValue);
				}
			}

			// calculate the GCD
			stepSize = MathFunctions.getGCD(distances);
			distances.clear();
		}
		stepSize = Math.abs(stepSize);

		// find gaps
		List<Long> missingValues = new LinkedList<Long>();
		long lastValue = 0;
		boolean first = true;
		long minValue = Long.MAX_VALUE;
		long maxValue = Long.MIN_VALUE;
		for (Example example : sortedSet) {
			long value = (long) example.getValue(idAttribute);
			minValue = Math.min(minValue, value);
			maxValue = Math.max(maxValue, value);
			if (first) {
				first = false;
				lastValue = value;
			} else {
				while (lastValue + stepSize < value) {
					lastValue += stepSize;
					missingValues.add(lastValue);
				}
				lastValue = value;
			}
		}

		if (isParameterSet(PARAMETER_START)) {
			long start = getParameterAsInt(PARAMETER_START);
			if (start < minValue) {
				lastValue = start;
				while (lastValue + stepSize <= minValue) {
					missingValues.add(lastValue);
					lastValue += stepSize;
				}
			}
		}

		if (isParameterSet(PARAMETER_END)) {
			long end = getParameterAsInt(PARAMETER_END);
			if (end > maxValue) {
				lastValue = maxValue + stepSize;
				while (lastValue <= end) {
					missingValues.add(lastValue);
					lastValue += stepSize;
				}
			}
		}

		// create table
		List<Attribute> attributes = new ArrayList<Attribute>(sortedSet.getAttributes().allSize());
		Map<Attribute, String> specialAttributes = new LinkedHashMap<Attribute, String>();
		Iterator<AttributeRole> a = sortedSet.getAttributes().allAttributeRoles();
		int idIndex = -1;
		int index = 0;
		while (a.hasNext()) {
			AttributeRole role = a.next();
			Attribute cloneAttribute = (Attribute) role.getAttribute().clone();
			attributes.add(cloneAttribute);
			if (role.isSpecial()) {
				specialAttributes.put(cloneAttribute, role.getSpecialName());
				if (role.getSpecialName().equals(Attributes.ID_NAME)) {
					idIndex = index;
				}
			}
			index++;
		}
		ExampleSetBuilder builder = ExampleSets.from(attributes).withExpectedSize(sortedSet.size() + missingValues.size());

		// copy data
		for (Example example : sortedSet) {
			double[] data = new double[attributes.size()];
			index = 0;
			Iterator<Attribute> i = sortedSet.getAttributes().allAttributes();
			while (i.hasNext()) {
				data[index++] = example.getValue(i.next());
			}
			builder.addRow(data);
		}

		// create missing rows
		for (long missingValue : missingValues) {
			double[] data = new double[attributes.size()];
			for (int d = 0; d < data.length; d++) {
				data[d] = Double.NaN;
			}
			data[idIndex] = missingValue;
			builder.addRow(data);
		}

		// create final example set
		ExampleSet resultSet = builder.withRoles(specialAttributes).build();

		// sort final result
		resultSet = sorting.apply(resultSet);

		return resultSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(
				PARAMETER_USE_GCD_FOR_STEP_SIZE,
				"Indicates if the greatest common divisor should be calculated and used as the underlying distance between all data points.",
				true));

		ParameterType type = new ParameterTypeInt(PARAMETER_STEP_SIZE,
				"The used step size for filling the gaps (only used if GCD calculation is not checked).", 1,
				Integer.MAX_VALUE, 1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_GCD_FOR_STEP_SIZE, false, false));
		types.add(type);

		types.add(new ParameterTypeInt(
				PARAMETER_START,
				"If this parameter is defined gaps at the beginning (if they occur) before the first data point will also be filled.",
				1, Integer.MAX_VALUE, true));
		types.add(new ParameterTypeInt(
				PARAMETER_END,
				"If this parameter is defined gaps at the end (if they occur) after the last data point will also be filled.",
				1, Integer.MAX_VALUE, true));
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false; // creates new table
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), FillDataGaps.class, null);
	}
}
