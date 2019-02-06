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

import com.rapidminer.gui.new_plotter.configuration.LineFormat.LineStyle;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.FillStyle;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.IndicatorType;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.ItemShape;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.StackingMode;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.VisualizationType;

import java.awt.Color;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class SeriesFormatChangeEvent implements ConfigurationChangeEvent {

	public enum SeriesFormatChangeType {
		ITEM_SHAPE, ITEM_SIZE, ITEM_COLOR, LINE_WIDTH, LINE_STYLE, LINE_COLOR, OPACITY, AREA_FILL_STYLE, SERIES_TYPE, STACKING_MODE, UTILITY_INDICATOR
	}

	private final SeriesFormat source;
	private final SeriesFormatChangeType type;

	private VisualizationType seriesType = null;
	private StackingMode stackingMode = null;
	private IndicatorType errorIndicator = null;
	private LineStyle lineStyle = null;
	private ItemShape itemShape = null;

	private Double itemSize = null;
	private Float lineWidth = null;
	private Color itemColor = null;

	private Color lineColor = null;
	private FillStyle areaFillStyle = null;
	private Integer opacity = null;

	public SeriesFormatChangeEvent(SeriesFormat source, VisualizationType seriesType) {
		this.type = SeriesFormatChangeType.SERIES_TYPE;
		this.seriesType = seriesType;
		this.source = source;
	}

	public SeriesFormatChangeEvent(SeriesFormat source, StackingMode stackingMode) {
		this.type = SeriesFormatChangeType.STACKING_MODE;
		this.stackingMode = stackingMode;
		this.source = source;
	}

	public SeriesFormatChangeEvent(SeriesFormat source, IndicatorType errorIndicator) {
		this.type = SeriesFormatChangeType.UTILITY_INDICATOR;
		this.errorIndicator = errorIndicator;
		this.source = source;
	}

	public SeriesFormatChangeEvent(SeriesFormat source, LineStyle lineStyle) {
		this.type = SeriesFormatChangeType.LINE_STYLE;
		this.lineStyle = lineStyle;
		this.source = source;
	}

	public SeriesFormatChangeEvent(SeriesFormat source, ItemShape itemShape) {
		this.type = SeriesFormatChangeType.ITEM_SHAPE;
		this.itemShape = itemShape;
		this.source = source;
	}

	public SeriesFormatChangeEvent(SeriesFormat source, Double itemSize) {
		super();
		this.type = SeriesFormatChangeType.ITEM_SIZE;
		this.itemSize = itemSize;
		this.source = source;
	}

	public SeriesFormatChangeEvent(SeriesFormat source, Float lineWidth) {
		this.type = SeriesFormatChangeType.LINE_WIDTH;
		this.lineWidth = lineWidth;
		this.source = source;
	}

	public SeriesFormatChangeEvent(SeriesFormat source, FillStyle areaFillStyle) {
		this.type = SeriesFormatChangeType.AREA_FILL_STYLE;
		this.areaFillStyle = areaFillStyle;
		this.source = source;
	}

	public SeriesFormatChangeEvent(SeriesFormat source, Integer opacity) {
		this.type = SeriesFormatChangeType.OPACITY;
		this.opacity = opacity;
		this.source = source;
	}

	/**
	 * Allowed {@link SeriesFormatChangeType}s are LINE_COLOR or ITEM_COLOR.
	 */
	public SeriesFormatChangeEvent(SeriesFormat source, SeriesFormatChangeType type, Color color) {
		this.type = type;
		if ((type != SeriesFormatChangeType.ITEM_COLOR) && (type != SeriesFormatChangeType.LINE_COLOR)) {
			throw new RuntimeException(type + " is not allowed calling this constructor.");
		}
		if (type == SeriesFormatChangeType.LINE_COLOR) {
			this.lineColor = color;
		} else {
			this.itemColor = color;
		}
		this.source = source;
	}

	public SeriesFormatChangeType getType() {
		return type;
	}

	public VisualizationType getSeriesType() {
		return seriesType;
	}

	public IndicatorType getErrorIndicator() {
		return errorIndicator;
	}

	public StackingMode getStackingMode() {
		return stackingMode;
	}

	/**
	 * @return the source
	 */
	public SeriesFormat getSource() {
		return source;
	}

	/**
	 * @return the lineStyle
	 */
	public LineStyle getLineStyle() {
		return lineStyle;
	}

	/**
	 * @return the itemShape
	 */
	public ItemShape getItemShape() {
		return itemShape;
	}

	/**
	 * @return the itemSize
	 */
	public Double getItemSize() {
		return itemSize;
	}

	/**
	 * @return the lineWidth
	 */
	public Float getLineWidth() {
		return lineWidth;
	}

	/**
	 * @return the lineColor
	 */
	public Color getLineColor() {
		return lineColor;
	}

	/**
	 * @return the opacity
	 */
	public Integer getOpacity() {
		return opacity;
	}

	/**
	 * @param seriesType
	 *            the seriesType to set
	 */
	public void setSeriesType(VisualizationType seriesType) {
		this.seriesType = seriesType;
	}

	/**
	 * @param stackingMode
	 *            the stackingMode to set
	 */
	public void setStackingMode(StackingMode stackingMode) {
		this.stackingMode = stackingMode;
	}

	/**
	 * @return the itemColor
	 */
	public Color getItemColor() {
		return itemColor;
	}

	/**
	 * @return the areaFillStyle
	 */
	public FillStyle getAreaFillStyle() {
		return areaFillStyle;
	}

	@Override
	public ConfigurationChangeType getConfigurationChangeType() {
		return ConfigurationChangeType.SERIES_FORMAT_CHANGE;
	}

	@Override
	public String toString() {
		return getType().toString();
	}

}
