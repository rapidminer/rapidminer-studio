/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.new_plotter.templates;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.new_plotter.ChartConfigurationException;
import com.rapidminer.gui.new_plotter.configuration.AxisParallelLineConfiguration;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn;
import com.rapidminer.gui.new_plotter.configuration.DefaultDimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.DistinctValueGrouping;
import com.rapidminer.gui.new_plotter.configuration.EquidistantFixedBinCountBinning;
import com.rapidminer.gui.new_plotter.configuration.LegendConfiguration.LegendPosition;
import com.rapidminer.gui.new_plotter.configuration.LineFormat.LineStyle;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.VisualizationType;
import com.rapidminer.gui.new_plotter.configuration.ValueGrouping.GroupingType;
import com.rapidminer.gui.new_plotter.configuration.ValueGrouping.ValueGroupingFactory;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.templates.gui.HistogrammTemplatePanel;
import com.rapidminer.gui.new_plotter.templates.style.ColorRGB;
import com.rapidminer.gui.new_plotter.templates.style.PlotterStyleProvider;
import com.rapidminer.gui.new_plotter.utility.DataTransformation;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.math.function.aggregation.AbstractAggregationFunction.AggregationFunctionType;

import java.awt.Color;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * The template for a histogram plot.
 * 
 * @author Marco Boeck
 * 
 */
public class HistogramTemplate extends PlotterTemplate {

	private static final String PLOT_NAME_ELEMENT = "plotName";

	private static final String PLOT_NAMES_ELEMENT = "plotNames";

	private static final String OPAQUE_ELEMENT = "opaque";

	private static final String BINS_ELEMENT = "bins";

	private static final String USE_ABSOLUTE_VALUES_ELEMENT = "useAbsoluteValues";

	private static final String Y_AXIS_LOGARITHMIC_ELEMENT = "yAxisLogarithmic";

	/** the current {@link DataTable} backup */
	private DataTable currentDataTableBackup;

	/** the original {@link DataTable} */
	private DataTable modifiedDataTable;

	/** the current {@link RangeAxisConfig}s */
	private List<RangeAxisConfig> currentRangeAxisConfigsList;

	/** the names of the plots to show */
	private Object[] plotNames;

	/** the number of bins */
	private int bins;

	/** the opaque value */
	private int opaque;

	/** determines if absolute values are used */
	private boolean useAbsoluteValues;

	/** determines if the range (Y) axis should be logarithmic */
	private boolean yAxisLogarithmic;

	/**
	 * Creates a new {@link HistogramTemplate}. This template allows easy configuration of the
	 * histogram chart for the plotter.
	 */
	public HistogramTemplate() {
		currentRangeAxisConfigsList = new LinkedList<RangeAxisConfig>();

		bins = 40;
		opaque = 255;

		useAbsoluteValues = false;
		yAxisLogarithmic = false;

		plotNames = new Object[0];

		guiPanel = new HistogrammTemplatePanel(this);
	}

	@Override
	public String getChartType() {
		return HistogramTemplate.getI18NName();
	}

	/**
	 * Sets the number of bins.
	 * 
	 * @param bins
	 *            must be >0 and <=100
	 */
	public void setBins(int bins) {
		if (bins <= 0 || bins > 100) {
			throw new IllegalArgumentException("bins must be > 0 and <= 100!");
		}

		this.bins = bins;

		updatePlotConfiguration();
		setChanged();
		notifyObservers();
	}

	/**
	 * Returns the number of bins.
	 * 
	 * @return
	 */
	public int getBins() {
		return bins;
	}

	/**
	 * Sets the opacity.
	 * 
	 * @param opaque
	 *            must be >=0 and <=255
	 */
	public void setOpaque(int opaque) {
		if (opaque < 0 || opaque > 255) {
			throw new IllegalArgumentException("opaque must be >= 0 and <= 255!");
		}

		this.opaque = opaque;

		updatePlotConfiguration();
		setChanged();
		notifyObservers();
	}

