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
import com.rapidminer.tools.Tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;


/**
 * Presents values by boxes more filled the higher the values are.
 * 
 * @author Daniel Hakenjos, Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class HintonDiagram extends PlotterAdapter implements MouseListener {

	private static final long serialVersionUID = -1299407916734619185L;

	private List<NameValue> values = new ArrayList<NameValue>();

	private double maxWeight;

	private int boxSize = 51;

	private int horizontalCount, verticalCount;

	private String currentToolTip;

	private double toolTipX, toolTipY;

	private int plotIndex = -1;

	private transient DataTable dataTable;

	private boolean absolute = false;

	private boolean sorting = false;

	public HintonDiagram(PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.white);
		addMouseListener(this);
	}

	public HintonDiagram(PlotterConfigurationModel settings, DataTable dataTable) {
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
	public String getPlotName() {
		return "Plot";
	}

	@Override
	public void setPlotColumn(int index, boolean plot) {
		plotIndex = index;
		repaint();
	}

	@Override
	public boolean getPlotColumn(int index) {
		return index == plotIndex;
	}

	@Override
	public boolean canHandleZooming() {
		return true;
	}

	@Override
	public Icon getIcon(int index) {
		return null;
	}

	@Override
	public void setZooming(int amount) {
		if (amount % 2 == 0) {
			amount++;
		}
		boxSize = amount;
		repaint();
	}

	@Override
	public int getInitialZoomFactor() {
		return boxSize;
	}

	@Override
	public void setAbsolute(boolean absolute) {
		this.absolute = absolute;
		repaint();
	}

	@Override
	public boolean isSupportingAbsoluteValues() {
		return true;
	}

	@Override
	public void setSorting(boolean sorting) {
		this.sorting = sorting;
		repaint();
	}

	@Override
	public boolean isSupportingSorting() {
		return true;
	}

	private void prepareData() {
		this.values.clear();
		if (plotIndex >= 0) {
			Iterator<DataTableRow> i = dataTable.iterator();
			this.maxWeight = Double.NEGATIVE_INFINITY;
			while (i.hasNext()) {
				DataTableRow row = i.next();
				double value = row.getValue(plotIndex);
				if (absolute) {
					value = Math.abs(value);
				}
				this.maxWeight = Math.max(maxWeight, Math.abs(value));
				String id = row.getId();
				if (id == null) {
					id = value + "";
				}
				values.add(new NameValue(id, value));
			}
			if (sorting) {
				Collections.sort(values);
			}
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		int width = this.getWidth();
		int height = this.getHeight();

		prepareData();

		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);

		// draw grid
		horizontalCount = (int) Math.floor(((double) (width + 1)) / ((double) (boxSize + 1)));
		verticalCount = (int) Math.floor(((double) (height + 1)) / ((double) (boxSize + 1)));

		if (horizontalCount * verticalCount < values.size()) {
			while (horizontalCount * verticalCount < values.size()) {
				verticalCount++;
			}
		} else if (horizontalCount * verticalCount > values.size()) {
			while (horizontalCount * (verticalCount - 1) > values.size()) {
				verticalCount--;
			}
		} else {
			// nothing to do here
		}
		this.setPreferredSize(new Dimension(horizontalCount * (boxSize + 1) + 1, verticalCount * (boxSize + 1) + 1));

		g.setColor(Color.BLACK);
		g.drawRect(0, 0, horizontalCount * (boxSize + 1), verticalCount * (boxSize + 1));

		for (int h = 1; h < horizontalCount; h++) {
			g.drawLine(boxSize * h + h, 0, boxSize * h + h, boxSize * verticalCount + verticalCount - 1);
		}
		for (int v = 1; v < verticalCount; v++) {
			g.drawLine(0, boxSize * v + v, boxSize * horizontalCount + horizontalCount - 1, boxSize * v + v);
		}

		int att = 0;
		int horiz = 1;
		int vert = 1;
		while (att < values.size()) {
			NameValue nameValue = values.get(att);
			double value = nameValue.getValue();
			ColorProvider colorProvider = getColorProvider();
			if (value < 0.0d) {
				g.setColor(ColorProvider.reduceColorBrightness(colorProvider.getMinLegendColor()));
			} else {
				g.setColor(ColorProvider.reduceColorBrightness(colorProvider.getMaxLegendColor()));
			}

			int breite = (int) (Math.abs(value) / maxWeight * boxSize);
			int centerx = (horiz - 1) * (boxSize + 1) + (boxSize + 1) / 2;
			int centery = (vert - 1) * (boxSize + 1) + (boxSize + 1) / 2;
			g.fillRect(centerx - breite / 2, centery - breite / 2, breite, breite);

			horiz++;
			if (horiz > horizontalCount) {
				horiz = 1;
				vert++;
			}
			att++;
		}
		g.setColor(Color.WHITE);
		if (horiz <= horizontalCount) {
			g.fillRect((horiz == 1) ? 0 : (horiz - 1) * (boxSize + 1) + 1, (vert - 1) * (boxSize + 1) + 1, width, height);
		}
		vert++;
		if (vert <= verticalCount) {
			g.fillRect(0, (vert - 1) * (boxSize + 1) + 1, width, height);
		}

		drawToolTip((Graphics2D) g);
	}

	private void drawToolTip(Graphics2D g) {
		if (currentToolTip != null) {
			g.setFont(LABEL_FONT);
			Rectangle2D stringBounds = LABEL_FONT.getStringBounds(currentToolTip, g.getFontRenderContext());
			g.setColor(TOOLTIP_COLOR);
			Rectangle2D bg = new Rectangle2D.Double(toolTipX - stringBounds.getWidth() / 2 - 4, toolTipY
					- stringBounds.getHeight() / 2 - 2, stringBounds.getWidth() + 5, Math.abs(stringBounds.getHeight()) + 3);
			g.fill(bg);
			g.setColor(Color.black);
			g.draw(bg);
			g.drawString(currentToolTip, (float) (toolTipX - stringBounds.getWidth() / 2) - 2, (float) (toolTipY + 3));
		}
	}

	private void setToolTip(String toolTip, double x, double y) {
		this.currentToolTip = toolTip;
		this.toolTipX = x;
		this.toolTipY = y;
		repaint();
	}

	private String getAttributeName(int x, int y) {
		int horiz = x / (boxSize + 1) + ((x % (boxSize + 1) > 0) ? 1 : 0);
		horiz = Math.min(horiz, horizontalCount);

		int vert = y / (boxSize + 1) + ((y % (boxSize + 1) > 0) ? 1 : 0);
		vert = Math.min(vert, verticalCount);

		int index = (vert - 1) * horizontalCount + horiz;
		index = Math.min(index, values.size());
		index = Math.max(index, 0);

		NameValue nameValue = values.get(index - 1);
		return nameValue.getName() + ": " + Tools.formatNumber(nameValue.getValue());
	}

	@Override
	public void mouseClicked(MouseEvent event) {
		String name = getAttributeName(event.getX(), event.getY());
		setToolTip(name, event.getX(), event.getY());
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		currentToolTip = null;
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {}

	@Override
	public void mouseExited(MouseEvent arg0) {}

	@Override
	public void mousePressed(MouseEvent arg0) {}

	public void mouseDragged(MouseEvent arg0) {}

	public void mouseMoved(MouseEvent event) {}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.HINTON_PLOT;
	}
}
