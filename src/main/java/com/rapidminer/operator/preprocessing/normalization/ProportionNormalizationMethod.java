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
package com.rapidminer.operator.preprocessing.normalization;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDReal;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.AboveOperatorVersionCondition;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.math.container.Range;


/**
 * A normalization method for bringing the sum of all attribute values to 1.
 *
 * @author Sebastian Land
 *
 */
public class ProportionNormalizationMethod extends AbstractNormalizationMethod {

	private static final String PARAMETER_ALLOW_NEGATIVE_VALUES = "allow_negative_values";

	@Override
	public Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd,
			InputPort exampleSetInputPort, ParameterHandler parameterHandler) throws UndefinedParameterError {
		if (amd.getValueSetRelation() == SetRelation.EQUAL) {
			if (emd.getNumberOfExamples().isKnown()) {
				amd.setMean(new MDReal(1d / emd.getNumberOfExamples().getValue()));
			} else {
				amd.setMean(new MDReal());
			}
			Range range = amd.getValueRange();
			if (range.getLower() < 0d && !areNegativeValuesAllowed(parameterHandler)) {
				List<QuickFix> quickFix = Collections.emptyList();
				if (parameterHandler instanceof Operator && doesVersionCheckForNonFiniteValues(parameterHandler)) {
					quickFix = Collections.singletonList(new ParameterSettingQuickFix((Operator) parameterHandler,
							PARAMETER_ALLOW_NEGATIVE_VALUES, Boolean.toString(true)));
				}
				exampleSetInputPort.addError(new SimpleMetaDataError(Severity.WARNING, exampleSetInputPort, quickFix,
						"attribute_contains_negative_values", amd.getName(), getName()));
			}
		} else {
			// set to unknown
			amd.setMean(new MDReal());
			amd.setValueRange(new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), SetRelation.UNKNOWN);
		}
		return Collections.singleton(amd);
	}

	@Override
	public AbstractNormalizationModel getNormalizationModel(ExampleSet exampleSet, Operator operator) throws UserError {
		boolean allowNegative = areNegativeValuesAllowed(operator);
		// calculating attribute sums
		HashMap<String, Double> attributeSums = new HashMap<String, Double>();
		boolean versionChecksForNonFinite = doesVersionCheckForNonFiniteValues(operator);
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNumerical()) {
				double sum = 0;
				boolean negValError = false;
				for (Example example : exampleSet) {
					double value = example.getValue(attribute);
					if (versionChecksForNonFinite && !Double.isFinite(value)) {
						// ignore nonsensical values
						continue;
					}
					if (value < 0) {
						if (!allowNegative) {
							throw new UserError(operator, 127, I18N.getMessage(I18N.getErrorBundle(),
									"metadata.error.attribute_contains_negative_values", attribute.getName(), getName()));
						}
						if (versionChecksForNonFinite) {
							value = -value;
						} else if (!negValError) {
							negativeValueWarning(operator, attribute.getName(), value);
							negValError = true;
						}
					}
					sum += value;
				}
				if (sum == 0 || !Double.isFinite(sum)) {
					if (versionChecksForNonFinite) {
						LogService.getRoot()
								.warning("Ignoring " + attribute.getName() + " in Normalization because of sum " + sum);
						// ignore attribute for nonsensical sums
						// see ProportionNormalizationModel#getTargetAttributes and #getValue
						continue;
					} else {
						divisorWarning(operator, attribute.getName(), sum);
					}
				}
				attributeSums.put(attribute.getName(), sum);
			}
		}
		return new ProportionNormalizationModel(exampleSet, attributeSums);
	}

	/**
	 * Returns whether negative values are allowed. The specified handler will be asked for
	 * compatibility if it is an {@link Operator}.
	 *
	 * @since 7.6
	 */
	private boolean areNegativeValuesAllowed(ParameterHandler handler) {
		return !doesVersionCheckForNonFiniteValues(handler)
				|| handler.getParameterAsBoolean(PARAMETER_ALLOW_NEGATIVE_VALUES);
	}

	/**
	 * Returns whether the {@link ParameterHandler} does check for non-finite values. If the handler
	 * is not an operator, non-finite values are not ignored. Otherwise the operator compatibility
	 * level is checked.
	 *
	 * @since 7.6
	 */
	private boolean doesVersionCheckForNonFiniteValues(ParameterHandler handler) {
		if (!(handler instanceof Operator)) {
			return true;
		}
		return ((Operator) handler).getCompatibilityLevel().isAbove(Normalization.BEFORE_NON_FINITE_VALUES_HANDLING);
	}

	@Override
	public String getName() {
		return "proportion transformation";
	}

	@Override
	public List<ParameterType> getParameterTypes(ParameterHandler handler) {
		ParameterType type = new ParameterTypeBoolean(PARAMETER_ALLOW_NEGATIVE_VALUES,
				"Whether negative values should be allowed and used as absolute values", false, true);
		type.registerDependencyCondition(new AboveOperatorVersionCondition(
				handler instanceof Operator ? (Operator) handler : null, Normalization.BEFORE_NON_FINITE_VALUES_HANDLING));
		return Collections.singletonList(type);
	}

}
