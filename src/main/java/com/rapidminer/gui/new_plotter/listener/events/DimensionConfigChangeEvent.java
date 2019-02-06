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

import com.rapidminer.gui.new_plotter.configuration.DataTableColumn;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.DomainConfigManager;
import com.rapidminer.gui.new_plotter.configuration.event.AxisParallelLinesConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.utility.ColorProvider;
import com.rapidminer.gui.new_plotter.utility.ShapeProvider;
import com.rapidminer.gui.new_plotter.utility.SizeProvider;

import java.text.DateFormat;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class DimensionConfigChangeEvent implements ConfigurationChangeEvent {

	public enum DimensionConfigChangeType {
		RESET, ABOUT_TO_CHANGE_GROUPING, 	// informing listernes that grouping will change
		GROUPING_CHANGED,			// the grouping has been changed its values
		RANGE,						// the user range has been changed or exchanged
		COLUMN,						// the datatable column has been changed or exchange
		SCALING,					// scaling for dimension config has changed (i.e. logarithmic or linear)
		LABEL,						// the dimensions label has changed
		COLOR_PROVIDER,				// the color provider has been changed or exchanged
		SHAPE_PROVIDER,				// the shape provider has been changed or exchanged
		SIZE_PROVIDER, 				// the size provider has been changed or exchanged
		SORTING,					// the sorting has been changed or exchanged
		AUTO_NAMING, COLOR_SCHEME, CROSSHAIR_LINES_CHANGED, DATE_FORMAT_CHANGED,
	}

	private final DimensionConfigChangeType type;
	private final DimensionConfig source;
	private final PlotDimension dimension;

	private DataTableColumn column = null;
	private ValueGroupingChangeEvent groupingChangeEvent = null;
	private ValueRangeChangeEvent valueRangeChangedEvent = null;

	private Boolean logarithmic = null;
	private String label = null;
	private ColorProvider colorProvider = null;
	private ShapeProvider shapeProvider = null;
	private SizeProvider sizeProvider = null;
	private DomainConfigManager.Sorting sortingMode = null;
	private Boolean includeZero = null;
	private Boolean autoNaming = null;
	private AxisParallelLinesConfigurationChangeEvent crosshairLinesChange;
	private DateFormat dateFormat;

	/**
	 * Allowed {@link DimensionConfigChangeType}s are ABOUT_TO_CHANGE_GROUPING or RESET or
	 * COLOR_SCHEME
	 */
	public DimensionConfigChangeEvent(DimensionConfig source, PlotDimension dimension, DimensionConfigChangeType type) {
		if ((type != DimensionConfigChangeType.ABOUT_TO_CHANGE_GROUPING) && (type != DimensionConfigChangeType.RESET)
				&& (type != DimensionConfigChangeType.COLOR_SCHEME)) {
			throw new RuntimeException(type + " is not allowed calling this constructor.");
		}
		this.source = source;
		this.type = type;
		this.dimension = dimension;
	}

	public DimensionConfigChangeEvent(DimensionConfig source, PlotDimension dimension,
			ValueGroupingChangeEvent groupingChangeEvent) {
		this.source = source;
		this.type = DimensionConfigChangeType.GROUPING_CHANGED;
		this.groupingChangeEvent = groupingChangeEvent;
		this.dimension = dimension;
	}

	public DimensionConfigChangeEvent(DimensionConfig source, PlotDimension dimension,
			ValueRangeChangeEvent valueRangeChangedEvent) {
		this.source = source;
		this.type = DimensionConfigChangeType.RANGE;
		this.valueRangeChangedEvent = valueRangeChangedEvent;
		this.dimension = dimension;
	}

	public DimensionConfigChangeEvent(DimensionConfig source, PlotDimension dimension, DataTableColumn column) {
		this.source = source;
		this.type = DimensionConfigChangeType.COLUMN;
		this.column = column;
		this.dimension = dimension;
	}

	public DimensionConfigChangeEvent(DimensionConfig source, PlotDimension dimension, Boolean bool,
			DimensionConfigChangeType type) {
		if ((type != DimensionConfigChangeType.SCALING) && (type != DimensionConfigChangeType.AUTO_NAMING)) {
			throw new RuntimeException(type + " is not allowed calling this constructor.");
		}
		this.source = source;
		this.type = type;
		if (type == DimensionConfigChangeType.SCALING) {
			this.logarithmic = bool;
		} else if (type == DimensionConfigChangeType.AUTO_NAMING) {
			this.autoNaming = bool;
		} else {
			this.includeZero = bool;
		}
		this.dimension = dimension;
	}

	public DimensionConfigChangeEvent(DimensionConfig source, PlotDimension dimension, String label) {
		this.source = source;
		this.type = DimensionConfigChangeType.LABEL;
		this.label = label;
		this.dimension = dimension;
	}

	public DimensionConfigChangeEvent(DimensionConfig source, PlotDimension dimension, ColorProvider colorProvider) {
		this.source = source;
		this.type = DimensionConfigChangeType.COLOR_PROVIDER;
		this.colorProvider = colorProvider;
		this.dimension = dimension;
	}

	public DimensionConfigChangeEvent(DimensionConfig source, PlotDimension dimension, SizeProvider sizeProvider) {
		this.source = source;
		this.type = DimensionConfigChangeType.SIZE_PROVIDER;
		this.sizeProvider = sizeProvider;
		this.dimension = dimension;
	}

	public DimensionConfigChangeEvent(DimensionConfig source, PlotDimension dimension, ShapeProvider shapeProvider) {
		this.source = source;
		this.type = DimensionConfigChangeType.SHAPE_PROVIDER;
		this.shapeProvider = shapeProvider;
		this.dimension = dimension;
	}

	public DimensionConfigChangeEvent(DimensionConfig source, PlotDimension dimension,
			DomainConfigManager.Sorting sortingMode) {
		this.source = source;
		this.type = DimensionConfigChangeType.SORTING;
		this.sortingMode = sortingMode;
		this.dimension = dimension;
	}

	public DimensionConfigChangeEvent(DomainConfigManager source, PlotDimension dimension,
			AxisParallelLinesConfigurationChangeEvent e) {
		this.type = DimensionConfigChangeType.CROSSHAIR_LINES_CHANGED;
		this.dimension = dimension;
		this.source = source;
		this.crosshairLinesChange = e;
	}

	public DimensionConfigChangeEvent(DimensionConfig source, DateFormat dateFormat) {
		this.source = source;
		this.dimension = source.getDimension();
		this.type = DimensionConfigChangeType.DATE_FORMAT_CHANGED;
		this.dateFormat = dateFormat;
	}

	/**
	 * @return the column
	 */
	public DataTableColumn getDataTableColumn() {
		return column;
	}

	/**
	 * @return the groupingChangeEvent
	 */
	public ValueGroupingChangeEvent getGroupingChangeEvent() {
		return groupingChangeEvent;
	}

	/**
	 * @return the valueRangeChangedEvent
	 */
	public ValueRangeChangeEvent getValueRangeChangedEvent() {
		return valueRangeChangedEvent;
	}

	/**
	 * @return the logarithmic
	 */
	public Boolean getLogarithmic() {
		return logarithmic;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return the colorProvider
	 */
	public ColorProvider getColorProvider() {
		return colorProvider;
	}

	/**
	 * @return the sizeProvider
	 */
	public SizeProvider getSizeProvider() {
		return sizeProvider;
	}

	/**
	 * @return the shapeProvider
	 */
	public ShapeProvider getShapeProvider() {
		return shapeProvider;
	}

	/**
	 * @return the type
	 */
	public DimensionConfigChangeType getType() {
		return type;
	}

	/**
	 * @return the source
	 */
	public DimensionConfig getSource() {
		return source;
	}

	/**
	 * @return the sortProvider
	 */
	public DomainConfigManager.Sorting getSortingMode() {
		return sortingMode;
	}

	/**
	 * @return
	 */
	public PlotDimension getDimension() {
		return dimension;
	}

	@Override
	public ConfigurationChangeType getConfigurationChangeType() {
		return ConfigurationChangeType.DIMENSION_CONFIG_CHANGE;
	}

	/**
	 * @return the includeZero
	 */
	public Boolean getIncludeZero() {
		return includeZero;
	}

	/**
	 * @return the autoNaming
	 */
	public Boolean getAutoNaming() {
		return autoNaming;
	}

	@Override
	public String toString() {
		return getType().toString();
	}

	public AxisParallelLinesConfigurationChangeEvent getCrosshairLinesChange() {
		return crosshairLinesChange;
	}

	public DateFormat getDateFormat() {
		return dateFormat;
	}
}
