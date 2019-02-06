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


/**
 * This function represents a sigmoid activation function by calculating 1 / (1 + exp(- weighted
 * sum). The sigmoid function is usually used for the input and hidden layers and for the output
 * layer for classification problems.
 * 
 * @author Ingo Mierswa
 */
public class SigmoidFunction extends ActivationFunction {

	private static final long serialVersionUID = 1L;

	@Override
	public String getTypeName() {
		return "Sigmoid";
	}

	@Override
	public double calculateValue(InnerNode node, Example example) {
		Node[] inputs = node.getInputNodes();
		double[] weights = node.getWeights();
		double weightedSum = weights[0]; // bias
		for (int i = 0; i < inputs.length; i++) {
			weightedSum += inputs[i].calculateValue(true, example) * weights[i + 1];
		}

		double result = 0.0d;
		if (weightedSum < -45.0d) {
			result = 0;
		} else if (weightedSum > 45.0d) {
			result = 1;
		} else {
			result = 1 / (1 + Math.exp(-1 * weightedSum));
		}
		return result;
	}

	@Override
	public double calculateError(InnerNode node, Example example) {
		Node[] outputs = node.getOutputNodes();
		int[] numberOfOutputs = node.getOutputNodeInputIndices();
		double errorSum = 0;
		for (int i = 0; i < outputs.length; i++) {
			errorSum += outputs[i].calculateError(true, example) * outputs[i].getWeight(numberOfOutputs[i]);
		}
		double value = node.calculateValue(false, example);
		return errorSum * value * (1 - value);
	}
}
