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

import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;

import java.util.Collection;
import java.util.LinkedList;


/**
 * Passes meta data from an input port to an output port or generates a new one if the input meta
 * data is null.
 * 
 * @author Simon Fischer
 * 
 */
public class PassThroughOrGenerateRule implements MDTransformationRule {

	private InputPort inputPort;
	private OutputPort outputPort;
	private MetaData generatedMetaData;
	private Collection<PassThroughOrGenerateRuleCondition> passThroughConditions = new LinkedList<PassThroughOrGenerateRuleCondition>();
	private Collection<PassThroughOrGenerateRuleCondition> generateConditions = new LinkedList<PassThroughOrGenerateRuleCondition>();

	public PassThroughOrGenerateRule(InputPort inputPort, OutputPort outputPort, MetaData generatedMetaData) {
		this.inputPort = inputPort;
		this.outputPort = outputPort;
		this.generatedMetaData = generatedMetaData;
	}

	@Override
	public void transformMD() {
		MetaData inputMD = inputPort.getMetaData();
		if (inputMD != null) {
			boolean ok = true;
			for (PassThroughOrGenerateRuleCondition condition : passThroughConditions) {
				if (!condition.conditionFullfilled()) {
					condition.registerErrors();
					ok = false;
				}
			}
			if (ok) {
				outputPort.deliverMD(transformPassedThrough(inputMD.clone()));
			}
		} else {
			boolean ok = true;
			for (PassThroughOrGenerateRuleCondition condition : generateConditions) {
				if (!condition.conditionFullfilled()) {
					condition.registerErrors();
					ok = false;
				}
			}
			if (ok) {
				outputPort.deliverMD(transformGenerated(generatedMetaData.clone()));
			}
		}
	}

	/**
	 * Can be overridden to make additional transformations to the generated meta data.
	 */
	public MetaData transformGenerated(MetaData md) {
		md.addToHistory(outputPort);
		return md;
	}

	/**
	 * Can be overridden to make additional transformations to the meta data passed through from the
	 * input port.
	 */
	public MetaData transformPassedThrough(MetaData md) {
		md.addToHistory(outputPort);
		return md;
	}

	/**
	 * this method allows to add additional conditions for passing through
	 * 
	 * @param condition
	 */
	public void addPassThroughCondition(PassThroughOrGenerateRuleCondition condition) {

		passThroughConditions.add(condition);
	}

	/**
	 * This allows to add additional conditions for generation
	 * 
	 * @param condition
	 */
	public void addGenerateCondition(PassThroughOrGenerateRuleCondition condition) {
		generateConditions.add(condition);
	}
}
