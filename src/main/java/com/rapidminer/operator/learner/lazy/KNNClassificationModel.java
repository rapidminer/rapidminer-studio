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
package com.rapidminer.operator.learner.lazy;

import java.util.ArrayList;
import java.util.Collection;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.example.set.ExampleSetUtilities.SetsCompareOption;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.learner.UpdateablePredictionModel;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Tupel;
import com.rapidminer.tools.math.container.GeometricDataCollection;


/**
 * An implementation of a knn model.
 *
 * @author Sebastian Land
 *
 */
public class KNNClassificationModel extends UpdateablePredictionModel {

	private static final long serialVersionUID = -6292869962412072573L;

	private static final int OPERATOR_PROGRESS_STEPS = 1000;

	private int k;

	private int size;

	private GeometricDataCollection<Integer> samples;

	private ArrayList<String> sampleAttributeNames;

	private boolean weightByDistance;

	public KNNClassificationModel(ExampleSet trainingSet, GeometricDataCollection<Integer> samples, int k,
			boolean weightByDistance) {
		super(trainingSet, SetsCompareOption.ALLOW_SUPERSET, ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		this.k = k;
		this.size = trainingSet.size();
		this.samples = samples;
		this.weightByDistance = weightByDistance;

		// finding training attributes
		Attributes attributes = trainingSet.getAttributes();
		sampleAttributeNames = new ArrayList<String>(attributes.size());
		for (Attribute attribute : attributes) {
			sampleAttributeNames.add(attribute.getName());
		}
	}

	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		// building attribute order from trainingset
		ArrayList<Attribute> sampleAttributes = new ArrayList<Attribute>(sampleAttributeNames.size());
		Attributes attributes = exampleSet.getAttributes();
		for (String attributeName : sampleAttributeNames) {
			sampleAttributes.add(attributes.get(attributeName));
		}

		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(exampleSet.size());
		}
		int progressCounter = 0;

		double[] values = new double[sampleAttributes.size()];
		for (Example example : exampleSet) {
			// reading values
			int i = 0;
			for (Attribute attribute : sampleAttributes) {
				values[i] = example.getValue(attribute);
				i++;
			}

			// counting frequency of labels
			double[] counter = new double[predictedLabel.getMapping().size()];
			double totalDistance = 0;
			if (!weightByDistance || k == 1) {
				// finding next k neighbours
				Collection<Integer> neighbourLabels = samples.getNearestValues(k, values);
				// distance is 1 for complete neighbourhood
				totalDistance = k;

				// counting frequency of labels
				for (int index : neighbourLabels) {
					counter[index] += 1 / totalDistance;
				}
			} else {
				// finding next k neighbours and their distances
				Collection<Tupel<Double, Integer>> neighbours = samples.getNearestValueDistances(k, values);
				for (Tupel<Double, Integer> tupel : neighbours) {
					totalDistance += tupel.getFirst();
				}

				double totalSimilarity = 0.0d;
				if (totalDistance == 0) {
					totalDistance = 1;
					totalSimilarity = k;
				} else {
					totalSimilarity = Math.max(k - 1, 1);
				}

				// counting frequency of labels
				for (Tupel<Double, Integer> tupel : neighbours) {
					counter[tupel.getSecond()] += (1d - tupel.getFirst() / totalDistance) / totalSimilarity;
				}
			}
			// finding most frequent class
			int mostFrequentIndex = Integer.MIN_VALUE;
			double mostFrequentFrequency = Double.NEGATIVE_INFINITY;
			for (int index = 0; index < counter.length; index++) {
				if (mostFrequentFrequency < counter[index]) {
					mostFrequentFrequency = counter[index];
					mostFrequentIndex = index;
				}
			}
			// setting prediction
			if (mostFrequentIndex == Integer.MIN_VALUE) {
				example.setValue(predictedLabel, Double.NaN);
			} else {
				example.setValue(predictedLabel, mostFrequentIndex);
			}

			// setting confidence
			for (int index = 0; index < counter.length; index++) {
				example.setConfidence(predictedLabel.getMapping().mapIndex(index), counter[index]);
			}

			// trigger progress
			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted(progressCounter);
			}
		}
		return exampleSet;
	}

	@Override
	public void update(ExampleSet updateSet) throws OperatorException {
		Attribute label = updateSet.getAttributes().getLabel();
		// check if exampleset header is correct
		if (label.isNominal()) {
			Attributes attributes = updateSet.getAttributes();

			int valuesSize = attributes.size();
			for (Example example : updateSet) {
				double[] values = new double[valuesSize];
				int i = 0;
				for (Attribute attribute : attributes) {
					values[i] = example.getValue(attribute);
					i++;
				}
				int labelValue = (int) example.getValue(label);
				samples.add(values, labelValue);
			}
		}
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		if (weightByDistance) {
			buffer.append("Weighted ");
		}
		buffer.append(k + "-Nearest Neighbour model for classification." + Tools.getLineSeparator());
		buffer.append("The model contains " + size + " examples with " + sampleAttributeNames.size()
				+ " dimensions of the following classes:");
		buffer.append(Tools.getLineSeparator());
		for (String value : getTrainingHeader().getAttributes().getLabel().getMapping().getValues()) {
			buffer.append("  " + value + Tools.getLineSeparator());
		}
		return buffer.toString();
	}
}
