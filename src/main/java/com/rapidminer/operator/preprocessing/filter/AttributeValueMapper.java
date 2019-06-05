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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.preprocessing.AbstractValueProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeRegexp;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.math.container.Range;


/**
 * <p>
 * This operator takes an <code>ExampleSet</code> as input and maps the values of certain attributes
 * to other values. The operator can replace nominal values (e.g. replace the value
 * &quot;green&quot; by the value &quot;green_color&quot;) as well as numerical values (e.g. replace
 * the all values &quot;3&quot; by &quot;-1&quot;). A single mapping can be specified using the
 * parameters <code>replace_what</code> and <code>replace_by</code>. Multiple mappings can be
 * specified in the parameter list <code>value_mappings</code>.
 * </p>
 *
 * <p>
 * Additionally, the operator allows to define (and consider) a default mapping. If
 * <code>add_default_mapping</code> is set to true and <code>default_value</code> is properly set,
 * all values that occur in the example set but are not listed in the value mappings list are
 * replaced by the default value. This may be helpful in cases where only some values should be
 * mapped explicitly and many unimportant values should be mapped to a default value (e.g. "other").
 * </p>
 *
 * <p>
 * If the parameter <code>consider_regular_expressions</code> is enabled, the values are replaced by
 * the new values if the original values match the given regular expressions. The value
 * corresponding to the first matching regular expression in the mappings list is taken as
 * replacement.
 * </p>
 *
 * <p>
 * This operator supports regular expressions for the attribute names, i.e. the value mapping is
 * applied on all attributes for which the name fulfills the pattern defined by the name expression.
 * </p>
 *
 * @author Tobias Malbrecht
 */
public class AttributeValueMapper extends AbstractValueProcessing {

	public static final String PARAMETER_NEW_VALUES = "new_value";

	/** The parameter name for &quot;The first value which should be merged.&quot; */
	public static final String PARAMETER_VALUE_MAPPINGS = "value_mappings";

	/** The parameter name for &quot;The second value which should be merged.&quot; */
	public static final String PARAMETER_OLD_VALUES = "old_values";

	/**
	 * The parameter name for &quot;All occurrences of this value will be replaced.&quot;
	 *
	 * @deprecated since 9.3; use {@link ParameterTypeRegexp#PARAMETER_REPLACE_WHAT} instead
	 */
	@Deprecated
	public static final String PARAMETER_REPLACE_WHAT = ParameterTypeRegexp.PARAMETER_REPLACE_WHAT;

	/**
	 * The parameter name for &quot;The new attribute value to use.&quot;
	 *
	 * @deprecated since 9.3; use {@link ParameterTypeRegexp#PARAMETER_REPLACE_BY} instead
	 */
	@Deprecated
	public static final String PARAMETER_REPLACE_BY = ParameterTypeRegexp.PARAMETER_REPLACE_BY;

	/**
	 * The parameter name for &quot;Enables matching based on regular expressions; original values
	 * may be specified as regular expressions.&quot
	 */
	public static final String PARAMETER_CONSIDER_REGULAR_EXPRESSIONS = "consider_regular_expressions";

	/**
	 * The parameter name for &quot;If set to true, all original values which are not listed in the
	 * value mappings list are mapped to the default value.&quot;
	 */
	public static final String PARAMETER_ADD_DEFAULT_MAPPING = "add_default_mapping";

	/**
	 * The parameter name for &quot;The default value all original values are mapped to, if
	 * add_default_mapping is set to true.&quot;
	 */
	public static final String PARAMETER_DEFAULT_VALUE = "default_value";

