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
package com.rapidminer.operator.learner.functions.kernel;

import java.util.Iterator;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;


/**
 * A model learned by the GPLearner.
 *
 * @author Piotr Kasprzak, Ingo Mierswa
 */
public class GPModel extends KernelModel {

	private static final long serialVersionUID = 6094706651995436944L;

	private static final int OPERATOR_PROGRESS_STEPS = 5000;

	private com.rapidminer.operator.learner.functions.kernel.gaussianprocess.Model model = null;

	public GPModel(ExampleSet exampleSet, com.rapidminer.operator.learner.functions.kernel.gaussianprocess.Model model) {
		super(exampleSet, ExampleSetUtilities.SetsCompareOption.ALLOW_SUPERSET,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		this.model = model;
	}

	@Override
	public boolean isClassificationModel() {
		return getLabel().isNominal();
	}

	@Override
	public double getBias() {
		return 0;
	}

	@Override
	public SupportVector getSupportVector(int index) {
		return null;
	}

	@Override
	public double getAlpha(int index) {
		return Double.NaN;
	}

	@Override
	public String getId(int index) {
		return null;
	}

	@Override
	public int getNumberOfSupportVectors() {
		return this.model.getNumberOfBasisVectors();
	}

	@Override
	public int getNumberOfAttributes() {
		return this.model.getInputDim();
	}

	@Override
	public double getAttributeValue(int exampleIndex, int attributeIndex) {
		return this.model.getBasisVectorValue(exampleIndex, attributeIndex);
	}

	@Override
	public String getClassificationLabel(int index) {
		return "?";
	}

	@Override
	public double getRegressionLabel(int index) {
		return Double.NaN;
	}

	@Override
	public double getFunctionValue(int index) {
		return model.applyToVector(this.model.getBasisVector(index));
	}

	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		Iterator<Example> i = exampleSet.iterator();
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(exampleSet.size());
		}
		int progressCounter = 0;
		while (i.hasNext()) {
			Example e = i.next();
			double functionValue = model.applyToVector(RVMModel.makeInputVector(e));
			if (getLabel().isNominal()) {
				if (functionValue > 0) {
					e.setValue(predictedLabel, getLabel().getMapping().getPositiveIndex());
				} else {
					e.setValue(predictedLabel, getLabel().getMapping().getNegativeIndex());
				}
				// set confidence to numerical prediction, such that can be scaled later
				e.setConfidence(predictedLabel.getMapping().getPositiveString(),
						1.0d / (1.0d + java.lang.Math.exp(-functionValue)));
				e.setConfidence(predictedLabel.getMapping().getNegativeString(),
						1.0d / (1.0d + java.lang.Math.exp(functionValue)));
			} else {
				e.setValue(predictedLabel, functionValue);
			}
			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted(progressCounter);
			}
		}
		return exampleSet;
	}

}
