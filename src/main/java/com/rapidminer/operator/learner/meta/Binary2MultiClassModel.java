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
package com.rapidminer.operator.learner.meta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Tools;


/**
 * This operator uses an inner learning scheme which is able to perform predictions for binary or
 * binominal classification problems and learns a set of these binary models in order to use this
 * set for a given data set with more than two classes.
 *
 * @author Helge Homburg, Ingo Mierswa
 */
public class Binary2MultiClassModel extends PredictionModel implements MetaModel {

	private static final long serialVersionUID = -8146985010710684043L;

	private static final int ONE_AGAINST_ALL = 0;

	private static final int ONE_AGAINST_ONE = 1;

	private static final int EXHAUSTIVE_CODE = 2;

	private static final int RANDOM_CODE = 3;

	private static final int OPERATOR_PROGRESS_STEPS = 1000;

	private final Model[] models;

	private LinkedList<String> modelNames;

	private String[][] codeMatrix;

	private final int classificationType;

	public Binary2MultiClassModel(ExampleSet exampleSet, Model[] models, int classificationType,
			LinkedList<String> modelNames) {
		super(exampleSet, null, null);
		this.models = models;
		this.classificationType = classificationType;
		this.modelNames = modelNames;
	}

	public Binary2MultiClassModel(ExampleSet exampleSet, Model[] models, int classificationType, String[][] codeMatrix) {
		super(exampleSet, null, null);
		this.models = models;
		this.classificationType = classificationType;
		this.codeMatrix = codeMatrix;
	}

	public int getNumberOfModels() {
		return models.length;
	}

	/** Returns a binary decision model for the given classification index. */
	public Model getModel(int index) {
		return models[index];
	}

	/**
	 * This method applies the models and evaluates the results of a multi class classification
	 * process according to a classification strategy that does not use error-correcting output
	 * codes. (currently supported: "1 against all", "1 against 1")
	 *
	 * @param originalExampleSet
	 * @param classificationStrategy
	 * @throws OperatorException
	 */
	private void startNonECOCProcess(ExampleSet originalExampleSet, int classificationStrategy) throws OperatorException {

		ExampleSet exampleSet = (ExampleSet) originalExampleSet.clone();
		int numberOfClasses = getLabel().getMapping().getValues().size();

		// Hash maps are used for addressing particular class values using
		// indices without relying
		// upon a consistent index distribution of the corresponding
		// substructure.
		int currentNumber = 0;
		HashMap<Integer, Integer> classIndexMap = new HashMap<Integer, Integer>(numberOfClasses);
		for (String currentClass : getLabel().getMapping().getValues()) {

			classIndexMap.put(currentNumber, getLabel().getMapping().mapString(currentClass));
			currentNumber++;
		}

		double[][] confidenceMatrix = new double[exampleSet.size()][getNumberOfModels()];

		// 1. Iterate over all models and all examples for every model to
		// receive all confidence
		// values.
		for (int k = 0; k < confidenceMatrix[0].length; k++) {

			Model model = getModel(k);
			exampleSet = model.apply(exampleSet);

			Iterator<Example> reader = exampleSet.iterator();
			int counter = 0;

			while (reader.hasNext()) {

				Example example = reader.next();

				if (classificationStrategy == ONE_AGAINST_ONE) {
					for (String className : exampleSet.getAttributes().getPredictedLabel().getMapping().getValues()) {
						confidenceMatrix[counter][getLabel().getMapping().mapString(className)] += example
								.getConfidence(className);
					}
				} else {
					Integer index = classIndexMap.get(k);
					confidenceMatrix[counter][k] = example.getConfidence(getLabel().getMapping().mapIndex(index));
				}
				counter++;
			}
			PredictionModel.removePredictedLabel(exampleSet);
		}

		Iterator<Example> reader = originalExampleSet.iterator();
		int counter = 0;

		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(originalExampleSet.size());
		}

