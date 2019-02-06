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

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.FastScatterPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;


/**
 * This class provides ways to shift (aka pan/scroll) a plot. The shift is done through the arrow
 * keys and its step can be configured to be a fixed amount, a percentual of the current axis or a
 * range in pixels.
 * <p>
 * This class only supports plots of type {@link org.jfree.chart.plot.XYPlot XYPlot},
 * {@link org.jfree.chart.plot.ContourPlot ContourPlot} and
 * {@link org.jfree.chart.plot.FastScatterPlot FastScatterPlot}.
 * <p>
 * Use &larr; and &rarr; to shift the plot left and right; <br>
 * Use &uarr; and &darr; to shift the plot up and down; <br>
 * Press the SHIFT key to increase the shift by a factor of 10.
 * 
 * @author Gustavo H. Sberze Ribas (CPqD), Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class ChartPanelShiftController implements MouseMotionListener, MouseListener {

	/** PAN plot by a fixed percentual of the range (eg. 1%) */
	public static final int SHIFT_PERCENTUAL = 1;

	/** PAN plot by a fixed number of pixels (eg. 1px) */
	public static final int SHIFT_PIXEL = 2;

	/** PAN plot by a fixed amout (eg. 5 range units) */
	public static final int SHIFT_FIXED = 3;

	/** The chart panel we're using */
	private ChartPanel chartPanel;

	/** Does this plot supports shifting? Pie charts for example don't. */
	private boolean plotSupported = false;

	/** The shift type. (default {@link #SHIFT_PIXEL} ) */
	private int shiftType = SHIFT_PIXEL;

	/** Fixed shift amount for domain axis */
	private double fixedDomainShiftUnits;
	/** Fixed shift amount for range axis */
	private double fixedRangeShiftUnits;

	private int oldx = -1;
	private int oldy = -1;

	/**
	 * By default we assume that the range axis is the vertical one (ie, PlotOrientation.VERTICAL
	 * (axesSwaped=false). If the range axis is the horizontal one (ie, PlotOrientation.HORIZONTAL)
	 * this variable should be set to true.
	 */
	private boolean axesSwaped = false;

	private boolean onlyXShift = false;

	/**
	 * Creates a new controller to handle plot shifts.
	 * 
	 * @param chartPanel
	 *            The panel displaying the plot.
	 */
	public ChartPanelShiftController(ChartPanel chartPanel) {
		super();
		this.chartPanel = chartPanel;

		// Check to see if plot is shiftable
		Plot plot = chartPanel.getChart().getPlot();
		if ((plot instanceof XYPlot) || (plot instanceof FastScatterPlot)) {
			plotSupported = true;
			axesSwaped = isHorizontalPlot(plot);
		}
	}

	public void setOnlyXShift(boolean onlyXShift) {
		this.onlyXShift = onlyXShift;
	}

	/**
	 * Returns the plot orientation.
	 * 
	 * @return True = {@link org.jfree.chart.plot.PlotOrientation#VERTICAL VERTICAL}; False =
	 *         {@link org.jfree.chart.plot.PlotOrientation#HORIZONTAL HORIZONTAL}
	 */
	protected boolean isHorizontalPlot(Plot plot) {
		if (plot instanceof XYPlot) {
			return ((XYPlot) plot).getOrientation() == PlotOrientation.HORIZONTAL;
		}
		if (plot instanceof FastScatterPlot) {
			return ((FastScatterPlot) plot).getOrientation() == PlotOrientation.HORIZONTAL;
		}
		return false;
	}

	/**
	 * Returns the ValueAxis for the plot or <code>null</code> if the plot doesn't have one.
	 * 
	 * @param chart
	 *            The chart
	 * @param domain
	 *            True = get Domain axis. False = get Range axis.
	 * @return The selected ValueAxis or <code>null</code> if the plot doesn't have one.
	 */
	protected ValueAxis[] getPlotAxis(JFreeChart chart, boolean domain) {
		// Where's the Shiftable interface when we need it ?? ;)
		Plot plot = chart.getPlot();
		if (plot instanceof XYPlot) {
			XYPlot xyPlot = (XYPlot) plot;
			// return domain ? ((XYPlot) plot).getDomainAxis() : ((XYPlot) plot).getRangeAxis();
			if (domain) {
				ValueAxis[] rangeAxes = new ValueAxis[xyPlot.getDomainAxisCount()];
				for (int i = 0; i < rangeAxes.length; i++) {
					rangeAxes[i] = xyPlot.getDomainAxis(i);
				}
				return rangeAxes;
			} else {
				ValueAxis[] rangeAxes = new ValueAxis[xyPlot.getRangeAxisCount()];
				for (int i = 0; i < rangeAxes.length; i++) {
					rangeAxes[i] = xyPlot.getRangeAxis(i);
				}
				return rangeAxes;
			}
		}
		if (plot instanceof FastScatterPlot) {
			return domain ? new ValueAxis[] { ((FastScatterPlot) plot).getDomainAxis() }
					: new ValueAxis[] { ((FastScatterPlot) plot).getRangeAxis() };
		}
		return null;
	}

	/**
	 * Pan / Shifts a plot if the arrow keys are pressed.
	 */
	public void keyPressed(KeyEvent e) {
		if (!plotSupported) {
			return;
		}

		int keyCode = e.getKeyCode();

		// we're only interested in arrows (code 37,38,39,40)
		if ((keyCode < 37) || (keyCode > 40)) {
			return;
		}

		// The axes we're gonna shift
		ValueAxis[] axes = null;

		boolean domainShift = false; // used for PAN_FIXED
		// Calculations for the domain axis
		if ((keyCode == KeyEvent.VK_LEFT) || (keyCode == KeyEvent.VK_RIGHT)) {
			axes = getPlotAxis(chartPanel.getChart(), !axesSwaped);
			domainShift = true;
		}
		// Calculations for the range axis
		else {
			axes = getPlotAxis(chartPanel.getChart(), axesSwaped);
		}

		// Delta is the amount we'll shift in axes units.
		double[] delta = new double[axes.length];

		// Let's calculate 'delta', the amount by which we'll shift the plot
		for (int i = 0; i < axes.length; i++) {
			switch (shiftType) {
				case SHIFT_PERCENTUAL:
					delta[i] = (axes[i].getUpperBound() - axes[i].getLowerBound()) / 100.0;
					break;
				case SHIFT_FIXED:
					delta[i] = (domainShift ? fixedDomainShiftUnits : fixedRangeShiftUnits);
					break;
				case SHIFT_PIXEL: // also the default
				default:
					// Let's find out what's the range for 1 pixel.
					final Rectangle2D scaledDataArea = chartPanel.getScreenDataArea();
					delta[i] = axes[i].getRange().getLength() / (scaledDataArea.getWidth());
					break;
			}
		}

		// Shift modifier multiplies delta by 10
		if (e.isShiftDown()) {
			for (int i = 0; i < delta.length; i++) {
				delta[i] *= 10;
			}
		}

		for (int i = 0; i < axes.length; i++) {
			switch (keyCode) {
				case KeyEvent.VK_LEFT:
				case KeyEvent.VK_DOWN:
					axes[i].setRange(axes[i].getLowerBound() - delta[i], axes[i].getUpperBound() - delta[i]);
					break;
				case KeyEvent.VK_UP:
				case KeyEvent.VK_RIGHT:
					axes[i].setRange(axes[i].getLowerBound() + delta[i], axes[i].getUpperBound() + delta[i]);
					break;
			}
		}
	}

	@Override
	public void mouseDragged(MouseEvent mouseEvent) {
		if (!mouseEvent.isControlDown()) {
			return;
		}

		if (oldx > -1 && oldy > -1) {
			int xdif = mouseEvent.getX() - oldx;
			int ydif = mouseEvent.getY() - oldy;

			final Rectangle2D scaledDataArea = chartPanel.getScreenDataArea();

			ValueAxis[] domAxes = getPlotAxis(chartPanel.getChart(), !axesSwaped);
			if (domAxes != null) {
				double[] xDelta = new double[domAxes.length];
				for (int i = 0; i < domAxes.length; i++) {
					xDelta[i] = xdif * domAxes[i].getRange().getLength() / (scaledDataArea.getWidth());
				}
				for (int i = 0; i < domAxes.length; i++) {
					domAxes[i].setRange(domAxes[i].getLowerBound() - xDelta[i], domAxes[i].getUpperBound() - xDelta[i]);
				}
			}

			ValueAxis[] rngAxes = getPlotAxis(chartPanel.getChart(), axesSwaped);
			if (rngAxes != null) {
				double[] yDelta = new double[rngAxes.length];
				for (int i = 0; i < rngAxes.length; i++) {
					yDelta[i] = ydif * rngAxes[i].getRange().getLength() / (scaledDataArea.getHeight());
				}
				if (!onlyXShift) {
					for (int i = 0; i < rngAxes.length; i++) {
						rngAxes[i].setRange(rngAxes[i].getLowerBound() + yDelta[i], rngAxes[i].getUpperBound() + yDelta[i]);
					}
				}
			}
		}

		oldx = mouseEvent.getX();
		oldy = mouseEvent.getY();
	}

	@Override
	public void mouseMoved(MouseEvent mouseEvent) {}

	@Override
	public void mouseReleased(MouseEvent mouseEvent) {
		oldx = -1;
		oldy = -1;
	}

	@Override
	public void mouseClicked(MouseEvent mouseEvent) {}

	@Override
	public void mousePressed(MouseEvent mouseEvent) {}

	@Override
	public void mouseEntered(MouseEvent mouseEvent) {}

	@Override
	public void mouseExited(MouseEvent mouseEvent) {}

	/**
	 * Returns the fixed shift step for the domain axis.
	 * 
	 * @return the fixed shift step for the domain axis.
	 */
	public double getFixedDomainShiftUnits() {
		return fixedDomainShiftUnits;
	}

	/**
	 * Sets the fixed shift step for the domain axis.
	 * 
	 * @param fixedDomainShiftUnits
	 *            the fixed shift step for the domain axis.
	 */
	public void setFixedDomainShiftUnits(double fixedDomainShiftUnits) {
		this.fixedDomainShiftUnits = fixedDomainShiftUnits;
	}

	/**
	 * Returns the fixed shift step for the range axis.
	 * 
	 * @return the fixed shift step for the range axis.
	 */
	public double getFixedRangeShiftUnits() {
		return fixedRangeShiftUnits;
	}

	/**
	 * Sets the fixed shift step for the range axis.
	 * 
	 * @param fixedRangeShiftUnits
	 *            the fixed shift step for the range axis.
	 */
	public void setFixedRangeShiftUnits(double fixedRangeShiftUnits) {
		this.fixedRangeShiftUnits = fixedRangeShiftUnits;
	}

	/**
	 * Returns the current shift type.
	 * 
	 * @return the current shift type.
	 * @see #SHIFT_FIXED
	 * @see #SHIFT_PERCENTUAL
	 * @see #SHIFT_PIXEL
	 */
	public int getShiftType() {
		return shiftType;
	}

	/**
	 * Sets the shift type.
	 * 
	 * @param shiftType
	 *            the new shift type.
	 * @see #SHIFT_FIXED
	 * @see #SHIFT_PERCENTUAL
	 * @see #SHIFT_PIXEL
	 */
	public void setShiftType(int shiftType) {
		this.shiftType = shiftType;
	}

	/**
	 * Returns whether or not the plot supports shifting.
	 * 
	 * @return True if plot can be shifted.
	 */
	public boolean isPlotSupported() {
		return plotSupported;
	}
}
