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

import java.util.Arrays;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeTypeException;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.AbstractExampleSetProcessing;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeExpression;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.ProcessTools;
import com.rapidminer.tools.expression.ExampleResolver;
import com.rapidminer.tools.expression.Expression;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionParser;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.internal.ExpressionParserUtils;


/**
 * Allows the declaration of a missing value (nominal or numeric) on a selected subset. The given
 * value will be converted to Double.NaN, so subsequent operators will treat it as a missing value.
 *
 * @author Marco Boeck, Marius Helf
 */
public class DeclareMissingValueOperator extends AbstractExampleSetProcessing {

	/** parameter to set the missing value for numeric type */
	public static final String PARAMETER_MISSING_VALUE_NUMERIC = "numeric_value";

	/** parameter to set the missing value for nominal type */
	public static final String PARAMETER_MISSING_VALUE_NOMINAL = "nominal_value";

	/** parameter to set the expression */
	public static final String PARAMETER_MISSING_VALUE_EXPRESSION = "expression_value";

	/** parameter to set the missing value type (numeric or nominal) */
	public static final String PARAMETER_MODE = "mode";

	public static final OperatorVersion VERSION_IGNORE_ATTRIBUTES_OF_WRONG_TYPE = new OperatorVersion(5, 2, 8);

	/** Subset Selector for parameter use */
	private AttributeSubsetSelector subsetSelector = new AttributeSubsetSelector(this, getExampleSetInputPort());

	/** constant for PARAMETER_VALUE_TYPE */
	private static final String NUMERIC = "numeric";

	/** constant for PARAMETER_VALUE_TYPE */
	private static final String NOMINAL = "nominal";

	/** constant for PARAMETER_VALUE_TYPE */
	private static final String EXPRESSION = "expression";

	/** value types to choose from in {@link #PARAMETER_MODE} */
	private static final String[] VALUE_TYPES = new String[] { NUMERIC, NOMINAL, EXPRESSION };

	/**
	 * Incompatible version, old version writes into the exampleset, if original output port is not
	 * connected.
	 */
	private static final OperatorVersion VERSION_MAY_WRITE_INTO_DATA = new OperatorVersion(7, 1, 1);

	public DeclareMissingValueOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		if (isParameterSet(PARAMETER_MISSING_VALUE_NOMINAL) || isParameterSet(PARAMETER_MISSING_VALUE_NUMERIC)) {
			ExampleSetMetaData subset = subsetSelector.getMetaDataSubset(metaData, false);
			if (subset != null) {
				MDInteger missingValueNumber;
				boolean parameterAttributeTypeExistsInSubset = false;
				String mode = getParameterAsString(PARAMETER_MODE);
				for (AttributeMetaData amd : subset.getAllAttributes()) {
					AttributeMetaData originalAMD = metaData.getAttributeByName(amd.getName());
					missingValueNumber = originalAMD.getNumberOfMissingValues();
					missingValueNumber.increaseByUnknownAmount();

					if (mode.equals(NUMERIC)) {
						switch (amd.getValueType()) {
							case Ontology.NUMERICAL:
							case Ontology.INTEGER:
							case Ontology.REAL:
								parameterAttributeTypeExistsInSubset = true;
								break;
							default:
								continue;
						}
					} else if (mode.equals(NOMINAL)) {
						switch (amd.getValueType()) {
							case Ontology.NOMINAL:
							case Ontology.STRING:
							case Ontology.BINOMINAL:
							case Ontology.POLYNOMINAL:
							case Ontology.FILE_PATH:
							case Ontology.DATE_TIME:
								parameterAttributeTypeExistsInSubset = true;
								break;
							default:
								continue;
						}
					} else if (mode.equals(EXPRESSION)) {
						// expression can be on all types so always true
						parameterAttributeTypeExistsInSubset = true;
					}
				}
				if (!parameterAttributeTypeExistsInSubset) {
					if (subset.getAllAttributes().size() <= 0) {
						getInputPort().addError(
								new SimpleMetaDataError(Severity.ERROR, getInputPort(), "attribute_selection_empty"));
					} else {
						if (mode.equals(NUMERIC)) {
							getInputPort().addError(new SimpleMetaDataError(Severity.ERROR, getInputPort(),
									"exampleset.must_contain_numerical_attribute"));
						}
						if (mode.equals(NOMINAL)) {
							getInputPort().addError(new SimpleMetaDataError(Severity.ERROR, getInputPort(),
									"exampleset.must_contain_nominal_attribute"));
						}
					}
				}
			}
		}

		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		ExampleSet subset = subsetSelector.getSubset(exampleSet, false);
		Attributes attributes = subset.getAttributes();
		String mode = getParameterAsString(PARAMETER_MODE);

