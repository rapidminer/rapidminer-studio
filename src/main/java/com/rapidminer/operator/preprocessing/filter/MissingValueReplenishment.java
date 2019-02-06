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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.ParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * Replaces missing values in examples. If a value is missing, it is replaced by one of the
 * functions &quot;minimum&quot;, &quot;maximum&quot;, &quot;average&quot;, and &quot;none&quot;,
 * which is applied to the non missing attribute values of the example set. &quot;none&quot; means,
 * that the value is not replaced. The function can be selected using the parameter list
 * <code>columns</code>. If an attribute's name appears in this list as a key, the value is used as
 * the function name. If the attribute's name is not in the list, the function specified by the
 * <code>default</code> parameter is used. For nominal attributes the mode is used for the average,
 * i.e. the nominal value which occurs most often in the data. For nominal attributes and
 * replacement type zero the first nominal value defined for this attribute is used. The
 * replenishment &quot;value&quot; indicates that the user defined parameter should be used for the
 * replacement.
 *
 * @author Ingo Mierswa, Simon Fischer, Marius Helf
 */
public class MissingValueReplenishment extends ValueReplenishment {

	/**
	 * The parameter name for &quot;This value is used for some of the replenishment types.&quot;
	 */
	public static final String PARAMETER_REPLENISHMENT_VALUE = "replenishment_value";

	private static final int NONE = 0;

	private static final int MINIMUM = 1;

	private static final int MAXIMUM = 2;

	private static final int AVERAGE = 3;

	private static final int ZERO = 4;

	private static final int VALUE = 5;

	private static final String[] REPLENISHMENT_NAMES = { "none", "minimum", "maximum", "average", "zero", "value" };

	public static final OperatorVersion VERSION_BEFORE_ROUND_ON_INTEGER_ATTRIBUTES = new OperatorVersion(5, 2, 0);

