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
package com.rapidminer.operator.features.selection;

import java.io.File;
import java.io.IOException;

import com.rapidminer.example.AttributeWeights;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.features.Individual;
import com.rapidminer.operator.features.Population;
import com.rapidminer.operator.features.PopulationOperator;
import com.rapidminer.tools.io.Encoding;


/**
 * This population operator writes the currently best weights into the specified file.
 *
 * @author Ingo Mierswa
 */
public class SaveIntermediateWeights implements PopulationOperator {

	private int whichGeneration;

	private AbstractGeneticAlgorithm operator;

	private String[] attributeNames;

	public SaveIntermediateWeights(AbstractGeneticAlgorithm operator, int whichGeneration, String[] attributeNames) {
		this.operator = operator;
		this.whichGeneration = whichGeneration;
		this.attributeNames = attributeNames;
	}

	@Override
	public void operate(Population pop) throws Exception {
		Individual bestIndividual = pop.getBestIndividualEver();
		if (bestIndividual != null) {
			File outputFile = operator.getParameterAsFile(AbstractGeneticAlgorithm.PARAMETER_INTERMEDIATE_WEIGHTS_FILE,
					true);
			if (outputFile != null) {
				double[] weightValues = bestIndividual.getWeights();
				if (weightValues.length == attributeNames.length) {
					AttributeWeights weights = new AttributeWeights();
					for (int i = 0; i < weightValues.length; i++) {
						weights.setWeight(attributeNames[i], weightValues[i]);
					}
					try {
						weights.writeAttributeWeights(outputFile, Encoding.getEncoding(operator.getRoot()));
					} catch (IOException e) {
						throw new UserError(operator, e, 303, new Object[] { outputFile, e.getMessage() });
					}
				}
			}
		}
	}

	@Override
	public boolean performOperation(int generation) {
		if (generation % whichGeneration == 0) {
			return true;
		} else {
			return false;
		}
	}
}
