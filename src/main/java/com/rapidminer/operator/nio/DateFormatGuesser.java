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
package com.rapidminer.operator.nio;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.rapidminer.core.io.data.DataSet;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetRow;
import com.rapidminer.operator.nio.model.CSVResultSet;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.tools.LogService;

/**
 * The DateFormatGuesser provides a constructor for basic initialization and a count method to read data.
 * The counting will check known dateformats from {@link ParameterTypeDateFormat#PREDEFINED_DATE_FORMATS} and the results
 * are then available in form of methods for the best matching {@link SimpleDateFormat}, the results with confidences and
 * the probably date typable attribute IDs.
 *
 * @author Andreas Timm, Jan Czogalla
 * @since 9.1.0
 */
public class DateFormatGuesser {

	/** keeping the initialized SimpleDateFormats for this instance */
	private SimpleDateFormat[] dateFormats;
	/** counting matches for the different dateFormats */
	private Map<String, double[]> dateFormatMatches = new HashMap<>();
	 /** counting matches for dateFormats for every Attribute */
	private Map<String, int[]> dateFormatAttributeMatchCount = new HashMap<>();
	 /** keeping the bestMatchingFormat until an update happens */
	private String bestMatchingFormat = null;
	 /** amount of attribute, requires this many elements when counting */
	private int attributeCount;
	 /** count of the amount of rows */
	private int countedRows = 0;
	/** preferred date formats */
	private String preferredDateFormat;

	/**
	 * Construct an instance, initializes data structures.
	 *
	 * @param attributeCount
	 * 		amount of attributes that will be handled, not less than 1
	 * @param customFormats
	 * 		optional, can be used to pass additional date format which are not part of {@link
	 * 		ParameterTypeDateFormat#PREDEFINED_DATE_FORMATS}
	 * @param preferredDateFormat a preferred Date Format
	 */
	public DateFormatGuesser(int attributeCount, List<String> customFormats, String preferredDateFormat) {
		if (attributeCount < 1) {
			throw new IllegalArgumentException("Date format guessing requires at least one column");
		}
		Set<String> allFormats = new HashSet<>(customFormats != null ? customFormats : Collections.emptyList());
		allFormats.addAll(Arrays.asList(ParameterTypeDateFormat.PREDEFINED_DATE_FORMATS));
		allFormats.add(preferredDateFormat);
		this.preferredDateFormat = preferredDateFormat;
		Set<SimpleDateFormat> dateFormatList = new LinkedHashSet<>();
		for (String dateFormat : allFormats) {
			if (dateFormat != null && !dateFormat.trim().isEmpty()) {
				try {
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
					simpleDateFormat.setLenient(false);
					dateFormatList.add(simpleDateFormat);
					dateFormatMatches.put(dateFormat, new double[attributeCount]);
					dateFormatAttributeMatchCount.put(dateFormat, new int[attributeCount]);
				} catch (Exception e) {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.import.could_not_create_dateformat", e);
				}
			}
		}
		dateFormats = dateFormatList.toArray(new SimpleDateFormat[0]);
		this.attributeCount = attributeCount;
	}

	/**
	 * Add these entries to the calculation. content.length needs to be equal to attributeCount fed to the constructor.
	 *
	 * @param content
	 * 		elements to be counted, may contain nulls
	 * @throws IllegalArgumentException
	 * 		if the given {@code content} array's length is not equal to the configured {@link #attributeCount}
	 */
	public void count(String[] content) {
		if (content == null) {
			return;
		}
		if (content.length != attributeCount) {
			throw new IllegalArgumentException("Expected " + attributeCount + " elements to count but got " + content.length);
		}
		bestMatchingFormat = null;
		for (int partNr = 0; partNr < content.length; partNr++) {
			String part = content[partNr];
			if (part == null || part.trim().isEmpty()) {
				continue;
			}
			part = part.trim();
			for (SimpleDateFormat dateFormat : dateFormats) {
				ParsePosition pos = new ParsePosition(0);
				if (dateFormat.parse(part, pos) != null) {
					// match both full and partial; weigh by percentage of used pattern
					double weight = (double) pos.getIndex() / part.length();
					dateFormatMatches.get(dateFormat.toPattern())[partNr] += weight;
					dateFormatAttributeMatchCount.get(dateFormat.toPattern())[partNr]++;
				}
			}
		}
		countedRows++;
	}

