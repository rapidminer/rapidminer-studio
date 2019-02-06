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
package com.rapidminer.gui.new_plotter.utility;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.tools.container.Pair;

import java.awt.Color;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Static only class with some useful helper functions to modify different kinds of data structures.
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class DataStructureUtils {

	/**
	 * A comparator which compares Pairs based on the second part of the pair.
	 * 
	 * @author Marius Helf
	 */
	public static class PairComparator<U, T extends Comparable<T>> implements Comparator<Pair<U, T>> {

		boolean ascending;

		public PairComparator(boolean ascending) {
			super();
			this.ascending = ascending;
		}

		@Override
		public int compare(Pair<U, T> o1, Pair<U, T> o2) {
			if (ascending) {
				return o1.getSecond().compareTo(o2.getSecond());
			} else {
				return -o1.getSecond().compareTo(o2.getSecond());
			}
		}
	}

	/**
	 * Clones a map (the returned map is always an instance of HashMap).
	 */
	public static <K, V> Map<K, V> getMapClone(Map<K, V> map) {
		Map<K, V> clone = new HashMap<K, V>();
		clone.putAll(map);
		return clone;
	}

	/**
	 * Returns the old color with new alpha value.
	 */
	public static Color setColorAlpha(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	/**
	 * Multiplies two opacities with range [0,255]. First maps both values to [0,1], then calculates
	 * the product and maps the result back to [0,255].
	 */
	public static int multiplyOpacities256(int opacity1, int opacity2) {
		double op1 = opacity1 / 255.0;
		double op2 = opacity2 / 255.0;
		double resultOpacity = op1 * op2;
		return (int) Math.round(resultOpacity * 255.0);
	}

	public static List<Double> getAllDataTableValues(DataTable table, int columnIdx) {
		List<Double> values = new LinkedList<Double>();
		for (DataTableRow row : table) {
			values.add(row.getValue(columnIdx));
		}
		return values;
	}

	public static <T> Set<T> getDistinctValues(T[] array) {
		Set<T> resultSet = new HashSet<T>();
		for (int i = 0; i < array.length; ++i) {
			resultSet.add(array[i]);
		}
		return resultSet;
	}

	public static <T> Set<T> getDistinctValues(Iterable<T> iterable) {
		Set<T> resultSet = new HashSet<T>();
		for (T value : iterable) {
			resultSet.add(value);
		}
		return resultSet;
	}

	/**
	 * Returns all distinct values in one column of the table in the order of their appearance in
	 * the table.
	 */
	public static List<Double> getDistinctDataTableValues(DataTable table, int columnIdx) {
		LinkedHashSet<Double> values = new LinkedHashSet<Double>();
		for (DataTableRow row : table) {
			double value = row.getValue(columnIdx);
			values.add(value);
		}

		List<Double> resultList = new LinkedList<Double>();
		for (Double value : values) {
			resultList.add(value);
		}
		return resultList;
	}

	public static float[] cloneAndMultiplyArray(float[] array, float factor) {
		float[] result = new float[array.length];
		for (int i = 0; i < array.length; ++i) {
			result[i] = array[i] * factor;
		}
		return result;
	}

	/**
	 * Calculates the optimal power of 10 for rounding. It is calculated based on min and max value.
	 * 
	 * If one of min, max or value is inifinity or NaN, or if min==max, then Integer.MAX_VALUE is
	 * returned.
	 */
	public static int getOptimalPrecision(double min, double max) {
		if (Double.isInfinite(min) || Double.isInfinite(max)) {
			return Integer.MAX_VALUE;
		}
		if (Double.isNaN(min) || Double.isNaN(max)) {
			return Integer.MAX_VALUE;
		}
		double range = max - min;
		if (range == 0) {
			return Integer.MAX_VALUE;
		}

		range = Math.abs(range);
		int powerOf10;
		if (range < 1) {
			powerOf10 = (int) (Math.floor((Math.log10(range))));
		} else {
			powerOf10 = (int) (Math.floor((Math.log10(range)))) - 1;
		}
		return powerOf10;
	}

	public static String getRoundedString(double value, int powerOf10) {
		StringBuilder builder = new StringBuilder();
		if (powerOf10 < Integer.MAX_VALUE - 100) {
			value = roundToPowerOf10(value, powerOf10);
		}
		Formatter formatter = new Formatter(builder, Locale.getDefault());
		String format;
		if (powerOf10 < 0) {
			format = "%." + -powerOf10 + "f";
		} else {
			format = "%.0f";
		}
		formatter.format(format, value);
		formatter.close();
		return builder.toString();
	}

	/**
	 * Rounds value. The precision is calculated based on min and max value. With precisionModifier
	 * the estimated best precision can be increased (positive values for precisionModifier) or
	 * decreased.
	 * 
	 * preDecimalRound is currently unused.
	 * 
	 * If one of min, max or value is inifinity or NaN, or if min==max, then value is returned
	 * unmodified.
	 */
	public static double intelligentRound(double min, double max, double value, int precisionModifier,
			boolean preDecimalRound) {
		int powerOf10 = getOptimalPrecision(min, max);

		if (powerOf10 == Integer.MAX_VALUE) {
			return value;
		}

		return roundToPowerOf10(value, powerOf10 + precisionModifier);
	}

	public static double roundToPowerOf10(double value, int powerOf10) {
		if (Double.isInfinite(value) || Double.isNaN(value)) {
			return value;
		}

		value *= Math.pow(10, -powerOf10);
		value = Math.round(value);
		value /= Math.pow(10, -powerOf10);
		return value;
	}

	public static boolean greaterOrAlmostEqual(float a, float b, float maxRelativeError) {
		return almostEqual(a, b, maxRelativeError) ? true : (a > b);
	}

	public static boolean greaterOrAlmostEqual(double a, double b, double maxRelativeError) {
		return almostEqual(a, b, maxRelativeError) ? true : (a > b);
	}

	public static boolean almostEqual(float a, float b, float maxRelativeError) {

		if (Float.isNaN(a) || Float.isNaN(b)) {
			return false;
		}

		if (a == b) {
			return true;
		}

		float relativeError;
		if (Math.abs(b) > Math.abs(a)) {
			relativeError = Math.abs((a - b) / b);
		} else {
			relativeError = Math.abs((a - b) / a);
		}
		if (relativeError <= maxRelativeError) {
			return true;
		}
		return false;

	}

	public static boolean almostEqual(double a, double b, double maxRelativeError) {

		if (Double.isNaN(a) || Double.isNaN(b)) {
			return false;
		}

		if (a == b) {
			return true;
		}

		double relativeError;
		if (Math.abs(b) > Math.abs(a)) {
			relativeError = Math.abs((a - b) / b);
		} else {
			relativeError = Math.abs((a - b) / a);
		}
		if (relativeError <= maxRelativeError) {
			return true;
		}
		return false;

	}

	// public static String getOptimalDateString(Set<Double> values, DateFormat dateFormat) {
	//
	// List<String> sortedDateStrings = new LinkedList<String>();
	// sortedDateStrings.add(e)
	//
	// List<Double> sortedDateValues = new LinkedList<Double>(values);
	// Collections.sort(sortedDateValues);
	//
	// }

}
