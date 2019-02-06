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

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.datatable.SimpleDataTableRow;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.plotter.SimplePlotterDialog;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * Helper class containing some methods for Lift plots.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class LiftDataGenerator {

	/** Defines the maximum amount of points which is plotted in the ROC curve. */
	public static final int MAX_LIFT_POINTS = 500;

	private static final int TP = 0;
	private static final int FP = 1;
	private static final int FN = 2;
	private static final int TN = 3;

	private double maxLift = 0;

	/** Creates a new Lift data generator. */
	public LiftDataGenerator() {}

	/**
	 * Creates a list of ROC data poings from the given example set. The example set must have a
	 * binary label attribute and confidence values for both values, i.e. a model must have been
	 * applied on the data.
	 */
	public List<double[]> createLiftDataList(ExampleSet exampleSet) {
		Attribute label = exampleSet.getAttributes().getLabel();
		Attribute predictedLabel = exampleSet.getAttributes().getPredictedLabel();

		// create sorted collection with all label values and example weights
		WeightedConfidenceAndLabel[] calArray = new WeightedConfidenceAndLabel[exampleSet.size()];
		Attribute weightAttr = exampleSet.getAttributes().getWeight();
		Attribute labelAttr = exampleSet.getAttributes().getLabel();
		String positiveClassName = labelAttr.getMapping().mapIndex(label.getMapping().getPositiveIndex());
		int index = 0;
		Iterator<Example> reader = exampleSet.iterator();
		while (reader.hasNext()) {
			Example example = reader.next();
			WeightedConfidenceAndLabel wcl;
			if (weightAttr == null) {
				wcl = new WeightedConfidenceAndLabel((-1) * example.getConfidence(positiveClassName),
						example.getValue(labelAttr), example.getValue(predictedLabel));
			} else {
				wcl = new WeightedConfidenceAndLabel((-1) * example.getConfidence(positiveClassName),
						example.getValue(labelAttr), example.getValue(weightAttr), example.getValue(predictedLabel));
			}
			calArray[index++] = wcl;
		}
		Arrays.sort(calArray);

		List<double[]> tableData = new LinkedList<double[]>();
		double[] confidenceMatrix = new double[4];

		// Iterate through the example set sorted by predictions.
		// In each iteration the lift is calculated and added to the list
		this.maxLift = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < calArray.length; i++) {
			WeightedConfidenceAndLabel wcl = calArray[i];
			double weight = wcl.getWeight();
			double labelValue = wcl.getLabel();
			double predictionValue = wcl.getPrediction();
			if (labelValue == label.getMapping().getPositiveIndex()) {
				if (predictionValue == label.getMapping().getPositiveIndex()) {
					confidenceMatrix[TP] += weight;
				} else {
					confidenceMatrix[FN] += weight;
				}
			} else {
				if (predictionValue == label.getMapping().getPositiveIndex()) {
					confidenceMatrix[FP] += weight;
				} else {
					confidenceMatrix[TN] += weight;
				}
			}
			double lift = (confidenceMatrix[TP] * (confidenceMatrix[FP] + confidenceMatrix[TN]))
					/ ((confidenceMatrix[TP] + confidenceMatrix[FP]) * (confidenceMatrix[TP] + confidenceMatrix[FN]));
			if (!Double.isNaN(lift)) {
				maxLift = Math.max(lift, this.maxLift);
			}

			tableData.add(new double[] { i, lift });
		}

		return tableData;
	}

	/** Creates a dialog containing a plotter for a given list of ROC data points. */
	public void createLiftChartPlot(List<double[]> data) {
		// create data table
		DataTable dataTable = new SimpleDataTable("Lift Chart", new String[] { "Fraction", "Lift" });
		int pointCounter = 0;
		int eachPoint = Math.max(1, (int) Math.round((double) data.size() / (double) MAX_LIFT_POINTS));
		for (double[] point : data) {
			if (pointCounter == 0 || pointCounter % eachPoint == 0 || pointCounter == data.size() - 1) {
				double fraction = point[0];
				double lift = point[1];
				if (Double.isNaN(lift)) {
					lift = this.maxLift;
				}
				dataTable.add(new SimpleDataTableRow(new double[] { fraction, lift }));
			}
			pointCounter++;
		}

		// create plotter
		SimplePlotterDialog plotter = new SimplePlotterDialog(dataTable);
		plotter.setXAxis(0);
		plotter.plotColumn(1, true);
		// plotter.setDrawRange(0.0d, 1.0d, 0.0d, 1.0d);
		plotter.setVisible(true);
	}
}
