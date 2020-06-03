/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.storage.hdf5;

import static com.rapidminer.storage.hdf5.ExampleSetHdf5Writer.MILLISECONDS_PER_SECOND;
import static com.rapidminer.storage.hdf5.ExampleSetHdf5Writer.NANOS_PER_MILLISECOND;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.hdf5.file.ColumnInfo;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MDReal;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.container.Range;

import io.jhdf.api.Dataset;


/**
 * Utility class for statistics writing and reading for hdf5 files. Writes and reads different statistical
 * hdf5-attributes.
 * <p>
 * {@link #STATISTICS_MISSING} is the number of missing values in a column,
 * {@link #STATISTICS_MODE} is the category index of the most often appearing nominal value (only for nominal columns),
 * in case of String columns without a dictionary this can be the row index instead,
 * {@link #STATISTICS_LEAST} is the category index of the least often appearing nominal value (only for nominal
 * columns),
 * in case of String columns without a dictionary this can be the row index instead,
 * {@link #STATISTICS_MIN} is the minimal value in the column (only for numeric or date-time columns), as double
 * value for numeric, long value for date-time with only second-precision, otherwise array of long values containing
 * seconds and nanoseconds
 * {@link #STATISTICS_MIN} is the maximal value in the column (only for numeric or date-time columns), as double
 * value for numeric, long value for date-time with only second-precision, otherwise array of long values containing
 * seconds and nanoseconds
 * {@link #STATISTICS_MEAN} is average value in the column (only for numeric columns), as double value
 *
 * @author  Gisa Meier
 * @since 9.7.0
 */
enum StatisticsHandler {

	;//No-instance enum, only static methods

	static final String STATISTICS_MODE = "statistics:mode";
	private static final String STATISTICS_MISSING = "statistics:missing";
	private static final String STATISTICS_LEAST = "statistics:least";
	private static final String STATISTICS_MIN = "statistics:min";
	private static final String STATISTICS_MAX = "statistics:max";
	private static final String STATISTICS_MEAN = "statistics:mean";


	/**
	 * Adds statistics from the {@link ExampleSet} to the {@link ColumnInfo} for the given attribute.
	 *
	 * @param info
	 * 		the column info to add to
	 * @param attribute
	 * 		the attribute for which to add the info
	 * @param statisticsProvider
	 * 		the example set providing the statistics
	 */
	static void addStatistics(ColumnInfo info, Attribute attribute, ExampleSet statisticsProvider) {
		double unknownStatistics = statisticsProvider.getStatistics(attribute, Statistics.UNKNOWN);
		if (!Double.isNaN(unknownStatistics)) {
			info.addAdditionalAttribute(STATISTICS_MISSING, int.class, (int) unknownStatistics);
		}
		if (attribute.isNominal()) {
			addNominalStatistics(info, attribute, statisticsProvider);
		} else if (attribute.isDateTime()) {
			addDateTimeStatistics(info, attribute, statisticsProvider);
		} else if (attribute.isNumerical()) {
			addNumericStatistics(info, attribute, statisticsProvider);
		}
	}

	/**
	 * Adds statistics from the {@link AttributeMetaData} to the {@link ColumnInfo}.
	 *
	 * @param info
	 * 		the column info to add to
	 * @param amd
	 * 		the attribute for which to add the info
	 */
	static void addStatistics(ColumnInfo info, AttributeMetaData amd) {
		Integer unknownStatistics = amd.getNumberOfMissingValues().getNumber();
		if (unknownStatistics != null) {
			info.addAdditionalAttribute(STATISTICS_MISSING, int.class, (int) unknownStatistics);
		}
		if (amd.isNominal()) {
			addNominalStatistics(info, amd);
		} else if (amd.isDateTime()) {
			addDateTimeStatistics(info, amd);
		} else if (amd.isNumerical()) {
			addNumericStatistics(info, amd);
		}
	}

	/**
	 * Reads statistics for the set into the attribute.
	 *
	 * @param set
	 * 		the set with the statistics attributes to read
	 * @param attributeMetaData
	 * 		the meta data to store the statistics in
	 */
	static void readStatistics(Dataset set, AttributeMetaData attributeMetaData) {
		io.jhdf.api.Attribute missings = set.getAttribute(STATISTICS_MISSING);
		MDInteger missingMD = new MDInteger();
		missingMD.setUnkown();
		if (missings != null) {
			Object data = missings.getData();
			if (data instanceof Number) {
				missingMD = new MDInteger(((Number) data).intValue());
			}
		}
		attributeMetaData.setNumberOfMissingValues(missingMD);
		if (attributeMetaData.isNumerical()) {
			readNumericStatistics(set, attributeMetaData);
		} else if (attributeMetaData.isDateTime()) {
			readDateTimeStatistics(set, attributeMetaData);
		}
	}

