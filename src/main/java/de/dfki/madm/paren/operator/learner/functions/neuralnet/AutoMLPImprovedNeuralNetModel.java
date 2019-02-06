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
package de.dfki.madm.paren.operator.learner.functions.neuralnet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.functions.neuralnet.ActivationFunction;
import com.rapidminer.operator.learner.functions.neuralnet.InnerNode;
import com.rapidminer.operator.learner.functions.neuralnet.InputNode;
import com.rapidminer.operator.learner.functions.neuralnet.LinearFunction;
import com.rapidminer.operator.learner.functions.neuralnet.Node;
import com.rapidminer.operator.learner.functions.neuralnet.OutputNode;
import com.rapidminer.operator.learner.functions.neuralnet.SigmoidFunction;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.Tools;


/**
 * The model of the improved neural net.
 *
 * @author Ingo Mierswa, modified by Syed Atif Mehdi (01/09/2010)
 */

public class AutoMLPImprovedNeuralNetModel extends PredictionModel {

	private static final long serialVersionUID = -2206598483097451366L;

	public static final ActivationFunction SIGMOID_FUNCTION = new SigmoidFunction();

	public static final ActivationFunction LINEAR_FUNCTION = new LinearFunction();

	private static final int OPERATOR_PROGRESS_STEPS = 100;

	public String[] attributeNames;

	public InputNode[] inputNodes = new InputNode[0];

	public InnerNode[] innerNodes = new InnerNode[0];

	public OutputNode[] outputNodes = new OutputNode[0];
	double error;

	public double getError() {
		return error;
	}

