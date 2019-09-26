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
package com.rapidminer.tools.math;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import javax.swing.JDialog;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.datatable.SimpleDataTableRow;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeTypeException;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.plotter.ScatterPlotter;
import com.rapidminer.gui.plotter.SimplePlotterDialog;
import com.rapidminer.gui.viewer.ROCChartPlotter;


/**
 * Helper class containing some methods for ROC plots, threshold finding and area under curve
 * calculation.
 *
 * @author Ingo Mierswa, Martin Scholz, Simon Fischer
 */
public class ROCDataGenerator implements Serializable {

	private static final long serialVersionUID = -4473681331604071436L;

	/**
	 * Defines the maximum amount of points which is plotted in the ROC curve.
	 */
	public static final int MAX_ROC_POINTS = 200;

	private double misclassificationCostsPositive = 1.0d;

	private double misclassificationCostsNegative = 1.0d;

	private double slope = 1.0d;

	private double bestThreshold = Double.NaN;

	/**
	 * Creates a new ROC data generator.
	 */
	public ROCDataGenerator(double misclassificationCostsPositive, double misclassificationCostsNegative) {
		this.misclassificationCostsPositive = misclassificationCostsPositive;
		this.misclassificationCostsNegative = misclassificationCostsNegative;
	}

	/**
	 * The best threshold will automatically be determined during the calculation of the ROC data
	 * list. Please note that the given weights are taken into account (defining the slope.
	 */
	public double getBestThreshold() {
		return bestThreshold;
	}

	/**
	 * Equivalent to calling {@link #createROCData(ExampleSet, boolean, ROCBias, String)} with {@code positiveClassName = null}.
	 */
	public ROCData createROCData(ExampleSet exampleSet, boolean useExampleWeights, ROCBias method) {
		return createROCData(exampleSet, useExampleWeights, method, null);
	}