	/**
	 * Returns the opacity.
	 * 
	 * @return
	 */
	public int getOpacity() {
		return opaque;
	}

	@Override
	protected void dataUpdated(final DataTable dataTable) {
		currentDataTable = dataTable;

		// convert to meta information DataTable
		updateMetaDataTable(currentDataTable);

		// clear possible existing data
		currentRangeAxisConfigsList.clear();
	}

	/**
	 * Converts a {@link DataTable} to the meta data {@link DataTable} which is used by the
	 * {@link HistogramTemplate}.
	 * 
	 * @param dataTable
	 *            the original {@link DataTable}
	 */
	private void updateMetaDataTable(final DataTable dataTable) {
		List<String> selectedNumericAttributes = new ArrayList<String>(plotNames.length);
		for (Object name : plotNames) {
			selectedNumericAttributes.add(String.valueOf(name));
		}
		ExampleSet newExampleSet = DataTableExampleSetAdapter.createExampleSetFromDataTable(dataTable);
		ExampleSet metaSet = DataTransformation.createDePivotizedExampleSet(newExampleSet, selectedNumericAttributes);
		// in case of error or no attributes specified
		if (metaSet == null) {
			return;
		}

		modifiedDataTable = new DataTableExampleSetAdapter(metaSet, null);
		PlotConfiguration plotConfiguration = new PlotConfiguration(new DataTableColumn(modifiedDataTable, 0));
		PlotInstance newPlotInstance = new PlotInstance(plotConfiguration, modifiedDataTable);
		// only set plotInstance if plotEngine currently displays the HistogramTemplate plotInstance
		// otherwise we would take another template the plotEngine away
		if (plotEngine.getPlotInstance() == getPlotInstance()) {
			plotEngine.setPlotInstance(newPlotInstance);
		}
		setPlotInstance(newPlotInstance);
	}

	/**
	 * Gets the current {@link DataTable} for this {@link HistogramTemplate}. Based on this a new
	 * {@link DataTable} can be created and given to the template via
	 * {@link #replaceTemporarilyCurrentDataTable(DataTable)}.
	 * 
	 * @return
	 */
	public DataTable getCurrentDataTable() {
		return this.currentDataTable;
	}

	/**
	 * Replaces the current {@link DataTable} with the given one. Can be used to filter the data for
	 * the histogram based on certain conditions.
	 * 
	 * @param replacementTable
	 * @param createBackup
	 *            if set to false, will NOT create a new backup. Use if chaining these calls without
	 *            calling {@link #revertToBackupDataTable()}.
	 */
	public void replaceTemporarilyCurrentDataTable(DataTable replacementTable, boolean createBackup) {
		if (createBackup) {
			currentDataTableBackup = currentDataTable;
		}
		currentDataTable = replacementTable;

		// convert to meta information DataTable
		updateMetaDataTable(currentDataTable);

		updatePlotConfiguration();
		setChanged();
		notifyObservers();
	}

	/**
	 * Returns the current {@link DataTable} back to the original. If it has not been modified via
	 * {@link #replaceTemporarilyCurrentDataTable(DataTable)}, nothing happens.
	 */
	public void revertToBackupDataTable() {
		if (currentDataTableBackup == null) {
			return;
		}
		currentDataTable = currentDataTableBackup;
		currentDataTableBackup = null;

		// convert to meta information DataTable
		updateMetaDataTable(currentDataTable);

		updatePlotConfiguration();
		setChanged();
		notifyObservers();
	}

	/**
	 * Returns <code>true</code> if a backup of the current {@link DataTable} exists.
	 * 
	 * @return
	 */
	public boolean hasBackupDataTable() {
		return currentDataTableBackup != null;
	}

	/**
	 * Gets the backup {@link DataTable} for this {@link HistogramTemplate}. Check prior to calling
	 * via {@link #hasBackupDataTable()}, otherwise you might get <code>null</code> as return value.
	 * 
	 * @return
	 */
	public DataTable getBackupDataTable() {
		return currentDataTableBackup;
	}

