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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * A Survey plot is a set of vertical histograms. Each column represents one dimension of the data
 * set. The data points are not grouped into bins but are drawn as horizontal lines. The length of
 * each line corresponds to the value of the data row with respect to the drawn column. Maximal
 * three dimensions can be selected in order to sort the dataset before the data is displayed, in
 * addition another color column can be selected.
 * 
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class SurveyPlotter extends PlotterAdapter implements MouseListener {

	private static final long serialVersionUID = -4510716260204035289L;

	private static final int MAX_NUMBER_OF_COLUMNS = 100;

	private static final int FIRST = 0;

	private static final int SECOND = 1;

	private static final int THIRD = 2;

	private class SurveyRow implements Comparable<SurveyRow> {

		private double[] data;
		private double color;

		private SurveyRow(double[] data, double color) {
			this.data = data;
			this.color = color;
		}

		@Override
		public int compareTo(SurveyRow row) {
			int result = 0;
			for (int i = 0; i < sortingDimensions.length; i++) {
				if (sortingDimensions[i] != -1) {
					result = Double.compare(this.data[sortingDimensions[i]], row.data[sortingDimensions[i]]);
					if (result != 0) {
						return result;
					}
				}
			}
			if ((result == 0) && (colorColumn > -1)) {
				result = Double.compare(this.data[colorColumn], row.data[colorColumn]);
			}
			return result;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof SurveyRow)) {
				return false;
			} else {
				return this.data == ((SurveyRow) o).data;
			}
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(data);
		}
	}

	private transient DataTable dataTable;

	private double maxWeight = Double.NaN;

	private int colorColumn = -1;

	private int[] sortingDimensions = new int[] { -1, -1, -1 };

	private List<SurveyRow> dataRows = new LinkedList<SurveyRow>();

	private double[] min, max;

	private transient ToolTip toolTip = null;

	public SurveyPlotter(PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.white);
		addMouseListener(this);
	}

	public SurveyPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
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

	@Override
	public int getNumberOfAxes() {
		return sortingDimensions.length;
	}

	@Override
	public void setAxis(int index, int dimension) {
		sortingDimensions[index] = dimension;
		repaint();
	}

	@Override
	public int getAxis(int index) {
		return sortingDimensions[index];
	}

	@Override
	public String getAxisName(int index) {
		switch (index) {
			case FIRST:
				return "First column";
			case SECOND:
				return "Second column";
			case THIRD:
				return "Third column";
			default:
				return "none";
		}
	}

	private void prepareData() {
		dataRows.clear();
		this.min = new double[this.dataTable.getNumberOfColumns()];
		this.max = new double[this.dataTable.getNumberOfColumns()];
		for (int d = 0; d < min.length; d++) {
			this.min[d] = Double.POSITIVE_INFINITY;
			this.max[d] = Double.NEGATIVE_INFINITY;
		}

		synchronized (dataTable) {
			Iterator<DataTableRow> i = this.dataTable.iterator();
			while (i.hasNext()) {
				DataTableRow row = i.next();
				for (int d = 0; d < row.getNumberOfValues(); d++) {
					double value = row.getValue(d);
					this.min[d] = MathFunctions.robustMin(this.min[d], value);
					this.max[d] = MathFunctions.robustMax(this.max[d], value);
				}
			}
			i = this.dataTable.iterator();
			while (i.hasNext()) {
				DataTableRow row = i.next();
				double[] data = new double[row.getNumberOfValues()];
				for (int d = 0; d < data.length; d++) {
					data[d] = row.getValue(d);
				}
				double color = 1.0d;
				if (colorColumn >= 0) {
					color = getColorProvider().getPointColorValue(this.dataTable, row, colorColumn, min[colorColumn],
							max[colorColumn]);
				}
				dataRows.add(new SurveyRow(data, color));
			}

			this.maxWeight = getMaxWeight(dataTable);

		}
		Collections.sort(dataRows);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(getWidth(), this.dataTable.getNumberOfRows() + 3 * MARGIN);
	}

	public void setToolTip(ToolTip toolTip) {
		this.toolTip = toolTip;
		repaint();
	}

	private boolean isSpecialColumn(int index, int[] differentSortingDimensions) {
		for (int i = 0; i < differentSortingDimensions.length; i++) {
			if (index == differentSortingDimensions[i]) {
				return true;
			}
		}
		return false;
	}

	private int[] getDifferentSortingDimensions() {
		List<Integer> dimensions = new LinkedList<Integer>();
		for (int i = 0; i < sortingDimensions.length; i++) {
			if (!dimensions.contains(sortingDimensions[i])) {
				dimensions.add(sortingDimensions[i]);
			}
		}
		int[] result = new int[dimensions.size()];
		Iterator<Integer> i = dimensions.iterator();
		int counter = 0;
		while (i.hasNext()) {
			result[counter++] = i.next();
		}
		return result;
	}

	private int[] getColumnMapping() {
		int numberOfColumns = this.dataTable.getNumberOfColumns();
		int[] mapping = new int[numberOfColumns];
		int counter = 0;
		int[] differentSortingDimensions = getDifferentSortingDimensions();
		for (int i = 0; i < differentSortingDimensions.length; i++) {
			if (differentSortingDimensions[i] > -1) {
				mapping[counter++] = differentSortingDimensions[i];
			}
		}
		for (int i = 0; i < this.dataTable.getNumberOfColumns(); i++) {
			if (!isSpecialColumn(i, differentSortingDimensions)) {
				mapping[counter++] = i;
			}
		}
		return mapping;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		paintSurveyPlot(g);
	}

	public void paintSurveyPlot(Graphics graphics) {
		prepareData();

		// legend
		if ((colorColumn >= 0) && (colorColumn < min.length) && !Double.isInfinite(min[colorColumn])
				&& !Double.isInfinite(max[colorColumn])
				&& (dataTable.isNominal(colorColumn) || (min[colorColumn] != max[colorColumn])) && (dataRows.size() > 0)) {
			drawLegend(graphics, dataTable, colorColumn);
		}

		// translation
		Graphics2D g = (Graphics2D) graphics.create();
		g.translate(MARGIN, MARGIN);
		int width = getWidth() - 2 * MARGIN;

		// frame
		Rectangle2D frame = new Rectangle2D.Double(-1, MARGIN - 1, width + 1, this.dataTable.getNumberOfRows() + 1);
		g.setColor(GRID_COLOR);
		g.draw(frame);

		// columns
		int[] mapping = getColumnMapping();
		float columnDistance = (float) width / (float) mapping.length;
		float currentX = 0.0f;
		for (int i = 0; i < mapping.length; i++) {
			paintSurveyColumn(g, mapping[i], currentX, columnDistance);
			currentX += columnDistance;
		}

		drawToolTip((Graphics2D) graphics, this.toolTip);
	}

	private void paintSurveyColumn(Graphics graphics, int column, float currentX, float columnDistance) {
		Graphics2D g = (Graphics2D) graphics.create();
		g.translate(currentX, 0);

		// draw weight rect
		if (dataTable.isSupportingColumnWeights()) {
			Color weightColor = getWeightColor(dataTable.getColumnWeight(column), this.maxWeight);
			Rectangle2D weightRect = new Rectangle2D.Double(0, MARGIN, columnDistance, this.dataTable.getNumberOfRows());
			g.setColor(weightColor);
			g.fill(weightRect);
		}

		if (this.dataTable.getNumberOfColumns() <= 10) {
			g.drawString(this.dataTable.getColumnName(column), 0, MARGIN - 3);
		}
		g.translate(0, MARGIN);

		g.setColor(GRID_COLOR);
		g.drawLine(0, 0, 0, this.dataTable.getNumberOfRows());

		g.translate(1, 0);
		columnDistance--;
		int counter = 0;
		Iterator<SurveyRow> s = dataRows.iterator();
		while (s.hasNext()) {
			SurveyRow row = s.next();
			double[] data = row.data;
			double length = norm(data, column) * columnDistance;
			double color = row.color;
			g.setColor(getColorProvider().getPointColor(color));
			g.drawLine(0, counter, (int) length, counter);
			counter++;
		}
	}

	private double norm(double[] data, int column) {
		return (data[column] - min[column]) / (max[column] - min[column]);
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {
		int xPos = e.getX();
		if ((xPos > MARGIN) && (xPos < getWidth() - MARGIN)) {
			int[] mapping = getColumnMapping();
			float columnDistance = (float) (getWidth() - 2 * MARGIN) / (float) mapping.length;
			int column = (int) ((xPos - MARGIN) / columnDistance);
			setToolTip(new ToolTip(this.dataTable.getColumnName(mapping[column]), xPos, e.getY()));
		} else {
			setToolTip(null);
		}
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.SURVEY_PLOT;
	}
}
