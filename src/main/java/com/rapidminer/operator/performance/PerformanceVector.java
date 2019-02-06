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

import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.Averagable;
import com.rapidminer.tools.math.AverageVector;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;


/**
 * Handles several performance criteria. It is possible to obtain more than one criterion and
 * therefore they are added to a criteria list.
 * 
 * @author Ingo Mierswa, Simon Fischer Exp $
 */
public class PerformanceVector extends AverageVector {

	private static final long serialVersionUID = 3123587140049371098L;

	public static final String MAIN_CRITERION_FIRST = "first";

	/**
	 * The default performance comparator compares the main criterion of two performance vectors. In
	 * case that the minimum description length (mdl) criterion is also calculated and have a weight
	 * &gt; 0 the weighted sums of the main and the mdl criterion are compared.
	 */
	public static class DefaultComparator implements PerformanceComparator {

		private static final long serialVersionUID = 8632060851821885142L;

		public DefaultComparator() {}

		@Override
		public int compare(PerformanceVector av1, PerformanceVector av2) {
			return av1.getMainCriterion().compareTo(av2.getMainCriterion());
		}
	}

	/**
	 * This value map will only be intialized before writing this vector to a file. This allows a
	 * quick human readable format in the resulting file.
	 */
	private Map<String, Double> currentValues = null;

	/** Used to compare two average vectors. */
	private PerformanceComparator comparator = new DefaultComparator();

	/** Name of the main criterion. */
	private String mainCriterion = null;

	public void setComparator(PerformanceComparator comparator) {
		this.comparator = comparator;
	}

	public void addCriterion(PerformanceCriterion crit) {
		PerformanceCriterion pc = getCriterion(crit.getName());
		if (pc != null) {
			removeAveragable(pc);
			// LogService.getGlobal().log("Performance criterion '" + crit.getName() +
			// "' was already part of performance vector. Overwritten...",
			// LogService.WARNING);
			LogService
					.getRoot()
					.log(Level.WARNING,
							"com.rapidminer.operator.performance.PerformanceVector.performance_criterion_already_part_of_performance_vector",
							crit.getName());
		}
		addAveragable(crit);
	}

	public PerformanceCriterion getCriterion(int index) {
		return (PerformanceCriterion) getAveragable(index);
	}

	public PerformanceCriterion getCriterion(String name) {
		return (PerformanceCriterion) getAveragable(name);
	}

	public String[] getCriteriaNames() {
		String[] criteriaNames = new String[getSize()];
		for (int i = 0; i < criteriaNames.length; i++) {
			criteriaNames[i] = getCriterion(i).getName();
		}
		return criteriaNames;
	}

	/**
	 * Sets the name of the main average (must be added by {@link #addAveragable(Averagable)})
	 */
	public void setMainCriterionName(String mcName) {
		if ((!mcName.equals(MAIN_CRITERION_FIRST)) && (getAveragable(mcName) == null)) {
			// LogService.getGlobal().log("Main criterion not found: '" + mcName + "'.",
			// LogService.ERROR);
			LogService.getRoot().log(Level.SEVERE,
					"com.rapidminer.operator.performance.PerformanceVector.main_criterion_not_found", mcName);
		}
		this.mainCriterion = mcName;
	}

	/**
	 * Returns the main {@link PerformanceCriterion}. If the main criterion is not specified by
	 * {@link #setMainCriterionName(String)}, the first criterion is returned.
	 */
	public PerformanceCriterion getMainCriterion() {
		if (mainCriterion == null) {
			return (PerformanceCriterion) getAveragable(0);
		} else {
			PerformanceCriterion pc = (PerformanceCriterion) getAveragable(mainCriterion);
			if (pc == null) {
				return (PerformanceCriterion) getAveragable(0);
			}
			return pc;
		}
	}

	/** Returns a negative value iff o is better than this performance vector */
	@Override
	public int compareTo(Object o) {
		double result = comparator.compare(this, (PerformanceVector) o);
		if (result < 0.0) {
			return -1;
		} else if (result > 0.0) {
			return +1;
		} else {
			return 0;
		}
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		PerformanceVector av = new PerformanceVector();
		for (int i = 0; i < size(); i++) {
			Averagable avg = getAveragable(i);
			av.addAveragable((Averagable) (avg).clone());
		}
		av.cloneAnnotationsFrom(this);
		return av;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(Tools.getLineSeparator() + "PerformanceVector [");
		for (int i = 0; i < size(); i++) {
			Averagable avg = getAveragable(i);
			if ((mainCriterion != null) && (avg.getName().equals(mainCriterion))) {
				result.append(Tools.getLineSeparator() + "*****");
			} else {
				result.append(Tools.getLineSeparator() + "-----");
			}
			result.append(avg);
		}
		result.append(Tools.getLineSeparator() + "]");
		return result.toString();
	}

	public String getExtension() {
		return "per";
	}

	public String getFileDescription() {
		return "performance vector file";
	}

	/** Init the value map which ensures an easy human readable format. */
	@Override
	public void initWriting() {
		this.currentValues = new HashMap<String, Double>();
		for (int i = 0; i < size(); i++) {
			Averagable averagable = getAveragable(i);
			this.currentValues.put(averagable.getName(), averagable.getAverage());
		}
	}
}
