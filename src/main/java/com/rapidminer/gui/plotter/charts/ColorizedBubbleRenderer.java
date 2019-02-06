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

import java.awt.Color;
import java.awt.Paint;

import org.jfree.chart.renderer.xy.XYBubbleRenderer;


/**
 * This renderer provides colorized renderering not based on the series but on a numerical value.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class ColorizedBubbleRenderer extends XYBubbleRenderer {

	private static final long serialVersionUID = 384459884477017759L;

	private double minColor;

	private double maxColor;

	private double[] colors;

	private ColorProvider colorProvider = new ColorProvider(true);

	public ColorizedBubbleRenderer(double[] colors) {
		super(XYBubbleRenderer.SCALE_ON_RANGE_AXIS);
		this.minColor = Double.POSITIVE_INFINITY;
		this.maxColor = Double.NEGATIVE_INFINITY;
		for (double c : colors) {
			minColor = MathFunctions.robustMin(minColor, c);
			maxColor = MathFunctions.robustMax(maxColor, c);
		}
		this.colors = colors;
	}

	@Override
	public Paint getItemPaint(int series, int item) {
		double normalized = (colors[item] - minColor) / (maxColor - minColor);
		return colorProvider.getPointColor(normalized);
	}

	@Override
	public Paint getItemOutlinePaint(int series, int item) {
		return Color.DARK_GRAY;
	}
}
