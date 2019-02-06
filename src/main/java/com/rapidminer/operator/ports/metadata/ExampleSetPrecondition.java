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

import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ModelApplier;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.quickfix.ChangeAttributeRoleQuickFix;
import com.rapidminer.operator.ports.quickfix.OperatorInsertionQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.operator.preprocessing.IdTagging;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;

import java.util.LinkedList;
import java.util.List;


/**
 * @author Simon Fischer
 */
public class ExampleSetPrecondition extends AbstractPrecondition {

	private final String[] requiredSpecials;
	private final int allowedValueTypes;
	private final String[] ignoreForTypeCheck;
	private final int allowedSpecialsValueType;
	private final String[] requiredAttributes;
	private boolean optional = false;

	public ExampleSetPrecondition(InputPort inputPort) {
		this(inputPort, Ontology.ATTRIBUTE_VALUE, (String[]) null);
	}

	public ExampleSetPrecondition(InputPort inputPort, int allowedValueTypesForRegularAttributes, String... requiredSpecials) {
		this(inputPort, new String[0], allowedValueTypesForRegularAttributes, new String[0], Ontology.ATTRIBUTE_VALUE,
				requiredSpecials);
	}

	public ExampleSetPrecondition(InputPort inputPort, String[] requiredAttributeNames, int allowedValueTypesForRegular,
			String... requiredSpecials) {
		this(inputPort, requiredAttributeNames, allowedValueTypesForRegular, new String[0], Ontology.ATTRIBUTE_VALUE,
				requiredSpecials);
	}

	public ExampleSetPrecondition(InputPort inputPort, String requiredSpecials, int allowedValueTypForSpecial) {
		this(inputPort, new String[0], Ontology.ATTRIBUTE_VALUE, new String[0], allowedValueTypForSpecial, requiredSpecials);
	}

	public ExampleSetPrecondition(InputPort inputPort, String[] requiredAttributeNames, int allowedValueTypesForRegular,
			String[] ignoreForTypeCheck, int allowedValueTypesForSpecial, String... requiredSpecials) {
		super(inputPort);
		this.allowedValueTypes = allowedValueTypesForRegular;
		this.requiredSpecials = requiredSpecials;
		this.requiredAttributes = requiredAttributeNames;
		this.allowedSpecialsValueType = allowedValueTypesForSpecial;
		this.ignoreForTypeCheck = ignoreForTypeCheck;
	}

	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	@Override
	public void assumeSatisfied() {
		getInputPort().receiveMD(new ExampleSetMetaData());
	}

	@Override
	public void check(MetaData metaData) {
		final InputPort inputPort = getInputPort();
		if (metaData == null) {
			if (!optional) {
				inputPort.addError(new InputMissingMetaDataError(inputPort, ExampleSet.class, null));
			} else {
				return;
			}
		} else {
			if (metaData instanceof ExampleSetMetaData) {
				ExampleSetMetaData emd = (ExampleSetMetaData) metaData;
				// checking attribute names
				for (String attributeName : requiredAttributes) {
					MetaDataInfo attInfo = emd.containsAttributeName(attributeName);
					if (attInfo == MetaDataInfo.NO) {
						createError(Severity.WARNING, "missing_attribute", attributeName);
					}
				}

				// checking allowed types
				if ((allowedValueTypes != Ontology.ATTRIBUTE_VALUE) && (allowedValueTypes != -1)) {
					for (AttributeMetaData amd : emd.getAllAttributes()) {
						if (amd.isSpecial()) {
							continue;
						}
						// check if name is in ignore list
						for (String name : ignoreForTypeCheck) {
							if (name.equals(amd.getName())) {
								continue;
							}
						}

						// otherwise do check
						if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(amd.getValueType(), allowedValueTypes)) {
							createError(Severity.ERROR, "regular_type_mismatch",
									new Object[] { Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(allowedValueTypes) });
							break;
						}
					}
				}

				// checking required special attribute roles
				if (requiredSpecials != null) {
					for (String name : requiredSpecials) {
						MetaDataInfo has = emd.hasSpecial(name);
						switch (has) {
							case NO:
								List<QuickFix> fixes = new LinkedList<QuickFix>();
								// ID-Tagging
								if (name.equals(Attributes.ID_NAME)) {
									OperatorDescription[] ods = OperatorService.getOperatorDescriptions(IdTagging.class);
									fixes.add(new OperatorInsertionQuickFix("insert_id_tagging",
											new Object[] { ods.length > 0 ? ods[0].getName() : "" },
											10, inputPort) {

										@Override
										public Operator createOperator() throws OperatorCreationException {
											return OperatorService.createOperator(IdTagging.class);
										}
									});
								}
								// Prediction
								if (name.equals(Attributes.PREDICTION_NAME)) {
									OperatorDescription[] ods = OperatorService.getOperatorDescriptions(ModelApplier.class);
									if (ods.length > 0) {
										fixes.add(new OperatorInsertionQuickFix("insert_model_applier",
												new Object[] { ods[0].getName() }, 10, inputPort, 1, 0) {

											@Override
											public Operator createOperator() throws OperatorCreationException {
												return OperatorService.createOperator(ModelApplier.class);
											}
										});
									}
								}

								// General Attribute Role Change
								fixes.add(new ChangeAttributeRoleQuickFix(inputPort, name, "change_attribute_role", name));

								if (fixes.size() > 0) {
									inputPort.addError(new SimpleMetaDataError(Severity.ERROR, inputPort, fixes,
											"exampleset.missing_role", name));
								} else {
									createError(Severity.ERROR, "special_missing", new Object[] { name });
								}
								break;
							case UNKNOWN:
								createError(Severity.WARNING, "special_unknown", new Object[] { name });
								break;
							case YES:
								// checking type
								AttributeMetaData amd = emd.getSpecial(name);
								if (amd == null) {
									// TODO: This can happen for confidence. Then, hasSpecial
									// returns YES, but getSpecial(confidence) returns null
									break;
								}
								if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(amd.getValueType(), allowedSpecialsValueType)) {
									createError(Severity.ERROR, "special_attribute_has_wrong_type", amd.getName(), name,
											Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(allowedSpecialsValueType));
								}
								break;
						}
					}
				}
				try {
					makeAdditionalChecks(emd);
				} catch (UndefinedParameterError e) {
				}
			} else {
				inputPort.addError(new MetaDataUnderspecifiedError(inputPort));
			}
		}
	}

	/**
	 * Can be implemented by subclasses.
	 * 
	 * @throws UndefinedParameterError
	 */
	public void makeAdditionalChecks(ExampleSetMetaData emd) throws UndefinedParameterError {}

	@Override
	public String getDescription() {
		return "<em>expects:</em> ExampleSet";
	}

	@Override
	public boolean isCompatible(MetaData input, CompatibilityLevel level) {
		return null != input && ExampleSet.class.isAssignableFrom(input.getObjectClass());
	}

	@Override
	public MetaData getExpectedMetaData() {
		return new ExampleSetMetaData();
	}
}
