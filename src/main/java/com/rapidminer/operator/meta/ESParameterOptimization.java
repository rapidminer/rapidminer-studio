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
package com.rapidminer.operator.meta;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.optimization.ec.es.ESOptimization;
import com.rapidminer.tools.math.optimization.ec.es.Individual;


/**
 * Evolutionary Strategy approach for an evolutionary parameter optimization.
 * 
 * @author Ingo Mierswa
 */
public class ESParameterOptimization extends ESOptimization {

	/** The parent operator. Used for fitness evaluation. */
	private EvolutionaryParameterOptimizationOperator operator;

	/** Creates a new evolutionary SVM optimization. */
	public ESParameterOptimization(EvolutionaryParameterOptimizationOperator operator, int individualSize, int initType, // start
																															// population
																															// creation
																															// type
																															// para
			int maxIterations, int generationsWithoutImprovement, int popSize, // GA paras
			int selectionType, double tournamentFraction, boolean keepBest, // selection paras
			int mutationType, // type of mutation
			double crossoverProb, boolean showPlot, RandomGenerator random, LoggingHandler logging) {

		super(0, 1, popSize, individualSize, initType, maxIterations, generationsWithoutImprovement, selectionType,
				tournamentFraction, keepBest, mutationType, crossoverProb, showPlot, false, random, logging, operator);
		this.operator = operator;

	}

	@Override
	public PerformanceVector evaluateIndividual(Individual individual) throws OperatorException {
		return operator.setParametersAndEvaluate(individual);
	}

	@Override
	public void nextIteration() throws OperatorException {
		this.operator.inApplyLoop();
	}
}