	public AttributeValueMapper(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSetMetaData applyOnFilteredMetaData(ExampleSetMetaData emd) {
		try {
			if (emd.getAllAttributes().isEmpty()) {
				return emd;
			}
			boolean first = true;
			boolean nominal = false;
			boolean dataTypeUnknown = true;
			for (AttributeMetaData amd : emd.getAllAttributes()) {
				if (first) {
					nominal = amd.isNominal();
					if (amd.getValueType() != Ontology.ATTRIBUTE_VALUE) {
						dataTypeUnknown = false;
					}
					first = false;
				} else {
					if (nominal != amd.isNominal()) {
						this.addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(),
								"attributes_must_have_same_type"));
						return emd;
					}
				}
			}

			boolean useValueRegex = getParameterAsBoolean(PARAMETER_CONSIDER_REGULAR_EXPRESSIONS);
			List<String[]> mappingParameterList = getParameterList(PARAMETER_VALUE_MAPPINGS);

			String replaceWhat = getParameterAsString(ParameterTypeRegexp.PARAMETER_REPLACE_WHAT);
			String replaceBy = getParameterAsString(ParameterTypeRegexp.PARAMETER_REPLACE_BY);
			HashMap<String, String> mappings = new LinkedHashMap<String, String>();
			HashMap<Pattern, String> patternMappings = new LinkedHashMap<Pattern, String>();
			if (replaceWhat != null && replaceBy != null && !replaceWhat.equals("") && !replaceBy.equals("")) {
				mappings.put(replaceWhat, replaceBy);
				if (useValueRegex) {
					try {
						Pattern valuePattern = Pattern.compile(replaceWhat);
						patternMappings.put(valuePattern, replaceBy);
					} catch (PatternSyntaxException e) {
					}
				}
			}
			Iterator<String[]> listIterator = mappingParameterList.iterator();
			while (listIterator.hasNext()) {
				String[] pair = listIterator.next();
				replaceWhat = pair[0];
				replaceBy = pair[1];
				mappings.put(replaceWhat, replaceBy);
				if (useValueRegex) {
					try {
						Pattern valuePattern = Pattern.compile(replaceWhat);
						patternMappings.put(valuePattern, replaceBy);
					} catch (PatternSyntaxException e) {
						continue;
					}
				}
			}

			boolean defaultMappingAdded = getParameterAsBoolean(PARAMETER_ADD_DEFAULT_MAPPING);
			String defaultValue = getParameterAsString(PARAMETER_DEFAULT_VALUE);

			if (nominal) {
				for (AttributeMetaData amd : emd.getAllAttributes()) {
					Set<String> valueSet = new TreeSet<String>();
					for (String value : amd.getValueSet()) {
						String mappedValue = mappings.get(value);
						if (useValueRegex) {
							for (Entry<Pattern, String> patternEntry : patternMappings.entrySet()) {
								Matcher matcher = patternEntry.getKey().matcher(value);
								if (matcher.matches()) {
									mappedValue = patternEntry.getValue();
									break;
								}
							}
						}
						if (mappedValue == null) {
							if (defaultMappingAdded) {
								if (defaultValue.equals("?")) {
								} else {
									valueSet.add(defaultValue);
								}
							} else {
								valueSet.add(value);
							}
						} else {
							valueSet.add(mappedValue);
						}
					}
					amd.setValueSet(valueSet, SetRelation.SUBSET);
				}
			} else if (!dataTypeUnknown) {
				HashMap<Double, Double> numericalValueMapping = new HashMap<Double, Double>();
				for (Entry<String, String> entry : mappings.entrySet()) {
					double oldValue = Double.NaN;
					double newValue = Double.NaN;
					if (!entry.getKey().equals("?")) {
						try {
							oldValue = Double.valueOf(entry.getKey());
						} catch (NumberFormatException e) {
							this.addError(new SimpleProcessSetupError(Severity.ERROR, AttributeValueMapper.this
									.getPortOwner(), "mapping_must_be_number", entry.getKey()));
							break;
						}
					}
					if (!entry.getValue().equals("?")) {
						try {
							newValue = Double.valueOf(entry.getValue());
						} catch (NumberFormatException e) {
							this.addError(new SimpleProcessSetupError(Severity.ERROR, AttributeValueMapper.this
									.getPortOwner(), "mapping_must_be_number", entry.getValue()));
							break;
						}
					}
					numericalValueMapping.put(oldValue, newValue);
				}
				double numericalDefaultValue = Double.NaN;
				if (defaultMappingAdded && !defaultValue.equals("?")) {
					numericalDefaultValue = Double.valueOf(defaultValue);
				}

				for (AttributeMetaData amd : emd.getAllAttributes()) {
					double lower = amd.getValueRange().getLower();
					double upper = amd.getValueRange().getUpper();
					double mappedLower = Double.POSITIVE_INFINITY;
					double mappedUpper = Double.NEGATIVE_INFINITY;
					for (Double value : numericalValueMapping.values()) {
						if (value < mappedLower) {
							mappedLower = value;
						}
						if (value > mappedUpper) {
							mappedUpper = value;
						}
					}
					if (!Double.isNaN(numericalDefaultValue) && numericalDefaultValue < mappedLower) {
						mappedLower = numericalDefaultValue;
					}
					if (!Double.isNaN(numericalDefaultValue) && numericalDefaultValue > mappedUpper) {
						mappedUpper = numericalDefaultValue;
					}
					amd.setValueRange(new Range(Math.min(lower, mappedLower), Math.max(upper, mappedUpper)),
							SetRelation.SUBSET);
				}
			}
		} catch (UndefinedParameterError e) {
		}

