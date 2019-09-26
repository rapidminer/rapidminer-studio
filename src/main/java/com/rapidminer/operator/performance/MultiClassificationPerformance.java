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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.Averagable;


/**
 * Measures the accuracy and classification error for both binary classification problems and multi
 * class problems. Additionally, this performance criterion can also compute the kappa statistics
 * for multi class problems. This is calculated as k = (P(A) - P(E)) / (1 - P(E)) with [ P(A) =
 * diagonal sum / number of examples ] and [ P(E) = sum over i of ((sum of i-th row * sum of i-th
 * column) / (n to the power of 2) ].
 *
 * @author Ingo Mierswa
 */
public class MultiClassificationPerformance extends MeasuredPerformance {

	private static final long serialVersionUID = 3068421566038331525L;

	/** Indicates an undefined type (should not happen). */
	public static final int UNDEFINED = -1;

	/** Indicates accuracy. */
	public static final int ACCURACY = 0;

	/** Indicates classification error. */
	public static final int ERROR = 1;

	/** Indicates kappa statistics. */
	public static final int KAPPA = 2;

	/** The names of the criteria. */
	public static final String[] NAMES = { "accuracy", "classification_error", "kappa" };

	/** The descriptions of the criteria. */
	public static final String[] DESCRIPTIONS = { "Relative number of correctly classified examples",
		"Relative number of misclassified examples", "The kappa statistics for the classification" };

	/**
	 * The counter for true labels and the prediction.
	 */
	private double[][] counter;

	/** The class names of the label. Used for logging and result display. */
	private String[] classNames;

	/** Maps class names to indices. */
	private Map<String, Integer> classNameMap = new HashMap<String, Integer>();

	/** The currently used label attribute. */
	private Attribute labelAttribute;

	/** The currently used predicted label attribute. */
	private Attribute predictedLabelAttribute;

	/** The weight attribute. Might be null. */
	private Attribute weightAttribute;

	/** The type of this performance: accuracy or classification error. */
	private int type = ACCURACY;

	/** Creates a MultiClassificationPerformance with undefined type. */
	public MultiClassificationPerformance() {
		this(UNDEFINED);
	}

	/** Creates a MultiClassificationPerformance with the given type. */
	public MultiClassificationPerformance(int type) {
		this.type = type;
	}

