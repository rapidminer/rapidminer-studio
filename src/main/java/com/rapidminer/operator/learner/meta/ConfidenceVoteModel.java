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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.SimplePredictionModel;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.Tools;


/**
 * {@link MetaModel} that bases its decision on the arithmetic mean of the confidence values of the
 * given {@link SimplePredictionModel}s. This value is at the same time used as confidence value for
 * the prediction.
 * <p>
 * This meta model only works with {@link SimplePredictionModel}s that calculate meaningful
 * confidence values and predict a nominal label.
 *
 * @author Zoltan Prekopcsak, Michael Knopf
 * @since 7.0.0
 */
public class ConfidenceVoteModel extends SimplePredictionModel implements MetaModel {

	private static final long serialVersionUID = 1L;

	/** List of voting models. */
	private List<? extends SimplePredictionModel> models;

	/**
	 * Creates a new {@link MetaModel} with confidence based voting for the given example set and
	 * models.
	 *
	 * @param exampleSet
	 *            the example set
	 * @param models
	 *            the voting models
	 * @throws IllegalArgumentException
	 *             if the given example set's label is not nominal
	 */
	public ConfidenceVoteModel(ExampleSet exampleSet, List<? extends SimplePredictionModel> models) {
		super(exampleSet, ExampleSetUtilities.SetsCompareOption.EQUAL,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		if (!getLabel().isNominal()) {
			throw new IllegalArgumentException("Label must be nominal.");
		}
		this.models = models;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		int i = 1;
		for (SimplePredictionModel model : models) {
			buffer.append(i);
			buffer.append(") ");
			buffer.append(model.getName());
			buffer.append(Tools.getLineSeparator());
			buffer.append("---");
			buffer.append(Tools.getLineSeparator());
			buffer.append(model.toString());
			buffer.append(Tools.getLineSeparators(2));
			i++;
		}
		return buffer.toString();
	}

	@Override
	public List<? extends Model> getModels() {
		return Collections.unmodifiableList(models);
	}

	@Override
	public List<String> getModelNames() {
		List<String> names = new ArrayList<>(models.size());
		for (SimplePredictionModel model : models) {
			names.add(model.getName());
		}
		return names;
	}

	@Override
	public double predict(Example example) throws OperatorException {
		Map<String, Double> classConfidenceSums = new HashMap<>();
		for (SimplePredictionModel model : models) {
			model.predict(example);
			for (String className : getLabel().getMapping().getValues()) {
				Double classConfidence = example.getConfidence(className);
				if (Double.isNaN(classConfidence)) {
					throw new OperatorException("Child model failed to compute confidence value.");
				}
				Double currentSum = classConfidenceSums.get(className);
				if (currentSum == null) {
					classConfidenceSums.put(className, classConfidence);
				} else {
					classConfidenceSums.put(className, currentSum + classConfidence);
				}
			}
		}

		// normalize confidence sums
		for (Entry<String, Double> entry : classConfidenceSums.entrySet()) {
			entry.setValue(entry.getValue() / models.size());
		}

		List<String> bestClasses = new ArrayList<>(classConfidenceSums.size());
		double maxConfidence = -1;
		for (Entry<String, Double> entry : classConfidenceSums.entrySet()) {
			String className = entry.getKey();
			double confidence = entry.getValue();
			if (confidence > maxConfidence) {
				maxConfidence = confidence;
				bestClasses.clear();
			}
			if (confidence == maxConfidence) {
				bestClasses.add(className);
			}
			example.setConfidence(className, confidence);
		}

		int bestClassIndex = 0;
		if (bestClasses.size() != 1) {
			bestClassIndex = RandomGenerator.getGlobalRandomGenerator().nextInt(bestClasses.size());
		}
		return getLabel().getMapping().getIndex(bestClasses.get(bestClassIndex));
	}

}