	/**
	 * Sets whether absolute values should be used or not.
	 * 
	 * @param useAbsoluteValues
	 */
	public void setUseAbsoluteValues(boolean useAbsoluteValues) {
		this.useAbsoluteValues = useAbsoluteValues;

		updatePlotConfiguration();
		setChanged();
		notifyObservers();
	}

	/**
	 * Returns whether absolute values are used or not.
	 * 
	 * @return
	 */
	public boolean isUsingAbsoluteValues() {
		return useAbsoluteValues;
	}

	/**
	 * Sets whether the range (Y) axis should be logarithmic or not.
	 * 
	 * @param yAxisLogarithmic
	 */
	public void setYAxisLogarithmic(boolean yAxisLogarithmic) {
		this.yAxisLogarithmic = yAxisLogarithmic;

		updatePlotConfiguration();
		setChanged();
		notifyObservers();
	}

	/**
	 * Returns whether the range (Y) axis is logarithmic or not.
	 * 
	 * @return
	 */
	public boolean isYAxisLogarithmic() {
		return yAxisLogarithmic;
	}

	/**
	 * Sets the currently selected plots by their name.
	 * 
	 * @param plotNames
	 */
	public void setPlotSelection(Object[] plotNames) {
		this.plotNames = plotNames;

		// plot selection has changed, update meta information DataTable
		updateMetaDataTable(currentDataTable);

		updatePlotConfiguration();
		setChanged();
		notifyObservers();
	}

	/**
	 * Returns the currently selected plots.
	 * 
	 * @return
	 */
	public Object[] getPlotSelection() {
		return plotNames;
	}