	public MissingValueReplenishment(OperatorDescription description) {
		super(description);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.rapidminer.operator.Operator#getIncompatibleVersionChanges()
	 */
	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] oldIncompatibleVersionChanges = super.getIncompatibleVersionChanges();
		OperatorVersion[] newIncompatibleVersionChanges = new OperatorVersion[oldIncompatibleVersionChanges.length + 1];
		for (int i = 0; i < oldIncompatibleVersionChanges.length; ++i) {
			newIncompatibleVersionChanges[i] = oldIncompatibleVersionChanges[i];
		}
		newIncompatibleVersionChanges[newIncompatibleVersionChanges.length - 1] = VERSION_BEFORE_ROUND_ON_INTEGER_ATTRIBUTES;
		return newIncompatibleVersionChanges;
	}

	private static boolean doesReplenishmentSupportValueType(int replenishment, int valueType) {

		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NOMINAL)) {
			// don't support MINIMUM, MAXIMUM, ZERO for NOMINAL attributes
			switch (replenishment) {
				case MINIMUM:
				case MAXIMUM:
				case ZERO:
					return false;
			}
		} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.DATE_TIME)) {
			// don't support AVERAGE for DATE_TIME attributes
			switch (replenishment) {
				case AVERAGE:
					return false;
			}
		}
		return true;
	}

	@Override
	protected void checkSelectedSubsetMetaData(ExampleSetMetaData subsetMetaData) {
		super.checkSelectedSubsetMetaData(subsetMetaData);

		int replenishment;
		try {
			replenishment = getParameterAsInt(PARAMETER_DEFAULT);
		} catch (UndefinedParameterError e) {
			// should never happen
			return;
		}
		Set<AttributeMetaData> unsupportedAttributes = new HashSet<AttributeMetaData>();
		for (AttributeMetaData amd : subsetMetaData.getAllAttributes()) {
			if (!doesReplenishmentSupportValueType(replenishment, amd.getValueType())) {
				unsupportedAttributes.add(amd);
			}
		}
		if (!unsupportedAttributes.isEmpty()) {
			StringBuilder builder = new StringBuilder();
			boolean first = true;
			for (AttributeMetaData amd : unsupportedAttributes) {
				if (!first) {
					builder.append(", ");
				} else {
					first = false;
				}
				builder.append("\"");
				builder.append(amd.getName());
				builder.append("\"");
			}
			getExampleSetInputPort().addError(
					new SimpleMetaDataError(Severity.WARNING, getExampleSetInputPort(),
					"missing_value_replenishment.value_type_not_supported_by_replenishment",
					REPLENISHMENT_NAMES[replenishment], builder.toString()));
		}
	}

	@Override
	protected Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd)
			throws UndefinedParameterError {
		if (doesReplenishmentSupportValueType(getParameterAsInt(PARAMETER_DEFAULT), amd.getValueType())) {
			amd.setNumberOfMissingValues(new MDInteger(0));
		}
		return Collections.singletonList(amd);
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.VALUE_TYPE };
	}

	@Override
	public String[] getFunctionNames() {
		return REPLENISHMENT_NAMES;
	}

	@Override
	public int getDefaultFunction() {
		return AVERAGE;
	}

	@Override
	public int getDefaultColumnFunction() {
		return AVERAGE;
	}

	@Override
	public double getReplacedValue() {
		return Double.NaN;
	}

	@Override
	public double getReplenishmentValue(int functionIndex, ExampleSet exampleSet, Attribute attribute) throws UserError {
		if (!doesReplenishmentSupportValueType(functionIndex, attribute.getValueType())) {
			logWarning("function \"" + REPLENISHMENT_NAMES[functionIndex] + "\" does not support attribute \""
					+ attribute.getName() + "\" of type \""
					+ Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(attribute.getValueType())
					+ "\". Ignoring missing values of this attribute.");
			return Double.NaN;
		}

		// no need to check for incompatibe valueTypes/functions, since we already did that above
		switch (functionIndex) {
			case NONE:
				return Double.NaN;
			case MINIMUM:
				final double min = exampleSet.getStatistics(attribute, Statistics.MINIMUM);
				return min;
			case MAXIMUM:
				final double max = exampleSet.getStatistics(attribute, Statistics.MAXIMUM);
				return max;
			case AVERAGE:
				if (attribute.isNominal()) {
					final double mode = exampleSet.getStatistics(attribute, Statistics.MODE);
					return mode;
				} else {
					double average = exampleSet.getStatistics(attribute, Statistics.AVERAGE);

					average = getProperlyRoundedValue(attribute, average);
					return average;
				}
			case ZERO:
				return 0.0d;
			case VALUE:
				String valueString = getParameterAsString(PARAMETER_REPLENISHMENT_VALUE);
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
					String formatString = null;
					if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE)) {
						formatString = ParameterTypeDateFormat.DATE_FORMAT_MM_DD_YYYY;
					} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.TIME)) {
						formatString = "hh.mm a";
					} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
						formatString = "MM/dd/yyyy hh.mm a";
					}
					SimpleDateFormat dateFormat = new SimpleDateFormat(formatString, Locale.US);
					try {
						Date date = dateFormat.parse(valueString);
						return date.getTime();
					} catch (ParseException e) {
						throw new UserError(this, 218, PARAMETER_REPLENISHMENT_VALUE, valueString);
					}
				} else if (attribute.isNominal()) {
					int categoryValue = attribute.getMapping().getIndex(valueString);
					if (categoryValue < 0) {
						if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.BINOMINAL)) {
							if (attribute.getMapping().size() < 2) {
								// clone mapping if possible to add additional value
								attribute.setMapping((NominalMapping) attribute.getMapping().clone());
							}
							return attribute.getMapping().mapString(valueString);
						} else {
							// attribute#setMapping clones the input parameter for polynomial attributes
							attribute.setMapping(attribute.getMapping());
							return attribute.getMapping().mapString(valueString);
						}
					} else {
						return categoryValue;
					}
				} else {	// any numerical type
					try {
						double value = Double.parseDouble(valueString);

						if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.INTEGER)
								&& !getCompatibilityLevel().isAtMost(VERSION_BEFORE_ROUND_ON_INTEGER_ATTRIBUTES)) {
							if (value != Math.round(value)) {
								throw new UserError(this, 225, PARAMETER_REPLENISHMENT_VALUE, valueString);
							}
						}

						return value;
					} catch (NumberFormatException e) {
						throw new UserError(this, 211, PARAMETER_REPLENISHMENT_VALUE, valueString);
					}
				}
			default:
				throw new RuntimeException("Illegal value functionIndex: " + functionIndex);
		}
	}

	/**
	 * @param attribute
	 * @param average2
	 * @return
	 */
	private double getProperlyRoundedValue(Attribute attribute, double value) {
		if (getCompatibilityLevel().isAtMost(VERSION_BEFORE_ROUND_ON_INTEGER_ATTRIBUTES)) {
			return value;
		} else {
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.INTEGER)) {
				return Math.round(value);
			} else {
				return value;
			}
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterTypeString type = new ParameterTypeString(PARAMETER_REPLENISHMENT_VALUE,
				"This value is used for some of the replenishment types.", true, false);
		type.registerDependencyCondition(new ParameterCondition(this, PARAMETER_DEFAULT, true) {

			@Override
			public boolean isConditionFullfilled() {
				// check if any of the options is set to value
				try {
					if (getParameterAsInt(PARAMETER_DEFAULT) == VALUE) {
						return true;
					}
					List<String[]> pairs = getParameterList(PARAMETER_COLUMNS);
					if (pairs != null) {
						for (String[] pair : pairs) {
							if (pair[1].equals("value") || pair[1].equals("" + VALUE)) {
								return true;
							}
						}
					}
				} catch (UndefinedParameterError e) {
				}
				return false;
			}
		});
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
				MissingValueReplenishment.class, attributeSelector);
	}

}
