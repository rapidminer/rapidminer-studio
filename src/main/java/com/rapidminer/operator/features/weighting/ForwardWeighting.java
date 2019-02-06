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
package com.rapidminer.operator.features.weighting;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.features.Individual;
import com.rapidminer.operator.features.Population;
import com.rapidminer.operator.features.PopulationOperator;


/**
 * This operator performs the weighting under the naive assumption that the features are independent
 * from each other. Each attribute is weighted with a linear search. This approach may deliver good
 * results after short time if the features indeed are not highly correlated.
 *
 * @author Ingo Mierswa
 */
public class ForwardWeighting extends FeatureWeighting {

	public ForwardWeighting(OperatorDescription description) {
		super(description);
	}

	@Override
	public PopulationOperator getWeightingOperator(String parameter) {
		double[] weights = new double[] { 0.25d, 0.5d, 0.75d, 1.0d };
		if (parameter != null && parameter.length() != 0) {
			try {
				String[] weightStrings = parameter.split(" ");
				weights = new double[weightStrings.length];
				for (int i = 0; i < weights.length; i++) {
					weights[i] = Double.parseDouble(weightStrings[i]);
				}
			} catch (Exception e) {
				logError("Could not create weights: " + e.getMessage() + "! Use standard weights.");
				weights = new double[] { 0.25d, 0.5d, 0.75d, 1.0d };
			}
		}
		return new SimpleWeighting(0.0d, weights);
	}

	@Override
	public Population createInitialPopulation(ExampleSet es) {
		Population initPop = new Population();
		for (int i = 0; i < es.getAttributes().size(); i++) {
			double[] weights = new double[es.getAttributes().size()];
			weights[i] = 1.0d;
			initPop.add(new Individual(weights));
		}
		return initPop;
	}
}
