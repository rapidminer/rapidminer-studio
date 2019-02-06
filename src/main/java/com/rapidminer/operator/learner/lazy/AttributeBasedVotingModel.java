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

import java.util.HashMap;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.HeaderExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.meta.Vote;
import com.rapidminer.tools.Tools;


/**
 * Average model simply calculates the average of the attributes as prediction. For classification
 * problems the mode of all attribute values is returned. This model is mainly used in meta learning
 * schemes (like {@link Vote}. Please keep in mind that each attribute available on creation time is
 * used for calculating the outcome.
 *
 * @author Ingo Mierswa
 */
public class AttributeBasedVotingModel extends PredictionModel {

	private static final long serialVersionUID = -8814468417883548971L;

	private double majorityVote;

	public AttributeBasedVotingModel(ExampleSet exampleSet, double majorityVote) {
		super(exampleSet, null, null);
		this.majorityVote = majorityVote;
	}

	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabelAttribute) throws OperatorException {
		Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		for (Example example : exampleSet) {
			if (predictedLabelAttribute.isNominal()) {
				// classification
				Map<String, Double> counter = new HashMap<String, Double>();
				for (Attribute attribute : regularAttributes) {
					if (!attribute.isNominal()) {
						throw new UserError(null, 103, "nominal voting");
					}

					String labelValue = attribute.getMapping().mapIndex((int) example.getValue(attribute));
					double labelSum = 0.0d;
					if (counter.get(labelValue) != null) {
						labelSum = counter.get(labelValue);
					}
					labelSum += 1.0d;
					counter.put(labelValue, labelSum);
				}

				// calculate confidences and best class
				String bestClass = null;
				double best = Double.NEGATIVE_INFINITY;
				for (String labelValue : getLabel().getMapping().getValues()) {
					Double sumObject = counter.get(labelValue);
					if (sumObject == null) {
						example.setConfidence(labelValue, 0.0d);
					} else {
						example.setConfidence(labelValue, sumObject / regularAttributes.length);
						if (sumObject > best) {
							best = sumObject;
							bestClass = labelValue;
						}
					}
				}

				// set crisp prediction
				if (bestClass != null) {
					example.setPredictedLabel(predictedLabelAttribute.getMapping().mapString(bestClass));
				} else {
					example.setPredictedLabel(majorityVote);
				}
			} else {
				// regression
				double average = 0.0d;
				for (Attribute attribute : regularAttributes) {
					average += example.getValue(attribute);
				}
				average /= example.getAttributes().size();
				example.setValue(predictedLabelAttribute, average);
			}
		}

		return exampleSet;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		HeaderExampleSet header = getTrainingHeader();
		Attribute label = header.getAttributes().getLabel();

		if (label.isNominal()) {
			buffer.append("Using the majority of the following attributes for prediction:" + Tools.getLineSeparator());
		} else {
			buffer.append("Using the avarage of the following attributes for prediction:" + Tools.getLineSeparator());
		}

		for (Attribute attribute : header.getAttributes()) {
			if (attribute.isNominal() && label.isNominal() || attribute.isNumerical() && label.isNumerical()) {
				buffer.append("  " + attribute.getName() + Tools.getLineSeparator());
			}
		}

		buffer.append(Tools.getLineSeparator());
		if (label.isNominal()) {
			buffer.append("The default value is " + label.getMapping().mapIndex((int) majorityVote));
		}
		return buffer.toString();
	}
}
