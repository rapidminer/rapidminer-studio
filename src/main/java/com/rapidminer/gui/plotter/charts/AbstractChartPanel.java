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

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import javax.swing.event.EventListenerList;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartTransferable;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.editor.ChartEditor;
import org.jfree.chart.editor.ChartEditorManager;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.OverlayChangeEvent;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.Pannable;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.Zoomable;
import org.jfree.data.Range;
import org.jfree.ui.ExtensionFileFilter;
import org.jfree.util.ResourceBundleWrapper;

import com.rapidminer.gui.actions.export.PrintableComponent;
import com.rapidminer.gui.plotter.AxisNameResolver;
import com.rapidminer.gui.plotter.CoordinateTransformation;
import com.rapidminer.gui.plotter.NullCoordinateTransformation;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.container.Pair;


/**
 * A Swing GUI component for displaying a {@link JFreeChart} object.
 * <P>
 * The panel registers with the chart to receive notification of changes to any component of the
 * chart. The chart is redrawn automatically whenever this notification is received.
 * 
 * This version of the ChartPanel provides the possibility to register a JXLayer, which is then
 * asked to resolve coordinates for every direct on screen painting.
 * 
 * @author Sebastian Land
 * @deprecated since 9.2.0
 */
@Deprecated
public class AbstractChartPanel extends ChartPanel implements PrintableComponent {

	public static class Selection {

		private Collection<Pair<String, Range>> selectedRegion;

		public Selection() {
			this.selectedRegion = new LinkedList<>();
		}

		public void addDelimiter(String dimensionName, Range range) {
			selectedRegion.add(new Pair<>(dimensionName, range));
		}

		public Collection<Pair<String, Range>> getDelimiters() {
			return selectedRegion;
		}
	}

	public static interface SelectionListener {

		/**
		 * This method is invoked on all listeners as a selection is made. If the selection was made
		 * by a mouse interaction like drawing a selection rectangle, the selectionEvent will be the
		 * causing mouse event. Otherwise it is null for example if the selection is performed by
		 * gui actions.
		 */
		public void selected(Selection selection, MouseEvent selectionEvent);
	}

	/** For serialization. */
	private static final long serialVersionUID = 6046366297214274674L;

	/** The chart that is displayed in the panel. */
	private JFreeChart chart;

	/**
	 * This is a transformation which transforms the components coordinates to screen coordinates.
	 * If is null, no transformation is needed.
	 */
	private transient CoordinateTransformation coordinateTransformation = new NullCoordinateTransformation();

	/** Storage for registered (chart) mouse listeners. */
	private transient EventListenerList chartMouseListeners;

	/**
	 * The minimum width for drawing a chart (uses scaling for smaller widths).
	 */
	private int minimumDrawWidth;

	/**
	 * The minimum height for drawing a chart (uses scaling for smaller heights).
	 */
	private int minimumDrawHeight;

	/**
	 * The maximum width for drawing a chart (uses scaling for bigger widths).
	 */
	private int maximumDrawWidth;

	/**
	 * The maximum height for drawing a chart (uses scaling for bigger heights).
	 */
	private int maximumDrawHeight;

	/** The popup menu for the frame. */
	private JPopupMenu popup;

	/** The drawing info collected the last time the chart was drawn. */
	private ChartRenderingInfo info;

	/** The chart anchor point. */
	private Point2D anchor;

	/** The scale factor used to draw the chart. */
	private double scaleX;

	/** The scale factor used to draw the chart. */
	private double scaleY;

	/** The plot orientation. */
	private PlotOrientation orientation = PlotOrientation.VERTICAL;

	/** A flag that controls whether or not domain zooming is enabled. */
	private boolean domainZoomable = false;

	/** A flag that controls whether or not range zooming is enabled. */
	private boolean rangeZoomable = false;

	/** This resolves the names of the axes plotted. The default asks this chart for the axis name **/
	private transient AxisNameResolver axisNameResolver = new AxisNameResolver() {

		@Override
		public Collection<String> resolveYAxis(int axisIndex) {
			Plot p = chart.getPlot();
			Collection<String> names = new LinkedList<>();
			if (p instanceof XYPlot) {
				XYPlot plot = (XYPlot) p;
				for (int i = 0; i < plot.getRangeAxisCount(); i++) {
					ValueAxis domain = plot.getRangeAxis(i);
					names.add(domain.getLabel());
				}
			}
			return names;
		}

		@Override
		public Collection<String> resolveXAxis(int axisIndex) {
			Plot p = chart.getPlot();
			Collection<String> names = new LinkedList<>();
			if (p instanceof XYPlot) {
				XYPlot plot = (XYPlot) p;
				for (int i = 0; i < plot.getDomainAxisCount(); i++) {
					ValueAxis domain = plot.getDomainAxis(i);
					names.add(domain.getLabel());
				}
			}
			return names;
		}
	};
	/**
	 * The zoom rectangle starting point (selected by the user with a mouse click). This is a point
	 * on the screen, not the chart (which may have been scaled up or down to fit the panel).
	 */
	private Point2D zoomPoint = null;

	/** The zoom rectangle (selected by the user with the mouse). */
	private transient Rectangle2D selectionRectangle = null;

	/** Controls if the zoom rectangle is drawn as an outline or filled. */
	private boolean fillSelectionRectangle = true;

	/** The minimum distance required to drag the mouse to trigger a zoom. */
	private int zoomTriggerDistance;

	/** A flag that controls whether or not horizontal tracing is enabled. */
	private boolean horizontalAxisTrace = false;

	/** A flag that controls whether or not vertical tracing is enabled. */
	private boolean verticalAxisTrace = false;

	/** A vertical trace line. */
	private transient Line2D verticalTraceLine;

	/** A horizontal trace line. */
	private transient Line2D horizontalTraceLine;

	/** Menu item for zooming in on a chart (both axes). */
	private JMenuItem zoomInBothMenuItem;

	/** Menu item for zooming in on a chart (domain axis). */
	private JMenuItem zoomInDomainMenuItem;

	/** Menu item for zooming in on a chart (range axis). */
	private JMenuItem zoomInRangeMenuItem;

	/** Menu item for zooming out on a chart. */
	private JMenuItem zoomOutBothMenuItem;

	/** Menu item for zooming out on a chart (domain axis). */
	private JMenuItem zoomOutDomainMenuItem;

	/** Menu item for zooming out on a chart (range axis). */
	private JMenuItem zoomOutRangeMenuItem;

	/** Menu item for resetting the zoom (both axes). */
	private JMenuItem zoomResetBothMenuItem;

	/** Menu item for resetting the zoom (domain axis only). */
	private JMenuItem zoomResetDomainMenuItem;

	/** Menu item for resetting the zoom (range axis only). */
	private JMenuItem zoomResetRangeMenuItem;

	/**
	 * The default directory for saving charts to file.
	 * 
	 * @since 1.0.7
	 */
	private File defaultDirectoryForSaveAs;

	/** A flag that controls whether or not file extensions are enforced. */
	private boolean enforceFileExtensions;

	/** A flag that indicates if original tooltip delays are changed. */
	private boolean ownToolTipDelaysActive;

	/** Original initial tooltip delay of ToolTipManager.sharedInstance(). */
	private int originalToolTipInitialDelay;

	/** Original reshow tooltip delay of ToolTipManager.sharedInstance(). */
	private int originalToolTipReshowDelay;

	/** Original dismiss tooltip delay of ToolTipManager.sharedInstance(). */
	private int originalToolTipDismissDelay;

	/** Own initial tooltip delay to be used in this chart panel. */
	private int ownToolTipInitialDelay;

	/** Own reshow tooltip delay to be used in this chart panel. */
	private int ownToolTipReshowDelay;

	/** Own dismiss tooltip delay to be used in this chart panel. */
	private int ownToolTipDismissDelay;

	/** The factor used to zoom in on an axis range. */
	private double zoomInFactor = 0.8;

	/** The factor used to zoom out on an axis range. */
	private double zoomOutFactor = 1.25;

	/**
	 * A flag that controls whether zoom operations are centred on the current anchor point, or the
	 * centre point of the relevant axis.
	 * 
	 * @since 1.0.7
	 */
	private boolean zoomAroundAnchor;

	/**
	 * The paint used to draw the zoom rectangle outline.
	 * 
	 * @since 1.0.13
	 */
	private transient Paint selectionOutlinePaint;

	/**
	 * The zoom fill paint (should use transparency).
	 * 
	 * @since 1.0.13
	 */
	private transient Paint selectionFillPaint;

	/** The resourceBundle for the localization. */
	protected static ResourceBundle localizationResources = ResourceBundleWrapper
			.getBundle("org.jfree.chart.LocalizationBundle");

	/**
	 * Temporary storage for the width and height of the chart drawing area during panning.
	 */
	private double panW, panH;

	/** The last mouse position during panning. */
	private Point panLast;

	/**
	 * The mask for mouse events to trigger panning.
	 * 
	 * @since 1.0.13
	 */
	private int panMask = InputEvent.CTRL_MASK;

	/**
	 * A list of overlays for the panel.
	 * 
	 * @since 1.0.13
	 */
	private List<Overlay> overlays;

	/**
	 * The list of all selection listeners
	 */
	private Collection<SelectionListener> selectionListeners = new LinkedList<>();

	/**
	 * Constructs a panel that displays the specified chart.
	 * 
	 * @param chart
	 *            the chart.
	 */
	public AbstractChartPanel(JFreeChart chart) {

		this(chart, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_MINIMUM_DRAW_WIDTH, DEFAULT_MINIMUM_DRAW_HEIGHT,
				DEFAULT_MAXIMUM_DRAW_WIDTH, DEFAULT_MAXIMUM_DRAW_HEIGHT, DEFAULT_BUFFER_USED, true, // properties
				true, // save
				true, // print
				true, // zoom
				true // tooltips
		);

	}

	/**
	 * Constructs a panel containing a chart. The <code>useBuffer</code> flag controls whether or
	 * not an offscreen <code>BufferedImage</code> is maintained for the chart. If the buffer is
	 * used, more memory is consumed, but panel repaints will be a lot quicker in cases where the
	 * chart itself hasn't changed (for example, when another frame is moved to reveal the panel).
	 * WARNING: If you set the <code>useBuffer</code> flag to false, note that the mouse zooming
	 * rectangle will (in that case) be drawn using XOR, and there is a SEVERE performance problem
	 * with that on JRE6 on Windows.
	 * 
	 * @param chart
	 *            the chart.
	 * @param useBuffer
	 *            a flag controlling whether or not an off-screen buffer is used (read the warning
	 *            above before setting this to <code>false</code>).
	 */
	public AbstractChartPanel(JFreeChart chart, boolean useBuffer) {

		this(chart, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_MINIMUM_DRAW_WIDTH, DEFAULT_MINIMUM_DRAW_HEIGHT,
				DEFAULT_MAXIMUM_DRAW_WIDTH, DEFAULT_MAXIMUM_DRAW_HEIGHT, useBuffer, true, // properties
				true, // save
				true, // print
				true, // zoom
				true // tooltips
		);

	}

