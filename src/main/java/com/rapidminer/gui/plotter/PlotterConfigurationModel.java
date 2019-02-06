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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.plotter.charts.BarChartPlotter;
import com.rapidminer.gui.plotter.charts.BlockChartPlotter;
import com.rapidminer.gui.plotter.charts.BubbleChartPlotter;
import com.rapidminer.gui.plotter.charts.DeviationChartPlotter;
import com.rapidminer.gui.plotter.charts.DistributionPlotter;
import com.rapidminer.gui.plotter.charts.HistogramChart;
import com.rapidminer.gui.plotter.charts.HistogramColorChart;
import com.rapidminer.gui.plotter.charts.MultipleScatterPlotter;
import com.rapidminer.gui.plotter.charts.MultipleSeriesChartPlotter;
import com.rapidminer.gui.plotter.charts.ParallelPlotter2;
import com.rapidminer.gui.plotter.charts.ParetoChartPlotter;
import com.rapidminer.gui.plotter.charts.PieChart2DPlotter;
import com.rapidminer.gui.plotter.charts.PieChart3DPlotter;
import com.rapidminer.gui.plotter.charts.RingChartPlotter;
import com.rapidminer.gui.plotter.charts.ScatterPlotter2;
import com.rapidminer.gui.plotter.charts.SeriesChartPlotter;
import com.rapidminer.gui.plotter.charts.StackedBarChartPlotter;
import com.rapidminer.gui.plotter.charts.WebPlotter;
import com.rapidminer.gui.plotter.mathplot.BoxPlot2D;
import com.rapidminer.gui.plotter.mathplot.BoxPlot3D;
import com.rapidminer.gui.plotter.mathplot.ScatterPlot3D;
import com.rapidminer.gui.plotter.mathplot.ScatterPlot3DColor;
import com.rapidminer.gui.plotter.mathplot.SticksPlot2D;
import com.rapidminer.gui.plotter.mathplot.SticksPlot3D;
import com.rapidminer.gui.plotter.mathplot.SurfacePlot3D;
import com.rapidminer.gui.plotter.som.SOMPlotter;
import com.rapidminer.operator.visualization.SOMModelPlotter;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeEnumeration;


/**
 * This class is the model of plotter configuration. It holds the connection to the configuration
 * data and all listeners. It might be notified of changes of the parameters, updates the
 * configuration data and informs all listeners of the changed configuration.
 * 
 * @author Sebastian Land
 * @deprecated since 9.2.0
 */
@Deprecated
public class PlotterConfigurationModel implements Cloneable {

	public static final String LINES_PLOT = "Lines";

	public static final String BOUND_PLOT = "Bound";

	public static final String HINTON_PLOT = "Hinton";

	public static final String SURFACE_PLOT_3D = "Surface 3D";

	public static final String BOX_CHART_3D = "Box 3D";

	public static final String BOX_CHART = "Box";

	public static final String STICK_CHART_3D = "Sticks 3D";

	public static final String STICK_CHART = "Sticks";

	public static final String QUARTILE_PLOT_COLOR_MATRIX = "Quartile Color Matrix";

	public static final String QUARTILE_PLOT_COLOR = "Quartile Color";

	public static final String QUARTILE_PLOT = "Quartile";

	public static final String HISTOGRAM_PLOT_COLOR = "Histogram Color";

	public static final String HISTOGRAM_PLOT = "Histogram";

	public static final String DISTRIBUTION_PLOT = "Distribution";

	public static final String ANDREWS_CURVES = "Andrews Curves";

	public static final String PARETO_PLOT = "Pareto";

	public static final String BAR_CHART = "Bars";

	public static final String BAR_CHART_STACKED = "Bars Stacked";

	public static final String RING_CHART = "Ring";

	public static final String PIE_CHART_3D = "Pie 3D";

	public static final String PIE_CHART = "Pie";

	public static final String DENSITY_PLOT = "Density";

	public static final String BLOCK_PLOT = "Block";

	public static final String SOM_PLOT = "SOM";

	public static final String SURVEY_PLOT = "Survey";

	public static final String MULTIPLE_SERIES_PLOT = "Series Multiple";

	public static final String SERIES_PLOT = "Series";

	public static final String DEVIATION_PLOT = "Deviation";

