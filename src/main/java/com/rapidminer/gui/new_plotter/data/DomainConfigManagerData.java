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
import com.rapidminer.datatable.DataTableNaturalSortProvider;
import com.rapidminer.datatable.SortedDataTableView;
import com.rapidminer.gui.new_plotter.PlotConfigurationError;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn;
import com.rapidminer.gui.new_plotter.configuration.DefaultDimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.DomainConfigManager;
import com.rapidminer.gui.new_plotter.listener.DimensionConfigListener;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent;
import com.rapidminer.gui.new_plotter.utility.NumericalValueRange;

import java.util.LinkedList;
import java.util.List;


/**
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class DomainConfigManagerData implements DimensionConfigListener {

	private PlotInstance plotInstance;
	private DimensionConfigChangeEvent lastProcessedEvent = null;

	public DomainConfigManagerData(PlotInstance plotInstance) {
		if (plotInstance == null) {
			throw new IllegalArgumentException("null not allowed");
		}

		this.plotInstance = plotInstance;
		applySorting();
	}

	public void applySorting() {
		SortedDataTableView dataTable = plotInstance.getPlotData().getSortedDataTableWithoutImplicitUpdate();
		DomainConfigManager domainConfigManager = plotInstance.getCurrentPlotConfigurationClone().getDomainConfigManager();
		switch (domainConfigManager.getSortingMode()) {
			case ASCENDING:
				int columnIdx = DataTableColumn.getColumnIndex(dataTable, domainConfigManager.getDataTableColumn());
				dataTable.setSortProvider(new DataTableNaturalSortProvider(columnIdx, true));
				break;
			case NONE:
				dataTable.setSortProvider(null);
				break;
		}
	}

	@Override
	public void dimensionConfigChanged(DimensionConfigChangeEvent e) {
		if (e == null || e == lastProcessedEvent) {
			return;
		}
		lastProcessedEvent = e;

		switch (e.getType()) {
			case COLUMN:
			case SORTING:
				applySorting();
				break;
			default:
		}
	}

	public List<PlotConfigurationError> getErrors() {
		// TODO check all preconditions

		LinkedList<PlotConfigurationError> errorList = new LinkedList<PlotConfigurationError>();
		DataTable dataTable = plotInstance.getPlotData().getOriginalDataTable();
		DomainConfigManager domainConfigManager = plotInstance.getCurrentPlotConfigurationClone().getDomainConfigManager();
		if (!domainConfigManager.getDataTableColumn().isValidForDataTable(dataTable)) {
			PlotConfigurationError error = new PlotConfigurationError("undefined_dimension", PlotDimension.DOMAIN.getName());
			errorList.add(error);
		}

		return errorList;
	}

	public List<PlotConfigurationError> getWarnings() {
		// TODO implement me
		List<PlotConfigurationError> warnings = new LinkedList<PlotConfigurationError>();
		return warnings;
	}

	public NumericalValueRange getEffectiveRange() {
		DomainConfigManager domainConfigManager = plotInstance.getCurrentPlotConfigurationClone().getDomainConfigManager();
		DefaultDimensionConfig domainConfig = domainConfigManager.getDomainConfig(domainConfigManager.isGrouping());
		DimensionConfigData domainData = plotInstance.getPlotData().getDimensionConfigData(domainConfig);
		return domainData.getEffectiveRange();
	}
}
