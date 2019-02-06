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

import com.rapidminer.gui.new_plotter.configuration.LegendConfiguration;
import com.rapidminer.gui.new_plotter.configuration.LegendConfiguration.LegendPosition;

import java.awt.Color;
import java.awt.Font;


/**
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class LegendConfigurationChangeEvent {

	public enum LegendConfigurationChangeType {
		POSITON, // the legend position has changed
		FONT, // the legend font has changed
		SHOW_DIMENSION_TYPE, // displays the type of a dimension in front of categorical dimension
								// legend items
		BACKGROUND_COLOR, FRAME_COLOR, SHOW_LEGEND_FRAME
	}

	private LegendPosition legendPosition = null;
	private Font legendFont = null;
	private LegendConfiguration source;
	private LegendConfigurationChangeType type;
	private boolean showDimensionType;
	private Color frameColor;
	private Color backgroundColor;
	private boolean showLegendFrame;

	public LegendConfigurationChangeEvent(LegendConfiguration source, LegendPosition legendPosition) {
		this.source = source;
		this.type = LegendConfigurationChangeType.POSITON;
		this.legendPosition = legendPosition;
	}

	public LegendConfigurationChangeEvent(LegendConfiguration source, Font font) {
		this.source = source;
		this.type = LegendConfigurationChangeType.FONT;
		this.legendFont = font;
	}

	public LegendConfigurationChangeEvent(LegendConfiguration source, boolean show, LegendConfigurationChangeType type) {
		if ((type != LegendConfigurationChangeType.SHOW_DIMENSION_TYPE)
				&& (type != LegendConfigurationChangeType.SHOW_LEGEND_FRAME)) {
			throw new RuntimeException(type + " is not allowed calling this constructor.");
		}
		this.source = source;
		this.type = type;
		if (type == LegendConfigurationChangeType.SHOW_DIMENSION_TYPE) {
			this.showDimensionType = show;
		} else {
			this.showLegendFrame = show;
		}
	}

	/**
	 * Only FRAME_COLOR or BACKGROUND_COLOR are allowed as type.
	 */
	public LegendConfigurationChangeEvent(LegendConfiguration source, Color color, LegendConfigurationChangeType type) {
		if ((type != LegendConfigurationChangeType.FRAME_COLOR) && (type != LegendConfigurationChangeType.BACKGROUND_COLOR)) {
			throw new RuntimeException(type + " is not allowed calling this constructor.");
		}
		this.source = source;
		this.type = type;
		if (type == LegendConfigurationChangeType.FRAME_COLOR) {
			frameColor = color;
		} else {
			backgroundColor = color;
		}
	}

	public boolean isShowDimensionType() {
		return showDimensionType;
	}

	/**
	 * @return the legendPosition
	 */
	public LegendPosition getLegendPosition() {
		return legendPosition;
	}

	/**
	 * @return the legendFont
	 */
	public Font getLegendFont() {
		return legendFont;
	}

	public LegendConfiguration getSource() {
		return source;
	}

	public LegendConfigurationChangeType getType() {
		return type;
	}

	/**
	 * @return the frameColor
	 */
	public Color getFrameColor() {
		return this.frameColor;
	}

	/**
	 * @return the backgroundColor
	 */
	public Color getBackgroundColor() {
		return this.backgroundColor;
	}

	/**
	 * @return the showLegendFrame
	 */
	public boolean isShowLegendFrame() {
		return this.showLegendFrame;
	}
}
