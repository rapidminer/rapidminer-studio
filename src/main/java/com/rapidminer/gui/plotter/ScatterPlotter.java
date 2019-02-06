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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.MathFunctions;


/**
 * The color plotter can display up to two dimensions and uses color schemes to indicate the third
 * dimension.
 * 
 * @author Ingo Mierswa, Simon Fischer
 * @deprecated since 9.2.0
 */
@Deprecated
public class ScatterPlotter extends PlotterAdapter {

	private static final long serialVersionUID = -6640810053422867017L;

	private static final Font SCALED_LABEL_FONT = LABEL_FONT.deriveFont(AffineTransform.getScaleInstance(1, -1));

	public static final String[] POINT_TYPES = new String[] { "lines_and_points", "lines", "points" };

	public static final int LINES_AND_POINTS = 0;

	public static final int LINES = 1;

	public static final int POINTS = 2;

	public static final int X_AXIS = 0;

	public static final int Y_AXIS = 1;

	private static final int LABEL_MARGIN_X = 15;

	private static final int LABEL_MARGIN_Y = 50;

	private static final int AXIS_PRECISION = 2;

	private transient DataTable dataTable;

	protected List<Plot> plots = new LinkedList<>();

	private double minX, maxX, minY, maxY;

	private double minColor, maxColor;

	private double xTicSize, yTicSize;

	private int colorColumn = -1;

	private double drawMinX = Double.NEGATIVE_INFINITY;

	private double drawMaxX = Double.POSITIVE_INFINITY;

	private double drawMinY = Double.NEGATIVE_INFINITY;

	private double drawMaxY = Double.POSITIVE_INFINITY;

	private int[] axis = new int[] { -1, -1 };

	/** The column which is currently used as x-axis for the plotter. */
	private int currentPlotterXAxis = -1;

	/** The column which is currently used as y-axis for the plotter. */
	private int currentPlotterYAxis = -1;

	private boolean[] columns;

	private String currentToolTip = null;

	private double toolTipX = 0.0d;

	private double toolTipY = 0.0d;

	private int dragX, dragY, dragWidth, dragHeight;

	private boolean drawAxes = true;

	private boolean drawLabel = true;

	private boolean draw2DLines = true;

	private boolean drawLegend = true;

	private String key = null;

	private JComboBox<String> pointTypeSelection;

	private int pointType = LINES_AND_POINTS;

	private int jitterAmount = 0;

	/** The transformations from pixel space into data space. */
	AffineTransform transform;
	transient AxisTransformation xTransformation = new AxisTransformationId();
	transient AxisTransformation yTransformation = new AxisTransformationId();

