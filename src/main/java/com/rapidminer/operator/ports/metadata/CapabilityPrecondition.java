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
package com.rapidminer.operator.ports.metadata;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attributes;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.quickfix.ChangeAttributeRoleQuickFix;
import com.rapidminer.operator.ports.quickfix.OperatorInsertionQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.operator.preprocessing.discretization.AbstractDiscretizationOperator;
import com.rapidminer.operator.preprocessing.filter.MissingValueReplenishment;
import com.rapidminer.operator.preprocessing.filter.NominalToBinominal;
import com.rapidminer.operator.preprocessing.filter.NominalToNumeric;
import com.rapidminer.operator.preprocessing.filter.attributes.RegexpAttributeFilter;
import com.rapidminer.operator.preprocessing.filter.attributes.SingleAttributeFilter;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;


/**
 * This is a precondition for {@link InputPort}s that ensures that the capabilities given by an
 * operator are matched by the delivered ExampleSet.
 * 
 * @author Sebastian Land
 */
public class CapabilityPrecondition extends ExampleSetPrecondition {

	protected final CapabilityProvider capabilityProvider;

	public CapabilityPrecondition(CapabilityProvider capabilityProvider, InputPort inputPort) {
		super(inputPort);
		this.capabilityProvider = capabilityProvider;
	}

	@Override
	public void makeAdditionalChecks(ExampleSetMetaData metaData) {
		// regular attributes
		if (metaData.containsAttributesWithValueType(Ontology.NOMINAL, false) == MetaDataInfo.YES) {
			if (metaData.containsAttributesWithValueType(Ontology.BINOMINAL, false) == MetaDataInfo.YES) {
				if (!capabilityProvider.supportsCapability(OperatorCapability.BINOMINAL_ATTRIBUTES)) {
					List<QuickFix> fixes = new LinkedList<>();
					if (capabilityProvider.supportsCapability(OperatorCapability.NUMERICAL_ATTRIBUTES)) {
						fixes.add(createToNumericalFix(null));
					}
					createLearnerError(OperatorCapability.BINOMINAL_ATTRIBUTES.getDescription(), fixes);
				}
			} else {
				if (!capabilityProvider.supportsCapability(OperatorCapability.POLYNOMINAL_ATTRIBUTES)) {
					List<QuickFix> fixes = new LinkedList<>();
					if (capabilityProvider.supportsCapability(OperatorCapability.BINOMINAL_ATTRIBUTES)) {
						fixes.add(createToBinominalFix(null));
					}
					if (capabilityProvider.supportsCapability(OperatorCapability.NUMERICAL_ATTRIBUTES)) {
						fixes.add(createToNumericalFix(null));
					}
					createLearnerError(OperatorCapability.POLYNOMINAL_ATTRIBUTES.getDescription(), fixes);
				}
			}
		}
		if (metaData.containsAttributesWithValueType(Ontology.NUMERICAL, false) == MetaDataInfo.YES
				&& !capabilityProvider.supportsCapability(OperatorCapability.NUMERICAL_ATTRIBUTES)) {
			createLearnerError(OperatorCapability.NUMERICAL_ATTRIBUTES.getDescription(),
					AbstractDiscretizationOperator.createDiscretizationFixes(getInputPort(), null));
		}

		checkLabelPreconditions(metaData);

		// weighted examples
		if (!capabilityProvider.supportsCapability(OperatorCapability.WEIGHTED_EXAMPLES)) {
			switch (metaData.hasSpecial(Attributes.WEIGHT_NAME)) {
				case YES:
					createError(Severity.WARNING, "learner_does_not_support_weights");
					break;
				case NO:
				case UNKNOWN:
				default:
					break;
			}
		}

		// missing values
		if (!capabilityProvider.supportsCapability(OperatorCapability.MISSING_VALUES)) {
			if (metaData.getAllAttributes() != null) {
				for (AttributeMetaData amd : metaData.getAllAttributes()) {
					if (!amd.isSpecial() || Attributes.LABEL_NAME.equals(amd.getRole())) {
						if (amd.containsMissingValues() == MetaDataInfo.YES) {
							createLearnerError(OperatorCapability.MISSING_VALUES.getDescription(),
									Collections.singletonList(new OperatorInsertionQuickFix(
											"insert_missing_value_replenishment", new String[0], 1, getInputPort()) {

										@Override
										public Operator createOperator() throws OperatorCreationException {
											return OperatorService.createOperator(MissingValueReplenishment.class);
										}
									}));
							break;
						}
					}
				}
			}
		}
	}

