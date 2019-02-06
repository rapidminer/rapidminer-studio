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
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.tools.math.kernels.Kernel;


/**
 * The model determined by the {@link KernelLogisticRegression} operator.
 *
 * @author Ingo Mierswa
 */
public class KernelLogisticRegressionModel extends KernelModel {

	private static final long serialVersionUID = 2848059541066828127L;

	private static final int OPERATOR_PROGRESS_STEPS = 2000;

	/** The used kernel function. */
	private Kernel kernel;

	/** The list of all support vectors. */
	private List<SupportVector> supportVectors;

	/** The bias. */
	private double bias;

	/** Creates a classification model. */
	public KernelLogisticRegressionModel(ExampleSet exampleSet, List<SupportVector> supportVectors, Kernel kernel,
			double bias) {
		super(exampleSet, ExampleSetUtilities.SetsCompareOption.ALLOW_SUPERSET,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		this.supportVectors = supportVectors;
		if (supportVectors == null || supportVectors.size() == 0) {
			throw new IllegalArgumentException("Null or empty support vector collection: not possible to predict values!");
		}
		this.kernel = kernel;
		this.bias = bias;
	}

	@Override
	public String getName() {
		return "KLR Model";
	}

	@Override
	public boolean isClassificationModel() {
		return getLabel().isNominal();
	}

	@Override
	public double getAlpha(int index) {
		return supportVectors.get(index).getAlpha();
	}

	@Override
	public String getId(int index) {
		return null;
	}

	@Override
	public double getBias() {
		return this.bias;
	}

	@Override
	public SupportVector getSupportVector(int index) {
		return supportVectors.get(index);
	}

	@Override
	public int getNumberOfSupportVectors() {
		return supportVectors.size();
	}

	@Override
	public int getNumberOfAttributes() {
		return supportVectors.get(0).getX().length;
	}

	@Override
	public double getAttributeValue(int exampleIndex, int attributeIndex) {
		return this.supportVectors.get(exampleIndex).getX()[attributeIndex];
	}

	@Override
	public String getClassificationLabel(int index) {
		double y = getRegressionLabel(index);
		if (y < 0) {
			return getLabel().getMapping().getNegativeString();
		} else {
			return getLabel().getMapping().getPositiveString();
		}
	}

	@Override
	public double getRegressionLabel(int index) {
		return this.supportVectors.get(index).getY();
	}

	@Override
	public double getFunctionValue(int index) {
		double[] values = this.supportVectors.get(index).getX();
		return bias + kernel.getSum(supportVectors, values);
	}

	/**
	 * Applies the model to each example of the example set.
	 *
	 * @throws ProcessStoppedException
	 */
	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predLabel) throws ProcessStoppedException {
		Iterator<Example> reader = exampleSet.iterator();
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(exampleSet.size());
		}
		int progressCounter = 0;
		Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		while (reader.hasNext()) {
			Example current = reader.next();
			double[] currentX = new double[regularAttributes.length];
			int x = 0;
			for (Attribute attribute : regularAttributes) {
				currentX[x++] = current.getValue(attribute);
			}
			double sum = bias + kernel.getSum(supportVectors, currentX);
			if (getLabel().isNominal()) {
				double probPos = 1.0d / (1.0d + Math.exp(-sum));
				int index = probPos > 0.5d ? getLabel().getMapping().getPositiveIndex()
						: getLabel().getMapping().getNegativeIndex();
				current.setValue(predLabel, index);
				current.setConfidence(predLabel.getMapping().getPositiveString(), probPos);
				current.setConfidence(predLabel.getMapping().getNegativeString(), 1.0d - probPos);
				/*
				 * current.setConfidence(predLabel.getMapping().getPositiveString(), 1.0d / (1.0d +
				 * java.lang.Math.exp(-sum)));
				 * current.setConfidence(predLabel.getMapping().getNegativeString(), 1.0d / (1.0d +
				 * java.lang.Math.exp(sum)));
				 */
			} else {
				current.setValue(predLabel, sum);
			}
			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted(progressCounter);
			}
		}
		return exampleSet;
	}
}
