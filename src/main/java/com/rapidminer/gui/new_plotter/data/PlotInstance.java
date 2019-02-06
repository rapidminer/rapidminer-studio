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
package com.rapidminer.gui.new_plotter.data;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.new_plotter.MasterOfDesaster;
import com.rapidminer.gui.new_plotter.PlotConfigurationError;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.engine.PlotEngine;
import com.rapidminer.gui.new_plotter.listener.PlotConfigurationListener;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;

import java.util.LinkedList;
import java.util.List;


/**
 * A plot instance, composed from a {@link PlotConfiguration} and {@link PlotData}. Can be used by a
 * {@link PlotEngine} to create a chart.
 * 
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class PlotInstance implements PlotConfigurationListener {

	private PlotConfiguration masterPlotConfiguration;
	private PlotData plotData;
	private MasterOfDesaster masterOfDesaster;
	private PlotConfiguration currentPlotConfigurationClone;

	public PlotInstance(PlotConfiguration plotConfiguration, DataTable dataTable) {
		this.masterOfDesaster = new MasterOfDesaster();
		this.masterPlotConfiguration = plotConfiguration;

		// THIS ENSURES THAT THE PLOT INSTANCE GETS TO KNOW THE CURRENT WORKING COPY AS FIRST
		// LISTENER
		this.masterPlotConfiguration.addPlotConfigurationListener(this, true);  // NEVER EVER REMOVE
																				// THIS.

		currentPlotConfigurationClone = plotConfiguration.clone();
		plotData = new PlotData(this, dataTable);
	}

	public PlotConfiguration getCurrentPlotConfigurationClone() {
		return currentPlotConfigurationClone;
	}

	public PlotConfiguration getMasterPlotConfiguration() {
		return masterPlotConfiguration;
	}

	public PlotData getPlotData() {
		return plotData;
	}

	public void setPlotData(PlotData plotData) {
		this.plotData = plotData;
	}

	public MasterOfDesaster getMasterOfDesaster() {
		return masterOfDesaster;
	}

	public List<PlotConfigurationError> getErrors() {
		List<PlotConfigurationError> errorList = new LinkedList<PlotConfigurationError>();
		errorList.addAll(currentPlotConfigurationClone.getErrors());
		errorList.addAll(plotData.getErrors());
		return errorList;
	}

	public List<PlotConfigurationError> getWarnings() {
		List<PlotConfigurationError> warnings = new LinkedList<PlotConfigurationError>();
		warnings.addAll(currentPlotConfigurationClone.getWarnings());
		warnings.addAll(plotData.getWarnings());
		return warnings;
	}

	public boolean isValid() {
		return currentPlotConfigurationClone.isValid() && plotData.isValid();
	}

	public boolean hasWarnings() {
		return currentPlotConfigurationClone.getWarnings().size() > 0 || plotData.getWarnings().size() > 0;
	}

	@Override
	public boolean plotConfigurationChanged(PlotConfigurationChangeEvent change) {
		this.currentPlotConfigurationClone = change.getSource();
		return true;
	}

	public void triggerReplot() {
		this.masterPlotConfiguration.triggerReplot();
	}
}
