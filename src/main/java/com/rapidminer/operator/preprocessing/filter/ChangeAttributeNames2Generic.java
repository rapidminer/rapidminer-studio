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
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.ProcessTools;


/**
 * <p>
 * This operator replaces the attribute names of the input example set by generic names like att1,
 * att2, att3 etc.
 * </p>
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class ChangeAttributeNames2Generic extends AbstractDataProcessing {

	public static final String PARAMETER_NAME_STEM = "generic_name_stem";

	private final AttributeSubsetSelector attributeSelector = new AttributeSubsetSelector(this, getExampleSetInputPort());

	public ChangeAttributeNames2Generic(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData exampleSetMetaData) {
		try {
			ExampleSetMetaData subsetMetaData = attributeSelector.getMetaDataSubset(exampleSetMetaData, false);
			String nameStem = isParameterSet(PARAMETER_NAME_STEM) ? getParameterAsString(PARAMETER_NAME_STEM) : "";

			int counter = 1;
			for (AttributeMetaData attributeMetaData : subsetMetaData.getAllAttributes()) {
				String name = attributeMetaData.getName();
				exampleSetMetaData.getAttributeByName(name).setName(nameStem + counter++);
			}
		} catch (UndefinedParameterError e) {
		}

		return exampleSetMetaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		Set<Attribute> attributeSubset = attributeSelector.getAttributeSubset(exampleSet, false);
		String nameStem = isParameterSet(PARAMETER_NAME_STEM) ? getParameterAsString(PARAMETER_NAME_STEM) : "";

		int counter = 1;
		for (Attribute attribute : attributeSubset) {
			attribute.setName(nameStem + counter++);
		}

		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(ProcessTools.setSubsetSelectorPrimaryParameter(attributeSelector.getParameterTypes(), true));
		ParameterType type = new ParameterTypeString(
				PARAMETER_NAME_STEM,
				"Specifies the name stem which is used to build generic names. Using 'att' as stem would lead to 'att1', 'att2', etc. as attribute names.",
				"att");
		type.setExpert(false);
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
				ChangeAttributeNames2Generic.class, attributeSelector);
	}
}
