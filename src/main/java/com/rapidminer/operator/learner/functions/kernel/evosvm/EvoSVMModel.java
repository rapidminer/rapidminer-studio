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
package com.rapidminer.operator.learner.functions.kernel.evosvm;

import java.util.Iterator;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.learner.FormulaProvider;
import com.rapidminer.operator.learner.functions.kernel.KernelModel;
import com.rapidminer.operator.learner.functions.kernel.SupportVector;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.kernels.DotKernel;
import com.rapidminer.tools.math.kernels.Kernel;


/**
 * The model for the evolutionary SVM. Basically the same as other SVM models.
 *
 * @author Ingo Mierswa
 */
public class EvoSVMModel extends KernelModel implements FormulaProvider {

	private static final long serialVersionUID = 2848059541066828127L;

	private static final int OPERATOR_PROGRESS_STEPS = 5000;

	/** The used kernel function. */
	private Kernel kernel;

	/** The list of all support vectors. */
	private List<SupportVector> supportVectors;

	/** The bias. */
	private double bias;

	private double[] weights = null;

	/** Creates a classification model. */
	public EvoSVMModel(ExampleSet exampleSet, List<SupportVector> supportVectors, Kernel kernel, double bias) {
		super(exampleSet, ExampleSetUtilities.SetsCompareOption.ALLOW_SUPERSET,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		this.supportVectors = supportVectors;
		if (supportVectors == null || supportVectors.size() == 0) {
			throw new IllegalArgumentException("Null or empty support vector collection: not possible to predict values!");
		}
		this.kernel = kernel;
		this.bias = bias;

		if (this.kernel instanceof DotKernel) {
			this.weights = new double[getNumberOfAttributes()];
			for (int i = 0; i < getNumberOfSupportVectors(); i++) {
				SupportVector sv = getSupportVector(i);
				if (sv != null) {
					double[] x = sv.getX();
					double alpha = sv.getAlpha();
					double y = sv.getY();
					for (int j = 0; j < weights.length; j++) {
						weights[j] += y * alpha * x[j];
					}
				} else {
					this.weights = null;
					break;
				}
			}
		}
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
		if (exampleSet.getAttributes().size() != getNumberOfAttributes()) {
			throw new RuntimeException("Cannot apply model: incompatible numbers of attributes ("
					+ exampleSet.getAttributes().size() + " != " + getNumberOfAttributes() + ")!");
		}

		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(exampleSet.size());
		}
		int progressCounter = 0;
		Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		if (kernel instanceof DotKernel) {
			if (weights != null) {
				for (Example example : exampleSet) {
					double sum = getBias();
					int a = 0;
					for (Attribute attribute : regularAttributes) {
						sum += weights[a] * example.getValue(attribute);
						a++;
					}

					if (getLabel().isNominal()) {
						int index = sum > 0 ? getLabel().getMapping().getPositiveIndex()
								: getLabel().getMapping().getNegativeIndex();
						example.setValue(predLabel, index);
						example.setConfidence(predLabel.getMapping().getPositiveString(),
								1.0d / (1.0d + java.lang.Math.exp(-sum)));
						example.setConfidence(predLabel.getMapping().getNegativeString(),
								1.0d / (1.0d + java.lang.Math.exp(sum)));
					} else {
						example.setValue(predLabel, sum);
					}
					if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
						progress.setCompleted(progressCounter);
					}
				}
				return exampleSet;
			}
		}

		Iterator<Example> reader = exampleSet.iterator();
		while (reader.hasNext()) {
			Example current = reader.next();
			double[] currentX = new double[exampleSet.getAttributes().size()];
			int x = 0;
			for (Attribute attribute : regularAttributes) {
				currentX[x++] = current.getValue(attribute);
			}
			double sum = bias + kernel.getSum(supportVectors, currentX);
			if (getLabel().isNominal()) {
				int index = sum > 0 ? getLabel().getMapping().getPositiveIndex()
						: getLabel().getMapping().getNegativeIndex();
				current.setValue(predLabel, index);
				current.setConfidence(predLabel.getMapping().getPositiveString(), 1.0d / (1.0d + java.lang.Math.exp(-sum)));
				current.setConfidence(predLabel.getMapping().getNegativeString(), 1.0d / (1.0d + java.lang.Math.exp(sum)));
			} else {
				current.setValue(predLabel, sum);
			}
			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted(progressCounter);
			}
		}

		return exampleSet;
	}

	@Override
	public String getFormula() {
		StringBuffer result = new StringBuffer();

		boolean first = true;
		for (int i = 0; i < getNumberOfSupportVectors(); i++) {
			SupportVector sv = getSupportVector(i);
			if (sv != null) {
				double alpha = sv.getAlpha();
				if (!Tools.isZero(alpha)) {
					result.append(Tools.getLineSeparator());
					double[] x = sv.getX();
					double y = sv.getY();
					double factor = y * alpha;
					if (factor < 0.0d) {
						if (first) {
							result.append("- " + Math.abs(factor));
						} else {
							result.append("- " + Math.abs(factor));
						}
					} else {
						if (first) {
							result.append("  " + factor);
						} else {
							result.append("+ " + factor);
						}
					}

					result.append(" * (" + kernel.getDistanceFormula(x, getAttributeConstructions()) + ")");
					first = false;
				}
			}
		}

		double bias = getBias();
		if (!Tools.isZero(bias)) {
			result.append(Tools.getLineSeparator());
			if (bias < 0.0d) {
				if (first) {
					result.append("- " + Math.abs(bias));
				} else {
					result.append("- " + Math.abs(bias));
				}
			} else {
				if (first) {
					result.append(bias);
				} else {
					result.append("+ " + bias);
				}
			}
		}

		return result.toString();
	}
}