	/**
	 * Reads the mode from the {@link #STATISTICS_MODE} attribute of the set.
	 *
	 * @param set
	 * 		the set with the mode attribute
	 * @return the value of the mode attribute or {@code -1} if there is none or it is not a number
	 */
	static int readModeIndex(Dataset set) {
		io.jhdf.api.Attribute mode = set.getAttribute(STATISTICS_MODE);
		if (mode != null) {
			Object data = mode.getData();
			if (data instanceof Number) {
				return ((Number) data).intValue();
			}
		}
		return -1;
	}

	/**
	 * Adds the numerical statistics (min/max/mean) for the {@link Attribute} to the info.
	 */
	private static void addNumericStatistics(ColumnInfo info, Attribute attribute, ExampleSet statisticsProvider) {
		double statisticsMin = statisticsProvider.getStatistics(attribute, Statistics.MINIMUM);
		double statisticsMax = statisticsProvider.getStatistics(attribute, Statistics.MAXIMUM);
		double statisticsMean = statisticsProvider.getStatistics(attribute, Statistics.AVERAGE);
		addNumericStatistics(info, statisticsMin, statisticsMax, statisticsMean);
	}

	/**
	 * Adds the numerical statistics (min/max/mean) for the {@link AttributeMetaData} to the info.
	 */
	private static void addNumericStatistics(ColumnInfo info, AttributeMetaData amd) {
		double statisticsMin = Double.NaN;
		double statisticsMax = Double.NaN;
		if (amd.getValueSetRelation() != SetRelation.UNKNOWN) {
			Range valueRange = amd.getValueRange();
			statisticsMin = valueRange.getLower();
			statisticsMax = valueRange.getUpper();
		}
		double statisticsMean = Optional.ofNullable(amd.getMean().getNumber()).orElse(Double.NaN);
		addNumericStatistics(info, statisticsMin, statisticsMax, statisticsMean);
	}

	/**
	 * Adds the numerical statistics (min/max/mean) for the attribute to the info.
	 */
	private static void addNumericStatistics(ColumnInfo info, double statisticsMin, double statisticsMax, double statisticsMean) {
		// ignore nonsense values
		if (statisticsMin <= statisticsMax) {
			info.addAdditionalAttribute(STATISTICS_MIN, double.class, statisticsMin);
			info.addAdditionalAttribute(STATISTICS_MAX, double.class, statisticsMax);
		}
		if (!Double.isNaN(statisticsMean)) {
			info.addAdditionalAttribute(STATISTICS_MEAN, double.class, statisticsMean);
		}
	}

	/**
	 * Adds the date-time statistics (min/max) for the {@link Attribute} to the info.
	 */
	private static void addDateTimeStatistics(ColumnInfo info, Attribute attribute, ExampleSet statisticsProvider) {
		double statisticsMin = statisticsProvider.getStatistics(attribute, Statistics.MINIMUM);
		double statisticsMax = statisticsProvider.getStatistics(attribute, Statistics.MAXIMUM);
		addDateTimeStatistics(info, statisticsMin, statisticsMax, attribute.getValueType());
	}

	/**
	 * Adds the date-time statistics (min/max) for the {@link AttributeMetaData} to the info.
	 */
	private static void addDateTimeStatistics(ColumnInfo info, AttributeMetaData amd) {
		double statisticsMin = Double.NaN;
		double statisticsMax = Double.NaN;
		if (amd.getValueSetRelation() != SetRelation.UNKNOWN) {
			Range valueRange = amd.getValueRange();
			statisticsMin = valueRange.getLower();
			statisticsMax = valueRange.getUpper();
		}
		addDateTimeStatistics(info, statisticsMin, statisticsMax, amd.getValueType());
	}

	/**
	 * Adds the date-time statistics (min/max) for the attribute to the info.
	 */
	private static void addDateTimeStatistics(ColumnInfo info, double statisticsMin, double statisticsMax, int valueType) {
		// ignore nonsense values and infinite dates
		if (statisticsMin <= statisticsMax && Double.isFinite(statisticsMin) && Double.isFinite(statisticsMax)) {
			if (valueType == Ontology.DATE) {
				info.addAdditionalAttribute(STATISTICS_MIN, long.class,
						(long) (statisticsMin / MILLISECONDS_PER_SECOND));
				info.addAdditionalAttribute(STATISTICS_MAX, long.class,
						(long) (statisticsMax / MILLISECONDS_PER_SECOND));
			} else {
				info.addAdditionalAttribute(STATISTICS_MIN, long[].class,
						new long[]{(long) (statisticsMin / MILLISECONDS_PER_SECOND),
								(long) (statisticsMin % MILLISECONDS_PER_SECOND * NANOS_PER_MILLISECOND)});
				info.addAdditionalAttribute(STATISTICS_MAX, long[].class,
						new long[]{(long) (statisticsMax / MILLISECONDS_PER_SECOND),
								(long) (statisticsMax % MILLISECONDS_PER_SECOND * NANOS_PER_MILLISECOND)});
			}
		}
	}