	/**
	 * After counting from given data, the best matching date format is available. Custom formats given to the constructor
	 * will be preferred.
	 *
	 * @param confidence
	 * 		percentage at which a column is considered a potential date/time column
	 * @return a {@link SimpleDateFormat} with the most matches over the complete previously given data. If no match
	 * could be found, returns {@code null}
	 */
	public SimpleDateFormat getBestMatch(double confidence) {
		if (bestMatchingFormat == null) {
			Map<String, Double> results = getResults(confidence);
			Stream<Entry<String, Double>> resultStream = results.entrySet().stream();
			boolean usePreferredFormat = preferredDateFormat != null;
			if (usePreferredFormat) {
				resultStream = resultStream.filter(e -> preferredDateFormat.equals(e.getKey()));
			}
			Optional<Entry<String, Double>> bestMatch = resultStream.min(Comparator.comparingDouble(e -> -e.getValue()));
			if (!usePreferredFormat) {
				double maxValue = bestMatch.map(Entry::getValue).orElse(0d);
				if (Double.isNaN(maxValue) || maxValue <= 0d) {
					return null;
				}
			}
			bestMatchingFormat = bestMatch.map(Entry::getKey).orElse(null);
		}
		return bestMatchingFormat == null ? null : new SimpleDateFormat(bestMatchingFormat);
	}

	/**
	 * The results contain all known date formats with their performance which is amount of matches of one date format
	 * divided by sum of all date format matches (i.e. maximum number of date/time columns times number of rows counted).
	 *
	 * @param confidence
	 * 		percentage at which a column is considered a potential date/time column
	 * @return the mapping for date format patterns to their performance
	 */
	public Map<String, Double> getResults(double confidence) {
		int[] matchMask = new int[attributeCount];
		// find maximum match count per column
		dateFormatAttributeMatchCount.values().forEach(matches ->
				IntStream.range(0, matchMask.length).filter(i -> matches[i] > matchMask[i]).forEach(i -> matchMask[i] = matches[i]));

		// find potential date/time columns (confidence based) and create a 0/1 mask
		double minimumRows = confidence * countedRows;
		Arrays.setAll(matchMask, i -> matchMask[i] >= minimumRows ? 1 : 0);

		int maxColumns = Arrays.stream(matchMask).sum();
		// calculate matching percentage; weighted sum over all potential date/time columns
		return dateFormatMatches.entrySet().stream().collect(Collectors.toMap(Entry::getKey,
				e -> dot(e.getValue(), matchMask) / (maxColumns * countedRows)));
	}

	/**
	 * The different attributes may fit better or worse for one date format. The attribute IDs which have at least the
	 * given confidence matching the pattern will be returned. The confidence is compared to the amount of matches of
	 * the attribute divided by the amount of rows.
	 *
	 * @param pattern
	 * 		one of the known date formats
	 * @param confidence
	 * 		number between 0.0 and 1.0 (inclusive) to get reasonable results
	 * @return the attribute indices that are probably of the type date. Based on the number of attribute fed to the
	 * constructor and the string array fed to {@link #count(String[])}
	 */
	public List<Integer> getDateAttributes(String pattern, double confidence) {
		final int[] matches = dateFormatAttributeMatchCount.get(pattern);
		if (matches == null) {
			return Collections.emptyList();
		}
		double minimumRows = confidence * countedRows;
		return IntStream.range(0, matches.length).filter(i -> matches[i] >= minimumRows).boxed().collect(Collectors.toList());
	}

	/**
	 * This factory method runs a data guessing procedure for the given {@link DataSet}.
	 * The data will be checked for usual {@link DateFormat DateFormats}.
	 * The result can afterwards be collected from the returned {@link DateFormatGuesser}.
	 *
	 * @param data
	 * 		the data set that may contain dates
	 * @return a {@link DateFormatGuesser} with preprocessed results.
	 * @throws DataSetException if an error occurs when accessing the data set
	 * @see ParameterTypeDateFormat#PREDEFINED_DATE_FORMATS
	 */
	public static DateFormatGuesser guessDateFormat(DataSet data, List<String> existingDateFormats, String preferredDateFormat) throws DataSetException {
		if (data == null) {
			return null;
		}
		DateFormatGuesser dfg = new DateFormatGuesser(data.getNumberOfColumns(), existingDateFormats, preferredDateFormat);
		data.reset();
		int lineCount = 0;
		while (data.hasNext() && lineCount < CSVResultSet.LINES_FOR_GUESSING) {
			final DataSetRow dataSetRow = data.nextRow();
			String[] row = new String[data.getNumberOfColumns()];
			for (int i = 0; i < data.getNumberOfColumns(); i++) {
				try {
					row[i] = dataSetRow.getString(i);
				} catch (com.rapidminer.core.io.data.ParseException e) {
					LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.import.could_not_read_from_dataset", e);
				}
			}
			dfg.count(row);
			lineCount++;
		}
		return dfg;
	}

	/** Create dot product of the given arguments. The arrays must be the same length. */
	private static double dot(double[] values, int[] matchMask) {
		return IntStream.range(0, matchMask.length).mapToDouble(i -> matchMask[i] * values[i]).sum();
	}
}
