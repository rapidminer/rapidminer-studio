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
package com.rapidminer.gui.new_plotter.gui;

import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.VisualizationType;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;

import java.awt.CardLayout;

import javax.swing.JTree;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class SeriesFormatConfigurationPanel extends AbstractTreeSelectionDependentPanel {

	private static final long serialVersionUID = 1L;

	private final String AREA_BAR = "area/bar";
	private final String LINES = "lines";

	private LineChartConfigurationPanel lineChartPanel;
	private AreaAndBarChartConfigurationPanel areaAndBarChartPanel;

	public SeriesFormatConfigurationPanel(boolean smallIcons, JTree plotConfigurationTree, PlotInstance plotInstance) {
		super(plotConfigurationTree, plotInstance);

		this.setLayout(new CardLayout());
		lineChartPanel = new LineChartConfigurationPanel(smallIcons, plotConfigurationTree, plotInstance);
		addPlotInstanceChangeListener(lineChartPanel);
		areaAndBarChartPanel = new AreaAndBarChartConfigurationPanel(smallIcons, plotConfigurationTree, plotInstance);
		addPlotInstanceChangeListener(areaAndBarChartPanel);

		this.add(lineChartPanel, LINES);
		this.add(areaAndBarChartPanel, AREA_BAR);
		registerAsPlotConfigurationListener();
	}

	@Override
	protected void adaptGUI() {

		if (getSelectedValueSource() != null) {
			VisualizationType seriesType = getSelectedValueSource().getSeriesFormat().getSeriesType();

			// check for series type and handle lines and shapes different
			if (seriesType == VisualizationType.AREA || seriesType == VisualizationType.BARS) {
				CardLayout cardLayout = (CardLayout) this.getLayout();
				cardLayout.show(this, AREA_BAR);
			} else {
				CardLayout cardLayout = (CardLayout) this.getLayout();
				cardLayout.show(this, LINES);
			}
		}

	}

	@Override
	public boolean plotConfigurationChanged(PlotConfigurationChangeEvent change) {
		adaptGUI();
		return true;
	}

}
