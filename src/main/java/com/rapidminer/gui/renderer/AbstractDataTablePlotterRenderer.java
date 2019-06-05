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
package com.rapidminer.gui.renderer;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.plotter.Plotter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.PlotterPanel;
import com.rapidminer.gui.plotter.settings.PlotterSettingsHistory;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualStringCondition;
import com.rapidminer.report.Reportable;

import java.awt.Component;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;


/**
 * This is the abstract renderer superclass for all renderers which should be a plotter based on a
 * given {@link DataTable}.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.1
 */
@Deprecated
public abstract class AbstractDataTablePlotterRenderer extends AbstractRenderer {

	public static final String PARAMETER_PLOTTER = "plotter";

	public abstract DataTable getDataTable(Object renderable, IOContainer ioContainer);

	@Override
	public String getName() {
		return "Plot View";
	}

	public LinkedHashMap<String, Class<? extends Plotter>> getPlotterSelection() {
		return PlotterConfigurationModel.COMPLETE_PLOTTER_SELECTION;
	}

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int width, int height) {
		DataTable dataTable = getDataTable(renderable, ioContainer);

		String plotterName = null;
		try {
			plotterName = getParameterAsString(PARAMETER_PLOTTER);
		} catch (UndefinedParameterError e) {
			e.printStackTrace();
		}

		PlotterConfigurationModel settings;
		if (plotterName != null) {
			settings = new PlotterConfigurationModel(getPlotterSelection(), plotterName, dataTable);
		} else {
			settings = new PlotterConfigurationModel(getPlotterSelection(), dataTable);
		}

		Plotter plotter = settings.getPlotter();
		List<ParameterType> plotterParameters = plotter.getParameterTypes(null);
		if (plotterParameters != null) {
			for (ParameterType type : plotterParameters) {
				String key = type.getKey();
				String value;
				try {
					value = getParameter(key);
					if (value != null) {
						settings.setParameterValue(key, value);
					}
				} catch (UndefinedParameterError e) {
					// only set defined parameters
				}
			}
		}
		plotter.getPlotter().setSize(width, height);
		return plotter;
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		String source = null;
		if (renderable instanceof ExampleSet) {
			source = ((ExampleSet) renderable).getSource();
		}
		return new PlotterPanel(PlotterSettingsHistory.getPlotterSettingsFromHistory((IOObject) renderable,
				getDataTable(renderable, ioContainer), getPlotterSelection()), source);
	}

	@Override
	public List<ParameterType> getParameterTypes(InputPort inputPort) {
		List<ParameterType> types = super.getParameterTypes(inputPort);

		String[] availablePlotterNames = getPlotterSelection().keySet().toArray(new String[getPlotterSelection().size()]);
		ParameterTypeStringCategory plotterType = null;
		if (availablePlotterNames.length == 0) {
			plotterType = new ParameterTypeStringCategory(PARAMETER_PLOTTER,
					"Indicates the type of the plotter which should be used.", availablePlotterNames, "dummy");
		} else {
			plotterType = new ParameterTypeStringCategory(PARAMETER_PLOTTER,
					"Indicates the type of the plotter which should be used.", availablePlotterNames,
					availablePlotterNames[0]);
		}
		plotterType.setExpert(false);
		plotterType.setEditable(false);
		types.add(plotterType);

		for (String plotterName : getPlotterSelection().keySet()) {
			Class<? extends Plotter> clazz = getPlotterSelection().get(plotterName);
			try {
				Constructor<? extends Plotter> constructor = clazz.getDeclaredConstructor(PlotterConfigurationModel.class);
				Plotter plotter = constructor.newInstance(new Object[] { new PlotterConfigurationModel(
						PlotterConfigurationModel.COMPLETE_PLOTTER_SELECTION, new SimpleDataTable("ForConstruction",
								new String[0])) });

				List<ParameterType> plotterParameters = plotter.getParameterTypes(inputPort);
				if (plotterParameters != null) {
					for (ParameterType type : plotterParameters) {
						type.registerDependencyCondition(new EqualStringCondition(this, PARAMETER_PLOTTER, false,
								plotterName));
						type.setHidden(false);
						type.setExpert(false);
						types.add(type);
					}
				}
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}

		return types;
	}
}
