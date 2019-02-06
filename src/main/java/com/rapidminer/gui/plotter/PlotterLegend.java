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

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;


/**
 * This plotter legend component can be used by external plotter components.
 * 
 * @author Sebastian Land, Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class PlotterLegend extends JComponent {

	private static final long serialVersionUID = -4737111168245916491L;

	private PlotterAdapter adapter;

	private transient DataTable dataTable;

	private int legendColumn = -1;

	public PlotterLegend(PlotterAdapter adapter) {
		super();
		this.adapter = adapter;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(adapter.getWidth() - 2 * PlotterAdapter.MARGIN, PlotterAdapter.MARGIN);
	}

	public void setLegendColumn(DataTable dataTable, int legendColumn) {
		this.dataTable = dataTable;
		this.legendColumn = legendColumn;
		repaint();
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		adapter.drawLegend(g, this.dataTable, legendColumn);
	}
}
