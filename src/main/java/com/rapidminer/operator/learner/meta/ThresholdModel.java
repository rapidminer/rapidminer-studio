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

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Tools;


/**
 * This model is created by the {@link CostBasedThresholdLearner}.
 *
 * @author Ingo Mierswa
 */
public class ThresholdModel extends PredictionModel implements DelegationModel {

	private static final long serialVersionUID = -4224958349396815500L;

	private double[] thresholds;

	private Model innerModel;

	public ThresholdModel(ExampleSet exampleSet, Model innerModel, double[] thresholds) {
		super(exampleSet, null, null);
		this.innerModel = innerModel;
		this.thresholds = thresholds;
	}

	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		exampleSet = innerModel.apply(exampleSet);

		for (Example example : exampleSet) {
			int predictionIndex = (int) example.getPredictedLabel();
			String className = getLabel().getMapping().mapIndex(predictionIndex);
			double confidence = example.getConfidence(className);
			if (confidence < thresholds[predictionIndex]) {
				example.setPredictedLabel(Double.NaN);
			}
		}

		return exampleSet;
	}

	@Override
	public String toString() {
		List<String> thresholdList = new LinkedList<String>();
		for (double d : thresholds) {
			thresholdList.add(Tools.formatIntegerIfPossible(d));
		}
		return "Thresholds: " + thresholdList + Tools.getLineSeparator() + innerModel.toString();
	}

	@Override
	public Model getBaseModel() {
		return innerModel;
	}

	@Override
	public String getShortInfo() {
		List<String> thresholdList = new LinkedList<String>();
		for (double d : thresholds) {
			thresholdList.add(Tools.formatIntegerIfPossible(d));
		}
		return "Thresholds: " + thresholdList;
	}
}
