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
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * <p>
 * This operator can be used to change the attribute type of an attribute of the input example set.
 * If you want to change the attribute name you should use the {@link ChangeAttributeName} operator.
 * </p>
 *
 * <p>
 * The target type indicates if the attribute is a regular attribute (used by learning operators) or
 * a special attribute (e.g. a label or id attribute). The following target attribute types are
 * possible:
 * </p>
 * <ul>
 * <li>regular: only regular attributes are used as input variables for learning tasks</li>
 * <li>id: the id attribute for the example set</li>
 * <li>label: target attribute for learning</li>
 * <li>prediction: predicted attribute, i.e. the predictions of a learning scheme</li>
 * <li>cluster: indicates the membership to a cluster</li>
 * <li>weight: indicates the weight of the example</li>
 * <li>batch: indicates the membership to an example batch</li>
 * </ul>
 * <p>
 * Users can also define own attribute types by simply using the desired name.
 * </p>
 * <p>
 * If the target attribute type is already in use the operator will since Version 5.3.13 change the
 * Type of the attribute to regular and changes the type of the given Attribute to the target
 * attribute type. Before these Version the Operator will delete the attribute which already has the
 * role.
 * </p>
 *
 * @author Ingo Mierswa
 */
public class ChangeAttributeRole extends AbstractDataProcessing {

	/**
	 * The parameter name for &quot;The name of the attribute of which the type should be
	 * changed.&quot;
	 */
	public static final String PARAMETER_NAME = "attribute_name";

	/**
	 * The parameter name for &quot;The target type of the attribute (only changed if parameter
	 * change_attribute_type is true).&quot;
	 */
	public static final String PARAMETER_TARGET_ROLE = "target_role";

	public static final String PARAMETER_CHANGE_ATTRIBUTES = "set_additional_roles";

	private static final String REGULAR_NAME = "regular";

	private static final String[] TARGET_ROLES = new String[] { REGULAR_NAME, Attributes.ID_NAME, Attributes.LABEL_NAME,
			Attributes.PREDICTION_NAME, Attributes.CLUSTER_NAME, Attributes.WEIGHT_NAME, Attributes.BATCH_NAME };

	private final OperatorVersion VERSION_BEFORE_KEEPING_SPECIAL_ATT_WHEN_IT_LOSE_ROLE = new OperatorVersion(5, 3, 13);

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
		newIncompatibleVersionChanges[newIncompatibleVersionChanges.length - 1] = VERSION_BEFORE_KEEPING_SPECIAL_ATT_WHEN_IT_LOSE_ROLE;
		return newIncompatibleVersionChanges;
	}

	public ChangeAttributeRole(OperatorDescription description) {
		super(description);
		getExampleSetInputPort().addPrecondition(
				new AttributeSetPrecondition(getExampleSetInputPort(), AttributeSetPrecondition.getAttributesByParameter(
						this, PARAMETER_NAME)));
		getExampleSetInputPort().addPrecondition(
				new AttributeSetPrecondition(getExampleSetInputPort(), AttributeSetPrecondition
						.getAttributesByParameterListEntry(this, PARAMETER_CHANGE_ATTRIBUTES, 0)));
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		try {
			String targetRole = null;
			if (isParameterSet(PARAMETER_TARGET_ROLE)) {
				targetRole = getParameterAsString(PARAMETER_TARGET_ROLE);
			}

			if (isParameterSet(PARAMETER_NAME)) {
				String name = getParameter(PARAMETER_NAME);
				setRoleMetaData(metaData, name, targetRole);
			}

			// now proceed with list
			if (isParameterSet(PARAMETER_CHANGE_ATTRIBUTES)) {
				List<String[]> list = getParameterList(PARAMETER_CHANGE_ATTRIBUTES);
				for (String[] pairs : list) {
					setRoleMetaData(metaData, pairs[0], pairs[1]);
				}
			}
		} catch (UndefinedParameterError e) {
		}
		return metaData;
	}

	private void setRoleMetaData(ExampleSetMetaData metaData, String name, String targetRole) {
		AttributeMetaData amd = metaData.getAttributeByName(name);
		if (amd != null) {
			if (targetRole != null) {
				if (REGULAR_NAME.equals(targetRole)) {
					amd.setRegular();
				} else {
					AttributeMetaData oldRole = metaData.getAttributeByRole(targetRole);
					if (oldRole != null && oldRole != amd) {
						if (getCompatibilityLevel().compareTo(VERSION_BEFORE_KEEPING_SPECIAL_ATT_WHEN_IT_LOSE_ROLE) > 0) {
							oldRole.setRegular();
						} else {
							getInputPort().addError(
									new SimpleMetaDataError(Severity.WARNING, getInputPort(), "already_contains_role",
											targetRole));
							metaData.removeAttribute(oldRole);
						}
					}
					amd.setRole(targetRole);
				}
			}
		}
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		String name = getParameterAsString(PARAMETER_NAME);
		String newRole = getParameterAsString(PARAMETER_TARGET_ROLE);

		setRole(exampleSet, name, newRole, PARAMETER_NAME);

		// now do the list
		if (isParameterSet(PARAMETER_CHANGE_ATTRIBUTES)) {
			List<String[]> list = getParameterList(PARAMETER_CHANGE_ATTRIBUTES);
			for (String[] pairs : list) {
				setRole(exampleSet, pairs[0], pairs[1], PARAMETER_CHANGE_ATTRIBUTES);
			}
		}

		return exampleSet;
	}

	private void setRole(ExampleSet exampleSet, String name, String newRole, String paramKey) throws UserError {
		Attribute attribute = exampleSet.getAttributes().get(name);

		if (attribute == null) {
			throw new AttributeNotFoundError(this, paramKey, name);
		}

		exampleSet.getAttributes().remove(attribute);
		if (newRole == null || newRole.trim().length() == 0) {
			throw new UndefinedParameterError(PARAMETER_TARGET_ROLE, this);
		}
		if (newRole.equals(REGULAR_NAME)) {
			exampleSet.getAttributes().addRegular(attribute);
		} else {
			if (getCompatibilityLevel().compareTo(VERSION_BEFORE_KEEPING_SPECIAL_ATT_WHEN_IT_LOSE_ROLE) > 0) {
				Attribute oldOne = exampleSet.getAttributes().getSpecial(newRole);
				if (oldOne != null) {
					exampleSet.getAttributes().remove(oldOne);
					exampleSet.getAttributes().addRegular(oldOne);
				}
			}
			exampleSet.getAttributes().setSpecialAttribute(attribute, newRole);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_NAME, "The name of the attribute whose role should be changed.",
				getExampleSetInputPort(), false, false));
		ParameterType type = new ParameterTypeStringCategory(PARAMETER_TARGET_ROLE,
				"The target role of the attribute (only changed if parameter change_attribute_type is true).", TARGET_ROLES,
				TARGET_ROLES[0]);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeList(PARAMETER_CHANGE_ATTRIBUTES,
				"This parameter defines additional attribute role combinations.", new ParameterTypeAttribute(PARAMETER_NAME,
						"The name of the attribute whose role should be changed.", getExampleSetInputPort(), false, false),
				new ParameterTypeStringCategory(PARAMETER_TARGET_ROLE,
						"The target role of the attribute (only changed if parameter change_attribute_type is true).",
						TARGET_ROLES, TARGET_ROLES[0]), false));
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), ChangeAttributeRole.class,
				null);
	}
}
