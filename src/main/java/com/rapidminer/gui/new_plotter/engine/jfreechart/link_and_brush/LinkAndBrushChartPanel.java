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
package com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush;

import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.listener.LinkAndBrushSelection;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.listener.LinkAndBrushSelection.SelectionType;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.listener.LinkAndBrushSelectionListener;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.plots.LinkAndBrushPlot;
import com.rapidminer.gui.plotter.CoordinateTransformation;
import com.rapidminer.gui.plotter.NullCoordinateTransformation;
import com.rapidminer.tools.container.Pair;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Pannable;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;


/**
 * A Swing GUI component for displaying a {@link JFreeChart} object. The chart will be buffered.
 * <P>
 * 
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class LinkAndBrushChartPanel extends ChartPanel {

	private static final long serialVersionUID = 1L;
	private boolean zoomOnLinkAndBrushSelection;
	private boolean blockSelectionOrZoom = false;

	/**
	 * This is a transformation which transforms the components coordinates to screen coordinates.
	 * If is null, no transformation is needed.
	 */
	private transient CoordinateTransformation coordinateTransformation = new NullCoordinateTransformation();

	private transient List<WeakReference<LinkAndBrushSelectionListener>> listeners = new LinkedList<WeakReference<LinkAndBrushSelectionListener>>();

	public LinkAndBrushChartPanel(JFreeChart chart, boolean zoomOnLinkAndBrushSelection) {
		super(chart, 600, 400, 200, 133, DEFAULT_MAXIMUM_DRAW_WIDTH, DEFAULT_MAXIMUM_DRAW_HEIGHT, DEFAULT_BUFFER_USED,
				false,  // copy
				false,  // properties
				false,  // save
				false,  // print
				false,  // zoom
				true);   // tooltips

		this.zoomOnLinkAndBrushSelection = zoomOnLinkAndBrushSelection;
		setInitialDelay(200);

		setMouseWheelEnabled(false);
	}

	public LinkAndBrushChartPanel(JFreeChart chart, int defaultWidth, int defaultHeigth, int minDrawWidth,
			int minDrawHeigth, boolean zoomOnLinkAndBrush) {
		super(chart, defaultWidth, defaultHeigth, minDrawWidth, minDrawHeigth, DEFAULT_MAXIMUM_DRAW_WIDTH,
				DEFAULT_MAXIMUM_DRAW_HEIGHT, DEFAULT_BUFFER_USED, false,  // copy
				false,  // properties
				false,  // save
				false,  // print
				false,  // zoom
				true);   // tooltips

		this.zoomOnLinkAndBrushSelection = zoomOnLinkAndBrush;
		setInitialDelay(200);

		setMouseWheelEnabled(false);
	}

	public LinkAndBrushChartPanel(JFreeChart chart, int defaultWidth, int defaultHeigth, int minDrawWidth,
			int minDrawHeigth, boolean zoomOnLinkAndBrush, boolean useBuffer) {
		super(chart, defaultWidth, defaultHeigth, minDrawWidth, minDrawHeigth, DEFAULT_MAXIMUM_DRAW_WIDTH,
				DEFAULT_MAXIMUM_DRAW_HEIGHT, useBuffer, false,  // copy
				false,  // properties
				false,  // save
				false,  // print
				false,  // zoom
				true);   // tooltips

		this.zoomOnLinkAndBrushSelection = zoomOnLinkAndBrush;

		setMouseWheelEnabled(false);
	}

	@Override
	public void restoreAutoBounds() {
		Plot plot = getChart().getPlot();
		if (plot == null) {
			return;
		}
		// here we tweak the notify flag on the plot so that only
		// one notification happens even though we update multiple
		// axes...
		boolean savedNotify = plot.isNotify();
		plot.setNotify(false);

		if (plot instanceof LinkAndBrushPlot) {

			LinkAndBrushPlot LABPlot = (LinkAndBrushPlot) plot;

			List<Pair<Integer, Range>> zoomedDomainAxisRanges = new LinkedList<Pair<Integer, Range>>();
			List<Pair<Integer, Range>> zoomedRangeAxisRanges = new LinkedList<Pair<Integer, Range>>();

			zoomedDomainAxisRanges.addAll(LABPlot.restoreAutoDomainAxisBounds(zoomOnLinkAndBrushSelection));
			zoomedRangeAxisRanges.addAll(LABPlot.restoreAutoRangeAxisBounds(zoomOnLinkAndBrushSelection));

			if (zoomOnLinkAndBrushSelection) {
				informLinkAndBrushSelectionListeners(new LinkAndBrushSelection(SelectionType.RESTORE_AUTO_BOUNDS,
						zoomedDomainAxisRanges, zoomedRangeAxisRanges));
			} else {
				informLinkAndBrushSelectionListeners(new LinkAndBrushSelection(SelectionType.RESTORE_SELECTION,
						zoomedDomainAxisRanges, zoomedRangeAxisRanges));
			}

		} else {
			restoreAutoDomainBounds();
			restoreAutoRangeBounds();
		}

		plot.setNotify(savedNotify);
	}

	@Override
	public void zoom(Rectangle2D selection) {
		// get the origin of the zoom selection in the Java2D space used for
		// drawing the chart (that is, before any scaling to fit the panel)
		Point2D selectOrigin = translateScreenToJava2D(new Point((int) Math.ceil(selection.getX()),
				(int) Math.ceil(selection.getY())));
		PlotRenderingInfo plotInfo = getChartRenderingInfo().getPlotInfo();
		Rectangle2D scaledDataArea = getScreenDataArea((int) selection.getCenterX(), (int) selection.getCenterY());
		if ((selection.getHeight() > 0) && (selection.getWidth() > 0)) {

			double hLower = (selection.getMinX() - scaledDataArea.getMinX()) / scaledDataArea.getWidth();
			double hUpper = (selection.getMaxX() - scaledDataArea.getMinX()) / scaledDataArea.getWidth();
			double vLower = (scaledDataArea.getMaxY() - selection.getMaxY()) / scaledDataArea.getHeight();
			double vUpper = (scaledDataArea.getMaxY() - selection.getMinY()) / scaledDataArea.getHeight();

			Plot p = getChart().getPlot();
			if (p instanceof LinkAndBrushPlot) {

				PlotOrientation orientation = null;
				if (p instanceof XYPlot) {
					XYPlot xyPlot = (XYPlot) p;
					orientation = xyPlot.getOrientation();
				}
				if (p instanceof CategoryPlot) {
					CategoryPlot categoryPlot = (CategoryPlot) p;
					orientation = categoryPlot.getOrientation();
				}

				// here we tweak the notify flag on the plot so that only
				// one notification happens even though we update multiple
				// axes...

				boolean savedNotify = p.isNotify();
				p.setNotify(false);
				LinkAndBrushPlot LABPlot = (LinkAndBrushPlot) p;

				List<Pair<Integer, Range>> zoomedDomainAxisRanges = new LinkedList<Pair<Integer, Range>>();
				List<Pair<Integer, Range>> zoomedRangeAxisRanges = new LinkedList<Pair<Integer, Range>>();

				if (orientation == PlotOrientation.HORIZONTAL) {
					zoomedDomainAxisRanges.addAll(LABPlot.calculateDomainAxesZoom(vLower, vUpper,
							zoomOnLinkAndBrushSelection));
					zoomedRangeAxisRanges.addAll(LABPlot.calculateRangeAxesZoom(hLower, hUpper, plotInfo, selectOrigin,
							zoomOnLinkAndBrushSelection));
				} else {
					zoomedDomainAxisRanges.addAll(LABPlot.calculateDomainAxesZoom(hLower, hUpper,
							zoomOnLinkAndBrushSelection));
					zoomedRangeAxisRanges.addAll(LABPlot.calculateRangeAxesZoom(vLower, vUpper, plotInfo, selectOrigin,
							zoomOnLinkAndBrushSelection));
				}
				p.setNotify(savedNotify);

				if (zoomOnLinkAndBrushSelection) {
					informLinkAndBrushSelectionListeners(new LinkAndBrushSelection(SelectionType.ZOOM_IN,
							zoomedDomainAxisRanges, zoomedRangeAxisRanges));
				} else {
					informLinkAndBrushSelectionListeners(new LinkAndBrushSelection(SelectionType.SELECTION,
							zoomedDomainAxisRanges, zoomedRangeAxisRanges));
				}

			} else {
				super.zoom(selection);
			}
		}
	}

	/**
	 * If set to <code>true</code>, will zoom on selection, otherwise will do a selection.
	 * 
	 * @param zoomOnLinkAndBrushSelection
	 */
	public void setZoomOnLinkAndBrushSelection(boolean zoomOnLinkAndBrushSelection) {
		this.zoomOnLinkAndBrushSelection = zoomOnLinkAndBrushSelection;
	}

	/**
	 * Returns whether the panel will zoom on selection or not.
	 *
	 * @return
	 */
	public boolean getZoomOnLinkAndBrushSelection() {
		return zoomOnLinkAndBrushSelection;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (getChart() == null) {
			return;
		}
		Graphics2D g2 = (Graphics2D) g.create();

		// first determine the size of the chart rendering area...
		Dimension size = getSize();
		Insets insets = getInsets();
		Rectangle2D available = new Rectangle2D.Double(insets.left, insets.top,
				size.getWidth() - insets.left - insets.right, size.getHeight() - insets.top - insets.bottom);

		// work out if scaling is required...
		boolean scale = false;
		double drawWidth = available.getWidth();
		double drawHeight = available.getHeight();
		setChartFieldValue(getChartFieldByName("scaleX"), 1.0);
		// this.scaleX = 1.0;
		setChartFieldValue(getChartFieldByName("scaleY"), 1.0);
		// this.scaleY = 1.0;

		if (drawWidth < getMinimumDrawWidth()) {
			setChartFieldValue(getChartFieldByName("scaleX"), drawWidth / getMinimumDrawWidth());
			// this.scaleX = drawWidth / getMinimumDrawWidth();
			drawWidth = getMinimumDrawWidth();
			scale = true;
		} else if (drawWidth > getMaximumDrawWidth()) {
			setChartFieldValue(getChartFieldByName("scaleX"), drawWidth / getMaximumDrawWidth());
			// this.scaleX = drawWidth / getMaximumDrawWidth();
			drawWidth = getMaximumDrawWidth();
			scale = true;
		}

		if (drawHeight < getMinimumDrawHeight()) {
			setChartFieldValue(getChartFieldByName("scaleY"), drawHeight / getMinimumDrawHeight());
			// this.scaleY = drawHeight / getMinimumDrawHeight();
			drawHeight = getMinimumDrawHeight();
			scale = true;
		} else if (drawHeight > getMaximumDrawHeight()) {
			setChartFieldValue(getChartFieldByName("scaleY"), drawHeight / getMaximumDrawHeight());
			// this.scaleY = drawHeight / getMaximumDrawHeight();
			drawHeight = getMaximumDrawHeight();
			scale = true;
		}

		Rectangle2D chartArea = new Rectangle2D.Double(0.0, 0.0, drawWidth, drawHeight);

		// are we using the chart buffer?
		if ((Boolean) getChartFieldValueByName("useBuffer")) {

			// do we need to resize the buffer?
			if ((getChartFieldValueByName("chartBuffer") == null)
					|| ((Integer) getChartFieldValueByName("chartBufferWidth") != available.getWidth())
					|| ((Integer) getChartFieldValueByName("chartBufferHeight") != available.getHeight())) {
				setChartFieldValue(getChartFieldByName("chartBufferWidth"), (int) available.getWidth());
				// this.chartBufferWidth = (int) available.getWidth();
				setChartFieldValue(getChartFieldByName("chartBufferHeight"), (int) available.getHeight());
				// this.chartBufferHeight = (int) available.getHeight();
				GraphicsConfiguration gc = g2.getDeviceConfiguration();
				setChartFieldValue(getChartFieldByName("chartBuffer"), gc.createCompatibleImage(
						(Integer) getChartFieldValueByName("chartBufferWidth"),
						(Integer) getChartFieldValueByName("chartBufferHeight"), Transparency.TRANSLUCENT));
				// this.chartBuffer = gc.createCompatibleImage(this.chartBufferWidth,
				// this.chartBufferHeight, Transparency.TRANSLUCENT);
				setRefreshBuffer(true);
			}

			// do we need to redraw the buffer?
			if (getRefreshBuffer()) {

				setRefreshBuffer(false); // clear the flag

				Rectangle2D bufferArea = new Rectangle2D.Double(0, 0,
						(Integer) getChartFieldValueByName("chartBufferWidth"),
						(Integer) getChartFieldValueByName("chartBufferHeight"));

				Graphics2D bufferG2 = (Graphics2D) ((Image) getChartFieldValueByName("chartBuffer")).getGraphics();
				Rectangle r = new Rectangle(0, 0, (Integer) getChartFieldValueByName("chartBufferWidth"),
						(Integer) getChartFieldValueByName("chartBufferHeight"));
				bufferG2.setPaint(getBackground());
				bufferG2.fill(r);
				if (scale) {
					AffineTransform saved = bufferG2.getTransform();
					AffineTransform st = AffineTransform.getScaleInstance((Double) getChartFieldValueByName("scaleX"),
							(Double) getChartFieldValueByName("scaleY"));
					bufferG2.transform(st);
					getChart().draw(bufferG2, chartArea, getAnchor(), getChartRenderingInfo());
					bufferG2.setTransform(saved);
				} else {
					getChart().draw(bufferG2, bufferArea, getAnchor(), getChartRenderingInfo());
				}

			}

			// zap the buffer onto the panel...
			g2.drawImage((Image) getChartFieldValueByName("chartBuffer"), insets.left, insets.top, this);

		}

		// or redrawing the chart every time...
		else {

			AffineTransform saved = g2.getTransform();
			g2.translate(insets.left, insets.top);
			if (scale) {
				AffineTransform st = AffineTransform.getScaleInstance((Double) getChartFieldValueByName("scaleX"),
						(Double) getChartFieldValueByName("scaleY"));
				g2.transform(st);
			}
			getChart().draw(g2, chartArea, getAnchor(), getChartRenderingInfo());
			g2.setTransform(saved);

		}

		for (Overlay overlay : (List<Overlay>) getChartFieldValueByName("overlays")) {
			overlay.paintOverlay(g2, this);
		}

		// redraw the zoom rectangle (if present) - if useBuffer is false,
		// we use XOR so we can XOR the rectangle away again without redrawing
		// the chart
		drawZoomRectangle(g2, !(Boolean) getChartFieldValueByName("useBuffer"));

		g2.dispose();

		setAnchor(null);
		setVerticalTraceLine(null);
		setHorizontalTraceLine(null);
	}

	@SuppressWarnings("unchecked")
	public List<Overlay> getOverlayList() {
		return (List<Overlay>) getChartFieldValueByName("overlays");
	}

	public void setOverlayList(List<Overlay> list) {
		setChartFieldValue(getChartFieldByName("overlays"), list);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// this is used to only allow left mouse button zoom / selection
		if (!SwingUtilities.isLeftMouseButton(e)) {
			blockSelectionOrZoom = true;
		} else {
			blockSelectionOrZoom = false;
		}
		super.mousePressed(e);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// when not allowed to zoom / select, return
		if (blockSelectionOrZoom) {
			return;
		}
		// if the popup menu has already been triggered, then ignore dragging...
		if (getChartFieldValueByName("popup") != null && ((JPopupMenu) getChartFieldValueByName("popup")).isShowing()) {
			return;
		}

		// handle panning if we have a start point
		if (getChartFieldValueByName("panLast") != null) {
			double dx = e.getX() - ((Point) getChartFieldValueByName("panLast")).getX();
			double dy = e.getY() - ((Point) getChartFieldValueByName("panLast")).getY();
			if (dx == 0.0 && dy == 0.0) {
				return;
			}
			double wPercent = -dx / ((Double) getChartFieldValueByName("panW"));
			double hPercent = dy / ((Double) getChartFieldValueByName("panH"));
			boolean old = getChart().getPlot().isNotify();
			getChart().getPlot().setNotify(false);
			Pannable p = (Pannable) getChart().getPlot();
			if (p.getOrientation() == PlotOrientation.VERTICAL) {
				p.panDomainAxes(wPercent, getChartRenderingInfo().getPlotInfo(), (Point) getChartFieldValueByName("panLast"));
				p.panRangeAxes(hPercent, getChartRenderingInfo().getPlotInfo(), (Point) getChartFieldValueByName("panLast"));
			} else {
				p.panDomainAxes(hPercent, getChartRenderingInfo().getPlotInfo(), (Point) getChartFieldValueByName("panLast"));
				p.panRangeAxes(wPercent, getChartRenderingInfo().getPlotInfo(), (Point) getChartFieldValueByName("panLast"));
			}
			setChartFieldValue((getChartFieldByName("panLast")), e.getPoint());
			getChart().getPlot().setNotify(old);
			return;
		}

		// if no initial zoom point was set, ignore dragging...
		if (getChartFieldValueByName("zoomPoint") == null) {
			return;
		}
		Graphics2D g2 = (Graphics2D) getGraphics();

		// erase the previous zoom rectangle (if any). We only need to do
		// this is we are using XOR mode, which we do when we're not using
		// the buffer (if there is a buffer, then at the end of this method we
		// just trigger a repaint)
		if (!(Boolean) getChartFieldValueByName("useBuffer")) {
			drawZoomRectangle(g2, true);
		}

		boolean hZoom = false;
		boolean vZoom = false;
		if ((PlotOrientation) getChartFieldValueByName("orientation") == PlotOrientation.HORIZONTAL) {
			hZoom = (Boolean) getChartFieldValueByName("rangeZoomable");
			vZoom = (Boolean) getChartFieldValueByName("domainZoomable");
		} else {
			hZoom = (Boolean) getChartFieldValueByName("domainZoomable");
			vZoom = (Boolean) getChartFieldValueByName("rangeZoomable");
		}
		Point2D zoomPoint = (Point2D) getChartFieldValueByName("zoomPoint");
		Rectangle2D scaledDataArea = getScreenDataArea((int) zoomPoint.getX(), (int) zoomPoint.getY());
		if (hZoom && vZoom) {
			// selected rectangle shouldn't extend outside the data area...
			double xmax = Math.min(e.getX(), scaledDataArea.getMaxX());
			double ymax = Math.min(e.getY(), scaledDataArea.getMaxY());
			setChartFieldValue(
					getChartFieldByName("zoomRectangle"),
					new Rectangle2D.Double(zoomPoint.getX(), zoomPoint.getY(), xmax - zoomPoint.getX(), ymax
							- zoomPoint.getY()));
		} else if (hZoom) {
			double xmax = Math.min(e.getX(), scaledDataArea.getMaxX());
			setChartFieldValue(getChartFieldByName("zoomRectangle"),
					new Rectangle2D.Double(zoomPoint.getX(), scaledDataArea.getMinY(), xmax - zoomPoint.getX(),
							scaledDataArea.getHeight()));
		} else if (vZoom) {
			double ymax = Math.min(e.getY(), scaledDataArea.getMaxY());
			setChartFieldValue(getChartFieldByName("zoomRectangle"), new Rectangle2D.Double(scaledDataArea.getMinX(),
					zoomPoint.getY(), scaledDataArea.getWidth(), ymax - zoomPoint.getY()));
		}

		// Draw the new zoom rectangle...
		if ((Boolean) getChartFieldValueByName("useBuffer")) {
			repaint();
		} else {
			// with no buffer, we use XOR to draw the rectangle "over" the
			// chart...
			drawZoomRectangle(g2, true);
		}
		g2.dispose();

	}

	@Override
	public void mouseReleased(MouseEvent e) {

		// if we've been panning, we need to reset now that the mouse is
		// released...
		Rectangle2D zoomRectangle = (Rectangle2D) getChartFieldValueByName("zoomRectangle");
		Point2D zoomPoint = (Point2D) getChartFieldValueByName("zoomPoint");
		if (getChartFieldValueByName("panLast") != null) {
			setChartFieldValue(getChartFieldByName("panLast"), null);
			setCursor(Cursor.getDefaultCursor());
		} else if (zoomRectangle != null) {
			boolean hZoom = false;
			boolean vZoom = false;
			if ((PlotOrientation) getChartFieldValueByName("orientation") == PlotOrientation.HORIZONTAL) {
				hZoom = (Boolean) getChartFieldValueByName("rangeZoomable");
				vZoom = (Boolean) getChartFieldValueByName("domainZoomable");
			} else {
				hZoom = (Boolean) getChartFieldValueByName("domainZoomable");
				vZoom = (Boolean) getChartFieldValueByName("rangeZoomable");
			}

			boolean zoomTrigger1 = hZoom
					&& Math.abs(e.getX() - zoomPoint.getX()) >= (Integer) getChartFieldValueByName("zoomTriggerDistance");
			boolean zoomTrigger2 = vZoom
					&& Math.abs(e.getY() - zoomPoint.getY()) >= (Integer) getChartFieldValueByName("zoomTriggerDistance");
			if (zoomTrigger1 || zoomTrigger2) {
				if (hZoom && e.getX() < zoomPoint.getX() || vZoom && e.getY() < zoomPoint.getY()) {
					restoreAutoBounds();
				} else {
					double x, y, w, h;
					Rectangle2D screenDataArea = getScreenDataArea((int) zoomPoint.getX(), (int) zoomPoint.getY());
					double maxX = screenDataArea.getMaxX();
					double maxY = screenDataArea.getMaxY();
					// for mouseReleased event, (horizontalZoom || verticalZoom)
					// will be true, so we can just test for either being false;
					// otherwise both are true
					if (!vZoom) {
						x = zoomPoint.getX();
						y = screenDataArea.getMinY();
						w = Math.min(zoomRectangle.getWidth(), maxX - zoomPoint.getX());
						h = screenDataArea.getHeight();
					} else if (!hZoom) {
						x = screenDataArea.getMinX();
						y = zoomPoint.getY();
						w = screenDataArea.getWidth();
						h = Math.min(zoomRectangle.getHeight(), maxY - zoomPoint.getY());
					} else {
						x = zoomPoint.getX();
						y = zoomPoint.getY();
						w = Math.min(zoomRectangle.getWidth(), maxX - zoomPoint.getX());
						h = Math.min(zoomRectangle.getHeight(), maxY - zoomPoint.getY());
					}
					Rectangle2D zoomArea = new Rectangle2D.Double(x, y, w, h);
					zoom(zoomArea);
				}
				setChartFieldValue(getChartFieldByName("zoomPoint"), null);
				setChartFieldValue(getChartFieldByName("zoomRectangle"), null);
			} else {
				// erase the zoom rectangle
				Graphics2D g2 = (Graphics2D) getGraphics();
				if ((Boolean) getChartFieldValueByName("useBuffer")) {
					repaint();
				} else {
					drawZoomRectangle(g2, true);
				}
				g2.dispose();
				setChartFieldValue(getChartFieldByName("zoomPoint"), null);
				setChartFieldValue(getChartFieldByName("zoomRectangle"), null);
			}

		}

		else if (e.isPopupTrigger()) {
			if (getChartFieldValueByName("popup") != null) {
				displayPopupMenu(e.getX(), e.getY());
			}
		}

	}

	/**
	 * Draws zoom rectangle (if present). The drawing is performed in XOR mode, therefore when this
	 * method is called twice in a row, the second call will completely restore the state of the
	 * canvas.
	 * 
	 * @param g2
	 *            the graphics device.
	 * @param xor
	 *            use XOR for drawing?
	 */
	private void drawZoomRectangle(Graphics2D g2, boolean xor) {
		Rectangle2D zoomRectangle = (Rectangle2D) getChartFieldValueByName("zoomRectangle");
		if (zoomRectangle != null) {
			// fix rectangle parameters when chart is transformed
			zoomRectangle = coordinateTransformation.transformRectangle(zoomRectangle, this);
			if (!(coordinateTransformation instanceof NullCoordinateTransformation)) {
				g2 = coordinateTransformation.getTransformedGraphics(this);
			}
			if (xor) {
				// Set XOR mode to draw the zoom rectangle
				g2.setXORMode(Color.gray);
			}
			if ((Boolean) getChartFieldValueByName("fillZoomRectangle")) {
				g2.setPaint((Paint) getChartFieldValueByName("zoomFillPaint"));
				g2.fill(zoomRectangle);
			} else {
				g2.setPaint((Paint) getChartFieldValueByName("zoomOutlinePaint"));
				g2.draw(zoomRectangle);
			}
			if (xor) {
				// Reset to the default 'overwrite' mode
				g2.setPaintMode();
			}
		}
	}

	/**
	 * Add a {@link LinkAndBrushSelectionListener}. The listener is saved as a {@link WeakReference}
	 * . Thus listener must not be hidden (anonymous) classes!
	 */
	public void addLinkAndBrushSelectionListener(LinkAndBrushSelectionListener l) {
		listeners.add(new WeakReference<LinkAndBrushSelectionListener>(l));
	}

	/**
	 * Remove a {@link LinkAndBrushSelectionListener}.
	 * 
	 * @param l
	 */
	public void removeLinkAndBrushSelectionListener(LinkAndBrushSelectionListener l) {
		Iterator<WeakReference<LinkAndBrushSelectionListener>> it = listeners.iterator();
		while (it.hasNext()) {
			WeakReference<LinkAndBrushSelectionListener> wrl = it.next();
			LinkAndBrushSelectionListener listener = wrl.get();
			if (listener == l || listener == null) {
				it.remove();
			}
		}
	}

	/**
	 * Informs all {@link LinkAndBrushSelectionListener} of a {@link LinkAndBrushSelection}.
	 * 
	 * @param e
	 */
	public void informLinkAndBrushSelectionListeners(LinkAndBrushSelection e) {
		// create a copy to avoid ConcurrentModificationException when informing and remove listener
		// happens
		List<WeakReference<LinkAndBrushSelectionListener>> listenersCopy = new LinkedList<WeakReference<LinkAndBrushSelectionListener>>();
		listenersCopy.addAll(listeners);
		Iterator<WeakReference<LinkAndBrushSelectionListener>> it = listenersCopy.iterator();
		while (it.hasNext()) {
			WeakReference<LinkAndBrushSelectionListener> wrl = it.next();
			LinkAndBrushSelectionListener l = wrl.get();
			if (l != null) {
				l.selectedLinkAndBrushRectangle(e);
			} else {
				it.remove();
			}
		}
	}

	/**
	 * Returns the value of a {@link Field} of the {@link ChartPanel}. If the field can not be
	 * found, returns {@code null}.
	 * 
	 * @param name
	 *            the name of the {@link Field}
	 * @return
	 */
	private Object getChartFieldValueByName(String name) {
		try {
			Field field = ChartPanel.class.getDeclaredField(name);
			field.setAccessible(true);
			Object value = field.get(this);
			field.setAccessible(false);
			return value;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns the {@link Field} of the {@link ChartPanel}. If the field can not be found, returns
	 * {@code null}.
	 * 
	 * @param name
	 *            the name of the {@link Field}
	 * @return
	 */
	private Field getChartFieldByName(String name) {
		try {
			Field field = ChartPanel.class.getDeclaredField(name);
			field.setAccessible(true);
			return field;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Sets the given value for the given {@link Field}.
	 * 
	 * @param field
	 *            the {@link Field} to change
	 * @param value
	 *            the new value for the {@link Field}
	 */
	private void setChartFieldValue(Field field, Object value) {
		try {
			field.setAccessible(true);
			field.set(this, value);
			field.setAccessible(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method sets the coordinate transformation for this component.
	 */
	public void setCoordinateTransformation(CoordinateTransformation transformation) {
		this.coordinateTransformation = transformation;
	}
}
