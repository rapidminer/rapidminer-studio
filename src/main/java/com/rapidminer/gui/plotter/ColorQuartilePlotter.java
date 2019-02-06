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
import com.rapidminer.gui.MainFrame;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.math.MathFunctions;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;


/**
 * This plotter can be used to create colorized quartile plots for one of the columns.
 *
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class ColorQuartilePlotter extends PlotterAdapter {

	private static final long serialVersionUID = -5115095967846809152L;

	private static final int LABEL_MARGIN_X = 50;

	private static final int NUMBER_OF_TICS = 6;

	private int columnIndex = -1;

	private int colorIndex = -1;

	protected transient DataTable dataTable;

	protected List<Quartile> allQuartiles = new LinkedList<Quartile>();

	private boolean drawLegend = true;

	private String key = null;

	protected double globalMin = Double.NaN;

	protected double globalMax = Double.NaN;

	public ColorQuartilePlotter(PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.white);
	}

	public ColorQuartilePlotter(PlotterConfigurationModel settings, DataTable dataTable) {
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
	public int getNumberOfAxes() {
		return 1;
	}

	@Override
	public int getAxis(int axis) {
		return columnIndex;
	}

	@Override
	public String getAxisName(int index) {
		if (index == 0) {
			return "Dimension";
		} else {
			return "empty";
		}
	}

	@Override
	public void setAxis(int index, int dimension) {
		if (this.columnIndex != dimension) {
			this.columnIndex = dimension;
			repaint();
		}
	}

	@Override
	public String getPlotName() {
		return "Color";
	}

	@Override
	public void setPlotColumn(int index, boolean plot) {
		colorIndex = index;
		repaint();
	}

	@Override
	public boolean getPlotColumn(int index) {
		return index == colorIndex;
	}

	public void setDrawLegend(boolean drawLegend) {
		this.drawLegend = drawLegend;
	}

	@Override
	public void setKey(String key) {
		this.key = key;
	}

	protected void prepareData() {
		allQuartiles.clear();
		this.globalMin = Double.POSITIVE_INFINITY;
		this.globalMax = Double.NEGATIVE_INFINITY;

		if (columnIndex != -1) {
			if (colorIndex != -1 && dataTable.isNominal(colorIndex)) {
				// create value map
				Map<Double, List<Double>> valueMap = new TreeMap<Double, List<Double>>();
				synchronized (dataTable) {
					Iterator<DataTableRow> i = dataTable.iterator();
					while (i.hasNext()) {
						DataTableRow row = i.next();
						double columnValue = row.getValue(columnIndex);
						double colorValue = row.getValue(colorIndex);
						List<Double> values = valueMap.get(colorValue);
						if (values == null) {
							values = new LinkedList<Double>();
							values.add(columnValue);
							valueMap.put(colorValue, values);
						} else {
							values.add(columnValue);
						}
					}

					String maxClassesProperty = ParameterService
							.getParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_COLORS_CLASSLIMIT);
					int maxClasses = 10;
					try {
						if (maxClassesProperty != null) {
							maxClasses = Integer.parseInt(maxClassesProperty);
						}
					} catch (NumberFormatException e) {
						LogService.getRoot().log(Level.WARNING,
								"com.rapidminer.gui.plotter.ColorQuartilePlotter.quartile_parsing_property_error");
					}

					if (valueMap.size() <= maxClasses) {
						// collect actual data and create a histogram for each different color value
						Iterator<Map.Entry<Double, List<Double>>> it = valueMap.entrySet().iterator();
						while (it.hasNext()) {
							Map.Entry<Double, List<Double>> e = it.next();
							Double key = e.getKey();
							int colorValue = (int) key.doubleValue();
							Color color = getColorProvider(true).getPointColor(
									colorValue / (dataTable.getNumberOfValues(colorIndex) - 1.0d));
							// color = new Color(color.getRed(), color.getGreen(), color.getBlue(),
							// RectangleStyle.ALPHA);
							Quartile quartile = Quartile.calculateQuartile(e.getValue());
							this.globalMin = MathFunctions.robustMin(this.globalMin, quartile.getMin());
							this.globalMax = MathFunctions.robustMax(this.globalMax, quartile.getMax());
							quartile.setColor(color);
							allQuartiles.add(quartile);
						}
					} else {
						// too many classes --> super method in order to create usual non-colored
						// histogram
						LogService
								.getRoot()
								.log(Level.WARNING,
								"com.rapidminer.gui.plotter.ColorQuartilePlotter.quartile_creating_colorized_quartile_error",
								new Object[] { valueMap.size(), maxClassesProperty });
					}
				}
			} else {
				Quartile quartile = Quartile.calculateQuartile(this.dataTable, columnIndex);
				allQuartiles.add(quartile);
				this.globalMin = quartile.getMin();
				this.globalMax = quartile.getMax();
			}
		}
	}

	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		if (drawLegend) {
			drawLegend(graphics, dataTable, colorIndex, 50, RectangleStyle.ALPHA);
		}
		int pixWidth = getWidth() - 2 * MARGIN;
		int pixHeight = getHeight() - 2 * MARGIN;
		Graphics2D translated = (Graphics2D) graphics.create();
		translated.translate(MARGIN, MARGIN);
		paintQuartiles(translated, pixWidth, pixHeight);
		translated.dispose();
	}

	public void paintQuartiles(Graphics2D g, int pixWidth, int pixHeight) {
			prepareData();

		if (allQuartiles.size() == 0) {
			return;
		}

		if (drawLegend) {
			drawGrid(g, pixWidth, pixHeight);
		}

		if (key != null) {
			Rectangle2D stringBounds = LABEL_FONT.getStringBounds(key, g.getFontRenderContext());
			int xPos = (int) (pixWidth / 2.0d - stringBounds.getWidth() / 2.0d);
			int yPos = 16;
			g.setColor(Color.black);
			g.drawString(key, xPos, yPos);
		}

		double offsetWidth = drawLegend ? pixWidth - LABEL_MARGIN_X : pixWidth;
		double offset = offsetWidth / (allQuartiles.size() + 1);
		if (drawLegend) {
			g.translate(LABEL_MARGIN_X, 0.0d);
		}

		Iterator<Quartile> i = allQuartiles.iterator();
		while (i.hasNext()) {
			Quartile quartile = i.next();
			g.translate(offset, 0.0d);
			paintQuartile(g, quartile, pixHeight);
		}
	}

	private void paintQuartile(Graphics2D g, Quartile quartile, int pixHeight) {
		// box
		double upperQPos = getNormedPosition(quartile.getUpperQuartile(), pixHeight);
		double lowerQPos = getNormedPosition(quartile.getLowerQuartile(), pixHeight);
		double quartileHeight = getNormedLength(quartile.getUpperQuartile() - quartile.getLowerQuartile(), pixHeight);
		Rectangle2D quartileRect = new Rectangle2D.Double(0, upperQPos, Quartile.QUARTILE_WIDTH, quartileHeight);
		g.setColor(quartile.getColor());
		g.fill(quartileRect);
		g.setColor(Color.BLACK);
		g.draw(quartileRect);

		// median
		double medianPos = getNormedPosition(quartile.getMedian(), pixHeight);
		g.draw(new Line2D.Double(0.0d, medianPos, Quartile.QUARTILE_WIDTH, medianPos));

		// whiskers
		double lowerWhiskerPos = getNormedPosition(quartile.getLowerWhisker(), pixHeight);
		g.draw(new Line2D.Double(0.0d, lowerWhiskerPos, Quartile.QUARTILE_WIDTH, lowerWhiskerPos));
		double upperWhiskerPos = getNormedPosition(quartile.getUpperWhisker(), pixHeight);
		g.draw(new Line2D.Double(0.0d, upperWhiskerPos, Quartile.QUARTILE_WIDTH, upperWhiskerPos));

		double whiskersXPos = Quartile.QUARTILE_WIDTH / 2.0d;
		g.draw(new Line2D.Double(whiskersXPos, upperQPos, whiskersXPos, upperWhiskerPos));
		g.draw(new Line2D.Double(whiskersXPos, lowerQPos, whiskersXPos, lowerWhiskerPos));

		// mean and standard deviation
		double meanXPos = Quartile.QUARTILE_WIDTH / 2.0d + 5.0d;
		double mean = getNormedPosition(quartile.getMean(), pixHeight);
		Rectangle2D meanRect = new Rectangle2D.Double(meanXPos - 2.0d, mean - 2.0d, 5.0d, 5.0d);
		g.fill(meanRect);
		double standardDeviation = getNormedLength(quartile.getStandardDeviation(), pixHeight);
		g.draw(new Line2D.Double(meanXPos, mean, meanXPos, mean + standardDeviation));
		g.draw(new Line2D.Double(meanXPos, mean, meanXPos, mean - standardDeviation));

		// outliers
		double outlierXPos = Quartile.QUARTILE_WIDTH / 2.0d;
		double[] outliers = quartile.getOutliers();
		for (double outlier : outliers) {
			double outlierYPos = getNormedPosition(outlier, pixHeight);
			drawPoint(g, outlierXPos, outlierYPos, Color.WHITE, Color.BLACK);
		}
	}

	private double getNormedPosition(double value, int pixHeight) {
		return pixHeight - (value - this.globalMin) / (this.globalMax - this.globalMin) * pixHeight;
	}

	private double getNormedLength(double length, int pixHeight) {
		return length / (this.globalMax - this.globalMin) * pixHeight;
	}

	private void drawGrid(Graphics2D g, int pixWidth, int pixHeight) {
		Graphics2D coordinateSpace = (Graphics2D) g.create();
		coordinateSpace.translate(LABEL_MARGIN_X, 0);
		drawGridLines(coordinateSpace, pixWidth - LABEL_MARGIN_X, pixHeight);
		coordinateSpace.dispose();
	}

	private void drawGridLines(Graphics2D g, int pixWidth, int pixHeight) {
		DecimalFormat format = new DecimalFormat("0.00E0");
		g.setFont(LABEL_FONT);

		double numberOfYTics = NUMBER_OF_TICS;
		double yTicSize = (this.globalMax - this.globalMin) / numberOfYTics;
		double ticDifference = pixHeight / numberOfYTics;
		for (int i = 0; i <= numberOfYTics; i++) {
			drawHorizontalTic(g, i, yTicSize, ticDifference, pixWidth, pixHeight, format);
		}
	}

	private void drawHorizontalTic(Graphics2D g, int ticNumber, double yTicSize, double ticDifference, int pixWidth,
			int pixHeight, DecimalFormat format) {
		g.setColor(GRID_COLOR);
		double yValue = this.globalMax - ticNumber * yTicSize;
		double yPos = ticNumber * ticDifference;
		g.draw(new Line2D.Double(0, yPos, pixWidth, yPos));
		g.setColor(Color.black);
		String label = format.format(yValue) + " ";
		Rectangle2D stringBounds = LABEL_FONT.getStringBounds(label, g.getFontRenderContext());
		g.drawString(label, (float) -stringBounds.getWidth(),
				(float) (yPos - stringBounds.getHeight() / 2 - stringBounds.getY()));
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.QUARTILE_PLOT_COLOR;
	}
}
