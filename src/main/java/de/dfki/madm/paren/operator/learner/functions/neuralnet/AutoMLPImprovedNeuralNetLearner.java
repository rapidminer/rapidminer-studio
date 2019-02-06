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

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.functions.neuralnet.InnerNode;
import com.rapidminer.operator.learner.functions.neuralnet.Node;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.RandomGenerator;


/**
 *
 * @rapidminer.index Neural Net
 *
 * @author Ingo Mierswa, modified by Syed Atif Mehdi (01/09/2010)
 */

public class AutoMLPImprovedNeuralNetLearner extends AbstractLearner {

	// hidden layers have been removed. - atif

	/**
	 * The parameter name for &quot;The number of training cycles used for the neural network
	 * training.&quot;
	 */
	public static final String PARAMETER_TRAINING_CYCLES = "training_cycles";

	private static final String PARAMETER_MAX_GENERATIONS = "number_of_generations";

	private static final String PARAMETER_NUMBER_ENSEMBLES = "number_of_esemble_mlps";

	RandomGenerator randomGenerator;

	protected PerformanceVector performance;

	public AutoMLPImprovedNeuralNetLearner(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {

		int maxCycles = getParameterAsInt(PARAMETER_TRAINING_CYCLES); // training cycles
		int max_generations = getParameterAsInt(PARAMETER_MAX_GENERATIONS); // number of generations
		int nensemble = getParameterAsInt(PARAMETER_NUMBER_ENSEMBLES); // number of mlps in ensemble

		double eta_init = 0.5;

		int min_hidden = 5;
		int max_hidden = 300;

		double eta_varlog = 1.5; // eta variance in lognormal
		double hidden_varlog = 1.8; // nhidden variance in lognormal

		int generations = 0;

		double maxError = 0.0;
		double momentum = 0.5;
		boolean decay = false;
		boolean shuffle = true;
		boolean normalize = true;

		int hidden_lo = 20;
		int hidden_hi = 80;

		double cv_split = 0.8;

		randomGenerator = RandomGenerator.getRandomGenerator(this);

		AutoMLPImprovedNeuralNetModel model;
		AutoMLPImprovedNeuralNetModel[] old_models = new AutoMLPImprovedNeuralNetModel[nensemble];

		boolean[] is_old_models = new boolean[nensemble];
		double[] learningRate = new double[nensemble]; // getParameterAsDouble(PARAMETER_LEARNING_RATE);

		// hidden layers have been removed.... - atif
		List<String[]>[] hiddenLayers = new LinkedList[nensemble];

		// initialize the hidden layers and learning rates.
		for (int i = 0; i < nensemble; i++) {
			hiddenLayers[i] = new LinkedList<String[]>();
			is_old_models[i] = false;
			do {
				learningRate[i] = rlognormal(eta_init, eta_varlog);
			} while (learningRate[i] < 0 || learningRate[i] >= 1.0);

			int nn = logspace(i, nensemble, hidden_lo, hidden_hi);
			{
				// now initialize the hidden layers and train. - atif
				if (nn < max_hidden) {
					// hiddenLayers[i].clear();
					hiddenLayers[i].add(new String[] { "Hidden", Integer.toString(nn) });
				}
			}
		} // initialization complete

		SplittedExampleSet splittedES = new SplittedExampleSet(exampleSet, cv_split,
				1 /*
					 * samplingType = 1 for shuffeled split
					 */, false, 1992 /*
									 * this is the seed that is used .
									 */);

		// NOTE the following -atif

		AutoMlpThreaded autoMlpThread;

		// create the thread and start training. after training, change the structure of the NN for
		// the next generation
		do {
			splittedES.selectSingleSubset(0); // training data 0.8
			autoMlpThread = new AutoMlpThreaded(splittedES, nensemble, hiddenLayers, maxCycles, maxError, learningRate,
					momentum, decay, shuffle, normalize, randomGenerator, is_old_models, old_models);
			autoMlpThread.StartTraining();

			// wait until the training stops.
			while (autoMlpThread.isAlive() == true) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
			// stores the trained NN.
			for (int i = 0; i < nensemble; i++) {
				old_models[i] = autoMlpThread.GetModel(i);
				is_old_models[i] = true;
			}

			// Do the Cross Validation here. and use that error to model the neural nets.
			splittedES.selectSingleSubset(1);

			autoMlpThread.CrossValidate(splittedES); // although it is working.. but the next
													 // function seems much
			// better... - atif

			for (int i = 0; i < nensemble; i++) {
				old_models[i].error = calculateError(splittedES, old_models[i]);
			}

			// if training is not to be stopped, then change the structure of half of the
			// nensenmbles

			// sort the learning rate and old models based on errors
			quicksort(old_models, learningRate, 0, old_models.length - 1);

			// initialize the hidden layers of half of the good NN with the good NN's number of
			// hidden nodes
			for (int i = 0; i < nensemble / 2; i++) {
				hiddenLayers[i].clear();
				int current_size = 0;
				for (int k = 0; k < old_models[i].innerNodes.length; k++) {
					InnerNode old_innerNode = old_models[i].innerNodes[k];
					int old_layerIndex = old_innerNode.getLayerIndex();
					if (old_layerIndex != Node.OUTPUT) {
						current_size++;
					}
				}
				hiddenLayers[i].add(new String[] { "Hidden", Integer.toString(current_size) });
			}

			// now for the rest half of NN, change their structure and the learning rate

			for (int i = nensemble / 2, j = 0; i < nensemble; i++, j++) // this loop should run
																		 // nensemble/2
			{
				do {
					learningRate[i] = rlognormal(eta_init, eta_varlog);
				} while (learningRate[i] < 0 || learningRate[i] >= 1.0);
				// modify the rest by modifying the copy of best
				old_models[i] = old_models[j];

				int current_size = 0;
				for (int k = 0; k < old_models[j].innerNodes.length; k++) {
					InnerNode old_innerNode = old_models[j].innerNodes[k];
					int old_layerIndex = old_innerNode.getLayerIndex();
					if (old_layerIndex != Node.OUTPUT) {
						current_size++;
					}
				}
				int value = 0;
				do {
					value = (int) rlognormal(current_size, hidden_varlog);
				} while (value < 0);
				if (value > 0) // since -1 is the error state - atif
				{
					// now change the hidden layers and retrain. - atif
					int nn = 0;
					do {
						nn = Math.min(Math.max(min_hidden, value), max_hidden);
					} while (nn > max_hidden);
					if (nn < max_hidden) {
						hiddenLayers[i].clear();
						hiddenLayers[i].add(new String[] { "Hidden", Integer.toString(nn) });
					}
				}
			}

			generations++;
		} while (generations < max_generations);

		// old model is sorted based on the error.. therefore 0th index will have the least error
		model = old_models[0];
		return model;
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return AutoMLPImprovedNeuralNetModel.class;
	}

	/**
	 * Returns true for all types of attributes and numerical and binominal labels.
	 */
	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		if (lc == OperatorCapability.NUMERICAL_ATTRIBUTES) {
			return true;
		}
		if (lc == OperatorCapability.POLYNOMINAL_LABEL) {
			return true;
		}
		if (lc == OperatorCapability.BINOMINAL_LABEL) {
			return true;
		}
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeInt(PARAMETER_TRAINING_CYCLES,
				"The number of maximum training cycles used for the neural network training.", 1, Integer.MAX_VALUE, 10);
		type.setExpert(true);
		types.add(type);

		ParameterType type2 = new ParameterTypeInt(PARAMETER_MAX_GENERATIONS,
				"The number of generations for AutoMLP training.", 1, Integer.MAX_VALUE, 10);
		type2.setExpert(true);
		types.add(type2);

		ParameterType type3 = new ParameterTypeInt(PARAMETER_NUMBER_ENSEMBLES, "The number of MLPs per ensemble.", 1,
				Integer.MAX_VALUE, 4);
		type3.setExpert(true);
		types.add(type3);
		return types;
	}

