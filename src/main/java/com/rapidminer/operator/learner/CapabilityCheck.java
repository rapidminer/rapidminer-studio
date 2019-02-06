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
package com.rapidminer.operator.learner;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.Ontology;


/**
 * Checks if the the given learner can work on the example set.
 * 
 * @author Ingo Mierswa
 */
public class CapabilityCheck {

	private CapabilityProvider capabilityProvider;

	private boolean onlyWarn;

	public CapabilityCheck(CapabilityProvider provider, boolean onlyWarn) {
		this.capabilityProvider = provider;
		this.onlyWarn = onlyWarn;
	}

	/**
	 * Checks if this learner can be used for the given example set, i.e. if it has sufficient
	 * capabilities.
	 */
	public void checkLearnerCapabilities(Operator learningOperator, ExampleSet exampleSet) throws OperatorException {
		try {
			// nominal attributes
			if (Tools.containsValueType(exampleSet, Ontology.NOMINAL)) {
				if (Tools.containsValueType(exampleSet, Ontology.BINOMINAL)) {
					if (!capabilityProvider.supportsCapability(OperatorCapability.BINOMINAL_ATTRIBUTES)) {
						throw new UserError(learningOperator, 501, learningOperator.getName(),
								OperatorCapability.BINOMINAL_ATTRIBUTES.getDescription());
					}
				} else {
					if (!capabilityProvider.supportsCapability(OperatorCapability.POLYNOMINAL_ATTRIBUTES)) {
						throw new UserError(learningOperator, 501, learningOperator.getName(),
								OperatorCapability.POLYNOMINAL_ATTRIBUTES.getDescription());
					}
				}
			}

			// numerical attributes
			if ((Tools.containsValueType(exampleSet, Ontology.NUMERICAL))
					&& !capabilityProvider.supportsCapability(OperatorCapability.NUMERICAL_ATTRIBUTES)) {
				throw new UserError(learningOperator, 501, learningOperator.getName(),
						OperatorCapability.NUMERICAL_ATTRIBUTES.getDescription());
			}

			// label
			Attribute labelAttribute = exampleSet.getAttributes().getLabel();
			if (labelAttribute != null) {
				if (labelAttribute.isNominal()) {
					if (labelAttribute.getMapping().size() == 1) {
						if (!(capabilityProvider.supportsCapability(OperatorCapability.ONE_CLASS_LABEL))) {
							throw new UserError(learningOperator, 502, learningOperator.getName());
						}
					} else {
						if (labelAttribute.getMapping().size() == 2) {
							if (!capabilityProvider.supportsCapability(OperatorCapability.BINOMINAL_LABEL)) {
								throw new UserError(learningOperator, 501, learningOperator.getName(),
										OperatorCapability.BINOMINAL_LABEL.getDescription());
							}
						} else {
							if (!capabilityProvider.supportsCapability(OperatorCapability.POLYNOMINAL_LABEL)) {
								throw new UserError(learningOperator, 501, learningOperator.getName(),
										OperatorCapability.POLYNOMINAL_LABEL.getDescription());
							}
						}
					}
				} else {
					if (labelAttribute.isNumerical()
							&& !capabilityProvider.supportsCapability(OperatorCapability.NUMERICAL_LABEL)) {
						throw new UserError(learningOperator, 501, learningOperator.getName(),
								OperatorCapability.NUMERICAL_LABEL.getDescription());
					}
				}
			} else {
				if (!(capabilityProvider.supportsCapability(OperatorCapability.NO_LABEL))) {
					throw new UserError(learningOperator, 501, learningOperator.getName(),
							OperatorCapability.NO_LABEL.getDescription());
				}
			}
			// missing values will only be checked with meta data to avoid data scan only for
			// capability check.
		} catch (UserError e) {
			if (onlyWarn) {
				learningOperator.logWarning(e.getMessage());
			} else {
				throw e;
			}
		}
	}
}
