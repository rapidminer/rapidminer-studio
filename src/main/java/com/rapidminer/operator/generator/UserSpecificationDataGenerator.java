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
package com.rapidminer.operator.generator;

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.io.AbstractExampleSource;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeExpression;
import com.rapidminer.parameter.ParameterTypeExpression.OperatorVersionCallable;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.ExampleResolver;
import com.rapidminer.tools.expression.Expression;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionParser;
import com.rapidminer.tools.expression.internal.ExpressionParserUtils;
import com.rapidminer.tools.expression.internal.UnknownResolverVariableException;


/**
 * This operator produces an {@link ExampleSet} containing only one example whose Attribute values
 * are derived from a user specified list.
 *
 * @author Sebastian Land
 * @deprecated since 9.2.0, use {@link com.rapidminer.extension.utility.operator.generator.CreateExampleSet} instead
 */
public class UserSpecificationDataGenerator extends AbstractExampleSource {

	public static final String PARAMETER_VALUES = "attribute_values";
	public static final String PARAMETER_ATTRIBUTE_NAME = "attribute_name";
	public static final String PARAMETER_ATTRIBUTE_VALUE = "attribute_value";

	/**
	 * The parameter name for &quot;The name of the attribute of which the type should be
	 * changed.&quot;
	 */
	public static final String PARAMETER_NAME = "name";

	/**
	 * The parameter name for &quot;The target type of the attribute (only changed if parameter
	 * change_attribute_type is true).&quot;
	 */
	public static final String PARAMETER_TARGET_ROLE = "target_role";

	public static final String PARAMETER_ROLES = "set_additional_roles";

	private static final String REGULAR_NAME = "regular";

	private static final String[] TARGET_ROLES = new String[] { REGULAR_NAME, Attributes.ID_NAME, Attributes.LABEL_NAME,
			Attributes.PREDICTION_NAME, Attributes.CLUSTER_NAME, Attributes.WEIGHT_NAME, Attributes.BATCH_NAME };

	public UserSpecificationDataGenerator(OperatorDescription description) {
		super(description);
	}

	@Override
	public MetaData getGeneratedMetaData() throws OperatorException {
		ExampleSetMetaData emd = new ExampleSetMetaData();
		emd.setNumberOfExamples(1);
		emd.attributesAreKnown();

		ExampleResolver exampleResolver = new ExampleResolver(emd);
		ExpressionParser expParser = ExpressionParserUtils.createAllModulesParser(this, exampleResolver);

		try {
			for (String[] nameFunctionPair : getParameterList(PARAMETER_VALUES)) {
				String name = nameFunctionPair[0];
				String function = nameFunctionPair[1];
				try {
					Expression parsedExp = expParser.parse(function);

					// generate AttributeMetaData and add it to the example resolver and ExampleSet
					// meta data
					AttributeMetaData amd = ExpressionParserUtils.generateAttributeMetaData(emd, name,
							parsedExp.getExpressionType());
					exampleResolver.addAttributeMetaData(amd);
					emd.addAttribute(amd);
				} catch (ExpressionException e) {
					OutputPort portByIndex = this.getOutputPorts().getPortByIndex(0);
					if (e.getCause() != null && e.getCause() instanceof UnknownResolverVariableException) {
						// in case a resolver variable cannot be resolved, return a new attribute
						// with nominal type
						emd.addAttribute(new AttributeMetaData(name, Ontology.NOMINAL));
					} else {
						// in all other cases abort meta data generation, add an error and return
						// empty meta data
						portByIndex.addError(new SimpleMetaDataError(Severity.ERROR, portByIndex,
								"cannot_create_exampleset_metadata", e.getShortMessage()));
						return new ExampleSetMetaData();
					}
				}

			}

			// now proceed with Attribute rules
			if (isParameterSet(PARAMETER_ROLES)) {
				List<String[]> list = getParameterList(PARAMETER_ROLES);
				for (String[] pairs : list) {
					setRoleMetaData(emd, pairs[0], pairs[1]);
				}
			}
		} catch (UndefinedParameterError e) {
		}

		return emd;
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
						addError(new SimpleProcessSetupError(Severity.WARNING, this.getPortOwner(), "already_contains_role",
								targetRole));
						metaData.removeAttribute(oldRole);
					}
					amd.setRole(targetRole);
				}
			}
		}
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		ExampleSet exampleSet = ExampleSets.from().withBlankSize(1).build();
		List<String[]> paramValues = getParameterList(PARAMETER_VALUES);

		// init operator progress
		List<String[]> roleList = null;
		if (isParameterSet(PARAMETER_ROLES)) {
			roleList = getParameterList(PARAMETER_ROLES);
			getProgress().setTotal(paramValues.size() + roleList.size());
		} else {
			getProgress().setTotal(paramValues.size());
		}

		// create resolver and parser
		ExampleResolver resolver = new ExampleResolver(exampleSet);
		ExpressionParser parser = ExpressionParserUtils.createAllModulesParser(this, resolver);

		for (String[] nameFunctionPair : paramValues) {
			String name = nameFunctionPair[0];
			String function = nameFunctionPair[1];
			try {
				ExpressionParserUtils.addAttribute(exampleSet, name, function, parser, resolver, this);
			} catch (ExpressionException e) {
				throw ExpressionParserUtils.convertToUserError(this, function, e);
			}

			getProgress().step();
		}

		// now set roles
		if (roleList != null) {
			for (String[] pairs : roleList) {
				setRole(exampleSet, pairs[0], pairs[1]);
				getProgress().step();
			}
		}

		getProgress().complete();

		return exampleSet;
	}

	private void setRole(ExampleSet exampleSet, String name, String newRole) throws UserError {
		Attribute attribute = exampleSet.getAttributes().get(name);

		if (attribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_ROLES, name);
		}

		exampleSet.getAttributes().remove(attribute);
		if (newRole == null || newRole.trim().length() == 0) {
			throw new UndefinedParameterError(PARAMETER_TARGET_ROLE, this);
		}
		if (newRole.equals(REGULAR_NAME)) {
			exampleSet.getAttributes().addRegular(attribute);
		} else {
			exampleSet.getAttributes().setSpecialAttribute(attribute, newRole);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeList(PARAMETER_VALUES,
				"This parameter defines the attributes and their values in the single example returned.",
				new ParameterTypeString(PARAMETER_ATTRIBUTE_NAME, "This is the name of the generated attribute.", false),
				new ParameterTypeExpression(PARAMETER_ATTRIBUTE_VALUE,
						"An expression that is parsed to derive the value of this attribute.", new OperatorVersionCallable(this)),
				false);
		type.setPrimary(true);
		types.add(type);

		types.add(new ParameterTypeList(PARAMETER_ROLES, "This parameter defines additional attribute role combinations.",
				new ParameterTypeString(PARAMETER_NAME, "The name of the attribute whose role should be changed.", false,
						false),
				new ParameterTypeStringCategory(PARAMETER_TARGET_ROLE,
						"The target role of the attribute (only changed if parameter change_attribute_type is true).",
						TARGET_ROLES, TARGET_ROLES[0]),
				false));
		return types;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return ExpressionParserUtils.addIncompatibleExpressionParserChange(super.getIncompatibleVersionChanges());
	}

}
