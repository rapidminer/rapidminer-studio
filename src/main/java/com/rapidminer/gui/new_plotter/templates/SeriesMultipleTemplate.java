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
import com.rapidminer.gui.new_plotter.configuration.AxisParallelLineConfiguration;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.LegendConfiguration.LegendPosition;
import com.rapidminer.gui.new_plotter.configuration.LineFormat.LineStyle;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.ItemShape;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.VisualizationType;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.templates.SeriesTemplate.DataTableWithIndexDelegate;
import com.rapidminer.gui.new_plotter.templates.gui.SeriesMultipleTemplatePanel;
import com.rapidminer.gui.new_plotter.templates.style.ColorRGB;
import com.rapidminer.gui.new_plotter.templates.style.PlotterStyleProvider;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.math.function.aggregation.AbstractAggregationFunction.AggregationFunctionType;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * The template for a series multiple plot.
 * 
 * @author Marco Boeck
 * 
 */
public class SeriesMultipleTemplate extends PlotterTemplate {

	private static final String PLOT_NAME_ELEMENT = "plotName";

	private static final String PLOT_NAMES_ELEMENT = "plotNames";

	private static final String INDEX_NAME_ELEMENT = "indexName";

	/** the current {@link DataTable} */
	private DataTable currentDataTable;

	/** the current {@link RangeAxisConfig}s */
	private List<RangeAxisConfig> currentRangeAxisConfigsList;

	/** the name of the index column */
	private String indexName;

	/** the names of the plots to show */
	private Object[] plotNames;

	/**
	 * Creates a new {@link SeriesMultipleTemplate}. This template allows easy configuration of the
	 * histogram chart for the plotter.
	 */
	public SeriesMultipleTemplate() {
		currentRangeAxisConfigsList = new LinkedList<RangeAxisConfig>();

		// value when "None" is selected
		String noSelection = I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.column.empty_selection.label");
		indexName = noSelection;
		plotNames = new Object[0];

		guiPanel = new SeriesMultipleTemplatePanel(this);
	}

	@Override
	public String getChartType() {
		return SeriesMultipleTemplate.getI18NName();
	}

	/**
	 * Sets the name for the index dimension column.
	 * 
	 * @param columnName
	 */
	public void setIndexDimensionName(String columnName) {
		indexName = columnName;

		updatePlotConfiguration();
		setChanged();
		notifyObservers();
	}

	/**
	 * Returns the name of the index dimension column.
	 * 
	 * @return
	 */
	public String getIndexDimensionName() {
		return indexName;
	}

	/**
	 * Sets the currently selected plots by their name.
	 * 
	 * @param plotNames
	 */
	public void setPlotSelection(Object[] plotNames) {
		this.plotNames = plotNames;

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

	@Override
	protected void dataUpdated(final DataTable dataTable) {
		// add artifical index column if needed
		if (dataTable.getColumnIndex(SeriesTemplate.ARTIFICAL_INDEX_COLUMN) == -1) {
			currentDataTable = new DataTableWithIndexDelegate(dataTable);

			PlotConfiguration plotConfiguration = new PlotConfiguration(new DataTableColumn(currentDataTable, 0));
			PlotInstance plotInstance = new PlotInstance(plotConfiguration, currentDataTable);
			setPlotInstance(plotInstance);
		}

		// clear possible existing data
		currentRangeAxisConfigsList.clear();
	}

	public static String getI18NName() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.series_multiple.name");
	}

	@Override
	protected void updatePlotConfiguration() {
		// don't do anything if updates are suspended due to batch updating
		if (suspendUpdates) {
			return;
		}

		PlotConfiguration plotConfiguration = getPlotConfiguration();

		// stop event processing
		boolean plotConfigurationProcessedEvents = plotConfiguration.isProcessingEvents();
		plotConfiguration.setProcessEvents(false);

		// remove old config(s)
		for (RangeAxisConfig rAConfig : currentRangeAxisConfigsList) {
			rAConfig.removeRangeAxisConfigListener(rangeAxisConfigListener);
			plotConfiguration.removeRangeAxisConfig(rAConfig);
		}
		currentRangeAxisConfigsList.clear();

		// no selection?
		if (plotNames.length == 0) {
			plotConfiguration.setProcessEvents(plotConfigurationProcessedEvents);
			return;
		}

		// value when "None" is selected
		String noSelection = I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.column.empty_selection.label");
		DataTableColumn indexColumn;
		if (indexName.equals(noSelection)) {
			indexColumn = new DataTableColumn(currentDataTable,
					currentDataTable.getColumnIndex(SeriesTemplate.ARTIFICAL_INDEX_COLUMN));
		} else {
			indexColumn = new DataTableColumn(currentDataTable, currentDataTable.getColumnIndex(indexName));
		}
		DimensionConfig domainDimensionConfig = plotConfiguration.getDimensionConfig(PlotDimension.DOMAIN);
		domainDimensionConfig.setDataTableColumn(indexColumn);

		// restore crosshairs
		List<AxisParallelLineConfiguration> clonedListOfDomainLines = new LinkedList<AxisParallelLineConfiguration>(
				listOfDomainLines);
		for (AxisParallelLineConfiguration lineConfig : clonedListOfDomainLines) {
			plotConfiguration.getDomainConfigManager().getCrosshairLines().addLine(lineConfig);
		}

		int indexOfPlots = 0;
		for (Object plot : plotNames) {
			String plotName = String.valueOf(plot);
			RangeAxisConfig newRangeAxisConfig = new RangeAxisConfig(plotName, plotConfiguration);
			ValueSource valueSource;
			DataTableColumn aDataTableColumn = new DataTableColumn(currentDataTable,
					currentDataTable.getColumnIndex(plotName));
			valueSource = new ValueSource(plotConfiguration, aDataTableColumn, AggregationFunctionType.count, false);
			SeriesFormat sFormat = new SeriesFormat();
			sFormat.setSeriesType(VisualizationType.LINES_AND_SHAPES);
			sFormat.setLineStyle(LineStyle.SOLID);
			sFormat.setItemShape(ItemShape.NONE);
			sFormat.setLineWidth(1.5f);
			ColorRGB yAxisColor = styleProvider.getColorScheme().getColors()
					.get(indexOfPlots++ % styleProvider.getColorScheme().getColors().size());
			sFormat.setItemColor(ColorRGB.convertToColor(yAxisColor));
			valueSource.setSeriesFormat(sFormat);
			newRangeAxisConfig.addRangeAxisConfigListener(rangeAxisConfigListener);
			newRangeAxisConfig.addValueSource(valueSource, null);

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
		}

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

		// continue event processing
		plotConfiguration.setProcessEvents(plotConfigurationProcessedEvents);
	}

	@Override
	public Element writeToXML(Document document) {
		Element template = document.createElement(PlotterTemplate.TEMPLATE_ELEMENT);
		template.setAttribute(PlotterTemplate.NAME_ELEMENT, getChartType());
		Element setting;

		setting = document.createElement(INDEX_NAME_ELEMENT);
		setting.setAttribute(VALUE_ATTRIBUTE, String.valueOf(indexName));
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
				} else if (setting.getNodeName().equals(INDEX_NAME_ELEMENT)) {
					setIndexDimensionName(setting.getAttribute(VALUE_ATTRIBUTE));
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
