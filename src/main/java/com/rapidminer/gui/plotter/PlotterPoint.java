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
package com.rapidminer.gui.plotter;

import java.awt.Color;


/**
 * Helper class for the plotter point positions and colors.
 *
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class PlotterPoint {

	private double x;
	private double y;
	private double color;
	private Color borderColor;

	public PlotterPoint(double x, double y, double color, Color borderColor) {
		this.x = x;
		this.y = y;
		this.color = color;
		this.borderColor = borderColor;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getColor() {
		return color;
	}

	public Color getBorderColor() {
		return borderColor;
	}
}
