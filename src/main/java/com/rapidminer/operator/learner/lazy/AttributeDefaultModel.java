/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.learner.PredictionModel;


/**
 * This variant of the DefaultModel sets the prediction according to another attribute given during
 * learn time.
 *
 * @author Sebastian Land
 *
 */
public class AttributeDefaultModel extends PredictionModel {

	private static final long serialVersionUID = 3987661566241516287L;

	private String sourceAttributeName;

	protected AttributeDefaultModel(ExampleSet trainingExampleSet, String sourceAttribute) {
		super(trainingExampleSet, null, null);
		this.sourceAttributeName = sourceAttribute;
	}

	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		Attribute label = getLabel();
		Attribute exampleSetLabel = exampleSet.getAttributes().getLabel();
		Attribute sourceAttribute = exampleSet.getAttributes().get(sourceAttributeName);
		if (sourceAttribute != null) {
			if (label.isNominal() && !exampleSetLabel.isNominal()) {
				throw new UserError(null, 120, exampleSetLabel.getName(), "numerical", "nominal");
			}
			if (!label.isNominal() && exampleSetLabel.isNominal()) {
				throw new UserError(null, 120, exampleSetLabel.getName(), "nominal", "numerical");
			}
			if (label.isNominal() && !sourceAttribute.isNominal()) {
				throw new UserError(null, 120, sourceAttributeName, "numerical", "nominal");
			}
			if (!label.isNominal() && sourceAttribute.isNominal()) {
				throw new UserError(null, 120, sourceAttributeName, "nominal", "numerical");
			}

			for (Example example : exampleSet) {
				if (label.isNominal()) {
					if (!exampleSetLabel.getMapping().equals(label.getMapping())) {
						throw new UserError(null, 969);
					}
					if (!sourceAttribute.getMapping().equals(label.getMapping())) {
						throw new UserError(null, 969);
					}
					String classValue = example.getValueAsString(sourceAttribute);
					example.setValue(predictedLabel, classValue);
					example.setConfidence(classValue, 1);
				} else {
					double classValue = example.getValue(sourceAttribute);
					example.setValue(predictedLabel, classValue);
				}

			}
		} else {
			throw new AttributeNotFoundError(null, null, sourceAttributeName);
		}
		return exampleSet;
	}

}
