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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;


/**
 * This class mirrors the behavior of an Operator's apply-method with respect to the I/O meta data.
 * This functionality is performed by a class of its own (rather than another method in Operator) to
 * keep Operator lean and since it is assumed that most meta data transformations can be handled by
 * a small set of standard rules.
 *
 * The general rule is that methods of this package should not throw exceptions but rather register
 * possible errors with the ports if preconditions are not satisfied etc.
 *
 * @author Simon Fischer
 */
public class MDTransformer {

	private final LinkedList<MDTransformationRule> transformationRules = new LinkedList<>();
	private final Operator operator;

	public MDTransformer(Operator op) {
		this.operator = op;
	}

	/** Executes all rules added by {@link #addRule}. */
	public void transformMetaData() {
		for (MDTransformationRule rule : new ArrayList<>(transformationRules)) {
			try {
				rule.transformMD();
			} catch (Exception e) {
				operator.getLogger().log(Level.WARNING, "Error during meta data transformation: " + e, e);
				operator.addError(new SimpleProcessSetupError(Severity.WARNING, operator.getPortOwner(),
						"exception_transforming_metadata", e.toString()));
			}
		}
	}

	public void addRule(MDTransformationRule rule) {
		transformationRules.add(rule);
	}

	/** Convenience method to generate a {@link PassThroughRule}. */
	public void addPassThroughRule(InputPort input, OutputPort output) {
		addRule(new PassThroughRule(input, output, false));
	}

	/** Convenience method to generate a {@link GenerateNewMDRule}. */
	public void addGenerationRule(OutputPort output, Class<? extends IOObject> clazz) {
		addRule(new GenerateNewMDRule(output, clazz));
	}

	public void clearRules() {
		transformationRules.clear();
	}

	public void addRuleAtBeginning(MDTransformationRule mdTransformationRule) {
		transformationRules.addFirst(mdTransformationRule);
	}

	/**
	 * @return an unmodifiable list of MDTransformationRules
	 */
	public final List<MDTransformationRule> getRules() {
		return Collections.unmodifiableList(transformationRules);
	}

}
