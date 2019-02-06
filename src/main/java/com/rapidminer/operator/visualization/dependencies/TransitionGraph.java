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
package com.rapidminer.operator.visualization.dependencies;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ResultObjectAdapter;
import com.rapidminer.tools.Tools;


/**
 * This is the result of the TransitionGraphOperator, i.e. a graph representing connections between
 * items (can be used for network visualizations).
 * 
 * @author Ingo Mierswa
 */
public class TransitionGraph extends ResultObjectAdapter {

	private static final long serialVersionUID = -4132479136625747895L;

	private String sourceAttribute;

	private String targetAttribute;

	private String strengthAttribute;

	private String typeAttribute;

	private String nodeDescription;

	private ExampleSet exampleSet;

	public TransitionGraph(ExampleSet exampleSet, String sourceAttribute, String targetAttribute, String strengthAttribute,
			String typeAttribute, String nodeDescription) {
		this.sourceAttribute = sourceAttribute;
		this.targetAttribute = targetAttribute;
		this.strengthAttribute = strengthAttribute;
		this.typeAttribute = typeAttribute;
		this.nodeDescription = nodeDescription;
		this.exampleSet = exampleSet;
	}

	public String getSourceAttribute() {
		return sourceAttribute;
	}

	public String getTargetAttribute() {
		return targetAttribute;
	}

	public String getStrengthAttribute() {
		return strengthAttribute;
	}

	public String getTypeAttribute() {
		return typeAttribute;
	}

	public String getNodeDescription() {
		return nodeDescription;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(getName() + Tools.getLineSeparator());
		result.append(Tools.getLineSeparator() + "Source Attribute: " + sourceAttribute + Tools.getLineSeparator());
		result.append(Tools.getLineSeparator() + "Target Attribute: " + targetAttribute + Tools.getLineSeparator());
		if (strengthAttribute != null) {
			result.append(Tools.getLineSeparator() + "Strength Attribute: " + strengthAttribute + Tools.getLineSeparator());
		}
		if (typeAttribute != null) {
			result.append(Tools.getLineSeparator() + "Type Attribute: " + typeAttribute + Tools.getLineSeparator());
		}
		return result.toString();
	}

	@Override
	public String getName() {
		return "Transition Graph";
	}

	public String getExtension() {
		return "tgr";
	}

	public String getFileDescription() {
		return "Transition Graph";
	}

	public ExampleSet getExampleSet() {
		return exampleSet;
	}
}
