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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.Averagable;


/**
 * This class encapsulates the well known binary classification criteria precision and recall.
 * Furthermore it can be used to calculate the fallout, the equally weighted f-measure (f1-measure),
 * the lift, and the values for TRUE_POSITIVE, FALSE_POSITIVE, TRUE_NEGATIVE, and FALSE_NEGATIVE.
 * With &quot;positive&quot; we refer to the first class and with &quot;negative&quot; we refer to
 * the second.
 *
 * @author Ingo Mierswa, Simon Fischer
 */
public class BinaryClassificationPerformance extends MeasuredPerformance {

	private static final long serialVersionUID = 7475134460409215015L;

	public static final int PRECISION = 0;

	public static final int RECALL = 1;

	public static final int LIFT = 2;

	public static final int FALLOUT = 3;

	public static final int F_MEASURE = 4;

	public static final int FALSE_POSITIVE = 5;

	public static final int FALSE_NEGATIVE = 6;

	public static final int TRUE_POSITIVE = 7;

	public static final int TRUE_NEGATIVE = 8;

	public static final int SENSITIVITY = 9;

	public static final int SPECIFICITY = 10;

	public static final int YOUDEN = 11;

	public static final int POSITIVE_PREDICTIVE_VALUE = 12;

	public static final int NEGATIVE_PREDICTIVE_VALUE = 13;

	public static final int PSEP = 14;

	private static final int N = 0;

	private static final int P = 1;

	public static final String[] NAMES = { "precision", "recall", "lift", "fallout", "f_measure", "false_positive",
			"false_negative", "true_positive", "true_negative", "sensitivity", "specificity", "youden",
			"positive_predictive_value", "negative_predictive_value", "psep" };

	public static final String[] DESCRIPTIONS = {
			"Relative number of correctly as positive classified examples among all examples classified as positive",
			"Relative number of correctly as positive classified examples among all positive examples",
			"The lift of the positive class",
			"Relative number of incorrectly as positive classified examples among all negative examples",
			"Combination of precision and recall: f=2pr/(p+r)",
			"Absolute number of incorrectly as positive classified examples",
			"Absolute number of incorrectly as negative classified examples",
			"Absolute number of correctly as positive classified examples",
			"Absolute number of correctly as negative classified examples",
			"Relative number of correctly as positive classified examples among all positive examples (like recall)",
			"Relative number of correctly as negative classified examples among all negative examples",
			"The sum of sensitivity and specificity minus 1",
			"Relative number of correctly as positive classified examples among all examples classified as positive (same as precision)",
			"Relative number of correctly as negative classified examples among all examples classified as negative",
			"The sum of the positive predicitve value and the negative predictive value minus 1" };

	private int type = 0;

	/** true label, predicted label. PP = TP, PN = FN, NP = FP, NN = TN. */
	private double[][] counter = new double[2][2];

	/** Name of the positive class. */
	private String positiveClassName = "";

	/** Name of the negative class. */
	private String negativeClassName = "";

	/** The predicted label attribute. */
	private Attribute predictedLabelAttribute;

	/** The label attribute. */
	private Attribute labelAttribute;

	/** The weight attribute. Might be null. */
	private Attribute weightAttribute;

	/**
	 * True if the user defined positive class should be used instead of the label's default mapping.
	 */
	private boolean userDefinedPositiveClass = false;

	public BinaryClassificationPerformance() {
		type = -1;
	}

	public BinaryClassificationPerformance(BinaryClassificationPerformance o) {
		super(o);
		this.type = o.type;
		this.counter = new double[2][2];
		this.counter[N][N] = o.counter[N][N];
		this.counter[P][N] = o.counter[P][N];
		this.counter[N][P] = o.counter[N][P];
		this.counter[P][P] = o.counter[P][P];
		if (o.predictedLabelAttribute != null) {
			this.predictedLabelAttribute = (Attribute) o.predictedLabelAttribute.clone();
		}
		if (o.labelAttribute != null) {
			this.labelAttribute = (Attribute) o.labelAttribute.clone();
		}
		if (o.weightAttribute != null) {
			this.weightAttribute = (Attribute) o.weightAttribute.clone();
		}
		this.positiveClassName = o.positiveClassName;
		this.negativeClassName = o.negativeClassName;
		this.userDefinedPositiveClass = o.userDefinedPositiveClass;
	}

	public BinaryClassificationPerformance(int type) {
		this.type = type;
	}

	/** For test cases only. */
	public BinaryClassificationPerformance(int type, double[][] counter) {
		this.type = type;
		this.counter[N][N] = counter[N][N];
		this.counter[N][P] = counter[N][P];
		this.counter[P][N] = counter[P][N];
		this.counter[P][P] = counter[P][P];
	}

	public static BinaryClassificationPerformance newInstance(String name) {
		for (int i = 0; i < NAMES.length; i++) {
			if (NAMES[i].equals(name)) {
				return new BinaryClassificationPerformance(i);
			}
		}
		return null;
	}

