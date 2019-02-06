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
import com.rapidminer.gui.plotter.conditions.ColumnsPlotterCondition;
import com.rapidminer.gui.plotter.conditions.PlotterCondition;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.math.MathFunctions;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/**
 * A Radial coordinate Visualization, hence the name RadViz. The spring paradigm for displaying
 * high-dimensional data has been quite successful. M lines radially eminate from the center of the
 * circle and terminate at the perimeter in special special endpoints called dimensional anchors
 * (DA). One end of a spring is attached to each DA. The other end of each spring is attached to a
 * data point. The spring constant K_j has the value of the j-th coordinate of the data point. The
 * data point values are typically locally normalized. Each data point is then displayed at the
 * position that produces a spring force sum of 0. If all m coordinates have the same value the data
 * point lies exactly in the center of the circle independently of the actual values. If the point
 * is a unit vector point it lies exaclty at the fixed point on the edge of the circle, where the
 * spring for that dimension is fixed. Many points can map to the same position. This mapping
 * represents a non-linear transformation of the data that preserves certain symmetries.
 * 
 * @author Daniel Hakenjos, Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class RadVizPlotter extends PlotterAdapter {

	private static final long serialVersionUID = 199188198448229742L;

	private static final int MAX_NUMBER_OF_COLUMNS = 1000;

	/** Indicates the initial zoom factor. */
	private static final int ZOOM_FACTOR = 50;

	/** Indicates which type of column mapping should be used. */
	private static final String[] COLUMN_MAPPING_TYPES = { "ordered", "weights", "random" };

	/** Indicates a ordered column mapping. */
	private static final int ORDERED = 0;

	/** Indicates a ordered column mapping. */
	private static final int WEIGHTS = 1;

	/** Indicates a ordered column mapping. */
	private static final int RANDOM = 2;

	/** The list of all plotter points. */
	protected List<PlotterPoint> plotterPoints = new LinkedList<PlotterPoint>();

	/** The currently used data table. */
	protected transient DataTable dataTable;

	/** Maps the axes to the data table columns. */
	protected int[] columnMapping;

	/** The maximum column weight (if weights are available in data table). */
	protected double maxWeight = Double.NaN;

	/** The vector directions of the axes of the rad viz. */
	protected double[] anchorVectorX, anchorVectorY;

	/** The angles between the axes. */
	private double[] angles;

	/** The column which should be used to colorize the data points. */
	protected int colorColumn = -1;

	/** The minimum value of the color column. */
	private double minColor;

	/** The maximum value of the color column. */
	private double maxColor;

	/** Selection of column mapping. */
	private JComboBox<String> columnMappingSelection;

	/** The list of columns which should not be used as dimension anchors. */
	protected JList<String> ignoreList;

	/** The currently selected type of column mapping. Default is ORDERED. */
	private int columnMappingType = ORDERED;

	/** The scaling factor for point plotting, usually 1. */
	protected double scale = 1;

	/** Currently used random seed for random ordering. */
	private long orderRandomSeed = 2001;

	/** The random number generator for random seeds. */
	private Random randomSeedRandom = new Random();

	/** Creates a new RadViz plotter. */
	public RadVizPlotter(PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.white);
		this.columnMappingSelection = new JComboBox<>(COLUMN_MAPPING_TYPES);
		columnMappingSelection.setToolTipText("Indicates the type of column mapping (reordering).");
		this.columnMappingSelection.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setColumnMapping(columnMappingSelection.getSelectedIndex());
			}
		});

		this.ignoreList = new JList<>(new DefaultListModel<>());
		this.ignoreList.setToolTipText("The selected columns will not be used as dimension anchors.");
		ignoreList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				repaint();
			}
		});
	}

	/** Creates a new RadViz plotter from the given data table. */
	public RadVizPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
		this(settings);
		setDataTable(dataTable);
	}

	@Override
	public PlotterCondition getPlotterCondition() {
		return new ColumnsPlotterCondition(MAX_NUMBER_OF_COLUMNS);
	}

	@Override
	public void setDataTable(DataTable dataTable) {
		super.setDataTable(dataTable);
		this.dataTable = dataTable;

		// ignore list
		DefaultListModel<String> ignoreModel = (DefaultListModel<String>) ignoreList.getModel();
		ignoreModel.clear();
		for (int i = 0; i < this.dataTable.getNumberOfColumns(); i++) {
			if (i == colorColumn) {
				continue;
			}
			ignoreModel.addElement(this.dataTable.getColumnName(i));
		}

		this.maxWeight = getMaxWeight(dataTable);

		repaint();
	}

	@Override
	public void setPlotColumn(int index, boolean plot) {
		if (plot) {
			this.colorColumn = index;
		} else {
			this.colorColumn = -1;
		}
		// ignore list
		DefaultListModel<String> ignoreModel = (DefaultListModel<String>) ignoreList.getModel();
		ignoreModel.clear();
		for (int i = 0; i < this.dataTable.getNumberOfColumns(); i++) {
			if (i == this.colorColumn) {
				continue;
			}
			ignoreModel.addElement(this.dataTable.getColumnName(i));
		}
		repaint();
	}

	@Override
	public boolean getPlotColumn(int index) {
		return colorColumn == index;
	}

	@Override
	public String getPlotName() {
		return "Color";
	}

	@Override
	public JComponent getOptionsComponent(int index) {
		if (index == 0) {
			JLabel label = new JLabel("Column mapping:");
			label.setToolTipText("Indicates the type of column mapping (reordering).");
			return label;
		} else if (index == 1) {
			return columnMappingSelection;
		} else if (index == 2) {
			JLabel label = new JLabel("Ignore columns:");
			label.setToolTipText("The selected columns will not be used as dimension anchors.");
			return label;
		} else if (index == 3) {
			return new ExtendedJScrollPane(ignoreList);
		} else {
			return null;
		}
	}

	@Override
	public boolean canHandleZooming() {
		return true;
	}

	@Override
	public void setZooming(int zooming) {
		this.scale = zooming / (double) ZOOM_FACTOR;
		repaint();
	}

	@Override
	public int getInitialZoomFactor() {
		return ZOOM_FACTOR;
	}

	// ===============================================================

	private void setColumnMapping(int mapping) {
		this.columnMappingType = mapping;
		if (mapping == RANDOM) {
			this.orderRandomSeed = randomSeedRandom.nextLong();
		}
		repaint();
	}

	protected boolean shouldIgnoreColumn(int column) {
		return shouldIgnoreColumn(this.dataTable.getColumnName(this.columnMapping[column]));
	}

	protected boolean shouldIgnoreColumn(String column) {
		for (Object ignored : ignoreList.getSelectedValuesList()) {
			if (ignored.equals(column)) {
				return true;
			}
		}
		return false;
	}

	/** Creates the column mapping. */
	private void calculateColumnMapping() {
		this.columnMapping = new int[this.dataTable.getNumberOfColumns()];
		for (int i = 0; i < this.columnMapping.length; i++) {
			this.columnMapping[i] = i;
		}
		switch (columnMappingType) {
			case ORDERED:
				// does nothing
				break;
			case WEIGHTS:
				if (this.dataTable.isSupportingColumnWeights()) {
					this.columnMapping = new int[this.dataTable.getNumberOfColumns()];
					List<WeightIndex> indices = new LinkedList<WeightIndex>();
					for (int i = 0; i < this.dataTable.getNumberOfColumns(); i++) {
						if ((colorColumn != i) && (!shouldIgnoreColumn(i))) {
							indices.add(new WeightIndex(i, Math.abs(this.dataTable.getColumnWeight(i))));
						} else {
							indices.add(new WeightIndex(i, 0.0d));
						}
					}
					Collections.sort(indices);
					Iterator<WeightIndex> w = indices.iterator();
					int counter = 0;
					while (w.hasNext()) {
						this.columnMapping[counter++] = w.next().getIndex();
					}
				} else {
					// LogService.getGlobal().log("Cannot use weight based ordering since no column weights are given.",
					// LogService.WARNING);
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.gui.plotter.RadVizPlotter.using_weight_based_ordering_error");
				}
				break;
			case RANDOM:
				this.columnMapping = new int[this.dataTable.getNumberOfColumns()];
				List<Integer> indices = new ArrayList<Integer>();
				for (int i = 0; i < this.columnMapping.length; i++) {
					this.columnMapping[i] = i;
					if ((colorColumn != i) && (!shouldIgnoreColumn(i))) {
						indices.add(i);
					}
				}
				Random random = new Random(orderRandomSeed);
				for (int i = 0; i < this.columnMapping.length; i++) {
					if ((colorColumn != i) && (!shouldIgnoreColumn(i))) {
						int other = indices.get(random.nextInt(indices.size()));
						int dummy = this.columnMapping[i];
						this.columnMapping[i] = this.columnMapping[other];
						this.columnMapping[other] = dummy;
					}
				}
				break;
			default:
				break;
		}
	}

	/**
	 * Calculates the sample points in the RadViz.
	 */
	protected void calculateSamplePoints() {
		plotterPoints.clear();

		// color min and max
		this.minColor = Double.POSITIVE_INFINITY;
		this.maxColor = Double.NEGATIVE_INFINITY;
		if (colorColumn >= 0) {
			Iterator<DataTableRow> sample = this.dataTable.iterator();
			while (sample.hasNext()) {
				DataTableRow row = sample.next();
				double color = row.getValue(colorColumn);
				this.minColor = MathFunctions.robustMin(minColor, color);
				this.maxColor = MathFunctions.robustMax(maxColor, color);
			}
		}

		Iterator<DataTableRow> sample = this.dataTable.iterator();
		ColorProvider colorProvider = getColorProvider();
		while (sample.hasNext()) {
			DataTableRow row = sample.next();
			double xPos = 0.0d;
			double yPos = 0.0d;

			// calculate sum and fetch color
			double sum = 0.0d;
			for (int d = 0; d < this.dataTable.getNumberOfColumns(); d++) {
				if ((d != colorColumn) && (!shouldIgnoreColumn(d))) {
					sum += row.getValue(columnMapping[d]);
				}
			}

			// calculate w
			double[] w = new double[this.dataTable.getNumberOfColumns()];
			for (int d = 0; d < this.dataTable.getNumberOfColumns(); d++) {
				if ((d == colorColumn) || (shouldIgnoreColumn(d))) {
					continue;
				}
				w[d] = row.getValue(columnMapping[d]) / sum;
			}

			// calculate u, i.e. the x and y pos in rad viz space
			for (int d = 0; d < this.dataTable.getNumberOfColumns(); d++) {
				if ((d == colorColumn) || (shouldIgnoreColumn(d))) {
					continue;
				}
				xPos += w[d] * anchorVectorX[d];
				yPos += w[d] * anchorVectorY[d];
			}
			double color = 1.0d;
			Color borderColor = Color.BLACK;
			if (colorColumn >= 0) {
				color = colorProvider.getPointColorValue(this.dataTable, row, colorColumn, this.minColor, this.maxColor);
				borderColor = colorProvider.getPointBorderColor(this.dataTable, row, colorColumn);
			}
			plotterPoints.add(new PlotterPoint(xPos, yPos, color, borderColor));
		}
	}

	/**
	 * Calculate the attribute vectors.
	 * 
	 */
	protected void calculateAttributeVectors() {
		anchorVectorX = new double[this.dataTable.getNumberOfColumns()];
		anchorVectorY = new double[this.dataTable.getNumberOfColumns()];

		for (int i = 0; i < this.dataTable.getNumberOfColumns(); i++) {
			if ((i == colorColumn) || (shouldIgnoreColumn(i))) {
				continue;
			}
			double angle = angles[i];

			double x = 0.0f, y = 0.0f;
			if ((int) angle / 90 == 0) {
				x = sin(angle);
				y = sin(90.0f - angle);
			} else if ((int) angle / 90 == 1) {
				angle = angle - 90.0f;
				x = sin(90.0f - angle);
				y = sin(angle);
				y = -y;
			} else if ((int) angle / 90 == 2) {
				angle = angle - 180.0f;
				x = sin(angle);
				y = sin(90.0f - angle);
				x = -x;
				y = -y;
			} else if ((int) angle / 90 == 3) {
				angle = angle - 270.0f;
				x = sin(90.0f - angle);
				y = sin(angle);
				x = -x;
			}

			anchorVectorX[i] = x;
			anchorVectorY[i] = y;
		}
	}

	private void calculateAngles() {
		int numberOfColumns = this.dataTable.getNumberOfColumns();
		if (colorColumn >= 0) {
			numberOfColumns--;
		}
		int[] ignoredColumns = ignoreList.getSelectedIndices();
		numberOfColumns -= ignoredColumns.length;
		double totalAngle = 360.0d;
		double delta = totalAngle / numberOfColumns;

		double angle = 0.0d;

		angles = new double[this.dataTable.getNumberOfColumns()];
		for (int i = 0; i < angles.length; i++) {
			if ((i == colorColumn) || (shouldIgnoreColumn(i))) {
				continue;
			}
			angles[i] = angle;
			angle += delta;
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		// the calculation of the column mapping must be done before the method paintPlotter
		// since this method can be performed for all types of dimension anchored plotters
		calculateColumnMapping();
		paintPlotter(g);
	}

	protected void paintPlotter(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics.create();
		calculateAngles();
		calculateAttributeVectors();
		calculateSamplePoints();

		int width = getWidth();
		int height = getHeight();

		int midX = width / 2;
		int midY = height / 2;
		double radius = (Math.min(width, height) - 4 * MARGIN) / 2.0d;

		// draw the circle
		g.setColor(GRID_COLOR);
		g.drawOval((int) (midX - radius), (int) (midY - radius), (int) (2.0d * radius), (int) (2.0d * radius));
		for (int i = 0; i < this.dataTable.getNumberOfColumns(); i++) {
			if ((i == colorColumn) || (shouldIgnoreColumn(i))) {
				continue;
			}
			int endX = (int) (midX + anchorVectorX[i] * radius);
			int endY = (int) (midY - anchorVectorY[i] * radius);
			g.drawLine(midX, midY, endX, endY);
		}

		// draw axis-labels
		g.setFont(LABEL_FONT);
		for (int i = 0; i < this.dataTable.getNumberOfColumns(); i++) {
			if ((i == colorColumn) || (shouldIgnoreColumn(i))) {
				continue;
			}
			double x = midX + anchorVectorX[i] * radius;
			double y = midY - anchorVectorY[i] * radius;

			// calculate offsets according to angle and string bounds
			Rectangle2D stringBounds = LABEL_FONT.getStringBounds(this.dataTable.getColumnName(columnMapping[i]),
					g.getFontRenderContext());
			if ((angles[i] >= 0) && (angles[i] <= 90)) {
				x += (anchorVectorX[i] * 5);
				y -= (anchorVectorY[i] * 5);
			} else if ((angles[i] >= 90) && (angles[i] < 180)) {
				x += (anchorVectorX[i] * 10);
				y -= (anchorVectorY[i] * 10);
			} else if ((angles[i] >= 180) && (angles[i] < 270)) {
				x += (anchorVectorX[i] * 15) - stringBounds.getWidth();
				y -= (anchorVectorY[i] * 15);
			} else if ((angles[i] >= 270) && (angles[i] < 360)) {
				x += (anchorVectorX[i] * 10) - stringBounds.getWidth();
				y -= (anchorVectorY[i] * 10);
			}
			if (this.dataTable.isSupportingColumnWeights()) {
				Rectangle2D weightRect = new Rectangle2D.Double(x - 2, y - stringBounds.getHeight(),
						stringBounds.getWidth() + 2, stringBounds.getHeight() + 3);
				g.setColor(getWeightColor(this.dataTable.getColumnWeight(columnMapping[i]), maxWeight));
				g.fill(weightRect);
			}
			g.setColor(GRID_COLOR);
			g.drawString(this.dataTable.getColumnName(columnMapping[i]), (int) x, (int) y);
		}

		// draw the points
		Iterator<PlotterPoint> i = plotterPoints.iterator();
		ColorProvider colorProvider = getColorProvider();
		while (i.hasNext()) {
			drawPoint(g, i.next(), colorProvider, midX, midY, radius);
		}

		// legend
		if ((colorColumn != -1) && (plotterPoints.size() > 0)) {
			drawLegend(g, dataTable, colorColumn);
		}
	}

	/**
	 * Draw a data point.
	 */
	protected void drawPoint(Graphics2D g, PlotterPoint point, ColorProvider colorProvider, int midX, int midY, double radius) {
		int x = midX;
		int y = midY;

		x += (int) (point.getX() * radius * scale);
		y -= (int) (point.getY() * radius * scale);

		Color pointColor = Color.red;
		if (colorColumn != -1) {
			pointColor = colorProvider.getPointColor(point.getColor());
		}

		drawPoint(g, x, y, pointColor, point.getBorderColor());
	}

	/**
	 * Gets the sinus of the angle.
	 * 
	 * @param angle
	 */
	private double sin(double angle) {
		while (angle >= 180.0d) {
			angle -= 180.0d;
		}
		double value = (angle / 180.0d * Math.PI);
		return Math.sin(value);
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.RADVIZ_PLOT;
	}
}
