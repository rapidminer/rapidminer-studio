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
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.math.MathFunctions;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * This plotter plots the data in parallel coordinates. One of the attributes can be selected to
 * define the color of the lines.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class ParallelPlotter extends PlotterAdapter implements MouseListener {

	private static final long serialVersionUID = -175572158812122874L;

	/** Indicates which type of column mapping should be used. */
	private static final String[] COLUMN_MAPPING_TYPES = { "ordered", "weights", "random" };

	/** Indicates a ordered column mapping. */
	private static final int ORDERED = 0;

	/** Indicates a ordered column mapping. */
	private static final int WEIGHTS = 1;

	/** Indicates a ordered column mapping. */
	private static final int RANDOM = 2;

	private transient DataTable dataTable;

	private double[] min = new double[0];

	private double[] max = new double[0];

	private double maxWeight;

	private double globalMin = Double.NEGATIVE_INFINITY;

	private double globalMax = Double.POSITIVE_INFINITY;

	private int colorColumn = -1;

	private transient ToolTip toolTip = null;

	private JCheckBox localNormalizationBox;

	/** Selection of column mapping. */
	private JComboBox<String> columnMappingSelection;

	private boolean localNormalization = false;

	/** The currently selected type of column mapping. Default is ORDERED. */
	private int columnMappingType = ORDERED;

	/** Currently used random seed for random ordering. */
	private long orderRandomSeed = 2001;

	/** The random number generator for random seeds. */
	private Random randomSeedRandom = new Random();

	public ParallelPlotter(PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.white);
		addMouseListener(this);
		localNormalizationBox = new JCheckBox("local normalization", localNormalization);
		localNormalizationBox
				.setToolTipText("Indicates if a local normalization for each dimension should be performed or not.");
		localNormalizationBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				setLocalNormalization(localNormalizationBox.isSelected());
			}
		});
		this.columnMappingSelection = new JComboBox<>(COLUMN_MAPPING_TYPES);
		columnMappingSelection.setToolTipText("Indicates the type of column mapping (reordering).");
		this.columnMappingSelection.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setColumnMapping(columnMappingSelection.getSelectedIndex());
			}
		});
	}

	public ParallelPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
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
	public JComponent getOptionsComponent(int index) {
		if (index == 0) {
			return this.localNormalizationBox;
		} else if (index == 1) {
			JLabel label = new JLabel("Column mapping:");
			label.setToolTipText("Indicates the type of column mapping (reordering).");
			return label;
		} else if (index == 2) {
			return columnMappingSelection;
		} else {
			return null;
		}
	}

	private void setColumnMapping(int mapping) {
		this.columnMappingType = mapping;
		if (mapping == RANDOM) {
			this.orderRandomSeed = randomSeedRandom.nextLong();
		}
		repaint();
	}

	public void setLocalNormalization(boolean localNormalization) {
		this.localNormalization = localNormalization;
		repaint();
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

	public void setToolTip(ToolTip toolTip) {
		this.toolTip = toolTip;
		repaint();
	}

	private void prepareData() {
		// calculate min and max
		int columns = this.dataTable.getNumberOfColumns();
		min = new double[columns];
		max = new double[columns];
		for (int c = 0; c < columns; c++) {
			min[c] = Double.POSITIVE_INFINITY;
			max[c] = Double.NEGATIVE_INFINITY;
		}
		globalMin = Double.POSITIVE_INFINITY;
		globalMax = Double.NEGATIVE_INFINITY;

		synchronized (dataTable) {
			Iterator<DataTableRow> i = dataTable.iterator();
			while (i.hasNext()) {
				DataTableRow row = i.next();
				for (int c = 0; c < dataTable.getNumberOfColumns(); c++) {
					double value = row.getValue(c);
					min[c] = MathFunctions.robustMin(min[c], value);
					max[c] = MathFunctions.robustMax(max[c], value);
					if (c != colorColumn) {
						globalMin = MathFunctions.robustMin(globalMin, min[c]);
						globalMax = MathFunctions.robustMax(globalMax, max[c]);
					}
				}
			}
		}

		this.maxWeight = getMaxWeight(this.dataTable);
	}

	private int[] getColumnMapping() {
		int numberOfColumns = this.dataTable.getNumberOfColumns();
		if (colorColumn >= 0) {
			numberOfColumns--;
		}
		int[] mapping = new int[numberOfColumns];
		int counter = 0;
		for (int i = 0; i < this.dataTable.getNumberOfColumns(); i++) {
			if (i != colorColumn) {
				mapping[counter++] = i;
			}
		}
		switch (columnMappingType) {
			case WEIGHTS:
				if (this.dataTable.isSupportingColumnWeights()) {
					List<WeightIndex> indices = new LinkedList<WeightIndex>();
					for (int i = 0; i < this.dataTable.getNumberOfColumns(); i++) {
						if (colorColumn != i) {
							indices.add(new WeightIndex(i, Math.abs(this.dataTable.getColumnWeight(i))));
						}
					}
					Collections.sort(indices);
					Iterator<WeightIndex> w = indices.iterator();
					counter = 0;
					while (w.hasNext()) {
						mapping[counter++] = w.next().getIndex();
					}
				} else {
					// LogService.getGlobal().log("Cannot use weight based ordering since no column weights are given.",
					// LogService.WARNING);
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.gui.plotter.ParallelPlotter.using_weight_based_ordering_error");
				}
				break;
			case RANDOM:
				List<Integer> indices = new ArrayList<Integer>();
				for (int i = 0; i < mapping.length; i++) {
					mapping[i] = i;
					if (colorColumn != i) {
						indices.add(i);
					}
				}
				Random random = new Random(orderRandomSeed);
				for (int i = 0; i < mapping.length; i++) {
					if (colorColumn != i) {
						int other = indices.get(random.nextInt(indices.size()));
						int dummy = mapping[i];
						mapping[i] = mapping[other];
						mapping[other] = dummy;
					}
				}
				break;
			case ORDERED:
			default: // do nothing
				break;
		}
		return mapping;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		paintParallelPlot(g);
	}

	public void paintParallelPlot(Graphics g) {
		int pixWidth = getWidth() - 2 * MARGIN;
		int pixHeight = getHeight() - 2 * MARGIN;

		// translate to ignore margins
		Graphics2D translated = (Graphics2D) g.create();
		translated.translate(MARGIN, MARGIN);

		// prepare data ...
		prepareData();

		// legend
		if ((colorColumn >= 0) && (colorColumn < min.length) && !Double.isInfinite(min[colorColumn])
				&& !Double.isInfinite(max[colorColumn])
				&& (dataTable.isNominal(colorColumn) || (min[colorColumn] != max[colorColumn]))
				&& (dataTable.getNumberOfRows() > 0)) {
			drawLegend(g, dataTable, colorColumn);
		}

		// draw grid, lines, etc.
		g.setColor(Color.black);
		draw(translated, pixWidth, pixHeight);
		translated.dispose();

		drawToolTip((Graphics2D) g, this.toolTip);
	}

	private void draw(Graphics g, int pixWidth, int pixHeight) {
		if (this.dataTable.isSupportingColumnWeights()) {
			drawWeights(g, pixWidth, pixHeight);
		}
		drawGrid(g, pixWidth, pixHeight);
		drawLines(g, pixWidth, pixHeight);
	}

	private void drawWeights(Graphics graphics, int pixWidth, int pixHeight) {
		double currentX = 0.0f;
		Graphics2D g = (Graphics2D) graphics;
		int[] columnMapping = getColumnMapping();
		double columnDistance = pixWidth / (double) (columnMapping.length - 1);
		for (int i = 0; i < columnMapping.length; i++) {
			g.setColor(getWeightColor(this.dataTable.getColumnWeight(columnMapping[i]), this.maxWeight));
			Rectangle2D weightRect = null;
			if (i == 0) {
				weightRect = new Rectangle2D.Double(currentX, 0.0d, columnDistance / 2.0d, pixHeight);
			} else if (i == columnMapping.length - 1) {
				weightRect = new Rectangle2D.Double(currentX - (columnDistance / 2.0d), 0.0d, columnDistance / 2.0d,
						pixHeight);
			} else {
				weightRect = new Rectangle2D.Double(currentX - (columnDistance / 2.0d), 0.0d, columnDistance, pixHeight);
			}
			g.fill(weightRect);
			currentX += columnDistance;
		}
	}

	private void drawGrid(Graphics graphics, int pixWidth, int pixHeight) {
		double currentX = 0.0f;
		Graphics2D g = (Graphics2D) graphics;
		int[] columnMapping = getColumnMapping();
		double columnDistance = pixWidth / (double) (columnMapping.length - 1);
		for (int i = 0; i < columnMapping.length; i++) {
			g.setColor(GRID_COLOR);
			if ((i == 0) || (i == columnMapping.length - 1) || (columnMapping.length < 100)) {
				g.drawLine((int) currentX, 0, (int) currentX, pixHeight);
			}
			if (columnMapping.length <= 10) {
				g.setColor(Color.BLACK);
				g.setFont(LABEL_FONT);
				Rectangle2D stringBounds = LABEL_FONT.getStringBounds(this.dataTable.getColumnName(columnMapping[i]),
						g.getFontRenderContext());
				double xPos = currentX;
				if (i == columnMapping.length - 1) {
					xPos -= stringBounds.getWidth();
				}
				g.drawString(this.dataTable.getColumnName(columnMapping[i]), (int) xPos,
						(int) (pixHeight + 2 + stringBounds.getHeight()));
			}
			currentX += columnDistance;
		}
		g.setColor(GRID_COLOR);
		g.drawLine(0, 0, (int) ((columnMapping.length - 1) * columnDistance), 0);
		g.drawLine(0, pixHeight, (int) ((columnMapping.length - 1) * columnDistance), pixHeight);
	}

	private void drawLines(Graphics g, int pixWidth, int pixHeight) {
		// ((Graphics2D)g).setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND,
		// BasicStroke.JOIN_ROUND));
		int[] columnMapping = getColumnMapping();
		double columnDistance = pixWidth / (double) (columnMapping.length - 1);
		Iterator<DataTableRow> i = this.dataTable.iterator();
		while (i.hasNext()) {
			DataTableRow row = i.next();
			GeneralPath path = new GeneralPath();
			boolean first = true;
			float currentX = 0;
			for (int k = 0; k < columnMapping.length; k++) {
				int d = columnMapping[k];
				float yPos = 0.0f;
				if (localNormalization) {
					yPos = (float) (pixHeight - ((row.getValue(d) - min[d]) / (max[d] - min[d]) * pixHeight));
				} else {
					yPos = (float) (pixHeight - ((row.getValue(d) - globalMin) / (globalMax - globalMin) * pixHeight));
				}
				if (first) {
					path.moveTo(currentX, yPos);
				} else {
					path.lineTo(currentX, yPos);
				}
				currentX += columnDistance;
				first = false;
			}
			Color color = Color.RED;
			if (colorColumn != -1) {
				double colorValue = getColorProvider().getPointColorValue(this.dataTable, row, colorColumn,
						min[colorColumn], max[colorColumn]);
				color = getColorProvider().getPointColor(colorValue);
			}
			g.setColor(color);
			((Graphics2D) g).draw(path);
		}
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
		int yPos = e.getY();
		if ((xPos > MARGIN) && (xPos < getWidth() - MARGIN)) {
			int[] mapping = getColumnMapping();
			float columnDistance = (float) (getWidth() - 2 * MARGIN) / (float) mapping.length;
			int column = (int) ((xPos - MARGIN) / columnDistance);
			setToolTip(new ToolTip(this.dataTable.getColumnName(mapping[column]), xPos, yPos));
		} else {
			setToolTip(null);
		}
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.PARALLEL_PLOT;
	}
}