	private double rlognormal(double etaInit, double r) {
		if (r > 1.0) {
			double result;
			do {
				double n = rnormal(Math.log(etaInit), Math.log(r));
				result = Math.exp(n);
			} while (Double.isNaN(result));
			return result;
		} else {
			return -1;
		}
	}

	private double rnormal(double d, double e) {
		return rnormal() * e + d;
	}

	private double rnormal() {
		double x, y, s;
		do {

			x = 2 * randomGenerator.nextGaussian() - 1;
			y = 2 * randomGenerator.nextGaussian() - 1;

			s = x * x + y * y;
		} while (s > 1.0);

		double retValue = 0.0;
		do {
			retValue = x * Math.sqrt(-Math.log(s) / s);

		} while (Double.isNaN(retValue));

		return retValue;
	}

	private int logspace(int i, int n, float lo, float hi) {
		Double d;
		do {
			d = Math.exp(i / (float) (n - 1) * (Math.log(hi) - Math.log(lo)) + Math.log(lo));
		} while (d.isNaN());
		return d.intValue();
	}

	// sort the array of old NN and also move the learning Rate according to the NN
	private void quicksort(AutoMLPImprovedNeuralNetModel[] old_nn, double[] lR, int low, int high) {
		int i = low, j = high;
		// Get the pivot element from the middle of the list
		// Using shift instead of division to avoid overflow if low or high get large.
		double pivot = old_nn[low + high >>> 1].getError();

		// Divide into two lists
		while (i <= j) {
			// If the current value from the left list is smaller or equal then the pivot
			// element then get the next element from the left list
			while (i < high && old_nn[i].getError() < pivot) {
				i++;
			}
			// If the current value from the right list is larger or equal then the pivot
			// element then get the next element from the right list
			while (j > low && old_nn[j].getError() > pivot) {
				j--;
			}

			// If we have found a values in the left list which is larger then
			// the pivot element and if we have found a value in the right list
			// which is smaller then the pivot element then we exchange the
			// values.
			// As we are done we can increase i and decrease j
			if (i <= j) {

				Swap(old_nn, lR, i, j);
				i++;
				j--;
			}
		}
		// Recursion
		if (low < j) {
			quicksort(old_nn, lR, low, j);
		}
		if (i < high) {
			quicksort(old_nn, lR, i, high);
		}
	}

