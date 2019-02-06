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
package com.rapidminer.gui.new_plotter.configuration;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.new_plotter.ChartConfigurationException;
import com.rapidminer.gui.new_plotter.listener.events.ValueGroupingChangeEvent;
import com.rapidminer.gui.new_plotter.utility.NumericalValueRange;
import com.rapidminer.gui.new_plotter.utility.ValueRange;

import java.text.DateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;


/**
 * This grouping creates a fixed number of bins. All bins contain the same number of examples. That
 * implies that in general the width of the bins is not equal.
 * 
 * Can currently only handle numerical values.
 * 
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class EqualDataFractionGrouping extends AbstractValueGrouping {

	private int binCount;
	private final GroupingType type = GroupingType.EQUAL_DATA_FRACTION;
	private Integer distinctValueCount = Integer.MAX_VALUE;

	/**
	 * Creates a new {@link EqualDataFractionGrouping}.
	 * 
	 * @param dataTableColumn
	 *            the data table column for which the grouping will be created
	 * @param binCount
	 *            the number of bins in this grouping
	 * @param categorical
	 *            indicates if this grouping creates categorical groups
	 * @param dateFormat
	 *            the format to be used to format dates (if dataTableColumn is a date)
	 * @throws ChartConfigurationException
	 *             if the data table column is nominal (not supported by this grouping)
	 */
	public EqualDataFractionGrouping(DataTableColumn dataTableColumn, int binCount, boolean categorical,
			DateFormat dateFormat) throws ChartConfigurationException {
		super(dataTableColumn, categorical, dateFormat);
		if (dataTableColumn.isNominal()) {
			throw new ChartConfigurationException("grouping.illegal_column_type", getGroupingType().getName(),
					dataTableColumn.getName(), dataTableColumn.getValueType(), "numerical or date.");
		}
		this.binCount = binCount;
	}

	/**
	 * Copy constructor
	 */
	protected EqualDataFractionGrouping(EqualDataFractionGrouping other) {
		super(other.getDataTableColumn(), other.isCategorical(), other.getDateFormat());
		this.forceDataTableColumn(other.getDataTableColumn());
		this.binCount = other.binCount;
	}

	public int getBinCount() {
		return binCount;
	}

	public void setBinCount(int binCount) {
		if (binCount != this.binCount) {
			if (binCount < distinctValueCount) {
				this.binCount = binCount;
			} else {
				this.binCount = distinctValueCount;
			}
			// invalidateCache();
			fireGroupingChanged(new ValueGroupingChangeEvent(this, this.binCount));
		}
	}

	@Override
	protected List<ValueRange> createGroupingModel(DataTable dataTable, double upperBound, double lowerBound) {
		int columnIdx = DataTableColumn.getColumnIndex(dataTable, getDataTableColumn());
		Map<Double, Integer> distinctValueCountMap = new HashMap<Double, Integer>();
		Vector<Double> sortedDistinctValueList = new Vector<Double>();
		int valueCount = 0;
		for (DataTableRow row : dataTable) {
			Double value = row.getValue(columnIdx);
			if (!Double.isNaN(value) && value >= lowerBound && value <= upperBound) {
				Integer currentCount = distinctValueCountMap.get(value);
				if (currentCount == null) {
					distinctValueCountMap.put(value, 1);
					sortedDistinctValueList.add(value);
				} else {
					distinctValueCountMap.put(value, (currentCount + 1));
				}
				++valueCount;
			}
		}

		Collections.sort(sortedDistinctValueList);

		// calculate max bin count
		distinctValueCount = distinctValueCountMap.keySet().size();

		List<ValueRange> valueGroups = new LinkedList<ValueRange>();

		if (sortedDistinctValueList.size() == 0) {
			return valueGroups;
		}

		// check if bin count is lower then max bin count
		if (binCount > distinctValueCount) {
			setBinCount(distinctValueCount);
		}

		boolean columnIsDate = dataTable.isDateTime(columnIdx);

		double averageBinSize = valueCount / (double) binCount;

		int currentUpperIdx = 0;

		int valuesUsed = 0;

		lowerBound = sortedDistinctValueList.get(0);
		upperBound = 0;

		// start iterating over data
		for (int binIdx = 1; binIdx <= binCount; ++binIdx) {

			// calculate values per next bin count
			int aimedValueCountForCurrentBin = ((int) Math.round((binIdx) * averageBinSize)) - valuesUsed;

			if (aimedValueCountForCurrentBin < 1) {
				aimedValueCountForCurrentBin = 1;
			}

			// number of bins we have to create after the current one
			int remainingBins = binCount - binIdx;

			// number of values without adding a new distinct value
			int valueCountInBin = distinctValueCountMap.get(sortedDistinctValueList.get(currentUpperIdx));
			int nextValueCount = 0;

			// dvIdx is the last idx which will be included in the range
			for (int dvIdx = currentUpperIdx + 1; dvIdx < distinctValueCount; ++dvIdx) {
				nextValueCount = valueCountInBin + distinctValueCountMap.get(sortedDistinctValueList.get(dvIdx));

				// number of remaining distinct values, if we included dvIdx in current bin
				int remainingDistinctValues = distinctValueCount - dvIdx - 1;
				boolean enoughRemainingDistinctValues = remainingDistinctValues >= remainingBins;

				if (nextValueCount >= aimedValueCountForCurrentBin || !enoughRemainingDistinctValues) {
					double currentDifferenceFromAverage = Math.abs(aimedValueCountForCurrentBin - valueCountInBin);
					double nextDifferenceFromAverage = Math.abs(aimedValueCountForCurrentBin - nextValueCount);

					if ((currentDifferenceFromAverage < nextDifferenceFromAverage || !enoughRemainingDistinctValues)
							&& valueCountInBin > 0) {
						// add current distinct value to bin
						currentUpperIdx = dvIdx;
						nextValueCount = valueCountInBin;
					} else {
						currentUpperIdx = dvIdx + 1;
					}
					break;
				}
				valueCountInBin = nextValueCount;
			}

			if (currentUpperIdx >= distinctValueCount) {
				currentUpperIdx = distinctValueCount - 1;
			}

			upperBound = sortedDistinctValueList.get(currentUpperIdx);

			valuesUsed += nextValueCount;
			NumericalValueRange currentGroup = new NumericalValueRange(lowerBound, upperBound, columnIdx, null, true,
					binIdx == binCount);
			valueGroups.add(currentGroup);
			lowerBound = upperBound;
		}

		// set precision for representation
		applyAdaptiveVisualRounding(valueGroups, columnIsDate);
		return valueGroups;
	}

	@Override
	public GroupingType getGroupingType() {
		return type;
	}

	@Override
	public EqualDataFractionGrouping clone() {
		return new EqualDataFractionGrouping(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof EqualDataFractionGrouping)) {
			return false;
		}

		EqualDataFractionGrouping tempObj = (EqualDataFractionGrouping) obj;

		if (tempObj.isCategorical() != isCategorical()) {
			return false;
		}

		if (tempObj.getBinCount() != getBinCount()) {
			return false;
		}

		return true;
	}

	@Override
	public boolean definesUpperLowerBounds() {
		return true;
	}
}