	/** Clone constructor. */
	public MultiClassificationPerformance(MultiClassificationPerformance m) {
		super(m);
		this.type = m.type;
		if (m.classNames != null) {
			this.classNames = Arrays.copyOf(m.classNames, m.classNames.length);
			this.classNameMap.putAll(m.classNameMap);
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

	/** Creates a MultiClassificationPerformance with the given type. */
	public static MultiClassificationPerformance newInstance(String name) {
		for (int i = 0; i < NAMES.length; i++) {
			if (NAMES[i].equals(name)) {
				return new MultiClassificationPerformance(i);
			}
		}
		return null;
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

	/** Initializes the criterion and sets the label. */
	@Override
	public void startCounting(ExampleSet eSet, boolean useExampleWeights) throws OperatorException {
		com.rapidminer.example.Tools.hasNominalLabels(eSet, "calculation of classification performance criteria");
		this.labelAttribute = eSet.getAttributes().getLabel();
		this.predictedLabelAttribute = eSet.getAttributes().getPredictedLabel();
		if (this.predictedLabelAttribute == null || !this.predictedLabelAttribute.isNominal()) {
			throw new UserError(null, 101, "calculation of classification performance criteria",
					predictedLabelAttribute.getName());
		}

		super.startCounting(eSet, useExampleWeights);
		if (useExampleWeights) {
			this.weightAttribute = eSet.getAttributes().getWeight();
		}

		Collection<String> labelValues = this.labelAttribute.getMapping().getValues();
		Collection<String> predictedLabelValues = this.predictedLabelAttribute.getMapping().getValues();

		// searching for greater mapping for making symmetric matrix in case of different mapping
		// sizes
		Collection<String> unionedMapping = new LinkedHashSet<String>(labelValues);
		unionedMapping.addAll(predictedLabelValues);

		this.counter = new double[unionedMapping.size()][unionedMapping.size()];
		this.classNames = new String[unionedMapping.size()];
		int n = 0;
		for (String labelValue : unionedMapping) {
			classNames[n] = labelValue;
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

	/** Returns either the accuracy or the classification error. */
	@Override
	public double getMikroAverage() {
		double diagonal = 0, total = 0;
		for (int i = 0; i < counter.length; i++) {
			diagonal += counter[i][i];
			for (int j = 0; j < counter[i].length; j++) {
				total += counter[i][j];
			}
		}
		if (total == 0) {
			return Double.NaN;
		}

		// returns either the accuracy, the error, or the kappa statistics
		double accuracy = diagonal / total;
		switch (type) {
			case ACCURACY:
				return accuracy;
			case ERROR:
				return 1.0d - accuracy;
			case KAPPA:
				double pa = accuracy;
				double pe = 0.0d;
				for (int i = 0; i < counter.length; i++) {
					double row = 0.0d;
					double column = 0.0d;
					for (int j = 0; j < counter[i].length; j++) {
						row += counter[i][j];
						column += counter[j][i];
					}
					// pe += ((row * column) / Math.pow(total, counter.length));
					pe += row * column / (total * total);
				}
				return (pa - pe) / (1.0d - pe);
			default:
				throw new RuntimeException("Unknown type " + type + " for multi class performance criterion!");
		}
	}

	/** Returns true. */
	@Override
	public boolean formatPercent() {
		if (type == KAPPA) {
			return false;
		} else {
			return true;
		}
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
		if (type == ERROR) {
			return 1.0d - getAverage();
		} else {
			return getAverage();
		}
	}

	/** Returns 1. */
	@Override
	public double getMaxFitness() {
		return 1.0d;
	}

	@Override
	public void buildSingleAverage(Averagable performance) {
		MultiClassificationPerformance other = (MultiClassificationPerformance) performance;

		// can only add the counter matrices if they have the same "headers" (classNames)
		if (this.classNames.length == other.classNames.length && Arrays.equals(this.classNames, other.classNames)) {
			for (int i = 0; i < this.counter.length; i++) {
				for (int j = 0; j < this.counter[i].length; j++) {
					this.counter[i][j] += other.counter[i][j];
				}
			}
		} else {
			// if the classNames are different build the union and create new counter matrix
			// associated to this union
			String[] unionClassNames = combineArrays(this.classNames, other.classNames);
			double[][] unionCounter = new double[unionClassNames.length][unionClassNames.length];
			Map<String, Integer> unionClassNameMap = new HashMap<String, Integer>();
			for (int i = 0; i < unionClassNames.length; i++) {
				String nameI = unionClassNames[i];
				unionClassNameMap.put(nameI, i);

				for (int j = 0; j < unionClassNames.length; j++) {
					String nameJ = unionClassNames[j];

					double thisValue = 0;
					Integer indexNameI = this.classNameMap.get(nameI);
					Integer indexNameJ = this.classNameMap.get(nameJ);
					if (indexNameI != null && indexNameJ != null) {
						thisValue = this.counter[indexNameI][indexNameJ];
					}

					double otherValue = 0;
					indexNameI = other.classNameMap.get(nameI);
					indexNameJ = other.classNameMap.get(nameJ);
					if (indexNameI != null && indexNameJ != null) {
						otherValue = other.counter[indexNameI][indexNameJ];
					}

					unionCounter[i][j] = thisValue + otherValue;

				}
			}

			this.classNames = unionClassNames;
			this.classNameMap = unionClassNameMap;
			this.counter = unionCounter;
		}

	}

	/**
	 * Builds the union of the arrays, taking first the elements of the firstArray and then the new
	 * elements of the secondArray.
	 *
	 * @param firstArray
	 * @param secondArray
	 * @return the union of firstArray and secondArray
	 */
	private String[] combineArrays(String[] firstArray, String[] secondArray) {
		Set<String> unionSet = new LinkedHashSet<>();
		unionSet.addAll(Arrays.asList(firstArray));
		unionSet.addAll(Arrays.asList(secondArray));
		return unionSet.toArray(new String[unionSet.size()]);
	}

	// ================================================================================

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
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

	public String getTitle() {
		return super.toString();
	}

	public String[] getClassNames() {
		return classNames;
	}

	public double[][] getCounter() {
		return counter;
	}
}
