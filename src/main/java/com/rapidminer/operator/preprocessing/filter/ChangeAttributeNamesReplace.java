/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeRegexp;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 * <p>
 * This operator replaces parts of the attribute names (like whitespaces, parentheses, or other
 * unwanted characters) by a specified replacement. The replace_what parameter can be defined as a
 * regular expression (please refer to the annex of the RapidMiner tutorial for a description). The
 * replace_by parameter can be defined as an arbitrary string. Empty strings are also allowed.
 * Capturing groups of the defined regular expression can be accessed with $1, $2, $3...
 * </p>
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class ChangeAttributeNamesReplace extends AbstractDataProcessing {

	public static final String PARAMETER_REPLACE_WHAT = "replace_what";

	public static final String PARAMETER_REPLACE_BY = "replace_by";

	private final AttributeSubsetSelector attributeSelector = new AttributeSubsetSelector(this, getExampleSetInputPort());

	public ChangeAttributeNamesReplace(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData exampleSetMetaData) {
		String replaceWhat = "";
		try {
			ExampleSetMetaData subsetMetaData = attributeSelector.getMetaDataSubset(exampleSetMetaData, false);
			replaceWhat = getParameterAsString(PARAMETER_REPLACE_WHAT);
			Pattern replaceWhatPattern = Pattern.compile(replaceWhat);
			String replaceByString = isParameterSet(PARAMETER_REPLACE_BY) ? getParameterAsString(PARAMETER_REPLACE_BY) : "";

			for (AttributeMetaData attributeMetaData : subsetMetaData.getAllAttributes()) {
				String name = attributeMetaData.getName();

				exampleSetMetaData.getAttributeByName(name).setName(
						replaceWhatPattern.matcher(name).replaceAll(replaceByString));
			}
		} catch (UndefinedParameterError e) {
		} catch (IndexOutOfBoundsException e) {
			addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(), "capturing_group_undefined",
					PARAMETER_REPLACE_BY, PARAMETER_REPLACE_WHAT));
		} catch (PatternSyntaxException e) {
			addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(), "invalid_regex", replaceWhat));
		}

		return exampleSetMetaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		Set<Attribute> attributeSubset = attributeSelector.getAttributeSubset(exampleSet, false);
		Pattern replaceWhatPattern = Pattern.compile(getParameterAsString(PARAMETER_REPLACE_WHAT));
		String replaceByString = isParameterSet(PARAMETER_REPLACE_BY) ? getParameterAsString(PARAMETER_REPLACE_BY) : "";

		try {
			for (Attribute attribute : attributeSubset) {
				attribute.setName(replaceWhatPattern.matcher(attribute.getName()).replaceAll(replaceByString));
			}
		} catch (IndexOutOfBoundsException e) {
			throw new UserError(this, 215, replaceByString, PARAMETER_REPLACE_WHAT);
		} catch (IllegalArgumentException e) {
			throw new UserError(this, 152, replaceByString);
		}

		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(attributeSelector.getParameterTypes());

		ParameterType type = new ParameterTypeRegexp(PARAMETER_REPLACE_WHAT,
				"A regular expression defining what should be replaced in the attribute names.", "\\W");
		type.setShowRange(false);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeString(PARAMETER_REPLACE_BY,
				"This string is used as replacement for all parts of the matching attributes where the parameter '"
						+ PARAMETER_REPLACE_WHAT + "' matches.", true, false));
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				ChangeAttributeNamesReplace.class, attributeSelector);
	}
}