	/**
	 * Adds the nominal statistics (mode/least) for the {@link Attribute} to the info.
	 */
	private static void addNominalStatistics(ColumnInfo info, Attribute attribute, ExampleSet statisticsProvider) {
		double statisticsMode = statisticsProvider.getStatistics(attribute, Statistics.MODE);
		if (statisticsMode >= 0) {
			// write statistics only if it was calculated (otherwise NaN or -1)
			// category index +1 because of shift of category indices in hdf5-format
			info.addAdditionalAttribute(STATISTICS_MODE, int.class, (int) statisticsMode + 1);
		}
		double statisticsLeast = statisticsProvider.getStatistics(attribute, Statistics.LEAST);
		if (statisticsLeast > 0 || statisticsLeast == 0 && statisticsMode != -1) {
			// write statistics only if it was calculated (otherwise NaN or 0)
			// category index +1 because of shift of category indices in hdf5-format
			info.addAdditionalAttribute(STATISTICS_LEAST, int.class, (int) statisticsLeast + 1);
		}
	}

	/**
	 * Adds the nominal statistics (mode) for the {@link AttributeMetaData} to the info.
	 */
	private static void addNominalStatistics(ColumnInfo info, AttributeMetaData amd) {
		String mode = amd.getMode();
		if (mode != null) {
			Set<String> valueSet = amd.getValueSet();
			Iterator<String> iterator = valueSet.iterator();
			int modeIndex = 0;
			boolean found = false;
			while (iterator.hasNext()) {
				String value = iterator.next();
				modeIndex++;
				if (mode.equals(value)) {
					found = true;
					break;
				}
			}
			info.addAdditionalAttribute(STATISTICS_MODE, int.class, modeIndex + (found ? 0 : 1));
		}
	}

	/**
	 * Reads the numeric statistics (min/max/mean) from the attributes of the set and adds them to the meta data.
	 */
	private static void readNumericStatistics(Dataset set, AttributeMetaData attributeMetaData) {
		io.jhdf.api.Attribute min = set.getAttribute(STATISTICS_MIN);
		io.jhdf.api.Attribute max = set.getAttribute(STATISTICS_MAX);
		if (min != null && max != null) {
			Object minData = min.getData();
			Object maxData = max.getData();
			if (minData instanceof Number && maxData instanceof Number) {
				SetRelation relation = attributeMetaData.getValueSetRelation();
				if (relation == SetRelation.UNKNOWN) {
					// if the set relation is unknown at this point, this might just be wrongly configured/missing
					// an attribute cannot have a range AND be an unknown relation
					relation = SetRelation.EQUAL;
				}
				attributeMetaData.setValueRange(new Range(((Number) min.getData()).doubleValue(),
						((Number) max.getData()).doubleValue()), relation);
			}
		}
		io.jhdf.api.Attribute mean = set.getAttribute(STATISTICS_MEAN);
		MDReal meanMD = new MDReal();
		meanMD.setUnkown();
		if (mean != null) {
			Object meanObject = mean.getData();
			if (meanObject instanceof Number) {
				meanMD = new MDReal(((Number) mean.getData()).doubleValue());
			}
		}
		attributeMetaData.setMean(meanMD);
	}

	/**
	 * Reads the date-time statistics (min/max) from the attributes of the set and adds them to the meta data.
	 */
	private static void readDateTimeStatistics(Dataset set, AttributeMetaData attribute) {
		io.jhdf.api.Attribute min = set.getAttribute(STATISTICS_MIN);
		io.jhdf.api.Attribute max = set.getAttribute(STATISTICS_MAX);
		if (min != null && max != null) {
			Object minData = min.getData();
			Object maxData = max.getData();
			if (minData instanceof long[] && maxData instanceof long[]) {
				long[] mins = (long[]) minData;
				long[] maxs = (long[]) maxData;
				if (mins.length == 2 && maxs.length == 2) {
					long minValue = mins[0] * MILLISECONDS_PER_SECOND + mins[1] / NANOS_PER_MILLISECOND;
					long maxValue = maxs[0] * MILLISECONDS_PER_SECOND + maxs[1] / NANOS_PER_MILLISECOND;
					attribute.setValueRange(new Range(minValue, maxValue), SetRelation.EQUAL);
				}
			} else if (minData instanceof Number && maxData instanceof Number) {
				attribute.setValueRange(new Range(((Number) minData).longValue() * MILLISECONDS_PER_SECOND,
						((Number) maxData).longValue() * MILLISECONDS_PER_SECOND), SetRelation.EQUAL);
			}
		}
	}

}
