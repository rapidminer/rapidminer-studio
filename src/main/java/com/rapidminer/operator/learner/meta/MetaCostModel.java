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
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;


/**
 * This class is associated to the MetaCost operator and supports the evaluation procedures of the
 * MetaCost method.
 *
 * @author Helge Homburg
 */
public class MetaCostModel extends PredictionModel implements MetaModel {

	private static final long serialVersionUID = -7378871544357578954L;

	private static final int OPERATOR_PROGRESS_STEPS = 5000;

	private Model[] models;

	private double[][] costMatrix;

	public MetaCostModel(ExampleSet exampleSet, Model[] models, double[][] costMatrix) {
		super(exampleSet, null, null);
		this.models = models;
		this.costMatrix = costMatrix;
	}

	public int getNumberOfModels() {
		return models.length;
	}

	/** Returns a binary decision model for the given classification index. */
	public Model getModel(int index) {
		return models[index];
	}

	/** Returns a single value from the cost matrix. */
	public double getCostValue(int i, int j) {
		return costMatrix[i][j];
	}

	@Override
	public ExampleSet performPrediction(ExampleSet originalExampleSet, Attribute predictedLabel) throws OperatorException {
		ExampleSet exampleSet = (ExampleSet) originalExampleSet.clone();
		int numberOfClasses = getLabel().getMapping().getValues().size();

		double[][] confidences = new double[exampleSet.size()][numberOfClasses];

		// Hash maps are used for addressing particular class values using indices without relying
		// upon a consistent index distribution of the corresponding substructure.
		int currentNumber = 0;
		HashMap<Integer, String> classIndexMap = new HashMap<Integer, String>(numberOfClasses);
		for (String currentClass : getLabel().getMapping().getValues()) {
			classIndexMap.put(currentNumber, currentClass);
			currentNumber++;
		}

		// initialize progress
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(100);
		}

		// 1. Iterate over all models and all examples for every model to receive all confidence
		// values.
		for (int k = 0; k < getNumberOfModels(); k++) {
			Model model = getModel(k);
			exampleSet = model.apply(exampleSet);

			Iterator<Example> reader = exampleSet.iterator();
			int counter = 0;

			while (reader.hasNext()) {
				Example example = reader.next();
				int currentClassNumber = 0;
				for (String currentClass : getLabel().getMapping().getValues()) {
					final double confidence = example.getConfidence(currentClass);
					confidences[counter][currentClassNumber] += confidence;
					currentClassNumber++;
				}
				counter++;
			}
			PredictionModel.removePredictedLabel(exampleSet);

			if (progress != null) {
				progress.setCompleted((int) (80.0 * k / getNumberOfModels()));
			}
		}

		// 2. Iterate again over all examples to compute a prediction and a confidence distribution
		// for
		// all examples depending on the results of step 1 and the cost matrix.
		Attribute classificationCost = AttributeFactory.createAttribute(Attributes.CLASSIFICATION_COST, Ontology.REAL);
		originalExampleSet.getExampleTable().addAttribute(classificationCost);
		originalExampleSet.getAttributes().setCost(classificationCost);
		int counter = 0;
		for (Example example : originalExampleSet) {
			for (int i = 0; i < numberOfClasses; i++) {
				confidences[counter][i] = confidences[counter][i] / getNumberOfModels();
			}

			double[] expectedCosts = new double[numberOfClasses];
			int bestIndex = 0; // if confidences evaluate to NaN: use first
			double bestValue = Double.POSITIVE_INFINITY;

			for (int i = 0; i < numberOfClasses; i++) {
				for (int j = 0; j < numberOfClasses; j++) {
					expectedCosts[i] += confidences[counter][j] * costMatrix[i][j];
				}
				if (expectedCosts[i] < bestValue) {
					bestValue = expectedCosts[i];
					bestIndex = i;
				}
			}

			// setting prediction, expectedCost and confidences
			example.setValue(predictedLabel, getLabel().getMapping().mapString(classIndexMap.get(bestIndex)));
			example.setValue(classificationCost, expectedCosts[bestIndex]);

			for (int i = 0; i < numberOfClasses; i++) {
				example.setConfidence(classIndexMap.get(i), confidences[counter][i]);
			}
			counter++;

			if (progress != null && counter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted((int) (20.0 * counter / originalExampleSet.size()) + 80);
			}
		}
		return originalExampleSet;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString() + Tools.getLineSeparator());
		for (int i = 0; i < models.length; i++) {
			result.append((i > 0 ? Tools.getLineSeparator() : "") + models[i].toString());
		}
		return result.toString();
	}

	@Override
	public List<String> getModelNames() {
		List<String> names = new LinkedList<String>();
		for (int i = 0; i < this.getNumberOfModels(); i++) {
			names.add("Model " + (i + 1));
		}
		return names;
	}

	@Override
	public List<Model> getModels() {
		return Arrays.asList(models);
	}

}
