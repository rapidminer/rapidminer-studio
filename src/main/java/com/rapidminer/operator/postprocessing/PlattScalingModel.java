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
package com.rapidminer.operator.postprocessing;

import java.util.Iterator;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Tools;


/**
 * A model that contains a boolean classifier and a scaling operation that turns confidence scores
 * into probability estimates. It is the result of a <code>PlattScaling</code> operator.
 *
 * @author Martin Scholz
 */
public class PlattScalingModel extends PredictionModel {

	private static final long serialVersionUID = 6281707312532843604L;

	private PlattParameters parameters;

	private Model model;

	public PlattScalingModel(ExampleSet exampleSet, Model model, PlattParameters parameters) {
		super(exampleSet, null, null);
		this.model = model;
		this.parameters = parameters;
	}

	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		Attribute label = this.getLabel();
		final int posLabel = label.getMapping().getPositiveIndex();
		final int negLabel = label.getMapping().getNegativeIndex();
		final String posLabelS = label.getMapping().mapIndex(posLabel);
		final String negLabelS = label.getMapping().mapIndex(negLabel);
		exampleSet = model.apply(exampleSet);
		Iterator<Example> reader = exampleSet.iterator();
		while (reader.hasNext()) {
			Example example = reader.next();
			double predicted = PlattScaling.getLogOddsPosConfidence(example.getConfidence(posLabelS));
			double scaledPos = 1.0d / (1.0d + Math.exp(predicted * parameters.getA() + parameters.getB()));
			double scaledNeg = 1.0d - scaledPos;

			example.setValue(predictedLabel, scaledPos >= scaledNeg ? posLabel : negLabel);
			example.setConfidence(posLabelS, scaledPos);
			example.setConfidence(negLabelS, scaledNeg);
		}
		return exampleSet;
	}

	/** @return a <code>String</code> representation of this scaling model. */
	@Override
	public String toString() {
		String result = super.toString() + " (" + this.parameters.toString() + ") " + Tools.getLineSeparator() + "Model: "
				+ model.toResultString();
		return result;
	}
}
