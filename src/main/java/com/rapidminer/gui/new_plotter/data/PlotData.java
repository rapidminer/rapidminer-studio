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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableListener;
import com.rapidminer.datatable.DataTableView;
import com.rapidminer.datatable.FilteredDataTable;
import com.rapidminer.datatable.NominalSortingDataTableMapping;
import com.rapidminer.datatable.SortedDataTableView;
import com.rapidminer.datatable.ValueMappingDataTableView;
import com.rapidminer.gui.new_plotter.PlotConfigurationError;
import com.rapidminer.gui.new_plotter.configuration.DefaultDimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.DomainConfigManager;
import com.rapidminer.gui.new_plotter.configuration.DomainConfigManager.GroupingState;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.listener.PlotConfigurationListener;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent.DimensionConfigChangeType;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.RangeAxisConfigChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.ValueSourceChangeEvent;
import com.rapidminer.gui.new_plotter.utility.NumericalValueRange;
import com.rapidminer.gui.new_plotter.utility.ValueRange;


/**
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class PlotData implements DataTableListener, PlotConfigurationListener {

	private final transient DataTable originalDataTable;
	private final transient ValueMappingDataTableView valueMappingDataTable;
	private final transient FilteredDataTable filteredDataTableView;
	private final transient SortedDataTableView sortedDataTableView;
	private transient DataTable cachedSampledDataTable = null;

	private transient boolean dataTableIsValid = false;

	/**
	 * This map also contains the DimensionConfigData for both domain {@link DefaultDimensionConfig}
	 * s.
	 */
	private Map<Integer, DimensionConfigData> dimensionConfigDataMap = new HashMap<Integer, DimensionConfigData>();

	private Map<Integer, ValueSourceData> valueSourceDataMap = new HashMap<Integer, ValueSourceData>();
	private Map<Integer, RangeAxisData> rangeAxisDataMap = new HashMap<Integer, RangeAxisData>();

	private DomainConfigManagerData domainConfigManagerData;

	private PlotInstance plotInstance;

	private PlotConfigurationChangeEvent lastProcessedEvent = null;

	public PlotData(PlotInstance plotInstance, DataTable dataTable) {
		if (plotInstance == null) {
			throw new IllegalArgumentException("null not allowed for plotInstance");
		}
		this.plotInstance = plotInstance;
		plotInstance.setPlotData(this);
		PlotConfiguration plotConfiguration = plotInstance.getMasterPlotConfiguration();
		// if (plotConfiguration.getPrioritizedListenerCount() > 0) {
		// plotConfiguration.clearPrioritizedListeners();
		// }
		plotConfiguration.addPlotConfigurationListener(this, true);

		this.originalDataTable = dataTable;
		originalDataTable.addDataTableListener(this, true);

		valueMappingDataTable = new ValueMappingDataTableView(originalDataTable);
		for (int i = 0; i < valueMappingDataTable.getColumnNumber(); ++i) {
			if (valueMappingDataTable.isNominal(i)) {
				valueMappingDataTable.setMappingProvider(i, new NominalSortingDataTableMapping(valueMappingDataTable, i,
						true));
			}
		}

		// add filtered data table view to view stack
		filteredDataTableView = new FilteredDataTable(valueMappingDataTable);

		// add sorted data table view on view stack (without sort provider for now)
		sortedDataTableView = new SortedDataTableView(filteredDataTableView, null);
		sortedDataTableView.addDataTableListener(this, true);

		// init valueSourceDataMap
		for (ValueSource valueSource : plotConfiguration.getAllValueSources()) {
			ValueSourceData valueSourceData = new ValueSourceData(valueSource, plotInstance);
			valueSourceDataMap.put(valueSource.getId(), valueSourceData);
		}

		// init dimensionConfigDataMap
		for (DefaultDimensionConfig dimensionConfig : plotConfiguration.getDefaultDimensionConfigs().values()) {
			DimensionConfigData dimensionConfigData = new DimensionConfigData(plotInstance, dimensionConfig);
			dimensionConfigDataMap.put(dimensionConfig.getId(), dimensionConfigData);
		}
		DefaultDimensionConfig domainConfig;
		domainConfig = plotConfiguration.getDomainConfigManager().getDomainConfig(true);
		dimensionConfigDataMap.put(domainConfig.getId(), new DimensionConfigData(plotInstance, domainConfig));
		domainConfig = plotConfiguration.getDomainConfigManager().getDomainConfig(false);
		dimensionConfigDataMap.put(domainConfig.getId(), new DimensionConfigData(plotInstance, domainConfig));

		// init DomainConfigManagerData
		domainConfigManagerData = new DomainConfigManagerData(plotInstance);

		// init RangeAxisDataMap
		for (RangeAxisConfig rangeAxisConfig : plotConfiguration.getRangeAxisConfigs()) {
			RangeAxisData rangeAxisData = new RangeAxisData(rangeAxisConfig, plotInstance);
			rangeAxisDataMap.put(rangeAxisConfig.getId(), rangeAxisData);
		}

		clearCache();
	}

	/**
	 * Updates the filters of the filteredDataTableView so that the data is filtered by the user
	 * defined ranges (if applicable) of the {@link DimensionConfig}s.
	 */
	private void updateFilteredDataTable() {
		cachedSampledDataTable = null;
		List<ValueRange> dimensionRanges = new LinkedList<ValueRange>();
		for (PlotDimension dimension : PlotDimension.values()) {
			DimensionConfig dimConf = plotInstance.getCurrentPlotConfigurationClone().getDimensionConfig(dimension);
			if (dimConf != null) {
				if (dimConf.isUsingUserDefinedLowerBound() || dimConf.isUsingUserDefinedUpperBound()) {
					ValueRange dimensionRange = dimConf.getUserDefinedRangeClone(filteredDataTableView.getParentTable());
					if (dimensionRange instanceof NumericalValueRange) {
						NumericalValueRange numericalDimensionRange = (NumericalValueRange) dimensionRange;
						if (!dimConf.isUsingUserDefinedLowerBound()) {
							numericalDimensionRange.setLowerBound(Double.NEGATIVE_INFINITY);
						}
						if (!dimConf.isUsingUserDefinedUpperBound()) {
							numericalDimensionRange.setUpperBound(Double.POSITIVE_INFINITY);
						}
					}
					if (dimensionRange != null) {
						dimensionRanges.add(dimensionRange);
					}
				}
			}
		}

		filteredDataTableView.replaceConditions(dimensionRanges);

		dataTableIsValid = true;
	}

	/**
	 * This method can be very slow. It updates the filtered data table if the current data table is
	 * not valid. CAUTION: DONT use this method in the event dispatcher thread.
	 */
	public SortedDataTableView getDataTable() {
		if (!dataTableIsValid) {
			updateFilteredDataTable();
		}
		return sortedDataTableView;
	}

	/**
	 * Returns the data table. If sampled is true, then a sampled data table is returned, which
	 * contains at most as many rows as the rapidminer property rapidminer.gui.plotter.rows.maximum
	 * suggests.
	 * 
	 * This method can be very slow. It updates the filtered data table if the current data table is
	 * not valid. CAUTION: DONT use this method in the event dispatcher thread.
	 */
	public DataTable getDataTable(boolean sampled) {
		SortedDataTableView currentDataTable = getDataTable();
		if (!sampled) {
			return currentDataTable;
		} else {
			if (cachedSampledDataTable == null) {
				if (currentDataTable == null) {
					return null;
				}
				int maxRowCount = PlotConfiguration.getMaxAllowedValueCount();
				if (currentDataTable.getRowNumber() <= maxRowCount) {
					cachedSampledDataTable = currentDataTable;
				} else {
					cachedSampledDataTable = currentDataTable.sample(maxRowCount);
				}
			}
			return cachedSampledDataTable;
		}
	}

	/**
	 * @return the sorted {@link DataTableView} without checking if it has to be updated. CAUTIO:
	 *         this may return an invalid datatable! Use only to register as listener or such stuff.
	 */
	public SortedDataTableView getSortedDataTableWithoutImplicitUpdate() {
		return sortedDataTableView;
	}

	/**
	 * @return the valueMappingDataTable
	 */
	public ValueMappingDataTableView getValueMappingDataTable() {
		return valueMappingDataTable;
	}

	/**
	 * @return the originalDataTable
	 */
	public DataTable getOriginalDataTable() {
		return originalDataTable;
	}

	private void clearCache() {
		for (ValueSourceData valueSourceData : getValueSourcesData()) {
			valueSourceData.clearCache();
		}
		for (DimensionConfigData dimensionConfigData : dimensionConfigDataMap.values()) {
			dimensionConfigData.clearCache();
		}
		dataTableIsValid = false;
		cachedSampledDataTable = null;
	}

	@Override
	public void dataTableUpdated(DataTable source) {

		// SortedDataTableView sortedDataTable = (SortedDataTableView) source;
		// this will be called when sorting has changed
		// DimensionConfigChangeEvent change = new DimensionConfigChangeEvent(domainConfigManager,
		// PlotDimension.DOMAIN,
		// sortedDataTable.getSortProvider());
		// informValueSourcesAboutDimensionChange(change);
		// firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this, change));
		if (source == originalDataTable) {
			clearCache();
			// TODO fire event
		} else if (source == sortedDataTableView) {
			cachedSampledDataTable = null;
		}
	}

	public DimensionConfigData getDimensionConfigData(DefaultDimensionConfig dimensionConfig) {
		if (dimensionConfig != null) {
			int id = dimensionConfig.getId();
			return dimensionConfigDataMap.get(id);
		}
		return null;
	}

	public ValueSourceData getValueSourceData(ValueSource valueSource) {
		if (valueSource != null) {
			int id = valueSource.getId();
			return valueSourceDataMap.get(id);
		}
		return null;
	}

	public RangeAxisData getRangeAxisData(RangeAxisConfig rangeAxisConfig) {
		if (rangeAxisConfig != null) {
			int id = rangeAxisConfig.getId();
			return rangeAxisDataMap.get(id);
		}
		return null;
	}

	public DomainConfigManagerData getDomainConfigManagerData() {
		return domainConfigManagerData;
	}

	@Override
	public synchronized boolean plotConfigurationChanged(PlotConfigurationChangeEvent change) {
		if (change == null || change == lastProcessedEvent) {
			return true;
		}
		lastProcessedEvent = change;

		PlotConfiguration currentPlotConfig = plotInstance.getCurrentPlotConfigurationClone();  // get
																								// current
																								// plot
																								// config

		// prepare temp variables
		int id = -1;
		DimensionConfig currentDimensionConfig = null;
		RangeAxisConfig currentRangeAxis = null;

		DimensionConfig changeDimensionConfig = change.getDimensionConfig();
		if (changeDimensionConfig != null) {
			// if event is a dimension config add/remove event, get current dimension config
			// (may be null if meta event is processed and it has been deleted afterwards)
			id = changeDimensionConfig.getId();
			currentDimensionConfig = currentPlotConfig.getDefaultDimensionConfigById(id);
		}

		RangeAxisConfig changeRangeAxis = change.getRangeAxisConfig();
		if (changeRangeAxis != null) {
			// if event is a range axis config add/remove event, get current range axis config
			// (may be null if meta event is processed and it has been deleted afterwards)
			id = changeRangeAxis.getId();
			currentRangeAxis = currentPlotConfig.getRangeAxisConfigById(id);
		}

		switch (change.getType()) {
			case TRIGGER_REPLOT:
				clearCache();
				break;
			case AXES_FONT:
				break;
			case AXIS_LINE_COLOR:
				break;
			case AXIS_LINE_WIDTH:
				break;
			case FRAME_BACKGROUND_COLOR:
				break;
			case CHART_TITLE:
				break;
			case COLOR_SCHEME:
				break;
			case DATA_TABLE_EXCHANGED:
				break;
			case DIMENSION_CONFIG_ADDED:
				// if current plot configuration still contains item..
				if (currentDimensionConfig != null && id != -1) {
					// add new dimension config data to map
					dimensionConfigDataMap.put(id, new DimensionConfigData(plotInstance,
							(DefaultDimensionConfig) currentDimensionConfig));
					clearCache();
				}
				break;
			case DIMENSION_CONFIG_CHANGED:
				dimensionConfigChanged(change.getDimensionChange());
				break;
			case DIMENSION_CONFIG_REMOVED:
				dimensionConfigDataMap.remove(changeDimensionConfig.getId());  // remove dimension
																				// config data from
																				// map
				clearCache();
				break;
			case LEGEND_CHANGED:
				break;
			case PLOT_BACKGROUND_COLOR:
				break;
			case PLOT_ORIENTATION:
				break;
			case RANGE_AXIS_CONFIG_ADDED:
				// if current plot configuration still contains item..
				if (currentRangeAxis != null && id != -1) {
					// add new range axis data to map
					rangeAxisDataMap.put(id, new RangeAxisData(currentRangeAxis, plotInstance));
					for (ValueSource valueSource : currentRangeAxis.getValueSources()) {  // also add
																							// containing
																							// value
																							// sources
																							// data
																							// to
																							// map
						valueSourceDataMap.put(valueSource.getId(), new ValueSourceData(valueSource, plotInstance));
					}
				}
				break;
			case RANGE_AXIS_CONFIG_CHANGED:
				RangeAxisConfigChangeEvent rangeAxisConfigChange = change.getRangeAxisConfigChange();
				rangeAxisConfigChanged(rangeAxisConfigChange);
				break;
			case RANGE_AXIS_CONFIG_MOVED:
				break;
			case RANGE_AXIS_CONFIG_REMOVED:
				RangeAxisConfig rangeAxis = change.getRangeAxisConfig();
				rangeAxisDataMap.remove(rangeAxis.getId());  					// remove range axis config from map
				for (ValueSource valueSource : rangeAxis.getValueSources()) {  	// also remove all
																				// containing value
																				// sources from data
																				// map
					valueSourceDataMap.remove(valueSource.getId());
				}
				clearCache();
				break;
			case LINK_AND_BRUSH_SELECTION:
				break;
			case META_CHANGE:
				for (PlotConfigurationChangeEvent e : change.getPlotConfigChangeEvents()) {
					plotConfigurationChanged(e);
				}
				break;
		}

		return true;
	}

	private void rangeAxisConfigChanged(RangeAxisConfigChangeEvent rangeAxisConfigChange) {

		PlotConfiguration currentPlotConfig = plotInstance.getCurrentPlotConfigurationClone();  // get
																								// current
																								// plot
																								// config
		int id = rangeAxisConfigChange.getSource().getId();  									// fetch id
		RangeAxisConfig currentRangeAxisConfig = currentPlotConfig.getRangeAxisConfigById(id);  // look
																								// up
																								// range
																								// axis
																								// config

		if (currentRangeAxisConfig == null) {
			// if current range axis config is null it has been deleted afterwards in a meta change
			// event
			return;
		}

		// inform range axis data
		RangeAxisData rangeAxisData = getRangeAxisData(currentRangeAxisConfig);
		rangeAxisData.rangeAxisConfigChanged(rangeAxisConfigChange);

		// and also process event here
		ValueSource changeValueSource = rangeAxisConfigChange.getValueSource();
		ValueSource currentValueSource = null;
		if (changeValueSource != null) {
			id = changeValueSource.getId(); 										// fetch id from value source add/remove event
			currentValueSource = currentRangeAxisConfig.getValueSourceById(id);		// look up current
																				// value source
		}
		// else {
		// return; // nothing to be done
		// }

		switch (rangeAxisConfigChange.getType()) {
			case VALUE_SOURCE_ADDED:
				if (currentValueSource != null) {
					valueSourceDataMap
							.put(currentValueSource.getId(), new ValueSourceData(currentValueSource, plotInstance));
					clearCache();
				} else {
					// if current value source is null it has been deleted afterwards in a meta
					// change event
					return; // nothing to be done
				}
				break;
			case VALUE_SOURCE_CHANGED:
				ValueSourceChangeEvent valueSourceChange = rangeAxisConfigChange.getValueSourceChange();
				changeValueSource = valueSourceChange.getSource();						// get source
				id = changeValueSource.getId();											// fetch id from changed value source
				currentValueSource = currentRangeAxisConfig.getValueSourceById(id);		// look up
																					// current value
																					// source

				if (currentValueSource != null) {
					getValueSourceData(currentValueSource).valueSourceChanged(valueSourceChange, currentValueSource);
				} else {
					// if current value source is null it has been deleted afterwards in a meta
					// change event
					return; // nothing to be done
				}
				break;
			case VALUE_SOURCE_REMOVED:
				valueSourceDataMap.remove(changeValueSource.getId());
				clearCache();
				break;
			default:
		}
	}

	private void dimensionConfigChanged(DimensionConfigChangeEvent dimensionChange) {
		PlotConfiguration currentPlotConfig = plotInstance.getCurrentPlotConfigurationClone(); 	// get
																								 	// current
																								 	// plot
																								 	// configuration

		// if domain config has changed
		if (dimensionChange.getDimension() == PlotDimension.DOMAIN) {
			getDomainConfigManagerData().dimensionConfigChanged(dimensionChange);  // inform domain
																					  // config data

			// also inform both domain dimension configs
			DomainConfigManager domainConfigManager = currentPlotConfig.getDomainConfigManager();
			getDimensionConfigData(domainConfigManager.getDomainConfig(false)).dimensionConfigChanged(dimensionChange);
			getDimensionConfigData(domainConfigManager.getDomainConfig(true)).dimensionConfigChanged(dimensionChange);

		} else {
			// inform default dimension config data about change

			int id = dimensionChange.getSource().getId();  											// fetch id of changed dimension
			DimensionConfig currentDimensionConfig = currentPlotConfig.getDefaultDimensionConfigById(id); 	// look
																											// up
																											// dimension
																											// config

			// if dimension config is still present, inform data about changes
			if (currentDimensionConfig != null) {
				DimensionConfigData dimData = getDimensionConfigData((DefaultDimensionConfig) currentDimensionConfig);
				dimData.dimensionConfigChanged(dimensionChange);
			} else {
				// if current dimension config is null it has been deleted afterwards in a meta
				// change event
				return; // do nothing and return
			}
		}

		// and process event here too
		if (dimensionChange.getType() == DimensionConfigChangeType.RANGE) {
			clearCache();
		}
	}

	public List<PlotConfigurationError> getErrors() {
		List<PlotConfigurationError> errors = new LinkedList<PlotConfigurationError>();

		for (ValueSourceData valueSourceData : getValueSourcesData()) {
			errors.addAll(valueSourceData.getErrors());
		}

		for (DimensionConfigData data : dimensionConfigDataMap.values()) {
			if (data.getDimensionConfig().getDimension() == PlotDimension.DOMAIN) {
				continue;
			} else {
				List<PlotConfigurationError> error = data.getErrors();
				errors.addAll(error);
			}
		}

		// check domain dimension for errors depending on grouping state
		DomainConfigManager domainConfigManager = plotInstance.getCurrentPlotConfigurationClone().getDomainConfigManager();
		GroupingState groupingState = domainConfigManager.getGroupingState();
		switch (groupingState) {
			case GROUPED:
				errors.addAll(getDimensionConfigData(domainConfigManager.getDomainConfig(true)).getErrors());
				break;
			case PARTIALLY_GROUPED:
				errors.addAll(getDimensionConfigData(domainConfigManager.getDomainConfig(true)).getErrors());
				errors.addAll(getDimensionConfigData(domainConfigManager.getDomainConfig(false)).getErrors());
				break;
			case UNGROUPED:
				errors.addAll(getDimensionConfigData(domainConfigManager.getDomainConfig(false)).getErrors());
				break;

		}

		for (RangeAxisData data : rangeAxisDataMap.values()) {
			errors.addAll(data.getErrors());
		}

		errors.addAll(domainConfigManagerData.getErrors());

		return errors;
	}

	public List<PlotConfigurationError> getWarnings() {
		List<PlotConfigurationError> warnings = new LinkedList<PlotConfigurationError>();
		for (ValueSourceData valueSourceData : getValueSourcesData()) {
			warnings.addAll(valueSourceData.getErrors());
		}
		boolean xAddedWarning = false;
		for (DimensionConfigData data : dimensionConfigDataMap.values()) {
			List<PlotConfigurationError> warning = data.getWarnings();
			if (data.getDimensionConfig().getDimension() == PlotDimension.DOMAIN) {
				if (!xAddedWarning) {
					xAddedWarning = warning.size() > 0;
					warnings.addAll(warning);
				}
			} else {
				warnings.addAll(warning);
			}
		}
		for (RangeAxisData data : rangeAxisDataMap.values()) {
			warnings.addAll(data.getWarnings());
		}

		warnings.addAll(domainConfigManagerData.getWarnings());
		return warnings;
	}

	/**
	 * @return
	 */
	private Collection<ValueSourceData> getValueSourcesData() {
		return valueSourceDataMap.values();
	}

	public boolean isValid() {
		return getErrors().isEmpty();
	}

	/**
	 * @return
	 */
	public boolean isDataTableValid() {
		return dataTableIsValid;
	}
}
