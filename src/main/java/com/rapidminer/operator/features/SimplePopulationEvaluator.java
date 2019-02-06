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
package com.rapidminer.operator.features;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.performance.PerformanceVector;


/**
 * This is the basic population operator for feature set evaluation schemes.
 *
 * @author Sebastian Land, Ingo Mierswa
 */
public class SimplePopulationEvaluator implements PopulationEvaluator {

	private ExampleSet originalSet;
	private FeatureOperator operator;

	public SimplePopulationEvaluator(FeatureOperator operator, ExampleSet originalSet) {
		this.originalSet = originalSet;
		this.operator = operator;
	}

	/**
	 * Evaluates the given individual. The performance is set as user data of the individual and
	 * also returned by this method.
	 */
	private final void evaluate(Individual individual) throws OperatorException {
		if (individual.getPerformance() == null) {

			double[] weights = individual.getWeights();
			ExampleSet clone = FeatureOperator.createCleanClone(originalSet, weights);

			PerformanceVector performanceVector = operator.executeEvaluationProcess(clone);
			individual.setPerformance(performanceVector);
		}
	}

	@Override
	public void evaluate(Population population) throws OperatorException {
		for (int i = 0; i < population.getNumberOfIndividuals(); i++) {
			evaluate(population.get(i));
			population.updateEvaluation();
			operator.getProgress().step();
		}
	}

}