	public AbstractChartPanel(JFreeChart chart, int width, int height) {
		this(chart, width, height, 100, 100, Integer.MAX_VALUE, Integer.MAX_VALUE, false, false, false, false, true, true);
	}

	/**
	 * Constructs a JFreeChart panel.
	 * 
	 * @param chart
	 *            the chart.
	 * @param properties
	 *            a flag indicating whether or not the chart property editor should be available via
	 *            the popup menu.
	 * @param save
	 *            a flag indicating whether or not save options should be available via the popup
	 *            menu.
	 * @param print
	 *            a flag indicating whether or not the print option should be available via the
	 *            popup menu.
	 * @param zoom
	 *            a flag indicating whether or not zoom options should be added to the popup menu.
	 * @param tooltips
	 *            a flag indicating whether or not tooltips should be enabled for the chart.
	 */
	public AbstractChartPanel(JFreeChart chart, boolean properties, boolean save, boolean print, boolean zoom,
			boolean tooltips) {

		this(chart, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_MINIMUM_DRAW_WIDTH, DEFAULT_MINIMUM_DRAW_HEIGHT,
				DEFAULT_MAXIMUM_DRAW_WIDTH, DEFAULT_MAXIMUM_DRAW_HEIGHT, DEFAULT_BUFFER_USED, properties, save, print, zoom,
				tooltips);

	}

	/**
	 * Constructs a JFreeChart panel.
	 * 
	 * @param chart
	 *            the chart.
	 * @param width
	 *            the preferred width of the panel.
	 * @param height
	 *            the preferred height of the panel.
	 * @param minimumDrawWidth
	 *            the minimum drawing width.
	 * @param minimumDrawHeight
	 *            the minimum drawing height.
	 * @param maximumDrawWidth
	 *            the maximum drawing width.
	 * @param maximumDrawHeight
	 *            the maximum drawing height.
	 * @param useBuffer
	 *            a flag that indicates whether to use the off-screen buffer to improve performance
	 *            (at the expense of memory).
	 * @param properties
	 *            a flag indicating whether or not the chart property editor should be available via
	 *            the popup menu.
	 * @param save
	 *            a flag indicating whether or not save options should be available via the popup
	 *            menu.
	 * @param print
	 *            a flag indicating whether or not the print option should be available via the
	 *            popup menu.
	 * @param zoom
	 *            a flag indicating whether or not zoom options should be added to the popup menu.
	 * @param tooltips
	 *            a flag indicating whether or not tooltips should be enabled for the chart.
	 */
	public AbstractChartPanel(JFreeChart chart, int width, int height, int minimumDrawWidth, int minimumDrawHeight,
			int maximumDrawWidth, int maximumDrawHeight, boolean useBuffer, boolean properties, boolean save, boolean print,
			boolean zoom, boolean tooltips) {

		this(chart, width, height, minimumDrawWidth, minimumDrawHeight, maximumDrawWidth, maximumDrawHeight, useBuffer,
				properties, true, save, print, zoom, tooltips);
	}

	/**
	 * Constructs a JFreeChart panel.
	 * 
	 * @param chart
	 *            the chart.
	 * @param width
	 *            the preferred width of the panel.
	 * @param height
	 *            the preferred height of the panel.
	 * @param minimumDrawWidth
	 *            the minimum drawing width.
	 * @param minimumDrawHeight
	 *            the minimum drawing height.
	 * @param maximumDrawWidth
	 *            the maximum drawing width.
	 * @param maximumDrawHeight
	 *            the maximum drawing height.
	 * @param useBuffer
	 *            a flag that indicates whether to use the off-screen buffer to improve performance
	 *            (at the expense of memory).
	 * @param properties
	 *            a flag indicating whether or not the chart property editor should be available via
	 *            the popup menu.
	 * @param copy
	 *            a flag indicating whether or not a copy option should be available via the popup
	 *            menu.
	 * @param save
	 *            a flag indicating whether or not save options should be available via the popup
	 *            menu.
	 * @param print
	 *            a flag indicating whether or not the print option should be available via the
	 *            popup menu.
	 * @param zoom
	 *            a flag indicating whether or not zoom options should be added to the popup menu.
	 * @param tooltips
	 *            a flag indicating whether or not tooltips should be enabled for the chart.
	 * 
	 * @since 1.0.13
	 */
	public AbstractChartPanel(JFreeChart chart, int width, int height, int minimumDrawWidth, int minimumDrawHeight,
			int maximumDrawWidth, int maximumDrawHeight, boolean useBuffer, boolean properties, boolean copy, boolean save,
			boolean print, boolean zoom, boolean tooltips) {
		super(chart, width, height, minimumDrawWidth, minimumDrawHeight, maximumDrawWidth, maximumDrawHeight, false,
				properties, copy, save, print, zoom, tooltips);
		setChart(chart);
		this.chartMouseListeners = new EventListenerList();
		this.info = new ChartRenderingInfo();
		setPreferredSize(new Dimension(width, height));

		this.minimumDrawWidth = minimumDrawWidth;
		this.minimumDrawHeight = minimumDrawHeight;
		this.maximumDrawWidth = maximumDrawWidth;
		this.maximumDrawHeight = maximumDrawHeight;
		this.zoomTriggerDistance = DEFAULT_ZOOM_TRIGGER_DISTANCE;

		// set up popup menu...
		this.popup = null;
		if (properties || copy || save || print || zoom) {
			this.popup = createPopupMenu(properties, copy, save, print, zoom);
		}

		enableEvents(AWTEvent.MOUSE_EVENT_MASK);
		enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);
		setDisplayToolTips(tooltips);
		// mouse listener registered in super class
		// addMouseListener(this);
		// addMouseMotionListener(this);

		this.defaultDirectoryForSaveAs = null;
		this.enforceFileExtensions = true;

		// initialize ChartPanel-specific tool tip delays with
		// values the from ToolTipManager.sharedInstance()
		ToolTipManager ttm = ToolTipManager.sharedInstance();
		this.ownToolTipInitialDelay = ttm.getInitialDelay();
		this.ownToolTipDismissDelay = ttm.getDismissDelay();
		this.ownToolTipReshowDelay = ttm.getReshowDelay();

		this.zoomAroundAnchor = false;
		this.selectionOutlinePaint = Color.blue;
		this.selectionFillPaint = new Color(0, 0, 255, 63);

