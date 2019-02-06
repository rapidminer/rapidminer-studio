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

import com.rapidminer.operator.ResultObjectAdapter;

import java.util.List;


/**
 * Displays the result of an ANOVA matrix calculation.
 * 
 * @author Ingo Mierswa
 */
public class ANOVAMatrix extends ResultObjectAdapter {

	private static final long serialVersionUID = 6245314851143584397L;

	private double[][] probabilities;

	private List<String> anovaAttributeNames;

	private List<String> groupNames;

	private double significanceLevel;

	public ANOVAMatrix(double[][] probabilities, List<String> anovaAttributeNames, List<String> groupNames,
			double significanceLevel) {
		this.probabilities = probabilities;
		this.anovaAttributeNames = anovaAttributeNames;
		this.groupNames = groupNames;
		this.significanceLevel = significanceLevel;
	}

	public double[][] getProbabilities() {
		return probabilities;
	}

	public List<String> getAnovaAttributeNames() {
		return anovaAttributeNames;
	}

	public List<String> getGroupingAttributeNames() {
		return groupNames;
	}

	public double getSignificanceLevel() {
		return significanceLevel;
	}

	@Override
	public String toResultString() {
		StringBuffer result = new StringBuffer();

		return result.toString();
	}

	@Override
	public String toString() {
		return "ANOVA matrix indicating which attributes provide significant differences "
				+ "between groups defined by other (nominal) attributes.";
	}

	public String getExtension() {
		return "ano";
	}

	public String getFileDescription() {
		return "anova matrix";
	}
}
