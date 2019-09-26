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
package com.rapidminer.operator.performance;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.Averagable;


/**
 * Measures the weighted mean of all per class recalls or per class precisions based on the weights
 * defined in the performance evaluator.
 *
 * @author Ingo Mierswa
 */
public class WeightedMultiClassPerformance extends MeasuredPerformance implements ClassWeightedPerformance {

	private static final long serialVersionUID = 8734250559680229116L;

	/** Indicates an undefined type (should not happen). */
	public static final int UNDEFINED = -1;

	/** Indicates accuracy. */
	public static final int WEIGHTED_RECALL = 0;

	/** Indicates classification error. */
	public static final int WEIGHTED_PRECISION = 1;

	/** The names of the criteria. */
	public static final String[] NAMES = { "weighted_mean_recall", "weighted_mean_precision" };

	/** The descriptions of the criteria. */
	public static final String[] DESCRIPTIONS = { "The weighted mean of all per class recall measurements.",
	"The weighted mean of all per class precision measurements." };

	/**
	 * The counter for true labels and the prediction.
	 */
	private double[][] counter;

	/** The class names of the label. Used for logging and result display. */
	private String[] classNames;

	/** Maps class names to indices. */
	private Map<String, Integer> classNameMap = new HashMap<String, Integer>();

	/** The type of this performance. */
	private int type = WEIGHTED_RECALL;

	/** The different class weights. */
	private double[] classWeights;

	/** The sum of all weights. */
	private double weightSum;

	/** The currently used label attribute. */
	private Attribute labelAttribute;

	/** The currently used predicted label attribute. */
	private Attribute predictedLabelAttribute;

	/** The weight attribute. Might be null. */
	private Attribute weightAttribute;

	/** Creates a WeightedMultiClassPerformance with undefined type. */
	public WeightedMultiClassPerformance() {
		this(UNDEFINED);
	}

	/** Creates a WeightedMultiClassPerformance with the given type. */
	public WeightedMultiClassPerformance(int type) {
		this.type = type;
	}

	public WeightedMultiClassPerformance(WeightedMultiClassPerformance m) {
		super(m);
		this.type = m.type;
		if (m.classNames != null) {
			this.classNames = Arrays.copyOf(m.classNames, m.classNames.length);
			this.classNameMap.putAll(m.classNameMap);
		}
		if (m.classWeights != null) {
			this.classWeights = Arrays.copyOf(m.classWeights, m.classWeights.length);
			this.weightSum = m.weightSum;
		}
		if (m.counter != null) {
			this.counter = new double[m.counter.length][];
			for (int i = 0; i < m.counter.length; i++) {
				this.counter[i] = Arrays.copyOf(m.counter[i], m.counter[i].length);
			}
		}
		if (m.labelAttribute != null) {
			this.labelAttribute = (Attribute) m.labelAttribute.clone();
		}
		if (m.predictedLabelAttribute != null) {
			this.predictedLabelAttribute = (Attribute) m.predictedLabelAttribute.clone();
		}
		if (m.weightAttribute != null) {
			this.weightAttribute = (Attribute) m.weightAttribute.clone();
		}
	}

	/** Creates a WeightedMultiClassPerformance with the given type. */
	public static WeightedMultiClassPerformance newInstance(String name) {
		for (int i = 0; i < NAMES.length; i++) {
			if (NAMES[i].equals(name)) {
				return new WeightedMultiClassPerformance(i);
			}
		}
		return null;
	}

	/** Sets the class weights. */
	@Override
	public void setWeights(double[] weights) {
		this.weightSum = 0.0d;
		this.classWeights = weights;
		for (double w : this.classWeights) {
			this.weightSum += w;
		}
	}

	/** Initializes the criterion and sets the label. */
	@Override
	public void startCounting(ExampleSet eSet, boolean useExampleWeights) throws OperatorException {
		com.rapidminer.example.Tools.hasNominalLabels(eSet, "calculation of classification performance criteria");
		super.startCounting(eSet, useExampleWeights);
		this.labelAttribute = eSet.getAttributes().getLabel();
		this.predictedLabelAttribute = eSet.getAttributes().getPredictedLabel();
		if (this.predictedLabelAttribute == null || !this.predictedLabelAttribute.isNominal()) {
			throw new UserError(null, 101, "calculation of classification performance criteria",
					predictedLabelAttribute.getName());
		}

		if (useExampleWeights) {
			this.weightAttribute = eSet.getAttributes().getWeight();
		}

		List<String> values = labelAttribute.getMapping().getValues();
		this.counter = new double[values.size()][values.size()];
		this.classNames = new String[values.size()];
		Iterator<String> i = values.iterator();
		int n = 0;
		while (i.hasNext()) {
			classNames[n] = i.next();
			classNameMap.put(classNames[n], n);
			n++;
		}
	}

