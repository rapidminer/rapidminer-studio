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
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.plotter.conditions.ColumnsPlotterCondition;
import com.rapidminer.gui.plotter.conditions.PlotterCondition;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.logging.Level;

import javax.swing.Icon;


/**
 * A quartil matrix plotter which uses the {@link ColorQuartilePlotter} for each of the plots.
 *
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class ColorQuartileMatrixPlotter extends PlotterAdapter {

	private static final long serialVersionUID = -3049267947471497204L;

	static final int MAX_NUMBER_OF_COLUMNS = 100;

	private ColorQuartilePlotter[] plotters = new ColorQuartilePlotter[0];

	private int plotterSize;

	private int colorIndex = -1;

	private double maxWeight = Double.NaN;

	private transient DataTable dataTable;

	public ColorQuartileMatrixPlotter(PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.white);
		String sizeProperty = ParameterService.getParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_MATRIXPLOT_SIZE);
		this.plotterSize = 200;
		try {
			if (sizeProperty != null) {
				this.plotterSize = Integer.parseInt(sizeProperty);
			}
		} catch (NumberFormatException e) {
			// LogService.getGlobal().log("Quartile matrix: cannot parse plotter size (was '" +
			// sizeProperty + "'), using default size (200).",
			// LogService.WARNING);
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.gui.plotter.ColorQuartileMatrixPlotter.parsing_plotter_size_error", sizeProperty);
		}
	}

	public ColorQuartileMatrixPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
		this(settings);
		setDataTable(dataTable);
	}

	@Override
	public void setDataTable(DataTable dataTable) {
		super.setDataTable(dataTable);
		this.dataTable = dataTable;
		updateMatrix();
	}

	@Override
	public PlotterCondition getPlotterCondition() {
		return new ColumnsPlotterCondition(MAX_NUMBER_OF_COLUMNS);
	}

	private void updateMatrix() {
		int numberOfPlotters = dataTable.getNumberOfColumns();
		if (colorIndex != -1) {
			numberOfPlotters--;
		}
		plotters = new ColorQuartilePlotter[numberOfPlotters];
		int x = 0;
		synchronized (dataTable) {
			for (int i = 0; i < dataTable.getNumberOfColumns(); i++) {
				if (i == colorIndex) {
					continue;
				}
				plotters[x] = new ColorQuartilePlotter(getPlotterSettings(), dataTable);
				plotters[x].getPlotter().setSize(new Dimension(plotterSize, plotterSize));
				plotters[x].setAxis(0, i);
				plotters[x].setPlotColumn(colorIndex, true);
				plotters[x].setDrawLegend(false);
				plotters[x].setKey(dataTable.getColumnName(i));
				x++;
			}

			this.maxWeight = getMaxWeight(dataTable);
		}
		repaint();
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(getMaxNumberPerRow() * plotterSize + 2 * MARGIN, getMaxNumberPerRow() * plotterSize + 2
				* MARGIN);
	}

	private int getMaxNumberPerRow() {
		return (int) Math.ceil(Math.sqrt(plotters.length));
	}

	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		int xPos = 0;
		int yPos = 0;
		for (int x = 0; x < plotters.length; x++) {
			if (xPos >= getMaxNumberPerRow()) {
				yPos++;
				xPos = 0;
			}
			Graphics2D newSpace = (Graphics2D) graphics.create();
			newSpace.translate(MARGIN + xPos * plotterSize, MARGIN + yPos * plotterSize);
			drawWeightRectangle(newSpace, this.dataTable, x, this.maxWeight, plotterSize);
			newSpace.translate(WEIGHT_BORDER_WIDTH + 2, WEIGHT_BORDER_WIDTH + 2);
			plotters[x].paintQuartiles(newSpace, plotterSize - (WEIGHT_BORDER_WIDTH + 8), plotterSize
					- (WEIGHT_BORDER_WIDTH + 8));
			newSpace.dispose();
			xPos++;
		}
		if (colorIndex != -1) {
			drawLegend(graphics, dataTable, colorIndex, 0, RectangleStyle.ALPHA);
		}
	}

	@Override
	public String getPlotName() {
		return "Color";
	}

	@Override
	public void setPlotColumn(int index, boolean plot) {
		colorIndex = index;
		updateMatrix();
	}

	@Override
	public boolean getPlotColumn(int index) {
		return index == colorIndex;
	}

	@Override
	public Icon getIcon(int index) {
		return null;
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.QUARTILE_PLOT_COLOR_MATRIX;
	}
}