	public AutoMLPImprovedNeuralNetModel(ExampleSet trainingExampleSet) {
		super(trainingExampleSet, ExampleSetUtilities.SetsCompareOption.ALLOW_SUPERSET,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		this.attributeNames = com.rapidminer.example.Tools.getRegularAttributeNames(trainingExampleSet);
	}

	public void train(ExampleSet exampleSet, List<String[]> hiddenLayers, int maxCycles, double maxError,
			double learningRate, double momentum, boolean decay, boolean shuffle, boolean normalize,
			RandomGenerator randomGenerator, boolean is_old_model, AutoMLPImprovedNeuralNetModel old_model) {

		Attribute label = exampleSet.getAttributes().getLabel();
		int numberOfClasses = getNumberOfClasses(label);
		// recalculate statistics for scaling
		if (normalize) {
			exampleSet.recalculateAllAttributeStatistics();
		} else {
			exampleSet.recalculateAttributeStatistics(label);
		}

		// SETUP NN
		initInputLayer(exampleSet, normalize);

		double labelMin = exampleSet.getStatistics(label, Statistics.MINIMUM);
		double labelMax = exampleSet.getStatistics(label, Statistics.MAXIMUM);
		initOutputLayer(label, numberOfClasses, labelMin, labelMax, randomGenerator);

		if (is_old_model == false) {
			initHiddenLayers(exampleSet, label, hiddenLayers, randomGenerator);
		} else		// an old model has been provided, so initialize the hidden layers with previous
		// knowledge.
		{
			initHiddenLayers(exampleSet, label, hiddenLayers, randomGenerator, old_model);
		}
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
			List<Integer> indices = new ArrayList<Integer>(exampleSet.size());
			for (int i = 0; i < exampleSet.size(); i++) {
				indices.add(i);
			}
			Collections.shuffle(indices, randomGenerator);
			exampleIndices = new int[indices.size()];
			int index = 0;
			for (int current : indices) {
				exampleIndices[index++] = current;
			}
		}

		// optimization loop
		for (int cycle = 0; cycle < maxCycles; cycle++) {
			error = 0;
			int maxSize = exampleSet.size();
			for (int index = 0; index < maxSize; index++) {
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
			if (Double.isInfinite(error) || Double.isNaN(error)) {
				if (Tools.isLessEqual(learningRate, 0.0d)) // should hardly happen
				{
					throw new RuntimeException("Cannot reset network to a smaller learning rate.");
				}
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

	public int getNumberOfClasses(Attribute label) {
		int numberOfClasses = 1;
		if (label.isNominal()) {
			numberOfClasses = label.getMapping().size();
		}
		return numberOfClasses;
	}

	public void addNode(InnerNode node) {
		InnerNode[] newInnerNodes = new InnerNode[innerNodes.length + 1];
		System.arraycopy(innerNodes, 0, newInnerNodes, 0, innerNodes.length);
		newInnerNodes[newInnerNodes.length - 1] = node;
		innerNodes = newInnerNodes;
	}

	public void resetNetwork() {
		for (int i = 0; i < outputNodes.length; i++) {
			outputNodes[i].reset();
		}
	}

	public void update(Example example, double learningRate, double momentum) {
		for (int i = 0; i < outputNodes.length; i++) {
			outputNodes[i].update(example, learningRate, momentum);
		}
	}

	public void calculateValue(Example example) {
		for (int i = 0; i < outputNodes.length; i++) {
			outputNodes[i].calculateValue(true, example);
		}
	}

	public double calculateError(Example example) {
		for (int i = 0; i < inputNodes.length; i++) {
			inputNodes[i].calculateError(true, example);
		}
		double totalError = 0.0d;
		for (int i = 0; i < outputNodes.length; i++) {
			double error = outputNodes[i].calculateError(false, example);
			totalError += error * error;
		}
		return totalError;
	}

	public int getDefaultLayerSize(ExampleSet exampleSet, Attribute label) {
		return (int) Math.round((exampleSet.getAttributes().size() + getNumberOfClasses(label)) / 2.0d) + 1;
	}

	public void initInputLayer(ExampleSet exampleSet, boolean normalize) {
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

	public void initOutputLayer(Attribute label, int numberOfClasses, double min, double max,
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

	public void initHiddenLayers(ExampleSet exampleSet, Attribute label, List<String[]> hiddenLayerList,
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
			log("No hidden layers defined. Using default hidden layer.");
			layerNames = new String[] { "Hidden" };
			layerSizes = new int[] { getDefaultLayerSize(exampleSet, label) };
		}

		int lastLayerSize = 0;
		for (int layerIndex = 0; layerIndex < layerNames.length; layerIndex++) {
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

	public void initHiddenLayers(ExampleSet exampleSet, Attribute label, List<String[]> hiddenLayerList,
			RandomGenerator randomGenerator, AutoMLPImprovedNeuralNetModel old_model) {
		// create the hidden layers as usual
		initHiddenLayers(exampleSet, label, hiddenLayerList, randomGenerator);
		// now initialize the layers according to the previously trained network's hidden layer's
		// weights.
		for (int i = 0; i < old_model.innerNodes.length && i < innerNodes.length; i++) {
			InnerNode old_innerNode = old_model.innerNodes[i];
			InnerNode new_innerNode = innerNodes[i];

			int old_layerIndex = old_innerNode.getLayerIndex();
			int new_layerIndex = new_innerNode.getLayerIndex();
			if (old_layerIndex == new_layerIndex && old_layerIndex != Node.OUTPUT) {
				double[] old_weights = old_innerNode.getWeights();
				double[] new_weights = new_innerNode.getWeights();

				int length = old_innerNode.getInputNodes().length;		// input nodes count should
																			// be
				// the same for both the nets .
				// copies all the weights and also the bias which is at index 0
				for (int j = 0; j <= length; j++) {
					new_weights[j] = old_weights[j];
				}
				innerNodes[i].setWeights(new_weights);
			}
		}
		for (int i = 0; i < old_model.innerNodes.length && i < innerNodes.length; i++) {
			InnerNode old_innerNode = old_model.innerNodes[i];
			InnerNode new_innerNode = innerNodes[i];

			int old_layerIndex = old_innerNode.getLayerIndex();
			int new_layerIndex = new_innerNode.getLayerIndex();
			if (old_layerIndex == new_layerIndex && old_layerIndex == Node.OUTPUT) {
				double[] old_weights = old_innerNode.getWeights();
				double[] new_weights = new_innerNode.getWeights();

				// here the length of input nodes may vary, since this time input node is the hidden
				// layer node
				int length = old_innerNode.getInputNodes().length;
				int length1 = new_innerNode.getInputNodes().length;
				// copies all the weights and also the bias which is at index 0
				for (int j = 0; j <= length && j <= length1; j++) {
					new_weights[j] = old_weights[j];
				}
				innerNodes[i].setWeights(new_weights);
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
				result.append("Threshold: " + Tools.formatNumber(weights[0]) + Tools.getLineSeparator());
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

}