	/** Increases the prediction value in the matrix. */
	@Override
	public void countExample(Example example) {
		int label = classNameMap.get(example.getNominalValue(labelAttribute));
		int plabel = classNameMap.get(example.getNominalValue(predictedLabelAttribute));
		double weight = 1.0d;
		if (weightAttribute != null) {
			weight = example.getValue(weightAttribute);
		}
		counter[label][plabel] += weight;
	}

	@Override
	public double getExampleCount() {
		double total = 0;
		for (int i = 0; i < counter.length; i++) {
			for (int j = 0; j < counter[i].length; j++) {
				total += counter[i][j];
			}
		}
		return total;
	}

	/** Returns either the accuracy or the classification error. */
	@Override
	public double getMikroAverage() {
		switch (type) {
			case WEIGHTED_RECALL:
				double[] columnSums = new double[classNames.length];
				for (int c = 0; c < columnSums.length; c++) {
					for (int r = 0; r < counter[c].length; r++) {
						columnSums[c] += counter[c][r];
					}
				}
				double result = 0.0d;
				for (int c = 0; c < columnSums.length; c++) {
					double r = counter[c][c] / columnSums[c];
					result += classWeights[c] * (Double.isNaN(r) ? 0 : r);
				}
				result /= weightSum;
				return result;
			case WEIGHTED_PRECISION:
				double[] rowSums = new double[classNames.length];
				for (int r = 0; r < counter.length; r++) {
					for (int c = 0; c < counter[r].length; c++) {
						rowSums[r] += counter[c][r];
					}
				}
				result = 0.0d;
				for (int r = 0; r < rowSums.length; r++) {
					double p = counter[r][r] / rowSums[r];
					result += classWeights[r] * (Double.isNaN(p) ? 0 : p);
				}
				result /= weightSum;
				return result;
			default:
				throw new RuntimeException("Unknown type " + type + " for weighted multi class performance criterion!");
		}
	}

	/** Returns true. */
	@Override
	public boolean formatPercent() {
		return true;
	}

	@Override
	public double getMikroVariance() {
		return Double.NaN;
	}

	/** Returns the name. */
	@Override
	public String getName() {
		return NAMES[type];
	}

	/** Returns the description. */
	@Override
	public String getDescription() {
		return DESCRIPTIONS[type];
	}

	// ================================================================================

	/** Returns the accuracy or 1 - error. */
	@Override
	public double getFitness() {
		return getAverage();
	}

	/** Returns 1. */
	@Override
	public double getMaxFitness() {
		return 1.0d;
	}

	@Override
	public void buildSingleAverage(Averagable performance) {
		WeightedMultiClassPerformance other = (WeightedMultiClassPerformance) performance;
		for (int i = 0; i < this.counter.length; i++) {
			for (int j = 0; j < this.counter[i].length; j++) {
				this.counter[i][j] += other.counter[i][j];
			}
		}
	}

	public String toWeightString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(", weights: ");
		boolean first = true;
		for (double w : this.classWeights) {
			if (!first) {
				result.append(", ");
			}
			result.append(Tools.formatIntegerIfPossible(w));
			first = false;
		}
		return result.toString();
	}

	// ================================================================================

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(toWeightString() + "");
		result.append(Tools.getLineSeparator() + "ConfusionMatrix:" + Tools.getLineSeparator() + "True:");
		for (int i = 0; i < this.counter.length; i++) {
			result.append("\t" + classNames[i]);
		}

		for (int i = 0; i < this.counter.length; i++) {
			result.append(Tools.getLineSeparator() + classNames[i] + ":");
			for (int j = 0; j < this.counter[i].length; j++) {
				result.append("\t" + Tools.formatIntegerIfPossible(this.counter[j][i]));
			}
		}
		return result.toString();
	}

	public String[] getClassNames() {
		return classNames;
	}

	public double[][] getCounter() {
		return counter;
	}
}
