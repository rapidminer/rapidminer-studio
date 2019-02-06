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
package com.rapidminer.gui.new_plotter.integration;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn.ValueType;
import com.rapidminer.gui.new_plotter.configuration.DefaultDimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.configuration.ValueSource.SeriesUsageType;
import com.rapidminer.gui.new_plotter.data.DataTableColumnIndex;
import com.rapidminer.gui.new_plotter.gui.AbstractConfigurationPanel.DatasetTransformationType;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.ProcessingStep;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;


/**
 * This class holds informations about plotter settings in the processing history since the
 * RapidMiner startup. They might be used for pre-initilizing the plotter with settings from the
 * past processing history.
 * 
 * @author Sebastian Land, Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public final class PlotConfigurationHistory {

	private static final HashMap<ProcessingStep, Map<DatasetTransformationType, PlotConfiguration>> settingsHistory = new HashMap<ProcessingStep, Map<DatasetTransformationType, PlotConfiguration>>();

	/**
	 * Private ctor - static only class.
	 */
	private PlotConfigurationHistory() {};

	public static Map<DatasetTransformationType, PlotConfiguration> getPlotConfigurationMap(IOObject object,
			DataTable dataTable) {
		List<ProcessingStep> steps = object.getProcessingHistory();
		ListIterator<ProcessingStep> iterator = steps.listIterator(steps.size());
		Map<DatasetTransformationType, PlotConfiguration> plotConfigMap = new HashMap<DatasetTransformationType, PlotConfiguration>();
		boolean isFirst = false;
		while (iterator.hasPrevious()) {
			ProcessingStep step = iterator.previous();
			if (!isFirst) {
				plotConfigMap = settingsHistory.get(step);
				if (plotConfigMap != null) {

					PlotConfiguration plotConfiguration = plotConfigMap.get(DatasetTransformationType.ORIGINAL);
					if (plotConfiguration != null) {
						// Clone and register for last process step
						if (isPlotConfigurationCompatible(plotConfiguration, dataTable)) {
							plotConfiguration = plotConfiguration.clone();
							settingsHistory.put(steps.get(steps.size() - 1), plotConfigMap);
							return plotConfigMap;
						} else {
							// plotConfiguration is not compatible -> crate a new one further down
							plotConfigMap = null;
							break;
						}
					}
				}
			} else {
				isFirst = false;
			}
		}

		// if we didn't find anything: Create new settings and add to history
		if (plotConfigMap == null || plotConfigMap.isEmpty()) {
			plotConfigMap = new HashMap<DatasetTransformationType, PlotConfiguration>();

			PlotConfiguration plotConfiguration = new PlotConfiguration(new DataTableColumn(dataTable, -1));
			plotConfigMap.put(DatasetTransformationType.ORIGINAL, plotConfiguration);

			PlotConfiguration metaPlotConfiguration = new PlotConfiguration(new DataTableColumn(null, ValueType.INVALID));
			plotConfigMap.put(DatasetTransformationType.DE_PIVOTED, metaPlotConfiguration);

			if (!steps.isEmpty()) {
				settingsHistory.put(steps.get(steps.size() - 1), plotConfigMap);
			}
		}
		return plotConfigMap;
	}

	/**
	 * Returns <code>true</code> iff all columns used in <code>plotConfiguration</code> are also
	 * present in <code>dataTable</code> and have compatible value types.
	 */
	private static boolean isPlotConfigurationCompatible(PlotConfiguration plotConfiguration, DataTable dataTable) {
		// check if columns from valueSources are present and compatible in dataTable
		for (ValueSource valueSource : plotConfiguration.getAllValueSources()) {
			for (SeriesUsageType usageType : valueSource.getDefinedUsageTypes()) {
				DataTableColumn column = valueSource.getDataTableColumn(usageType);
				if (column != null) {
					DataTableColumnIndex columnIdx = new DataTableColumnIndex(column, dataTable);
					if (columnIdx.getIndex() < 0) {
						return false;
					}
				}
			}
		}

		// check if columns from defaultDimensionConfigs are present and compatible in dataTable
		for (DefaultDimensionConfig defaultDimensionConfig : plotConfiguration.getDefaultDimensionConfigs().values()) {
			DataTableColumn column = defaultDimensionConfig.getDataTableColumn();
			DataTableColumnIndex columnIdx = new DataTableColumnIndex(column, dataTable);
			if (columnIdx.getIndex() < 0) {
				return false;
			}
		}

		// check if columns from domainConfigManager is present and compatible in dataTable
		DataTableColumn column = plotConfiguration.getDomainConfigManager().getDataTableColumn();
		DataTableColumnIndex columnIdx = new DataTableColumnIndex(column, dataTable);
		if (columnIdx.getIndex() < 0) {
			return false;
		}

		return true;
	}
}
