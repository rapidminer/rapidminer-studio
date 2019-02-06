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
package com.rapidminer.gui.plotter;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.tools.SwingTools;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * This class encapsulates all information about quartiles.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class Quartile {

	public static final int QUARTILE_WIDTH = 20;

	private double median;
	private double mean;
	private double standardDeviation;
	private double lowerQuartile;
	private double upperQuartile;
	private double lowerWhisker;
	private double upperWhisker;
	private double[] outliers;

	private Color color = SwingTools.LIGHT_BLUE;

	public Quartile(double median, double mean, double standardDeviation, double lowerQuartile, double upperQuartile,
			double lowerWhisker, double upperWhisker, double[] outliers) {
		this.median = median;
		this.mean = mean;
		this.standardDeviation = standardDeviation;
		this.lowerQuartile = lowerQuartile;
		this.upperQuartile = upperQuartile;
		this.lowerWhisker = lowerWhisker;
		this.upperWhisker = upperWhisker;
		this.outliers = outliers;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public Color getColor() {
		return this.color;
	}

	/** Returns the smallest value occupied by this quartile. */
	public double getMin() {
		double min = Math.min(lowerWhisker, mean - standardDeviation);
		for (int i = 0; i < outliers.length; i++) {
			min = Math.min(min, outliers[i]);
		}
		return min;
	}

	/** Returns the biggest value occupied by this quartile. */
	public double getMax() {
		double max = Math.max(upperWhisker, mean + standardDeviation);
		for (int i = 0; i < outliers.length; i++) {
			max = Math.max(max, outliers[i]);
		}
		return max;
	}

	public double getMedian() {
		return median;
	}

	public double getMean() {
		return mean;
	}

	public double getStandardDeviation() {
		return standardDeviation;
	}

	public double getLowerQuartile() {
		return lowerQuartile;
	}

	public double getUpperQuartile() {
		return upperQuartile;
	}

	public double getLowerWhisker() {
		return lowerWhisker;
	}

	public double getUpperWhisker() {
		return upperWhisker;
	}

	public double[] getOutliers() {
		return outliers;
	}

	public static Quartile calculateQuartile(DataTable table, int column) {
		double mean = 0.0d;
		double squaredSum = 0.0d;
		List<Double> values = new ArrayList<Double>();
		Iterator<DataTableRow> i = table.iterator();

		while (i.hasNext()) {
			DataTableRow row = i.next();
			double value = row.getValue(column);
			mean += value;
			squaredSum += value * value;
			values.add(value);
		}
		mean /= table.getNumberOfRows();
		squaredSum /= table.getNumberOfRows();
		double standardDeviation = Math.sqrt(squaredSum - (mean * mean));
		return calculateQuartile(mean, standardDeviation, values);
	}

	public static Quartile calculateQuartile(List<Double> values) {
		double mean = 0.0d;
		double squaredSum = 0.0d;
		Iterator<Double> i = values.iterator();
		while (i.hasNext()) {
			double value = i.next();
			mean += value;
			squaredSum += value * value;
		}
		mean /= values.size();
		squaredSum /= values.size();
		double standardDeviation = Math.sqrt(squaredSum - (mean * mean));
		return calculateQuartile(mean, standardDeviation, values);
	}

	private static Quartile calculateQuartile(double mean, double standardDeviation, List<Double> values) {
		Collections.sort(values);
		int medianIndex = (int) (values.size() * 0.5d);
		int lowerQuartileIndex = (int) (values.size() * 0.25d);
		int upperQuartileIndex = (int) (values.size() * 0.75d);
		int lowerWhiskerIndex = (int) (values.size() * 0.05d);
		int upperWhiskerIndex = (int) (values.size() * 0.95d);
		double median = values.get(medianIndex);
		double lowerQuartile = values.get(lowerQuartileIndex);
		double upperQuartile = values.get(upperQuartileIndex);
		double lowerWhisker = values.get(lowerWhiskerIndex);
		double upperWhisker = values.get(upperWhiskerIndex);

		double[] outliers = null;
		int numberOfOutliers = (lowerWhiskerIndex - 1) + (values.size() - upperWhiskerIndex);
		if (numberOfOutliers >= 0) {
			outliers = new double[numberOfOutliers];
			int counter = 0;
			for (int i = 0; i < lowerWhiskerIndex; i++) {
				outliers[counter++] = values.get(i);
			}
			for (int i = upperWhiskerIndex + 1; i < values.size(); i++) {
				outliers[counter++] = values.get(i);
			}
		}
		return new Quartile(median, mean, standardDeviation, lowerQuartile, upperQuartile, lowerWhisker, upperWhisker,
				outliers);
	}

	@Override
	public String toString() {
		return "Quartile (median: " + median + ", lower q: " + lowerQuartile + ", upper q: " + upperQuartile + "lower w: "
				+ lowerWhisker + ", upper w: " + upperWhisker + ", mean: " + mean + ", sd: " + standardDeviation
				+ ", number of outliers: " + outliers.length + ")";
	}
}