	protected void checkLabelPreconditions(ExampleSetMetaData metaData) {
		// label
		// check if needs label
		// TODO: This checks if it is supported, but not if it is required. This test will break if
		// we add a new label type
		// because it will then be incomplete.
		if (!capabilityProvider.supportsCapability(OperatorCapability.NO_LABEL)) {
			// if it supports no label it's simply irrelevant if label is present.
			if (capabilityProvider.supportsCapability(OperatorCapability.ONE_CLASS_LABEL)
					|| capabilityProvider.supportsCapability(OperatorCapability.BINOMINAL_LABEL)
					|| capabilityProvider.supportsCapability(OperatorCapability.POLYNOMINAL_LABEL)
					|| capabilityProvider.supportsCapability(OperatorCapability.NUMERICAL_LABEL)) {
				switch (metaData.hasSpecial(Attributes.LABEL_NAME)) {
					case UNKNOWN:
						getInputPort().addError(
								new SimpleMetaDataError(Severity.WARNING, getInputPort(), Collections
										.singletonList(new ChangeAttributeRoleQuickFix(getInputPort(),
												Attributes.LABEL_NAME, "change_attribute_role", Attributes.LABEL_NAME)),
										"special_unknown", new Object[] { Attributes.LABEL_NAME }));
						break;
					case NO:
						getInputPort().addError(
								new SimpleMetaDataError(Severity.ERROR, getInputPort(), Collections
										.singletonList(new ChangeAttributeRoleQuickFix(getInputPort(),
												Attributes.LABEL_NAME, "change_attribute_role", Attributes.LABEL_NAME)),
										"special_missing", new Object[] { Attributes.LABEL_NAME }));
						break;
					case YES:
						AttributeMetaData label = metaData.getLabelMetaData();
						List<QuickFix> fixes = new LinkedList<>();
						if (label.isNominal()) {
							if (capabilityProvider.supportsCapability(OperatorCapability.NUMERICAL_LABEL)) {
								fixes.addAll(getFixesForClassificationWhenRegressionSupported());
							}
							if (capabilityProvider.supportsCapability(OperatorCapability.ONE_CLASS_LABEL)
									&& !capabilityProvider.supportsCapability(OperatorCapability.BINOMINAL_LABEL)
									&& !capabilityProvider.supportsCapability(OperatorCapability.POLYNOMINAL_LABEL)) {

								// if it only supports one class label
								if (label.getValueSet().size() > 1 && label.getValueSetRelation() != SetRelation.UNKNOWN) {
									createError(Severity.ERROR, "one_class_label_invalid", label.getValueSetRelation()
											.toString() + " " + label.getValueSet().size());
								}
							} else {
								// if it supports two or more classes
								if (label.getValueSet().size() == 1 && label.getValueSetRelation() == SetRelation.EQUAL) {
									createError(Severity.ERROR, "no_polynomial_label");

								} else {
									// if two or more classes are present
									if (label.isBinominal() || label.getValueSetRelation() == SetRelation.EQUAL
											&& label.getValueSet().size() == 2) {
										if (!capabilityProvider.supportsCapability(OperatorCapability.BINOMINAL_LABEL)) {
											createLearnerError(OperatorCapability.BINOMINAL_LABEL.getDescription(), fixes);
										}
									} else {
										if (!capabilityProvider.supportsCapability(OperatorCapability.POLYNOMINAL_LABEL)) {

											/*
											 * check if not nominal label is in fact a binominal
											 * label
											 */
											if (label.getValueSetRelation() != SetRelation.EQUAL
													|| label.getValueSet().size() != 2) {
												if (capabilityProvider
														.supportsCapability(OperatorCapability.BINOMINAL_LABEL)) {
													fixes.add(createToBinominalFix(label.getName()));
													fixes.addAll(getFixesForPolynomialClassificationWhenBinominalSupported());
												}
												if (capabilityProvider
														.supportsCapability(OperatorCapability.NUMERICAL_LABEL)) {
													fixes.add(createToNumericalFix(label.getName()));
												}
												createLearnerError(OperatorCapability.POLYNOMINAL_LABEL.getDescription(),
														fixes);
											} else {
												createLearnerError(OperatorCapability.BINOMINAL_LABEL.getDescription(),
														fixes);
											}
										}
									}
								}
							}
						} else if (label.isNumerical()
								&& !capabilityProvider.supportsCapability(OperatorCapability.NUMERICAL_LABEL)) {
							createLearnerError(OperatorCapability.NUMERICAL_LABEL.getDescription(),
									getFixesForRegressionWhenClassificationSupported(label));
						}
				}
			}
		}
	}

