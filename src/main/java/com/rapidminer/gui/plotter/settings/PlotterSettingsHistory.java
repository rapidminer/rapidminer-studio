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
package com.rapidminer.gui.plotter.settings;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.plotter.HeuristicPlotterConfigurator;
import com.rapidminer.gui.plotter.Plotter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.PlotterConfigurationSettings;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.ProcessingStep;


/**
 * This class holds informations about plotter settings in the processing history since the
 * RapidMiner startup. They might be used for pre-initilizing the plotter with settings from the
 * past processing history.
 *
 * Please note that this class must NOT store all information in
 *
 * @author Sebastian Land
 * @deprecated since 9.2.0
 */
@Deprecated
public final class PlotterSettingsHistory {

	private static final HashMap<ProcessingStep, PlotterConfigurationSettings> settingsHistory = new HashMap<>();

	@SuppressWarnings("unchecked")
	public static PlotterConfigurationModel getPlotterSettingsFromHistory(IOObject object, DataTable dataTable,
			LinkedHashMap<String, Class<? extends Plotter>> plotterSelections) {
		List<ProcessingStep> steps = object.getProcessingHistory();
		ListIterator<ProcessingStep> iterator = steps.listIterator(steps.size());
		PlotterConfigurationModel configurationModel = null;
		boolean isFirst = true;
		while (iterator.hasPrevious()) {
			ProcessingStep step = iterator.previous();
			PlotterConfigurationSettings settings = settingsHistory.get(step);
			if (settings != null) {
				// if it's not the last operator in history: Clone and register for last process
				// step
				if (!isFirst) {
					settings = settings.clone();
					settingsHistory.put(steps.get(steps.size() - 1), settings);
				}
				configurationModel = new PlotterConfigurationModel(settings, plotterSelections, dataTable);

				if (object.getUserData(PlotterConfigurationSettings.IOOBJECT_USER_DATA_SETTINGS_KEY) != null) { // check
																												// for
																												// user
																												// specified
																												// plotter
																												// settings
					configurationModel.addParameters((HashMap<String, String>) object
							.getUserData(PlotterConfigurationSettings.IOOBJECT_USER_DATA_SETTINGS_KEY));
				}
				if (object.getUserData(PlotterConfigurationSettings.IOOBJECT_USER_DATA_PLOTTER_KEY) != null) { // check
																												// for
																												// user
																												// specified
																												// plotter
					String p = (String) object.getUserData(PlotterConfigurationSettings.IOOBJECT_USER_DATA_PLOTTER_KEY);
					configurationModel.setPlotter(p);
				}

				// then break: Use the last found known step
				break;
			}
			isFirst = false;
		}
		// if we didn't find anything: Create new settings (with default or user specified values)
		// and add them to history
		if (configurationModel == null) {

			configurationModel = new PlotterConfigurationModel(plotterSelections, dataTable);
			HeuristicPlotterConfigurator heuristicConfigurator = new HeuristicPlotterConfigurator(configurationModel,
					dataTable);

			if (object.getUserData(PlotterConfigurationSettings.IOOBJECT_USER_DATA_SETTINGS_KEY) != null) { // check
																											// for
																											// user
																											// specified
																											// plotter
																											// settings
				configurationModel.addParameters((HashMap<String, String>) object
						.getUserData(PlotterConfigurationSettings.IOOBJECT_USER_DATA_SETTINGS_KEY));
			} else {
				configurationModel.setParameters(heuristicConfigurator.getDefaultSelection(configurationModel
						.getParameterSettings()));
			}
			if (object.getUserData(PlotterConfigurationSettings.IOOBJECT_USER_DATA_PLOTTER_KEY) != null) { // check
																											// for
																											// a
																											// user
																											// specified
																											// plotter
				configurationModel.setPlotter((String) object
						.getUserData(PlotterConfigurationSettings.IOOBJECT_USER_DATA_PLOTTER_KEY));
			} else {
				configurationModel.setPlotter(heuristicConfigurator.getDefaultPlotter());
			}

			if (!steps.isEmpty()) {
				settingsHistory.put(steps.get(steps.size() - 1), configurationModel.getPlotterSettings());
			}
		}
		return configurationModel;
	}
}
