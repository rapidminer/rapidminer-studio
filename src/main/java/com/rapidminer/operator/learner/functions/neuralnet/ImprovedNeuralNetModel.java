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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.Tools;


/**
 * The model of the improved neural net.
 *
 * @author Ingo Mierswa
 */
public class ImprovedNeuralNetModel extends PredictionModel {

	private static final long serialVersionUID = -2206598483097451366L;

	private static final int OPERATOR_PROGRESS_STEPS = 1000;

	private static final ActivationFunction SIGMOID_FUNCTION = new SigmoidFunction();

	private static final ActivationFunction LINEAR_FUNCTION = new LinearFunction();

	private String[] attributeNames;

	private InputNode[] inputNodes = new InputNode[0];

	private InnerNode[] innerNodes = new InnerNode[0];

	private OutputNode[] outputNodes = new OutputNode[0];

	protected ImprovedNeuralNetModel(ExampleSet trainingExampleSet) {
		super(trainingExampleSet, ExampleSetUtilities.SetsCompareOption.ALLOW_SUPERSET,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		this.attributeNames = com.rapidminer.example.Tools.getRegularAttributeNames(trainingExampleSet);
	}

	/**
	 * Trains the model.
	 *
	 * @param exampleSet
	 * @param hiddenLayers
	 * @param maxCycles
	 * @param maxError
	 * @param learningRate
	 * @param momentum
	 * @param decay
	 * @param shuffle
	 * @param normalize
	 * @param randomGenerator
	 * @param operator
	 *            can be <code>null</code>, used to checkForStop
	 * @throws OperatorException
	 */
	public void train(ExampleSet exampleSet, List<String[]> hiddenLayers, int maxCycles, double maxError,
			double learningRate, double momentum, boolean decay, boolean shuffle, boolean normalize,
			RandomGenerator randomGenerator, Operator operator) throws OperatorException {
		Attribute label = exampleSet.getAttributes().getLabel();

		int numberOfClasses = getNumberOfClasses(label);

		// recalculate statistics for scaling
		if (normalize) {
			exampleSet.recalculateAllAttributeStatistics();
		} else {
			exampleSet.recalculateAttributeStatistics(label);
		}
		checkForStop(operator);

		// SETUP NN
		initInputLayer(exampleSet, normalize);

		double labelMin = exampleSet.getStatistics(label, Statistics.MINIMUM);
		double labelMax = exampleSet.getStatistics(label, Statistics.MAXIMUM);
		initOutputLayer(label, numberOfClasses, labelMin, labelMax, randomGenerator);

		initHiddenLayers(exampleSet, label, hiddenLayers, randomGenerator);

		// calculate total weight
		Attribute weightAttribute = exampleSet.getAttributes().getWeight();
		double totalWeight = 0;
		for (Example example : exampleSet) {
			double weight = 1.0d;
			if (weightAttribute != null) {
				weight = example.getValue(weightAttribute);
			}
			totalWeight += weight;
		}

		// shuffle data
		int[] exampleIndices = null;
		if (shuffle) {
			List<Integer> indices = new ArrayList<>(exampleSet.size());
			for (int i = 0; i < exampleSet.size(); i++) {
				indices.add(i);
			}
			Collections.shuffle(indices, randomGenerator);
			checkForStop(operator);
			exampleIndices = new int[indices.size()];
			int index = 0;
			for (int current : indices) {
				exampleIndices[index++] = current;
			}
		}

		// optimization loop
		for (int cycle = 0; cycle < maxCycles; cycle++) {
			checkForStop(operator);
			double error = 0;
			int maxSize = exampleSet.size();
			for (int index = 0; index < maxSize; index++) {
				checkForStop(operator);
				int exampleIndex = index;
				if (exampleIndices != null) {
					exampleIndex = exampleIndices[index];
				}

				Example example = exampleSet.getExample(exampleIndex);

				resetNetwork();

				calculateValue(example);

				double weight = 1.0;
				if (weightAttribute != null) {
					weight = example.getValue(weightAttribute);
				}

				double tempRate = learningRate * weight;
				if (decay) {
					tempRate /= cycle + 1;
				}

				error += calculateError(example) / numberOfClasses * weight;
				update(example, tempRate, momentum);
			}

			error /= totalWeight;

			if (error < maxError) {
				break;
			}

			if (Double.isInfinite(error) || Double.isNaN(error)) {
				if (learningRate <= Double.MIN_VALUE) {
					// if (Tools.isLessEqual(learningRate, 0.0d)) // should hardly happen.
					// Unfortunately wrong. See Bug 527 and its duplicates
					throw new OperatorException("Cannot reset network to a smaller learning rate.");
				}
				learningRate /= 2;
				train(exampleSet, hiddenLayers, maxCycles, maxError, learningRate, momentum, decay, shuffle, normalize,
						randomGenerator, operator);
			}
		}
	}

	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(exampleSet.size());
		}
		int progressCounter = 0;
		for (Example example : exampleSet) {
			resetNetwork();

			if (predictedLabel.isNominal()) {
				int numberOfClasses = getNumberOfClasses(getLabel());
				double[] classProbabilities = new double[numberOfClasses];
				for (int c = 0; c < numberOfClasses; c++) {
					classProbabilities[c] = outputNodes[c].calculateValue(true, example);
				}

				double total = 0.0;
				for (int c = 0; c < numberOfClasses; c++) {
					total += classProbabilities[c];
				}

				double maxConfidence = Double.NEGATIVE_INFINITY;
				int maxIndex = 0;
				for (int c = 0; c < numberOfClasses; c++) {
					classProbabilities[c] /= total;
					if (classProbabilities[c] > maxConfidence) {
						maxIndex = c;
						maxConfidence = classProbabilities[c];
					}
				}

				example.setValue(predictedLabel,
						predictedLabel.getMapping().mapString(getLabel().getMapping().mapIndex(maxIndex)));
				for (int c = 0; c < numberOfClasses; c++) {
					example.setConfidence(getLabel().getMapping().mapIndex(c), classProbabilities[c]);
				}
			} else {
				double value = outputNodes[0].calculateValue(true, example);
				example.setValue(predictedLabel, value);
			}
			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted(progressCounter);
			}
		}

		return exampleSet;
	}

	public String[] getAttributeNames() {
		return this.attributeNames;
	}

	public InputNode[] getInputNodes() {
		return this.inputNodes;
	}

	public OutputNode[] getOutputNodes() {
		return this.outputNodes;
	}

	public InnerNode[] getInnerNodes() {
		return this.innerNodes;
	}

	private int getNumberOfClasses(Attribute label) {
		int numberOfClasses = 1;
		if (label.isNominal()) {
			numberOfClasses = label.getMapping().size();
		}
		return numberOfClasses;
	}

	private void addNode(InnerNode node) {
		InnerNode[] newInnerNodes = new InnerNode[innerNodes.length + 1];
		System.arraycopy(innerNodes, 0, newInnerNodes, 0, innerNodes.length);
		newInnerNodes[newInnerNodes.length - 1] = node;
		innerNodes = newInnerNodes;
	}

	private void resetNetwork() {
		for (OutputNode outputNode : outputNodes) {
			outputNode.reset();
		}
	}

	private void update(Example example, double learningRate, double momentum) {
		for (OutputNode outputNode : outputNodes) {
			outputNode.update(example, learningRate, momentum);
		}
	}

	private void calculateValue(Example example) {
		for (OutputNode outputNode : outputNodes) {
			outputNode.calculateValue(true, example);
		}
	}

	private double calculateError(Example example) {
		for (InputNode inputNode : inputNodes) {
			inputNode.calculateError(true, example);
		}
		double totalError = 0.0d;
		for (OutputNode outputNode : outputNodes) {
			double error = outputNode.calculateError(false, example);
			totalError += error * error;
		}
		return totalError;
	}

	private int getDefaultLayerSize(ExampleSet exampleSet, Attribute label) {
		return (int) Math.round((exampleSet.getAttributes().size() + getNumberOfClasses(label)) / 2.0d) + 1;
	}

	private void initInputLayer(ExampleSet exampleSet, boolean normalize) {
		inputNodes = new InputNode[exampleSet.getAttributes().size()];
		int a = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			inputNodes[a] = new InputNode(attribute.getName());
			double range = 1;
			double offset = 0;
			if (normalize) {
				double min = exampleSet.getStatistics(attribute, Statistics.MINIMUM);
				double max = exampleSet.getStatistics(attribute, Statistics.MAXIMUM);
				range = (max - min) / 2;
				offset = (max + min) / 2;
			}
			inputNodes[a].setAttribute(attribute, range, offset, normalize);
			a++;
		}
	}

	private void initOutputLayer(Attribute label, int numberOfClasses, double min, double max,
			RandomGenerator randomGenerator) {
		double range = (max - min) / 2;
		double offset = (max + min) / 2;

		outputNodes = new OutputNode[numberOfClasses];
		for (int o = 0; o < numberOfClasses; o++) {
			if (!label.isNominal()) {
				outputNodes[o] = new OutputNode(label.getName(), label, range, offset);
			} else {
				outputNodes[o] = new OutputNode(label.getMapping().mapIndex(o), label, range, offset);
				outputNodes[o].setClassIndex(o);
			}
			InnerNode actualOutput = null;
			if (label.isNominal()) {
				String classValue = label.getMapping().mapIndex(o);
				actualOutput = new InnerNode("Class '" + classValue + "'", Node.OUTPUT, randomGenerator, SIGMOID_FUNCTION);
			} else {
				actualOutput = new InnerNode("Regression", Node.OUTPUT, randomGenerator, LINEAR_FUNCTION);
			}
			addNode(actualOutput);
			Node.connect(actualOutput, outputNodes[o]);
		}
	}

	private void initHiddenLayers(ExampleSet exampleSet, Attribute label, List<String[]> hiddenLayerList,
			RandomGenerator randomGenerator) {
		String[] layerNames = null;
		int[] layerSizes = null;
		if (hiddenLayerList.size() > 0) {
			layerNames = new String[hiddenLayerList.size()];
			layerSizes = new int[hiddenLayerList.size()];

			int index = 0;
			Iterator<String[]> i = hiddenLayerList.iterator();
			while (i.hasNext()) {
				String[] nameSizePair = i.next();
				layerNames[index] = nameSizePair[0];
				int layerSize = Integer.valueOf(nameSizePair[1]);
				if (layerSize <= 0) {
					layerSize = getDefaultLayerSize(exampleSet, label);
				}
				layerSizes[index] = layerSize;
				index++;
			}
		} else {
			// create at least one hidden layer if no other layers were created
			// log("No hidden layers defined. Using default hidden layer.");
			layerNames = new String[] { "Hidden" };
			layerSizes = new int[] { getDefaultLayerSize(exampleSet, label) };
		}

		int lastLayerSize = 0;
		for (int layerIndex = 0; layerIndex < layerNames.length; layerIndex++) {
			// String layerName = layerNames[layerIndex];
			int numberOfNodes = layerSizes[layerIndex];
			for (int nodeIndex = 0; nodeIndex < numberOfNodes; nodeIndex++) {
				InnerNode innerNode = new InnerNode("Node " + (nodeIndex + 1), layerIndex, randomGenerator,
						SIGMOID_FUNCTION);
				addNode(innerNode);
				if (layerIndex > 0) {
					// connect to all nodes of previous layer
					for (int i = innerNodes.length - nodeIndex - 1 - lastLayerSize; i < innerNodes.length - nodeIndex
							- 1; i++) {
						Node.connect(innerNodes[i], innerNode);
					}
				}
			}
			lastLayerSize = numberOfNodes;
		}

		int firstLayerSize = layerSizes[0];
		int numberOfAttributes = exampleSet.getAttributes().size();
		int numberOfClasses = getNumberOfClasses(label);
		if (firstLayerSize == 0) { // direct connection between in- and outputs
			for (int i = 0; i < numberOfAttributes; i++) {
				for (int o = 0; o < numberOfClasses; o++) {
					Node.connect(inputNodes[i], innerNodes[o]);
				}
			}
		} else {
			// connect input to first hidden layer
			for (int i = 0; i < numberOfAttributes; i++) {
				for (int o = numberOfClasses; o < numberOfClasses + firstLayerSize; o++) {
					Node.connect(inputNodes[i], innerNodes[o]);
				}
			}
			// connect last hidden layer to output
			for (int i = innerNodes.length - lastLayerSize; i < innerNodes.length; i++) {
				for (int o = 0; o < numberOfClasses; o++) {
					Node.connect(innerNodes[i], innerNodes[o]);
				}
			}
		}
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		int lastLayerIndex = -99;
		boolean first = true;
		for (InnerNode innerNode : innerNodes) {
			// skip outputs here and add them later

			// layer name
			int layerIndex = innerNode.getLayerIndex();
			if (layerIndex != Node.OUTPUT) {
				if (lastLayerIndex == -99 || lastLayerIndex != layerIndex) {
					if (!first) {
						result.append(Tools.getLineSeparators(2));
					}
					first = false;

					String layerName = "Hidden " + (layerIndex + 1);

					result.append(layerName + Tools.getLineSeparator());
					for (int t = 0; t < layerName.length(); t++) {
						result.append("=");
					}
					lastLayerIndex = layerIndex;
					result.append(Tools.getLineSeparator());
				}

				// node name and type
				String nodeName = innerNode.getNodeName() + " (" + innerNode.getActivationFunction().getTypeName() + ")";
				result.append(Tools.getLineSeparator() + nodeName + Tools.getLineSeparator());
				for (int t = 0; t < nodeName.length(); t++) {
					result.append("-");
				}
				result.append(Tools.getLineSeparator());

				// input weights
				double[] weights = innerNode.getWeights();
				Node[] inputNodes = innerNode.getInputNodes();
				for (int i = 0; i < inputNodes.length; i++) {
					result.append(inputNodes[i].getNodeName() + ": " + Tools.formatNumber(weights[i + 1])
							+ Tools.getLineSeparator());
				}

				// threshold weight
				result.append("Bias: " + Tools.formatNumber(weights[0]) + Tools.getLineSeparator());
			}
		}

		// add output nodes
		first = true;
		for (InnerNode innerNode : innerNodes) {
			// layer name
			int layerIndex = innerNode.getLayerIndex();
			if (layerIndex == Node.OUTPUT) {
				if (first) {
					result.append(Tools.getLineSeparators(2));
					String layerName = "Output";
					result.append(layerName + Tools.getLineSeparator());
					for (int t = 0; t < layerName.length(); t++) {
						result.append("=");
					}
					lastLayerIndex = layerIndex;
					result.append(Tools.getLineSeparator());
					first = false;
				}

				// node name and type
				String nodeName = innerNode.getNodeName() + " (" + innerNode.getActivationFunction().getTypeName() + ")";
				result.append(Tools.getLineSeparator() + nodeName + Tools.getLineSeparator());
				for (int t = 0; t < nodeName.length(); t++) {
					result.append("-");
				}
				result.append(Tools.getLineSeparator());

				// input weights
				double[] weights = innerNode.getWeights();
				Node[] inputNodes = innerNode.getInputNodes();
				for (int i = 0; i < inputNodes.length; i++) {
					result.append(inputNodes[i].getNodeName() + ": " + Tools.formatNumber(weights[i + 1])
							+ Tools.getLineSeparator());
				}

				// threshold weight
				result.append("Threshold: " + Tools.formatNumber(weights[0]) + Tools.getLineSeparator());
			}
		}
		return result.toString();
	}

	/**
	 * Checks if the {@link Operator} has requested stopping. If so, throws a
	 * {@link ProcessStoppedException}.
	 *
	 * @param operator
	 * @throws ProcessStoppedException
	 */
	private void checkForStop(Operator operator) throws ProcessStoppedException {
		if (operator != null) {
			operator.checkForStop();
		}
	}
}