	/**
	 * This method has to return a collection of quick fixes which are appropriate when
	 * classification is supported and the data needs regression. The default implementation will
	 * return fixes for discretization.
	 */
	protected List<QuickFix> getFixesForRegressionWhenClassificationSupported(AttributeMetaData labelMD) {
		return AbstractDiscretizationOperator.createDiscretizationFixes(getInputPort(), labelMD.getName());
	}

	/**
	 * This method has to return a collection of quick fixes which are appropriate when regression
	 * is supported and the data needs classification.
	 */
	protected Collection<QuickFix> getFixesForClassificationWhenRegressionSupported() {
		return Collections.emptyList();
	}

	/**
	 * This has to return a list of appropriate quick fixes in the case, that only binominal labels
	 * are supported but the data contains polynomials.
	 */
	protected Collection<QuickFix> getFixesForPolynomialClassificationWhenBinominalSupported() {
		return Collections.emptyList();
	}

	/**
	 * Creates a quickfix to convert to nominal.
	 * 
	 * @param labelName
	 *            If null, regular attributes will be converted. Otherwise the special attribute
	 *            with the given name will be converted.
	 */
	protected QuickFix createToBinominalFix(final String labelName) {
		return new OperatorInsertionQuickFix("insert_nominal_to_binominal_" + (labelName != null ? "label" : "attributes"),
				new Object[0], 10, getInputPort()) {

			@Override
			public Operator createOperator() throws OperatorCreationException {
				Operator op = OperatorService.createOperator(NominalToBinominal.class);
				if (labelName != null) {
					op.setParameter(AttributeSubsetSelector.PARAMETER_FILTER_TYPE,
							AttributeSubsetSelector.CONDITION_NAMES[AttributeSubsetSelector.CONDITION_SINGLE]);
					op.setParameter(AttributeSubsetSelector.PARAMETER_INCLUDE_SPECIAL_ATTRIBUTES, "true");
					op.setParameter(SingleAttributeFilter.PARAMETER_ATTRIBUTE, labelName);
				}
				return op;
			}
		};
	}

	/**
	 * Creates a quickfix to convert to numerical.
	 * 
	 * @param labelName
	 *            If null, regular attributes will be converted. Otherwise the special attribute
	 *            with the given name will be converted.
	 */
	protected QuickFix createToNumericalFix(final String labelName) {
		return new OperatorInsertionQuickFix("insert_nominal_to_numerical_" + (labelName != null ? "label" : "attributes"),
				new Object[0], 10, getInputPort()) {

			@Override
			public Operator createOperator() throws OperatorCreationException {
				Operator op = OperatorService.createOperator(NominalToNumeric.class);
				if (labelName != null) {
					op.setParameter(AttributeSubsetSelector.PARAMETER_FILTER_TYPE,
							AttributeSubsetSelector.CONDITION_NAMES[AttributeSubsetSelector.CONDITION_REGULAR_EXPRESSION]);
					op.setParameter(AttributeSubsetSelector.PARAMETER_INCLUDE_SPECIAL_ATTRIBUTES, "true");
					op.setParameter(RegexpAttributeFilter.PARAMETER_REGULAR_EXPRESSION, labelName);
				}
				return op;
			}
		};
	}

	protected void createLearnerError(String description, List<? extends QuickFix> list) {
		String id = "Learner";
		if (capabilityProvider instanceof Operator) {
			id = ((Operator) capabilityProvider).getOperatorDescription().getName();
		}
		createError(Severity.ERROR, list, "learner_cannot_handle", new Object[] { id, description });
	}
}