	public static final String PARALLEL_PLOT = "Parallel";

	public static final String BUBBLE_PLOT = "Bubble";

	public static final String SCATTER_PLOT_3D_COLOR = "Scatter 3D Color";

	public static final String SCATTER_PLOT_3D = "Scatter 3D";

	public static final String SCATTER_PLOT_MATRIX = "Scatter Matrix";

	public static final String MULTIPLE_SELECTION_SCATTER_PLOT = "Scatter Multiple";

	public static final String SCATTER_PLOT = "Scatter";

	public static final String RADVIZ_PLOT = "RadViz";

	public static final String WEB_PLOT = "Web";

	public final static LinkedHashMap<String, Class<? extends Plotter>> COMPLETE_PLOTTER_SELECTION = new LinkedHashMap<>();

	public final static LinkedHashMap<String, Class<? extends Plotter>> WEIGHT_PLOTTER_SELECTION = new LinkedHashMap<>();

	public final static LinkedHashMap<String, Class<? extends Plotter>> DATA_SET_PLOTTER_SELECTION = new LinkedHashMap<>();

	public final static LinkedHashMap<String, Class<? extends Plotter>> MODEL_PLOTTER_SELECTION = new LinkedHashMap<>();

	public final static LinkedHashMap<String, Class<? extends Plotter>> COLOR_HISTOGRAM_PLOTTER_SELECTION = new LinkedHashMap<>();