	public static String getI18NName() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.histogram.name");
	}

	@Override
	protected void updatePlotConfiguration() {
		// don't do anything if updates are suspended due to batch updating
		if (suspendUpdates) {
			return;
		}

		PlotConfiguration plotConfiguration = plotInstance.getMasterPlotConfiguration();
		// stop event processing
		boolean plotConfigurationProcessedEvents = plotConfiguration.isProcessingEvents();
		plotConfiguration.setProcessEvents(false);

		// remove old config(s)
		for (RangeAxisConfig rAConfig : currentRangeAxisConfigsList) {
			if (plotConfiguration.getIndexOfRangeAxisConfigById(rAConfig.getId()) != -1) {
				rAConfig.removeRangeAxisConfigListener(rangeAxisConfigListener);
				plotConfiguration.removeRangeAxisConfig(rAConfig);
			}
		}
		currentRangeAxisConfigsList.clear();

		// no selection?
		if (plotNames.length == 0) {
			plotConfiguration.setProcessEvents(plotConfigurationProcessedEvents);
			return;
		}

		try {
			DataTableColumn valueTableColumn = new DataTableColumn(modifiedDataTable,
					modifiedDataTable.getColumnIndex("value"));
			DataTableColumn attributeTableColumn = new DataTableColumn(modifiedDataTable,
					modifiedDataTable.getColumnIndex("attribute"));

			// set binning
			EquidistantFixedBinCountBinning newValueGrouping;
			newValueGrouping = (EquidistantFixedBinCountBinning) ValueGroupingFactory.getValueGrouping(
					GroupingType.EQUIDISTANT_FIXED_BIN_COUNT, valueTableColumn, false, DateFormat.getDateTimeInstance());
			newValueGrouping.setBinCount(bins);

			plotConfiguration.getDomainConfigManager().setGrouping(newValueGrouping);
			plotConfiguration.getDomainConfigManager().setDataTableColumn(valueTableColumn);

			// restore crosshairs
			List<AxisParallelLineConfiguration> clonedListOfDomainLines = new LinkedList<AxisParallelLineConfiguration>(
					listOfDomainLines);
			for (AxisParallelLineConfiguration lineConfig : clonedListOfDomainLines) {
				plotConfiguration.getDomainConfigManager().getCrosshairLines().addLine(lineConfig);
			}

			RangeAxisConfig newRangeAxisConfig = new RangeAxisConfig(null, plotConfiguration);
			ValueSource valueSource;
			valueSource = new ValueSource(plotConfiguration, attributeTableColumn, AggregationFunctionType.count, true);
			valueSource.setUseDomainGrouping(true);
			SeriesFormat sFormat = new SeriesFormat();
			sFormat.setSeriesType(VisualizationType.BARS);
			sFormat.setOpacity(opaque);
			valueSource.setSeriesFormat(sFormat);
			newRangeAxisConfig.addValueSource(valueSource, null);
			newRangeAxisConfig.setLogarithmicAxis(yAxisLogarithmic);
			newRangeAxisConfig.addRangeAxisConfigListener(rangeAxisConfigListener);

			// add new config(s) and restore crosshairs
			List<AxisParallelLineConfiguration> clonedRangeAxisLineList = rangeAxisCrosshairLinesMap.get(newRangeAxisConfig
					.getLabel());
			if (clonedRangeAxisLineList != null) {
				for (AxisParallelLineConfiguration lineConfig : clonedRangeAxisLineList) {
					newRangeAxisConfig.getCrossHairLines().addLine(lineConfig);
				}
			}
			plotConfiguration.addRangeAxisConfig(newRangeAxisConfig);
			// remember the new config so we can remove it later again
			currentRangeAxisConfigsList.add(newRangeAxisConfig);

			plotConfiguration.setDimensionConfig(PlotDimension.COLOR, null);
			DefaultDimensionConfig dimConfig = new DefaultDimensionConfig(plotConfiguration, attributeTableColumn,
					PlotDimension.COLOR);
			dimConfig.setGrouping(new DistinctValueGrouping(attributeTableColumn, true, null));
			plotConfiguration.setDimensionConfig(PlotDimension.COLOR, dimConfig);

			// general settings
			plotConfiguration.setAxesFont(styleProvider.getAxesFont());
			plotConfiguration.setTitleFont(styleProvider.getTitleFont());
			plotConfiguration.getLegendConfiguration().setLegendFont(styleProvider.getLegendFont());
			plotConfiguration.addColorSchemeAndSetActive(styleProvider.getColorScheme());
			if (styleProvider.isShowLegend()) {
				plotConfiguration.getLegendConfiguration().setLegendPosition(LegendPosition.BOTTOM);
			} else {
				plotConfiguration.getLegendConfiguration().setLegendPosition(LegendPosition.NONE);
			}
			plotConfiguration.setFrameBackgroundColor(ColorRGB.convertToColor(styleProvider.getFrameBackgroundColor()));
			plotConfiguration.setPlotBackgroundColor(ColorRGB.convertToColor(styleProvider.getPlotBackgroundColor()));
			plotConfiguration.setTitleText(styleProvider.getTitleText());
		} catch (ChartConfigurationException e) {
			// LogService.getRoot().log(Level.WARNING, "Chart could not be configured.", e);
			LogService.getRoot().log(
					Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.new_plotter.templates.HistrogramTemplate.configurating_chart_error"), e);

		} finally {
			// continue event processing
			plotConfiguration.setProcessEvents(plotConfigurationProcessedEvents);
		}

	}

	@Override
	public Element writeToXML(Document document) {
		Element template = document.createElement(PlotterTemplate.TEMPLATE_ELEMENT);
		template.setAttribute(PlotterTemplate.NAME_ELEMENT, getChartType());
		Element setting;

		setting = document.createElement(Y_AXIS_LOGARITHMIC_ELEMENT);
		setting.setAttribute(VALUE_ATTRIBUTE, String.valueOf(yAxisLogarithmic));
		template.appendChild(setting);

		setting = document.createElement(USE_ABSOLUTE_VALUES_ELEMENT);
		setting.setAttribute(VALUE_ATTRIBUTE, String.valueOf(useAbsoluteValues));
		template.appendChild(setting);

		setting = document.createElement(BINS_ELEMENT);
		setting.setAttribute(VALUE_ATTRIBUTE, String.valueOf(bins));
		template.appendChild(setting);

		setting = document.createElement(OPAQUE_ELEMENT);
		setting.setAttribute(VALUE_ATTRIBUTE, String.valueOf(opaque));
		template.appendChild(setting);

		setting = document.createElement(PLOT_NAMES_ELEMENT);
		for (Object key : plotNames) {
			Element plotNameElement = document.createElement(PLOT_NAME_ELEMENT);
			plotNameElement.setAttribute(VALUE_ATTRIBUTE, String.valueOf(key));
			setting.appendChild(plotNameElement);
		}
		template.appendChild(setting);

		// store crosshairs (RangeAxis)
		setting = document.createElement(CROSSHAIR_RANGE_AXIS_TOP_ELEMENT);
		for (String rangeAxisLabel : rangeAxisCrosshairLinesMap.keySet()) {
			// add crosshairs of currently not displayed range axes
			List<AxisParallelLineConfiguration> lines = rangeAxisCrosshairLinesMap.get(rangeAxisLabel);
			if (lines != null) {
				for (AxisParallelLineConfiguration line : lines) {
					Element rangeCrosshairElement = document.createElement(CROSSHAIR_RANGE_AXIS_ELEMENT);
					rangeCrosshairElement.setAttribute(CROSSHAIR_RANGE_AXIS_LABEL_ATTRIBUTE, rangeAxisLabel);
					rangeCrosshairElement.setAttribute(CROSSHAIR_VALUE_ATTRIBUTE, String.valueOf(line.getValue()));
					rangeCrosshairElement.setAttribute(CROSSHAIR_WIDTH_ATTRIBUTE,
							String.valueOf(line.getFormat().getWidth()));
					rangeCrosshairElement.setAttribute(CROSSHAIR_STYLE_ATTRIBUTE,
							String.valueOf(line.getFormat().getStyle()));
					Element colorElement = document.createElement(CROSSHAIR_COLOR_ELEMENT);
					colorElement.setAttribute(CROSSHAIR_COLOR_R_ATTRIBUTE,
							String.valueOf(line.getFormat().getColor().getRed()));
					colorElement.setAttribute(CROSSHAIR_COLOR_G_ATTRIBUTE,
							String.valueOf(line.getFormat().getColor().getGreen()));
					colorElement.setAttribute(CROSSHAIR_COLOR_B_ATTRIBUTE,
							String.valueOf(line.getFormat().getColor().getBlue()));
					rangeCrosshairElement.appendChild(colorElement);
					setting.appendChild(rangeCrosshairElement);
				}
			}
		}
		template.appendChild(setting);

		// store crosshairs (domainAxis)
		setting = document.createElement(CROSSHAIR_DOMAIN_TOP_ELEMENT);
		Element domainCrosshairElement = document.createElement(CROSSHAIR_DOMAIN_ELEMENT);
		for (AxisParallelLineConfiguration line : listOfDomainLines) {
			domainCrosshairElement.setAttribute(CROSSHAIR_VALUE_ATTRIBUTE, String.valueOf(line.getValue()));
			domainCrosshairElement.setAttribute(CROSSHAIR_WIDTH_ATTRIBUTE, String.valueOf(line.getFormat().getWidth()));
			domainCrosshairElement.setAttribute(CROSSHAIR_STYLE_ATTRIBUTE, String.valueOf(line.getFormat().getStyle()));
			Element colorElement = document.createElement(CROSSHAIR_COLOR_ELEMENT);
			colorElement.setAttribute(CROSSHAIR_COLOR_R_ATTRIBUTE, String.valueOf(line.getFormat().getColor().getRed()));
			colorElement.setAttribute(CROSSHAIR_COLOR_G_ATTRIBUTE, String.valueOf(line.getFormat().getColor().getGreen()));
			colorElement.setAttribute(CROSSHAIR_COLOR_B_ATTRIBUTE, String.valueOf(line.getFormat().getColor().getBlue()));
			domainCrosshairElement.appendChild(colorElement);
			setting.appendChild(domainCrosshairElement);
		}
		template.appendChild(setting);

		template.appendChild(styleProvider.createXML(document));

		return template;
	}

	@Override
	public void loadFromXML(Element templateElement) {
		suspendUpdates = true;

		for (int i = 0; i < templateElement.getChildNodes().getLength(); i++) {
			Node node = templateElement.getChildNodes().item(i);
			if (node instanceof Element) {
				Element setting = (Element) node;

				if (setting.getNodeName().equals(PLOT_NAMES_ELEMENT)) {
					List<Object> plotNamesList = new LinkedList<Object>();
					for (int j = 0; j < setting.getChildNodes().getLength(); j++) {
						Node plotNode = setting.getChildNodes().item(j);
						if (plotNode instanceof Element) {
							Element plotNameElement = (Element) plotNode;

							if (plotNameElement.getNodeName().equals(PLOT_NAME_ELEMENT)) {
								plotNamesList.add(plotNameElement.getAttribute(VALUE_ATTRIBUTE));
							}
						}
					}
					setPlotSelection(plotNamesList.toArray());
				} else if (setting.getNodeName().equals(USE_ABSOLUTE_VALUES_ELEMENT)) {
					setUseAbsoluteValues(Boolean.parseBoolean(setting.getAttribute(VALUE_ATTRIBUTE)));
				} else if (setting.getNodeName().equals(Y_AXIS_LOGARITHMIC_ELEMENT)) {
					setYAxisLogarithmic(Boolean.parseBoolean(setting.getAttribute(VALUE_ATTRIBUTE)));
				} else if (setting.getNodeName().equals(BINS_ELEMENT)) {
					try {
						setBins(Integer.parseInt(setting.getAttribute(VALUE_ATTRIBUTE)));
					} catch (NumberFormatException e) {
						// LogService.getRoot().warning("Could not restore bins setting for histogram template!");
						LogService
								.getRoot()
								.log(Level.WARNING,
										I18N.getMessage(LogService.getRoot().getResourceBundle(),
												"com.rapidminer.gui.new_plotter.templates.HistrogramTemplate.restoring_bins_setting_error"),
										e);
					}
				} else if (setting.getNodeName().equals(OPAQUE_ELEMENT)) {
					try {
						setOpaque(Integer.parseInt(setting.getAttribute(VALUE_ATTRIBUTE)));
					} catch (NumberFormatException e) {
						// LogService.getRoot().warning("Could not restore opaque setting for histogram template!");
						LogService
								.getRoot()
								.log(Level.WARNING,
										I18N.getMessage(LogService.getRoot().getResourceBundle(),
												"com.rapidminer.gui.new_plotter.templates.HistrogramTemplate.restoring_opaque_setting_error"),
										e);
					}
				} else if (setting.getNodeName().equals(CROSSHAIR_RANGE_AXIS_TOP_ELEMENT)) {
					try {
						// load range axes crosshairs
						for (int j = 0; j < setting.getChildNodes().getLength(); j++) {
							Node rangeCrosshairNode = setting.getChildNodes().item(j);
							if (rangeCrosshairNode instanceof Element) {
								Element rangeCrosshairElement = (Element) rangeCrosshairNode;

								if (rangeCrosshairElement.getNodeName().equals(CROSSHAIR_RANGE_AXIS_ELEMENT)) {
									// load range axis crosshair
									AxisParallelLineConfiguration line = new AxisParallelLineConfiguration(1.0, false);
									String rangeAxisLabel = rangeCrosshairElement
											.getAttribute(CROSSHAIR_RANGE_AXIS_LABEL_ATTRIBUTE);
									Double value = Double.parseDouble(rangeCrosshairElement
											.getAttribute(CROSSHAIR_VALUE_ATTRIBUTE));
									Float width = Float.parseFloat(rangeCrosshairElement
											.getAttribute(CROSSHAIR_WIDTH_ATTRIBUTE));
									LineStyle style = LineStyle.valueOf(rangeCrosshairElement
											.getAttribute(CROSSHAIR_STYLE_ATTRIBUTE));
									for (int k = 0; k < rangeCrosshairElement.getChildNodes().getLength(); k++) {
										Node colorNode = rangeCrosshairElement.getChildNodes().item(k);
										if (colorNode.getNodeName().equals(CROSSHAIR_COLOR_ELEMENT)) {
											Element colorElement = (Element) colorNode;
											int r = Integer.parseInt(colorElement.getAttribute(CROSSHAIR_COLOR_R_ATTRIBUTE));
											int g = Integer.parseInt(colorElement.getAttribute(CROSSHAIR_COLOR_G_ATTRIBUTE));
											int b = Integer.parseInt(colorElement.getAttribute(CROSSHAIR_COLOR_B_ATTRIBUTE));
											Color color = new Color(r, g, b);
											line.getFormat().setColor(color);
										}
									}
									line.setValue(value);
									line.getFormat().setWidth(width);
									line.getFormat().setStyle(style);

									// decide if crosshair is of the currently selected range axis
									// or not
									List<AxisParallelLineConfiguration> listOfLines = rangeAxisCrosshairLinesMap
											.get(rangeAxisLabel);
									if (listOfLines == null) {
										listOfLines = new LinkedList<AxisParallelLineConfiguration>();
									}
									listOfLines.add(line);
									rangeAxisCrosshairLinesMap.put(rangeAxisLabel, listOfLines);
								}
							}
						}
					} catch (NumberFormatException e) {
						LogService.getRoot().warning("Could not restore range axis crosshairs!");
					}
				} else if (setting.getNodeName().equals(CROSSHAIR_DOMAIN_TOP_ELEMENT)) {
					try {
						// load domain axis crosshairs
						for (int j = 0; j < setting.getChildNodes().getLength(); j++) {
							Node domainCrosshairNode = setting.getChildNodes().item(j);
							if (domainCrosshairNode instanceof Element) {
								Element domainCrosshairElement = (Element) domainCrosshairNode;

								if (domainCrosshairElement.getNodeName().equals(CROSSHAIR_DOMAIN_ELEMENT)) {
									// load domain axis crosshair
									AxisParallelLineConfiguration line = new AxisParallelLineConfiguration(1.0, false);
									Double value = Double.parseDouble(domainCrosshairElement
											.getAttribute(CROSSHAIR_VALUE_ATTRIBUTE));
									Float width = Float.parseFloat(domainCrosshairElement
											.getAttribute(CROSSHAIR_WIDTH_ATTRIBUTE));
									LineStyle style = LineStyle.valueOf(domainCrosshairElement
											.getAttribute(CROSSHAIR_STYLE_ATTRIBUTE));
									for (int k = 0; k < domainCrosshairElement.getChildNodes().getLength(); k++) {
										Node colorNode = domainCrosshairElement.getChildNodes().item(k);
										if (colorNode.getNodeName().equals(CROSSHAIR_COLOR_ELEMENT)) {
											Element colorElement = (Element) colorNode;
											int r = Integer.parseInt(colorElement.getAttribute(CROSSHAIR_COLOR_R_ATTRIBUTE));
											int g = Integer.parseInt(colorElement.getAttribute(CROSSHAIR_COLOR_G_ATTRIBUTE));
											int b = Integer.parseInt(colorElement.getAttribute(CROSSHAIR_COLOR_B_ATTRIBUTE));
											Color color = new Color(r, g, b);
											line.getFormat().setColor(color);
										}
									}
									line.setValue(value);
									line.getFormat().setWidth(width);
									line.getFormat().setStyle(style);

									// add to DomainConfigManager
									plotInstance.getMasterPlotConfiguration().getDomainConfigManager().getCrosshairLines()
											.addLine(line);
								}
							}
						}
					} catch (NumberFormatException e) {
						LogService.getRoot().warning("Could not restore domain axis crosshairs!");
					}
				} else if (setting.getNodeName().equals(PlotterStyleProvider.STYLE_ELEMENT)) {
					styleProvider.loadFromXML(setting);
				}
			}
		}

		suspendUpdates = false;
		updatePlotConfiguration();
	}
}
