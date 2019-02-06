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

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.preprocessing.GuessValueTypes;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.AboveOperatorVersionCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.StrictDecimalFormat;
import com.rapidminer.tools.math.container.Range;


/**
 * <p>
 * This operator transforms nominal attributes into numerical ones. In contrast to the
 * NominalToNumeric operator, this operator directly parses numbers from the wrongly as nominal
 * values encoded values. Please note that this operator will first check the stored nominal
 * mappings for all attributes. If (old) mappings are still stored which actually are nominal
 * (without the corresponding data being part of the example set), the attribute will not be
 * converted. Please use the operator {@link GuessValueTypes} in these cases.
 * </p>
 *
 * @author Regina Fritsch, Ingo Mierswa
 */
public class NominalNumbers2Numerical extends AbstractFilteredDataProcessing {

	/** Last version where unparsable values were being ignored. */
	public static final OperatorVersion CHANGE_6_0_3_UNPARSABLE_VALUES_ACTION = new OperatorVersion(6, 0, 3);

	/**
	 * The parameter name for &quot;Character that is used as decimal point.&quot;
	 */
	public static final String PARAMETER_DECIMAL_POINT_CHARACTER = "decimal_point_character";

	/** Used for separation of digits (1,000,000.0 or 1.000.000,0) . */
	public static final String PARAMETER_GROUP_SEPARATOR = "group_separator";

	/** Allow unparsable values, use missing values instead. */
	public static final String PARAMETER_UNPARSABLE_VALUE_HANDLING = "unparsable_value_handling";

	public static final String[] UNPARSABLE_VALUES_HANDLING_METHOD = new String[] { "skip attribute", "fail",
			"replace with missing values" };

	public static final int IGNORE = 0;
	public static final int COMPLAIN = 1;
	public static final int REPLACE_WITH_MISSING_VALUES = 2;

	public NominalNumbers2Numerical(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSetMetaData applyOnFilteredMetaData(ExampleSetMetaData emd) throws UndefinedParameterError {
		NumberFormat format = makeFormat();

		Iterator<AttributeMetaData> iterator = emd.getAllAttributes().iterator();
		List<AttributeMetaData> affectedList = new LinkedList<>();
		while (iterator.hasNext()) {
			AttributeMetaData amd = iterator.next();
			if (amd.isNominal()) {
				Set<String> values = amd.getValueSet();
				// check if values are transformed
				boolean isTransformed = true;
				double min = Double.POSITIVE_INFINITY;
				double max = Double.NEGATIVE_INFINITY;

				try {
					for (String value : values) {
						double numValue = format.parse(value).doubleValue();
						min = Math.min(min, numValue);
						max = Math.max(max, numValue);
					}
				} catch (ParseException e) {
					isTransformed = false;
				}

				if (isTransformed) {
					// removing and inserting in order to reflect correct order
					iterator.remove();
					affectedList.add(amd);

					// transform attribute
					amd.setType(Ontology.NUMERICAL);
					if (min == Double.POSITIVE_INFINITY) {
						min = Double.NEGATIVE_INFINITY;
					}
					if (max == Double.NEGATIVE_INFINITY) {
						max = Double.POSITIVE_INFINITY;
					}
					amd.setValueRange(new Range(min, max), SetRelation.EQUAL);
				}
			}
		}
		emd.addAllAttributes(affectedList);
		return emd;
	}

	@Override
	public ExampleSet applyOnFiltered(ExampleSet exampleSet) throws OperatorException {
		int unparsableValueHandling = getParameterAsInt(PARAMETER_UNPARSABLE_VALUE_HANDLING);
		NumberFormat format = makeFormat();

		List<Attribute> newAttributes = new LinkedList<>();
		// using iterator for avoiding "concurrent modification"
		Iterator<Attribute> a = exampleSet.getAttributes().iterator();
		while (a.hasNext()) {
			Attribute attribute = a.next();
			if (attribute.isNominal()) {
				if (getCompatibilityLevel().isAtMost(CHANGE_6_0_3_UNPARSABLE_VALUES_ACTION)
						|| unparsableValueHandling == IGNORE) {
					try {
						for (String value : attribute.getMapping().getValues()) {
							format.parse(value);
						}
					} catch (ParseException e) {
						// only if unparsable values should be ignored:
						// run next iteration if the value can not be parsed to a number
						continue;
					}
				}

				// new attribute
				Attribute newAttribute = AttributeFactory.createAttribute(Ontology.NUMERICAL);
				exampleSet.getExampleTable().addAttribute(newAttribute);
				newAttributes.add(newAttribute);

				// copy data
				for (Example e : exampleSet) {
					double oldValue = e.getValue(attribute);
					if (!Double.isNaN(oldValue)) {
						String value = e.getValueAsString(attribute);
						try {
							e.setValue(newAttribute, format.parse(value).doubleValue());
						} catch (ParseException ex) {
							switch (unparsableValueHandling) {

								case IGNORE:
									continue;
								default:
								case COMPLAIN:
									throw new UserError(this, ex, 946, value);
								case REPLACE_WITH_MISSING_VALUES:
									e.setValue(newAttribute, Double.NaN);
									break;
							}
						}
					} else {
						e.setValue(newAttribute, Double.NaN);
					}
				}

				// delete attribute and rename the new attribute
				a.remove();
				newAttribute.setName(attribute.getName());

			}
		}

		for (Attribute attribute : newAttributes) {
			exampleSet.getAttributes().addRegular(attribute);
		}

		return exampleSet;
	}

	private NumberFormat makeFormat() throws UndefinedParameterError {
		StrictDecimalFormat format = StrictDecimalFormat.getInstance(this);
		return format;
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.NOMINAL };
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(StrictDecimalFormat.getParameterTypes(this));
		ParameterType type;
		type = new ParameterTypeCategory(PARAMETER_UNPARSABLE_VALUE_HANDLING,
				"This selects the method for handling occurrences of values which are not parsable to numbers.",
				UNPARSABLE_VALUES_HANDLING_METHOD, COMPLAIN, false);
		type.registerDependencyCondition(new AboveOperatorVersionCondition(this, CHANGE_6_0_3_UNPARSABLE_VALUES_ACTION));
		types.add(type);

		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return new OperatorVersion[] { CHANGE_6_0_3_UNPARSABLE_VALUES_ACTION };
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				NominalNumbers2Numerical.class, null);
	}

}