	public ScatterPlotter(PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.white);
		this.pointTypeSelection = new JComboBox<>(POINT_TYPES);
		this.pointTypeSelection.setToolTipText("Indicates which type of points should be used for plotting.");
		this.pointTypeSelection.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setPointType(pointTypeSelection.getSelectedIndex());
			}
		});
	}

	public ScatterPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
		this(settings);
		setDataTable(dataTable);
	}

	@Override
	public void setDataTable(DataTable dataTable) {
		super.setDataTable(dataTable);
		this.dataTable = dataTable;
		columns = new boolean[dataTable.getNumberOfColumns()];
	}

	public void setPointType(int pointType) {
		this.pointType = pointType;
		repaint();
	}

	@Override
	public String getAxisName(int index) {
		switch (index) {
			case X_AXIS:
				return "x-Axis";
			case Y_AXIS:
				return "y-Axis";
			default:
				return "none";
		}
	}

	@Override
	public int getValuePlotSelectionType() {
		return MULTIPLE_SELECTION;
	}

	@Override
	public boolean isSaveable() {
		return true;
	}

	@Override
	public void save() {
		JFileChooser chooser = SwingTools.createFileChooser("file_chooser.save", null, false, new FileFilter[0]);
		if (chooser.showSaveDialog(ScatterPlotter.this) == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			try (FileWriter fw = new FileWriter(file); PrintWriter out = new PrintWriter(fw)) {
				dataTable.write(out);
			} catch (Exception ex) {
				SwingTools.showSimpleErrorMessage("cannot_write_to_file_0", ex, file);
			}
		}
	}

	@Override
	public int getNumberOfAxes() {
		return axis.length;
	}

	@Override
	public void setAxis(int index, int dimension) {
		if (axis[index] != dimension) {
			axis[index] = dimension;
			repaint();
		}
	}

	@Override
	public int getAxis(int index) {
		return axis[index];
	}

	@Override
	public JComponent getOptionsComponent(int index) {
		if (index == 0) {
			return pointTypeSelection;
		} else {
			return null;
		}
	}

	/** Returns true. */
	@Override
	public boolean canHandleJitter() {
		return true;
	}

	/** Sets the level of jitter and initiates a repaint. */
	@Override
	public void setJitter(int jitter) {
		this.jitterAmount = jitter;
		repaint();
	}

	/** Disables all plotting but does not invoke repaint. */
	protected void clearPlotColumns() {
		for (int i = 0; i < columns.length; i++) {
			columns[i] = false;
		}
	}

	@Override
	public void setPlotColumn(int index, boolean plot) {
		if (this.columns[index] != plot) {
			columns[index] = plot;
		}
		repaint();
	}

	@Override
	public boolean getPlotColumn(int index) {
		return columns[index];
	}

	public void setDrawLegend(boolean drawLegend) {
		this.drawLegend = drawLegend;
	}

	public boolean getDrawLegend() {
		return this.drawLegend;
	}

	@Override
	public Point2D getPositionInDataSpace(Point point) {
		Point2D p = null;
		if (transform != null) {
			try {
				p = transform.inverseTransform(point, null);
				p = new Point2D.Double(xTransformation.inverseTransform(p.getX()),
						yTransformation.inverseTransform(p.getY()));
			} catch (java.awt.geom.NoninvertibleTransformException ex) {
				// do nothing --> return null
			}
		}
		return p;
	}

	/**
	 * Sets the draw range in data space. Please note that you might have to transform coordinated
	 * from mouse (plotter) space into data space before with the method
	 * {@link #getPositionInDataSpace(Point)}.
	 */
	@Override
	public void setDrawRange(double drawMinX, double drawMaxX, double drawMinY, double drawMaxY) {
		if (drawMinX == -1 || drawMaxX == -1 || drawMinY == -1 || drawMaxY == -1) {
			this.drawMinX = Double.NEGATIVE_INFINITY;
			this.drawMaxX = Double.POSITIVE_INFINITY;
			this.drawMinY = Double.NEGATIVE_INFINITY;
			this.drawMaxY = Double.POSITIVE_INFINITY;
		} else {
			this.drawMinX = drawMinX;
			this.drawMaxX = drawMaxX;
			this.drawMinY = drawMinY;
			this.drawMaxY = drawMaxY;
		}
		repaint();
	}

	private int getNumberOfCurrentlySelectedPlots() {
		int counter = 0;
		for (int column = 0; column < columns.length; column++) {
			if (columns[column]) {
				counter++;
			}
		}
		return counter;
	}

	private synchronized void prepareData() {
		synchronized (plots) {
			plots.clear();

			maxX = maxY = maxColor = Double.NEGATIVE_INFINITY;
			minX = minY = minColor = Double.POSITIVE_INFINITY;

			if (axis[X_AXIS] < 0) {
				return;
			}

			currentPlotterXAxis = axis[X_AXIS];
			currentPlotterYAxis = -1;
			for (int column = 0; column < columns.length; column++) {
				if (columns[column]) {
					String name = dataTable.getColumnName(column);
					Plot points = new Plot(name, column);
					synchronized (dataTable) {
						Iterator<DataTableRow> i = dataTable.iterator();
						// get min and max color
						if (axis[Y_AXIS] != -1 && getNumberOfCurrentlySelectedPlots() == 1) {
							colorColumn = column;
							while (i.hasNext()) {
								DataTableRow row = i.next();
								double color = row.getValue(colorColumn);
								minColor = MathFunctions.robustMin(minColor, color);
								maxColor = MathFunctions.robustMax(maxColor, color);
							}
							i = dataTable.iterator();
						}
						ColorProvider colorProvider = getColorProvider();
						while (i.hasNext()) {
							DataTableRow row = i.next();
							try {
								double x = row.getValue(currentPlotterXAxis);
								double y = Double.NaN;
								double color = Double.NaN;
								Color borderColor = Color.BLACK;
								if (axis[Y_AXIS] != -1) {
									currentPlotterYAxis = axis[Y_AXIS];
									y = row.getValue(currentPlotterYAxis);
									if (getNumberOfCurrentlySelectedPlots() == 1) {
										color = colorProvider.getPointColorValue(this.dataTable, row, colorColumn, minColor,
												maxColor);
										borderColor = colorProvider.getPointBorderColor(this.dataTable, row, colorColumn);
									}
								} else {
									currentPlotterYAxis = column;
									y = row.getValue(column);
								}

								ColorPlotterPoint currentPoint = new ColorPlotterPoint(this, row.getId(), x, y, color,
										borderColor);
								if (currentPoint.isIn(drawMinX, drawMaxX, drawMinY, drawMaxY)) {
									points.add(currentPoint);
									minX = Math.min(x, minX);
									maxX = Math.max(x, maxX);
									minY = Math.min(y, minY);
									maxY = Math.max(y, maxY);
								}
							} catch (NumberFormatException e) {
								throw new IllegalArgumentException("Not a numerical data column: " + column);
							}
						}
						if (this.jitterAmount > 0) {
							Random jitterRandom = new Random(2001);
							double oldXRange = maxX - minX;
							double oldYRange = maxY - minY;
							Iterator<ColorPlotterPoint> p = points.iterator();
							while (p.hasNext()) {
								ColorPlotterPoint point = p.next();
								if (Double.isInfinite(oldXRange) || Double.isNaN(oldXRange)) {
									oldXRange = 0;
								}
								if (Double.isInfinite(oldYRange) || Double.isNaN(oldYRange)) {
									oldYRange = 0;
								}
								double pertX = oldXRange * (jitterAmount / 200.0d) * jitterRandom.nextGaussian();
								double pertY = oldYRange * (jitterAmount / 200.0d) * jitterRandom.nextGaussian();
								double x = point.getX() + pertX;
								double y = point.getY() + pertY;
								minX = Math.min(x, minX);
								maxX = Math.max(x, maxX);
								minY = Math.min(y, minY);
								maxY = Math.max(y, maxY);
								point.setX(x);
								point.setY(y);
							}
						}
						plots.add(points);
					}
				}
			}
		}

		if (!Double.isInfinite(drawMinX)) {
			minX = drawMinX;
		}
		if (!Double.isInfinite(drawMaxX)) {
			maxX = drawMaxX;
		}
		if (!Double.isInfinite(drawMinY)) {
			minY = drawMinY;
		}
		if (!Double.isInfinite(drawMaxY)) {
			maxY = drawMaxY;
		}

		if (dataTable.getNumberOfRows() == 0) {
			minX = minY = 0;
			maxX = maxY = 1;
		}

		if (minX == maxX) {
			minX -= 0.5;
			maxX += 0.5;
		}
		if (minY == maxY) {
			minY -= 0.5;
			maxY += 0.5;
		}

		xTicSize = getTicSize(dataTable, currentPlotterXAxis, minX, maxX);
		yTicSize = getTicSize(dataTable, currentPlotterYAxis, minY, maxY);

		minX = xTransformation.adaptTicsMin(minX, xTicSize);
		maxX = xTransformation.adaptTicsMax(maxX, xTicSize);
		minY = yTransformation.adaptTicsMin(minY, yTicSize);
		maxY = yTransformation.adaptTicsMax(maxY, yTicSize);
	}

	@Override
	public void setKey(String key) {
		this.key = key;
	}

	public void setDrawAxes(boolean drawAxes) {
		this.drawAxes = drawAxes;
		repaint();
	}

	public void setDrawLabel(boolean drawLabel) {
		this.drawLabel = drawLabel;
		repaint();
	}

	/** Sets the mouse position in the shown data space. */
	@Override
	public void setMousePosInDataSpace(int x, int y) {
		ColorPlotterPoint point = getPlotterPointForPos(x, y);
		if (point != null) {
			String id = point.getId();
			if (id != null) {
				setToolTip(id, xTransformation.transform(point.getX()), yTransformation.transform(point.getY()));
			} else {
				setToolTip(null, 0.0d, 0.0d);
			}
		} else {
			setToolTip(null, 0.0d, 0.0d);
		}
	}

	@Override
	public String getIdForPos(int x, int y) {
		ColorPlotterPoint point = getPlotterPointForPos(x, y);
		if (point != null) {
			return point.getId();
		} else {
			return null;
		}
	}

	private synchronized ColorPlotterPoint getPlotterPointForPos(int x, int y) {
		synchronized (plots) {
			for (Plot plot : plots) {
				for (ColorPlotterPoint current : plot) {
					try {
						if (current.contains(x, y)) {
							return current;
						}
					} catch (IllegalArgumentException e) {
						// cannot apply axis transformation
						return null;
					}
				}
			}
		}
		return null;
	}

	@Override
	public void setDragBounds(int dragX, int dragY, int dragWidth, int dragHeight) {
		this.dragX = dragX;
		this.dragY = dragY;
		this.dragWidth = dragWidth;
		this.dragHeight = dragHeight;
		repaint();
	}

	private void setToolTip(String toolTip, double x, double y) {
		this.currentToolTip = toolTip;
		this.toolTipX = x;
		this.toolTipY = y;
		repaint();
	}

	protected synchronized void drawPoints(Graphics2D g, double dx, double dy, double sx, double sy) {
		synchronized (plots) {
			if (plots.size() == 0) {
				return;
			}

			int c = 0;
			Iterator<Plot> p = plots.iterator();
			while (p.hasNext()) {
				Plot plot = p.next();
				if (plot.size() > 0) {
					// draw path
					Iterator<ColorPlotterPoint> i = plot.iterator();
					if (pointType != POINTS && axis[Y_AXIS] < 0 && draw2DLines) {
						GeneralPath path = new GeneralPath();
						boolean first = true;
						while (i.hasNext()) {
							ColorPlotterPoint plotterPoint = i.next();
							float gSpaceX = (float) ((xTransformation.transform(plotterPoint.getX()) + dx) * sx);
							float gSpaceY = (float) ((yTransformation.transform(plotterPoint.getY()) + dy) * sy);
							if (first) {
								path.moveTo(gSpaceX, gSpaceY);
							} else {
								path.lineTo(gSpaceX, gSpaceY);
							}
							first = false;
						}
						plot.getLineStyle().set(g);
						g.draw(path);
					}
					if (this.pointType == LINES_AND_POINTS || this.pointType == POINTS) {
						// draw points
						g.setStroke(new BasicStroke());
						i = plot.iterator();
						ColorProvider colorProvider = getColorProvider();
						while (i.hasNext()) {
							ColorPlotterPoint plotterPoint = i.next();
							Color pointColor = plot.getLineStyle().getColor();
							PointStyle pointStyle = plot.getPointStyle();
							if (axis[Y_AXIS] >= 0) {
								pointColor = colorProvider.getPointColor(plotterPoint.getColor());
								if (plots.size() <= 1) {
									pointStyle = ELLIPSOID_POINT_STYLE;
								}
							}
							Color pointBorderColor = plotterPoint.getBorderColor();
							try {
								float gSpaceX = (float) ((xTransformation.transform(plotterPoint.getX()) + dx) * sx);
								float gSpaceY = (float) ((yTransformation.transform(plotterPoint.getY()) + dy) * sy);
								drawPoint(g, pointStyle, gSpaceX, gSpaceY, pointColor, pointBorderColor);
							} catch (IllegalArgumentException e) {
								// LogService.getGlobal().log("Cannot apply axis scale transformation to point ("
								// + plotterPoint.getX() + "," +
								// plotterPoint.getY() + "), skipping...", LogService.WARNING);
								LogService
										.getRoot()
										.log(Level.WARNING,
												"com.rapidminer.gui.plotter.ScatterPlotter.applying_axis_scale_transformation_error",
												new Object[] { plotterPoint.getX(), plotterPoint.getY() });
							}
						}
					}
					c = (c + 1) % LINE_STYLES.length;
				}
			}
		}
	}

	private void drawToolTip(Graphics2D g, double dx, double dy, double sx, double sy) {
		if (currentToolTip != null) {
			g.setFont(SCALED_LABEL_FONT);
			Rectangle2D stringBounds = SCALED_LABEL_FONT.getStringBounds(currentToolTip, g.getFontRenderContext());
			g.setColor(TOOLTIP_COLOR);
			Rectangle2D bg = new Rectangle2D.Double((toolTipX + dx) * sx - stringBounds.getWidth() / 2 - 4, (toolTipY + dy)
					* sy + 3, stringBounds.getWidth() + 5, Math.abs(stringBounds.getHeight()) + 3);
			g.fill(bg);
			g.setColor(Color.black);
			g.draw(bg);
			g.drawString(currentToolTip, (float) ((toolTipX + dx) * sx - stringBounds.getWidth() / 2) - 2,
					(float) ((toolTipY + dy) * sy) + 6);
		}
	}

	protected void drawGrid(Graphics2D g, double dx, double dy, double sx, double sy) {
		g.setFont(SCALED_LABEL_FONT);
		int numberOfXTics = (int) Math.ceil((maxX - minX) / xTicSize) + 1;
		for (int i = 0; i < numberOfXTics; i++) {
			drawVerticalTic(g, i, dx, dy, sx, sy);
		}

		int numberOfYTics = (int) Math.ceil((maxY - minY) / yTicSize) + 1;
		for (int i = 0; i < numberOfYTics; i++) {
			drawHorizontalTic(g, i, dx, dy, sx, sy);
		}
	}

	private void drawVerticalTic(Graphics2D g, int ticNumber, double dx, double dy, double sx, double sy) {
		double xValue = ticNumber * xTicSize + minX;
		double x = xTransformation.transform(xValue);
		g.setColor(GRID_COLOR);
		g.draw(new Line2D.Double((x + dx) * sx, (yTransformation.transform(minY) + dy) * sy, (x + dx) * sx, (yTransformation
				.transform(maxY) + dy) * sy));
		g.setColor(Color.black);
		if (drawAxes) {
			String label = null;
			if (getNumberOfPlots(dataTable) == 1 && dataTable.isNominal(currentPlotterXAxis)) {
				int index = (int) Math.round(xValue);
				if (index >= 0 && index < dataTable.getNumberOfValues(currentPlotterXAxis)) {
					label = dataTable.mapIndex(currentPlotterXAxis, index);
				}
			} else if (getNumberOfPlots(dataTable) == 1 && dataTable.isDate(currentPlotterXAxis)) {
				long index = Math.round(xValue);
				label = Tools.formatDate(new Date(index));
			} else if (getNumberOfPlots(dataTable) == 1 && dataTable.isTime(currentPlotterXAxis)) {
				long index = Math.round(xValue);
				label = Tools.formatTime(new Date(index));
			} else if (getNumberOfPlots(dataTable) == 1 && dataTable.isDateTime(currentPlotterXAxis)) {
				long index = Math.round(xValue);
				label = Tools.formatDateTime(new Date(index));
			} else {
				label = Tools.formatNumber(xValue, AXIS_PRECISION);
			}
			if (label != null) {
				Rectangle2D stringBounds = SCALED_LABEL_FONT.getStringBounds(label, g.getFontRenderContext());
				g.drawString(label, (float) ((x + dx) * sx - stringBounds.getWidth() / 2),
						(float) ((yTransformation.transform(minY) + dy) * sy + stringBounds.getHeight()));
			}
		}
	}

	private void drawHorizontalTic(Graphics2D g, int ticNumber, double dx, double dy, double sx, double sy) {
		double yValue = ticNumber * yTicSize + minY;
		double y = yTransformation.transform(yValue);
		g.setColor(GRID_COLOR);
		g.draw(new Line2D.Double((xTransformation.transform(minX) + dx) * sx, (y + dy) * sy, (xTransformation
				.transform(maxX) + dx) * sx, (y + dy) * sy));
		g.setColor(Color.black);
		if (drawAxes) {
			String label = null;
			if (getNumberOfPlots(dataTable) == 1 && dataTable.isNominal(currentPlotterYAxis)) {
				int index = (int) Math.round(yValue);
				if (index >= 0 && index < dataTable.getNumberOfValues(currentPlotterYAxis)) {
					label = dataTable.mapIndex(currentPlotterYAxis, index);
				}
			} else if (getNumberOfPlots(dataTable) == 1 && dataTable.isDate(currentPlotterYAxis)) {
				long index = Math.round(yValue);
				label = Tools.formatDate(new Date(index));
			} else if (getNumberOfPlots(dataTable) == 1 && dataTable.isTime(currentPlotterYAxis)) {
				long index = Math.round(yValue);
				label = Tools.formatTime(new Date(index));
			} else if (getNumberOfPlots(dataTable) == 1 && dataTable.isDateTime(currentPlotterYAxis)) {
				long index = Math.round(yValue);
				label = Tools.formatDateTime(new Date(index));
			} else {

				label = Tools.formatNumber(yValue, AXIS_PRECISION);
			}
			if (label != null) {
				Rectangle2D stringBounds = SCALED_LABEL_FONT.getStringBounds(label, g.getFontRenderContext());
				g.drawString(label, (float) ((xTransformation.transform(minX) + dx) * sx - stringBounds.getWidth()),
						(float) ((y + dy) * sy - stringBounds.getHeight() / 2 - stringBounds.getY()));
			}
		}
	}

	private void draw(Graphics2D g, int pixWidth, int pixHeight) {
		double sx = 0.0d;
		double sy = 0.0d;
		try {
			if (drawAxes) {
				sx = ((double) pixWidth - LABEL_MARGIN_Y)
						/ (xTransformation.transform(maxX) - xTransformation.transform(minX));
				sy = ((double) pixHeight - LABEL_MARGIN_X)
						/ (yTransformation.transform(maxY) - yTransformation.transform(minY));
			} else {
				sx = pixWidth / (xTransformation.transform(maxX) - xTransformation.transform(minX));
				sy = pixHeight / (yTransformation.transform(maxY) - yTransformation.transform(minY));
			}
		} catch (IllegalArgumentException e) {
			g.scale(1, -1);
			g.drawString("Cannot apply axis transformation. Please make sure that the value range", 0, -60);
			g.drawString("can be transformed by the selected axis transformation, for example", 0, -40);
			g.drawString("negative values or zero cannot be transformed by a log scale transformation", 0, -20);
			g.drawString("(applying a normalization operator to the desired range might help). ", 0, 0);
			return;
		}

		Graphics2D coordinateSpace = (Graphics2D) g.create();
		if (drawAxes) {
			coordinateSpace.translate(LABEL_MARGIN_Y, LABEL_MARGIN_X);
		}

		if (Double.isNaN(sx) || Double.isNaN(sy)) {
			coordinateSpace.scale(1, -1);
			coordinateSpace.drawString("No data points available (yet).", 0, -20);
			coordinateSpace.drawString("Zooming out with a right click might help.", 0, 0);
		} else {
			if (drawAxes) {
				transform.translate(LABEL_MARGIN_Y, LABEL_MARGIN_X);
			}
			transform.scale(sx, sy);
			transform.translate(-xTransformation.transform(minX), -yTransformation.transform(minY));

			drawGrid(coordinateSpace, -xTransformation.transform(minX), -yTransformation.transform(minY), sx, sy);
			drawPoints(coordinateSpace, -xTransformation.transform(minX), -yTransformation.transform(minY), sx, sy);
			drawToolTip(coordinateSpace, -xTransformation.transform(minX), -yTransformation.transform(minY), sx, sy);
		}
		coordinateSpace.dispose();
	}

	private void drawDragRectangle(Graphics2D g) {
		if (dragX != -1 && dragY != -1 && dragWidth != -1 && dragHeight != -1) {
			g.setColor(Color.gray);
			Rectangle2D dragBounds = new Rectangle2D.Double(dragX, dragY, dragWidth, dragHeight);
			g.draw(dragBounds);
		}
	}

	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		paint2DPlots((Graphics2D) graphics);
	}

	public void paint2DPlots(Graphics2D g) {
		int pixWidth = getWidth() - 2 * MARGIN;
		int pixHeight = getHeight() - 2 * MARGIN;

		// translate to ignore margins
		Graphics2D scaled = (Graphics2D) g.create();
		scaled.translate(MARGIN, MARGIN);
		scaled.translate(0, pixHeight + 1);

		// grid, data, ...
		prepareData();

		// actual plots
		if (plots.size() == 0) {
			scaled.drawString("No plots selected.", 0, 0);
		} else {
			scaled.scale(1, -1);
			g.setColor(Color.black);

			transform = new AffineTransform();
			transform.translate(MARGIN, MARGIN);
			transform.translate(0, pixHeight + 1);
			transform.scale(1, -1);

			draw(scaled, pixWidth, pixHeight);
		}
		scaled.dispose();

		// x-axis label
		if (drawLabel && axis[X_AXIS] >= 0) {
			String xAxisLabel = dataTable.getColumnName(axis[X_AXIS]);
			Rectangle2D stringBounds = SCALED_LABEL_FONT.getStringBounds(xAxisLabel, g.getFontRenderContext());
			g.drawString(xAxisLabel, MARGIN + (int) (pixWidth / 2.0d - stringBounds.getWidth() / 2.0d), MARGIN
					+ (int) (pixHeight + stringBounds.getY()) + 3);
		}

		// y-axis label or key or legend
		if (drawLegend && axis[Y_AXIS] == -1 && plots.size() > 1) {
			String[] names = new String[plots.size()];
			PointStyle[] pointStyles = new PointStyle[plots.size()];
			Color[] colors = new Color[plots.size()];
			Iterator<Plot> p = plots.iterator();
			int counter = 0;
			while (p.hasNext()) {
				Plot plot = p.next();
				names[counter] = plot.getName();
				colors[counter] = plot.getLineStyle().getColor();
				pointStyles[counter] = plot.getPointStyle();
				counter++;
			}
			drawGenericNominalLegend(g, names, pointStyles, colors, 0, 255);
		} else {
			int xOffset = 0;
			if (drawLabel) {
				StringBuffer yAxisLabel = new StringBuffer();
				if (axis[Y_AXIS] >= 0) {
					yAxisLabel.append(dataTable.getColumnName(axis[Y_AXIS]));
				} else {
					boolean first = true;
					for (int column = 0; column < columns.length; column++) {
						if (columns[column]) {
							if (!first) {
								yAxisLabel.append(", ");
							}
							yAxisLabel.append(dataTable.getColumnName(column));
							first = false;
						}
					}
				}
				if (yAxisLabel.length() == 0) {
					yAxisLabel.append("unknown");
				}
				Rectangle2D stringBounds = LABEL_FONT.getStringBounds(yAxisLabel.toString(), g.getFontRenderContext());
				xOffset += stringBounds.getWidth() + 20;
				g.drawString(yAxisLabel.toString(), MARGIN - 6, MARGIN - 6);
			}
			if (drawLegend && axis[Y_AXIS] != -1 && plots.size() == 1) {
				drawLegend(g, dataTable, colorColumn, xOffset, 255);
			}
		}

		// draw key
		if (key != null) {
			Rectangle2D stringBounds = SCALED_LABEL_FONT.getStringBounds(key, g.getFontRenderContext());
			int keyX = MARGIN + (int) (pixWidth / 2.0d - stringBounds.getWidth() / 2.0d);
			int keyY = (int) (MARGIN / 2 - stringBounds.getHeight() / 2) + 25;
			Rectangle2D bg = new Rectangle2D.Double(keyX - 2, keyY - 2 - Math.abs(stringBounds.getHeight()),
					stringBounds.getWidth() + 11, Math.abs(stringBounds.getHeight()) + 9);
			g.setColor(TOOLTIP_COLOR);
			g.fill(bg);
			g.setColor(Color.black);
			g.draw(bg);
			g.drawString(key, keyX, keyY);
		}

		// drag rectangle
		drawDragRectangle(g);
	}

	public void setDraw2DLines(boolean v) {
		this.draw2DLines = v;
	}

	public boolean getDraw2DLines() {
		return this.draw2DLines;
	}

	@Override
	public boolean isProvidingCoordinates() {
		return true;
	}

	/**
	 * Returns true if a log scale for this column is supported. Returns true for the x- and y-axis.
	 */
	@Override
	public boolean isSupportingLogScale(int axis) {
		if (axis == X_AXIS || axis == Y_AXIS) {
			return true;
		} else {
			return super.isSupportingLogScale(axis);
		}
	}

	/** Sets if the given axis should be plotted with log scale. */
	@Override
	public void setLogScale(int axis, boolean logScale) {
		if (axis == X_AXIS) {
			if (logScale) {
				xTransformation = new AxisTransformationLog();
			} else {
				xTransformation = new AxisTransformationId();
			}
		} else if (axis == Y_AXIS) {
			if (logScale) {
				yTransformation = new AxisTransformationLog();
			} else {
				yTransformation = new AxisTransformationId();
			}
		}
		repaint();
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.LINES_PLOT;
	}
}