		// handle EXPRESSION mode
		if (mode.equals(EXPRESSION)) {

			ExampleResolver resolver = new ExampleResolver(exampleSet);
			ExpressionParser expParser = ExpressionParserUtils.createAllModulesParser(this, resolver);

			String expression = getParameterAsString(PARAMETER_MISSING_VALUE_EXPRESSION);
			// error after parsing?
			Expression result = null;
			try {
				result = expParser.parse(expression);
			} catch (ExpressionException e) {
				throw ExpressionParserUtils.convertToUserError(this, expression, e);
			}

			if (result.getExpressionType() == ExpressionType.BOOLEAN) {
				int exampleCounter = 0;
				for (Example example : exampleSet) {
					// assign values to the variables
					resolver.bind(example);
					try {
						if (++exampleCounter % 1000 == 0) {
							checkForStop();
						}
						Boolean resultBoolean;
						try {
							resultBoolean = result.evaluateBoolean();
						} catch (ExpressionException e) {
							throw ExpressionParserUtils.convertToUserError(this, expression, e);
						}
						for (Attribute attribute : attributes) {
							// change to missing on true evaluation
							if (resultBoolean) {
								example.setValue(attribute, Double.NaN);
							}
						}
					} finally {
						// avoid memory leak
						resolver.unbind();
					}
				}
			}
		}

		boolean ignoreIncompatibleAttributes = getCompatibilityLevel().isAtMost(VERSION_IGNORE_ATTRIBUTES_OF_WRONG_TYPE);
		String nominalString = getParameterAsString(PARAMETER_MISSING_VALUE_NOMINAL);
		double missingValueNumeric = 0;
		if (mode.equals(NUMERIC)) {
			missingValueNumeric = getParameterAsDouble(PARAMETER_MISSING_VALUE_NUMERIC);
		}
		if (nominalString == null) {
			nominalString = "";
		}

		for (Attribute attribute : attributes) {
			for (Example example : subset) {
				checkForStop();
				if (mode.equals(NUMERIC)) {
					if (ignoreIncompatibleAttributes || attribute.isNumerical()) {
						if (example.getValue(attribute) == missingValueNumeric) {
							example.setValue(attribute, Double.NaN);
						}
					}
				} else if (mode.equals(NOMINAL)) {
					if (ignoreIncompatibleAttributes || attribute.isNominal()
							|| attribute.getValueType() == Ontology.FILE_PATH
							|| Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
						try {
							if (example.getNominalValue(attribute).equals(nominalString)) {
								example.setValue(attribute, Double.NaN);
							}
						} catch (AttributeTypeException e) {
							throw new UserError(this, 119, attribute.getName(), this.getName());
						}
					}
				}
			}
		}

		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> parameters = super.getParameterTypes();

		parameters.addAll(ProcessTools.setSubsetSelectorPrimaryParameter(subsetSelector.getParameterTypes(), true));

		ParameterType type = new ParameterTypeCategory(PARAMETER_MODE, "Select the value type of the missing value",
				VALUE_TYPES, 0);
		type.setExpert(false);
		parameters.add(type);

		type = new ParameterTypeDouble(PARAMETER_MISSING_VALUE_NUMERIC, "This parameter defines the missing numerical value",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_MODE, VALUE_TYPES, true, 0));
		type.setExpert(false);
		parameters.add(type);

		type = new ParameterTypeString(PARAMETER_MISSING_VALUE_NOMINAL, "This parameter defines the missing nominal value",
				true, false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_MODE, VALUE_TYPES, false, 1));
		type.setExpert(false);
		parameters.add(type);

		type = new ParameterTypeExpression(PARAMETER_MISSING_VALUE_EXPRESSION,
				"This parameter defines the expression which if true equals the missing value", getInputPort(), true, false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_MODE, VALUE_TYPES, true, 2));
		type.setExpert(false);
		type.setPrimary(true);
		parameters.add(type);

		return parameters;
	}

	@Override
	public boolean writesIntoExistingData() {
		if (getCompatibilityLevel().isAbove(VERSION_MAY_WRITE_INTO_DATA)) {
			return true;
		} else {
			// old version: true only if original output port is connected
			return isOriginalOutputConnected();
		}
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				DeclareMissingValueOperator.class, null);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] incompatibleVersions = super.getIncompatibleVersionChanges();
		OperatorVersion[] extendedIncompatibleVersions = Arrays.copyOf(incompatibleVersions,
				incompatibleVersions.length + 2);
		extendedIncompatibleVersions[incompatibleVersions.length] = VERSION_IGNORE_ATTRIBUTES_OF_WRONG_TYPE;
		extendedIncompatibleVersions[incompatibleVersions.length + 1] = VERSION_MAY_WRITE_INTO_DATA;

		return ExpressionParserUtils.addIncompatibleExpressionParserChange(extendedIncompatibleVersions);
	}
}