		this.panMask = InputEvent.CTRL_MASK;
		// for MacOSX we can't use the CTRL key for mouse drags, see:
		// http://developer.apple.com/qa/qa2004/qa1362.html
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.startsWith("mac os x")) {
			this.panMask = InputEvent.ALT_MASK;
		}

		this.overlays = new java.util.ArrayList<>();

		// adding wheel listener
		addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (e.getScrollType() != MouseWheelEvent.WHEEL_UNIT_SCROLL) {
					return;
				}
				if (e.getWheelRotation() < 0) {
					shrinkSelectionOnCenter(e.getX(), e.getY(), e);
				} else {
					enlargeSelectionOnCenter(e.getX(), e.getY(), e);
				}
			}
		});
	}

	/**
	 * Returns the chart contained in the panel.
	 * 
	 * @return The chart (possibly <code>null</code>).
	 */

	@Override
	public JFreeChart getChart() {
		return this.chart;
	}

	/**
	 * Sets the chart that is displayed in the panel.
	 * 
	 * @param chart
	 *            the chart (<code>null</code> permitted).
	 */

	@Override
	public void setChart(JFreeChart chart) {

		// stop listening for changes to the existing chart
		if (this.chart != null) {
			this.chart.removeChangeListener(this);
			this.chart.removeProgressListener(this);
		}

		// add the new chart
		this.chart = chart;
		if (chart != null) {
			this.chart.addChangeListener(this);
			this.chart.addProgressListener(this);
			Plot plot = chart.getPlot();
			this.domainZoomable = false;
			this.rangeZoomable = false;
			if (plot instanceof Zoomable) {
				Zoomable z = (Zoomable) plot;
				this.domainZoomable = z.isDomainZoomable();
				this.rangeZoomable = z.isRangeZoomable();
				this.orientation = z.getOrientation();
			}
		} else {
			this.domainZoomable = false;
			this.rangeZoomable = false;
		}

		repaint();

	}

	/**
	 * Returns the minimum drawing width for charts.
	 * <P>
	 * If the width available on the panel is less than this, then the chart is drawn at the minimum
	 * width then scaled down to fit.
	 * 
	 * @return The minimum drawing width.
	 */

	@Override
	public int getMinimumDrawWidth() {
		return this.minimumDrawWidth;
	}

	/**
	 * Sets the minimum drawing width for the chart on this panel.
	 * <P>
	 * At the time the chart is drawn on the panel, if the available width is less than this amount,
	 * the chart will be drawn using the minimum width then scaled down to fit the available space.
	 * 
	 * @param width
	 *            The width.
	 */

	@Override
	public void setMinimumDrawWidth(int width) {
		this.minimumDrawWidth = width;
	}

	/**
	 * Returns the maximum drawing width for charts.
	 * <P>
	 * If the width available on the panel is greater than this, then the chart is drawn at the
	 * maximum width then scaled up to fit.
	 * 
	 * @return The maximum drawing width.
	 */

	@Override
	public int getMaximumDrawWidth() {
		return this.maximumDrawWidth;
	}

	/**
	 * Sets the maximum drawing width for the chart on this panel.
	 * <P>
	 * At the time the chart is drawn on the panel, if the available width is greater than this
	 * amount, the chart will be drawn using the maximum width then scaled up to fit the available
	 * space.
	 * 
	 * @param width
	 *            The width.
	 */

	@Override
	public void setMaximumDrawWidth(int width) {
		this.maximumDrawWidth = width;
	}

	/**
	 * Returns the minimum drawing height for charts.
	 * <P>
	 * If the height available on the panel is less than this, then the chart is drawn at the
	 * minimum height then scaled down to fit.
	 * 
	 * @return The minimum drawing height.
	 */

	@Override
	public int getMinimumDrawHeight() {
		return this.minimumDrawHeight;
	}

	/**
	 * Sets the minimum drawing height for the chart on this panel.
	 * <P>
	 * At the time the chart is drawn on the panel, if the available height is less than this
	 * amount, the chart will be drawn using the minimum height then scaled down to fit the
	 * available space.
	 * 
	 * @param height
	 *            The height.
	 */

	@Override
	public void setMinimumDrawHeight(int height) {
		this.minimumDrawHeight = height;
	}

	/**
	 * Returns the maximum drawing height for charts.
	 * <P>
	 * If the height available on the panel is greater than this, then the chart is drawn at the
	 * maximum height then scaled up to fit.
	 * 
	 * @return The maximum drawing height.
	 */

	@Override
	public int getMaximumDrawHeight() {
		return this.maximumDrawHeight;
	}

	/**
	 * Sets the maximum drawing height for the chart on this panel.
	 * <P>
	 * At the time the chart is drawn on the panel, if the available height is greater than this
	 * amount, the chart will be drawn using the maximum height then scaled up to fit the available
	 * space.
	 * 
	 * @param height
	 *            The height.
	 */

	@Override
	public void setMaximumDrawHeight(int height) {
		this.maximumDrawHeight = height;
	}

	/**
	 * Returns the X scale factor for the chart. This will be 1.0 if no scaling has been used.
	 * 
	 * @return The scale factor.
	 */

	@Override
	public double getScaleX() {
		return this.scaleX;
	}

	/**
	 * Returns the Y scale factory for the chart. This will be 1.0 if no scaling has been used.
	 * 
	 * @return The scale factor.
	 */

	@Override
	public double getScaleY() {
		return this.scaleY;
	}

	/**
	 * Returns the anchor point.
	 * 
	 * @return The anchor point (possibly <code>null</code>).
	 */

	@Override
	public Point2D getAnchor() {
		return this.anchor;
	}

	/**
	 * Sets the anchor point. This method is provided for the use of subclasses, not end users.
	 * 
	 * @param anchor
	 *            the anchor point (<code>null</code> permitted).
	 */

	@Override
	protected void setAnchor(Point2D anchor) {
		this.anchor = anchor;
	}

	/**
	 * Returns the popup menu.
	 * 
	 * @return The popup menu.
	 */

	@Override
	public JPopupMenu getPopupMenu() {
		return this.popup;
	}

	/**
	 * Sets the popup menu for the panel.
	 * 
	 * @param popup
	 *            the popup menu (<code>null</code> permitted).
	 */

	@Override
	public void setPopupMenu(JPopupMenu popup) {
		this.popup = popup;
	}

	/**
	 * Returns the chart rendering info from the most recent chart redraw.
	 * 
	 * @return The chart rendering info.
	 */

	@Override
	public ChartRenderingInfo getChartRenderingInfo() {
		return this.info;
	}

	/**
	 * A convenience method that switches on mouse-based zooming.
	 * 
	 * @param flag
	 *            <code>true</code> enables zooming and rectangle fill on zoom.
	 */

	@Override
	public void setMouseZoomable(boolean flag) {
		setMouseZoomable(flag, true);
	}

	/**
	 * A convenience method that switches on mouse-based zooming.
	 * 
	 * @param flag
	 *            <code>true</code> if zooming enabled
	 * @param fillRectangle
	 *            <code>true</code> if zoom rectangle is filled, false if rectangle is shown as
	 *            outline only.
	 */

	@Override
	public void setMouseZoomable(boolean flag, boolean fillRectangle) {
		setDomainZoomable(flag);
		setRangeZoomable(flag);
		setFillZoomRectangle(fillRectangle);
	}

	/**
	 * Returns the flag that determines whether or not zooming is enabled for the domain axis.
	 * 
	 * @return A boolean.
	 */

	@Override
	public boolean isDomainZoomable() {
		return this.domainZoomable;
	}

	/**
	 * Sets the flag that controls whether or not zooming is enable for the domain axis. A check is
	 * made to ensure that the current plot supports zooming for the domain values.
	 * 
	 * @param flag
	 *            <code>true</code> enables zooming if possible.
	 */

	@Override
	public void setDomainZoomable(boolean flag) {
		if (flag) {
			Plot plot = this.chart.getPlot();
			if (plot instanceof Zoomable) {
				Zoomable z = (Zoomable) plot;
				this.domainZoomable = flag && z.isDomainZoomable();
			}
		} else {
			this.domainZoomable = false;
		}
	}

	/**
	 * Returns the flag that determines whether or not zooming is enabled for the range axis.
	 * 
	 * @return A boolean.
	 */

	@Override
	public boolean isRangeZoomable() {
		return this.rangeZoomable;
	}

	/**
	 * A flag that controls mouse-based zooming on the vertical axis.
	 * 
	 * @param flag
	 *            <code>true</code> enables zooming.
	 */

	@Override
	public void setRangeZoomable(boolean flag) {
		if (flag) {
			Plot plot = this.chart.getPlot();
			if (plot instanceof Zoomable) {
				Zoomable z = (Zoomable) plot;
				this.rangeZoomable = flag && z.isRangeZoomable();
			}
		} else {
			this.rangeZoomable = false;
		}
	}

	/**
	 * Returns the flag that controls whether or not the zoom rectangle is filled when drawn.
	 * 
	 * @return A boolean.
	 */

	@Override
	public boolean getFillZoomRectangle() {
		return this.fillSelectionRectangle;
	}

	/**
	 * A flag that controls how the zoom rectangle is drawn.
	 * 
	 * @param flag
	 *            <code>true</code> instructs to fill the rectangle on zoom, otherwise it will be
	 *            outlined.
	 */

	@Override
	public void setFillZoomRectangle(boolean flag) {
		this.fillSelectionRectangle = flag;
	}

	/**
	 * Returns the zoom trigger distance. This controls how far the mouse must move before a zoom
	 * action is triggered.
	 * 
	 * @return The distance (in Java2D units).
	 */

	@Override
	public int getZoomTriggerDistance() {
		return this.zoomTriggerDistance;
	}

	/**
	 * Sets the zoom trigger distance. This controls how far the mouse must move before a zoom
	 * action is triggered.
	 * 
	 * @param distance
	 *            the distance (in Java2D units).
	 */

	@Override
	public void setZoomTriggerDistance(int distance) {
		this.zoomTriggerDistance = distance;
	}

	/**
	 * Returns the flag that controls whether or not a horizontal axis trace line is drawn over the
	 * plot area at the current mouse location.
	 * 
	 * @return A boolean.
	 */

	@Override
	public boolean getHorizontalAxisTrace() {
		return this.horizontalAxisTrace;
	}

	/**
	 * A flag that controls trace lines on the horizontal axis.
	 * 
	 * @param flag
	 *            <code>true</code> enables trace lines for the mouse pointer on the horizontal
	 *            axis.
	 */

	@Override
	public void setHorizontalAxisTrace(boolean flag) {
		this.horizontalAxisTrace = flag;
	}

	/**
	 * Returns the horizontal trace line.
	 * 
	 * @return The horizontal trace line (possibly <code>null</code>).
	 */

	@Override
	protected Line2D getHorizontalTraceLine() {
		return this.horizontalTraceLine;
	}

	/**
	 * Sets the horizontal trace line.
	 * 
	 * @param line
	 *            the line (<code>null</code> permitted).
	 */

	@Override
	protected void setHorizontalTraceLine(Line2D line) {
		this.horizontalTraceLine = line;
	}

	/**
	 * Returns the flag that controls whether or not a vertical axis trace line is drawn over the
	 * plot area at the current mouse location.
	 * 
	 * @return A boolean.
	 */

	@Override
	public boolean getVerticalAxisTrace() {
		return this.verticalAxisTrace;
	}

	/**
	 * A flag that controls trace lines on the vertical axis.
	 * 
	 * @param flag
	 *            <code>true</code> enables trace lines for the mouse pointer on the vertical axis.
	 */

	@Override
	public void setVerticalAxisTrace(boolean flag) {
		this.verticalAxisTrace = flag;
	}

	/**
	 * Returns the vertical trace line.
	 * 
	 * @return The vertical trace line (possibly <code>null</code>).
	 */

	@Override
	protected Line2D getVerticalTraceLine() {
		return this.verticalTraceLine;
	}

	/**
	 * Sets the vertical trace line.
	 * 
	 * @param line
	 *            the line (<code>null</code> permitted).
	 */

	@Override
	protected void setVerticalTraceLine(Line2D line) {
		this.verticalTraceLine = line;
	}

	/**
	 * Returns the default directory for the "save as" option.
	 * 
	 * @return The default directory (possibly <code>null</code>).
	 * 
	 * @since 1.0.7
	 */

	@Override
	public File getDefaultDirectoryForSaveAs() {
		return this.defaultDirectoryForSaveAs;
	}

	/**
	 * Sets the default directory for the "save as" option. If you set this to <code>null</code>,
	 * the user's default directory will be used.
	 * 
	 * @param directory
	 *            the directory (<code>null</code> permitted).
	 * 
	 * @since 1.0.7
	 */

	@Override
	public void setDefaultDirectoryForSaveAs(File directory) {
		if (directory != null) {
			if (!directory.isDirectory()) {
				throw new IllegalArgumentException("The 'directory' argument is not a directory.");
			}
		}
		this.defaultDirectoryForSaveAs = directory;
	}

	/**
	 * Returns <code>true</code> if file extensions should be enforced, and <code>false</code>
	 * otherwise.
	 * 
	 * @return The flag.
	 * 
	 * @see #setEnforceFileExtensions(boolean)
	 */

	@Override
	public boolean isEnforceFileExtensions() {
		return this.enforceFileExtensions;
	}

	/**
	 * Sets a flag that controls whether or not file extensions are enforced.
	 * 
	 * @param enforce
	 *            the new flag value.
	 * 
	 * @see #isEnforceFileExtensions()
	 */

	@Override
	public void setEnforceFileExtensions(boolean enforce) {
		this.enforceFileExtensions = enforce;
	}

	/**
	 * Returns the flag that controls whether or not zoom operations are centered around the current
	 * anchor point.
	 * 
	 * @return A boolean.
	 * 
	 * @since 1.0.7
	 * 
	 * @see #setZoomAroundAnchor(boolean)
	 */

	@Override
	public boolean getZoomAroundAnchor() {
		return this.zoomAroundAnchor;
	}

	/**
	 * Sets the flag that controls whether or not zoom operations are centered around the current
	 * anchor point.
	 * 
	 * @param zoomAroundAnchor
	 *            the new flag value.
	 * 
	 * @since 1.0.7
	 * 
	 * @see #getZoomAroundAnchor()
	 */

	@Override
	public void setZoomAroundAnchor(boolean zoomAroundAnchor) {
		this.zoomAroundAnchor = zoomAroundAnchor;
	}

	/**
	 * Returns the zoom rectangle fill paint.
	 * 
	 * @return The zoom rectangle fill paint (never <code>null</code>).
	 * 
	 * @see #setZoomFillPaint(java.awt.Paint)
	 * @see #setFillZoomRectangle(boolean)
	 * 
	 * @since 1.0.13
	 */

	@Override
	public Paint getZoomFillPaint() {
		return this.selectionFillPaint;
	}

	/**
	 * Sets the zoom rectangle fill paint.
	 * 
	 * @param paint
	 *            the paint (<code>null</code> not permitted).
	 * 
	 * @see #getZoomFillPaint()
	 * @see #getFillZoomRectangle()
	 * 
	 * @since 1.0.13
	 */

	@Override
	public void setZoomFillPaint(Paint paint) {
		if (paint == null) {
			throw new IllegalArgumentException("Null 'paint' argument.");
		}
		this.selectionFillPaint = paint;
	}

	/**
	 * Returns the zoom rectangle outline paint.
	 * 
	 * @return The zoom rectangle outline paint (never <code>null</code>).
	 * 
	 * @see #setZoomOutlinePaint(java.awt.Paint)
	 * @see #setFillZoomRectangle(boolean)
	 * 
	 * @since 1.0.13
	 */

	@Override
	public Paint getZoomOutlinePaint() {
		return this.selectionOutlinePaint;
	}

	/**
	 * Sets the zoom rectangle outline paint.
	 * 
	 * @param paint
	 *            the paint (<code>null</code> not permitted).
	 * 
	 * @see #getZoomOutlinePaint()
	 * @see #getFillZoomRectangle()
	 * 
	 * @since 1.0.13
	 */

	@Override
	public void setZoomOutlinePaint(Paint paint) {
		this.selectionOutlinePaint = paint;
	}

	/**
	 * The mouse wheel handler. This will be an instance of MouseWheelHandler but we can't reference
	 * that class directly because it depends on JRE 1.4 and we still want to support JRE 1.3.1.
	 */
	private Object mouseWheelHandler;

	/**
	 * Returns <code>true</code> if the mouse wheel handler is enabled, and <code>false</code>
	 * otherwise.
	 * 
	 * @return A boolean.
	 * 
	 * @since 1.0.13
	 */

	@Override
	public boolean isMouseWheelEnabled() {
		return this.mouseWheelHandler != null;
	}

	/**
	 * Enables or disables mouse wheel support for the panel. Note that this method does nothing
	 * when running JFreeChart on JRE 1.3.1, because that older version of the Java runtime does not
	 * support mouse wheel events.
	 * 
	 * @param flag
	 *            a boolean.
	 * 
	 * @since 1.0.13
	 */

	@Override
	public void setMouseWheelEnabled(boolean flag) {
		if (flag && this.mouseWheelHandler == null) {
			// use reflection to instantiate a mouseWheelHandler because to
			// continue supporting JRE 1.3.1 we cannot depend on the
			// MouseWheelListener interface directly
			try {
				Class<?> c = Class.forName("org.jfree.chart.MouseWheelHandler");
				Constructor<?> cc = c.getConstructor(new Class[] { ChartPanel.class });
				Object mwh = cc.newInstance(new Object[] { this });
				this.mouseWheelHandler = mwh;
			} catch (ClassNotFoundException e) {
				// the class isn't there, so we must have compiled JFreeChart
				// with JDK 1.3.1 - thus, we can't have mouse wheel support
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		} else if (!flag && this.mouseWheelHandler != null) {
			// use reflection to deregister the mouseWheelHandler
			try {
				Class<?> mwl = Class.forName("java.awt.event.MouseWheelListener");
				Class<ChartPanel> c2 = ChartPanel.class;
				Method m = c2.getMethod("removeMouseWheelListener", new Class[] { mwl });
				m.invoke(this, new Object[] { this.mouseWheelHandler });
				this.mouseWheelHandler = null;
			} catch (ClassNotFoundException e) {
				// must be running on JRE 1.3.1, so just ignore this
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Add an overlay to the panel.
	 * 
	 * @param overlay
	 *            the overlay (<code>null</code> not permitted).
	 * 
	 * @since 1.0.13
	 */

	@Override
	public void addOverlay(Overlay overlay) {
		if (overlay == null) {
			throw new IllegalArgumentException("Null 'overlay' argument.");
		}
		this.overlays.add(overlay);
		overlay.addChangeListener(this);
		repaint();
	}

	/**
	 * Removes an overlay from the panel.
	 * 
	 * @param overlay
	 *            the overlay to remove (<code>null</code> not permitted).
	 * 
	 * @since 1.0.13
	 */

	@Override
	public void removeOverlay(Overlay overlay) {
		if (overlay == null) {
			throw new IllegalArgumentException("Null 'overlay' argument.");
		}
		boolean removed = this.overlays.remove(overlay);
		if (removed) {
			overlay.removeChangeListener(this);
			repaint();
		}
	}

	/**
	 * Handles a change to an overlay by repainting the panel.
	 * 
	 * @param event
	 *            the event.
	 * 
	 * @since 1.0.13
	 */

	@Override
	public void overlayChanged(OverlayChangeEvent event) {
		repaint();
	}

	/**
	 * Switches the display of tooltips for the panel on or off. Note that tooltips can only be
	 * displayed if the chart has been configured to generate tooltip items.
	 * 
	 * @param flag
	 *            <code>true</code> to enable tooltips, <code>false</code> to disable tooltips.
	 */

	@Override
	public void setDisplayToolTips(boolean flag) {
		if (flag) {
			ToolTipManager.sharedInstance().registerComponent(this);
		} else {
			ToolTipManager.sharedInstance().unregisterComponent(this);
		}
	}

	/**
	 * Returns a string for the tooltip.
	 * 
	 * @param e
	 *            the mouse event.
	 * 
	 * @return A tool tip or <code>null</code> if no tooltip is available.
	 */

	@Override
	public String getToolTipText(MouseEvent e) {

		String result = null;
		if (this.info != null) {
			EntityCollection entities = this.info.getEntityCollection();
			if (entities != null) {
				Insets insets = getInsets();
				ChartEntity entity = entities.getEntity((int) ((e.getX() - insets.left) / this.scaleX),
						(int) ((e.getY() - insets.top) / this.scaleY));
				if (entity != null) {
					result = entity.getToolTipText();
				}
			}
		}
		return result;

	}

	/**
	 * Translates a Java2D point on the chart to a screen location.
	 * 
	 * @param java2DPoint
	 *            the Java2D point.
	 * 
	 * @return The screen location.
	 */

	@Override
	public Point translateJava2DToScreen(Point2D java2DPoint) {
		Insets insets = getInsets();
		int x = (int) (java2DPoint.getX() * this.scaleX + insets.left);
		int y = (int) (java2DPoint.getY() * this.scaleY + insets.top);
		return new Point(x, y);
	}

	/**
	 * Translates a panel (component) location to a Java2D point.
	 * 
	 * @param screenPoint
	 *            the screen location (<code>null</code> not permitted).
	 * 
	 * @return The Java2D coordinates.
	 */

	@Override
	public Point2D translateScreenToJava2D(Point screenPoint) {
		Insets insets = getInsets();
		double x = (screenPoint.getX() - insets.left) / this.scaleX;
		double y = (screenPoint.getY() - insets.top) / this.scaleY;
		return new Point2D.Double(x, y);
	}

	/**
	 * Applies any scaling that is in effect for the chart drawing to the given rectangle.
	 * 
	 * @param rect
	 *            the rectangle (<code>null</code> not permitted).
	 * 
	 * @return A new scaled rectangle.
	 */

	@Override
	public Rectangle2D scale(Rectangle2D rect) {
		Insets insets = getInsets();
		double x = rect.getX() * getScaleX() + insets.left;
		double y = rect.getY() * getScaleY() + insets.top;
		double w = rect.getWidth() * getScaleX();
		double h = rect.getHeight() * getScaleY();
		return new Rectangle2D.Double(x, y, w, h);
	}

	/**
	 * Returns the chart entity at a given point.
	 * <P>
	 * This method will return null if there is (a) no entity at the given point, or (b) no entity
	 * collection has been generated.
	 * 
	 * @param viewX
	 *            the x-coordinate.
	 * @param viewY
	 *            the y-coordinate.
	 * 
	 * @return The chart entity (possibly <code>null</code>).
	 */

	@Override
	public ChartEntity getEntityForPoint(int viewX, int viewY) {

		ChartEntity result = null;
		if (this.info != null) {
			Insets insets = getInsets();
			double x = (viewX - insets.left) / this.scaleX;
			double y = (viewY - insets.top) / this.scaleY;
			EntityCollection entities = this.info.getEntityCollection();
			result = entities != null ? entities.getEntity(x, y) : null;
		}
		return result;

	}

	/**
	 * Paints the component by drawing the chart to fill the entire component, but allowing for the
	 * insets (which will be non-zero if a border has been set for this component). To increase
	 * performance (at the expense of memory), an off-screen buffer image can be used.
	 * 
	 * @param g
	 *            the graphics device for drawing on.
	 */

	@Override
	public void paintComponent(Graphics g) {
		if (this.chart == null) {
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
		this.scaleX = 1.0;
		this.scaleY = 1.0;

		if (drawWidth < this.minimumDrawWidth) {
			this.scaleX = drawWidth / this.minimumDrawWidth;
			drawWidth = this.minimumDrawWidth;
			scale = true;
		} else if (drawWidth > this.maximumDrawWidth) {
			this.scaleX = drawWidth / this.maximumDrawWidth;
			drawWidth = this.maximumDrawWidth;
			scale = true;
		}

		if (drawHeight < this.minimumDrawHeight) {
			this.scaleY = drawHeight / this.minimumDrawHeight;
			drawHeight = this.minimumDrawHeight;
			scale = true;
		} else if (drawHeight > this.maximumDrawHeight) {
			this.scaleY = drawHeight / this.maximumDrawHeight;
			drawHeight = this.maximumDrawHeight;
			scale = true;
		}

		Rectangle2D chartArea = new Rectangle2D.Double(0.0, 0.0, drawWidth, drawHeight);
		// redrawing the chart every time...

		AffineTransform saved = g2.getTransform();
		g2.translate(insets.left, insets.top);
		if (scale) {
			AffineTransform st = AffineTransform.getScaleInstance(this.scaleX, this.scaleY);
			g2.transform(st);
		}
		this.chart.draw(g2, chartArea, this.anchor, this.info);
		g2.setTransform(saved);

		Iterator<Overlay> iterator = this.overlays.iterator();
		while (iterator.hasNext()) {
			Overlay overlay = iterator.next();
			overlay.paintOverlay(g2, this);
		}

		// redraw the zoom rectangle (if present) - if useBuffer is false,
		// we use XOR so we can XOR the rectangle away again without redrawing
		// the chart
		drawSelectionRectangle(g2);

		g2.dispose();

		this.anchor = null;
		this.verticalTraceLine = null;
		this.horizontalTraceLine = null;

	}

	/**
	 * Receives notification of changes to the chart, and redraws the chart.
	 * 
	 * @param event
	 *            details of the chart change event.
	 */

	@Override
	public void chartChanged(ChartChangeEvent event) {
		Plot plot = this.chart.getPlot();
		if (plot instanceof Zoomable) {
			Zoomable z = (Zoomable) plot;
			this.orientation = z.getOrientation();
		}
		repaint();
	}

	/**
	 * Receives notification of a chart progress event.
	 * 
	 * @param event
	 *            the event.
	 */

	@Override
	public void chartProgress(ChartProgressEvent event) {
		// does nothing - override if necessary
	}

	/**
	 * Handles action events generated by the popup menu.
	 * 
	 * @param event
	 *            the event.
	 */

	@Override
	public void actionPerformed(ActionEvent event) {

		String command = event.getActionCommand();

		// many of the zoom methods need a screen location - all we have is
		// the zoomPoint, but it might be null. Here we grab the x and y
		// coordinates, or use defaults...
		double screenX = -1.0;
		double screenY = -1.0;
		if (this.zoomPoint != null) {
			screenX = this.zoomPoint.getX();
			screenY = this.zoomPoint.getY();
		}

		if (super.getChart() == null) {
			return;
		}

		if (command.equals(PROPERTIES_COMMAND)) {
			doEditChartProperties();
		} else if (command.equals(COPY_COMMAND)) {
			doCopy();
		} else if (command.equals(SAVE_COMMAND)) {
			try {
				doSaveAs();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (command.equals(PRINT_COMMAND)) {
			createChartPrintJob();
		} else if (command.equals(ZOOM_IN_BOTH_COMMAND)) {
			zoomInBoth(screenX, screenY);
		} else if (command.equals(ZOOM_IN_DOMAIN_COMMAND)) {
			shrinkSelectionOnCenter(screenX, screenY, null);
		} else if (command.equals(ZOOM_IN_RANGE_COMMAND)) {
			shrinkSelectionOnRange(screenX, screenY, null);
		} else if (command.equals(ZOOM_OUT_BOTH_COMMAND)) {
			enlargeSelectionOnCenter(screenX, screenY, null);
		} else if (command.equals(ZOOM_OUT_DOMAIN_COMMAND)) {
			enlargeSelectionOnDomain(screenX, screenY, null);
		} else if (command.equals(ZOOM_OUT_RANGE_COMMAND)) {
			enlargeSelectionOnRange(screenX, screenY, null);
		} else if (command.equals(ZOOM_RESET_BOTH_COMMAND)) {
			restoreAutoBounds();
		} else if (command.equals(ZOOM_RESET_DOMAIN_COMMAND)) {
			selectCompleteDomainBounds();
		} else if (command.equals(ZOOM_RESET_RANGE_COMMAND)) {
			selectCompleteRangeBounds();
		}

	}

	/**
	 * Handles a 'mouse entered' event. This method changes the tooltip delays of
	 * ToolTipManager.sharedInstance() to the possibly different values set for this chart panel.
	 * 
	 * @param e
	 *            the mouse event.
	 */

	@Override
	public void mouseEntered(MouseEvent e) {
		if (!this.ownToolTipDelaysActive) {
			ToolTipManager ttm = ToolTipManager.sharedInstance();

			this.originalToolTipInitialDelay = ttm.getInitialDelay();
			ttm.setInitialDelay(this.ownToolTipInitialDelay);

			this.originalToolTipReshowDelay = ttm.getReshowDelay();
			ttm.setReshowDelay(this.ownToolTipReshowDelay);

			this.originalToolTipDismissDelay = ttm.getDismissDelay();
			ttm.setDismissDelay(this.ownToolTipDismissDelay);

			this.ownToolTipDelaysActive = true;
		}
	}

	/**
	 * Handles a 'mouse exited' event. This method resets the tooltip delays of
	 * ToolTipManager.sharedInstance() to their original values in effect before mouseEntered()
	 * 
	 * @param e
	 *            the mouse event.
	 */

	@Override
	public void mouseExited(MouseEvent e) {
		if (this.ownToolTipDelaysActive) {
			// restore original tooltip dealys
			ToolTipManager ttm = ToolTipManager.sharedInstance();
			ttm.setInitialDelay(this.originalToolTipInitialDelay);
			ttm.setReshowDelay(this.originalToolTipReshowDelay);
			ttm.setDismissDelay(this.originalToolTipDismissDelay);
			this.ownToolTipDelaysActive = false;
		}
	}

	/**
	 * Handles a 'mouse pressed' event.
	 * <P>
	 * This event is the popup trigger on Unix/Linux. For Windows, the popup trigger is the 'mouse
	 * released' event.
	 * 
	 * @param e
	 *            The mouse event.
	 */

	@Override
	public void mousePressed(MouseEvent e) {
		if (this.chart == null) {
			return;
		}
		Plot plot = this.chart.getPlot();
		int mods = e.getModifiers();
		if ((mods & this.panMask) == this.panMask) {
			// can we pan this plot?
			if (plot instanceof Pannable) {
				Rectangle2D screenDataArea = getScreenDataArea(e.getX(), e.getY());
				if (screenDataArea != null && screenDataArea.contains(e.getPoint())) {
					this.panW = screenDataArea.getWidth();
					this.panH = screenDataArea.getHeight();
					this.panLast = e.getPoint();
					setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
				}
			}
		} else if (this.selectionRectangle == null) {
			Rectangle2D screenDataArea = getScreenDataArea(e.getX(), e.getY());
			if (screenDataArea != null) {
				this.zoomPoint = getPointInRectangle(e.getX(), e.getY(), screenDataArea);
			} else {
				this.zoomPoint = null;
			}
			if (e.isPopupTrigger()) {
				if (this.popup != null) {
					displayPopupMenu(e.getX(), e.getY());
				}
			}
		}
	}

	/**
	 * Returns a point based on (x, y) but constrained to be within the bounds of the given
	 * rectangle. This method could be moved to JCommon.
	 * 
	 * @param x
	 *            the x-coordinate.
	 * @param y
	 *            the y-coordinate.
	 * @param area
	 *            the rectangle (<code>null</code> not permitted).
	 * 
	 * @return A point within the rectangle.
	 */
	private Point2D getPointInRectangle(int x, int y, Rectangle2D area) {
		double xx = Math.max(area.getMinX(), Math.min(x, area.getMaxX()));
		double yy = Math.max(area.getMinY(), Math.min(y, area.getMaxY()));
		return new Point2D.Double(xx, yy);
	}

	/**
	 * Handles a 'mouse dragged' event.
	 * 
	 * @param e
	 *            the mouse event.
	 */

	@Override
	public void mouseDragged(MouseEvent e) {

		// if the popup menu has already been triggered, then ignore dragging...
		if (this.popup != null && this.popup.isShowing()) {
			return;
		}

		// handle panning if we have a start point
		if (this.panLast != null) {
			double dx = e.getX() - this.panLast.getX();
			double dy = e.getY() - this.panLast.getY();
			if (dx == 0.0 && dy == 0.0) {
				return;
			}
			double wPercent = -dx / this.panW;
			double hPercent = dy / this.panH;
			boolean old = this.chart.getPlot().isNotify();
			this.chart.getPlot().setNotify(false);
			Pannable p = (Pannable) this.chart.getPlot();
			if (p.getOrientation() == PlotOrientation.VERTICAL) {
				panAxes(wPercent, hPercent, e);
			} else {
				panAxes(hPercent, wPercent, e);
			}
			this.panLast = e.getPoint();
			this.chart.getPlot().setNotify(old);
			return;
		}

		// if no initial zoom point was set, ignore dragging...
		if (this.zoomPoint == null) {
			return;
		}

		boolean hZoom = false;
		boolean vZoom = false;
		if (this.orientation == PlotOrientation.HORIZONTAL) {
			hZoom = this.rangeZoomable;
			vZoom = this.domainZoomable;
		} else {
			hZoom = this.domainZoomable;
			vZoom = this.rangeZoomable;
		}
		Rectangle2D scaledDataArea = getScreenDataArea((int) this.zoomPoint.getX(), (int) this.zoomPoint.getY());
		if (hZoom && vZoom) {
			// selected rectangle shouldn't extend outside the data area...
			double xmax = Math.min(e.getX(), scaledDataArea.getMaxX());
			double ymax = Math.min(e.getY(), scaledDataArea.getMaxY());
			this.selectionRectangle = new Rectangle2D.Double(this.zoomPoint.getX(), this.zoomPoint.getY(), xmax
					- this.zoomPoint.getX(), ymax - this.zoomPoint.getY());
		} else if (hZoom) {
			double xmax = Math.min(e.getX(), scaledDataArea.getMaxX());
			this.selectionRectangle = new Rectangle2D.Double(this.zoomPoint.getX(), scaledDataArea.getMinY(), xmax
					- this.zoomPoint.getX(), scaledDataArea.getHeight());
		} else if (vZoom) {
			double ymax = Math.min(e.getY(), scaledDataArea.getMaxY());
			this.selectionRectangle = new Rectangle2D.Double(scaledDataArea.getMinX(), this.zoomPoint.getY(),
					scaledDataArea.getWidth(), ymax - this.zoomPoint.getY());
		}

		// Draw the new zoom rectangle...
		repaint();

	}

	private void panAxes(double wPercent, double hPercent, MouseEvent selectionEvent) {
		Plot p = this.chart.getPlot();
		if (p instanceof XYPlot) {
			XYPlot plot = (XYPlot) p;
			Selection selectionObject = new Selection();
			for (int i = 0; i < plot.getRangeAxisCount(); i++) {
				ValueAxis axis = plot.getRangeAxis(i);
				double lowerBound = axis.getLowerBound();
				double upperBound = axis.getUpperBound();
				double shift = (upperBound - lowerBound) * hPercent;
				lowerBound += shift;
				upperBound += shift;
				Range axisRange = new Range(lowerBound, upperBound);
				for (String axisName : axisNameResolver.resolveYAxis(i)) {
					selectionObject.addDelimiter(axisName, axisRange);
				}
			}
			for (int i = 0; i < plot.getDomainAxisCount(); i++) {
				ValueAxis axis = plot.getDomainAxis(i);
				double lowerBound = axis.getLowerBound();
				double upperBound = axis.getUpperBound();
				double shift = (upperBound - lowerBound) * wPercent;
				lowerBound += shift;
				upperBound += shift;
				Range axisRange = new Range(lowerBound, upperBound);
				for (String axisName : axisNameResolver.resolveXAxis(i)) {
					selectionObject.addDelimiter(axisName, axisRange);
				}
			}
			informSelectionListener(selectionObject, selectionEvent);
		}
	}

	/**
	 * Handles a 'mouse released' event. On Windows, we need to check if this is a popup trigger,
	 * but only if we haven't already been tracking a zoom rectangle.
	 * 
	 * @param e
	 *            information about the event.
	 */

	@Override
	public void mouseReleased(MouseEvent e) {
		// if we've been panning, we need to reset now that the mouse is
		// released...
		if (this.panLast != null) {
			this.panLast = null;
			setCursor(Cursor.getDefaultCursor());
		} else if (this.selectionRectangle != null) {
			boolean hZoom = false;
			boolean vZoom = false;
			if (this.orientation == PlotOrientation.HORIZONTAL) {
				hZoom = this.rangeZoomable;
				vZoom = this.domainZoomable;
			} else {
				hZoom = this.domainZoomable;
				vZoom = this.rangeZoomable;
			}

			boolean zoomTrigger1 = hZoom && Math.abs(e.getX() - this.zoomPoint.getX()) >= this.zoomTriggerDistance;
			boolean zoomTrigger2 = vZoom && Math.abs(e.getY() - this.zoomPoint.getY()) >= this.zoomTriggerDistance;
			if (zoomTrigger1 || zoomTrigger2) {
				if (hZoom && e.getX() < this.zoomPoint.getX() || vZoom && e.getY() < this.zoomPoint.getY()) {
					restoreAutoBounds();
				} else {
					double x, y, w, h;
					Rectangle2D screenDataArea = getScreenDataArea((int) this.zoomPoint.getX(), (int) this.zoomPoint.getY());
					double maxX = screenDataArea.getMaxX();
					double maxY = screenDataArea.getMaxY();
					// for mouseReleased event, (horizontalZoom || verticalZoom)
					// will be true, so we can just test for either being false;
					// otherwise both are true
					if (!vZoom) {
						x = this.zoomPoint.getX();
						y = screenDataArea.getMinY();
						w = Math.min(this.selectionRectangle.getWidth(), maxX - this.zoomPoint.getX());
						h = screenDataArea.getHeight();
					} else if (!hZoom) {
						x = screenDataArea.getMinX();
						y = this.zoomPoint.getY();
						w = screenDataArea.getWidth();
						h = Math.min(this.selectionRectangle.getHeight(), maxY - this.zoomPoint.getY());
					} else {
						x = this.zoomPoint.getX();
						y = this.zoomPoint.getY();
						w = Math.min(this.selectionRectangle.getWidth(), maxX - this.zoomPoint.getX());
						h = Math.min(this.selectionRectangle.getHeight(), maxY - this.zoomPoint.getY());
					}
					Rectangle2D zoomArea = new Rectangle2D.Double(x, y, w, h);
					selectRectangle(zoomArea, e);
				}
				this.zoomPoint = null;
				this.selectionRectangle = null;
			} else {
				this.zoomPoint = null;
				this.selectionRectangle = null;
			}

		}

		else if (e.isPopupTrigger()) {
			if (this.popup != null) {
				displayPopupMenu(e.getX(), e.getY());
			}
		}

	}

	/**
	 * Receives notification of mouse clicks on the panel. These are translated and passed on to any
	 * registered {@link ChartMouseListener}s.
	 * 
	 * @param event
	 *            Information about the mouse event.
	 */

	@Override
	public void mouseClicked(MouseEvent event) {

		Insets insets = getInsets();
		int x = (int) ((event.getX() - insets.left) / this.scaleX);
		int y = (int) ((event.getY() - insets.top) / this.scaleY);

		this.anchor = new Point2D.Double(x, y);
		if (this.chart == null) {
			return;
		}
		this.chart.setNotify(true); // force a redraw
		// new entity code...
		Object[] listeners = this.chartMouseListeners.getListeners(ChartMouseListener.class);
		if (listeners.length == 0) {
			return;
		}

		ChartEntity entity = null;
		if (this.info != null) {
			EntityCollection entities = this.info.getEntityCollection();
			if (entities != null) {
				entity = entities.getEntity(x, y);
			}
		}
		ChartMouseEvent chartEvent = new ChartMouseEvent(getChart(), event, entity);
		for (int i = listeners.length - 1; i >= 0; i -= 1) {
			((ChartMouseListener) listeners[i]).chartMouseClicked(chartEvent);
		}

	}

	/**
	 * Implementation of the MouseMotionListener's method.
	 * 
	 * @param e
	 *            the event.
	 */

	@Override
	public void mouseMoved(MouseEvent e) {
		Graphics2D g2 = (Graphics2D) getGraphics();
		if (this.horizontalAxisTrace) {
			drawHorizontalAxisTrace(g2, e.getX());
		}
		if (this.verticalAxisTrace) {
			drawVerticalAxisTrace(g2, e.getY());
		}
		g2.dispose();

		Object[] listeners = this.chartMouseListeners.getListeners(ChartMouseListener.class);
		if (listeners.length == 0) {
			return;
		}
		Insets insets = getInsets();
		int x = (int) ((e.getX() - insets.left) / this.scaleX);
		int y = (int) ((e.getY() - insets.top) / this.scaleY);

		ChartEntity entity = null;
		if (this.info != null) {
			EntityCollection entities = this.info.getEntityCollection();
			if (entities != null) {
				entity = entities.getEntity(x, y);
			}
		}

		// we can only generate events if the panel's chart is not null
		// (see bug report 1556951)
		if (this.chart != null) {
			ChartMouseEvent event = new ChartMouseEvent(getChart(), e, entity);
			for (int i = listeners.length - 1; i >= 0; i -= 1) {
				((ChartMouseListener) listeners[i]).chartMouseMoved(event);
			}
		}

	}

	/**
	 * Zooms in on an anchor point (specified in screen coordinate space).
	 * 
	 * @param x
	 *            the x value (in screen coordinates).
	 * @param y
	 *            the y value (in screen coordinates).
	 */

	public void shrinkSelectionOnCenter(double x, double y, MouseEvent selectionEvent) {
		Plot plot = this.chart.getPlot();
		if (plot == null) {
			return;
		}
		// here we tweak the notify flag on the plot so that only
		// one notification happens even though we update multiple
		// axes...
		boolean savedNotify = plot.isNotify();
		plot.setNotify(false);
		shrinkSelectionOnDomain(x, y, selectionEvent);
		shrinkSelectionOnRange(x, y, selectionEvent);
		plot.setNotify(savedNotify);
	}

	/**
	 * Zooms out on an anchor point (specified in screen coordinate space).
	 * 
	 * @param x
	 *            the x value (in screen coordinates).
	 * @param y
	 *            the y value (in screen coordinates).
	 */

	public void enlargeSelectionOnCenter(double x, double y, MouseEvent selectionEvent) {
		Plot plot = this.chart.getPlot();
		if (plot == null) {
			return;
		}
		// here we tweak the notify flag on the plot so that only
		// one notification happens even though we update multiple
		// axes...
		boolean savedNotify = plot.isNotify();
		plot.setNotify(false);
		enlargeSelectionOnDomain(x, y, selectionEvent);
		enlargeSelectionOnRange(x, y, selectionEvent);
		plot.setNotify(savedNotify);
	}

	/**
	 * Decreases the length of the domain axis, centered about the given coordinate on the screen.
	 * The length of the domain axis is reduced by the value of {@link #getZoomInFactor()}.
	 * 
	 * @param x
	 *            the x coordinate (in screen coordinates).
	 * @param y
	 *            the y-coordinate (in screen coordinates).
	 */

	public void shrinkSelectionOnDomain(double x, double y, MouseEvent selectionEvent) {
		Plot p = this.chart.getPlot();
		if (p instanceof XYPlot) {
			XYPlot plot = (XYPlot) p;
			Selection selectionObject = new Selection();
			for (int i = 0; i < plot.getDomainAxisCount(); i++) {
				ValueAxis domain = plot.getDomainAxis(i);
				double zoomFactor = getZoomInFactor();
				shrinkSelectionXAxis(x, y, selectionObject, domain, i, zoomFactor);
			}
			informSelectionListener(selectionObject, selectionEvent);
		}
	}

	/**
	 * Increases the length of the domain axis, centered about the given coordinate on the screen.
	 * The length of the domain axis is increased by the value of {@link #getZoomOutFactor()}.
	 * 
	 * @param x
	 *            the x coordinate (in screen coordinates).
	 * @param y
	 *            the y-coordinate (in screen coordinates).
	 */

	public void enlargeSelectionOnDomain(double x, double y, MouseEvent selectionEvent) {
		Plot p = this.chart.getPlot();
		if (p instanceof XYPlot) {
			XYPlot plot = (XYPlot) p;
			Selection selectionObject = new Selection();
			for (int i = 0; i < plot.getDomainAxisCount(); i++) {
				ValueAxis domain = plot.getDomainAxis(i);
				double zoomFactor = getZoomOutFactor();
				shrinkSelectionXAxis(x, y, selectionObject, domain, i, zoomFactor);
			}
			informSelectionListener(selectionObject, selectionEvent);
		}
	}

	private void shrinkSelectionYAxis(double x, double y, Selection selectionObject, ValueAxis axis, int axisIndex,
			double zoomFactor) {
		Rectangle2D scaledDataArea = getScreenDataArea((int) x, (int) y);

		double minY = scaledDataArea.getMinY();
		double maxY = scaledDataArea.getMaxY();
		double partToTop = (y - minY) / (maxY - minY);

		double lowerDomain = axis.getLowerBound();
		double upperDomain = axis.getUpperBound();
		double middlePointTop = lowerDomain + (upperDomain - lowerDomain) * (1d - partToTop);
		double width = (upperDomain - lowerDomain) * zoomFactor;
		Range axisRange = new Range(middlePointTop - width / 2d, middlePointTop + width / 2d);
		for (String axisName : axisNameResolver.resolveYAxis(axisIndex)) {
			selectionObject.addDelimiter(axisName, axisRange);
		}
	}

	/**
	 * Decreases the length of the range axis, centered about the given coordinate on the screen.
	 * The length of the range axis is reduced by the value of {@link #getZoomInFactor()}.
	 * 
	 * @param x
	 *            the x-coordinate (in screen coordinates).
	 * @param y
	 *            the y coordinate (in screen coordinates).
	 */

	public void shrinkSelectionOnRange(double x, double y, MouseEvent selectionEvent) {
		Plot p = this.chart.getPlot();
		if (p instanceof XYPlot) {
			XYPlot plot = (XYPlot) p;
			Selection selectionObject = new Selection();
			for (int i = 0; i < plot.getRangeAxisCount(); i++) {
				ValueAxis domain = plot.getRangeAxis(i);
				double zoomFactor = getZoomInFactor();
				shrinkSelectionYAxis(x, y, selectionObject, domain, i, zoomFactor);
			}
			informSelectionListener(selectionObject, selectionEvent);
		}
	}

	/**
	 * Increases the length the range axis, centered about the given coordinate on the screen. The
	 * length of the range axis is increased by the value of {@link #getZoomOutFactor()}.
	 * 
	 * @param x
	 *            the x coordinate (in screen coordinates).
	 * @param y
	 *            the y-coordinate (in screen coordinates).
	 */

	public void enlargeSelectionOnRange(double x, double y, MouseEvent selectionEvent) {
		Plot p = this.chart.getPlot();
		if (p instanceof XYPlot) {
			XYPlot plot = (XYPlot) p;
			Selection selectionObject = new Selection();
			for (int i = 0; i < plot.getRangeAxisCount(); i++) {
				ValueAxis domain = plot.getRangeAxis(i);
				double zoomFactor = getZoomOutFactor();
				shrinkSelectionYAxis(x, y, selectionObject, domain, i, zoomFactor);
			}
			informSelectionListener(selectionObject, selectionEvent);
		}
	}

	private void shrinkSelectionXAxis(double x, double y, Selection selectionObject, ValueAxis axis, int axisIndex,
			double zoomFactor) {
		Rectangle2D scaledDataArea = getScreenDataArea((int) x, (int) y);

		double minX = scaledDataArea.getMinX();
		double maxX = scaledDataArea.getMaxX();
		double partToLeft = (x - minX) / (maxX - minX);

		double lowerDomain = axis.getLowerBound();
		double upperDomain = axis.getUpperBound();
		double middlePointLeft = lowerDomain + (upperDomain - lowerDomain) * partToLeft;
		double width = (upperDomain - lowerDomain) * zoomFactor;
		Range domainRange = new Range(middlePointLeft - width / 2d, middlePointLeft + width / 2d);
		for (String axisName : axisNameResolver.resolveXAxis(axisIndex)) {
			selectionObject.addDelimiter(axisName, domainRange);
		}
	}

	public void selectRectangle(Rectangle2D selection, MouseEvent selectionEvent) {
		Rectangle2D scaledDataArea = getScreenDataArea((int) selection.getCenterX(), (int) selection.getCenterY());
		if (selection.getHeight() > 0 && selection.getWidth() > 0) {

			double hLower = (selection.getMinX() - scaledDataArea.getMinX()) / scaledDataArea.getWidth();
			double hUpper = (selection.getMaxX() - scaledDataArea.getMinX()) / scaledDataArea.getWidth();
			double vLower = (scaledDataArea.getMaxY() - selection.getMaxY()) / scaledDataArea.getHeight();
			double vUpper = (scaledDataArea.getMaxY() - selection.getMinY()) / scaledDataArea.getHeight();

			Plot p = this.chart.getPlot();
			if (p instanceof XYPlot) {
				XYPlot plot = (XYPlot) p;
				Selection selectionObject = new Selection();
				for (int i = 0; i < plot.getDomainAxisCount(); i++) {
					ValueAxis domain = plot.getDomainAxis(i);
					double lowerDomain = domain.getLowerBound();
					double upperDomain = domain.getUpperBound();
					Range axisRange = new Range(lowerDomain + (upperDomain - lowerDomain) * hLower, lowerDomain
							+ (upperDomain - lowerDomain) * hUpper);
					for (String axisName : axisNameResolver.resolveXAxis(i)) {
						selectionObject.addDelimiter(axisName, axisRange);
					}
				}
				for (int i = 0; i < plot.getRangeAxisCount(); i++) {
					ValueAxis range = plot.getRangeAxis(i);
					double lowerRange = range.getLowerBound();
					double upperRange = range.getUpperBound();
					Range axisRange = new Range(lowerRange + (upperRange - lowerRange) * vLower, lowerRange
							+ (upperRange - lowerRange) * vUpper);
					for (String axisName : axisNameResolver.resolveYAxis(i)) {
						selectionObject.addDelimiter(axisName, axisRange);
					}
				}
				informSelectionListener(selectionObject, selectionEvent);
			}
		}

	}

	private void informSelectionListener(Selection selectionObject, MouseEvent selectionEvent) {
		for (SelectionListener listener : selectionListeners) {
			listener.selected(selectionObject, selectionEvent);
		}
		if (selectionListeners.isEmpty()) {
			repaint();
		}
	}

	/**
	 * Restores the auto-range calculation on both axes.
	 */

	@Override
	public void restoreAutoBounds() {
		Plot plot = this.chart.getPlot();
		if (plot == null) {
			return;
		}
		// here we tweak the notify flag on the plot so that only
		// one notification happens even though we update multiple
		// axes...
		boolean savedNotify = plot.isNotify();
		plot.setNotify(false);
		selectCompleteDomainBounds();
		selectCompleteRangeBounds();
		plot.setNotify(savedNotify);
	}

	/**
	 * Restores the auto-range calculation on the domain axis.
	 */

	public void selectCompleteDomainBounds() {
		Plot plot = this.chart.getPlot();
		if (plot instanceof Zoomable) {
			Zoomable z = (Zoomable) plot;
			// here we tweak the notify flag on the plot so that only
			// one notification happens even though we update multiple
			// axes...
			boolean savedNotify = plot.isNotify();
			plot.setNotify(false);
			// we need to guard against this.zoomPoint being null
			Point2D zp = this.zoomPoint != null ? this.zoomPoint : new Point();
			z.zoomDomainAxes(0.0, this.info.getPlotInfo(), zp);
			plot.setNotify(savedNotify);

			if (plot instanceof XYPlot) {
				XYPlot xyPlot = (XYPlot) plot;
				Selection selectionObject = new Selection();
				for (int i = 0; i < xyPlot.getDomainAxisCount(); i++) {
					ValueAxis domain = xyPlot.getDomainAxis(i);
					Range axisRange = new Range(domain.getLowerBound(), domain.getUpperBound());
					for (String axisName : axisNameResolver.resolveXAxis(i)) {
						selectionObject.addDelimiter(axisName, axisRange);
					}
				}
				informSelectionListener(selectionObject, null);
			}
		}
	}

	/**
	 * Restores the auto-range calculation on the range axis.
	 */

	public void selectCompleteRangeBounds() {
		Plot plot = this.chart.getPlot();
		if (plot instanceof Zoomable) {
			Zoomable z = (Zoomable) plot;
			// here we tweak the notify flag on the plot so that only
			// one notification happens even though we update multiple
			// axes...
			boolean savedNotify = plot.isNotify();
			plot.setNotify(false);
			// we need to guard against this.zoomPoint being null
			Point2D zp = this.zoomPoint != null ? this.zoomPoint : new Point();
			z.zoomRangeAxes(0.0, this.info.getPlotInfo(), zp);
			plot.setNotify(savedNotify);

			if (plot instanceof XYPlot) {
				XYPlot xyPlot = (XYPlot) plot;
				Selection selectionObject = new Selection();
				for (int i = 0; i < xyPlot.getRangeAxisCount(); i++) {
					ValueAxis range = xyPlot.getRangeAxis(i);
					Range axisRange = new Range(range.getLowerBound(), range.getUpperBound());
					for (String axisName : axisNameResolver.resolveYAxis(i)) {
						selectionObject.addDelimiter(axisName, axisRange);
					}
				}
				informSelectionListener(selectionObject, null);
			}
		}
	}

	/**
	 * Returns the data area for the chart (the area inside the axes) with the current scaling
	 * applied (that is, the area as it appears on screen).
	 * 
	 * @return The scaled data area.
	 */

	@Override
	public Rectangle2D getScreenDataArea() {
		Rectangle2D dataArea = this.info.getPlotInfo().getDataArea();
		Insets insets = getInsets();
		double x = dataArea.getX() * this.scaleX + insets.left;
		double y = dataArea.getY() * this.scaleY + insets.top;
		double w = dataArea.getWidth() * this.scaleX;
		double h = dataArea.getHeight() * this.scaleY;
		return new Rectangle2D.Double(x, y, w, h);
	}

	/**
	 * Returns the data area (the area inside the axes) for the plot or subplot, with the current
	 * scaling applied.
	 * 
	 * @param x
	 *            the x-coordinate (for subplot selection).
	 * @param y
	 *            the y-coordinate (for subplot selection).
	 * 
	 * @return The scaled data area.
	 */

	@Override
	public Rectangle2D getScreenDataArea(int x, int y) {
		PlotRenderingInfo plotInfo = this.info.getPlotInfo();
		Rectangle2D result;
		if (plotInfo.getSubplotCount() == 0) {
			result = getScreenDataArea();
		} else {
			// get the origin of the zoom selection in the Java2D space used for
			// drawing the chart (that is, before any scaling to fit the panel)
			Point2D selectOrigin = translateScreenToJava2D(new Point(x, y));
			int subplotIndex = plotInfo.getSubplotIndex(selectOrigin);
			if (subplotIndex == -1) {
				return null;
			}
			result = scale(plotInfo.getSubplotInfo(subplotIndex).getDataArea());
		}
		return result;
	}

	/**
	 * Returns the initial tooltip delay value used inside this chart panel.
	 * 
	 * @return An integer representing the initial delay value, in milliseconds.
	 * 
	 * @see javax.swing.ToolTipManager#getInitialDelay()
	 */

	@Override
	public int getInitialDelay() {
		return this.ownToolTipInitialDelay;
	}

	/**
	 * Returns the reshow tooltip delay value used inside this chart panel.
	 * 
	 * @return An integer representing the reshow delay value, in milliseconds.
	 * 
	 * @see javax.swing.ToolTipManager#getReshowDelay()
	 */

	@Override
	public int getReshowDelay() {
		return this.ownToolTipReshowDelay;
	}

	/**
	 * Returns the dismissal tooltip delay value used inside this chart panel.
	 * 
	 * @return An integer representing the dismissal delay value, in milliseconds.
	 * 
	 * @see javax.swing.ToolTipManager#getDismissDelay()
	 */

	@Override
	public int getDismissDelay() {
		return this.ownToolTipDismissDelay;
	}

	/**
	 * Specifies the initial delay value for this chart panel.
	 * 
	 * @param delay
	 *            the number of milliseconds to delay (after the cursor has paused) before
	 *            displaying.
	 * 
	 * @see javax.swing.ToolTipManager#setInitialDelay(int)
	 */

	@Override
	public void setInitialDelay(int delay) {
		this.ownToolTipInitialDelay = delay;
	}

	/**
	 * Specifies the amount of time before the user has to wait initialDelay milliseconds before a
	 * tooltip will be shown.
	 * 
	 * @param delay
	 *            time in milliseconds
	 * 
	 * @see javax.swing.ToolTipManager#setReshowDelay(int)
	 */

	@Override
	public void setReshowDelay(int delay) {
		this.ownToolTipReshowDelay = delay;
	}

	/**
	 * Specifies the dismissal delay value for this chart panel.
	 * 
	 * @param delay
	 *            the number of milliseconds to delay before taking away the tooltip
	 * 
	 * @see javax.swing.ToolTipManager#setDismissDelay(int)
	 */

	@Override
	public void setDismissDelay(int delay) {
		this.ownToolTipDismissDelay = delay;
	}

	/**
	 * Returns the zoom in factor.
	 * 
	 * @return The zoom in factor.
	 * 
	 * @see #setZoomInFactor(double)
	 */

	@Override
	public double getZoomInFactor() {
		return this.zoomInFactor;
	}

	/**
	 * Sets the zoom in factor.
	 * 
	 * @param factor
	 *            the factor.
	 * 
	 * @see #getZoomInFactor()
	 */

	@Override
	public void setZoomInFactor(double factor) {
		this.zoomInFactor = factor;
	}

	/**
	 * Returns the zoom out factor.
	 * 
	 * @return The zoom out factor.
	 * 
	 * @see #setZoomOutFactor(double)
	 */

	@Override
	public double getZoomOutFactor() {
		return this.zoomOutFactor;
	}

	/**
	 * Sets the zoom out factor.
	 * 
	 * @param factor
	 *            the factor.
	 * 
	 * @see #getZoomOutFactor()
	 */

	@Override
	public void setZoomOutFactor(double factor) {
		this.zoomOutFactor = factor;
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
	private void drawSelectionRectangle(Graphics2D g2) {
		if (this.selectionRectangle != null) {
			if (this.fillSelectionRectangle) {
				g2.setPaint(this.selectionFillPaint);
				g2.fill(selectionRectangle);
			} else {
				g2.setPaint(this.selectionOutlinePaint);
				g2.draw(selectionRectangle);
			}
		}
	}

	/**
	 * Draws a vertical line used to trace the mouse position to the horizontal axis.
	 * 
	 * @param g2
	 *            the graphics device.
	 * @param x
	 *            the x-coordinate of the trace line.
	 */
	private void drawHorizontalAxisTrace(Graphics2D g2, int x) {

		Rectangle2D dataArea = getScreenDataArea();

		g2.setXORMode(Color.orange);
		if ((int) dataArea.getMinX() < x && x < (int) dataArea.getMaxX()) {

			if (this.verticalTraceLine != null) {
				g2.draw(this.verticalTraceLine);
				this.verticalTraceLine.setLine(x, (int) dataArea.getMinY(), x, (int) dataArea.getMaxY());
			} else {
				this.verticalTraceLine = new Line2D.Float(x, (int) dataArea.getMinY(), x, (int) dataArea.getMaxY());
			}
			g2.draw(this.verticalTraceLine);
		}

		// Reset to the default 'overwrite' mode
		g2.setPaintMode();
	}

	/**
	 * Draws a horizontal line used to trace the mouse position to the vertical axis.
	 * 
	 * @param g2
	 *            the graphics device.
	 * @param y
	 *            the y-coordinate of the trace line.
	 */
	private void drawVerticalAxisTrace(Graphics2D g2, int y) {

		Rectangle2D dataArea = getScreenDataArea();

		g2.setXORMode(Color.orange);
		if ((int) dataArea.getMinY() < y && y < (int) dataArea.getMaxY()) {

			if (this.horizontalTraceLine != null) {
				g2.draw(this.horizontalTraceLine);
				this.horizontalTraceLine.setLine((int) dataArea.getMinX(), y, (int) dataArea.getMaxX(), y);
			} else {
				this.horizontalTraceLine = new Line2D.Float((int) dataArea.getMinX(), y, (int) dataArea.getMaxX(), y);
			}
			g2.draw(this.horizontalTraceLine);
		}

		// Reset to the default 'overwrite' mode
		g2.setPaintMode();
	}

	/**
	 * Displays a dialog that allows the user to edit the properties for the current chart.
	 * 
	 * @since 1.0.3
	 */

	@Override
	public void doEditChartProperties() {

		ChartEditor editor = ChartEditorManager.getChartEditor(this.chart);
		int result = JOptionPane.showConfirmDialog(this, editor, localizationResources.getString("Chart_Properties"),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {
			editor.updateChart(this.chart);
		}

	}

	/**
	 * Copies the current chart to the system clipboard.
	 * 
	 * @since 1.0.13
	 */

	@Override
	public void doCopy() {
		Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Insets insets = getInsets();
		int w = getWidth() - insets.left - insets.right;
		int h = getHeight() - insets.top - insets.bottom;
		ChartTransferable selection = new ChartTransferable(this.chart, w, h);
		systemClipboard.setContents(selection, null);
	}

	/**
	 * Opens a file chooser and gives the user an opportunity to save the chart in PNG format.
	 * 
	 * @throws IOException
	 *             if there is an I/O error.
	 */

	@Override
	public void doSaveAs() throws IOException {

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(this.defaultDirectoryForSaveAs);
		ExtensionFileFilter filter = new ExtensionFileFilter(localizationResources.getString("PNG_Image_Files"), ".png");
		fileChooser.addChoosableFileFilter(filter);

		int option = fileChooser.showSaveDialog(this);
		if (option == JFileChooser.APPROVE_OPTION) {
			String filename = fileChooser.getSelectedFile().getPath();
			if (isEnforceFileExtensions()) {
				if (!filename.endsWith(".png")) {
					filename = filename + ".png";
				}
			}
			ChartUtilities.saveChartAsPNG(new File(filename), this.chart, getWidth(), getHeight());
		}

	}

	/**
	 * Creates a print job for the chart.
	 */

	@Override
	public void createChartPrintJob() {

		PrinterJob job = PrinterJob.getPrinterJob();
		PageFormat pf = job.defaultPage();
		PageFormat pf2 = job.pageDialog(pf);
		if (pf2 != pf) {
			job.setPrintable(this, pf2);
			if (job.printDialog()) {
				try {
					job.print();
				} catch (PrinterException e) {
					JOptionPane.showMessageDialog(this, e);
				}
			}
		}

	}

	/**
	 * Prints the chart on a single page.
	 * 
	 * @param g
	 *            the graphics context.
	 * @param pf
	 *            the page format to use.
	 * @param pageIndex
	 *            the index of the page. If not <code>0</code>, nothing gets print.
	 * 
	 * @return The result of printing.
	 */

	@Override
	public int print(Graphics g, PageFormat pf, int pageIndex) {

		if (pageIndex != 0) {
			return NO_SUCH_PAGE;
		}
		Graphics2D g2 = (Graphics2D) g;
		double x = pf.getImageableX();
		double y = pf.getImageableY();
		double w = pf.getImageableWidth();
		double h = pf.getImageableHeight();
		this.chart.draw(g2, new Rectangle2D.Double(x, y, w, h), this.anchor, null);
		return PAGE_EXISTS;

	}

	/**
	 * Adds a listener to the list of objects listening for chart mouse events.
	 * 
	 * @param listener
	 *            the listener (<code>null</code> not permitted).
	 */

	@Override
	public void addChartMouseListener(ChartMouseListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("Null 'listener' argument.");
		}
		this.chartMouseListeners.add(ChartMouseListener.class, listener);
	}

	/**
	 * Removes a listener from the list of objects listening for chart mouse events.
	 * 
	 * @param listener
	 *            the listener.
	 */

	@Override
	public void removeChartMouseListener(ChartMouseListener listener) {
		this.chartMouseListeners.remove(ChartMouseListener.class, listener);
	}

	/**
	 * The idea is to modify the zooming options depending on the type of chart being displayed by
	 * the panel.
	 * 
	 * @param x
	 *            horizontal position of the popup.
	 * @param y
	 *            vertical position of the popup.
	 */

	@Override
	protected void displayPopupMenu(int x, int y) {

		if (this.popup == null) {
			return;
		}

		// go through each zoom menu item and decide whether or not to
		// enable it...
		boolean isDomainZoomable = false;
		boolean isRangeZoomable = false;
		Plot plot = this.chart != null ? this.chart.getPlot() : null;
		if (plot instanceof Zoomable) {
			Zoomable z = (Zoomable) plot;
			isDomainZoomable = z.isDomainZoomable();
			isRangeZoomable = z.isRangeZoomable();
		}

		if (this.zoomInDomainMenuItem != null) {
			this.zoomInDomainMenuItem.setEnabled(isDomainZoomable);
		}
		if (this.zoomOutDomainMenuItem != null) {
			this.zoomOutDomainMenuItem.setEnabled(isDomainZoomable);
		}
		if (this.zoomResetDomainMenuItem != null) {
			this.zoomResetDomainMenuItem.setEnabled(isDomainZoomable);
		}

		if (this.zoomInRangeMenuItem != null) {
			this.zoomInRangeMenuItem.setEnabled(isRangeZoomable);
		}
		if (this.zoomOutRangeMenuItem != null) {
			this.zoomOutRangeMenuItem.setEnabled(isRangeZoomable);
		}

		if (this.zoomResetRangeMenuItem != null) {
			this.zoomResetRangeMenuItem.setEnabled(isRangeZoomable);
		}

		if (this.zoomInBothMenuItem != null) {
			this.zoomInBothMenuItem.setEnabled(isDomainZoomable && isRangeZoomable);
		}
		if (this.zoomOutBothMenuItem != null) {
			this.zoomOutBothMenuItem.setEnabled(isDomainZoomable && isRangeZoomable);
		}
		if (this.zoomResetBothMenuItem != null) {
			this.zoomResetBothMenuItem.setEnabled(isDomainZoomable && isRangeZoomable);
		}

		coordinateTransformation.showPopupMenu(new Point(x, y), this, popup);
	}

	/**
	 * This method sets the coordinate transformation for this component.
	 */
	public void setCoordinateTransformation(CoordinateTransformation transformation) {
		this.coordinateTransformation = transformation;
	}

	/**
	 * This method will add the given selection listener to the list of objects which will be
	 * notified as soon as a selection is made.
	 */
	public void registerSelectionListener(SelectionListener listener) {
		this.selectionListeners.add(listener);
	}

	/**
	 * This one clears the complete list of registered selection listeners. This might be useful if
	 * a default listener should be replaced.
	 */
	public void clearSelectionListener() {
		this.selectionListeners.clear();
	}

	public void registerAxisNameResolver(AxisNameResolver axisNameResolver) {
		this.axisNameResolver = axisNameResolver;
	}

	@Override
	public Component getExportComponent() {
		return this;
	}

	@Override
	public String getExportName() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.cards.result_view.plot_view.title");
	}

	@Override
	public String getIdentifier() {
		return null;
	}

	@Override
	public String getExportIconName() {
		return I18N.getGUIMessage("gui.cards.result_view.plot_view.icon");
	}
}
