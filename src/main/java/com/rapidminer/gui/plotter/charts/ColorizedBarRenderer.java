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

import java.awt.Color;
import java.awt.Paint;

import org.jfree.chart.renderer.category.BarRenderer;

import com.rapidminer.gui.plotter.ColorProvider;
import com.rapidminer.tools.math.MathFunctions;


/**
 * Paints the bars in colorized RapidMiner style.
 *
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class ColorizedBarRenderer extends BarRenderer {

	private static final long serialVersionUID = 5262078816286731693L;

	private double[] colorValues;

	private double minColor;

	private double maxColor;

	private ColorProvider colorProvider = new ColorProvider(true);

	public ColorizedBarRenderer(double[] colorValues) {
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

	@Override
	public Paint getItemPaint(int series, int item) {
		if (colorValues == null || colorValues.length <= item || minColor == maxColor) {
			return Color.RED;
		} else {
			double normalized = (colorValues[item] - minColor) / (maxColor - minColor);
			return colorProvider.getPointColor(normalized);
		}
	}
}
