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
package com.rapidminer.example;

import java.util.Arrays;
import java.util.logging.Level;

import com.rapidminer.tools.LogService;


/**
 * Attribute statistics object for nominal attributes.
 *
 * @author Ingo Mierswa
 */
public class NominalStatistics implements Statistics {

	private static final long serialVersionUID = -7644523717916796701L;

	private long mode = -1;

	private long maxCounter = 0;

	private long[] scores;

	public NominalStatistics() {}

	/** Clone constructor. */
	private NominalStatistics(NominalStatistics other) {
		this.mode = other.mode;
		this.maxCounter = other.maxCounter;
		if (other.scores != null) {
			this.scores = Arrays.copyOf(other.scores, other.scores.length);
		}
	}

	/** Returns a clone of this statistics object. The attribute is only cloned by reference. */
	@Override
	public Object clone() {
		return new NominalStatistics(this);
	}

	@Override
	public void startCounting(Attribute attribute) {
		this.scores = new long[attribute.getMapping().size()];
		this.mode = -1;
		this.maxCounter = 0;
	}

	@Override
	public void count(double doubleIndex, double weight) {
		if (!Double.isNaN(doubleIndex)) {
			int index = (int) doubleIndex;
			if (index >= 0) {
				// more values than before? Increase Array size...
				if (index >= scores.length) {
					long[] newScores = new long[index + 1];
					System.arraycopy(scores, 0, newScores, 0, scores.length);
					scores = newScores;
				}
				scores[index]++;
				if (scores[index] > maxCounter) {
					maxCounter = scores[index];
					mode = index;
				}
			}
		}
	}

	@Override
	public boolean handleStatistics(String name) {
		return MODE.equals(name) || COUNT.equals(name) || LEAST.equals(name);
	}

	@Override
	public double getStatistics(Attribute attribute, String name, String parameter) {
		if (MODE.equals(name)) {
			return this.mode;
		} else if (COUNT.equals(name)) {
			if (parameter != null) {
				return getValueCount(attribute, parameter);
			} else {
				// LogService.getGlobal().log("Cannot calculate statistics COUNT for attribute
				// "+attribute.getName()+": no value given...",
				// LogService.WARNING);
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.example.NominalStatistics.calculating_statistics_count_for_attribute_error",
						attribute.getName());
				return Double.NaN;
			}
		}
		if (LEAST.equals(name)) {
			long minCounter = Integer.MAX_VALUE;
			long least = 0;
			for (int i = 0; i < scores.length; i++) {
				if (scores[i] < minCounter) {
					minCounter = scores[i];
					least = i;
				}
			}
			return least;
		} else {
			// LogService.getGlobal().log("Cannot calculate statistics, unknown type: " + name,
			// LogService.WARNING);
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.example.NominalStatistics.calculating_statistics_unknown_type_error", name);
			return Double.NaN;
		}
	}

	private long getValueCount(Attribute attribute, String value) {
		if (attribute != null && attribute.getMapping() != null) {
			int index = attribute.getMapping().getIndex(value);
			if (index < 0) {
				return -1;
			} else {
				return scores[index];
			}
		} else {
			return -1;
		}
	}

	@Override
	public String toString() {
		return "Counts: " + Arrays.toString(scores);
	}
}