	private void Swap(AutoMLPImprovedNeuralNetModel[] models, double[] lR, int index_1, int index_2) {
		AutoMLPImprovedNeuralNetModel temp = models[index_1];
		double l_temp = lR[index_1];

		models[index_1] = models[index_2];
		models[index_2] = temp;

		lR[index_1] = lR[index_2];
		lR[index_2] = l_temp;

	}

	protected float calculateError(ExampleSet exampleSet, AutoMLPImprovedNeuralNetModel model) {
		Attribute predictedLabel = exampleSet.getAttributes().getLabel();
		long count = 0;
		long misclassified = 0;
		for (Example example : exampleSet) {
			model.resetNetwork();
			count++;
			if (predictedLabel.isNominal()) {
				int numberOfClasses = model.getNumberOfClasses(predictedLabel);
				double[] classProbabilities = new double[numberOfClasses];
				for (int c = 0; c < numberOfClasses; c++) {
					classProbabilities[c] = model.outputNodes[c].calculateValue(true, example);
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
				if (maxIndex != example.getLabel()) {
					misclassified++;
				}
			}
		}
		return (float) misclassified / (float) count;
	}

}


class AutoMlpThreaded extends Thread {

	AutoMLPImprovedNeuralNetModel[] model;
	int nensembles = 1;
	ExampleSet exampleSet;
	List<String[]>[] hiddenLayers;
	int maxCycles;
	double maxError;
	double[] learningRate;
	double momentum;
	boolean decay;
	boolean shuffle;
	boolean normalize;
	RandomGenerator randomGenerator;
	boolean[] isOldModels;
	AutoMLPImprovedNeuralNetModel[] oldModels;

	AutoMlpThreaded(ExampleSet example, int nn, List<String[]>[] hidden_layers, int max_cycles, double max_Error,
			double[] learning_Rate, double moment, boolean is_decay, boolean is_shuffle, boolean is_normalize,
			RandomGenerator random_Generator, boolean[] is_old_models, AutoMLPImprovedNeuralNetModel[] old_models) {
		exampleSet = example;
		nensembles = nn;
		hiddenLayers = hidden_layers;
		maxCycles = max_cycles;
		maxError = max_Error;
		learningRate = learning_Rate;
		momentum = moment;
		decay = is_decay;
		shuffle = is_shuffle;
		normalize = is_normalize;
		randomGenerator = random_Generator;
		isOldModels = is_old_models;
		oldModels = old_models;

		model = new AutoMLPImprovedNeuralNetModel[nensembles];
		for (int i = 0; i < nensembles; i++) {
			model[i] = new AutoMLPImprovedNeuralNetModel(exampleSet);
		}
	}

	@Override
	public void run() {
		for (int i = 0; i < nensembles; i++) {
			model[i].train(exampleSet, hiddenLayers[i], maxCycles, maxError, learningRate[i], momentum, decay, shuffle,
					normalize, randomGenerator, isOldModels[i], oldModels[i]);
		}
	}

	public void StartTraining() {
		start(); // start the thread
	}

	void CrossValidate(ExampleSet splittedES) {
		for (int i = 0; i < nensembles; i++) {
			int maxSize = splittedES.size();
			double error = 0.0;
			for (int index = 0; index < maxSize; index++) {
				error += model[i].calculateError(splittedES.getExample(index));
			}
			// ::::TODO::::.. this is not the right way.. to setup the error value..
			// it is done here because have to test it first.
			// this also that in quicksort.. calls GetError() and over there error is required.
			model[i].error = error;
		}
	}

	public double[] GetModelsErrors() {
		double[] errors = new double[nensembles];
		for (int i = 0; i < nensembles; i++) {
			errors[i] = model[i].getError();
		}
		return errors;
	}

	public AutoMLPImprovedNeuralNetModel GetModel(int index) {
		return model[index];
	}
}
