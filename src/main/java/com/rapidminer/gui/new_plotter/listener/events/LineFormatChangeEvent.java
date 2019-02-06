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

import com.rapidminer.gui.new_plotter.configuration.LineFormat;
import com.rapidminer.gui.new_plotter.configuration.LineFormat.LineStyle;

import java.awt.Color;


/**
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class LineFormatChangeEvent {

	enum LineFormatChangeType {
		WIDTH, STYLE, COLOR,
	}

	private LineFormatChangeType type;
	private float width;
	private LineStyle style = null;
	private Color color = null;

	public LineFormatChangeEvent(LineFormat lineFormat, Color color) {
		this.type = LineFormatChangeType.COLOR;
		this.color = color;
	}

	public LineFormatChangeEvent(LineFormat lineFormat, LineStyle style) {
		this.type = LineFormatChangeType.STYLE;
		this.style = style;
	}

	public LineFormatChangeEvent(LineFormat lineFormat, float width) {
		this.type = LineFormatChangeType.WIDTH;
		this.width = width;
	}

	public LineFormatChangeType getType() {
		return type;
	}

	public float getWidth() {
		return width;
	}

	public LineStyle getStyle() {
		return style;
	}

	public Color getColor() {
		return color;
	}
}
