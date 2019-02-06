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
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.learner.FormulaProvider;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExample;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples.MeanVariance;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel.Kernel;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel.KernelDot;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.svm.SVMInterface;
import com.rapidminer.tools.Tools;


/**
 * The abstract superclass for the SVM models by Stefan Rueping.
 *
 * @author Ingo Mierswa
 */
public abstract class AbstractMySVMModel extends KernelModel implements FormulaProvider {

	private static final long serialVersionUID = 2812901947459843681L;

	private static final int OPERATOR_PROGRESS_STEPS = 5000;

	private com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples model;

	private Kernel kernel;

	private double[] weights = null;

	public AbstractMySVMModel(ExampleSet exampleSet,
			com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples model, Kernel kernel,
			int kernelType) {
		super(exampleSet, ExampleSetUtilities.SetsCompareOption.ALLOW_SUPERSET,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		this.model = model;
		this.kernel = kernel;

		if (this.kernel instanceof KernelDot) {
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

	/** Creates a new SVM for prediction. */
	public abstract SVMInterface createSVM();

	@Override
	public boolean isClassificationModel() {
		return getLabel().isNominal();
	}

	@Override
	public double getBias() {
		return model.get_b();
	}

	/**
	 * This method must divide the alpha by the label since internally the alpha value is already
	 * multiplied with y.
	 */
	@Override
	public SupportVector getSupportVector(int index) {
		double alpha = model.get_alpha(index);
		double y = model.get_y(index);
		if (y != 0.0d) {
			alpha /= y;
		}
		return new SupportVector(model.get_example(index).toDense(getNumberOfAttributes()), y, alpha);
	}

	@Override
	public double getAlpha(int index) {
		return model.get_alpha(index);
	}

	@Override
	public String getId(int index) {
		return model.getId(index);
	}

	@Override
	public int getNumberOfSupportVectors() {
		return model.count_examples();
	}

	@Override
	public int getNumberOfAttributes() {
		return model.get_dim();
	}

	@Override
	public double getAttributeValue(int exampleIndex, int attributeIndex) {
		com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExample sVMExample = model
				.get_example(exampleIndex);
		double value = 0.0d;
		try {
			value = sVMExample.toDense(getNumberOfAttributes())[attributeIndex];
		} catch (ArrayIndexOutOfBoundsException e) {
			// dense array to short --> use default value
		}
		return value;
	}

	@Override
	public String getClassificationLabel(int index) {
		double y = model.get_y(index);
		if (y < 0) {
			return getLabel().getMapping().getNegativeString();
		} else {
			return getLabel().getMapping().getPositiveString();
		}
	}

	@Override
	public double getRegressionLabel(int index) {
		return model.get_y(index);
	}

	@Override
	public double getFunctionValue(int index) {
		SVMInterface svm = createSVM();
		svm.init(kernel, model);
		// need to clone the support vector, since internally there is only one instance of an
		// SVMExample
		// in the data, where only its data pointers are exchanged. This instance is also changed in
		// svm.predict(),
		// so we need to clone.
		SVMExample sv = new SVMExample(model.get_example(index));
		return svm.predict(sv);
	}

	/** Gets the kernel. */
	public Kernel getKernel() {
		return kernel;
	}

	/** Gets the model, i.e. an SVM example set. */
	public com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples getExampleSet() {
		return model;
	}

	/**
	 * Sets the correct prediction to the example from the result value of the SVM.
	 */
	public abstract void setPrediction(Example example, double prediction);

	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabelAttribute) throws OperatorException {
		if (kernel instanceof KernelDot) {
			if (weights != null) {
				Map<Integer, MeanVariance> meanVariances = model.getMeanVariances();
				OperatorProgress progress = null;
				if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
					progress = getOperator().getProgress();
					progress.setTotal(exampleSet.size());
				}
				int progressCounter = 0;
				Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
				for (Example example : exampleSet) {
					double prediction = getBias();
					int a = 0;
					for (Attribute attribute : regularAttributes) {
						double value = example.getValue(attribute);
						MeanVariance meanVariance = meanVariances.get(a);
						if (meanVariance != null) {
							if (meanVariance.getVariance() == 0.0d) {
								value = 0.0d;
							} else {
								value = (value - meanVariance.getMean()) / Math.sqrt(meanVariance.getVariance());
							}
						}
						prediction += weights[a] * value;
						a++;
					}
					setPrediction(example, prediction);

					if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
						progress.setCompleted(progressCounter);
					}
				}
				return exampleSet;
			}
		}

		// only if not simple dot hyperplane (see above)...
		com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples toPredict = new com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples(
				exampleSet, exampleSet.getAttributes().getPredictedLabel(), model.getMeanVariances());

		SVMInterface svm = createSVM();
		svm.init(kernel, model);
		svm.predict(toPredict);

		// set predictions from toPredict
		Iterator<Example> reader = exampleSet.iterator();
		int k = 0;
		while (reader.hasNext()) {
			setPrediction(reader.next(), toPredict.get_y(k++));
		}
		return exampleSet;
	}

	@Override
	public String getFormula() {
		StringBuffer result = new StringBuffer();

		Kernel kernel = getKernel();

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

	/**
	 * Need to synchronized since {@link SVMExamples#get_example(int)} is not thread safe. (That
	 * method always returns the same instance.)
	 */
	@Override
	public synchronized String toString() {
		return super.toString();
	}
}
