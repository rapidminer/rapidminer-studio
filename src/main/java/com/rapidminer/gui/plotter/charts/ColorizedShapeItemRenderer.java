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

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import org.jfree.util.ShapeUtilities;


/**
 * This renderer provides colorized renderering not based on the series but on a numerical value.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class ColorizedShapeItemRenderer extends XYShapeRenderer {

	private static final long serialVersionUID = 1110895790394519633L;

	private double minColor;

	private double maxColor;

	private ColorProvider colorProvider = new ColorProvider(true);

	public ColorizedShapeItemRenderer(double minColor, double maxColor) {
		super();
		this.minColor = minColor;
		this.maxColor = maxColor;
	}

	/**
	 * Draws the block representing the specified item.
	 * 
	 * @param g2
	 *            the graphics device.
	 * @param state
	 *            the state.
	 * @param dataArea
	 *            the data area.
	 * @param info
	 *            the plot rendering info.
	 * @param plot
	 *            the plot.
	 * @param domainAxis
	 *            the x-axis.
	 * @param rangeAxis
	 *            the y-axis.
	 * @param dataset
	 *            the dataset.
	 * @param series
	 *            the series index.
	 * @param item
	 *            the item index.
	 * @param crosshairState
	 *            the crosshair state.
	 * @param pass
	 *            the pass index.
	 */
	@Override
	public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info,
			XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset, int series, int item,
			CrosshairState crosshairState, int pass) {

		Shape hotspot = null;
		EntityCollection entities = null;
		if (info != null) {
			entities = info.getOwner().getEntityCollection();
		}

		double x = dataset.getXValue(series, item);
		double y = dataset.getYValue(series, item);
		double colorValue = ((XYZDataset) dataset).getZValue(series, item);
		double normalized = (colorValue - minColor) / (maxColor - minColor);

		if (Double.isNaN(x) || Double.isNaN(y)) {
			// can't draw anything
			return;
		}

		double transX = domainAxis.valueToJava2D(x, dataArea, plot.getDomainAxisEdge());
		double transY = rangeAxis.valueToJava2D(y, dataArea, plot.getRangeAxisEdge());

		PlotOrientation orientation = plot.getOrientation();

		Shape shape = getItemShape(series, item);
		if (orientation == PlotOrientation.HORIZONTAL) {
			shape = ShapeUtilities.createTranslatedShape(shape, transY, transX);
		} else if (orientation == PlotOrientation.VERTICAL) {
			shape = ShapeUtilities.createTranslatedShape(shape, transX, transY);
		}
		hotspot = shape;
		if (shape.intersects(dataArea)) {
			g2.setPaint(colorProvider.getPointColor(normalized));
			g2.fill(shape);
			if (getDrawOutlines()) {
				if (getUseOutlinePaint()) {
					g2.setPaint(getItemOutlinePaint(series, item));
				} else {
					g2.setPaint(getItemPaint(series, item));
				}
				g2.setStroke(getItemOutlineStroke(series, item));
				g2.draw(shape);
			}
		}

		// add an entity for the item...
		if (entities != null) {
			addEntity(entities, hotspot, dataset, series, item, transX, transY);
		}
	}
}
