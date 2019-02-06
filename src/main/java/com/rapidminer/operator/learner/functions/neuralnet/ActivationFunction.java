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
package com.rapidminer.operator.learner.functions.neuralnet;

import com.rapidminer.example.Example;

import java.io.Serializable;


/**
 * This is the activation function of a neural net node. This class performs the calculation of the
 * node's output values as well as the error calculation and the update of the weights.
 * 
 * @author Ingo Mierswa
 */
public abstract class ActivationFunction implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Delivers the name of this activation function.
	 */
	public abstract String getTypeName();

	/**
	 * Calculates the value.
	 */
	public abstract double calculateValue(InnerNode node, Example example);

	/**
	 * Calculates the error.
	 */
	public abstract double calculateError(InnerNode node, Example example);

	/**
	 * Calculates the update of the weights.
	 */
	public void update(InnerNode node, Example example, double learningRate, double momentum) {
		Node[] inputs = node.getInputNodes();
		double[] weights = node.getWeights();
		double[] weightChanges = node.getWeightChanges();
		double delta = learningRate * node.calculateError(false, example);

		// threshold update
		double thresholdChange = delta + momentum * weightChanges[0];
		weights[0] += thresholdChange;
		weightChanges[0] = thresholdChange;

		// update node weights
		for (int i = 1; i < inputs.length + 1; i++) {
			double currentChange = delta * inputs[i - 1].calculateValue(false, example);
			currentChange += momentum * weightChanges[i];
			weights[i] += currentChange;
			weightChanges[i] = currentChange;
		}

		node.setWeights(weights);
		node.setWeightChanges(weightChanges);
	}

}
