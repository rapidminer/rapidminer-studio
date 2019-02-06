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

import java.text.DateFormat;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn.ValueType;
import com.rapidminer.gui.new_plotter.listener.ValueGroupingListener;
import com.rapidminer.gui.new_plotter.listener.events.ValueGroupingChangeEvent;
import com.rapidminer.gui.new_plotter.utility.DataStructureUtils;
import com.rapidminer.gui.new_plotter.utility.NumericalValueRange;
import com.rapidminer.gui.new_plotter.utility.ValueRange;


/**
 * Base class for most implementations of ValueGrouping.
 * 
 * All groupings inheriting this class have in common that they depend on exactly one column of a
 * datatable.
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public abstract class AbstractValueGrouping implements ValueGrouping {

	private boolean categorical;
	private DataTableColumn dataTableColumn;

	private List<ValueGroupingListener> listeners = new LinkedList<ValueGroupingListener>();
	private DateFormat dateFormat;

	public AbstractValueGrouping(DataTableColumn dataTableColumn, boolean isCategorical, DateFormat dateFormat) {
		this.categorical = isCategorical;
		this.dataTableColumn = dataTableColumn;
		this.dateFormat = dateFormat;
	}

	@Override
	public boolean isCategorical() {
		return categorical || getDomainType() == ValueType.NOMINAL;
	}

	/**
	 * @param categorical
	 *            the categorical to set
	 */
	public void setCategorical(boolean categorical) {
		if (categorical != this.categorical) {
			this.categorical = categorical;
			fireGroupingChanged(new ValueGroupingChangeEvent(this, categorical));
		}
	}

	@Override
	public void addListener(ValueGroupingListener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(ValueGroupingListener l) {
		listeners.remove(l);
	}

	protected void fireGroupingChanged(ValueGroupingChangeEvent e) {
		for (ValueGroupingListener l : listeners) {
			l.valueGroupingChanged(e);
		}
	}

	@Override
	public abstract ValueGrouping clone();

	public DataTableColumn getDataTableColumn() {
		return dataTableColumn;
	}

	public void setDataTableColumn(DataTableColumn dataTableColumn) {
		if (dataTableColumn != this.dataTableColumn) {
			this.dataTableColumn = dataTableColumn;
		}
	}

	/**
	 * Is called each time the underlying data (or the data column) changes. The default
	 * implementation invalidates the cache.
	 */
	// protected void invalidateCache() {
	// cachedGroupingModel = null;
	// }

	protected void forceDataTableColumn(DataTableColumn dataTableColumn) {
		this.dataTableColumn = dataTableColumn;
	}

	@Override
	public ValueType getDomainType() {
		if (categorical) {
			return ValueType.NOMINAL;
		} else {
			return dataTableColumn.getValueType();
		}
	}

	@Override
	public final List<ValueRange> getGroupingModel(DataTable data, double upperBound, double lowerBound) {
		return createGroupingModel(data, upperBound, lowerBound);
	}

	/**
	 * Returns an up-to-date grouping model without cumulation applied. Does not need to implement
	 * caching, since this is handled in AbstractValueGrouping.
	 */
	protected abstract List<ValueRange> createGroupingModel(DataTable data, double upperBound, double lowerBound);

	@Override
	public abstract boolean equals(Object obj);

	/**
	 * Configures the value ranges in valueGroups such that their toString() method display the
	 * optimal number of digits to discriminate both the intervals itself (upper/lower bound) and
	 * also discriminate intervals from their neighbours. For each data point a different precision
	 * might be calculated.
	 * 
	 * For dates this functions searches for the biggest displayed time unit by looking at the
	 * distance between the two farthest data points, and for the smallest displayed value by
	 * looking at the closest distance between two neighbouring data points. For all date values the
	 * same format string is used.
	 * 
	 * Expects the list to contain only {@link NumericalValueRange}s.
	 */
	/**
	 * @param valueGroups
	 * @param valuesAreDates
	 * 
	 *            TODO use param valuesAreDates
	 */
	protected void applyAdaptiveVisualRounding(List<ValueRange> valueGroups, boolean valuesAreDates) {
		if (valuesAreDates) {
			DateFormat dateFormat = getDateFormat();
			for (ValueRange range : valueGroups) {
				NumericalValueRange numericalValueRange = (NumericalValueRange) range;
				numericalValueRange.setDateFormat(dateFormat);
			}
		} else {
			// values are not dates

			// first pass
			NumericalValueRange previous = null;
			NumericalValueRange current = null;
			NumericalValueRange next = null;
			for (ValueRange valueGroup : valueGroups) {
				next = (NumericalValueRange) valueGroup;
				if (previous != null) {
					int precisionLower = Math.min(
							DataStructureUtils.getOptimalPrecision(current.getLowerBound(), current.getUpperBound()),
							DataStructureUtils.getOptimalPrecision(previous.getLowerBound(), current.getLowerBound()));
					int precisionUpper = Math.min(
							DataStructureUtils.getOptimalPrecision(current.getLowerBound(), current.getUpperBound()),
							DataStructureUtils.getOptimalPrecision(current.getUpperBound(), next.getUpperBound()));
					if (precisionUpper >= Integer.MAX_VALUE) {
						precisionUpper = precisionLower;
					}
					current.setVisualPrecision(precisionLower, precisionUpper);
				} else if (current != null) {
					int precisionLower = DataStructureUtils.getOptimalPrecision(current.getLowerBound(),
							current.getUpperBound());
					int precisionUpper = Math.min(
							DataStructureUtils.getOptimalPrecision(current.getLowerBound(), current.getUpperBound()),
							DataStructureUtils.getOptimalPrecision(current.getUpperBound(), next.getUpperBound()));
					if (precisionUpper >= Integer.MAX_VALUE) {
						precisionUpper = precisionLower;
					}
					current.setVisualPrecision(precisionLower, precisionUpper);
				}
				previous = current;
				current = next;
			}
			if (previous != null) {
				// even if eclipse states that this code is dead, it is not! (eclipse bug)
				int precisionLower = Math.min(
						DataStructureUtils.getOptimalPrecision(current.getLowerBound(), current.getUpperBound()),
						DataStructureUtils.getOptimalPrecision(previous.getLowerBound(), current.getLowerBound()));
				int precisionUpper = DataStructureUtils
						.getOptimalPrecision(current.getLowerBound(), current.getUpperBound());
				if (precisionUpper >= Integer.MAX_VALUE) {
					precisionUpper = precisionLower;
				}
				current.setVisualPrecision(precisionLower, precisionUpper);
			} else if (current != null) {
				int precision = DataStructureUtils.getOptimalPrecision(current.getLowerBound(), current.getUpperBound());
				current.setVisualPrecision(precision, precision);
			}

			// // second pass
			// current = null;
			// next = null;
			// for (ValueRange valueGroup : valueGroups ) {
			// next = (NumericalValueRange)valueGroup;
			// if (current != null) {
			// int currentPrecision = current.getUpperPrecision();
			// int nextPrecision = next.getLowerPrecision();
			// int precision = Math.min(nextPrecision, currentPrecision);
			// current.setUpperPrecision(precision);
			// next.setLowerPrecision(precision);
			// }
			// current = next;
			// }
		}
	}

	@Override
	public DateFormat getDateFormat() {
		return dateFormat;
	}

	@Override
	public void setDateFormat(DateFormat dateFormat) {
		if (dateFormat != this.dateFormat) {
			this.dateFormat = dateFormat;
			fireGroupingChanged(new ValueGroupingChangeEvent(this, dateFormat));
		}
	}
}
