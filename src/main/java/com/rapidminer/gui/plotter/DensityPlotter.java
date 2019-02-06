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

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.tools.math.MathFunctions;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;


/**
 * The density plotter does not only plot the single plot points but also tries to calculate a color
 * for all pixels in between.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class DensityPlotter extends PlotterAdapter {

	/**
	 * Helper class containing the position of a point in matrix (plotter) space, its density color,
	 * and its point color.
	 */
	private static class Point {

		String id;
		double x;
		double y;
		double densityColor;
		double pointColor;
		Color borderColor;

		public Point(double x, double y, double densityColor, double pointColor, Color borderColor, String id) {
			this.x = x;
			this.y = y;
			this.densityColor = densityColor;
			this.pointColor = pointColor;
			this.borderColor = borderColor;
			this.id = id;
		}
	}

	private static final long serialVersionUID = -3723769116082161327L;

	/** Indices of axis components. */
	private static final int X_AXIS = 0;

	private static final int Y_AXIS = 1;

	private static final int POINT_COLOR = 2;

	/** Matrix size */
	private static final int MATRIX_WIDTH = 300;

	private static final int MATRIX_HEIGHT = 200;

	private transient DataTable dataTable;

	private List<Point> points = new LinkedList<Point>();

	private int[] axes = new int[] { -1, -1 };

	private int pointColorIndex = -1;

	private int densityColorIndex = -1;

	private double minDensityColor;
	private double maxDensityColor;

	private double minPointColor;
	private double maxPointColor;

	private double[] min;
	private double[] max;

	private double[][] colorMatrix;

	private String currentToolTip = null;

	private double toolTipX = 0.0d;

	private double toolTipY = 0.0d;

	private transient BufferedImage image = null;

	public DensityPlotter(PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.white);
	}

	public DensityPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
		this(settings);
		setDataTable(dataTable);
	}

	@Override
	public void setDataTable(DataTable dataTable) {
		super.setDataTable(dataTable);
		this.dataTable = dataTable;
		prepareData();
		repaint();
	}

	@Override
	public int getNumberOfAxes() {
		return 3;
	}

	@Override
	public int getAxis(int axis) {
		switch (axis) {
			case X_AXIS:
				return axes[X_AXIS];
			case Y_AXIS:
				return axes[Y_AXIS];
			case POINT_COLOR:
				return pointColorIndex;
			default:
				return -1;
		}
	}

	@Override
	public String getAxisName(int index) {
		switch (index) {
			case X_AXIS:
				return "x-Axis";
			case Y_AXIS:
				return "y-Axis";
			case POINT_COLOR:
				return "Point Color";
			default:
				return "none";
		}
	}

	@Override
	public void setAxis(int index, int dimension) {
		if ((index == 0) || (index == 1)) {
			this.axes[index] = dimension;
		} else if (index == 2) {
			this.pointColorIndex = dimension;
		}
		prepareData();
		repaint();
	}

	@Override
	public String getPlotName() {
		return "Density Color";
	}

	@Override
	public void setPlotColumn(int index, boolean plot) {
		densityColorIndex = index;
		prepareData();
		repaint();
	}

	@Override
	public boolean getPlotColumn(int index) {
		return index == densityColorIndex;
	}

	private void prepareData() {
		points.clear();
		this.minPointColor = Double.POSITIVE_INFINITY;
		this.maxPointColor = Double.NEGATIVE_INFINITY;
		this.minDensityColor = Double.POSITIVE_INFINITY;
		this.maxDensityColor = Double.NEGATIVE_INFINITY;
		this.min = new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY };
		this.max = new double[] { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };

		this.colorMatrix = new double[MATRIX_WIDTH][MATRIX_HEIGHT];
		for (int x = 0; x < MATRIX_WIDTH; x++) {
			for (int y = 0; y < MATRIX_HEIGHT; y++) {
				this.colorMatrix[x][y] = 0.5d;
			}
		}

		if ((axes[X_AXIS] >= 0) && (axes[Y_AXIS] >= 0) && (densityColorIndex >= 0)) {
			Iterator<DataTableRow> i = this.dataTable.iterator();
			if (pointColorIndex >= 0) {
				while (i.hasNext()) {
					DataTableRow row = i.next();
					double pointColor = row.getValue(pointColorIndex);
					if (!Double.isNaN(pointColor)) {
						this.minPointColor = MathFunctions.robustMin(this.minPointColor, pointColor);
						this.maxPointColor = MathFunctions.robustMax(this.maxPointColor, pointColor);
					}
				}
			}

			i = this.dataTable.iterator();
			ColorProvider colorProvider = getColorProvider();
			while (i.hasNext()) {
				DataTableRow row = i.next();
				double densityColor = row.getValue(densityColorIndex);
				double xValue = row.getValue(axes[X_AXIS]);
				double yValue = row.getValue(axes[Y_AXIS]);
				if (!Double.isNaN(xValue) && !Double.isNaN(yValue)) {
					this.minDensityColor = MathFunctions.robustMin(this.minDensityColor, densityColor);
					this.maxDensityColor = MathFunctions.robustMax(this.maxDensityColor, densityColor);
					this.min[X_AXIS] = MathFunctions.robustMin(this.min[X_AXIS], xValue);
					this.max[X_AXIS] = MathFunctions.robustMax(this.max[X_AXIS], xValue);
					this.min[Y_AXIS] = MathFunctions.robustMin(this.min[Y_AXIS], yValue);
					this.max[Y_AXIS] = MathFunctions.robustMax(this.max[Y_AXIS], yValue);
					double pointColor = 0.0d;
					Color borderColor = Color.BLACK;
					if (pointColorIndex >= 0) {
						pointColor = colorProvider.getPointColorValue(this.dataTable, row, pointColorIndex,
								this.minPointColor, this.maxPointColor);
						borderColor = colorProvider.getPointBorderColor(this.dataTable, row, pointColorIndex);
					}
					points.add(new Point(xValue, yValue, densityColor, pointColor, borderColor, row.getId()));
				}
			}

			Collections.shuffle(points, new Random(2001));
			int radius = (int) Math.max(1, 2 * Math.max(MATRIX_WIDTH, MATRIX_HEIGHT) / Math.sqrt(points.size()));
			Iterator<Point> p = points.iterator();
			while (p.hasNext()) {
				Point point = p.next();
				double color = (point.densityColor - minDensityColor) / (maxDensityColor - minDensityColor);
				int matrixX = (int) (((point.x - min[X_AXIS]) / (max[X_AXIS] - min[X_AXIS])) * MATRIX_WIDTH);
				int matrixY = (int) (((point.y - min[Y_AXIS]) / (max[Y_AXIS] - min[Y_AXIS])) * MATRIX_HEIGHT);
				changeColor(colorMatrix, matrixX, matrixY, color, radius);
			}
			this.image = new BufferedImage(MATRIX_WIDTH, MATRIX_HEIGHT, BufferedImage.TYPE_INT_RGB);
			for (int x = 0; x < MATRIX_WIDTH; x++) {
				for (int y = 0; y < MATRIX_HEIGHT; y++) {
					image.setRGB(x, MATRIX_HEIGHT - y - 1, colorProvider.getPointColor(colorMatrix[x][y]).getRGB());
				}
			}
		}
	}

	private void changeColor(double[][] colorMatrix, int matrixX, int matrixY, double color, int radius) {
		double maxDistance = radius;
		for (int x = matrixX - radius; x < matrixX + radius; x++) {
			for (int y = matrixY - radius; y < matrixY + radius; y++) {
				if ((x < 0) || (x >= MATRIX_WIDTH) || (y < 0) || (y >= MATRIX_HEIGHT)) {
					continue;
				}
				int xDiff = x - matrixX;
				int yDiff = y - matrixY;
				double distanceFactor = MathFunctions.robustMax(0,
						((maxDistance - Math.sqrt(xDiff * xDiff + yDiff * yDiff)) / maxDistance));
				double colorDiff = color - colorMatrix[x][y];
				colorMatrix[x][y] = MathFunctions.robustMax(0.0d,
						MathFunctions.robustMin(1.0d, colorMatrix[x][y] + distanceFactor * colorDiff));
			}
		}
	}

	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		if (points.size() > 0) {
			if (pointColorIndex >= 0) {
				drawLegend(graphics, this.dataTable, pointColorIndex);
			} else if (densityColorIndex >= 0) {
				drawLegend(graphics, this.dataTable, densityColorIndex);
			}
			int pixWidth = getWidth() - 2 * MARGIN;
			int pixHeight = getHeight() - 2 * MARGIN;
			Graphics2D translated = (Graphics2D) graphics.create();
			translated.translate(MARGIN, MARGIN);
			paintDensity(translated, pixWidth, pixHeight);
			if (pointColorIndex >= 0) {
				paintPoints(translated, pixWidth, pixHeight);
			}
			drawToolTip(translated);
			translated.dispose();
		}
	}

	private void paintDensity(Graphics2D g, int pixWidth, int pixHeight) {
		g.drawImage(image, 0, 0, pixWidth, pixHeight, Color.WHITE, null);
	}

	private void paintPoints(Graphics2D g, int pixWidth, int pixHeight) {
		Iterator<Point> p = points.iterator();
		ColorProvider colorProvider = getColorProvider(true);
		while (p.hasNext()) {
			Point point = p.next();
			double xPos = (point.x - min[X_AXIS]) / (max[X_AXIS] - min[X_AXIS]) * pixWidth;
			double yPos = (point.y - min[Y_AXIS]) / (max[Y_AXIS] - min[Y_AXIS]) * pixHeight;
			drawPoint(g, xPos, pixHeight - yPos, colorProvider.getPointColor(point.pointColor), point.borderColor);
		}
	}

	@Override
	public boolean isProvidingCoordinates() {
		return true;
	}

	/**
	 * Returns the position in data space from a position in screen space should return the proper
	 * position.
	 */
	@Override
	public Point2D getPositionInDataSpace(java.awt.Point p) {
		double pixWidth = getWidth() - 2 * MARGIN;
		double pixHeight = getHeight() - 2 * MARGIN;
		double dataX = (((p.getX() - MARGIN) * (max[X_AXIS] - min[X_AXIS])) / pixWidth) + min[X_AXIS];
		double dataY = (((pixHeight - p.getY() - MARGIN) * (max[Y_AXIS] - min[Y_AXIS])) / pixHeight) + min[Y_AXIS];
		return new Point2D.Double(dataX, dataY);
	}

	@Override
	public String getIdForPos(int x, int y) {
		Point point = getPlotterPointForPos(x, y);
		if (point != null) {
			return point.id;
		}
		return null;
	}

	private Point getPlotterPointForPos(int _x, int _y) {
		int x = _x - MARGIN;
		int y = _y - MARGIN;
		double pixWidth = getWidth() - 2 * MARGIN;
		double pixHeight = getHeight() - 2 * MARGIN;
		Iterator<Point> i = points.iterator();
		while (i.hasNext()) {
			Point point = i.next();
			double xPos = (point.x - min[X_AXIS]) / (max[X_AXIS] - min[X_AXIS]) * pixWidth;
			double yPos = pixHeight - (point.y - min[Y_AXIS]) / (max[Y_AXIS] - min[Y_AXIS]) * pixHeight;
			if ((Math.abs(xPos - x) < 3) && (Math.abs(yPos - y) < 3)) {
				return point;
			}
		}
		return null;
	}

	/** Sets the mouse position in the shown data space. */
	@Override
	public void setMousePosInDataSpace(int x, int y) {
		if (pointColorIndex >= 0) {
			Point point = getPlotterPointForPos(x, y);
			if (point != null) {
				String id = point.id;
				if (id != null) {
					double pixWidth = (getWidth() - 2 * MARGIN);
					double pixHeight = (getHeight() - 2 * MARGIN);
					double xPos = (point.x - min[X_AXIS]) / (max[X_AXIS] - min[X_AXIS]) * pixWidth;
					double yPos = pixHeight - ((point.y - min[Y_AXIS]) / (max[Y_AXIS] - min[Y_AXIS]) * pixHeight);
					setToolTip(id, xPos, yPos);
				} else {
					setToolTip(null, 0.0d, 0.0d);
				}
			} else {
				setToolTip(null, 0.0d, 0.0d);
			}
		} else {
			setToolTip(null, 0.0d, 0.0d);
		}
	}

	private void setToolTip(String toolTip, double x, double y) {
		this.currentToolTip = toolTip;
		this.toolTipX = x;
		this.toolTipY = y;
		repaint();
	}

	private void drawToolTip(Graphics2D g) {
		if (currentToolTip != null) {
			g.setFont(LABEL_FONT);
			Rectangle2D stringBounds = LABEL_FONT.getStringBounds(currentToolTip, g.getFontRenderContext());
			g.setColor(TOOLTIP_COLOR);
			Rectangle2D bg = new Rectangle2D.Double((toolTipX) - stringBounds.getWidth() - 15,
					(toolTipY - (stringBounds.getHeight() / 2)), stringBounds.getWidth() + 6, Math.abs(stringBounds
							.getHeight()) + 4);
			g.fill(bg);
			g.setColor(Color.black);
			g.draw(bg);
			g.drawString(currentToolTip, (float) ((toolTipX) - stringBounds.getWidth() - 12),
					(float) ((toolTipY + stringBounds.getHeight() * 0.5) + 1));
		}
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.DENSITY_PLOT;
	}
}
