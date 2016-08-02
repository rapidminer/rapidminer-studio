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
package com.rapidminer.operator.learner.meta;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Tools;


/**
 * This class is the model build by the {@link Stacking} operator.
 *
 * @author Ingo Mierswa, Helge Homburg
 */
public class StackingModel extends PredictionModel implements MetaModel {

	private static final long serialVersionUID = -3978054415189320147L;

	private String modelName;

	private List<Model> baseModels;

	private Model stackingModel;

	private boolean useAllAttributes;

	public StackingModel(ExampleSet exampleSet, String modelName, List<Model> baseModels, Model stackingModel,
			boolean useAllAttributes) {
		super(exampleSet, null, null);
		this.modelName = modelName;
		this.baseModels = baseModels;
		this.stackingModel = stackingModel;
		this.useAllAttributes = useAllAttributes;
	}

	@Override
	public String getName() {
		return this.modelName;
	}

	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		// init
		PredictionModel.removePredictedLabel(exampleSet, true, true);
		ExampleSet stackingExampleSet = (ExampleSet) exampleSet.clone();
		if (!useAllAttributes) {
			stackingExampleSet.getAttributes().clearRegular();
		}

		// create predictions from base models
		List<Attribute> tempPredictions = new LinkedList<Attribute>();
		int i = 0;
		for (Model baseModel : baseModels) {
			exampleSet = baseModel.apply(exampleSet);
			Attribute basePrediction = exampleSet.getAttributes().getPredictedLabel();
			// renaming attribute
			basePrediction.setName("base_prediction" + i);

			PredictionModel.removePredictedLabel(exampleSet, false, true);
			stackingExampleSet.getAttributes().addRegular(basePrediction);
			tempPredictions.add(basePrediction);
			i++;
		}

		// apply stacking model and copy prediction to original example set
		stackingExampleSet = stackingModel.apply(stackingExampleSet);
		PredictionModel.copyPredictedLabel(stackingExampleSet, exampleSet);

		// remove temporary predictions from table
		for (Attribute tempPrediction : tempPredictions) {
			stackingExampleSet.getAttributes().remove(tempPrediction);
			stackingExampleSet.getExampleTable().removeAttribute(tempPrediction);
		}

		return exampleSet;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString() + Tools.getLineSeparators(2));
		result.append(this.modelName + ":");
		result.append(Tools.getLineSeparator() + stackingModel.toString() + Tools.getLineSeparators(2));

		result.append("Base Models:");
		for (Model baseModel : baseModels) {
			result.append(Tools.getLineSeparator() + baseModel.toString());
		}
		return result.toString();
	}

	@Override
	public List<String> getModelNames() {
		List<String> names = new LinkedList<String>();
		for (int i = 0; i < this.baseModels.size(); i++) {
			names.add("Model " + (i + 1));
		}
		names.add("Stacking Model");
		return names;
	}

	@Override
	public List<? extends Model> getModels() {
		ArrayList<Model> models = new ArrayList<Model>(baseModels);
		models.add(stackingModel);
		return models;
	}
}
