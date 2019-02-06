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
import com.rapidminer.gui.new_plotter.ChartConfigurationException;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn.ValueType;
import com.rapidminer.gui.new_plotter.listener.ValueGroupingListener;
import com.rapidminer.gui.new_plotter.utility.ValueRange;
import com.rapidminer.tools.I18N;

import java.text.DateFormat;
import java.util.List;


/**
 * Groups values by predefined criteria, e.g. binning of a numerical value source.
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public interface ValueGrouping {

	public class ValueGroupingFactory {

		private final static int binCount = 5;

		public static ValueGrouping getValueGrouping(GroupingType type, DataTableColumn dataTableColumn,
				boolean categorical, DateFormat dateFormat) throws ChartConfigurationException {
			switch (type) {
				case DISTINCT_VALUES:
					return new DistinctValueGrouping(dataTableColumn, categorical, dateFormat);
				case EQUAL_DATA_FRACTION:
					return new EqualDataFractionGrouping(dataTableColumn, binCount, categorical, dateFormat);
				case EQUIDISTANT_FIXED_BIN_COUNT:
					return new EquidistantFixedBinCountBinning(binCount, Double.NaN, Double.NaN, dataTableColumn,
							categorical, dateFormat);
				case NONE:
					return null;
				default:
					throw new RuntimeException("This cannot happen");
			}

		}
	}

	public enum GroupingType {
		NONE(I18N.getGUILabel("plotter.grouping_type.NONE.label")), EQUIDISTANT_FIXED_BIN_COUNT(I18N
				.getGUILabel("plotter.grouping_type.EQUIDISTANT_FIXED_BIN_COUNT.label")), DISTINCT_VALUES(I18N
				.getGUILabel("plotter.grouping_type.DISTINCT_VALUES.label")), EQUAL_DATA_FRACTION(I18N
				.getGUILabel("plotter.grouping_type.EQUAL_DATA_FRACTION.label"));

		private final String name;

		private GroupingType(String name) {
			this.name = name;
		}

		/**
		 * @return The display name of the enum value.
		 */
		public String getName() {
			return name;
		}
	}

	public List<ValueRange> getGroupingModel(DataTable data, double upperBoud, double lowerBound);

	public boolean isCategorical();

	/**
	 * Returns the type of the grouping, like Distinct values or Equal data fraction grouping.
	 */
	public GroupingType getGroupingType();

	public void addListener(ValueGroupingListener l);

	public void removeListener(ValueGroupingListener l);

	public ValueGrouping clone();

	/**
	 * The type of the groups created by this ValueGrouping.
	 */
	public ValueType getDomainType();

	/**
	 * Returns true iff this ValueGrouping guarantees that each ValueRange in each possible
	 * resulting grouping model defines upper and lower bounds.
	 */
	public boolean definesUpperLowerBounds();

	public DateFormat getDateFormat();

	public void setDateFormat(DateFormat dateFormat);

	@Override
	public boolean equals(Object obj);
}