	@Override
	public double getExampleCount() {
		return counter[P][P] + counter[N][P] + counter[P][N] + counter[N][N];
	}

	// ================================================================================

	@Override
	public void startCounting(ExampleSet eSet, boolean useExampleWeights) throws OperatorException {
		super.startCounting(eSet, useExampleWeights);
		this.predictedLabelAttribute = eSet.getAttributes().getPredictedLabel();
		this.labelAttribute = eSet.getAttributes().getLabel();
		if (!labelAttribute.isNominal()) {
			throw new UserError(null, 120, labelAttribute.getName(), Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(labelAttribute
					.getValueType()), Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(Ontology.NOMINAL));
		}
		if (!predictedLabelAttribute.isNominal()) {
			throw new UserError(null, 120, predictedLabelAttribute.getName(),
					Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(predictedLabelAttribute.getValueType()),
					Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(Ontology.NOMINAL));
		}
		if (labelAttribute.getMapping().size() != 2) {
			throw new UserError(null, 118, new Object[] { "'" + labelAttribute.getName() + "'",
					Integer.valueOf(labelAttribute.getMapping().getValues().size()),
					"2 for calculation of '" + getName() + "'" });
		}
		if (predictedLabelAttribute.getMapping().size() != 2) {
			throw new UserError(null, 118, new Object[] { "'" + predictedLabelAttribute.getName() + "'",
					Integer.valueOf(predictedLabelAttribute.getMapping().getValues().size()),
					"2 for calculation of '" + getName() + "'" });
		}
		if (!labelAttribute.getMapping().equals(predictedLabelAttribute.getMapping())) {
			throw new UserError(null, 157);
		}

		updatePosNegClassNames();

		if (useExampleWeights) {
			this.weightAttribute = eSet.getAttributes().getWeight();
		}
		this.counter = new double[2][2];
	}

	@Override
	public void countExample(Example example) {
		String labelString = example.getNominalValue(labelAttribute);
		int label = positiveClassName.equals(labelString) ? P : N;
		String predString = example.getNominalValue(predictedLabelAttribute);
		int plabel = positiveClassName.equals(predString) ? P : N;

		double weight = 1.0d;
		if (weightAttribute != null) {
			weight = example.getValue(weightAttribute);
		}
		counter[label][plabel] += weight;
	}

	@Override
	public double getMikroAverage() {
		double x = 0.0d, y = 0.0d;
		switch (type) {
			case PRECISION:
				x = counter[P][P];
				y = counter[P][P] + counter[N][P];
				break;
			case RECALL:
				x = counter[P][P];
				y = counter[P][P] + counter[P][N];
				break;
			case LIFT:
				x = counter[P][P] / (counter[P][P] + counter[P][N]);
				y = (counter[P][P] + counter[N][P]) / (counter[P][P] + counter[P][N] + counter[N][P] + counter[N][N]);
				break;
			case FALLOUT:
				x = counter[N][P];
				y = counter[N][P] + counter[N][N];
				break;

			case F_MEASURE:
				x = counter[P][P];
				x *= x;
				x *= 2;
				y = x + counter[P][P] * counter[P][N] + counter[P][P] * counter[N][P];
				break;

			case FALSE_NEGATIVE:
				x = counter[P][N];
				y = 1;
				break;
			case FALSE_POSITIVE:
				x = counter[N][P];
				y = 1;
				break;
			case TRUE_NEGATIVE:
				x = counter[N][N];
				y = 1;
				break;
			case TRUE_POSITIVE:
				x = counter[P][P];
				y = 1;
				break;
			case SENSITIVITY:
				x = counter[P][P];
				y = counter[P][P] + counter[P][N];
				break;
			case SPECIFICITY:
				x = counter[N][N];
				y = counter[N][N] + counter[N][P];
				break;
			case YOUDEN:
				x = counter[N][N] * counter[P][P] - counter[P][N] * counter[N][P];
				y = (counter[P][P] + counter[P][N]) * (counter[N][P] + counter[N][N]);
				break;
			case POSITIVE_PREDICTIVE_VALUE:
				x = counter[P][P];
				y = counter[P][P] + counter[N][P];
				break;
			case NEGATIVE_PREDICTIVE_VALUE:
				x = counter[N][N];
				y = counter[N][N] + counter[P][N];
				break;
			case PSEP:
				x = counter[N][N] * counter[P][P] + counter[N][N] * counter[N][P] - counter[N][P] * counter[N][N]
						- counter[N][P] * counter[P][N];
				y = counter[P][P] * counter[N][N] + counter[P][P] * counter[P][N] + counter[N][P] * counter[N][N]
						+ counter[N][P] * counter[P][N];
				break;
			default:
				throw new RuntimeException("Illegal value for type in BinaryClassificationPerformance: " + type);
		}
		if (y == 0) {
			return Double.NaN;
		}
		return x / y;
	}

