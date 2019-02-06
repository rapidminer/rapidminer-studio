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
import com.rapidminer.tools.RandomGenerator;


/**
 * This class is used to represent a hidden node in the neural net. The base difference to the input
 * and output nodes is that the inner nodes performs the weight handling while the input nodes only
 * use the input values and the output nodes simply produce the output value(s).
 * 
 * @author Ingo Mierswa
 */
public class InnerNode extends Node {

	private static final long serialVersionUID = 8187951447455119892L;

	/** The weights for the inputs, the first weight is used for the threshold. */
	private double[] weights;

	/** The last change of the weights. */
	private double[] weightChanges;

	/** The random generator used for the creation of random numbers. */
	private RandomGenerator randomGenerator;

	/** The activation function for this hidden node. */
	private ActivationFunction function;

	public InnerNode(String nodeName, int layerIndex, RandomGenerator randomGenerator, ActivationFunction function) {
		super(nodeName, layerIndex, HIDDEN);
		this.randomGenerator = randomGenerator;
		this.function = function;
		weights = new double[] { this.randomGenerator.nextDouble() * 0.1d - 0.05d };
		weightChanges = new double[] { 0 };
	}

	public void setActivationFunction(ActivationFunction function) {
		this.function = function;
	}

	public ActivationFunction getActivationFunction() {
		return function;
	}

	@Override
	public double calculateValue(boolean shouldCalculate, Example example) {
		if (Double.isNaN(currentValue) && shouldCalculate) {
			currentValue = function.calculateValue(this, example);
		}
		return currentValue;
	}

	@Override
	public double calculateError(boolean shouldCalculate, Example example) {
		if (!Double.isNaN(currentValue) && Double.isNaN(currentError) && shouldCalculate) {
			currentError = function.calculateError(this, example);
		}
		return currentError;
	}

	@Override
	public double getWeight(int n) {
		return weights[n + 1];
	}

	public double[] getWeights() {
		return weights;
	}

	public void setWeights(double[] weights) {
		this.weights = weights;
	}

	public double[] getWeightChanges() {
		return weightChanges;
	}

	public void setWeightChanges(double[] weightChanges) {
		this.weightChanges = weightChanges;
	}

	@Override
	public void update(Example example, double learningRate, double momentum) {
		if (!areWeightsUpdated() && !Double.isNaN(currentError)) {
			function.update(this, example, learningRate, momentum);
			super.update(example, learningRate, momentum);
		}
	}

	/** Overwrites the super method and also adds weight handling. */
	@Override
	protected boolean connectInput(Node i, int n) {
		if (!super.connectInput(i, n)) {
			return false;
		}

		double[] newWeights = new double[weights.length + 1];
		System.arraycopy(weights, 0, newWeights, 0, weights.length);
		newWeights[newWeights.length - 1] = this.randomGenerator.nextDouble() * 0.1d - 0.05d;
		weights = newWeights;

		double[] newWeightChanges = new double[weightChanges.length + 1];
		System.arraycopy(weightChanges, 0, newWeightChanges, 0, weightChanges.length);
		newWeightChanges[newWeightChanges.length - 1] = 0;
		weightChanges = newWeightChanges;

		return true;
	}

	/** Overrides super method and also adds weights handling. */
	@Override
	protected boolean disconnectInput(Node inputNode, int inputNodeOutputIndex) {
		int deleteIndex = -1;
		boolean removed = false;
		int numberOfInputs = inputNodes.length;
		do {
			deleteIndex = -1;
			for (int i = 0; i < inputNodes.length; i++) {
				if (inputNode == inputNodes[i]
						&& (inputNodeOutputIndex == -1 || inputNodeOutputIndex == inputNodeOutputIndices[i])) {
					deleteIndex = i;
					break;
				}
			}

			if (deleteIndex >= 0) {
				for (int i = deleteIndex + 1; i < inputNodes.length; i++) {
					inputNodes[i - 1] = inputNodes[i];
					inputNodeOutputIndices[i - 1] = inputNodeOutputIndices[i];
					weights[i] = weights[i + 1];
					weightChanges[i] = weightChanges[i + 1];
					inputNodes[i - 1].outputNodeInputIndices[inputNodeOutputIndices[i - 1]] = i - 1;
				}
				numberOfInputs--;
				removed = true;
			}
		} while (inputNodeOutputIndex == -1 && deleteIndex != -1);

		Node[] newInputNodes = new Node[numberOfInputs];
		System.arraycopy(inputNodes, 0, newInputNodes, 0, numberOfInputs);
		inputNodes = newInputNodes;

		int[] newInputNodeOutputIndices = new int[numberOfInputs];
		System.arraycopy(inputNodeOutputIndices, 0, newInputNodeOutputIndices, 0, numberOfInputs);
		inputNodeOutputIndices = newInputNodeOutputIndices;

		double[] newWeights = new double[numberOfInputs + 1];
		System.arraycopy(weights, 0, newWeights, 0, numberOfInputs + 1);
		weights = newWeights;

		double[] newWeightChanges = new double[numberOfInputs + 1];
		System.arraycopy(weightChanges, 0, newWeightChanges, 0, numberOfInputs + 1);
		weightChanges = newWeightChanges;

		return removed;
	}
}