		// 2. Iterate again over all examples to compute a prediction and a
		// confidence distribution
		// for
		// all examples depending on the results of step 1.
		while (reader.hasNext()) {

			Example example = reader.next();
			double confidenceSum = 0.0, currentConfidence = 0.0, bestConfidence = Double.NEGATIVE_INFINITY;
			int bestIndex = -1;

			for (int i = 0; i < confidenceMatrix[counter].length; i++) {

				currentConfidence = confidenceMatrix[counter][i];
				if (currentConfidence > bestConfidence) {
					bestConfidence = currentConfidence;
					bestIndex = i;
				}
				confidenceSum += currentConfidence;
			}

			example.setPredictedLabel(classIndexMap.get(bestIndex));

			for (int i = 0; i < numberOfClasses; i++) {
				example.setConfidence(getLabel().getMapping().mapIndex(classIndexMap.get(i)),
						confidenceMatrix[counter][i] / confidenceSum);
			}

			counter++;
			if (progress != null && counter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted(counter);
			}
		}
	}

	/**
	 * This method applies the models and evaluates the results of a multi class classification
	 * process according to a classification strategy that uses error-correcting output codes.
	 * (currently available: "exhaustive code", "random code")
	 *
	 * @param originalExampleSet
	 * @param codeMatrix
	 * @throws OperatorException
	 */
	private void startECOCProcess(ExampleSet originalExampleSet, String[][] codeMatrix) throws OperatorException {

		ExampleSet exampleSet = (ExampleSet) originalExampleSet.clone();
		int numberOfClasses = codeMatrix.length;

		// Hash maps are used for addressing particular class values using
		// indices without relying
		// upon a consistent index distribution of the corresponding
		// substructure.
		int currentNumber = 0;
		HashMap<Integer, String> classIndexMap = new HashMap<Integer, String>(numberOfClasses);
		for (String currentClass : getLabel().getMapping().getValues()) {

			classIndexMap.put(currentNumber, currentClass);
			currentNumber++;
		}

		// 1. Transform the code words from true/false to 1.0/0.0 for an easier
		// application.
		double[][] codeWords = new double[codeMatrix.length][codeMatrix[0].length];
		for (int i = 0; i < codeMatrix.length; i++) {
			for (int j = 0; j < codeMatrix[0].length; j++) {
				codeWords[i][j] = "true".equals(codeMatrix[i][j]) ? 1.0 : 0.0;
			}
		}

		double[][] confidenceMatrix = new double[exampleSet.size()][getNumberOfModels()];
		String currentLabel;
		double currentConfidence;

		// 2. Iterate over all models and all examples for every model to
		// receive all confidence
		// values.
		for (int k = 0; k < confidenceMatrix[0].length; k++) {

			Model model = getModel(k);
			exampleSet = model.apply(exampleSet);

			Iterator<Example> reader = exampleSet.iterator();
			int counter = 0;

			while (reader.hasNext()) {

				Example example = reader.next();
				Attribute predictedLabel = example.getAttributes().getPredictedLabel();
				currentLabel = predictedLabel.getMapping().mapIndex((int) example.getValue(predictedLabel));
				currentConfidence = example.getConfidence(currentLabel);
				confidenceMatrix[counter][k] = "true".equals(currentLabel) ? currentConfidence : 1 - currentConfidence;
				counter++;
			}

			PredictionModel.removePredictedLabel(exampleSet);
		}

		Iterator<Example> reader = originalExampleSet.iterator();
		int counter = 0;

		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(originalExampleSet.size());
		}

		// 3. Iterate again over all examples to compute a prediction and a
		// confidence distribution
		// for
		// all examples depending on the results of step 2.
		while (reader.hasNext()) {

			Example example = reader.next();
			int bestIndex = -1;
			double[] confidenceVector = new double[numberOfClasses];
			double bestConfidence = Double.POSITIVE_INFINITY;

			for (int i = 0; i < numberOfClasses; i++) {
				confidenceVector[i] = 0.0;
				for (int j = 0; j < confidenceMatrix[counter].length; j++) {
					confidenceVector[i] = confidenceVector[i] + Math.abs(confidenceMatrix[counter][j] - codeWords[i][j]);
				}
				if (confidenceVector[i] < bestConfidence) {
					bestConfidence = confidenceVector[i];
					bestIndex = i;
				}
			}
			example.setPredictedLabel(getLabel().getMapping().mapString(classIndexMap.get(bestIndex)));
			int numberOfFunctions = codeMatrix[0].length;

			for (int i = 0; i < numberOfClasses; i++) {
				example.setConfidence(classIndexMap.get(i), (numberOfFunctions - confidenceVector[i]) / numberOfFunctions);
			}
			counter++;
			if (progress != null && counter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted(counter);
			}
		}
	}

	/**
	 * Chooses the right evaluation procedure depending on classificationType.
	 */
	@Override
	public ExampleSet performPrediction(ExampleSet originalExampleSet, Attribute predictedLabel) throws OperatorException {

		switch (classificationType) {

			case ONE_AGAINST_ALL: {
				startNonECOCProcess(originalExampleSet, ONE_AGAINST_ALL);
				break;
			}

			case ONE_AGAINST_ONE: {
				startNonECOCProcess(originalExampleSet, ONE_AGAINST_ONE);
				break;
			}

			case EXHAUSTIVE_CODE: {
				startECOCProcess(originalExampleSet, codeMatrix);
				break;
			}

			case RANDOM_CODE: {
				startECOCProcess(originalExampleSet, codeMatrix);
				break;
			}

			default: {
				throw new OperatorException("Unknown classification strategy selected");
			}
		}

		return originalExampleSet;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString() + Tools.getLineSeparators(2));
		for (int i = 0; i < models.length; i++) {
			result.append((i > 0 ? Tools.getLineSeparator() : "") + models[i].toString());
		}
		return result.toString();
	}

	@Override
	public List<String> getModelNames() {
		List<String> names = new LinkedList<String>();
		// determine the model labels (special names for 1vs1 and 1vsall,
		// increasing numbers for
		// ecoc models)
		if (classificationType >= 2) {
			for (int index = 1; index <= models.length; index++) {
				names.add("Model " + index);
			}
		} else {
			for (int index = 0; index < modelNames.size(); index++) {
				names.add(modelNames.get(index));
			}
		}
		return names;
	}

	@Override
	public List<Model> getModels() {
		return Arrays.asList(models);
	}
}
