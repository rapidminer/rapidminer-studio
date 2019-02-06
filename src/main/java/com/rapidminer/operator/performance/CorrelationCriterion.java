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
import com.rapidminer.tools.math.Averagable;


/**
 * Computes the empirical corelation coefficient 'r' between label and prediction. For
 * <code>P=prediction, L=label, V=Variance, Cov=Covariance</code> we calculate r by: <br>
 * <code>Cov(L,P) / sqrt(V(L)*V(P))</code>.
 * 
 * Implementation hint: this implementation intensionally recomputes the mean and variance of
 * prediction and label despite the fact that they are available by the Attribute objects. The
 * reason: it can happen, that there are some examples which have a NaN as prediction or label, but
 * not both. In this case, mean and variance stored in tie Attributes and computed here can differ.
 * 
 * @author Robert Rudolph, Ingo Mierswa, Sebastian Land
 */
public class CorrelationCriterion extends MeasuredPerformance {

	private static final long serialVersionUID = -8789903466296509903L;

	private Attribute labelAttribute;

	private Attribute predictedLabelAttribute;

	private Attribute weightAttribute;

	private double exampleCount = 0;

	private double sumLabel;

	private double sumPredict;

	private double sumLabelPredict;

	private double sumLabelSqr;

	private double sumPredictSqr;

	public CorrelationCriterion() {}

	public CorrelationCriterion(CorrelationCriterion sc) {
		super(sc);
		this.sumLabelPredict = sc.sumLabelPredict;
		this.sumLabelSqr = sc.sumLabelSqr;
		this.sumPredictSqr = sc.sumPredictSqr;
		this.sumLabel = sc.sumLabel;
		this.sumPredict = sc.sumPredict;
		this.exampleCount = sc.exampleCount;
		this.labelAttribute = (Attribute) sc.labelAttribute.clone();
		this.predictedLabelAttribute = (Attribute) sc.predictedLabelAttribute.clone();
		if (sc.weightAttribute != null) {
			this.weightAttribute = (Attribute) sc.weightAttribute.clone();
		}
	}

	@Override
	public double getExampleCount() {
		return exampleCount;
	}

	/** Returns the maximum fitness of 1.0. */
	@Override
	public double getMaxFitness() {
		return 1.0d;
	}

	/** Updates all sums needed to compute the correlation coefficient. */
	@Override
	public void countExample(Example example) {
		double label = example.getValue(labelAttribute);
		double plabel = example.getValue(predictedLabelAttribute);
		if (labelAttribute.isNominal()) {
			String predLabelString = predictedLabelAttribute.getMapping().mapIndex((int) plabel);
			plabel = labelAttribute.getMapping().getIndex(predLabelString);
		}

		double weight = 1.0d;
		if (weightAttribute != null) {
			weight = example.getValue(weightAttribute);
		}

		double prod = label * plabel * weight;
		if (!Double.isNaN(prod)) {
			sumLabelPredict += prod;
			sumLabel += label * weight;
			sumLabelSqr += label * label * weight;
			sumPredict += plabel * weight;
			sumPredictSqr += plabel * plabel * weight;
			exampleCount += weight;
		}
	}

	@Override
	public String getDescription() {
		return "Returns the correlation coefficient between the label and predicted label.";
	}

	@Override
	public double getMikroAverage() {
		double divider = Math.sqrt(exampleCount * sumLabelSqr - sumLabel * sumLabel)
				* Math.sqrt(exampleCount * sumPredictSqr - sumPredict * sumPredict);
		double r = (exampleCount * sumLabelPredict - sumLabel * sumPredict) / divider;
		if (r < 0 || Double.isNaN(r)) {
			return 0; // possible due to rounding errors
		}
		if (r > 1) {
			return 1;
		}
		return r;
	}

	@Override
	public double getMikroVariance() {
		return Double.NaN;
	}

	@Override
	public void startCounting(ExampleSet eset, boolean useExampleWeights) throws OperatorException {
		super.startCounting(eset, useExampleWeights);
		exampleCount = 0;
		sumLabelPredict = sumLabel = sumPredict = sumLabelSqr = sumPredictSqr = 0.0d;
		this.labelAttribute = eset.getAttributes().getLabel();
		this.predictedLabelAttribute = eset.getAttributes().getPredictedLabel();
		if (useExampleWeights) {
			this.weightAttribute = eset.getAttributes().getWeight();
		}
	}

	@Override
	public void buildSingleAverage(Averagable performance) {
		CorrelationCriterion other = (CorrelationCriterion) performance;
		this.sumLabelPredict += other.sumLabelPredict;
		this.sumLabelSqr += other.sumLabelSqr;
		this.sumPredictSqr += other.sumPredictSqr;
		this.sumLabel += other.sumLabel;
		this.sumPredict += other.sumPredict;
		this.exampleCount += other.exampleCount;
	}

	@Override
	public double getFitness() {
		return getAverage();
	}

	@Override
	public String getName() {
		return "correlation";
	}
}
