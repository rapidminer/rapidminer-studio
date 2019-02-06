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
import com.rapidminer.tools.math.MathFunctions;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * This idea of coding and representing multivariate data by curves was suggested by Andrews (1972).
 * Each multivariate observation X_i = (X_i1, .. ,X_ip) is transformed into a curve of fourier
 * coefficients.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class AndrewsCurves extends PlotterAdapter {

	private static final long serialVersionUID = -471636460650394557L;

	static final int MAX_NUMBER_OF_COLUMNS = 1000;

	private static final int NUMBER_OF_SUPPORT_POINTS = 100;

	/** Helper class containing the support points for each line and its color. */
	private static class LinePlot {

		private double[] lineData;
		private double color;

		private LinePlot(double[] lineData, double color) {
			this.lineData = lineData;
			this.color = color;
		}
	}

	private transient DataTable dataTable;

	private List<LinePlot> lines = new LinkedList<>();

	private double minY;

	private double maxY;

	private int colorColumn = -1;

	private double minColor = 0.0d;

	private double maxColor = 1.0d;

	/**
	 * Init the AndrewsCurvesPlot.
	 */
	public AndrewsCurves(PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.white);
	}

	/**
	 * Init the AndrewsCurvesPlot.
	 */
	public AndrewsCurves(PlotterConfigurationModel settings, DataTable dataTable) {
		this(settings);
		setDataTable(dataTable);
	}

	@Override
	public void setDataTable(DataTable dataTable) {
		super.setDataTable(dataTable);
		this.dataTable = dataTable;
		repaint();
	}

	@Override
	public PlotterCondition getPlotterCondition() {
		return new ColumnsPlotterCondition(MAX_NUMBER_OF_COLUMNS);
	}

	@Override
	public void setPlotColumn(int index, boolean plot) {
		if (plot) {
			this.colorColumn = index;
		} else {
			this.colorColumn = -1;
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

	private void prepareData() {
		lines.clear();
		int numberOfColumns = this.dataTable.getNumberOfColumns() - this.dataTable.getNumberOfSpecialColumns();
		if (colorColumn >= 0) {
			numberOfColumns--;
			if (this.dataTable.isSpecial(colorColumn)) {
				numberOfColumns++;  // add one since it was subtracted twice
			}
		}

		// otherwise we divide by 0 and (int) Math.ceil(NaN) returns actually Integer.MAX_VALUE
		// which is then later added to 1 which results
		// in Integer.MIN_VALUE which then results in an NegativeArraySizeException...
		if (numberOfColumns == 0) {
			return;
		}

		int supportPoints = 1;
		if (numberOfColumns < NUMBER_OF_SUPPORT_POINTS) {
			supportPoints = (int) Math.ceil((double) NUMBER_OF_SUPPORT_POINTS / (double) numberOfColumns);
		}
		int totalNumberOfPoints = (numberOfColumns - 1) * supportPoints + 1;

		// fetch data
		synchronized (dataTable) {
			this.minY = Double.POSITIVE_INFINITY;
			this.maxY = Double.NEGATIVE_INFINITY;
			this.minColor = Double.POSITIVE_INFINITY;
			this.maxColor = Double.NEGATIVE_INFINITY;
			Iterator<DataTableRow> s = this.dataTable.iterator();
			if (colorColumn != -1) {
				while (s.hasNext()) {
					DataTableRow row = s.next();
					double color = row.getValue(colorColumn);
					this.minColor = MathFunctions.robustMin(this.minColor, color);
					this.maxColor = MathFunctions.robustMax(this.maxColor, color);
				}
			}
			s = this.dataTable.iterator();
			while (s.hasNext()) {
				DataTableRow row = s.next();
				double color = 1.0d;
				if (colorColumn != -1) {
					color = getColorProvider().getPointColorValue(this.dataTable, row, colorColumn, this.minColor,
							this.maxColor);
				}
				double[] linePlotData = getFourierTransform(row, totalNumberOfPoints);
				for (int d = 0; d < linePlotData.length; d++) {
					this.minY = Math.min(this.minY, linePlotData[d]);
					this.maxY = Math.max(this.maxY, linePlotData[d]);
				}
				lines.add(new LinePlot(linePlotData, color));
			}
		}
	}

	/** Gets sort of a the Fourier transfrom of an example. */
	private double[] getFourierTransform(DataTableRow row, int totalNumberOfPoints) {
		double[] result = new double[totalNumberOfPoints];
		double time = -Math.PI;
		double timeDelta = 2.0d * Math.PI / result.length;
		for (int t = 0; t < result.length; t++) {
			int counter = 1;
			int columnCounter = 0;
			for (int i = 0; i < row.getNumberOfValues(); i++) {
				if ((i == colorColumn) || (dataTable.isSpecial(i))) {
					continue;
				}
				if (columnCounter == 0) {
					result[t] = row.getValue(i) / Math.sqrt(2);
				} else if ((columnCounter + 1) % 2 == 0) {
					result[t] += row.getValue(i) * Math.sin(time * counter);
				} else {
					result[t] += row.getValue(i) * Math.cos(time * counter);
					counter++;
				}
				columnCounter++;
			}
			time += timeDelta;
		}
		return result;
	}

	/**
	 * @see java.awt.Component#paint(java.awt.Graphics)
	 */
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		paintAndrewsPlot(g);
	}

	public void paintAndrewsPlot(Graphics g) {
		int pixWidth = getWidth() - 2 * MARGIN;
		int pixHeight = getHeight() - 2 * MARGIN;

		// translate to ignore margins
		Graphics2D translated = (Graphics2D) g.create();
		translated.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		translated.translate(MARGIN, MARGIN);

		// prepare data ...
		prepareData();

		// legend
		if ((colorColumn != -1) && !Double.isInfinite(minColor) && !Double.isInfinite(maxColor)
				&& (dataTable.isNominal(colorColumn) || (minColor != maxColor)) && (lines.size() > 0)) {
			drawLegend(g, dataTable, colorColumn);
		}

		// draw grid, lines, etc.
		if (lines.size() == 0) {
			translated.drawString("No plots selected.", 0, 0);
		} else {
			g.setColor(Color.black);
			draw(translated, pixWidth, pixHeight);
		}
		translated.dispose();
	}

	private void draw(Graphics g, int pixWidth, int pixHeight) {
		Rectangle2D frame = new Rectangle2D.Double(0, 0, pixWidth, pixHeight);
		drawLines(g, pixWidth, pixHeight);
		((Graphics2D) g).draw(frame);
	}

	private void drawLines(Graphics g, int width, int height) {
		Iterator<LinePlot> l = lines.iterator();
		while (l.hasNext()) {
			drawLine(g, l.next(), width, height);
		}
	}

	private void drawLine(Graphics g, LinePlot line, int width, int height) {
		double[] data = line.lineData;
		float columnDistance = (float) width / (float) (data.length - 1);
		GeneralPath path = new GeneralPath();
		float xPos = 0.0f;
		path.moveTo(xPos, normY(data[0]) * height);
		for (int d = 1; d < data.length; d++) {
			xPos += columnDistance;
			path.lineTo(xPos, normY(data[d]) * height);
		}
		Color color = Color.RED;
		if (colorColumn != -1) {
			color = getColorProvider().getPointColor(line.color);
		}
		g.setColor(color);
		((Graphics2D) g).draw(path);
	}

	private float normY(double y) {
		return (float) ((y - minY) / (maxY - minY));
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.ANDREWS_CURVES;
	}
}