	/**
	 * Creates a list of ROC data points from the given example set. The example set must have a binary label attribute
	 * and confidence values for both values, i.e. a model must have been applied on the data.
	 *
	 * @param exampleSet
	 * 		An example set with a binary label and corresponding confidence values.
	 * @param useExampleWeights
	 * 		If {@code true}, the weight attribute from the specified example set will used for the calculations.
	 * @param method
	 * 		See {@link ROCBias}.
	 * @param positiveClassName
	 * 		If non-{@code null}, this will be used as the positive class. Otherwise the method will fall back to using the
	 * 		labels mapping to decide which is the positive class.
	 * @return The generated {@link ROCData}.
	 */
	public ROCData createROCData(ExampleSet exampleSet, boolean useExampleWeights, ROCBias method, String positiveClassName) {
		Attribute label = exampleSet.getAttributes().getLabel();
		exampleSet.recalculateAttributeStatistics(label);
		Attribute predictedLabel = exampleSet.getAttributes().getPredictedLabel();

		// create sorted collection with all label values and example weights
		WeightedConfidenceAndLabel[] calArray = new WeightedConfidenceAndLabel[exampleSet.size()];
		Attribute weightAttr = null;
		if (useExampleWeights) {
			weightAttr = exampleSet.getAttributes().getWeight();
		}
		Attribute labelAttr = exampleSet.getAttributes().getLabel();

		int positiveIndex = positiveClassName != null ? label.getMapping().getIndex(positiveClassName) :
				label.getMapping().getPositiveIndex();
		int negativeIndex = positiveIndex == label.getMapping().getPositiveIndex() ?
				label.getMapping().getNegativeIndex() : label.getMapping().getPositiveIndex();

		if (label.isNominal() && (label.getMapping().size() == 2)) {
			positiveClassName = labelAttr.getMapping().mapIndex(positiveIndex);
		} else if (label.isNominal() && (label.getMapping().size() == 1)) {
			positiveClassName = labelAttr.getMapping().mapIndex(0);
		} else {
			throw new AttributeTypeException(
					"Cannot calculate ROC data for non-classification labels or for labels with more than 2 classes.");
		}

		int index = 0;
		Iterator<Example> reader = exampleSet.iterator();
		while (reader.hasNext()) {
			Example example = reader.next();
			WeightedConfidenceAndLabel wcl;
			if (weightAttr == null) {
				wcl = new WeightedConfidenceAndLabel(example.getConfidence(positiveClassName), example.getValue(labelAttr),
						example.getValue(predictedLabel));
			} else {
				wcl = new WeightedConfidenceAndLabel(example.getConfidence(positiveClassName), example.getValue(labelAttr),
						example.getValue(predictedLabel), example.getValue(weightAttr));
			}
			calArray[index++] = wcl;
		}
		Arrays.sort(calArray, new WeightedConfidenceAndLabel.WCALComparator(method) {
			/**
			 * Compares two {@link WeightedConfidenceAndLabel}s based on their confidence using
			 * {@link Double#compare(double, double)}. If the confidence is equal, the labels are compared
			 * according to the chosen {@link ROCBias}.
			 */
			@Override
			public int compare(WeightedConfidenceAndLabel o1, WeightedConfidenceAndLabel o2) {
				int compi = (-1) * Double.compare(o1.getConfidence(), o2.getConfidence());
				if (compi == 0) {
					switch (method) {
						case OPTIMISTIC:
							return positiveIndex == 1 ? -Double.compare(o1.getLabel(), o2.getLabel()) : Double.compare(o1.getLabel(), o2.getLabel());
						case PESSIMISTIC:
							return positiveIndex == 1 ? Double.compare(o1.getLabel(), o2.getLabel()) : -Double.compare(o1.getLabel(), o2.getLabel());
						case NEUTRAL:
						default:
							return Double.compare(o1.getLabel(), o2.getLabel());
					}
				} else {
					return compi;
				}
			}
		});

		// The slope is defined by the ratio of positive examples and the
		// different misclassification costs.
		// The formula for the slope is (#pos / #neg) / (costs_neg / costs_pos).
		double ratio = exampleSet.getStatistics(label, Statistics.COUNT, positiveClassName)
				/ exampleSet.getStatistics(label, Statistics.COUNT,
				label.getMapping().mapIndex(negativeIndex));
		slope = misclassificationCostsNegative / misclassificationCostsPositive;
		slope = ratio / slope;

		// The task is to find the isometric that crosses the TP-axis as high as
		// possible
		// The TP value of the best isometric seen so far is stored in
		// bestIsometricsTpValue,
		// the corresponding threshold is stored in bestThreshold.
		double truePositiveWeight = 0.0d;
		double totalWeight = 0.0d;
		double bestIsometricsTpValue = 0;
		bestThreshold = Double.POSITIVE_INFINITY;
		double oldConfidence = 1.0d;

		ROCData rocData = new ROCData();
		ROCPoint last = new ROCPoint(0.0d, 0.0d, 1.0d);

		// Iterate through the example set sorted by predictions.
		// In each iteration the example with next highest confidence of being
		// positive
		// is added to the set of covered examples.
		double oldLabel = -1;
		for (int i = 0; i < calArray.length; i++) {
			WeightedConfidenceAndLabel wcl = calArray[i];
			double currentConfidence = wcl.getConfidence();

			boolean mustStartNewPoint = (currentConfidence != oldConfidence);
			if (method != ROCBias.NEUTRAL) {
				mustStartNewPoint |= (oldLabel != wcl.getLabel());
			}
			if (mustStartNewPoint) {
				rocData.addPoint(last);
				oldConfidence = currentConfidence;
				oldLabel = wcl.getLabel();
			}
			double weight = wcl.getWeight();
			double falsePositiveWeight = totalWeight - truePositiveWeight;
			if (wcl.getLabel() == positiveIndex) {
				truePositiveWeight += weight;
			} else {
				// c is the value at the TP axis connecting the current point in
				// ROC space
				// with a line with the slope given by the user.
				double c = truePositiveWeight - (falsePositiveWeight * slope);
				if (c > bestIsometricsTpValue) {
					bestIsometricsTpValue = c;
					bestThreshold = wcl.getConfidence();
				}
			}

			totalWeight += weight;
			last = new ROCPoint(totalWeight - truePositiveWeight, truePositiveWeight, currentConfidence);
		}
		rocData.addPoint(last);

		// Calculation for last point (upper right):
		double c = truePositiveWeight - ((totalWeight - truePositiveWeight) * slope);
		if (c > bestIsometricsTpValue) {
			bestThreshold = Double.NEGATIVE_INFINITY;
			bestIsometricsTpValue = c;
		}

		// scaling for plotting
		rocData.setTotalPositives(truePositiveWeight);
		rocData.setTotalNegatives(totalWeight - truePositiveWeight);
		if (truePositiveWeight != 0) {
			rocData.setBestIsometricsTPValue(bestIsometricsTpValue / truePositiveWeight);
		} else {
			rocData.setBestIsometricsTPValue(0);
		}
		return rocData;
	}

