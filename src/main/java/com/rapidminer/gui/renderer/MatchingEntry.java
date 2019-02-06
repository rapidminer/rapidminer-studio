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
package com.rapidminer.gui.renderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;


/**
 * An entry which stores a match score how well this entry matches.
 *
 * @author Andreas Timm, Marco Böck, Jan Czogalla
 * @since 9.1.0
 */
public class MatchingEntry implements Comparable<MatchingEntry> {

	private String entryName;
	private double match;


	/**
	 * Creates a new data match element.
	 *
	 * @param entryName
	 * 		the name of the data
	 * @param match
	 * 		the match score
	 */
	private MatchingEntry(String entryName, double match) {
		this.entryName = entryName;
		this.match = match;
	}

	/**
	 * Returns the name.
	 *
	 * @return the name
	 */
	public String getEntryName() {
		return entryName;
	}

	/**
	 * Returns the match score.
	 *
	 * @return the match score
	 */
	public double getMatch() {
		return this.match;
	}

	/**
	 * Compares this element to a different one.  Returns the result of Double.compare(o.match, this.match).
	 * Unknown matches will be sorted to the end.
	 *
	 * @param o
	 * 		the other
	 * @return the result of Double.compare(o.match, this.match)
	 */
	@Override
	public int compareTo(MatchingEntry o) {
		return Double.compare(-match, -o.match);
	}

	/**
	 * Change the match value.
	 *
	 * @param match
	 * 		a double value between 0.0 and 1.0
	 */
	public void setMatch(double match) {
		this.match = match;
	}

	@Override
	public String toString() {
		return entryName;
	}

	/**
	 * Factory method to create a single {@link MatchingEntry}. Will return {@code null} if the {@code entryName} is {@code null}.
	 */
	public static MatchingEntry create(String entryName, double match) {
		if (entryName == null) {
			return null;
		}
		return new MatchingEntry(entryName, match);
	}

	/**
	 * Factory method to create a bunch of {@link MatchingEntry} elements.
	 *
	 * @param entryValueMap
	 * 		a String mapping to prediction values
	 * @return the sorted {@link MatchingEntry} array
	 */
	public static MatchingEntry[] create(Map<String, Double> entryValueMap) {
		if (entryValueMap == null || entryValueMap.isEmpty()) {
			return new MatchingEntry[0];
		}
		List<MatchingEntry> result = new ArrayList<>();
		entryValueMap.forEach((key, value) -> result.add(new MatchingEntry(key, value)));
		Collections.sort(result);
		return result.toArray(new MatchingEntry[0]);
	}

	/**
	 * A factory method to create elements without a prediction match value of this kind.
	 *
	 * @param values
	 * 		the Strings that are available but have no prediction
	 * @return an array of {@link MatchingEntry}
	 */
	public static MatchingEntry[] createArray(String[] values) {
		if (values == null) {
			return new MatchingEntry[0];
		}
		MatchingEntry[] result = new MatchingEntry[values.length];
		Arrays.setAll(result, i -> new MatchingEntry(values[i], 0));
		return result;
	}

	/**
	 * A factory method to create elements without a prediction match value of this kind.
	 *
	 * @param values
	 * 		the Strings that are available but have no prediction
	 * @return a vector of {@link MatchingEntry}
	 */
	public static Vector<MatchingEntry> createVector(String[] values) {
		return new Vector<>(Arrays.asList(createArray(values)));
	}
}