		return emd;
	}

	@Override
	public ExampleSet applyOnFiltered(ExampleSet exampleSet) throws OperatorException {
		boolean first = true;
		boolean nominal = false;
		LinkedHashMap<Attribute, Attribute> attributeMap = new LinkedHashMap<Attribute, Attribute>();
		for (Attribute oldAttribute : exampleSet.getAttributes()) {
			if (first) {
				nominal = oldAttribute.isNominal();
				first = false;
			} else {
				if (nominal != oldAttribute.isNominal()) {
					throw new UserError(this, 126);
				}
			}
			Attribute newAttribute = AttributeFactory.createAttribute(oldAttribute.getValueType());
			attributeMap.put(oldAttribute, newAttribute);
		}

		boolean useValueRegex = getParameterAsBoolean(PARAMETER_CONSIDER_REGULAR_EXPRESSIONS);
		List<String[]> mappingParameterList = getParameterList(PARAMETER_VALUE_MAPPINGS);

		String replaceWhat = getParameterAsString(ParameterTypeRegexp.PARAMETER_REPLACE_WHAT);
		String replaceBy = getParameterAsString(ParameterTypeRegexp.PARAMETER_REPLACE_BY);
		HashMap<String, String> mappings = new LinkedHashMap<String, String>();
		HashMap<Pattern, String> patternMappings = new LinkedHashMap<Pattern, String>();
		if (replaceWhat != null && replaceBy != null && !replaceWhat.equals("") && !replaceBy.equals("")) {
			mappings.put(replaceWhat, replaceBy);
			if (useValueRegex) {
				try {
					Pattern valuePattern = Pattern.compile(replaceWhat);
					patternMappings.put(valuePattern, replaceBy);
				} catch (PatternSyntaxException e) {
					throw new UserError(this, 206, replaceWhat, e.getMessage());
				}
			}
		}
		Iterator<String[]> listIterator = mappingParameterList.iterator();
		boolean entered = false;
		while (listIterator.hasNext()) {
			String[] pair = listIterator.next();
			replaceWhat = pair[0];
			replaceBy = pair[1];
			mappings.put(replaceWhat, replaceBy);
			if (useValueRegex) {
				try {
					Pattern valuePattern = Pattern.compile(replaceWhat);
					patternMappings.put(valuePattern, replaceBy);
				} catch (PatternSyntaxException e) {
					if (!entered) {
						entered = true;
						throw new UserError(this, 206, replaceWhat, e.getMessage());
					}
				}
			}
		}

		boolean defaultMappingAdded = getParameterAsBoolean(PARAMETER_ADD_DEFAULT_MAPPING);
		String defaultValue = getParameterAsString(PARAMETER_DEFAULT_VALUE);
		if (defaultMappingAdded) {
			if (defaultValue == null || defaultValue.equals("")) {
				throw new UserError(this, 201,
						new Object[] { PARAMETER_ADD_DEFAULT_MAPPING, "true", PARAMETER_DEFAULT_VALUE });
			}
		}

		if (attributeMap.size() > 0) {
			if (nominal) {
				for (Entry<Attribute, Attribute> entry : attributeMap.entrySet()) {
					Attribute oldAttribute = entry.getKey();
					Attribute newAttribute = entry.getValue();
					exampleSet.getExampleTable().addAttribute(newAttribute);
					exampleSet.getAttributes().addRegular(newAttribute);
					for (Example example : exampleSet) {
						double value = example.getValue(oldAttribute);
						String stringValue = null;
						if (Double.isNaN(value)) {
							stringValue = "?";
						} else {
							stringValue = oldAttribute.getMapping().mapIndex((int) value);
						}
						String mappedValue = mappings.get(stringValue);
						if (useValueRegex) {
							for (Entry<Pattern, String> patternEntry : patternMappings.entrySet()) {
								Matcher matcher = patternEntry.getKey().matcher(stringValue);
								if (matcher.matches()) {
									mappedValue = patternEntry.getValue();
									break;
								}
							}
						}
						if (mappedValue == null) {
							if (stringValue.equals("?")) {
								example.setValue(newAttribute, Double.NaN);
							} else {
								if (defaultMappingAdded) {
									if (defaultValue.equals("?")) {
										example.setValue(newAttribute, Double.NaN);
									} else {
										example.setValue(newAttribute, defaultValue);
									}
								} else {
									example.setValue(newAttribute, newAttribute.getMapping().mapString(stringValue));
								}
							}
						} else {
							if (mappedValue.equals("?")) {
								example.setValue(newAttribute, Double.NaN);
							} else {
								example.setValue(newAttribute, newAttribute.getMapping().mapString(mappedValue));
							}
						}
						checkForStop();
					}
					exampleSet.getAttributes().remove(oldAttribute);
					newAttribute.setName(oldAttribute.getName());
				}
			} else {
				HashMap<Double, Double> numericalValueMapping = new HashMap<Double, Double>();
				for (Entry<String, String> entry : mappings.entrySet()) {
					double oldValue = Double.NaN;
					double newValue = Double.NaN;
					if (!entry.getKey().equals("?")) {
						try {
							oldValue = Double.valueOf(entry.getKey());
						} catch (NumberFormatException e) {
							continue;
						}
					}
					if (!entry.getValue().equals("?")) {
						try {
							newValue = Double.valueOf(entry.getValue());
						} catch (NumberFormatException e) {
							continue;
						}
					}
					numericalValueMapping.put(oldValue, newValue);
				}
				double numericalDefaultValue = Double.NaN;
				if (defaultMappingAdded && !defaultValue.equals("?")) {
					numericalDefaultValue = Double.valueOf(defaultValue);
				}
				for (Entry<Attribute, Attribute> entry : attributeMap.entrySet()) {
					Attribute oldAttribute = entry.getKey();
					Attribute newAttribute = entry.getValue();
					exampleSet.getExampleTable().addAttribute(newAttribute);
					exampleSet.getAttributes().addRegular(newAttribute);
					for (Example example : exampleSet) {
						double value = example.getValue(oldAttribute);
						Double mappedValue = numericalValueMapping.get(Double.valueOf(value));
						if (mappedValue == null) {
							if (defaultMappingAdded) {
								example.setValue(newAttribute, numericalDefaultValue);
							} else {
								example.setValue(newAttribute, value);
							}
						} else {
							example.setValue(newAttribute, mappedValue);
						}
						checkForStop();
					}
					exampleSet.getAttributes().remove(oldAttribute);
					newAttribute.setName(oldAttribute.getName());
				}
			}
		}

		return exampleSet;
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.ATTRIBUTE_VALUE };
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeList(PARAMETER_VALUE_MAPPINGS, "The value mappings.", new ParameterTypeString(
				PARAMETER_OLD_VALUES, "The original values which should be replaced.", false), new ParameterTypeString(
				PARAMETER_NEW_VALUES, "Specifies the new value", false));
		type.setExpert(false);
		type.setPrimary(true);
		types.add(type);

		ParameterTypeRegexp regexp = new ParameterTypeRegexp(ParameterTypeRegexp.PARAMETER_REPLACE_WHAT, "All occurrences of this value will be replaced.", true, false);
		types.add(regexp);

		ParameterTypeString replacement = new ParameterTypeString(ParameterTypeRegexp.PARAMETER_REPLACE_BY, "The new attribute value to use.", true);
		regexp.setReplacementParameter(replacement);
		replacement.setExpert(false);
		types.add(replacement);

		types.add(new ParameterTypeBoolean(PARAMETER_CONSIDER_REGULAR_EXPRESSIONS,
				"Enables matching based on regular expressions; original values may be specified as regular expressions.",
				false));

		type = new ParameterTypeBoolean(
				PARAMETER_ADD_DEFAULT_MAPPING,
				"If set to true, all original values which are not listed in the value mappings list are mapped to the default value.",
				false);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeString(PARAMETER_DEFAULT_VALUE,
				"The default value all original values are mapped to, if add_default_mapping is set to true.", true);
		type.setExpert(false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_ADD_DEFAULT_MAPPING, true, true));
		types.add(type);

		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				AttributeValueMapper.class, null);
	}

}
