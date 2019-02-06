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
package com.rapidminer.gui.new_plotter.configuration;

import com.rapidminer.gui.new_plotter.ChartConfigurationException;
import com.rapidminer.gui.new_plotter.PlotConfigurationError;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn.ValueType;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.LineFormat.LineStyle;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.FillStyle;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.IndicatorType;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.ItemShape;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.VisualizationType;
import com.rapidminer.gui.new_plotter.listener.AggregationWindowingListener;
import com.rapidminer.gui.new_plotter.listener.SeriesFormatListener;
import com.rapidminer.gui.new_plotter.listener.ValueSourceListener;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.SeriesFormatChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.ValueSourceChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.ValueSourceChangeEvent.ValueSourceChangeType;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.function.aggregation.AbstractAggregationFunction;
import com.rapidminer.tools.math.function.aggregation.AbstractAggregationFunction.AggregationFunctionType;
import com.rapidminer.tools.math.function.aggregation.AggregationFunction;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A source for actual plot values.
 * 
 * Each ValueSource holds a reference to a DimensionConfig which configures the domain for this
 * value source. Since all value sources share the same domain axis but hold individual domain
 * configurations, there are some constraints for these configurations. Because of that the contract
 * for the domain configuration is that all operations which change the value type or the categories
 * of the domain must be passed through the {@link DomainConfigManager} of the the
 * {@link PlotConfiguration} and may not be called directly on the object returned by
 * getDomainConfig().
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class ValueSource implements AggregationWindowingListener, SeriesFormatListener {

	public enum SeriesUsageType {
		MAIN_SERIES,		// The main series, used for plotting the values of a series
		INDICATOR_1,    // Errors of the main series. If INDICATOR_2 is not set,
		// symmetric error bars are generated based on INDICATOR_1.
		// Otherwise INDICATOR_1 defines the upper error. The series
		// must contain error values (like stdev or variance), not the
		// absolute position of the error bars in the coordinate system.
		INDICATOR_2,    // Lower errors of the main series. If unset, a symmetric error
		// from INDICATOR_1 is plotted.
	}

	private boolean useDomainGrouping;
	private SeriesFormat format;

	private Map<SeriesUsageType, DataTableColumn> dataTableColumnMap = new HashMap<SeriesUsageType, DataTableColumn>();

	private List<WeakReference<ValueSourceListener>> listeners = new LinkedList<WeakReference<ValueSourceListener>>();

	private Map<SeriesUsageType, AggregationFunction> aggregationFunctionMap = new HashMap<SeriesUsageType, AggregationFunction>();
	private Map<SeriesUsageType, AggregationFunctionType> aggregationFunctionTypeMap = new HashMap<SeriesUsageType, AggregationFunctionType>();
	private AggregationWindowing aggregationWindowing = new AggregationWindowing(0, 0, false);

	private boolean useRelativeUtilities = true;

	private String label;

	private boolean autoNaming;

	private final int Id;
	private DomainConfigManager domainConfigManager;

	/**
	 * Constructor for a default value source. Check if grouping is required by calling
	 * isGroupingRequired() at the destinated {@link RangeAxisConfig} parent.
	 */
	public ValueSource(PlotConfiguration plotConfiguration, DataTableColumn mainDataTableColumn,
			AggregationFunctionType aggregationFunctionType, boolean grouped) {
		this(plotConfiguration.getDomainConfigManager(), mainDataTableColumn, aggregationFunctionType, grouped,
				plotConfiguration.getNextId());
	}

	/**
	 * Private C'tor that is used for cloning.
	 */
	private ValueSource(DomainConfigManager domainConfigManager, DataTableColumn mainDataTableColumn,
			AggregationFunctionType aggregationFunctionType, boolean grouped, int Id) {
		this.domainConfigManager = domainConfigManager;
		this.Id = Id;

		this.dataTableColumnMap.put(SeriesUsageType.MAIN_SERIES, mainDataTableColumn);

		setAggregationFunction(SeriesUsageType.MAIN_SERIES, aggregationFunctionType);
		setAggregationFunction(SeriesUsageType.INDICATOR_1, AggregationFunctionType.standard_deviation);
		setAggregationFunction(SeriesUsageType.INDICATOR_2, AggregationFunctionType.standard_deviation);

		aggregationWindowing.addAggregationWindowingListener(this);

		format = new SeriesFormat();
		format.addChangeListener(this);

		setUseDomainGrouping(grouped);

		this.setAutoNaming(true);
	}

	public AggregationFunction getAggregationFunction(SeriesUsageType usageType) {
		return aggregationFunctionMap.get(usageType);
	}

	/**
	 * Returns the name of the {@link RangeAxisConfig}. Maybe <code>null</code>.
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Sets the name of the plot range axis
	 */
	public void setLabel(String label) {
		if (label == null ? label != this.label : !label.equals(this.label)) {
			this.label = label;
			fireValueSourceChanged(new ValueSourceChangeEvent(this, label));
		}
	}

	private void setAutoLabelIfEnabled() {
		if (isAutoNaming()) {
			setLabel(getAutoLabel());
		}
	}

	/**
	 * @return the autoName
	 */
	public boolean isAutoNaming() {
		return autoNaming;
	}

	public String getAutoLabel() {
		DataTableColumn dataTableColumn = getDataTableColumn(SeriesUsageType.MAIN_SERIES);
		if (dataTableColumn != null) {
			String label = dataTableColumn.getName();
			if (isUsingDomainGrouping()) {
				label = getAggregationFunctionType(SeriesUsageType.MAIN_SERIES) + "(" + label + ")";
			}
			return label;
		} else {
			return "-Empty-"; // TODO I18N
		}
	}

	/**
	 * @param autoName
	 *            the autoName to set
	 */
	public void setAutoNaming(boolean autoName) {
		if (autoName != this.autoNaming) {
			this.autoNaming = autoName;
			setAutoLabelIfEnabled();
			fireValueSourceChanged(new ValueSourceChangeEvent(this, ValueSourceChangeType.AUTO_NAMING, autoName));
		}
	}

	public void setDataTableColumn(SeriesUsageType seriesUsage, DataTableColumn dataTableColumn)
			throws ChartConfigurationException {
		DataTableColumn oldDataTableColumn = dataTableColumnMap.get(seriesUsage);

		if (dataTableColumn != null && !dataTableColumn.equals(oldDataTableColumn)) {

			dataTableColumnMap.put(seriesUsage, dataTableColumn);

			fireUsageTypeToColumnMapChanged(dataTableColumn, seriesUsage);

			if (seriesUsage == SeriesUsageType.MAIN_SERIES) {
				setAutoLabelIfEnabled();
			}

		} else if (dataTableColumn == null && dataTableColumn != oldDataTableColumn) {

			if (seriesUsage == SeriesUsageType.MAIN_SERIES) {
				throw new ChartConfigurationException("remove_main_domain_column");
			}

			dataTableColumnMap.remove(seriesUsage);

			fireUsageTypeToColumnMapChanged(dataTableColumn, seriesUsage);

		}

	}

	public void setAggregationFunction(SeriesUsageType seriesUsage, AggregationFunctionType functionType) {
		if (functionType == null) {
			aggregationFunctionMap.remove(seriesUsage);
			if (seriesUsage == SeriesUsageType.MAIN_SERIES) {
				setAutoLabelIfEnabled();
			}
			fireAggregationFunctionChanged(null, seriesUsage);
		} else {
			// return if aggregationFunctionName is equal to currently set name
			AggregationFunction currentFunction = aggregationFunctionMap.get(seriesUsage);
			String currentFunctionName = null;
			if (currentFunction != null) {
				currentFunctionName = currentFunction.getName();
			}
			if (functionType.toString().equals(currentFunctionName)) {
				return;
			}

			AggregationFunction aggregationFunction = null;
			try {
				aggregationFunction = AbstractAggregationFunction.createAggregationFunction(functionType.toString());
			} catch (InstantiationException e) {
				throw new RuntimeException("Unknown aggregation function type " + functionType);
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Unknown aggregation function type " + functionType);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Unknown aggregation function type " + functionType);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException("Unknown aggregation function type " + functionType);
			} catch (InvocationTargetException e) {
				throw new RuntimeException("Unknown aggregation function type " + functionType);
			}
			aggregationFunctionMap.put(seriesUsage, aggregationFunction);
			aggregationFunctionTypeMap.put(seriesUsage, functionType);

			if (seriesUsage == SeriesUsageType.MAIN_SERIES) {
				setAutoLabelIfEnabled();
			}

			fireAggregationFunctionChanged(aggregationFunction, seriesUsage);

		}
	}

	public boolean isNominal() {
		return getValueType() == ValueType.NOMINAL;
	}

	/**
	 * Returns the {@link ValueType} of the output values of this value source (which are not the
	 * necessarily the input values).
	 */
	public ValueType getValueType(SeriesUsageType usageType) {
		if (!getDefinedUsageTypes().contains(usageType)) {
			return ValueType.INVALID;
		}

		if (isUsingDomainGrouping()) {
			ValueType valueType = getDataTableColumn(usageType).getValueType();
			int rmInputValueType = ValueType.convertToRapidMinerOntology(valueType);
			int rmOutputValueType = getAggregationFunction(usageType).getValueTypeOfResult(rmInputValueType);
			ValueType valueTypeOfResult = ValueType.convertFromRapidMinerOntology(rmOutputValueType);
			return valueTypeOfResult;
		} else {
			DataTableColumn dataTableColumn = dataTableColumnMap.get(usageType);
			return dataTableColumn.getValueType();
		}
	}

	/**
	 * Returns the associated domain config's value type.
	 */
	public ValueType getDomainConfigValueType() {
		return getDomainConfig().getValueType();
	}

	public ValueType getValueType() {
		return getValueType(SeriesUsageType.MAIN_SERIES);
	}

	public boolean isDate() {
		return getValueType() == ValueType.DATE_TIME;
	}

	/**
	 * Returns true if the output values of this value source (not the necessarily the input values)
	 * are numerical, either because the input data itself is numerical, or the applied grouping and
	 * aggregation function results in numerical values.
	 */
	public boolean isNumerical() {
		return getValueType() == ValueType.NUMERICAL;
	}

	public AggregationFunctionType getAggregationFunctionType(SeriesUsageType usageType) {
		AggregationFunction aggregationFunction = aggregationFunctionMap.get(usageType);
		if (aggregationFunction != null) {
			return AggregationFunctionType.valueOf(aggregationFunction.getName());
		} else {
			return null;
		}
	}

	public Set<SeriesUsageType> getDefinedUsageTypes() {
		return dataTableColumnMap.keySet();
	}

	@Override
	public String toString() {
		if (label == null) {
			return I18N.getGUILabel("plotter.unnamed_value_label");
		}
		return getLabel();
	}

	// public boolean useFormatFromDimensionConfig(PlotDimension dimension) {
	// if (isUsingDomainGrouping()) {
	// return false;
	// }
	// if (plotConfiguration.getDimensionConfig(dimension) != null) {
	// return true;
	// }
	// return false;
	// }
	//
	// public boolean hasSeriesValueForDimension(PlotDimension dimension) {
	// DefaultDimensionConfig dimensionConfig = (DefaultDimensionConfig)
	// plotConfiguration.getDimensionConfig(dimension);
	// if (useSeriesFormatForDimension(dimension)) {
	// return true;
	// }
	// if (dimensionConfig != null && !isUsingDomainGrouping()) {
	// return false; // use value from DimensionConfig, e.g. different color for each item from
	// ColorProvider
	// }
	// if (dimensionConfig != null && dimensionConfig.isGrouping()) {
	// return true; // use value from DimensionConfig for this series
	// }
	//
	// throw new
	// RuntimeException("Should not happen, one of the above conditions should always be true.");
	// }

	public boolean useSeriesFormatForDimension(PlotConfiguration plotConfig, PlotDimension dimension) {
		DefaultDimensionConfig dimensionConfig = (DefaultDimensionConfig) plotConfig.getDimensionConfig(dimension);
		if (dimensionConfig == null) {
			return true; // use value from SeriesFormat
		}
		if (!dimensionConfig.isGrouping() && isUsingDomainGrouping()) {
			// aggregated, but not by given dimension
			// --> use value from SeriesFormat (not possible to use e.g. color from ColorProvider
			// for each item,
			// because each item is aggregated from possibly many items with possibly different
			// values for
			// the given dimension)
			return true;
		}
		return false;
	}

	public void addValueSourceListener(ValueSourceListener l) {
		listeners.add(new WeakReference<ValueSourceListener>(l));
	}

	public void removeValueSourceListener(ValueSourceListener l) {
		Iterator<WeakReference<ValueSourceListener>> it = listeners.iterator();
		while (it.hasNext()) {
			ValueSourceListener listener = it.next().get();
			if (listener == null || listener == l) {
				it.remove();
			}
		}
	}

	public SeriesFormat getSeriesFormat() {
		return format;
	}

	public void setSeriesFormat(SeriesFormat format) {
		if (format != this.format) {
			if (this.format != null) {
				format.removeChangeListener(this);
			}
			this.format = format;
			if (format != null) {
				format.addChangeListener(this);
			}
		}
	}

	private void fireValueSourceChanged(ValueSourceChangeEvent e) {
		Iterator<WeakReference<ValueSourceListener>> it = listeners.iterator();
		while (it.hasNext()) {
			ValueSourceListener l = it.next().get();
			if (l != null) {
				l.valueSourceChanged(e);
			} else {
				it.remove();
			}
		}
	}

	private void fireAggregationFunctionChanged(AggregationFunction function, SeriesUsageType seriesUsage) {
		if (function != null) {
			fireValueSourceChanged(new ValueSourceChangeEvent(this, AggregationFunctionType.valueOf(function.getName()),
					seriesUsage));
		}
	}

	private void fireAggregationWindowingChanged() {
		fireValueSourceChanged(new ValueSourceChangeEvent(this, aggregationWindowing));
	}

	private void fireUsageTypeToColumnMapChanged(DataTableColumn column, SeriesUsageType seriesUsage) {
		fireValueSourceChanged(new ValueSourceChangeEvent(this, column, seriesUsage));
	}

	public void removeAllListeners() {
		listeners.clear();
	}

	public DefaultDimensionConfig getDomainConfig() {
		return domainConfigManager.getDomainConfig(useDomainGrouping);
	}

	@Override
	public void seriesFormatChanged(SeriesFormatChangeEvent e) {
		fireValueSourceChanged(new ValueSourceChangeEvent(this, e));
	}

	/**
	 * Returns true if this ValueSource delivers aggregated values of some kind.
	 */
	public boolean isUsingDomainGrouping() {
		return useDomainGrouping;
	}

	/**
	 * Defines if this ValueSource uses the domain grouping provided by the
	 * {@link DomainConfigManager}.
	 */
	public void setUseDomainGrouping(boolean useDomainGrouping) {
		if (this.useDomainGrouping != useDomainGrouping) {
			this.useDomainGrouping = useDomainGrouping;
			setAutoLabelIfEnabled();
			fireValueSourceChanged(new ValueSourceChangeEvent(this, ValueSourceChangeType.USES_GROUPING, useDomainGrouping));
		}
	}

	public void dimensionConfigChanged(DimensionConfigChangeEvent e) {
		switch (e.getType()) {
			case RESET:
			case SORTING:
			case COLUMN:
			case GROUPING_CHANGED:
			case RANGE:
				fireValueSourceChanged(new ValueSourceChangeEvent(this));
				break;
			default:
		}
	}

	/**
	 * Returns the {@link AggregationWindowing} for the domain dimension. Never returns null.
	 */
	public AggregationWindowing getAggregationWindowing() {
		return aggregationWindowing;
	}

	public void setAggregationWindowing(AggregationWindowing aggregationWindowing) {
		if (aggregationWindowing == null) {
			throw new IllegalArgumentException("null not allowed for aggregationWindowing");
		}
		if (this.aggregationWindowing != aggregationWindowing) {
			this.aggregationWindowing.removeAggregationWindowingListener(this);
			this.aggregationWindowing = aggregationWindowing;
			aggregationWindowing.addAggregationWindowingListener(this);
		}
	}

	@Override
	public void aggregationWindowingChanged(AggregationWindowing source) {
		fireAggregationWindowingChanged();
	}

	public boolean isUsingRelativeIndicator() {
		return useRelativeUtilities;
	}

	public void setUseRelativeUtilities(boolean relativeUtilities) {
		if (relativeUtilities != this.useRelativeUtilities) {
			this.useRelativeUtilities = relativeUtilities;
			fireValueSourceChanged(new ValueSourceChangeEvent(this, ValueSourceChangeType.USE_RELATIVE_UTILITIES,
					relativeUtilities));
		}
	}

	@Override
	public ValueSource clone() {
		ValueSource clone = new ValueSource(domainConfigManager, null, null, useDomainGrouping, Id);
		clone.setLabel(label);
		clone.setAutoNaming(autoNaming);
		clone.setSeriesFormat(format.clone());
		clone.useRelativeUtilities = useRelativeUtilities;
		clone.setAggregationWindowing(aggregationWindowing.clone());
		for (Map.Entry<SeriesUsageType, AggregationFunctionType> entry : aggregationFunctionTypeMap.entrySet()) {
			clone.setAggregationFunction(entry.getKey(), entry.getValue());
		}
		for (Map.Entry<SeriesUsageType, DataTableColumn> entry : dataTableColumnMap.entrySet()) {
			try {
				clone.setDataTableColumn(entry.getKey(), entry.getValue().clone());
			} catch (ChartConfigurationException e) {
				throw new RuntimeException("this should not happen");
			}
		}
		return clone;
	}

	/**
	 * Returns the {@link DataTableColumn} associated with the specified {@link SeriesUsageType}.
	 */
	public DataTableColumn getDataTableColumn(SeriesUsageType seriesUsage) {
		return dataTableColumnMap.get(seriesUsage);
	}

	public boolean doesAggregationFunctionSupportValueType(AggregationFunction function, ValueType valueType) {
		switch (valueType) {
			case DATE_TIME:
				return function.supportsValueType(Ontology.DATE_TIME);
			case NOMINAL:
				return function.supportsValueType(Ontology.NOMINAL);
			case NUMERICAL:
				return function.supportsValueType(Ontology.NUMERICAL);
			case INVALID:
			case UNKNOWN:
				return false;
			default:
				return false;
		}
	}

	public List<PlotConfigurationError> getErrors() {
		List<PlotConfigurationError> errors = new LinkedList<PlotConfigurationError>();

		if (useDomainGrouping) {
			for (SeriesUsageType usageType : getDefinedUsageTypes()) {
				ValueType valueType = getDataTableColumn(usageType).getValueType();
				AggregationFunction aggregationFunction = getAggregationFunction(usageType);
				if (!doesAggregationFunctionSupportValueType(aggregationFunction, valueType)) {
					errors.add(new PlotConfigurationError("value_type_not_supported_by_aggregation", this.toString(),
							usageType, valueType, aggregationFunction.getName()));
				}
			}
		}

		// check that all series usage types have the same value type
		ValueType valueType = getValueType();
		if (format.getUtilityUsage() != IndicatorType.NONE && getDefinedUsageTypes().contains(SeriesUsageType.INDICATOR_1)) {
			ValueType seriesValueType = getValueType(SeriesUsageType.INDICATOR_1);
			if (seriesValueType != valueType) {
				errors.add(new PlotConfigurationError("incompatible_utility_value_type", this.toString(),
						SeriesUsageType.INDICATOR_1, valueType, seriesValueType));
			}
		}
		if (format.getUtilityUsage() != IndicatorType.NONE && getDefinedUsageTypes().contains(SeriesUsageType.INDICATOR_2)) {
			if (format.getUtilityUsage() != IndicatorType.DIFFERENCE) {
				ValueType seriesValueType = getValueType(SeriesUsageType.INDICATOR_2);
				if (seriesValueType != valueType) {
					errors.add(new PlotConfigurationError("incompatible_utility_value_type", this.toString(),
							SeriesUsageType.INDICATOR_2, valueType, seriesValueType));
				}
			}
		}

		return errors;
	}

	public List<PlotConfigurationError> getWarnings() {
		List<PlotConfigurationError> warnings = new LinkedList<PlotConfigurationError>();

		if (format.getSeriesType() == VisualizationType.LINES_AND_SHAPES) {
			if (format.getItemShape() == ItemShape.NONE && format.getLineStyle() == LineStyle.NONE
					&& format.getUtilityUsage() == IndicatorType.NONE) {
				warnings.add(new PlotConfigurationError("invisible_format", this.toString()));
			} else if (format.getItemShape() == ItemShape.NONE && format.getAreaFillStyle() == FillStyle.NONE) {
				warnings.add(new PlotConfigurationError("invisible_format", this.toString()));
			}
		}

		if (format.getUtilityUsage() == IndicatorType.NONE) {
			if (getDataTableColumn(SeriesUsageType.INDICATOR_1) != null
					|| getDataTableColumn(SeriesUsageType.INDICATOR_2) != null) {
				warnings.add(new PlotConfigurationError("unused_utility_series", this.toString(), IndicatorType.NONE
						.getName()));
			}
		} else if (format.getUtilityUsage() == IndicatorType.DIFFERENCE) {
			if (getDataTableColumn(SeriesUsageType.INDICATOR_2) != null) {
				warnings.add(new PlotConfigurationError("unused_secondary_utility_series", this.toString(),
						IndicatorType.DIFFERENCE.getName()));
			}
		}

		return warnings;
	}

	/**
	 * Returns true iff this {@link ValueSource} suggests to sample the data if it is large. Large
	 * is defined as being larger than the RapidMiner property rapidminer.gui.plotter.rows.maximum.<br>
	 * 
	 * Currently this function returns true for non-aggregated scatter plots and false otherwise.
	 */
	public boolean isSamplingSuggested() {
		if (!isUsingDomainGrouping()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return Id;
	}

	/**
	 * Exchanges the domainConfigManager of this ValueSource. Should only be called directly after
	 * cloning.
	 */
	void setDomainConfigManager(DomainConfigManager domainConfigManager) {
		this.domainConfigManager = domainConfigManager;
	}
}
