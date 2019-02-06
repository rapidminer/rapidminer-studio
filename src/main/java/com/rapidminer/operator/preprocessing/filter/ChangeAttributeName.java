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
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * <p>
 * This operator can be used to rename an attribute of the input example set. If you want to change
 * the attribute type (e.g. from regular to id attribute or from label to regular etc.), you should
 * use the {@link ChangeAttributeType} operator.
 * </p>
 *
 * @author Ingo Mierswa, Sebastian Land
 */
public class ChangeAttributeName extends AbstractDataProcessing {

	/** The parameter name for &quot;The old name of the attribute.&quot; */
	public static final String PARAMETER_OLD_NAME = "old_name";

	/** The parameter name for &quot;The new name of the attribute.&quot; */
	public static final String PARAMETER_NEW_NAME = "new_name";

	public static final String PARAMETER_RENAME_ATTRIBUTES = "rename_additional_attributes";

	public ChangeAttributeName(OperatorDescription description) {
		super(description);
		getExampleSetInputPort().addPrecondition(
				new AttributeSetPrecondition(getExampleSetInputPort(), AttributeSetPrecondition.getAttributesByParameter(
						this, PARAMETER_OLD_NAME)));
		getExampleSetInputPort().addPrecondition(
				new AttributeSetPrecondition(getExampleSetInputPort(), AttributeSetPrecondition
						.getAttributesByParameterListEntry(this, PARAMETER_RENAME_ATTRIBUTES, 0)));
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		try {
			String newName = null;
			if (isParameterSet(PARAMETER_NEW_NAME)) {
				newName = getParameterAsString(PARAMETER_NEW_NAME);
			}

			if (isParameterSet(PARAMETER_OLD_NAME)) {
				String oldName = getParameter(PARAMETER_OLD_NAME);
				renameAttributeMetaData(metaData, oldName, newName);
			}

			if (isParameterSet(PARAMETER_RENAME_ATTRIBUTES)) {
				List<String[]> renamings = getParameterList(PARAMETER_RENAME_ATTRIBUTES);
				for (String[] pair : renamings) {
					renameAttributeMetaData(metaData, pair[0], pair[1]);
				}
			}
		} catch (UndefinedParameterError e) {
		}
		return metaData;
	}

	private void renameAttributeMetaData(ExampleSetMetaData metaData, String oldName, String newName) {
		AttributeMetaData amd = metaData.getAttributeByName(oldName);
		if (amd != null && newName != null) {
			if (metaData.containsAttributeName(newName) == MetaDataInfo.YES) {
				if (oldName != null && oldName.equals(newName)) {
					return;
				}
				getInputPort().addError(
						new SimpleMetaDataError(Severity.ERROR, getInputPort(), "already_contains_attribute", newName));
			} else {
				amd.setName(newName);
			}
		}
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		String oldName = getParameterAsString(PARAMETER_OLD_NAME);
		String newName = getParameterAsString(PARAMETER_NEW_NAME);

		renameAttribute(exampleSet, oldName, newName);

		if (isParameterSet(PARAMETER_RENAME_ATTRIBUTES)) {
			List<String[]> renamings = getParameterList(PARAMETER_RENAME_ATTRIBUTES);
			for (String[] pair : renamings) {
				renameAttribute(exampleSet, pair[0], pair[1]);
			}
		}

		return exampleSet;
	}

	private void renameAttribute(ExampleSet exampleSet, String oldName, String newName) throws UserError {
		if (oldName != null && oldName.equals(newName)) {
			return;
		}
		Attribute attribute = exampleSet.getAttributes().get(oldName);

		if (attribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_OLD_NAME, oldName);
		}

		attribute.setName(newName);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_OLD_NAME, "The old name of the attribute.", getExampleSetInputPort(),
				false));
		types.add(new ParameterTypeString(PARAMETER_NEW_NAME, "The new name of the attribute.", false));

		types.add(new ParameterTypeList(PARAMETER_RENAME_ATTRIBUTES,
				"A list that can be used to define additional attributes that should be renamed.",
				new ParameterTypeAttribute(PARAMETER_OLD_NAME, "The old name of the attribute.", getExampleSetInputPort(),
						false), new ParameterTypeString(PARAMETER_NEW_NAME, "The new name of the attribute.", false), false));
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), ChangeAttributeName.class,
				null);
	}
}
