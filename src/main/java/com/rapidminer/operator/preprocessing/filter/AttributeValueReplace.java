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
import com.rapidminer.parameter.ParameterTypeRegexp;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * <p>
 * This operator creates new attributes from nominal attributes where the new attributes contain the
 * original values which replaced substrings. The replace_what parameter can be defined as a regular
 * expression (please refer to the annex of the RapidMiner tutorial for a description). The
 * replace_by parameter can be defined as an arbitrary string. Empty strings are also allowed.
 * Capturing groups of the defined regular expression can be accessed with $1, $2, $3...
 * </p>
 *
 * @author Ingo Mierswa, Helge Homburg, Tobias Malbrecht
 */
public class AttributeValueReplace extends AbstractValueProcessing {

	/** @deprecated since 9.3; use {@link ParameterTypeRegexp#PARAMETER_REPLACE_WHAT} instead */
	@Deprecated
	public static final String PARAMETER_REPLACE_WHAT = ParameterTypeRegexp.PARAMETER_REPLACE_WHAT;

	/** @deprecated since 9.3; use {@link ParameterTypeRegexp#PARAMETER_REPLACE_BY} instead */
	@Deprecated
	public static final String PARAMETER_REPLACE_BY = ParameterTypeRegexp.PARAMETER_REPLACE_BY;

	public AttributeValueReplace(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSetMetaData applyOnFilteredMetaData(ExampleSetMetaData emd) throws UndefinedParameterError {
		String replaceWhat = getParameterAsString(ParameterTypeRegexp.PARAMETER_REPLACE_WHAT);
		Pattern whatPattern;
		try {
			whatPattern = Pattern.compile(replaceWhat);
		} catch (PatternSyntaxException e) {
			addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(), "invalid_regex", replaceWhat));
			return emd;
		}
		String replaceBy = "";
		if (isParameterSet(ParameterTypeRegexp.PARAMETER_REPLACE_BY)) {
			replaceBy = getParameterAsString(ParameterTypeRegexp.PARAMETER_REPLACE_BY);
		}
		for (AttributeMetaData amd : emd.getAllAttributes()) {
			Set<String> valueSet = new TreeSet<>();
			for (String value : amd.getValueSet()) {
				Matcher whatMatcher = whatPattern.matcher(value);
				try {
					String replacedValue = whatMatcher.replaceAll(replaceBy);
					if (replacedValue.length() > 0) {
						valueSet.add(replacedValue);
					}
				} catch (StringIndexOutOfBoundsException e) {
					addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(), "invalid_regex_replacement",
							replaceBy));
					return emd;
				}
			}
			amd.setValueSet(valueSet, SetRelation.SUBSET);
		}
		return emd;
	}

	@Override
	public ExampleSet applyOnFiltered(ExampleSet exampleSet) throws OperatorException {
		String replaceWhat = getParameterAsString(ParameterTypeRegexp.PARAMETER_REPLACE_WHAT);
		Pattern whatPattern;
		try {
			whatPattern = Pattern.compile(replaceWhat);
		} catch (PatternSyntaxException e) {
			throw new UserError(this, 206, replaceWhat, e.getMessage());
		}
		String replaceBy = "";
		if (isParameterSet(ParameterTypeRegexp.PARAMETER_REPLACE_BY)) {
			replaceBy = getParameterAsString(ParameterTypeRegexp.PARAMETER_REPLACE_BY);
		}

		LinkedHashMap<Attribute, Attribute> attributeMap = new LinkedHashMap<>();
		for (Attribute oldAttribute : exampleSet.getAttributes()) {
			Attribute newAttribute = AttributeFactory.createAttribute(oldAttribute.getValueType());
			attributeMap.put(oldAttribute, newAttribute);
			if (oldAttribute.isNominal() && newAttribute.isNominal()) {
				for (String value : oldAttribute.getMapping().getValues()) {
					Matcher whatMatcher = whatPattern.matcher(value);
					String replacedValue = null;
					try {
						replacedValue = whatMatcher.replaceAll(replaceBy);
					} catch (Exception e) {
						throw new UserError(this, "malformed_regexp_replacement", replaceBy, replaceWhat);
					}
					if (replacedValue.length() > 0) {
						newAttribute.getMapping().mapString(replacedValue);
					}
				}
			}
		}

		for (Entry<Attribute, Attribute> entry : attributeMap.entrySet()) {
			Attribute oldAttribute = entry.getKey();
			Attribute newAttribute = entry.getValue();
			if (oldAttribute.isNominal() && newAttribute.isNominal()) {
				exampleSet.getExampleTable().addAttribute(newAttribute);
				exampleSet.getAttributes().addRegular(newAttribute);
				for (Example example : exampleSet) {
					double value = example.getValue(oldAttribute);
					if (Double.isNaN(value)) {
						example.setValue(newAttribute, Double.NaN);
					} else {
						String stringValue = oldAttribute.getMapping().mapIndex((int) value);
						Matcher whatMatcher = whatPattern.matcher(stringValue);
						String newValue = whatMatcher.replaceAll(replaceBy);
						if (newValue.length() == 0) {
							example.setValue(newAttribute, Double.NaN);
						} else {
							example.setValue(newAttribute, newAttribute.getMapping().mapString(newValue));
						}
					}
				}
				exampleSet.getAttributes().remove(oldAttribute);
				newAttribute.setName(oldAttribute.getName());
				newAttribute
						.setConstruction("replace(" + oldAttribute.getName() + "," + replaceWhat + "," + replaceBy + ")");
			}
		}
		return exampleSet;
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.NOMINAL };
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterTypeRegexp regexp = new ParameterTypeRegexp(ParameterTypeRegexp.PARAMETER_REPLACE_WHAT, "A regular expression specifying what should be replaced.",
				false, false);
		regexp.setPrimary(true);
		types.add(regexp);
		ParameterTypeString replacement = new ParameterTypeString(ParameterTypeRegexp.PARAMETER_REPLACE_BY,
				"The replacement for the region matched by the regular expression. Possibly including capturing groups.",
				true, false);
		regexp.setReplacementParameter(replacement);
		types.add(replacement);
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				AttributeValueReplace.class, null);
	}
}
