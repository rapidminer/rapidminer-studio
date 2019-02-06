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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.learner.PredictionModel;


/**
 * The model for the relative regression meta learner.
 *
 * @author Ingo Mierswa
 */
public class RelativeRegressionModel extends PredictionModel implements DelegationModel {

	private static final long serialVersionUID = -8474869399613666453L;

	private String relativeAttributeName;

	private Model baseModel;

	protected RelativeRegressionModel(ExampleSet trainingExampleSet, Model baseModel, String relativeAttributeName) {
		super(trainingExampleSet, null, null);
		this.baseModel = baseModel;
		this.relativeAttributeName = relativeAttributeName;
	}

	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		ExampleSet resultSet = baseModel.apply(exampleSet);
		Attribute relativeAttribute = resultSet.getAttributes().get(relativeAttributeName);
		Attribute predLabel = resultSet.getAttributes().getPredictedLabel();

		// checks
		if (relativeAttribute == null) {
			throw new AttributeNotFoundError(null, null, relativeAttributeName);
		}

		if (predLabel == null) {
			throw new UserError(null, 107);
		}

		// change predictions
		for (Example e : resultSet) {
			double relativeValue = e.getValue(relativeAttribute);
			double predictedValue = e.getValue(predLabel);
			e.setValue(predLabel, relativeValue + predictedValue);
		}

		return resultSet;
	}

	@Override
	public String getName() {
		return "Relative Model for " + baseModel.getName();
	}

	@Override
	public Model getBaseModel() {
		return baseModel;
	}

	@Override
	public String getShortInfo() {
		return "Regression relative to attribute \"" + relativeAttributeName + "\"";
	}
}
