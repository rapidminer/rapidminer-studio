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
package com.rapidminer.gui.new_plotter.engine.jfreechart;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleEdge;


/**
 * A {@link CrosshairOverlay} which supports multiple range axes.
 * 
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class MultiAxesCrosshairOverlay extends CrosshairOverlay {

	private static final long serialVersionUID = 1L;

	private Vector<ArrayList<Crosshair>> rangeCrosshairs = new Vector<ArrayList<Crosshair>>();

	/**
	 * Adds a crosshair for the range axis with index 0.
	 */
	@Override
	public void addRangeCrosshair(Crosshair crosshair) {
		addRangeCrosshair(0, crosshair);
	}

	public void addRangeCrosshair(int axisIdx, Crosshair crosshair) {
		while (rangeCrosshairs.size() < axisIdx + 1) {
			rangeCrosshairs.add(new ArrayList<Crosshair>());
		}

		rangeCrosshairs.get(axisIdx).add(crosshair);
		crosshair.addPropertyChangeListener(this);
	}

	/**
	 * removes a crosshair from the range axis with index 0.
	 */
	@Override
	public void removeRangeCrosshair(Crosshair crosshair) {
		removeRangeCrosshair(0, crosshair);
	}

	public void removeRangeCrosshair(int axisIdx, Crosshair crosshair) {
		if (rangeCrosshairs.size() > axisIdx) {
			ArrayList<Crosshair> crosshairsForRange = rangeCrosshairs.get(axisIdx);
			crosshairsForRange.remove(crosshair);
			crosshair.removePropertyChangeListener(this);
		}
	}

	/**
	 * Clears all range crosshairs on all axes.
	 */
	@Override
	public void clearRangeCrosshairs() {
		for (List<Crosshair> crosshairsForRange : rangeCrosshairs) {
			for (Crosshair crosshair : crosshairsForRange) {
				crosshair.removePropertyChangeListener(this);
			}
		}
		rangeCrosshairs.clear();
	}

	/**
	 * Returns the crosshairs on the range axis with index 0.
	 */
	@Override
	public List<Crosshair> getRangeCrosshairs() {
		return getRangeCrosshairs(0);
	}

	private List<Crosshair> getRangeCrosshairs(int axisIdx) {
		return rangeCrosshairs.get(axisIdx);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
		Shape savedClip = g2.getClip();
		Rectangle2D dataArea = chartPanel.getScreenDataArea();
		g2.clip(dataArea);
		JFreeChart chart = chartPanel.getChart();
		XYPlot plot = (XYPlot) chart.getPlot();
		ValueAxis xAxis = plot.getDomainAxis();
		RectangleEdge xAxisEdge = plot.getDomainAxisEdge();
		for (Crosshair ch : (List<Crosshair>) getDomainCrosshairs()) {
			if (ch.isVisible()) {
				double x = ch.getValue();
				double xx = xAxis.valueToJava2D(x, dataArea, xAxisEdge);
				if (plot.getOrientation() == PlotOrientation.VERTICAL) {
					drawVerticalCrosshair(g2, dataArea, xx, ch);
				} else {
					drawHorizontalCrosshair(g2, dataArea, xx, ch);
				}
			}
		}

		int rangeAxisIdx = 0;
		for (ArrayList<Crosshair> crosshairsForRange : rangeCrosshairs) {
			ValueAxis yAxis = plot.getRangeAxis(rangeAxisIdx);
			RectangleEdge yAxisEdge = plot.getRangeAxisEdge(rangeAxisIdx);
			for (Crosshair ch : crosshairsForRange) {
				if (ch.isVisible()) {
					double y = ch.getValue();
					double yy = yAxis.valueToJava2D(y, dataArea, yAxisEdge);
					if (plot.getOrientation() == PlotOrientation.VERTICAL) {
						drawHorizontalCrosshair(g2, dataArea, yy, ch);
					} else {
						drawVerticalCrosshair(g2, dataArea, yy, ch);
					}
				}
			}
			g2.setClip(savedClip);
			++rangeAxisIdx;
		}
	}
}
