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
package com.rapidminer.gui.new_plotter.listener.events;

import com.rapidminer.gui.new_plotter.configuration.AggregationWindowing;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn;
import com.rapidminer.gui.new_plotter.configuration.ValueGrouping;

import java.text.DateFormat;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class ValueGroupingChangeEvent implements ConfigurationChangeEvent {

	public enum ValueGroupingChangeType {
		CATEGORICAL,			// categorical has changed
		DATA_TABLE_COLUMN,		// the data table column has been changed
		BIN_COUNT,				// the group count has been changed
		RESET,					// total reset or exchange of the grouping
		DATE_FORMAT
	}

	private final ValueGrouping source;
	private final ValueGroupingChangeType type;

	private Integer binCount = null;
	private DataTableColumn dataTableColumn = null;
	private AggregationWindowing groupingCumulation = null;
	private Boolean categorical = null;

	public ValueGroupingChangeEvent(ValueGrouping source) {
		this.source = source;
		this.type = ValueGroupingChangeType.RESET;
	}

	public ValueGroupingChangeEvent(ValueGrouping source, Integer binCount) {
		this.source = source;
		this.type = ValueGroupingChangeType.BIN_COUNT;
		this.binCount = binCount;
	}

	public ValueGroupingChangeEvent(ValueGrouping source, DataTableColumn dataTableColumn) {
		this.source = source;
		this.type = ValueGroupingChangeType.DATA_TABLE_COLUMN;
		this.dataTableColumn = dataTableColumn;
	}

	public ValueGroupingChangeEvent(ValueGrouping source, Boolean categorical) {
		this.source = source;
		this.type = ValueGroupingChangeType.CATEGORICAL;
		this.categorical = categorical;
	}

	public ValueGroupingChangeEvent(ValueGrouping source, DateFormat dateFormat) {
		this.source = source;
		this.type = ValueGroupingChangeType.DATE_FORMAT;
	}

	public AggregationWindowing getGroupingCumulation() {
		return groupingCumulation;
	}

	public DataTableColumn getDataTableColumn() {
		return dataTableColumn;
	}

	public Integer getBinCount() {
		return binCount;
	}

	/**
	 * @return the source
	 */
	public ValueGrouping getSource() {
		return source;
	}

	/**
	 * @return the type
	 */
	public ValueGroupingChangeType getType() {
		return type;
	}

	/**
	 * @return the categorical
	 */
	public Boolean getCategorical() {
		return categorical;
	}

	@Override
	public ConfigurationChangeType getConfigurationChangeType() {
		return ConfigurationChangeType.VALUE_GROUPING_CHANGE;
	}

	@Override
	public String toString() {
		return getType().toString();
	}

}
