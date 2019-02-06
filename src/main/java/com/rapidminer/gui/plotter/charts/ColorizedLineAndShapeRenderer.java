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
package com.rapidminer.gui.plotter.charts;

import com.rapidminer.gui.plotter.ColorProvider;
import com.rapidminer.tools.math.MathFunctions;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;

import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;


/**
 * This renderer provides colorized renderering not based on the series but on a numerical value.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class ColorizedLineAndShapeRenderer extends XYLineAndShapeRenderer {

	private static final long serialVersionUID = 6884606136158793687L;

	public static final Stroke STROKE = new BasicStroke(0.75f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	private double[] colorValues;

	private double minColor;

	private double maxColor;

	private ColorProvider colorProvider = new ColorProvider();

	public ColorizedLineAndShapeRenderer(double[] colorValues) {
		this.colorValues = colorValues;
		this.minColor = Double.POSITIVE_INFINITY;
		this.maxColor = Double.NEGATIVE_INFINITY;
		if (this.colorValues != null) {
			for (double d : this.colorValues) {
				this.minColor = MathFunctions.robustMin(this.minColor, d);
				this.maxColor = MathFunctions.robustMax(this.maxColor, d);
			}
		}
	}

	public double getMinColorValue() {
		return minColor;
	}

	public double getMaxColorValue() {
		return maxColor;
	}

	@Override
	public Paint getItemPaint(int series, int item) {
		if ((colorValues == null) || (minColor == maxColor)) {
			return Color.RED;
		} else {
			double normalized = (colorValues[series] - minColor) / (maxColor - minColor);
			return colorProvider.getPointColor(normalized);
		}
	}

	@Override
	public Stroke getItemStroke(int series, int item) {
		return STROKE;
	}

	@Override
	public boolean getItemShapeVisible(int series, int item) {
		return false;
	}
}
