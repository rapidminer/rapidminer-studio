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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.tools.ParameterService;


/**
 * Control class used to handle the logic behind preselecting plotter axes.
 *
 * @author David Arnu, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class HeuristicPlotterConfigurator {

	public static final int DEFAULT_SELECTION_NUMBER = 3;

	public static final int MAX_NOMINAL_VALUES = 200;

	private static final String AGGREGATIOTN_DEFAULT_FUNCTION = "count";
	private static final String SUM = "sum";

	public static final String NUMERICAL = "Numerical";
	public static final String NOMINAL = "Nominal";
	public static final String DATE = "Date";

	public static final String LABEL = "Label";

	private Map<String, List<String>> categoryToColumnListMap;
	private final String[] columnNames;
	private final PlotterConfigurationModel settings;

	public HeuristicPlotterConfigurator(PlotterConfigurationModel plotterSettings, DataTable dataTable) {

		List<String> columnNamesNotTooBig = new ArrayList<>();
		// only take nominal columns with not to many values
		for (int i = 0; i < dataTable.getNumberOfColumns(); i++) {
			if (!(dataTable.isNominal(i) && dataTable.getNumberOfValues(i) > MAX_NOMINAL_VALUES)) {
				columnNamesNotTooBig.add(dataTable.getColumnName(i));
			}
		}

		this.columnNames = columnNamesNotTooBig.toArray(new String[columnNamesNotTooBig.size()]);
		this.settings = plotterSettings;
		this.categoryToColumnListMap = new HashMap<>();
		populateCategoryMap(dataTable);
	}

	private void populateCategoryMap(DataTable dataTable) {
		this.categoryToColumnListMap.put(HeuristicPlotterConfigurator.NUMERICAL, new ArrayList<String>());
		this.categoryToColumnListMap.put(HeuristicPlotterConfigurator.LABEL, new ArrayList<String>());
		this.categoryToColumnListMap.put(HeuristicPlotterConfigurator.NOMINAL, new ArrayList<String>());
		this.categoryToColumnListMap.put(HeuristicPlotterConfigurator.DATE, new ArrayList<String>());

		for (int i = 0; i < dataTable.getNumberOfColumns(); i++) {
			if (dataTable.isNumerical(i)) {
				this.categoryToColumnListMap.get(HeuristicPlotterConfigurator.NUMERICAL).add(dataTable.getColumnName(i));
			}
			if (dataTable.isNominal(i)) {
				if (dataTable.getNumberOfValues(i) <= MAX_NOMINAL_VALUES) {
					this.categoryToColumnListMap.get(HeuristicPlotterConfigurator.NOMINAL).add(dataTable.getColumnName(i));
				}
			}
			if (dataTable.isDateTime(i)) {
				this.categoryToColumnListMap.get(HeuristicPlotterConfigurator.DATE).add(dataTable.getColumnName(i));
			}
		}

		if (dataTable instanceof DataTableExampleSetAdapter) {
			DataTableExampleSetAdapter mine = (DataTableExampleSetAdapter) dataTable;

			String labelName = mine.getLabelName();
			if (labelName != null) {

				if (mine.isLabelNominal()) {
					// only add nominal labels with less than MAX_NOMINAL_VALUES different values
					if (mine.getNumberOfValues(mine.getColumnIndex(labelName)) <= MAX_NOMINAL_VALUES) {
						this.categoryToColumnListMap.get(HeuristicPlotterConfigurator.NOMINAL).add(labelName);
						this.categoryToColumnListMap.get(HeuristicPlotterConfigurator.LABEL).add(labelName);
					}
				} else {
					this.categoryToColumnListMap.get(HeuristicPlotterConfigurator.LABEL).add(labelName);
				}
				// remove Label from the numerical attribute list
				if (this.categoryToColumnListMap.get(HeuristicPlotterConfigurator.NUMERICAL).contains(labelName)) {
					this.categoryToColumnListMap.get(HeuristicPlotterConfigurator.NUMERICAL).remove(labelName);
				}
			}
			// if there is a cluster attribute it can be handled as an additional label for axis
			// selection:
			if (mine.getClusterName() != null) {
				this.categoryToColumnListMap.get(HeuristicPlotterConfigurator.LABEL).add(mine.getClusterName());
			}

		} else { // set empty values unless Label and Nominal are already set
			if (categoryToColumnListMap.get(HeuristicPlotterConfigurator.LABEL).isEmpty()) {
				this.categoryToColumnListMap.put(HeuristicPlotterConfigurator.LABEL, new ArrayList<String>());
			}
			if (categoryToColumnListMap.get(HeuristicPlotterConfigurator.NOMINAL).isEmpty()) {
				this.categoryToColumnListMap.put(HeuristicPlotterConfigurator.NOMINAL, new ArrayList<String>());
			}
		}
	}

	/**
	 * Returns name of the n-th entry of the specified value type or an empty String.
	 *
	 * @param type
	 *            The wanted value type
	 * @param number
	 *            Which entry of the specific value type should be returned
	 * @return The name list entry or an empty String. Default case is the first entry in the list.
	 */
	private String getNameListEntry(String type, int number) {

		String selection = "";
		if (categoryToColumnListMap.get(type).size() > number) {
			selection = categoryToColumnListMap.get(type).get(number);
		} else {
			if (!type.equals(DATE) && columnNames.length != 0) {
				selection = columnNames[0]; // default case
			} // selection remains empty for "Date" if there is no Date value or if the columnNames
				 // list is empty
		}

		return selection;
	}

	public HashMap<String, String> getDefaultSelection(HashMap<String, String> settings) {
		int maxDataPoints = Integer.parseInt(ParameterService
				.getParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_DEFAULT_MAXIMUM));
		if (this.settings.getDataTable().getNumberOfRows() > maxDataPoints && maxDataPoints != -1) {
			return settings; // return empty settings to prevent blocking the GUI
		}

		settings.put(PlotterConfigurationSettings.AXIS_X, getNameListEntry(NUMERICAL, 0));
		settings.put(PlotterConfigurationSettings.AXIS_Y, getNameListEntry(NUMERICAL, 1));
		settings.put(PlotterConfigurationSettings.AXIS_Z, getNameListEntry(NUMERICAL, 2));
		settings.put(PlotterConfigurationSettings.BUBBLE_SIZE, getNameListEntry(NUMERICAL, 2));
		settings.put(PlotterConfigurationSettings.AXIS_PLOT_COLUMN, getNameListEntry(LABEL, 0));
		settings.put(PlotterConfigurationSettings.CLASS_COLUMN, getNameListEntry(LABEL, 0));

		String multipleNames = getNameListEntry(NUMERICAL, 1);  // selected axes for multiple
		// selection field
		for (int i = 2; i <= DEFAULT_SELECTION_NUMBER; i++) {
			if (i >= categoryToColumnListMap.get(NUMERICAL).size()) {
				break;
			}
			multipleNames = multipleNames.concat(", " + getNameListEntry(NUMERICAL, i));
		}
		settings.put(PlotterConfigurationSettings.AXIS_PLOT_COLUMNS, multipleNames);
		settings.put(PlotterConfigurationSettings.AGGREGATION, AGGREGATIOTN_DEFAULT_FUNCTION);
		settings.put(PlotterConfigurationSettings.GROUP_BY_COLUMN, getNameListEntry(NOMINAL, 0));
		settings.put(PlotterConfigurationSettings.POINT_COLOR, getNameListEntry(LABEL, 0));
		settings.put(PlotterConfigurationSettings.AXIS_HISTOGRAM, getNameListEntry(NUMERICAL, 0));
		settings.put(PlotterConfigurationSettings.DIMENSION, getNameListEntry(NUMERICAL, 0));
		settings.put(PlotterConfigurationSettings.INDEX_DIMENSION, getNameListEntry(DATE, 0));
		settings.put(PlotterConfigurationSettings.AXIS_STACK_COLUMN, getNameListEntry(LABEL, 0));

		// declare and overwrite special cases for certain renderer types here:
		if (this.settings.getAvailablePlotters().containsKey(PlotterConfigurationModel.LINES_PLOT)) {  // AttributeWeight
			// IOObject
			settings.remove(PlotterConfigurationSettings.AGGREGATION);
			settings.put(PlotterConfigurationSettings.AGGREGATION, SUM);
			settings.remove(PlotterConfigurationSettings.AXIS_PLOT_COLUMN);
			settings.put(PlotterConfigurationSettings.AXIS_PLOT_COLUMN, getNameListEntry(NUMERICAL, 0));

		}

		return settings;
	}

	public String getDefaultPlotter() {

		if (categoryToColumnListMap.get(DATE).size() != 0
				&& this.settings.getAvailablePlotters().containsKey(PlotterConfigurationModel.SERIES_PLOT)) {
			return PlotterConfigurationModel.SERIES_PLOT;
		}

		if (categoryToColumnListMap.get(NUMERICAL).size() < 2
				&& this.settings.getAvailablePlotters().containsKey(PlotterConfigurationModel.BAR_CHART)) { // no
			// numerical
			// values
			// present
			return PlotterConfigurationModel.BAR_CHART;
		}
		if (this.settings.getAvailablePlotters().containsKey(PlotterConfigurationModel.LINES_PLOT)) { // AttributeWeight
			// IOObject
			return PlotterConfigurationModel.LINES_PLOT;
		}

		return PlotterConfigurationModel.SCATTER_PLOT; // default case

	}

}
