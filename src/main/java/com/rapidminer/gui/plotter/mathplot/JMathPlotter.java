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
package com.rapidminer.gui.plotter.mathplot;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.math.plot.PlotPanel;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.plotter.Plotter;
import com.rapidminer.gui.plotter.PlotterAdapter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.PlotterLegend;


/**
 * The abstract super class for all plotters using the JMathPlot library. The actual plotting must
 * be done in the method {@link #paintComponent(Graphics)} where some helper methods defined in this
 * class can be used. Another method usually implemented is {@link #getNumberOfAxes()}.
 *
 * @author Ingo Mierswa, Sebastian Land
 * @deprecated since 9.2.0
 */
@Deprecated
public abstract class JMathPlotter extends PlotterAdapter {

	/**
	 *
	 */
	private static final long serialVersionUID = -7018389000051768349L;

	/** Indicates the position of the JMathPlot legend. */
	private static final String LEGEND_POSITION = "NORTH";

	/** The currently used data table object. */
	private DataTable dataTable;

	/** The actual plotter panel of JMathPlot. */
	private PlotPanel plotpanel;

	/** The plotter legend which can be used to display the values with respect to the used colors. */
	private PlotterLegend legend;

	/** Indicates which columns will be plotted. */
	private boolean[] columns = new boolean[0];

	/** The used axes columns. */
	private int[] axis = new int[] { -1, -1 };

	/**
	 * Creates a new JMathPlotter. If the method {@link #hasRapidMinerValueLegend()} returns true,
	 * the usual RapidMiner color legend will be used ( {@link PlotterLegend}).
	 */
	public JMathPlotter(PlotterConfigurationModel settings) {
		super(settings);
		this.axis = new int[getNumberOfAxes()];
		for (int i = 0; i < this.axis.length; i++) {
			this.axis[i] = -1;
		}
	}

	/** Creates the new plotter and sets the data table. */
	public JMathPlotter(PlotterConfigurationModel settings, DataTable dataTable) {
		this(settings);
		setDataTable(dataTable);
	}

	/**
	 * Returns this. Subclasses which do not want to use this object (JPanel) for plotting should
	 * directly implement {@link Plotter}.
	 */
	@Override
	public JComponent getPlotter() {
		if (this.plotpanel == null) {
			this.plotpanel = createPlotPanel();

			if (hasLegend()) {
				this.plotpanel.addLegend(LEGEND_POSITION);
			}

			GridBagLayout layout = new GridBagLayout();
			this.setLayout(layout);
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 1;
			if (hasRapidMinerValueLegend()) {
				legend = new PlotterLegend(this);
				c.weighty = 0;
				JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
				legendPanel.setBackground(Color.white);
				legendPanel.add(legend);
				layout.setConstraints(legendPanel, c);
				add(legendPanel);
			}
			c.weighty = 1;
			layout.setConstraints(plotpanel, c);
			add(plotpanel);
		}

		return this;
	}

	/** Must be implemented by subclasses in order to support 2D or 3D plots. */
	protected abstract PlotPanel createPlotPanel();

	protected abstract void update();

	protected abstract int getNumberOfOptionIcons();

	// =============================
	// helper method for subclasses
	// =============================

	protected PlotterLegend getLegendComponent() {
		return this.legend;
	}

	protected boolean hasLegend() {
		return true;
	}

	protected boolean hasRapidMinerValueLegend() {
		return false;
	}

	protected DataTable getDataTable() {
		return this.dataTable;
	}

	protected int countColumns() {
		return this.columns.length;
	}

	protected PlotPanel getPlotPanel() {
		if (this.plotpanel == null) {
			getPlotter();
		}
		return this.plotpanel;
	}

	// ==============================================

	@Override
	public void setAxis(int index, int dimension) {
		if (index < getNumberOfAxes()) {
			if (axis[index] != dimension) {
				axis[index] = dimension;
			}
		}
		repaint();
	}

	@Override
	public int getAxis(int index) {
		if (index >= 0 && index < getNumberOfAxes()) {
			if (this.axis == null) {
				return -1;
			} else {
				return this.axis[index];
			}
		} else {
			return -1;
		}
	}

	@Override
	public void setDataTable(DataTable dataTable) {
		super.setDataTable(dataTable);
		this.dataTable = dataTable;
		columns = new boolean[dataTable.getNumberOfColumns()];
	}

	@Override
	public Icon getIcon(int index) {
		return null;
	}

	@Override
	public int getNumberOfAxes() {
		return 2;
	}

	@Override
	public String getAxisName(int index) {
		switch (index) {
			case 0:
				return "x-Axis";
			case 1:
				return "y-Axis";
			default:
				return "none";
		}
	}

	@Override
	public void setPlotColumn(int index, boolean plot) {
		if (getValuePlotSelectionType() == MULTIPLE_SELECTION) {
			if (this.columns[index] != plot) {
				if (index != -1) {
					columns[index] = plot;
				}
			}
		} else {
			this.columns = new boolean[columns.length];
			if (index != -1) {
				this.columns[index] = plot;
			}
		}
		repaint();
	}

	@Override
	public boolean getPlotColumn(int index) {
		return columns[index];
	}

	/**
	 * Removes the data view button and adds the legend under the plotter panel if the method
	 * {@link #hasLegend()} returns true.
	 */
	@Override
	public JComponent getOptionsComponent(int index) {
		if (index == 0) {
			// removes the icon for dataview in the toolbar
			while (this.plotpanel.plotToolBar.getComponentCount() > getNumberOfOptionIcons()) {
				this.plotpanel.plotToolBar.remove(this.plotpanel.plotToolBar.getComponentCount() - 1);
			}
			for (int i = 0; i < plotpanel.plotToolBar.getComponentCount(); i++) {
				Component c = plotpanel.plotToolBar.getComponent(i);
				if (c instanceof JButton) {
					((AbstractButton) c).setContentAreaFilled(false);
				}
			}
			return this.plotpanel.plotToolBar;
		} else {
			return null;
		}
	}

	@Override
	public void repaint() {
		update();
		super.repaint();
	}
}
