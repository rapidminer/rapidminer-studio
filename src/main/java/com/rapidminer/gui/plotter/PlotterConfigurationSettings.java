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

import com.rapidminer.operator.IOObject;

import java.util.HashMap;


/**
 * This is the data holding class for plotter settings. It is used by the PlotterConfigurationModel
 * to store the actual parameters and their values.
 * 
 * @author Sebastian Land
 * @deprecated since 9.2.0
 */
@Deprecated
public class PlotterConfigurationSettings {

	/**
	 * Key to use in {@link IOObject#setUserData(String, Object)} when attaching a
	 * Map<String,String> with the plotter settings as user data.
	 */
	public static final String IOOBJECT_USER_DATA_SETTINGS_KEY = PlotterConfigurationSettings.class.getName() + ".settings";
	public static final String IOOBJECT_USER_DATA_PLOTTER_KEY = PlotterConfigurationSettings.class.getName() + ".plotter";

	// common settings
	public static final String AXIS_Z = "_axis_z_axis";
	public static final String AXIS_Y = "_axis_y_axis";
	public static final String AXIS_X = "_axis_x_axis";
	public static final String AXIS_STACK_COLUMN = "_axis_stack_column";
	public static final String INDEX_DIMENSION = "_axis_index_dimension";
	public static final String DIMENSION = "_axis_dimension";
	public static final String AXIS_HISTOGRAM = "_axis_histogram";
	public static final String POINT_COLOR = "_axis_point_color";
	public static final String GROUP_BY_COLUMN = "_axis_group_by_column";
	public static final String AGGREGATION = "_aggregation";
	public static final String AXIS_PLOT_COLUMNS = "_plot_columns";
	public static final String CLASS_COLUMN = "_axis_class_column";
	public static final String AXIS_PLOT_COLUMN = "_plot_column";
	public static final String BUBBLE_SIZE = "_axis_bubble_size";
	public static final String NUMBER_OF_BINS = "_number_of_bins";

	private String plotterName;

	// TODO change to Map<String, Map<String, String>>
	private HashMap<String, String> parameterSettings = new HashMap<>();
	private HashMap<String, Class<? extends Plotter>> availablePlotters;

	public PlotterConfigurationSettings() {}

	/**
	 * This is the clone constructor.
	 */
	private PlotterConfigurationSettings(PlotterConfigurationSettings other) {
		plotterName = other.plotterName;
		parameterSettings.putAll(other.parameterSettings);
		availablePlotters = other.availablePlotters;
	}

	@Override
	public PlotterConfigurationSettings clone() {
		return new PlotterConfigurationSettings(this);
	}

	public String getPlotterName() {
		return plotterName;
	}

	public void setPlotterName(String plotterName) {
		this.plotterName = plotterName;
	}

	public HashMap<String, String> getParameterSettings() {
		return parameterSettings;
	}

	public HashMap<String, Class<? extends Plotter>> getAvailablePlotters() {
		return availablePlotters;
	}

	public void setAvailablePlotters(HashMap<String, Class<? extends Plotter>> availablePlotters) {
		this.availablePlotters = availablePlotters;
	}

	/* Parameter methods */

	/**
	 * This method sets a parameter specified by the key to the given value. Calling this method
	 * will not inform any listener since it's only a data storage. Please use setParameterValue of
	 * PlotterConfigurationModel instead.
	 */
	public void setParameterValue(String key, String value) {
		parameterSettings.put(key, value);
	}

	/**
	 * This method will return the parameter value of the given generalized key. Generalized keys
	 * will be used internally by the PlotterSettings.
	 */
	public String getParameterValue(String generalizedKey) {
		return parameterSettings.get(generalizedKey);
	}

	/**
	 * This method checks whether the parameter with this generalized key is stored.
	 */
	public boolean isParameterSet(String generalizedKeyName) {
		return parameterSettings.containsKey(generalizedKeyName);
	}

	public void setParamterSettings(HashMap<String, String> settings) {
		this.parameterSettings = settings;

	}
}