	@Override
	public double getFitness() {
		switch (type) {
			case PRECISION:
			case RECALL:
			case LIFT:
			case TRUE_POSITIVE:
			case TRUE_NEGATIVE:
			case F_MEASURE:
			case SENSITIVITY:
			case SPECIFICITY:
			case YOUDEN:
			case POSITIVE_PREDICTIVE_VALUE:
			case NEGATIVE_PREDICTIVE_VALUE:
			case PSEP:
				return getAverage();
			case FALLOUT:
			case FALSE_POSITIVE:
			case FALSE_NEGATIVE:
				if (getAverage() == 0.0d) {
					return Double.POSITIVE_INFINITY;
				}
				return 1.0d / getAverage();
			default:
				throw new RuntimeException("Illegal value for type in BinaryClassificationPerformance: " + type);
		}
	}

	@Override
	public double getMaxFitness() {
		switch (type) {
			case PRECISION:
			case RECALL:
			case F_MEASURE:
			case SENSITIVITY:
			case SPECIFICITY:
				return 1.0d;
			case LIFT:
			case TRUE_POSITIVE:
			case TRUE_NEGATIVE:
			case FALLOUT:
			case FALSE_POSITIVE:
			case FALSE_NEGATIVE:
			case YOUDEN:
			case POSITIVE_PREDICTIVE_VALUE:
			case NEGATIVE_PREDICTIVE_VALUE:
			case PSEP:
				return Double.POSITIVE_INFINITY;
			default:
				throw new RuntimeException("Illegal value for type in BinaryClassificationPerformance: " + type);
		}
	}

	@Override
	public double getMikroVariance() {
		return Double.NaN;
	}

	// ================================================================================

	@Override
	public String getName() {
		return NAMES[type];
	}

	@Override
	public String getDescription() {
		return DESCRIPTIONS[type];
	}

	@Override
	public boolean formatPercent() {
		switch (type) {
			case TRUE_POSITIVE:
			case TRUE_NEGATIVE:
			case FALSE_POSITIVE:
			case FALSE_NEGATIVE:
			case YOUDEN:
			case PSEP:
				return false;
			default:
				return true;
		}
	}

	@Override
	public void buildSingleAverage(Averagable performance) {
		BinaryClassificationPerformance other = (BinaryClassificationPerformance) performance;
		if (this.type != other.type) {
			throw new RuntimeException("Cannot build average of different error types (" + NAMES[this.type] + "/"
					+ NAMES[other.type] + ").");
		}
		if (!this.positiveClassName.equals(other.positiveClassName)) {
			throw new RuntimeException("Cannot build average for different positive classes (" + this.positiveClassName
					+ "/" + other.positiveClassName + ").");
		}
		this.counter[N][N] += other.counter[N][N];
		this.counter[N][P] += other.counter[N][P];
		this.counter[P][N] += other.counter[P][N];
		this.counter[P][P] += other.counter[P][P];
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (positive class: " + positiveClassName + ")");
		result.append(Tools.getLineSeparator() + "ConfusionMatrix:" + Tools.getLineSeparator() + "True:");
		result.append("\t" + negativeClassName);
		result.append("\t" + positiveClassName);
		result.append(Tools.getLineSeparator() + negativeClassName + ":");
		result.append("\t" + Tools.formatIntegerIfPossible(counter[N][N]));
		result.append("\t" + Tools.formatIntegerIfPossible(counter[P][N]));
		result.append(Tools.getLineSeparator() + positiveClassName + ":");
		result.append("\t" + Tools.formatIntegerIfPossible(counter[N][P]));
		result.append("\t" + Tools.formatIntegerIfPossible(counter[P][P]));
		return result.toString();
	}

	public double[][] getCounter() {
		return counter;
	}

	public String getNegativeClassName() {
		return negativeClassName;
	}

	public String getPositiveClassName() {
		return positiveClassName;
	}

	public String getTitle() {
		return super.toString() + " (positive class: " + getPositiveClassName() + ")";
	}

	/**
	 * Overrides the default positive class name with a user defined positive class name. If the argument is null it
	 * falls back to the default positive class name defined by the label's intern mapping.
	 *
	 * @param positiveClassName
	 * 		The positive class name or null to fall back to the default positive class name.
	 */
	public void setUserDefinedPositiveClassName(String positiveClassName) {
		this.positiveClassName = positiveClassName;
		userDefinedPositiveClass = positiveClassName != null;
	}

	private void updatePosNegClassNames() throws UserError {
		String mapNegativeClassName = predictedLabelAttribute.getMapping().getNegativeString();
		String mapPositiveClassName = predictedLabelAttribute.getMapping().getPositiveString();
		if (userDefinedPositiveClass) {
			if (positiveClassName.equals(mapPositiveClassName)) {
				negativeClassName = mapNegativeClassName;
			} else if (positiveClassName.equals(mapNegativeClassName)) {
				negativeClassName = mapPositiveClassName;
			} else {
				throw new UserError(null, "invalid_positive_class", positiveClassName);
			}
		} else {
			positiveClassName = mapPositiveClassName;
			negativeClassName = mapNegativeClassName;
		}
	}
}
