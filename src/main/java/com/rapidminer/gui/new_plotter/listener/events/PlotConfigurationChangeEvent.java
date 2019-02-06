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

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.listener.LinkAndBrushSelection;
import com.rapidminer.gui.new_plotter.templates.style.ColorScheme;

import java.awt.Color;
import java.awt.Font;
import java.util.LinkedList;
import java.util.List;

import org.jfree.chart.plot.PlotOrientation;


/**
 * A {@link PlotConfigurationChangeEvent} represents a change of the {@link PlotConfiguration}.
 * Every {@link PlotConfigurationChangeEvent} has a cloned source of its starting
 * {@link PlotConfiguration}.
 * 
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class PlotConfigurationChangeEvent implements ConfigurationChangeEvent {

	public enum PlotConfigurationChangeType {
		RANGE_AXIS_CONFIG_ADDED, // a range axis config was added
		RANGE_AXIS_CONFIG_REMOVED, // a range axis config was removed
		RANGE_AXIS_CONFIG_MOVED, // a range axis config was moved to another
									// index
		DIMENSION_CONFIG_ADDED, // a dimension config was added
		DIMENSION_CONFIG_REMOVED, // a dimension config was removed
		CHART_TITLE, // the chart title has changed
		AXES_FONT, // the axes font has changed
		FRAME_BACKGROUND_COLOR, // the chart background has changed
		PLOT_BACKGROUND_COLOR, // the plot background has changed
		PLOT_ORIENTATION, // the domain axis orientation has changed
		DATA_TABLE_EXCHANGED, // the data table has been exchanged

		// Refer to other events
		RANGE_AXIS_CONFIG_CHANGED, // a range axis configuration has changed
		DIMENSION_CONFIG_CHANGED, // the configuration for a dimension has been changed or exchanged
		AXIS_LINE_COLOR, AXIS_LINE_WIDTH, COLOR_SCHEME, LINK_AND_BRUSH_SELECTION, LEGEND_CHANGED, META_CHANGE,  // this
																												// means
																												// the
																												// a
																												// list
																												// of
																												// plot
																												// config
																												// change
																												// events
																												// is
																												// about
																												// to
																												// happen
		TRIGGER_REPLOT // this event is used to trigger a replot
	}

	private final PlotConfigurationChangeType type;
	private PlotConfiguration source;

	private final List<PlotConfigurationChangeEvent> plotConfigChangeEvents = new LinkedList<PlotConfigurationChangeEvent>();

	// Range Axis Config added, removed, moved
	private RangeAxisConfig rangeAxisConfig = null;
	private Integer index = null;

	private PlotDimension dimension = null;
	private DimensionConfig dimensionConfig = null;

	private String chartTitle = null;

	private Font axesFont = null;

	private Color plotBackgroundColor = null;
	private Color frameBackgroundColor = null;
	private Color axisLineColor = null;

	private PlotOrientation orientation = null;

	// refer to other events
	private DimensionConfigChangeEvent dimensionChange = null;
	private RangeAxisConfigChangeEvent rangeAxisConfigChange = null;
	private Float domainAxisLineWidth = null;
	private ColorScheme colorScheme = null;
	private DataTable dataTable = null;
	private LinkAndBrushSelection linkAndBrushSelection = null;
	private LegendConfigurationChangeEvent legendConfigurationChangeEvent = null;

	public DataTable getDataTable() {
		return dataTable;
	}

	/**
	 * Allowed {@link PlotConfigurationChangeType}s are RANGE_AXIS_CONFIG_ADDED,
	 * RANGE_AXIS_CONFIG_REMOVED and RANGE_AXIS_CONFIG_MOVED
	 */
	public PlotConfigurationChangeEvent(PlotConfiguration source, PlotConfigurationChangeType type,
			RangeAxisConfig rangeAxis, Integer index) {
		setSource(source);
		if ((type != PlotConfigurationChangeType.RANGE_AXIS_CONFIG_ADDED)
				&& (type != PlotConfigurationChangeType.RANGE_AXIS_CONFIG_REMOVED)
				&& (type != PlotConfigurationChangeType.RANGE_AXIS_CONFIG_MOVED)) {
			throw new RuntimeException(type + " is not allowed calling this constructor.");
		}
		this.type = type;
		this.rangeAxisConfig = rangeAxis;
		this.index = index;
	}

	public PlotConfigurationChangeEvent(PlotConfiguration source) {
		setSource(source);
		this.type = PlotConfigurationChangeType.TRIGGER_REPLOT;
	}

	/**
	 * Allowed {@link PlotConfigurationChangeType}s are DIMENSION_CONFIG_ADDED or DIMENSION_REMOVED
	 */
	public PlotConfigurationChangeEvent(PlotConfiguration source, PlotConfigurationChangeType type, PlotDimension dimension,
			DimensionConfig dimensionConfig) {
		if ((type != PlotConfigurationChangeType.DIMENSION_CONFIG_ADDED)
				&& (type != PlotConfigurationChangeType.DIMENSION_CONFIG_REMOVED)) {
			throw new RuntimeException(type + " is not allowed calling this constructor.");
		}

		setSource(source);
		this.type = type;
		this.dimension = dimension;
		this.dimensionConfig = dimensionConfig;
	}

	public PlotConfigurationChangeEvent(PlotConfiguration source, String chartTitle) {
		setSource(source);
		this.type = PlotConfigurationChangeType.CHART_TITLE;
		this.chartTitle = chartTitle;
	}

	public PlotConfigurationChangeEvent(PlotConfiguration source, List<PlotConfigurationChangeEvent> plotConfigChangeEvents) {
		setSource(source);
		this.type = PlotConfigurationChangeType.META_CHANGE;
		this.plotConfigChangeEvents.addAll(plotConfigChangeEvents);
	}

	public PlotConfigurationChangeEvent(PlotConfiguration source, Font axesFont) {
		setSource(source);
		this.type = PlotConfigurationChangeType.AXES_FONT;
		this.axesFont = axesFont;
	}

	/**
	 * Allowed {@link PlotConfigurationChangeType}s are FRAME_BACKGROUND_COLOR or
	 * PLOT_BACKGROUND_COLOR or AXIS_LINE_COLOR
	 */
	public PlotConfigurationChangeEvent(PlotConfiguration source, PlotConfigurationChangeType type, Color color) {
		setSource(source);
		this.type = type;
		if ((type != PlotConfigurationChangeType.FRAME_BACKGROUND_COLOR)
				&& (type != PlotConfigurationChangeType.PLOT_BACKGROUND_COLOR)
				&& (type != PlotConfigurationChangeType.AXIS_LINE_COLOR)) {
			throw new RuntimeException(type + " is not allowed calling this constructor.");
		}

		if (type == PlotConfigurationChangeType.FRAME_BACKGROUND_COLOR) {
			frameBackgroundColor = color;
		} else if (type == PlotConfigurationChangeType.AXIS_LINE_COLOR) {
			axisLineColor = color;
		} else if (type == PlotConfigurationChangeType.PLOT_BACKGROUND_COLOR) {
			plotBackgroundColor = color;
		} else {
			throw new RuntimeException("Unknown type for color assignment");
		}
	}

	public PlotConfigurationChangeEvent(PlotConfiguration source, DimensionConfigChangeEvent dimensionChange) {
		setSource(source);
		this.type = PlotConfigurationChangeType.DIMENSION_CONFIG_CHANGED;
		this.dimensionChange = dimensionChange;
	}

	public PlotConfigurationChangeEvent(PlotConfiguration source, LinkAndBrushSelection selection) {
		setSource(source);
		this.type = PlotConfigurationChangeType.LINK_AND_BRUSH_SELECTION;
		this.linkAndBrushSelection = selection;
	}

	public PlotConfigurationChangeEvent(PlotConfiguration source, RangeAxisConfigChangeEvent rangeAxisChange) {
		setSource(source);
		this.type = PlotConfigurationChangeType.RANGE_AXIS_CONFIG_CHANGED;
		this.rangeAxisConfigChange = rangeAxisChange;
	}

	/**
	 * @param plotConfiguration
	 * @param orientation
	 */
	public PlotConfigurationChangeEvent(PlotConfiguration plotConfiguration, PlotOrientation orientation) {
		setSource(plotConfiguration);
		this.type = PlotConfigurationChangeType.PLOT_ORIENTATION;
		this.orientation = orientation;
	}

	/**
	 * @param plotConfiguration
	 * @param domainAxisLineWidth
	 */
	public PlotConfigurationChangeEvent(PlotConfiguration plotConfiguration, float domainAxisLineWidth) {
		setSource(plotConfiguration);
		this.type = PlotConfigurationChangeType.AXIS_LINE_WIDTH;
		this.domainAxisLineWidth = domainAxisLineWidth;
	}

	/**
	 * @param plotConfiguration
	 * @param colorScheme
	 */
	public PlotConfigurationChangeEvent(PlotConfiguration plotConfiguration, ColorScheme colorScheme) {
		setSource(plotConfiguration);
		this.type = PlotConfigurationChangeType.COLOR_SCHEME;
		this.colorScheme = colorScheme;
	}

	public PlotConfigurationChangeEvent(PlotConfiguration plotConfiguration, DataTable dataTable) {
		setSource(plotConfiguration);
		this.type = PlotConfigurationChangeType.DATA_TABLE_EXCHANGED;
		this.dataTable = dataTable;
	}

	public PlotConfigurationChangeEvent(PlotConfiguration plotConfiguration, LegendConfigurationChangeEvent change) {
		setSource(plotConfiguration);
		this.type = PlotConfigurationChangeType.LEGEND_CHANGED;
		this.legendConfigurationChangeEvent = change;
	}

	/**
	 * @param plotConfiguration
	 */
	public void setSource(PlotConfiguration plotConfiguration) {
		this.source = plotConfiguration;
	}

	public DimensionConfigChangeEvent getDimensionChange() {
		return dimensionChange;
	}

	/**
	 * This function can only be called if type of change event is META_CHANGE
	 * 
	 * @param changeEvent
	 */
	public void addPlotConfigChangeEvent(PlotConfiguration newSource, PlotConfigurationChangeEvent changeEvent) {
		if (type != PlotConfigurationChangeType.META_CHANGE) {
			throw new IllegalArgumentException("Wrong type. Only META_CHANGE is allowed!");
		}
		source = newSource;
		plotConfigChangeEvents.add(changeEvent);
	}

	/**
	 * @return the list of plot configuration events, if this event is a META_CHANGE. The returned
	 *         list must NOT be changed!
	 */
	public List<PlotConfigurationChangeEvent> getPlotConfigChangeEvents() {
		return plotConfigChangeEvents;
	}

	public PlotConfigurationChangeType getType() {
		return type;
	}

	/**
	 * @return the rangeAxis
	 */
	public RangeAxisConfig getRangeAxisConfig() {
		return rangeAxisConfig;
	}

	/**
	 * @return the index
	 */
	public Integer getIndex() {
		return index;
	}

	/**
	 * @return the colorScheme
	 */
	public ColorScheme getColorScheme() {
		return colorScheme;
	}

	/**
	 * @return the linkAndBrushSelection
	 */
	public LinkAndBrushSelection getLinkAndBrushSelection() {
		return linkAndBrushSelection;
	}

	/**
	 * @return the dimension
	 */
	public PlotDimension getDimension() {
		return dimension;
	}

	/**
	 * @return the dimensionConfig
	 */
	public DimensionConfig getDimensionConfig() {
		return dimensionConfig;
	}

	/**
	 * @return the domainAxisLineColor
	 */
	public Color getDomainAxisLineColor() {
		return axisLineColor;
	}

	/**
	 * @return the domainAxisLineWidth
	 */
	public float getDomainAxisLineWidth() {
		return domainAxisLineWidth;
	}

	/**
	 * @return the rangeAxisChange
	 */
	public RangeAxisConfigChangeEvent getRangeAxisConfigChange() {
		return rangeAxisConfigChange;
	}

	/**
	 * @return the source
	 */
	public PlotConfiguration getSource() {
		return source;
	}

	/**
	 * @return the chartTitle
	 */
	public String getChartTitle() {
		return chartTitle;
	}

	/**
	 * @return the axesFont
	 */
	public Font getAxesFont() {
		return axesFont;
	}

	/**
	 * @return the plotBackgroundColor
	 */
	public Color getPlotBackgroundColor() {
		return plotBackgroundColor;
	}

	/**
	 * @return the chartBackgroundColor
	 */
	public Color getFrameBackgroundColor() {
		return frameBackgroundColor;
	}

	@Override
	public ConfigurationChangeType getConfigurationChangeType() {
		return ConfigurationChangeType.PLOT_CONFIGURATION_CHANGE;
	}

	/**
	 * @return the orientation
	 */
	public PlotOrientation getOrientation() {
		return orientation;
	}

	@Override
	public String toString() {
		return getType().toString();
	}

	public LegendConfigurationChangeEvent getLegendConfigurationChangeEvent() {
		return legendConfigurationChangeEvent;
	}

}
