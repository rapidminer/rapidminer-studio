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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.features.selection.AbstractFeatureSelection;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * This operator removes the attributes of a given range. The first and last attribute of the range
 * will be removed, too. Counting starts with 1.
 * 
 * @author Sebastian Land
 */
public class FeatureRangeRemoval extends AbstractFeatureSelection {

	/**
	 * The parameter name for &quot;The first attribute of the attribute range which should be
	 * removed&quot;
	 */
	public static final String PARAMETER_FIRST_ATTRIBUTE = "first_attribute";

	/**
	 * The parameter name for &quot;The last attribute of the attribute range which should be
	 * removed&quot;
	 */
	public static final String PARAMETER_LAST_ATTRIBUTE = "last_attribute";

	public FeatureRangeRemoval(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		int firstIndex = getParameterAsInt(PARAMETER_FIRST_ATTRIBUTE);
		int secondIndex = getParameterAsInt(PARAMETER_LAST_ATTRIBUTE);
		boolean warning = false;
		if (secondIndex < firstIndex) {
			addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(),
					Collections
							.singletonList(new ParameterSettingQuickFix(this, PARAMETER_FIRST_ATTRIBUTE, secondIndex + "")),
					"parameter_combination_forbidden_range", PARAMETER_FIRST_ATTRIBUTE, PARAMETER_LAST_ATTRIBUTE));
			warning = true;
		}
		if (metaData.getAttributeSetRelation() == SetRelation.EQUAL
				|| metaData.getAttributeSetRelation() == SetRelation.SUBSET) {
			if (secondIndex > metaData.getNumberOfRegularAttributes()) {
				getExampleSetInputPort().addError(
						new SimpleMetaDataError(Severity.ERROR, getExampleSetInputPort(), Collections
								.singletonList(new ParameterSettingQuickFix(this, PARAMETER_LAST_ATTRIBUTE, metaData
										.getNumberOfRegularAttributes() + "")), "exampleset.parameters.need_more_examples",
								secondIndex, PARAMETER_LAST_ATTRIBUTE, secondIndex));
				warning = true;
			}
		}

		// doing transformation
		if (!warning) {
			int i = 0;
			Iterator<AttributeMetaData> iterator = metaData.getAllAttributes().iterator();
			while (iterator.hasNext() && i < secondIndex) {
				AttributeMetaData amd = iterator.next();
				if (!amd.isSpecial()) {
					i++;
				}
				if (i >= firstIndex) {
					iterator.remove();
				}
			}
		}

		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		int first = getParameterAsInt(PARAMETER_FIRST_ATTRIBUTE) - 1;
		int last = getParameterAsInt(PARAMETER_LAST_ATTRIBUTE) - 1;
		if (last < first) {
			logWarning("Last attribute is smaller than first. No change performed.");
		}

		if (last >= exampleSet.getAttributes().size()) {
			throw new UserError(this, 125, String.valueOf(exampleSet.getAttributes().size()), String.valueOf(last + 1));
		}

		Iterator<Attribute> i = exampleSet.getAttributes().iterator();
		int counter = 0;
		while (i.hasNext() && counter <= last) {
			i.next();
			if ((counter >= first) && (counter <= last)) {
				i.remove();
			}
			checkForStop();
			counter++;
		}
		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> parameterTypes = super.getParameterTypes();
		ParameterType parameterType = new ParameterTypeInt(PARAMETER_FIRST_ATTRIBUTE,
				"The first attribute of the attribute range which should  be removed", 1, Integer.MAX_VALUE, false);
		parameterType.setExpert(false);
		parameterTypes.add(parameterType);
		parameterType = new ParameterTypeInt(PARAMETER_LAST_ATTRIBUTE,
				"The last attribute of the attribute range which should  be removed", 1, Integer.MAX_VALUE, false);
		parameterType.setExpert(false);
		parameterTypes.add(parameterType);
		return parameterTypes;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), FeatureRangeRemoval.class,
				null);
	}
}