	static {

		COMPLETE_PLOTTER_SELECTION.put(SCATTER_PLOT, ScatterPlotter2.class);
		COMPLETE_PLOTTER_SELECTION.put(MULTIPLE_SELECTION_SCATTER_PLOT, MultipleScatterPlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(SCATTER_PLOT_MATRIX, ScatterMatrixPlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(SCATTER_PLOT_3D, ScatterPlot3D.class);
		COMPLETE_PLOTTER_SELECTION.put(SCATTER_PLOT_3D_COLOR, ScatterPlot3DColor.class);
		COMPLETE_PLOTTER_SELECTION.put(BUBBLE_PLOT, BubbleChartPlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(PARALLEL_PLOT, ParallelPlotter2.class);
		COMPLETE_PLOTTER_SELECTION.put(DEVIATION_PLOT, DeviationChartPlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(SERIES_PLOT, SeriesChartPlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(MULTIPLE_SERIES_PLOT, MultipleSeriesChartPlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(SURVEY_PLOT, SurveyPlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(SOM_PLOT, SOMPlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(BLOCK_PLOT, BlockChartPlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(DENSITY_PLOT, DensityPlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(PIE_CHART, PieChart2DPlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(PIE_CHART_3D, PieChart3DPlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(RING_CHART, RingChartPlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(BAR_CHART, BarChartPlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(BAR_CHART_STACKED, StackedBarChartPlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(PARETO_PLOT, ParetoChartPlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(ANDREWS_CURVES, AndrewsCurves.class);
		COMPLETE_PLOTTER_SELECTION.put(DISTRIBUTION_PLOT, DistributionPlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(HISTOGRAM_PLOT, HistogramChart.class);
		COMPLETE_PLOTTER_SELECTION.put(HISTOGRAM_PLOT_COLOR, HistogramColorChart.class);
		COMPLETE_PLOTTER_SELECTION.put(QUARTILE_PLOT, QuartilePlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(QUARTILE_PLOT_COLOR, ColorQuartilePlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(QUARTILE_PLOT_COLOR_MATRIX, ColorQuartileMatrixPlotter.class);
		COMPLETE_PLOTTER_SELECTION.put(STICK_CHART, SticksPlot2D.class);
		COMPLETE_PLOTTER_SELECTION.put(STICK_CHART_3D, SticksPlot3D.class);
		COMPLETE_PLOTTER_SELECTION.put(BOX_CHART, BoxPlot2D.class);
		COMPLETE_PLOTTER_SELECTION.put(BOX_CHART_3D, BoxPlot3D.class);
		COMPLETE_PLOTTER_SELECTION.put(SURFACE_PLOT_3D, SurfacePlot3D.class);
		COMPLETE_PLOTTER_SELECTION.put(HINTON_PLOT, HintonDiagram.class);
		COMPLETE_PLOTTER_SELECTION.put(BOUND_PLOT, BoundDiagram.class);
		COMPLETE_PLOTTER_SELECTION.put(WEB_PLOT, WebPlotter.class);

		WEIGHT_PLOTTER_SELECTION.put(LINES_PLOT, ScatterPlotter.class);
		WEIGHT_PLOTTER_SELECTION.put(HISTOGRAM_PLOT, HistogramChart.class);
		WEIGHT_PLOTTER_SELECTION.put(HINTON_PLOT, HintonDiagram.class);
		WEIGHT_PLOTTER_SELECTION.put(BOUND_PLOT, BoundDiagram.class);
		WEIGHT_PLOTTER_SELECTION.put(PIE_CHART, PieChart2DPlotter.class);
		WEIGHT_PLOTTER_SELECTION.put(PIE_CHART_3D, PieChart3DPlotter.class);
		WEIGHT_PLOTTER_SELECTION.put(RING_CHART, RingChartPlotter.class);
		WEIGHT_PLOTTER_SELECTION.put(BAR_CHART, BarChartPlotter.class);
		WEIGHT_PLOTTER_SELECTION.put(WEB_PLOT, WebPlotter.class);

		DATA_SET_PLOTTER_SELECTION.put(SCATTER_PLOT, ScatterPlotter2.class);
		DATA_SET_PLOTTER_SELECTION.put(MULTIPLE_SELECTION_SCATTER_PLOT, MultipleScatterPlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(SCATTER_PLOT_MATRIX, ScatterMatrixPlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(SCATTER_PLOT_3D, ScatterPlot3D.class);
		DATA_SET_PLOTTER_SELECTION.put(SCATTER_PLOT_3D_COLOR, ScatterPlot3DColor.class);
		DATA_SET_PLOTTER_SELECTION.put(BUBBLE_PLOT, BubbleChartPlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(PARALLEL_PLOT, ParallelPlotter2.class);
		DATA_SET_PLOTTER_SELECTION.put(SERIES_PLOT, SeriesChartPlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(MULTIPLE_SERIES_PLOT, MultipleSeriesChartPlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(SURVEY_PLOT, SurveyPlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(SOM_PLOT, SOMPlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(BLOCK_PLOT, BlockChartPlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(DENSITY_PLOT, DensityPlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(DEVIATION_PLOT, DeviationChartPlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(HISTOGRAM_PLOT, HistogramChart.class);
		DATA_SET_PLOTTER_SELECTION.put(HISTOGRAM_PLOT_COLOR, HistogramColorChart.class);
		DATA_SET_PLOTTER_SELECTION.put(BAR_CHART, BarChartPlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(BAR_CHART_STACKED, StackedBarChartPlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(PARETO_PLOT, ParetoChartPlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(ANDREWS_CURVES, AndrewsCurves.class);
		DATA_SET_PLOTTER_SELECTION.put(DISTRIBUTION_PLOT, DistributionPlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(WEB_PLOT, WebPlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(QUARTILE_PLOT, QuartilePlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(QUARTILE_PLOT_COLOR, ColorQuartilePlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(QUARTILE_PLOT_COLOR_MATRIX, ColorQuartileMatrixPlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(PIE_CHART, PieChart2DPlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(PIE_CHART_3D, PieChart3DPlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(RING_CHART, RingChartPlotter.class);
		DATA_SET_PLOTTER_SELECTION.put(STICK_CHART, SticksPlot2D.class);
		DATA_SET_PLOTTER_SELECTION.put(STICK_CHART_3D, SticksPlot3D.class);
		DATA_SET_PLOTTER_SELECTION.put(BOX_CHART, BoxPlot2D.class);
		DATA_SET_PLOTTER_SELECTION.put(BOX_CHART_3D, BoxPlot3D.class);
		DATA_SET_PLOTTER_SELECTION.put(SURFACE_PLOT_3D, SurfacePlot3D.class);

		MODEL_PLOTTER_SELECTION.put(SOM_PLOT, SOMModelPlotter.class);

		COLOR_HISTOGRAM_PLOTTER_SELECTION.put(HISTOGRAM_PLOT_COLOR, HistogramColorChart.class);
	}

	public static interface PlotterSettingsChangedListener {

		public void settingChanged(String generalKey, String specificKey, String value);
	}

	public static interface PlotterChangedListener {

		/**
		 * This has to return a complete list of the objects listening to the plotter settings,
		 * defined inside this class.
		 */
		public List<PlotterSettingsChangedListener> getListeningObjects();

		public void plotterChanged(String plotterName);
	}

	private PlotterConfigurationSettings settings = new PlotterConfigurationSettings();
	private Plotter plotter;
	private DataTable dataTable;
	private List<PlotterSettingsChangedListener> settingsListeners = new LinkedList<>();
	private List<PlotterChangedListener> plotterListeners = new LinkedList<>();
	private boolean isInitialPlotter = true;

	/**
	 * This is a convenience constructor building settings providing the complete plotter selection
	 * and creating the given plotter per default. This might be used, if exactly one plotter should
	 * be build.
	 */
	public PlotterConfigurationModel(String plotterName, DataTable dataTable) {
		this(COMPLETE_PLOTTER_SELECTION, plotterName, dataTable);
		isInitialPlotter = false;
	}

	/**
	 * @param availablePlotters
	 * @param dataTable
	 */
	public PlotterConfigurationModel(HashMap<String, Class<? extends Plotter>> availablePlotters, DataTable dataTable) {
		this(availablePlotters, availablePlotters.keySet().iterator().next(), dataTable);
	}

	public PlotterConfigurationModel(HashMap<String, Class<? extends Plotter>> availablePlotters, String defaultPlotter,
			DataTable dataTable) {
		this.settings.setAvailablePlotters(availablePlotters);
		setDataTable(dataTable);
		setPlotter(defaultPlotter);

	}

	/**
	 * This constructor builds a new plotter with the given data set and selection but uses the
	 * given settings.
	 */
	public PlotterConfigurationModel(PlotterConfigurationSettings settings,
			HashMap<String, Class<? extends Plotter>> availablePlotters, DataTable dataTable) {
		this.settings = settings;
		setDataTable(dataTable);
		setPlotter(settings.getPlotterName());
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new PlotterConfigurationModel(settings.clone(), settings.getAvailablePlotters(), dataTable);
	}

	public void setParameterAsBoolean(String key, boolean value) {
		String name = PlotterAdapter.transformParameterName(plotter.getPlotterName());
		if (key.startsWith("_")) {
			setParameterValue(name + key, Boolean.toString(value));
		} else {
			setParameterValue(name + "_" + key, Boolean.toString(value));
		}
	}

	public void setParameterAsDouble(String key, double value) {
		String name = PlotterAdapter.transformParameterName(plotter.getPlotterName());
		if (key.startsWith("_")) {
			setParameterValue(name + key, Double.toString(value));
		} else {
			setParameterValue(name + "_" + key, Double.toString(value));
		}
	}

	public void setParameterAsInt(String key, int value) {
		String name = PlotterAdapter.transformParameterName(plotter.getPlotterName());
		if (key.startsWith("_")) {
			setParameterValue(name + key, Integer.toString(value));
		} else {
			setParameterValue(name + "_" + key, Integer.toString(value));
		}
	}

	public void setParameterAsString(String key, String value) {
		if (!key.startsWith("_")) {
			key = "_" + key;
		}
		if (plotter == null) {
			settings.setParameterValue(key, value);
		} else {
			String name = transformPlotterName(plotter.getPlotterName());
			setParameterValue(name + key, value);
		}
	}

	public void setParameterValue(String key, String value) {
		// if not already the same, save and inform listener
		String generalizedKey = generalizeKeyName(key);
		Object knownSetting = settings.getParameterValue(generalizedKey);
		if (knownSetting == null || !knownSetting.equals(value)) {
			settings.setParameterValue(generalizedKey, value);
			for (PlotterSettingsChangedListener listener : settingsListeners) {
				listener.settingChanged(generalizedKey, key, value);
			}
		}
	}

	public String getParameter(String key) {
		return settings.getParameterValue(key);
	}

	public void setPlotter(String name) {
		if (settings.getPlotterName() == null || !settings.getPlotterName().equals(name) || isInitialPlotter) {
			Class<?> plotterClass = settings.getAvailablePlotters().get(name);
			if (plotterClass != null
					&& (isInitialPlotter || plotter == null || !(plotterClass.isAssignableFrom(plotter.getClass()) && plotter
							.getClass().isAssignableFrom(plotterClass)))) {
				isInitialPlotter = plotter == null;
				Plotter plotter;
				try {
					plotter = settings.getAvailablePlotters().get(name).getConstructor(PlotterConfigurationModel.class)
							.newInstance(this);
					setPlotter(plotter);
				} catch (InstantiationException e) {
				} catch (IllegalAccessException e) {
				} catch (IllegalArgumentException e) {
				} catch (SecurityException e) {
				} catch (InvocationTargetException e) {
				} catch (NoSuchMethodException e) {
				}
			}
		}
	}

	public void setPlotter(Plotter plotter) {
		settings.setPlotterName(plotter.getPlotterName());

		// removing everything listening depending on old plotter
		removeSettingsListener(this.plotter);
		for (PlotterChangedListener plotterListener : plotterListeners) {
			for (PlotterSettingsChangedListener listener : plotterListener.getListeningObjects()) {
				removeSettingsListener(listener);
			}
		}

		// now saving new as current plotter
		removePlotterListener(this.plotter);
		this.plotter = plotter;
		plotter.setDataTable(getDataTable());

		registerPlotterListener(plotter);

		// inform everyone of plotter change and hence of all parameters changed
		informAllPlotterListener(plotter);

		// now retrieving every plotterSettingsListener and register it
		registerSettingsListener(plotter);
		for (PlotterChangedListener plotterListener : plotterListeners) {
			for (PlotterSettingsChangedListener listener : plotterListener.getListeningObjects()) {
				registerSettingsListener(listener);
			}
		}

		// setting default values
		List<ParameterType> parameters = plotter.getParameterTypes(null);
		// this keeps old settings because they could be needed again and might be more useful than
		// defaults
		for (ParameterType type : parameters) {
			String generalizedKeyName = generalizeKeyName(type.getKey());
			if (!settings.isParameterSet(generalizedKeyName) && type.getDefaultValueAsString() != null) {
				settings.setParameterValue(generalizedKeyName, type.getDefaultValueAsString());
			}
		}

		informAllSettingsListener();
	}

	/**
	 * Adds a set of parameters to the current settings, overriding old ones with the same name
	 * 
	 * @param settings
	 *            A map of parameter settings
	 */
	public void addParameters(HashMap<String, String> settings) {
		for (String key : settings.keySet()) {
			this.settings.setParameterValue(key, settings.get(key));
		}
	}

	private void informAllPlotterListener(Plotter plotter) {
		List<PlotterChangedListener> copyPlotterListener = new ArrayList<>(plotterListeners.size());
		copyPlotterListener.addAll(plotterListeners);
		for (PlotterChangedListener listener : copyPlotterListener) {
			listener.plotterChanged(plotter.getPlotterName());
		}
	}

	private void informAllSettingsListener() {
		// and finally inform every listener
		try {
			List<PlotterSettingsChangedListener> copySettingsChangedListener = new ArrayList<>(settingsListeners.size());
			copySettingsChangedListener.addAll(settingsListeners);
			for (PlotterSettingsChangedListener listener : copySettingsChangedListener) {
				informSettingsListener(listener);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void informSettingsListener(PlotterSettingsChangedListener listener) {
		Set<String> copiedSet = new HashSet<>();
		copiedSet.addAll(settings.getParameterSettings().keySet());
		for (String generalizedKey : copiedSet) {
			String specifiedKey = specifyKeyName(generalizedKey);
			String value = settings.getParameterValue(generalizedKey);
			listener.settingChanged(generalizedKey, specifiedKey, value);
		}
	}

	public void removeSettingsListener(PlotterSettingsChangedListener listener) {
		settingsListeners.remove(listener);
	}

	public void removePlotterListener(PlotterChangedListener listener) {
		plotterListeners.remove(listener);
	}

	public HashMap<String, Class<? extends Plotter>> getAvailablePlotters() {
		return settings.getAvailablePlotters();
	}

	public DataTable getDataTable() {
		return dataTable;
	}

	/**
	 * This listener will be registered and immediately informed over all settings
	 */
	public void registerSettingsListener(PlotterSettingsChangedListener listener) {
		settingsListeners.add(listener);
		informSettingsListener(listener);
	}

	/**
	 * This listener will be registered and immediately informed about the current plotter
	 */
	public void registerPlotterListener(PlotterChangedListener listener) {
		plotterListeners.add(listener);
		// & changed == false
		if (plotter != null) {
			listener.plotterChanged(plotter.getPlotterName());
		}
		settingsListeners.addAll(listener.getListeningObjects());
		for (PlotterSettingsChangedListener settingsListener : listener.getListeningObjects()) {
			informSettingsListener(settingsListener);
		}
	}

	private String specifyKeyName(String generalizedKey) {
		return transformPlotterName(settings.getPlotterName()) + generalizedKey;
	}

	private String generalizeKeyName(String key) {
		return key.substring(transformPlotterName(settings.getPlotterName()).length());
	}

	private String transformPlotterName(String name) {
		String result = name.toLowerCase();
		result = result.replaceAll("\\W", "_");
		return result;
	}

	public void setDataTable(DataTable dataTable) {
		this.dataTable = dataTable;
		if (plotter != null) {
			plotter.setDataTable(dataTable);
		}
		informAllSettingsListener();
	}

	public Plotter getPlotter() {
		return plotter;
	}

	/**
	 * This method returns an immutable set of the keys stored here.
	 */
	public Set<String> getParameterSet() {
		return this.settings.getParameterSettings().keySet();
	}

	public HashMap<String, String> getParameterSettings() {
		return this.settings.getParameterSettings();
	}

	// convenient parameter methods: This theoretically should not be necessary!
	public void setAxis(int axis, int dimension) {
		setAxis(axis, dataTable.getColumnName(dimension));
	}

	public void setAxis(int axis, String column) {
		setParameterAsString(
				PlotterAdapter.PARAMETER_SUFFIX_AXIS + PlotterAdapter.transformParameterName(plotter.getAxisName(axis)),
				column);
	}

	public void enablePlotColumn(int dimension) {
		enablePlotColumn(dataTable.getColumnName(dimension));
	}

	public void enablePlotColumn(String column) {
		String key = PlotterAdapter.PARAMETER_PLOT_COLUMNS;
		String[] oldColumns = ParameterTypeEnumeration.transformString2Enumeration(getParameter(key));
		List<String> columns = new LinkedList<>();
		columns.addAll(Arrays.asList(oldColumns));
		columns.add(column);
		setParameterAsString(key, ParameterTypeEnumeration.transformEnumeration2String(columns));
	}

	public PlotterConfigurationSettings getPlotterSettings() {
		return settings;
	}

	/*
	 * IO Methods
	 */

	public static Element writeToXML(Document document, PlotterConfigurationModel model) {
		Element parameters = document.createElement("plottersettings");
		parameters.setAttribute("plotter", model.getPlotter().getPlotterName());
		for (String key : model.settings.getParameterSettings().keySet()) {
			Element parameter = document.createElement("setting");
			parameter.setAttribute("key", key);
			parameter.setAttribute("value", model.settings.getParameterSettings().get(key).toString());
			parameters.appendChild(parameter);
		}
		return parameters;
	}

	public static PlotterConfigurationModel loadFromXML(Node node, DataTable table) {
		return loadFromXML(node, table, COMPLETE_PLOTTER_SELECTION);
	}

	public static PlotterConfigurationModel loadFromXML(Node node, DataTable table,
			HashMap<String, Class<? extends Plotter>> availablePlotters) {
		if (node instanceof Element) {
			Element parameters = (Element) node;
			if (parameters.getNodeName().equals("plottersettings")) {
				String plotterName = parameters.getAttribute("plotter");
				PlotterConfigurationModel settings = new PlotterConfigurationModel(availablePlotters, plotterName, table);

				// now searching all child elements for settings
				NodeList list = parameters.getElementsByTagName("setting");
				for (int i = 0; i < list.getLength(); i++) {
					Node settingsNode = list.item(i);
					if (settingsNode instanceof Element) {
						Element parameter = (Element) settingsNode;
						settings.setParameterAsString(parameter.getAttribute("key"), parameter.getAttribute("value"));
					}
				}
				return settings;
			}
		}
		return null;
	}

	public void setParameters(HashMap<String, String> settings) {
		this.settings.setParamterSettings(settings);

	}

}
