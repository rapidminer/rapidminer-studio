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
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDReal;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PredictionModelMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.container.Range;

import java.util.Iterator;


/**
 * For a classified dataset (with possibly more than two classes) builds a classifier using a
 * regression method which is specified by the inner operator. For each class {@rapidminer.math i} a
 * regression model is trained after setting the label to {@rapidminer.math +1} if the label equals
 * {@rapidminer.math i} and to {@rapidminer.math -1} if it is not. Then the regression models are
 * combined into a classification model. In order to determine the prediction for an unlabeled
 * example, all models are applied and the class belonging to the regression model which predicts
 * the greatest value is chosen.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class ClassificationByRegression extends AbstractMetaLearner {

	private int numberOfClasses;

	public ClassificationByRegression(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyExampleSetMetaData(ExampleSetMetaData unmodifiedMetaData) {
		switch (unmodifiedMetaData.hasSpecial(Attributes.LABEL_NAME)) {
			case NO:
				getTrainingSetInputPort().addError(
						new SimpleMetaDataError(Severity.ERROR, getTrainingSetInputPort(), "special_missing", "label"));
				return unmodifiedMetaData;
			case UNKNOWN:
				getTrainingSetInputPort().addError(
						new SimpleMetaDataError(Severity.WARNING, getTrainingSetInputPort(), "special_unknown", "label"));
				return unmodifiedMetaData;
			case YES:
				AttributeMetaData labelMD = unmodifiedMetaData.getLabelMetaData();
				unmodifiedMetaData.removeAttribute(labelMD);
				AttributeMetaData transformedMD = new AttributeMetaData("regression(" + labelMD.getName() + ")",
						Ontology.REAL, Attributes.LABEL_NAME);
				transformedMD.setValueRange(new Range(-1d, 1d), SetRelation.EQUAL);
				transformedMD.setValueSetRelation(SetRelation.EQUAL);
				transformedMD.setMean(new MDReal());

				unmodifiedMetaData.addAttribute(transformedMD);
				return unmodifiedMetaData;
			default:
				return unmodifiedMetaData;
		}
	}

	/** Transforms the regression label back into a classification label. */
	@Override
	protected MetaData modifyGeneratedModelMetaData(PredictionModelMetaData unmodifiedMetaData) {
		InputPort in = getTrainingSetInputPort();
		MetaData esetIn = in.getMetaData();
		if ((esetIn != null) && (esetIn instanceof ExampleSetMetaData)) {
			return new PredictionModelMetaData(MultiModelByRegression.class, (ExampleSetMetaData) esetIn);
		}
		return unmodifiedMetaData;
	}

	@Override
	public Model learn(ExampleSet inputSet) throws OperatorException {
		Attribute classLabel = inputSet.getAttributes().getLabel();
		numberOfClasses = classLabel.getMapping().getValues().size();
		Model[] models = new Model[numberOfClasses];

		ExampleSet eSet = (ExampleSet) inputSet.clone();
		Attribute tempLabel = AttributeFactory.createAttribute("regression(" + classLabel.getName() + ")", Ontology.REAL);
		eSet.getExampleTable().addAttribute(tempLabel);
		eSet.getAttributes().setLabel(tempLabel);

		for (int i = 0; i < numberOfClasses; i++) {
			// 1. Set regression labels
			Iterator<Example> r = eSet.iterator();
			while (r.hasNext()) {
				Example e = r.next();
				if (e.getValue(classLabel) == i) {
					e.setValue(tempLabel, +1.0);
				} else {
					e.setValue(tempLabel, -1.0);
				}
			}
			// 2. Apply learner
			models[i] = applyInnerLearner(eSet);
			inApplyLoop();
		}

		return new MultiModelByRegression(inputSet, models);
	}

	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		switch (lc) {
			case NUMERICAL_LABEL:
			case NO_LABEL:
			case UPDATABLE:
			case FORMULA_PROVIDER:
				return false;
			default:
				return true;
		}
	}
}
