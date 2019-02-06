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

import java.util.Comparator;


/**
 * Helper class for finding thresholds for cost sensitive learning or calculating the AUC
 * performance criterion.
 * 
 * @author Martin Scholz, Ingo Mierswa ingomierswa Exp $
 */
public class WeightedConfidenceAndLabel implements Comparable<WeightedConfidenceAndLabel> {

	public static class WCALComparator implements Comparator<WeightedConfidenceAndLabel> {

		private ROCBias method;

		public WCALComparator(ROCBias method) {
			this.method = method;
		}

		@Override
		public int compare(WeightedConfidenceAndLabel o1, WeightedConfidenceAndLabel o2) {
			int compi = (-1) * Double.compare(o1.confidence, o2.confidence);
			if (compi == 0) {
				switch (method) {
					case OPTIMISTIC:
						return -Double.compare(o1.label, o2.label);
					case PESSIMISTIC:
					case NEUTRAL:
					default:
						return Double.compare(o1.label, o2.label);
				}
			} else {
				return compi;
			}
		}

	}

	private final double confidence, label, prediction;

	private double weight = 1.0d;

	public WeightedConfidenceAndLabel(double confidence, double label, double prediction) {
		this(confidence, label, prediction, 1.0d);
	}

	public WeightedConfidenceAndLabel(double confidence, double label, double prediction, double weight) {
		this.confidence = confidence;
		this.label = label;
		this.prediction = prediction;
		this.weight = weight;
	}

	@Override
	public int compareTo(WeightedConfidenceAndLabel obj) {
		// We need to sort the examples by *decreasing* confidence:
		int compi = -1 * Double.compare(this.confidence, obj.confidence);
		if (compi == 0) {
			return -Double.compare(this.label, obj.label);
		} else {
			return compi;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof WeightedConfidenceAndLabel)) {
			return false;
		} else {
			WeightedConfidenceAndLabel l = (WeightedConfidenceAndLabel) o;
			return this.label == l.label && this.confidence == l.confidence;
		}
	}

	@Override
	public int hashCode() {
		return Double.valueOf(this.label).hashCode() ^ Double.valueOf(this.confidence).hashCode();
	}

	public double getLabel() {
		return this.label;
	}

	public double getPrediction() {
		return this.prediction;
	}

	public double getConfidence() {
		return this.confidence;
	}

	public double getWeight() {
		return weight;
	}

	@Override
	public String toString() {
		return "conf: " + confidence + ", label: " + label + ", weight: " + weight;
	}
}
