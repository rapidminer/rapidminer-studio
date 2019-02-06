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
import com.rapidminer.gui.plotter.conditions.ColumnsPlotterCondition;
import com.rapidminer.gui.plotter.conditions.PlotterCondition;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;


/**
 * <code>GridViz</code> is a simple extension of <code>RadViz</code> that places the dimensional
 * anchors on a rectangular grid instead of using the perimeter of a circle. The number of
 * dimensions that can be displayed increases significantly.
 * 
 * @author Daniel Hakenjos, Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class GridVizPlotter extends RadVizPlotter {

	private static final long serialVersionUID = 9178351977037267613L;

	private static final int MAX_NUMBER_OF_COLUMNS = 10000;

	/** Constructs a new GridVizPlotter. */
	public GridVizPlotter(PlotterConfigurationModel settings) {
		super(settings);
	}

	/** Constructs a new GridVizPlotter. */
	public GridVizPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
		super(settings);
		setDataTable(dataTable);
	}

	@Override
	public PlotterCondition getPlotterCondition() {
		return new ColumnsPlotterCondition(MAX_NUMBER_OF_COLUMNS);
	}

	protected void calculateAttributeVectors(int totalSize, int gridSize, int gridDelta) {
		int dim = this.dataTable.getNumberOfColumns();
		anchorVectorX = new double[dim];
		anchorVectorY = new double[dim];

		int gridX = 0;
		int gridY = totalSize;
		int counter = 0;
		for (int i = 0; i < this.dataTable.getNumberOfColumns(); i++) {
			if ((i == colorColumn) || (shouldIgnoreColumn(i))) {
				continue;
			}
			if (counter % gridSize == 0) {
				gridX = 0;
				gridY -= gridDelta;
			}
			anchorVectorX[i] = gridX;
			anchorVectorY[i] = gridY;

			gridX += gridDelta;
			counter++;
		}
	}

	@Override
	protected void paintPlotter(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics.create();
		int width = getWidth();
		int height = getHeight();

		g.setColor(Color.BLACK);
		int totalSize = Math.min(width, height) - 2 * MARGIN;
		int numberOfColumns = this.dataTable.getNumberOfColumns();
		if (colorColumn >= 0) {
			numberOfColumns--;
		}
		numberOfColumns -= ignoreList.getSelectedIndices().length;
		if (numberOfColumns == 0) {
			return;
		}

		int gridSize = (int) Math.ceil(Math.sqrt(numberOfColumns));
		int gridDelta = totalSize / gridSize;

		calculateAttributeVectors(totalSize, gridSize, gridDelta);

		// draw the axis and calculate the attribute vectors
		g.setFont(LABEL_FONT);
		for (int i = 0; i < this.dataTable.getNumberOfColumns(); i++) {
			if ((i == colorColumn) || (shouldIgnoreColumn(i))) {
				continue;
			}
			int x = (int) (MARGIN + anchorVectorX[i]);
			int y = (int) (MARGIN + totalSize - anchorVectorY[i]);
			// draw weights
			if (this.dataTable.isSupportingColumnWeights()) {
				g.setColor(getWeightColor(this.dataTable.getColumnWeight(columnMapping[i]), this.maxWeight));
				Rectangle2D weightRect = new Rectangle2D.Double(x, y - gridDelta, gridDelta, gridDelta);
				g.fill(weightRect);
			}
			// draw lines
			g.setColor(GRID_COLOR);
			g.drawLine(x, y, x + gridDelta, y);
			g.drawLine(x, y, x, y - gridDelta);
			// draw axis-labels
			g.drawString(this.dataTable.getColumnName(columnMapping[i]), x + 5, y - 5);
		}

		// draw points
		calculateSamplePoints();
		int centerPoint = totalSize / 2 + MARGIN;
		Iterator<PlotterPoint> i = plotterPoints.iterator();
		ColorProvider colorProvider = getColorProvider();
		while (i.hasNext()) {
			drawPoint(g, i.next(), colorProvider, centerPoint, centerPoint, 1.0d);
		}

		// legend
		if ((colorColumn != -1) && (plotterPoints.size() > 0)) {
			drawLegend(g, dataTable, colorColumn);
		}
	}
}
