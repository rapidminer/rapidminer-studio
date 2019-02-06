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

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.axis.ValueTick;
import org.jfree.ui.RectangleEdge;

import com.rapidminer.gui.plotter.PlotterAdapter;
import com.rapidminer.tools.math.MathFunctions;


/**
 * This symbol axis draws a yellowisch background according to the axis weight.
 *
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class WeightBasedSymbolAxis extends SymbolAxis {

	private static final long serialVersionUID = -2836199514919011137L;

	private double[] weights;

	private double maxWeight;

	public WeightBasedSymbolAxis(String name, String[] symbols, double[] weights) {
		super(name, symbols);
		this.weights = weights;
		for (double d : weights) {
			maxWeight = MathFunctions.robustMax(Math.abs(d), maxWeight);
		}
	}

	/**
	 * Draws the grid bands for the axis when it is at the top or bottom of the plot.
	 * 
	 * @param g2
	 *            the graphics device.
	 * @param drawArea
	 *            the area within which the chart should be drawn.
	 * @param plotArea
	 *            the area within which the plot should be drawn (a subset of the drawArea).
	 * @param firstGridBandIsDark
	 *            True: the first grid band takes the color of <CODE>gridBandPaint<CODE>. False: the
	 *            second grid band takes the color of <CODE>gridBandPaint<CODE>.
	 * @param ticks
	 *            a list of ticks.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void drawGridBandsVertical(Graphics2D g2, Rectangle2D drawArea, Rectangle2D plotArea,
			boolean firstGridBandIsDark, List ticks) {
		double xx = plotArea.getX();
		double yy1, yy2;

		// gets the outline stroke width of the plot
		double outlineStrokeWidth;
		Stroke outlineStroke = getPlot().getOutlineStroke();
		if (outlineStroke != null && outlineStroke instanceof BasicStroke) {
			outlineStrokeWidth = ((BasicStroke) outlineStroke).getLineWidth();
		} else {
			outlineStrokeWidth = 1d;
		}

		Rectangle2D band;
		for (ValueTick tick : (List<ValueTick>) ticks) {
			int weightIndex = (int) tick.getValue();
			yy1 = valueToJava2D(tick.getValue() + 0.5d, plotArea, RectangleEdge.LEFT);
			yy2 = valueToJava2D(tick.getValue() - 0.5d, plotArea, RectangleEdge.LEFT);

			g2.setColor(PlotterAdapter.getWeightColor(this.weights[weightIndex], this.maxWeight));

			band = new Rectangle2D.Double(xx + outlineStrokeWidth, yy1, plotArea.getMaxX() - xx - outlineStrokeWidth,
					yy2 - yy1);
			g2.fill(band);
		}
		g2.setPaintMode();
	}

	/**
	 * Draws the grid bands for the axis when it is at the top or bottom of the plot.
	 * 
	 * @param g2
	 *            the graphics device.
	 * @param plotArea
	 *            the area within which the chart should be drawn.
	 * @param dataArea
	 *            the area within which the plot should be drawn (a subset of the drawArea).
	 * @param firstGridBandIsDark
	 *            True: the first grid band takes the color of <CODE>gridBandPaint<CODE>. False: the
	 *            second grid band takes the color of <CODE>gridBandPaint<CODE>.
	 * @param ticks
	 *            the ticks.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void drawGridBandsHorizontal(Graphics2D g2, Rectangle2D plotArea, Rectangle2D dataArea,
			boolean firstGridBandIsDark, List ticks) {
		double yy = dataArea.getY();
		double xx1, xx2;

		// gets the outline stroke width of the plot
		double outlineStrokeWidth;
		if (getPlot().getOutlineStroke() != null) {
			outlineStrokeWidth = ((BasicStroke) getPlot().getOutlineStroke()).getLineWidth();
		} else {
			outlineStrokeWidth = 1d;
		}

		Rectangle2D band;
		for (ValueTick tick : (List<ValueTick>) ticks) {
			int weightIndex = (int) tick.getValue();
			xx1 = valueToJava2D(tick.getValue() - 0.5d, dataArea, RectangleEdge.BOTTOM);
			xx2 = valueToJava2D(tick.getValue() + 0.5d, dataArea, RectangleEdge.BOTTOM);

			g2.setColor(PlotterAdapter.getWeightColor(this.weights[weightIndex], this.maxWeight));

			band = new Rectangle2D.Double(xx1, yy + outlineStrokeWidth, xx2 - xx1,
					dataArea.getMaxY() - yy - outlineStrokeWidth);
			g2.fill(band);
		}
		g2.setPaintMode();
	}
}