	private DataTable createDataTable(ROCData data, boolean showSlope, boolean showThresholds) {
		DataTable dataTable = new SimpleDataTable("ROC Plot", new String[]{"FP/N", "TP/P", "Slope", "Threshold"});
		Iterator<ROCPoint> i = data.iterator();
		int pointCounter = 0;
		int eachPoint = Math.max(1, (int) Math.round((double) data.getNumberOfPoints() / (double) MAX_ROC_POINTS));
		while (i.hasNext()) {
			ROCPoint point = i.next();
			if ((pointCounter == 0) || ((pointCounter % eachPoint) == 0) || (!i.hasNext())) {
				// draw only MAX_ROC_POINTS points
				double fpRate = 0;
				if (point.getFalsePositives() != 0 && data.getTotalNegatives() != 0) {
					fpRate = point.getFalsePositives() / data.getTotalNegatives();
				}
				double tpRate = 0;
				if (point.getTruePositives() != 0 && data.getTotalPositives() != 0) {
					tpRate = point.getTruePositives() / data.getTotalPositives();
				}
				double threshold = point.getConfidence();
				double tnovertp = 0;
				if (data.getTotalNegatives() != 0 && data.getTotalPositives() != 0) {
					tnovertp = data.getTotalNegatives() / data.getTotalPositives();
				}
				dataTable.add(new SimpleDataTableRow(new double[]{
						fpRate, // x
						tpRate, // y1
						data.getBestIsometricsTPValue() + (fpRate * slope * tnovertp), // y2: slope
						threshold // y3: threshold or confidence
				}));
			}
			pointCounter++;
		}
		return dataTable;
	}

	/**
	 * Creates a dialog containing a plotter for a given list of ROC data points.
	 */
	public void createROCPlotDialog(ROCData data, boolean showSlope, boolean showThresholds) {
		SimplePlotterDialog plotter = new SimplePlotterDialog(createDataTable(data, showSlope, showThresholds));
		plotter.setXAxis(0);
		plotter.plotColumn(1, true);
		if (showSlope) {
			plotter.plotColumn(2, true);
		}
		if (showThresholds) {
			plotter.plotColumn(3, true);
		}
		plotter.setDrawRange(0.0d, 1.0d, 0.0d, 1.0d);
		plotter.setPointType(ScatterPlotter.LINES);
		plotter.setSize(500, 500);
		plotter.setLocationRelativeTo(plotter.getOwner());
		plotter.setVisible(true);
	}

	/**
	 * Creates a dialog containing a plotter for a given list of ROC data points.
	 */
	public void createROCPlotDialog(ROCData data) {
		ROCChartPlotter plotter = new ROCChartPlotter();
		plotter.addROCData("ROC", data);
		JDialog dialog = new JDialog();
		dialog.setTitle("ROC Plot");
		dialog.add(plotter);
		dialog.setSize(500, 500);
		dialog.setLocationRelativeTo(ApplicationFrame.getApplicationFrame());
		dialog.setVisible(true);
	}

	/**
	 * Calculates the area under the curve for a given list of ROC data points.
	 */
	public double calculateAUC(ROCData rocData) {
		if (rocData.getNumberOfPoints() == 2) {
			return 0.5;
		}

		// calculate AUC (area under curve)
		double aucSum = 0.0d;
		double[] last = null;
		Iterator<ROCPoint> i = rocData.iterator();
		while (i.hasNext()) {
			ROCPoint point = i.next();
			double fpDivN = 0;
			if (point.getFalsePositives() != 0 && rocData.getTotalNegatives() != 0) {
				// false positives divided by sum of all negatives
				fpDivN = point.getFalsePositives() / rocData.getTotalNegatives();
			}

			double tpDivP = 0;
			if (point.getTruePositives() != 0 && rocData.getTotalPositives() != 0) {
				// true positives divided by sum of all positives
				tpDivP = point.getTruePositives() / rocData.getTotalPositives();
			}

			if (last != null) {

				double width = fpDivN - last[0];
				double leftHeight = last[1];
				double rightHeight = tpDivP;
				aucSum += leftHeight * width + (rightHeight - leftHeight) * width / 2;
			}
			last = new double[]{fpDivN, tpDivP};
		}

		return aucSum;
	}
}
