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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.plotter.PlotterConfigurationModel.PlotterSettingsChangedListener;
import com.rapidminer.gui.plotter.conditions.BasicPlotterCondition;
import com.rapidminer.gui.plotter.conditions.PlotterCondition;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.ModelMetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.MathFunctions;


/**
 * This adapter class can be used for simple plotter implementation which only need to overwrite the
 * methods they need. Most method implementations are rather restrictive and need to be overwritten
 * for the more sophisticated plotter possibilities. The complete plotting has to be done in the
 * {@link #paintComponent(Graphics)} method (which must be invoked by super.paintComponent in order
 * to get the correct color schemes), plotter updates should be initiated by invoking
 * {@link #repaint()}.
 *
 * Subclasses should at least react to {@link #setDataTable(DataTable)} in order to properly update
 * the plotter. Another method usually overridden is {@link #setPlotColumn(int, boolean)}. Other
 * overridden methods might include the methods for plot column and axis column handling.
 *
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public abstract class PlotterAdapter extends JPanel implements Plotter {

	public static final String PARAMETER_SUFFIX_LOG_SCALE = "_log_scale";

	public static final String PARAMETER_SUFFIX_ABSOLUTE_VALUES = "_absolute_values";

	public static final String PARAMETER_SUFFIX_AXIS = "_axis_";

	public static final String PARAMETER_PLOT_COLUMNS = "_plot_columns";

	public static final String PARAMETER_PLOT_COLUMN = "_plot_column";

	public static final String PARAMETER_SUFFIX_ZOOM_FACTOR = "_zoom_factor";

	public static final String PARAMETER_JITTER_AMOUNT = "_jitter_amount";

	public static final String PARAMETER_SUFFIX_SORTING = "_sorting";

	private static final long serialVersionUID = -8994113034200480325L;

	public static final double POINTSIZE = 7.0d;

	private static final int[] TICS = { 1, 2, 5 };

	public static final int MARGIN = 20;

	public static final int WEIGHT_BORDER_WIDTH = 5;

	public static final Font LABEL_FONT_BOLD = FontTools.getFont(Font.SANS_SERIF, Font.BOLD, 11);

	public static final Font LABEL_FONT = FontTools.getFont(Font.SANS_SERIF, Font.PLAIN, 11);

	protected static final Color GRID_COLOR = Color.lightGray;

	protected static final Color TOOLTIP_COLOR = new Color(170, 150, 240, 210);

	protected static final PointStyle ELLIPSOID_POINT_STYLE = new EllipsoidPointStyle();
	protected static final PointStyle RECTANGLE_POINT_STYLE = new RectanglePointStyle();
	protected static final PointStyle TRIANGUALAR_POINT_STYLE = new TriangularPointStyle();
	protected static final PointStyle TURNED_TRIANGUALAR_POINT_STYLE = new TurnedTriangularPointStyle();
	protected static final PointStyle STAR_POINT_STYLE = new StarPointStyle();

	protected static final Color[] LINE_COLORS = { new Color(255, 0, 0), new Color(0, 255, 0), new Color(0, 0, 255),
			new Color(255, 0, 255), Color.ORANGE, new Color(255, 255, 0), new Color(0, 255, 255), new Color(200, 100, 0),
			new Color(100, 200, 0), new Color(0, 100, 200), };

	protected static final PointStyle[] KNOWN_POINT_STYLES = { ELLIPSOID_POINT_STYLE, RECTANGLE_POINT_STYLE,
			TRIANGUALAR_POINT_STYLE, TURNED_TRIANGUALAR_POINT_STYLE, STAR_POINT_STYLE, ELLIPSOID_POINT_STYLE,
			RECTANGLE_POINT_STYLE, TRIANGUALAR_POINT_STYLE, TURNED_TRIANGUALAR_POINT_STYLE, STAR_POINT_STYLE };

	// stroked lines are very slow!!!
	protected static final Stroke[] LINE_STROKES = { new BasicStroke(2.0f) };

	protected static final LineStyle[] LINE_STYLES = new LineStyle[LINE_COLORS.length * LINE_STROKES.length];

	protected static final PointStyle[] POINT_STYLES = new PointStyle[LINE_COLORS.length * LINE_STROKES.length];

	protected final static Icon[] LINE_STYLE_ICONS = new LineStyleIcon[LINE_STYLES.length];

	static {
		for (int i = 0; i < LINE_STROKES.length; i++) {
			for (int j = 0; j < LINE_COLORS.length; j++) {
				LINE_STYLES[i * LINE_COLORS.length + j] = new LineStyle(LINE_COLORS[j], LINE_STROKES[i]);
			}
		}

		for (int i = 0; i < LINE_STYLE_ICONS.length; i++) {
			LINE_STYLE_ICONS[i] = new LineStyleIcon(i);
		}

		for (int i = 0; i < LINE_STROKES.length; i++) {
			for (int j = 0; j < LINE_COLORS.length; j++) {
				POINT_STYLES[i * LINE_COLORS.length + j] = KNOWN_POINT_STYLES[j];
			}
		}
	}

	/**
	 * This icon is displayed before the columns to indicate the color and line style (as a legend
	 * or key).
	 */
	protected static class LineStyleIcon implements Icon {

		private int index;

		private LineStyleIcon(int index) {
			this.index = index;
		}

		@Override
		public int getIconWidth() {
			return 20;
		}

		@Override
		public int getIconHeight() {
			return 2;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			LINE_STYLES[index].set((Graphics2D) g);
			g.drawLine(x, y, x + 20, y);
		}
	}

	/**
	 * This icon is displayed before the columns to indicate the color and line style (as a legend
	 * or key).
	 */
	protected static class LineColorIcon implements Icon {

		private Color color;

		public LineColorIcon(Color color) {
			this.color = color;
		}

		@Override
		public int getIconWidth() {
			return 20;
		}

		@Override
		public int getIconHeight() {
			return 2;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			g.setColor(color);
			g.drawLine(x, y, x + 20, y);
		}
	}

	/**
	 * The line style that should be used for plotting lines. Please note that rendering dashed
	 * lines might be very slow which was the reason that only solid lines can be used in recent
	 * releases.
	 */
	protected static class LineStyle {

		private Color color;

		private Stroke stroke;

		private LineStyle(Color color, Stroke stroke) {
			this.color = color;
			this.stroke = stroke;
		}

		public void set(Graphics2D g) {
			g.setColor(color);
			g.setStroke(stroke);
		}

		public Color getColor() {
			return color;
		}

		public Stroke getStroke() {
			return this.stroke;
		}
	}

	/**
	 * The point style that should be used for plotting points.
	 */
	protected static interface PointStyle {

		public Shape createShape(double x, double y);

	}

	/**
	 * The point style that should be used for plotting points.
	 */
	protected static class EllipsoidPointStyle implements PointStyle {

		@Override
		public Shape createShape(double x, double y) {
			return new Ellipse2D.Double(x - POINTSIZE / 2.0d, y - POINTSIZE / 2.0d, POINTSIZE, POINTSIZE);
		}
	}

	/**
	 * The point style that should be used for plotting points.
	 */
	protected static class RectanglePointStyle implements PointStyle {

		@Override
		public Shape createShape(double x, double y) {
			return new Rectangle2D.Double(x - POINTSIZE / 2.0d, y - POINTSIZE / 2.0d, POINTSIZE - 1, POINTSIZE - 1);
		}
	}

	/**
	 * The point style that should be used for plotting points.
	 */
	protected static class TriangularPointStyle implements PointStyle {

		@Override
		public Shape createShape(double x, double y) {
			int[] xPoints = new int[] { (int) Math.ceil(x - POINTSIZE / 2.0d), (int) Math.ceil(x),
					(int) Math.ceil(x + POINTSIZE / 2.0d) };
			int[] yPoints = new int[] { (int) Math.ceil(y + POINTSIZE / 2.0d), (int) Math.ceil(y - POINTSIZE / 2.0d),
					(int) Math.ceil(y + POINTSIZE / 2.0d) };
			return new Polygon(xPoints, yPoints, xPoints.length);
		}
	}

	/**
	 * The point style that should be used for plotting points.
	 */
	protected static class TurnedTriangularPointStyle implements PointStyle {

		@Override
		public Shape createShape(double x, double y) {
			int[] xPoints = new int[] { (int) Math.ceil(x - POINTSIZE / 2.0d), (int) Math.ceil(x),
					(int) Math.ceil(x + POINTSIZE / 2.0d) };
			int[] yPoints = new int[] { (int) Math.ceil(y - POINTSIZE / 2.0d), (int) Math.ceil(y + POINTSIZE / 2.0d),
					(int) Math.ceil(y - POINTSIZE / 2.0d) };
			return new Polygon(xPoints, yPoints, xPoints.length);
		}
	}

	/**
	 * The point style that should be used for plotting points.
	 */
	protected static class StarPointStyle implements PointStyle {

		@Override
		public Shape createShape(double x, double y) {
			double pointSize = POINTSIZE - 1.0d;
			int[] xPoints = new int[] { (int) Math.ceil(x - pointSize / 6.0d), (int) Math.ceil(x + pointSize / 6.0d),
					(int) Math.ceil(x + pointSize / 6.0d), (int) Math.ceil(x + pointSize / 2.0d),
					(int) Math.ceil(x + pointSize / 2.0d), (int) Math.ceil(x + pointSize / 6.0d),
					(int) Math.ceil(x + pointSize / 6.0d), (int) Math.ceil(x - pointSize / 6.0d),
					(int) Math.ceil(x - pointSize / 6.0d), (int) Math.ceil(x - pointSize / 2.0d),
					(int) Math.ceil(x - pointSize / 2.0d), (int) Math.ceil(x - pointSize / 6.0d) };
			int[] yPoints = new int[] { (int) Math.ceil(y - pointSize / 2.0d), (int) Math.ceil(y - pointSize / 2.0d),
					(int) Math.ceil(y - pointSize / 6.0d), (int) Math.ceil(y - pointSize / 6.0d),
					(int) Math.ceil(y + pointSize / 6.0d), (int) Math.ceil(y + pointSize / 6.0d),
					(int) Math.ceil(y + pointSize / 2.0d), (int) Math.ceil(y + pointSize / 2.0d),
					(int) Math.ceil(y + pointSize / 6.0d), (int) Math.ceil(y + pointSize / 6.0d),
					(int) Math.ceil(y - pointSize / 6.0d), (int) Math.ceil(y - pointSize / 6.0d) };
			return new Polygon(xPoints, yPoints, xPoints.length);
		}
	}

	protected PlotterConfigurationModel settings;

	// ===============================================================================================

	public PlotterAdapter(PlotterConfigurationModel settings) {
		this.settings = settings;
	}

	/**
	 * This default implementation does nothing. Subclasses might implement this method to enforce
	 * plotter generation for reporting / file writing.
	 */
	@Override
	public void forcePlotGeneration() {}

	/**
	 * This default implementation does nothing. Subclasses might use this hint that graphical
	 * updates should not be performed until all settings are made.
	 */
	@Override
	public void stopUpdates(boolean value) {}

	/**
	 * Invokes super method and sets correct color schemes. Should be overwritten by children, but
	 * invokation of this super method must still be performed in order to get correct color
	 * schemes.
	 */
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
	}

	/**
	 * Returns false. Subclasses should overwrite this method if they want to allow jittering.
	 * Subclasses overriding this method should also override {@link #setJitter(int)}.
	 */
	@Override
	public boolean canHandleJitter() {
		return false;
	}

	@Override
	public boolean canHandleContinousJittering() {
		return true;
	}

	/**
	 * Returns false. Subclasses should overwrite this method if they want to allow zooming.
	 * Subclasses overriding this method should also override {@link #setZooming(int)}.
	 */
	@Override
	public boolean canHandleZooming() {
		return false;
	}

	/**
	 * Returns -1. Subclasses overriding this method should also override {@link #getAxisName(int)},
	 * {@link #setAxis(int, int)}, and {@link #getNumberOfAxes()}.
	 */
	@Override
	public int getAxis(int axis) {
		return -1;
	}

	/**
	 * Returns null. Subclasses overriding this method should also override {@link #getAxis(int)},
	 * {@link #setAxis(int, int)}, and {@link #getNumberOfAxes()}.
	 */
	@Override
	public String getAxisName(int index) {
		return null;
	}

	/** Returns a line icon depending on the index. */
	@Override
	public Icon getIcon(int index) {
		return LINE_STYLE_ICONS[index % LINE_STYLE_ICONS.length];
	}

	/**
	 * Returns null. Subclasses which are able to derive a point from a mouse position should return
	 * a proper Id which can be used for object visualizers.
	 */
	@Override
	public String getIdForPos(int x, int y) {
		return null;
	}

	/**
	 * Returns 1. Subclasses might want to deliver another initial zoom factor between 1 and 100.
	 */
	@Override
	public int getInitialZoomFactor() {
		return 1;
	}

	/**
	 * Returns 0. Subclasses overriding this method should also override {@link #getAxisName(int)},
	 * {@link #setAxis(int, int)}, and {@link #getAxis(int)}.
	 */
	@Override
	public int getNumberOfAxes() {
		return 0;
	}

	/**
	 * Returns null. Subclasses might override this method in order to provide additional option
	 * components.
	 */
	@Override
	public JComponent getOptionsComponent(int index) {
		return null;
	}

	/**
	 * Returns false. Subclasses should override this method and return true for the columns which
	 * should be plotted.
	 */
	@Override
	public boolean getPlotColumn(int dimension) {
		return false;
	}

	/**
	 * Returns null. Subclasses might return another name more fitting the plot selection box or
	 * list.
	 */
	@Override
	public String getPlotName() {
		return null;
	}

	/**
	 * Returns this. Subclasses which do not want to use this object (JPanel) for plotting should
	 * directly implement {@link Plotter}.
	 */
	@Override
	public JComponent getPlotter() {
		return this;
	}

	/**
	 * Returns a {@link BasicPlotterCondition} allowing for all {@link DataTable}s. Subclasses
	 * should override this method in order to indicate that they might not be able to handle
	 * certain data tables.
	 */
	/**
	 * Returns the plotter component for rendering purposes like reporting. In most cases this will
	 * be the same component but sometimes a plotter might want to left out additional control
	 * elements etc.
	 *
	 * The default implementation returns the component delivered by getPlotter().
	 */
	@Override
	public JComponent getRenderComponent() {
		return getPlotter();
	}

	/**
	 * Returns a {@link BasicPlotterCondition} allowing for all {@link DataTable}s. Subclasses
	 * should override this method in order to indicate that they might not be able to handle
	 * certain data tables.
	 */
	@Override
	public PlotterCondition getPlotterCondition() {
		return new BasicPlotterCondition();
	}

	/**
	 * Returns null. Subclasses which are able to calculate the position in data space from a
	 * position in screen space should return the proper position. Please note that you have to
	 * override the method {@link #isProvidingCoordinates()}, too.
	 */
	@Override
	public Point2D getPositionInDataSpace(Point p) {
		return null;
	}

	/**
	 * Returns {@link #SINGLE_SELECTION}. Subclasses might override this method and return
	 * {@link #NO_SELECTION} or {@link #MULTIPLE_SELECTION}.
	 */
	@Override
	public int getValuePlotSelectionType() {
		return SINGLE_SELECTION;
	}

	/**
	 * Returns false. Subclasses should override this method if they want to provide an options
	 * dialog.
	 */
	@Override
	public boolean hasOptionsDialog() {
		return false;
	}

	/**
	 * Returns false. Subclasses might want to indicate that this plotter has an save (export) image
	 * button of its own by returning true.
	 */
	@Override
	public boolean hasSaveImageButton() {
		return false;
	}

	/**
	 * Returns false. Subclasses might override this method in order to indicate that this plotter
	 * is able to deliver plot coordinates. Please note that overriding subclasses should also
	 * override {@link #getPositionInDataSpace(Point)}.
	 */
	@Override
	public boolean isProvidingCoordinates() {
		return false;
	}

	/**
	 * Returns false. Subclasses might want to override this method to indicate that they are able
	 * to save the data into a file. In this case, the method {@link #save()} should also be
	 * overridden.
	 */
	@Override
	public boolean isSaveable() {
		return false;
	}

	/**
	 * Does nothing. Please note that subclasses which want to allow saving should also override the
	 * method {@link #isSaveable()}.
	 */
	@Override
	public void save() {}

	/**
	 * Does nothing. Subclasses overriding this method should also override {@link #getAxis(int)},
	 * {@link #getAxisName(int)}, and {@link #getNumberOfAxes()}.
	 */
	@Override
	public void setAxis(int plotterAxis, int dimension) {}

	/** Does nothing. Can be used for setting the current drag bounds in screen space. */
	@Override
	public void setDragBounds(int x, int y, int w, int h) {}

	/**
	 * Does nothing. Subclasses might override this method if they want to allow setting the actual
	 * draw range which might be different from the data range.
	 */
	@Override
	public void setDrawRange(double x, double y, double w, double h) {}

	/**
	 * Does nothing. Subclasses should overwrite this method if they want to allow jittering.
	 * Subclasses overriding this method should also override {@link #canHandleJitter()}.
	 */
	@Override
	public void setJitter(int jitter) {}

	/** Does nothing. Subclasses might override this method if they want to allow a key (legend). */
	@Override
	public void setKey(String key) {}

	/**
	 * Does nothing. This method might be used by subclasses if they want to react on mouse moves,
	 * e.g. by showing tool tips.
	 */
	@Override
	public void setMousePosInDataSpace(int mouseX, int mouseY) {}

	/**
	 * Does nothing. Subclasses should override this method if they want to allow plot column
	 * selection. In this case, the method {@link #getPlotColumn(int)} should also be overriden.
	 */
	@Override
	public void setPlotColumn(int dimension, boolean plot) {}

	/** Does nothing. */
	@Override
	public void setDataTable(DataTable dataTable) {}

	/**
	 * Does nothing. Subclasses should overwrite this method if they want to allow zooming.
	 * Subclasses overriding this method should also override {@link #canHandleZooming()}.
	 */
	@Override
	public void setZooming(int zooming) {}

	/**
	 * Does nothing. Subclasses might implement this method in order to provide an options dialog.
	 */
	@Override
	public void showOptionsDialog() {}

	/**
	 * Returns true if a log scale for this column is supported. The default implementation returns
	 * false.
	 */
	@Override
	public boolean isSupportingLogScale(int axis) {
		return false;
	}

	/**
	 * Returns true if a log scale for the plot columns is supported. The default implementation
	 * returns false.
	 */
	@Override
	public boolean isSupportingLogScaleForPlotColumns() {
		return false;
	}

	/**
	 * Sets if the given axis should be plotted with log scale. The default implementation does
	 * nothing.
	 */
	@Override
	public void setLogScale(int axis, boolean logScale) {}

	/**
	 * Sets if the plot columns should be plotted with log scale. The default implementation does
	 * nothing.
	 */
	@Override
	public void setLogScaleForPlotColumns(boolean logScale) {}

	/** Returns false. */
	@Override
	public boolean isSupportingAbsoluteValues() {
		return false;
	}

	/** Returns false. */
	@Override
	public boolean isSupportingSorting() {
		return false;
	}

	/** Does nothing. */
	@Override
	public void setAbsolute(boolean absolute) {}

	/** Does nothing. */
	@Override
	public void setSorting(boolean sorting) {}

	/**
	 * Invokes {@link #repaint()}. Will be invoked since all plotters are
	 * {@link com.rapidminer.datatable.DataTableListener}s.
	 */
	public final void dataTableUpdated(DataTable source) {
		int maxRowNumber = PlotterPanel.DEFAULT_MAX_NUMBER_OF_DATA_POINTS;
		String maxRowNumberString = ParameterService
				.getParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_ROWS_MAXIMUM);
		if (maxRowNumberString != null && maxRowNumberString.trim().length() > 0) {
			try {
				int newMaxRows = Integer.parseInt(maxRowNumberString);
				maxRowNumber = newMaxRows;
			} catch (NumberFormatException e) {
				// LogService.getGlobal().logWarning("Plotter: cannot read maximum number of plotter
				// points (was '"
				// + maxRowNumberString + "').");
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.plotter.PlotterAdapter.reading_maximum_number_of_plotter_points_error",
						maxRowNumberString);
			}
		}

		if (source.getNumberOfRows() > maxRowNumber) {
			DataTable sampledDataTable = source.sample(maxRowNumber);
			// LogService.getGlobal().logWarning("Cannot plot all data points, using only a sample
			// of "
			// + maxRowNumber + " rows.");
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.gui.plotter.PlotterAdapter.ploting_all_data_points_error", maxRowNumber);
			setDataTable(sampledDataTable);
		} else {
			setDataTable(source);
		}

		repaint();
	}

	// ===================================================================================
	// Helper methods
	// ===================================================================================

	public ColorProvider getColorProvider() {
		return new ColorProvider();
	}

	/**
	 * Creates a new {@link ColorProvider} which reduces the brightness of the returned colors.
	 *
	 * @param reduceBrightness
	 *            if <code>true</code>, will reduce brightness of returned colors
	 */
	public ColorProvider getColorProvider(boolean reduceBrightness) {
		return new ColorProvider(reduceBrightness);
	}

	protected PointStyle getPointStyle(int styleIndex) {
		return POINT_STYLES[styleIndex % POINT_STYLES.length];
	}

	/** This helper method can be used to draw a point in the given graphics object. */
	protected void drawPoint(Graphics2D g, double x, double y, Color color, Color borderColor) {
		drawPoint(g, ELLIPSOID_POINT_STYLE, x, y, color, borderColor);
	}

	/** This helper method can be used to draw a point in the given graphics object. */
	protected void drawPoint(Graphics2D g, PointStyle pointStyle, double x, double y, Color color, Color borderColor) {
		Shape pointShape = pointStyle.createShape(x, y);
		g.setColor(color);
		g.fill(pointShape);
		g.setColor(borderColor);
		g.draw(pointShape);
	}

	/** This method can be used to draw a legend on the given graphics context. */
	protected void drawLegend(Graphics graphics, DataTable table, int legendColumn) {
		drawLegend(graphics, table, legendColumn, 0, 255);
	}

	/** This method can be used to draw a legend on the given graphics context. */
	protected void drawLegend(Graphics graphics, DataTable table, int legendColumn, int xOffset, int alpha) {
		if (legendColumn < 0 || legendColumn > table.getNumberOfColumns() - 1) {
			return;
		}
		if (table.isNominal(legendColumn)) {
			String maxNominalValuesString = ParameterService
					.getParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_CLASSLIMIT);
			int maxNumberOfNominalValues = 10;
			try {
				if (maxNominalValuesString != null) {
					maxNumberOfNominalValues = Integer.parseInt(maxNominalValuesString);
				}
			} catch (NumberFormatException e) {
				// LogService.getGlobal().logWarning("Plotter: cannot parse maximal number of
				// nominal values for legend ("
				// + maxNominalValuesString +
				// ")! Using 10...");
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.plotter.PlotterAdapter.parsing_maximal_number_of_nominal_values_error",
						maxNominalValuesString);
			}
			if (maxNumberOfNominalValues == -1 || table.getNumberOfValues(legendColumn) <= maxNumberOfNominalValues) {
				drawNominalLegend(graphics, table, legendColumn, xOffset, alpha);
			} else {
				// LogService.getGlobal().logWarning("Plotter: cannot draw nominal legend since
				// number of different values is too high (more than "
				// +
				// maxNominalValuesString + ")! Using numerical legend instead.");
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.plotter.PlotterAdapter.drawing_nominal_legend_error", maxNominalValuesString);
				drawNumericalLegend(graphics, table, legendColumn, alpha);
			}
		} else if (table.isDate(legendColumn) || table.isTime(legendColumn) || table.isDateTime(legendColumn)) {
			drawDateLegend(graphics, table, legendColumn, alpha);
		} else {
			drawNumericalLegend(graphics, table, legendColumn, alpha);
		}
	}

	private void drawNominalLegend(Graphics graphics, DataTable table, int legendColumn, int xOffset, int alpha) {
		Graphics2D g = (Graphics2D) graphics.create();
		g.translate(xOffset, 0);

		// painting label name
		String legendName = table.getColumnName(legendColumn);
		g.drawString(legendName, MARGIN, 15);
		Rectangle2D legendNameBounds = LABEL_FONT.getStringBounds(legendName, g.getFontRenderContext());
		g.translate(legendNameBounds.getWidth(), 0);

		// painting values
		int numberOfValues = table.getNumberOfValues(legendColumn);
		int currentX = MARGIN;

		for (int i = 0; i < numberOfValues; i++) {
			if (currentX > getWidth()) {
				break;
			}
			String nominalValue = table.mapIndex(legendColumn, i);
			if (nominalValue.length() > 16) {
				nominalValue = nominalValue.substring(0, 16) + "...";
			}
			Shape colorBullet = new Ellipse2D.Double(currentX, 7, 7.0d, 7.0d);
			Color color = getColorProvider().getPointColor((double) i / (double) (numberOfValues - 1), alpha);
			g.setColor(color);
			g.fill(colorBullet);
			g.setColor(Color.black);
			g.draw(colorBullet);
			currentX += 12;
			g.drawString(nominalValue, currentX, 15);
			Rectangle2D stringBounds = LABEL_FONT.getStringBounds(nominalValue, g.getFontRenderContext());
			currentX += stringBounds.getWidth() + 15;
		}
	}

	private void drawDateLegend(Graphics graphics, DataTable table, int legendColumn, int alpha) {
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		synchronized (table) {
			Iterator<DataTableRow> i = table.iterator();
			while (i.hasNext()) {
				DataTableRow row = i.next();
				double colorValue = row.getValue(legendColumn);
				min = MathFunctions.robustMin(min, colorValue);
				max = MathFunctions.robustMax(max, colorValue);
			}
		}
		String minColorString = null;
		String maxColorString = null;
		if (table.isDate(legendColumn)) {
			minColorString = Tools.createDateAndFormat(min);
			maxColorString = Tools.createDateAndFormat(max);
		} else if (table.isTime(legendColumn)) {
			minColorString = Tools.createTimeAndFormat(min);
			maxColorString = Tools.createTimeAndFormat(max);
		} else if (table.isDateTime(legendColumn)) {
			minColorString = Tools.createDateTimeAndFormat(min);
			maxColorString = Tools.createDateTimeAndFormat(max);
		} else {
			minColorString = Tools.formatNumber(min);
			maxColorString = Tools.formatNumber(max);
		}

		drawNumericalLegend(graphics, getWidth(), minColorString, maxColorString, table.getColumnName(legendColumn), alpha);
	}

	private void drawNumericalLegend(Graphics graphics, DataTable table, int legendColumn, int alpha) {
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		synchronized (table) {
			Iterator<DataTableRow> i = table.iterator();
			while (i.hasNext()) {
				DataTableRow row = i.next();
				double colorValue = row.getValue(legendColumn);
				min = MathFunctions.robustMin(min, colorValue);
				max = MathFunctions.robustMax(max, colorValue);
			}
		}
		drawNumericalLegend(graphics, table.getColumnName(legendColumn), min, max, alpha);
	}

	/** This method can be used to draw a legend on the given graphics context. */
	private void drawNumericalLegend(Graphics graphics, String legendColumnName, double minColor, double maxColor,
			int alpha) {
		// key or legend
		String minColorString = Tools.formatNumber(minColor);
		String maxColorString = Tools.formatNumber(maxColor);
		drawNumericalLegend(graphics, getWidth(), minColorString, maxColorString, legendColumnName, alpha);
	}

	/** This method can be used to draw a legend on the given graphics context. */
	public void drawNumericalLegend(Graphics graphics, int width, String minColorString, String maxColorString,
			String legendColumnName, int alpha) {
		Graphics2D g = (Graphics2D) graphics.create();

		// painting label name
		g.drawString(legendColumnName, MARGIN, 15);
		Rectangle2D legendNameBounds = LABEL_FONT.getStringBounds(legendColumnName, g.getFontRenderContext());
		g.translate(legendNameBounds.getWidth(), 0);

		// painting legend

		Rectangle2D minStringBounds = LABEL_FONT.getStringBounds(minColorString, g.getFontRenderContext());
		Rectangle2D maxStringBounds = LABEL_FONT.getStringBounds(maxColorString, g.getFontRenderContext());
		int legendWidth = (int) (minStringBounds.getWidth() + 114 + maxStringBounds.getWidth());
		int keyX = MARGIN + width / 2 - legendWidth / 2;
		int keyY = (int) (MARGIN + 2 - minStringBounds.getHeight() / 2);
		g.setColor(Color.black);
		g.drawString(minColorString, keyX, keyY);
		keyX += minStringBounds.getWidth() + 10;
		for (int i = 0; i < 100; i++) {
			double scaledColor = i / 100.0d;
			Color lineColor = getColorProvider().getPointColor(scaledColor, alpha);
			g.setColor(lineColor);
			g.drawLine(keyX, keyY, keyX, keyY - 10);
			keyX++;
		}
		g.setColor(Color.black);
		Rectangle2D frame = new Rectangle2D.Double(keyX - 101, keyY - 11, 101, 11);
		g.draw(frame);
		keyX += 4;
		g.drawString(maxColorString, keyX, keyY);
	}

	/** This method can be used to draw a legend on the given graphics context. */
	public void drawSimpleNumericalLegend(Graphics graphics, int x, int y, String legendColumnName, String minColorString,
			String maxColorString) {
		Graphics2D g = (Graphics2D) graphics.create();

		// painting label name
		g.setFont(LABEL_FONT_BOLD);
		g.setColor(Color.black);
		g.drawString(legendColumnName, x, y + 1);
		Rectangle2D legendNameBounds = LABEL_FONT.getStringBounds(legendColumnName, g.getFontRenderContext());
		g.translate(legendNameBounds.getWidth() + 5, 0);

		// painting legend
		g.setFont(LABEL_FONT);
		g.setColor(Color.black);
		Rectangle2D minStringBounds = LABEL_FONT.getStringBounds(minColorString, g.getFontRenderContext());

		int keyX = x;
		int keyY = y;

		g.drawString(minColorString, keyX, keyY + 1);
		keyX += minStringBounds.getWidth() + 5;
		for (int i = 0; i < 100; i++) {
			double scaledColor = i / 100.0d;
			Color lineColor = getColorProvider().getPointColor(scaledColor, 255);
			g.setColor(lineColor);
			g.drawLine(keyX, keyY, keyX, keyY - 8);
			keyX++;
		}
		g.setColor(Color.black);
		Rectangle2D frame = new Rectangle2D.Double(keyX - 101, keyY - 8, 101, 8);
		g.draw(frame);
		keyX += 4;
		g.drawString(maxColorString, keyX, keyY + 1);
	}

	public void drawSimpleDateLegend(Graphics graphics, int x, int y, DataTable table, int legendColumn, double min,
			double max) {
		String minColorString = null;
		String maxColorString = null;
		if (table.isDate(legendColumn)) {
			minColorString = Tools.createDateAndFormat(min);
			maxColorString = Tools.createDateAndFormat(max);
		} else if (table.isTime(legendColumn)) {
			minColorString = Tools.createTimeAndFormat(min);
			maxColorString = Tools.createTimeAndFormat(max);
		} else if (table.isDateTime(legendColumn)) {
			minColorString = Tools.createDateTimeAndFormat(min);
			maxColorString = Tools.createDateTimeAndFormat(max);
		} else {
			minColorString = Tools.formatNumber(min);
			maxColorString = Tools.formatNumber(max);
		}

		drawSimpleNumericalLegend(graphics, x, y, table.getColumnName(legendColumn), minColorString, maxColorString);
	}

	protected void drawGenericNominalLegend(Graphics graphics, String[] names, PointStyle[] pointStyles, Color[] colors,
			int xOffset, int alpha) {
		Graphics2D g = (Graphics2D) graphics.create();
		g.translate(xOffset, 0);
		int numberOfValues = names.length;
		int currentX = MARGIN;
		for (int i = 0; i < numberOfValues; i++) {
			if (currentX > getWidth()) {
				break;
			}
			String nominalValue = names[i];
			if (nominalValue.length() > 16) {
				nominalValue = nominalValue.substring(0, 16) + "...";
			}
			Shape shape = pointStyles[i].createShape(currentX, 11);
			Color color = colors[i];
			g.setColor(color);
			g.fill(shape);
			g.setColor(Color.black);
			g.draw(shape);
			currentX += 8;
			g.drawString(nominalValue, currentX, 15);
			Rectangle2D stringBounds = LABEL_FONT.getStringBounds(nominalValue, g.getFontRenderContext());
			currentX += stringBounds.getWidth() + 15;
		}
	}

	protected void drawToolTip(Graphics2D g, ToolTip toolTip) {
		if (toolTip != null) {
			g.setFont(LABEL_FONT);
			Rectangle2D stringBounds = LABEL_FONT.getStringBounds(toolTip.getText(), g.getFontRenderContext());
			g.setColor(TOOLTIP_COLOR);
			Rectangle2D bg = new Rectangle2D.Double(toolTip.getX() - stringBounds.getWidth() / 2 - 4,
					toolTip.getY() - stringBounds.getHeight() / 2, stringBounds.getWidth() + 5,
					stringBounds.getHeight() + 3);
			g.fill(bg);
			g.setColor(Color.black);
			g.draw(bg);
			g.drawString(toolTip.getText(), (int) (toolTip.getX() - stringBounds.getWidth() / 2 - 2), toolTip.getY() + 6);
		}
	}

	protected int getNumberOfPlots(DataTable table) {
		int counter = 0;
		for (int i = 0; i < table.getNumberOfColumns(); i++) {
			if (getPlotColumn(i)) {
				counter++;
			}
		}
		return counter;
	}

	protected double getTicSize(DataTable dataTable, int column, double min, double max) {
		if (column < 0) {
			return Double.NaN;
		}
		if (getNumberOfPlots(dataTable) == 1 && dataTable.isNominal(column)) {
			if (dataTable.getNumberOfValues(column) <= 10) {
				return 1;
			} else {
				return getNumericalTicSize(min, max);
			}
		} else {
			return getNumericalTicSize(min, max);
		}
	}

	protected double getNumericalTicSize(double min, double max) {
		double delta = (max - min) / 5;
		double e = Math.floor(Math.log(delta) / Math.log(10));
		double factor = Math.pow(10, e);
		for (int i = TICS.length - 1; i >= 0; i--) {
			if (TICS[i] * factor <= delta) {
				return TICS[i] * factor;
			}
		}
		return factor;
	}

	protected double getMaxWeight(DataTable dataTable) {
		double maxWeight = Double.NaN;
		if (dataTable.isSupportingColumnWeights()) {
			maxWeight = Double.NEGATIVE_INFINITY;
			for (int c = 0; c < dataTable.getNumberOfColumns(); c++) {
				double weight = dataTable.getColumnWeight(c);
				if (!Double.isNaN(weight)) {
					maxWeight = Math.max(Math.abs(weight), maxWeight);
				}
			}
		}
		return maxWeight;
	}

	/**
	 * Returns a color for the given weight. If weight or maxWeight are Double.NaN, just Color.white
	 * will be returned.
	 */
	public static Color getWeightColor(double weight, double maxWeight) {
		Color weightColor = Color.white;
		if (!Double.isNaN(weight) && !Double.isNaN(maxWeight)) {
			weightColor = new Color(255, 255, 0, (int) (Math.abs(weight) / maxWeight * 100));
		}
		return weightColor;
	}

	protected void drawWeightRectangle(Graphics2D newSpace, DataTable dataTable, int column, double maxWeight,
			int plotterSize) {
		if (dataTable.isSupportingColumnWeights()) {
			newSpace.setColor(getWeightColor(dataTable.getColumnWeight(column), maxWeight));
			Rectangle2D weightRect = new Rectangle2D.Double(1, 1, plotterSize - 2, plotterSize - 2);
			newSpace.fill(weightRect);
			newSpace.setColor(Color.WHITE);
			int weightBorder = WEIGHT_BORDER_WIDTH + 1;
			weightRect = new Rectangle2D.Double(weightBorder, weightBorder, plotterSize - 2 * weightBorder,
					plotterSize - 2 * weightBorder);
			newSpace.fill(weightRect);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(800, 600);
	}

	/** Invokes force plot generation. */
	@Override
	public void prepareRendering() {
		forcePlotGeneration();
	}

	/** Do nothing. */
	@Override
	public void finishRendering() {}

	@Override
	public int getRenderHeight(int preferredHeight) {
		if (preferredHeight < 0) {
			return getPreferredSize().height;
		} else {
			return preferredHeight;
		}
	}

	@Override
	public int getRenderWidth(int preferredWidth) {
		if (preferredWidth < 0) {
			return getPreferredSize().width;
		} else {
			return preferredWidth;
		}
	}

	@Override
	public void render(Graphics graphics, int width, int height) {
		JComponent renderComponent = getRenderComponent();
		renderComponent.setSize(width, height);
		renderComponent.setDoubleBuffered(false);
		renderComponent.paint(graphics);
		renderComponent.setDoubleBuffered(true);
	}

	// =======================================================
	// Parameter Handling
	// =======================================================

	public PlotterConfigurationModel getPlotterSettings() {
		return settings;
	}

	public static String transformParameterName(String name) {
		if (name == null) {
			return "default";
		}
		String result = name.toLowerCase();
		result = result.replaceAll("\\W", "_");
		return result;
	}

	@Override
	public void applyParameterSetting(final DataTable dataTable, String key, String value) {
		// plotting axes?
		String compareKey = null;
		for (int i = 0; i < getNumberOfAxes(); i++) {
			String axisName = getAxisName(i);
			compareKey = transformParameterName(getPlotterName()) + PARAMETER_SUFFIX_AXIS + transformParameterName(axisName);
			if (compareKey.equals(key)) {
				if (value != null) {
					int columnIndex = dataTable.getColumnIndex(value);
					setAxis(i, columnIndex);
				}
				return;
			}

			if (isSupportingLogScale(i)) {
				compareKey = transformParameterName(getPlotterName()) + PARAMETER_SUFFIX_AXIS
						+ transformParameterName(axisName) + PARAMETER_SUFFIX_LOG_SCALE;
				if (compareKey.equals(key)) {
					setLogScale(i, Boolean.parseBoolean(value));
					return;
				}
			}
		}

		// plotting dimensions?
		switch (getValuePlotSelectionType()) {
			case Plotter.MULTIPLE_SELECTION:
				compareKey = transformParameterName(getPlotterName()) + PARAMETER_PLOT_COLUMNS;
				if (compareKey.equals(key)) {
					// searching indices of selected dimensions
					String[] names = ParameterTypeEnumeration.transformString2Enumeration(value);
					boolean[] selectedDimensions = new boolean[dataTable.getNumberOfColumns()];
					for (String name2 : names) {
						String name = name2.trim();
						int columnIndex = dataTable.getColumnIndex(name);
						if (columnIndex >= 0 && columnIndex < selectedDimensions.length) { // can
																							 // be
																							 // -1
																							 // if
																							 // column
																							 // names
																							 // have
																							 // changed
							selectedDimensions[columnIndex] = true;
						}
					}

					// switching all differing dimensions
					for (int i = 0; i < dataTable.getNumberOfColumns(); i++) {
						if (getPlotColumn(i) != selectedDimensions[i]) {
							setPlotColumn(i, selectedDimensions[i]);
						}
					}
					return;
				}
				break;
			case Plotter.SINGLE_SELECTION:
				compareKey = transformParameterName(getPlotterName()) + PARAMETER_PLOT_COLUMN;
				if (compareKey.equals(key)) {
					if (dataTable != null) {
						int columnIndex = dataTable.getColumnIndex(value);
						setPlotColumn(columnIndex, true);
					}
					return;
				}
				break;
			case Plotter.NO_SELECTION:
				// do nothing
				break;
		}
		if (isSupportingLogScaleForPlotColumns()) {
			compareKey = transformParameterName(getPlotterName()) + PARAMETER_PLOT_COLUMNS + PARAMETER_SUFFIX_LOG_SCALE;
			if (compareKey.equals(key)) {
				setLogScaleForPlotColumns("true".equals(value));
			}
		}

		// zooming?
		if (canHandleZooming()) {
			compareKey = transformParameterName(getPlotterName()) + PARAMETER_SUFFIX_ZOOM_FACTOR;
			if (compareKey.equals(key)) {
				int zoomFactor = Integer.valueOf(value);
				setZooming(zoomFactor);
				return;
			}
		}

		// jitter?
		if (canHandleJitter()) {
			compareKey = transformParameterName(getPlotterName()) + PARAMETER_JITTER_AMOUNT;
			if (compareKey.equals(key)) {
				int jitterAmount = Integer.valueOf(value);
				setJitter(jitterAmount);
				return;
			}
		}

		// sorting?
		if (isSupportingSorting()) {
			compareKey = transformParameterName(getPlotterName()) + PARAMETER_SUFFIX_SORTING;
			if (compareKey.equals(key)) {
				boolean sorting = Tools.booleanValue(value, false);
				setSorting(sorting);
				return;
			}
		}

		// absolute values?
		if (isSupportingAbsoluteValues()) {
			compareKey = transformParameterName(getPlotterName()) + PARAMETER_SUFFIX_ABSOLUTE_VALUES;
			if (compareKey.equals(key)) {
				boolean absoluteValues = Tools.booleanValue(value, false);
				setAbsolute(absoluteValues);
				return;
			}
		}

		// try additional parameter

		// remove plotter name
		String plotterName = transformParameterName(getPlotterName()) + "_";
		if (key.startsWith(plotterName)) {
			String actualKey = key.substring(plotterName.length());
			setAdditionalParameter(actualKey, value);
		}
	}

	/**
	 * The default implementation delivers an empty set.
	 *
	 * @param inputPort
	 */
	public List<ParameterType> getAdditionalParameterKeys(InputPort inputPort) {
		return new LinkedList<>();
	}

	/**
	 * If subclasses override this method, the MUST call the super implementation, since many
	 * parameters are evaluated there.
	 */
	public void setAdditionalParameter(String key, String value) {}

	@Override
	public List<ParameterType> getParameterTypes(InputPort inputPort) {
		List<ParameterType> types = new LinkedList<>();

		boolean inputDeliversAttributes = false;
		if (inputPort != null) {
			MetaData metaData = inputPort.getMetaData();
			if (metaData != null && (metaData instanceof ExampleSetMetaData || metaData instanceof ModelMetaData)) {
				inputDeliversAttributes = true;
			}
		}

		// plotting axes
		for (int i = 0; i < getNumberOfAxes(); i++) {
			String axisName = getAxisName(i);
			if (inputDeliversAttributes) {
				types.add(new ParameterTypeAttribute(
						transformParameterName(getPlotterName()) + PARAMETER_SUFFIX_AXIS + transformParameterName(axisName),
						"The name of the column which should be used for this axis", inputPort, true));
			} else {
				types.add(new ParameterTypeString(
						transformParameterName(getPlotterName()) + PARAMETER_SUFFIX_AXIS + transformParameterName(axisName),
						"The name of the column which should be used for this axis", true));
			}

			if (isSupportingLogScale(i)) {
				types.add(new ParameterTypeBoolean(
						transformParameterName(getPlotterName()) + PARAMETER_SUFFIX_AXIS + transformParameterName(axisName)
								+ PARAMETER_SUFFIX_LOG_SCALE,
						"Indicates if this axis should be plotter with a log scale.", false));
			}
		}

		// plotting dimensions
		switch (getValuePlotSelectionType()) {
			case Plotter.MULTIPLE_SELECTION:
				types.add(new ParameterTypeString(transformParameterName(getPlotterName()) + PARAMETER_PLOT_COLUMNS,
						"A comma separated list of the names of the columns which should be used for plotting.", true));
				break;
			case Plotter.SINGLE_SELECTION:
				if (inputDeliversAttributes) {
					types.add(new ParameterTypeAttribute(transformParameterName(getPlotterName()) + PARAMETER_PLOT_COLUMN,
							"The name of the column which should be used for plotting.", inputPort, true));
				} else {
					types.add(new ParameterTypeString(transformParameterName(getPlotterName()) + PARAMETER_PLOT_COLUMN,
							"The name of the column which should be used for plotting.", true));
				}
				break;
			case Plotter.NO_SELECTION:
				// do nothing
				break;
		}

		// zooming
		if (canHandleZooming()) {
			types.add(new ParameterTypeInt(transformParameterName(getPlotterName()) + PARAMETER_SUFFIX_ZOOM_FACTOR,
					"The zoom factor for this plotter.", 1, 100, getInitialZoomFactor()));
		}

		// jitter
		if (canHandleJitter()) {
			types.add(new ParameterTypeInt(transformParameterName(getPlotterName()) + PARAMETER_JITTER_AMOUNT,
					"The jittering amount for this plotter.", 0, 100, 0));
		}

		// sorting
		if (isSupportingSorting()) {
			types.add(new ParameterTypeBoolean(transformParameterName(getPlotterName()) + PARAMETER_SUFFIX_SORTING,
					"Indicates if the plotter should sort the values according to the selected column.", false));
		}

		// absolute values
		if (isSupportingAbsoluteValues()) {
			types.add(new ParameterTypeBoolean(transformParameterName(getPlotterName()) + PARAMETER_SUFFIX_ABSOLUTE_VALUES,
					"Indicates if the plotter should use absolute values.", false));
		}

		// additional parameters
		List<ParameterType> additionalTypes = getAdditionalParameterKeys(inputPort);
		for (ParameterType type : additionalTypes) {
			type.setKey(transformParameterName(getPlotterName()) + "_" + type.getKey());
			types.add(type);
		}

		return types;
	}

	@Override
	public void plotterChanged(String plotterName) {
		// Do nothing
	}

	@Override
	public List<PlotterSettingsChangedListener> getListeningObjects() {
		List<PlotterSettingsChangedListener> listeningObjects = new LinkedList<>();
		return listeningObjects;
	}

	@Override
	public void settingChanged(String generalKey, String specificKey, String value) {
		if (settings.getDataTable() != null) {
			applyParameterSetting(settings.getDataTable(), specificKey, value);
		}
	}

	@Override
	public void setCoordinateTransformation(CoordinateTransformation transformation) {
		// throw new UnsupportedOperationException("The plotter " + getName() +
		// " doesn't support coordinate transformations.");
	}

	@Override
	public String getIdentifier() {
		return getPlotterName();
	}

	@Override
	public Component getExportComponent() {
		return this;
	}

	@Override
	public String getExportIconName() {
		return I18N.getGUIMessage("gui.cards.result_view.plot_view.icon");
	}

	@Override
	public String getExportName() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.cards.result_view.plot_view.title");
	}
}
