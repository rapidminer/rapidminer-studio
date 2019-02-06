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

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jfree.chart.plot.PlotOrientation;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.new_plotter.PlotConfigurationError;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.DomainConfigManager.GroupingState;
import com.rapidminer.gui.new_plotter.configuration.LineFormat.LineStyle;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.ItemShape;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.VisualizationType;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.listener.LinkAndBrushListener;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.listener.LinkAndBrushSelection;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.listener.LinkAndBrushSelectionListener;
import com.rapidminer.gui.new_plotter.listener.DimensionConfigListener;
import com.rapidminer.gui.new_plotter.listener.LegendConfigurationListener;
import com.rapidminer.gui.new_plotter.listener.PlotConfigurationListener;
import com.rapidminer.gui.new_plotter.listener.PlotConfigurationProcessingListener;
import com.rapidminer.gui.new_plotter.listener.RangeAxisConfigListener;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent.DimensionConfigChangeType;
import com.rapidminer.gui.new_plotter.listener.events.LegendConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent.PlotConfigurationChangeType;
import com.rapidminer.gui.new_plotter.listener.events.RangeAxisConfigChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.ValueGroupingChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.ValueGroupingChangeEvent.ValueGroupingChangeType;
import com.rapidminer.gui.new_plotter.listener.events.ValueRangeChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.ValueRangeChangeEvent.ValueRangeChangeType;
import com.rapidminer.gui.new_plotter.listener.events.ValueSourceChangeEvent;
import com.rapidminer.gui.new_plotter.templates.style.ColorRGB;
import com.rapidminer.gui.new_plotter.templates.style.ColorScheme;
import com.rapidminer.gui.new_plotter.utility.CategoricalColorProvider;
import com.rapidminer.gui.new_plotter.utility.ContinuousColorProvider;
import com.rapidminer.gui.new_plotter.utility.DataStructureUtils;
import com.rapidminer.gui.new_plotter.utility.ListUtility;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ParameterService;


/**
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class PlotConfiguration implements DimensionConfigListener, RangeAxisConfigListener, Cloneable,
		LinkAndBrushSelectionListener, LegendConfigurationListener, LinkAndBrushListener {

	public static final Paint DEFAULT_SERIES_OUTLINE_PAINT = Color.BLACK;

	public static final Font DEFAULT_AXES_FONT = FontTools.getFont(Font.DIALOG, Font.PLAIN, 10);

	public static final Color DEFAULT_OUTLINE_COLOR = Color.BLACK;

	private boolean initializing = false;

	private static final double MIN_SHAPE_SCALING_FACTOR = 0.4;
	private static final double MAX_SHAPE_SCALING_FACTOR = 5.0;

	public static final int GUI_PLOTTER_ROWS_MAXIMUM_IF_RAPIDMINER_PROPERTY_NOT_READABLE = 5000;

	private static final String DEFAULT_TITLE_TEXT = null;
	private static final Font DEFAULT_TITLE_FONT = FontTools.getFont("Arial", Font.PLAIN, 20);

	private static final Color DEFAULT_PLOT_BACKGROUND_COLOR = Color.white;
	private static final Color DEFAULT_FRAME_BACKGROUND_COLOR = Color.white;
	private static final PlotOrientation DEFAULT_PLOT_ORIENTATION = PlotOrientation.VERTICAL;

	private static final float DEFAULT_AXIS_LINE_WIDTH = 1.0f;

	private static final Color DEFAULT_AXIS_COLOR = Color.black;

	public static final Color DEFAULT_TITLE_COLOR = Color.black;

	private final List<RangeAxisConfig> rangeAxisConfigs = new LinkedList<RangeAxisConfig>();
	private final DomainConfigManager domainConfigManager;

	/**
	 * Stores which DimensionConfig is used for a Dimension. All {@link ValueSource} use the
	 * same DimensionConfig. To be precisely, all {@link ValueSource}s reference the same object
	 * and take the reference from this map.
	 *
	 * Exception: the domain Dimension is stored in the DomainConfigManager, to control proper
	 * setting of groupings for all {@link ValueSource}s.
	 *
	 * For obvious reason also the VALUE-dimension is NOT stored in this map.
	 */
	private Map<PlotDimension, DefaultDimensionConfig> dimensionConfigMap = new HashMap<DimensionConfig.PlotDimension, DefaultDimensionConfig>();

	private String titleText = DEFAULT_TITLE_TEXT;
	private Font titleFont = DEFAULT_TITLE_FONT;

	private Color plotBackgroundColor = DEFAULT_PLOT_BACKGROUND_COLOR;
	private Color frameBackgroundColor = DEFAULT_FRAME_BACKGROUND_COLOR;
	private Font axesFont = DEFAULT_AXES_FONT;
	private PlotOrientation orientation = DEFAULT_PLOT_ORIENTATION;

	/**
	 * If this variable is true, events that happen inside this PlotConfiguration, e.g. changes of
	 * RangeAxis, ValueSource etc., are process by the event queue. If this variable is false, no
	 * events are processed.
	 *
	 * Best Practice to use it: boolean processing = isProcessingEvents(); setProcessEvents(false);
	 * OTHER CODE setProcessEvents(processing);
	 */
	private Boolean processEvents = Boolean.TRUE;

	private List<PlotConfigurationChangeEvent> eventList = new LinkedList<PlotConfigurationChangeEvent>();
	private Integer listenersInformedCounter = 0;

	private transient List<WeakReference<PlotConfigurationListener>> defaultListeners = new LinkedList<WeakReference<PlotConfigurationListener>>();
	private transient List<WeakReference<PlotConfigurationListener>> prioritizedListeners = new LinkedList<WeakReference<PlotConfigurationListener>>();
	private transient List<WeakReference<PlotConfigurationProcessingListener>> processingListeners = new LinkedList<WeakReference<PlotConfigurationProcessingListener>>();

	private boolean changingRange;
	private boolean changingGrouping;

	private float axisLineWidth = DEFAULT_AXIS_LINE_WIDTH;
	private Color axisLineColor = DEFAULT_AXIS_COLOR;

	private Map<String, ColorScheme> colorSchemes = new HashMap<String, ColorScheme>();
	private String activeSchemeName;

	/**
	 * The event which is currently fired.
	 */
	private PlotConfigurationChangeEvent currentEvent = null;

	private LinkAndBrushMaster linkAndBrushMaster;

	private LegendConfiguration legendConfiguration = new LegendConfiguration();

	private int idCounter = 0;

	private Color titleColor = DEFAULT_TITLE_COLOR;

	/**
	 * Creates a plot configuration with one empty {@link RangeAxisConfig}.
	 *
	 * @param domainColumn
	 */
	public PlotConfiguration(DataTableColumn domainColumn) {
		this.domainConfigManager = new DomainConfigManager(this, domainColumn);
		this.linkAndBrushMaster = new LinkAndBrushMaster(this);
		this.linkAndBrushMaster.addLinkAndBrushListener(this);
		domainConfigManager.addDimensionConfigListener(this);
		legendConfiguration.addListener(this);

		createDefaultColorSchemes();

	}

	/**
	 *
	 */
	private void createDefaultColorSchemes() {
		ColorScheme colorScheme = getDefaultColorScheme();
		this.colorSchemes.put(colorScheme.getName(), colorScheme);

		/*
		 * default color schemes are defined here
		 */
		List<ColorRGB> listOfColors = new LinkedList<ColorRGB>();
		listOfColors.add(new ColorRGB(124, 181, 236));
		listOfColors.add(new ColorRGB(67, 67, 72));
		listOfColors.add(new ColorRGB(144, 237, 125));
		listOfColors.add(new ColorRGB(247, 163, 92));
		listOfColors.add(new ColorRGB(128, 133, 233));
		listOfColors.add(new ColorRGB(241, 92, 128));
		listOfColors.add(new ColorRGB(228, 211, 84));
		listOfColors.add(new ColorRGB(128, 133, 232));
		listOfColors.add(new ColorRGB(141, 70, 83));
		listOfColors.add(new ColorRGB(145, 232, 225));
		ColorScheme cs = new ColorScheme("Pastel", listOfColors);
		this.colorSchemes.put(cs.getName(), cs);

		listOfColors = new LinkedList<ColorRGB>();
		ColorRGB colorful1 = new ColorRGB(222, 217, 26);
		ColorRGB colorful2 = new ColorRGB(219, 138, 47);
		ColorRGB colorful3 = new ColorRGB(217, 26, 21);
		ColorRGB colorful4 = new ColorRGB(156, 217, 84);
		ColorRGB colorful5 = new ColorRGB(83, 70, 255);

		listOfColors.add(colorful1);
		listOfColors.add(colorful2);
		listOfColors.add(colorful3);
		listOfColors.add(colorful4);
		listOfColors.add(colorful5);
		cs = new ColorScheme("Mud", listOfColors, colorful2, colorful5);
		this.colorSchemes.put(cs.getName(), cs);

		listOfColors = new LinkedList<ColorRGB>();
		ColorRGB forest1 = new ColorRGB(94, 173, 0);
		ColorRGB forest2 = new ColorRGB(255, 188, 10);
		ColorRGB forest3 = new ColorRGB(189, 39, 53);
		ColorRGB forest4 = new ColorRGB(255, 119, 0);
		ColorRGB forest5 = new ColorRGB(81, 17, 84);

		listOfColors.add(forest1);
		listOfColors.add(forest2);
		listOfColors.add(forest3);
		listOfColors.add(forest4);
		listOfColors.add(forest5);
		cs = new ColorScheme("Forest", listOfColors, forest4, forest5);
		this.colorSchemes.put(cs.getName(), cs);

		listOfColors = new LinkedList<ColorRGB>();
		ColorRGB baw1 = new ColorRGB(0, 0, 0);
		ColorRGB baw2 = new ColorRGB(204, 204, 204);
		ColorRGB baw3 = new ColorRGB(255, 255, 255);
		ColorRGB baw4 = new ColorRGB(102, 102, 102);
		ColorRGB baw5 = new ColorRGB(51, 51, 51);

		listOfColors.add(baw1);
		listOfColors.add(baw2);
		listOfColors.add(baw3);
		listOfColors.add(baw4);
		listOfColors.add(baw5);
		cs = new ColorScheme("Grayscale", listOfColors, baw2, baw1);
		this.colorSchemes.put(cs.getName(), cs);

		this.activeSchemeName = colorScheme.getName();
	}

	public PlotConfiguration(DataTableColumn domainColumn, ColorScheme activeColorScheme,
			Map<String, ColorScheme> colorSchemes) {
		this(domainColumn);

		this.colorSchemes = colorSchemes;

		if (activeColorScheme != null) {
			setActiveColorScheme(activeColorScheme.getName());
		}

	}

	/**
	 * Private ctor, used only by {@link #clone()} method
	 */
	private PlotConfiguration(DomainConfigManager domainConfigManager, LegendConfiguration legendConfiguration) {
		this.linkAndBrushMaster = new LinkAndBrushMaster(this);
		this.linkAndBrushMaster.addLinkAndBrushListener(this);
		this.legendConfiguration = legendConfiguration;
		this.domainConfigManager = domainConfigManager;
		domainConfigManager.setPlotConfiguration(this);
		domainConfigManager.addDimensionConfigListener(this);
		legendConfiguration.addListener(this);
	}

	/**
	 * Returns the list of all {@link RangeAxisConfig}. All plot value configurations of a plot
	 * range axis *must* refer to the same column of the underlying DataTable.
	 *
	 * All x-Dimensions must use either the same categories, or all x-Dimensions must be numerical.
	 */
	public List<RangeAxisConfig> getRangeAxisConfigs() {
		return rangeAxisConfigs;
	}

	public void addRangeAxisConfig(int index, RangeAxisConfig rangeAxis) {

		rangeAxisConfigs.add(index, rangeAxis);
		fireRangeAxisAdded(index, rangeAxis);
		rangeAxis.addRangeAxisConfigListener(this);
	}

	/**
	 * Adds a PlotRangeAxis to this PlotConfiguration.
	 *
	 * See also the comment of getPlotRangeAxis() for the contract for the x-Dimension.
	 */
	public void addRangeAxisConfig(RangeAxisConfig rangeAxis) {
		addRangeAxisConfig(rangeAxisConfigs.size(), rangeAxis);
	}

	public void removeRangeAxisConfig(RangeAxisConfig rangeAxis) {
		final int index = rangeAxisConfigs.indexOf(rangeAxis);
		removeRangeAxisConfig(index);
	}

	public int getNextId() {
		++idCounter;
		return idCounter;
	}

	public void changeIndex(int index, RangeAxisConfig rangeAxis) {
		if (ListUtility.changeIndex(rangeAxisConfigs, rangeAxis, index)) {
			linkAndBrushMaster.clearZooming(false);
			fireRangeAxisMoved(index, rangeAxis);
		}
	}

	public int getRangeAxisCount() {
		return rangeAxisConfigs.size();
	}

	public void removeRangeAxisConfig(int index) {
		RangeAxisConfig rangeAxis = rangeAxisConfigs.get(index);
		rangeAxis.removeRangeAxisConfigListener(this);
		rangeAxisConfigs.remove(index);
		fireRangeAxisRemoved(index, rangeAxis);
	}

	public void addPlotConfigurationListener(PlotConfigurationListener l) {
		addPlotConfigurationListener(l, false);
	}

	/**
	 * This functions registers a {@link PlotConfigurationListener} as prioritized. This means that
	 * is is being informed at first when new events are being processed. At the moment only one
	 * prioritized listener may be registered at the same time. Check if you may register via
	 * getPrioritizedListenerCount().
	 */
	public void addPlotConfigurationListener(PlotConfigurationListener l, boolean prioritized) {
		if (prioritized) {
			synchronized (prioritizedListeners) {
				prioritizedListeners.add(new WeakReference<PlotConfigurationListener>(l));
			}
		} else {
			synchronized (defaultListeners) {
				defaultListeners.add(new WeakReference<PlotConfigurationListener>(l));
			}
		}
	}

	public int getPrioritizedListenerCount() {
		synchronized (prioritizedListeners) {
			return prioritizedListeners.size();
		}
	}

	/**
	 * Removes prioritized and default listeners if contained in one of these lists.
	 */
	public void removePlotConfigurationListener(PlotConfigurationListener l) {
		synchronized (prioritizedListeners) {
			Iterator<WeakReference<PlotConfigurationListener>> it = prioritizedListeners.iterator();
			while (it.hasNext()) {
				PlotConfigurationListener listener = it.next().get();
				if (listener == null || listener == l) {
					it.remove();
				}
			}
		}

		synchronized (defaultListeners) {
			Iterator<WeakReference<PlotConfigurationListener>> it_default = defaultListeners.iterator();
			while (it_default.hasNext()) {
				PlotConfigurationListener listener = it_default.next().get();
				if (listener == null || listener == l) {
					it_default.remove();
				}
			}
		}
	}

	/**
	 * @return the title
	 */
	public String getTitleText() {
		return titleText;
	}

	/**
	 * @param title
	 *            the title to set
	 */
	public void setTitleText(String title) {
		if (!Objects.equals(this.titleText, title)) {
			this.titleText = title;
			fireTitleChanged();
		}
	}

	/**
	 * @return the titleFont
	 */
	public Font getTitleFont() {
		return titleFont;
	}

	/**
	 * @param titleFont
	 *            the titleFont to set
	 */
	public void setTitleFont(Font titleFont) {
		if (!titleFont.equals(this.titleFont)) {
			this.titleFont = titleFont;
			fireTitleChanged();
		}
	}

	/**
	 * @return the backGroundColor
	 */
	public Color getPlotBackgroundColor() {
		return plotBackgroundColor;
	}

	/**
	 * @param backgroundColor
	 *            the backGroundColor to set
	 */
	public void setPlotBackgroundColor(Color backgroundColor) {
		if (!backgroundColor.equals(this.plotBackgroundColor)) {
			this.plotBackgroundColor = backgroundColor;
			firePlotBackgroundColorChanged();
		}
	}

	/**
	 * @return the axesFont
	 */
	public Font getAxesFont() {
		return axesFont;
	}

	/**
	 * @param axesFont
	 *            the axesFont to set
	 */
	public void setAxesFont(Font axesFont) {
		if (axesFont != this.axesFont) {
			this.axesFont = axesFont;
			fireAxesFontChanged();
		}
	}

	/**
	 * Returns the next free color for a value source based on the active color schema.
	 */
	public Color getNextColor() {
		int idx = 0;
		Set<Color> usedColors = new HashSet<Color>();
		for (ValueSource valueSource : getAllValueSources()) {
			usedColors.add(DataStructureUtils.setColorAlpha(valueSource.getSeriesFormat().getItemColor(), 255));
		}
		ColorScheme colorScheme = getActiveColorScheme();
		while (usedColors.contains(CategoricalColorProvider.getColorForCategoryIdx(idx, colorScheme))) {
			++idx;
		}
		return CategoricalColorProvider.getColorForCategoryIdx(idx, colorScheme);
	}

	/**
	 * @return the orientation
	 */
	public PlotOrientation getOrientation() {
		return orientation;
	}

	/**
	 * @param orientation
	 *            the orientation to set
	 */
	public void setOrientation(PlotOrientation orientation) {
		if (!this.orientation.equals(orientation)) {
			this.orientation = orientation;
			firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this, orientation));
		}
	}

	/**
	 * @return the domainAxisLineStroke
	 */
	public float getAxisLineWidth() {
		return axisLineWidth;
	}

	/**
	 * @return the domainAxisLineColor
	 */
	public Color getAxisLineColor() {
		return axisLineColor;
	}

	/**
	 * @param axisLineWidth
	 */
	public void setAxisLineWidth(float axisLineWidth) {
		if (axisLineWidth != this.axisLineWidth) {
			this.axisLineWidth = axisLineWidth;
			firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this, axisLineWidth));
		}
	}

	public void setAxisLineColor(Color axisLineColor) {
		if (!axisLineColor.equals(this.axisLineColor)) {
			this.axisLineColor = axisLineColor;
			firePlotConfigurationChanged(
					new PlotConfigurationChangeEvent(this, PlotConfigurationChangeType.AXIS_LINE_COLOR, axisLineColor));
		}
	}

	public Color getChartBackgroundColor() {
		return frameBackgroundColor;
	}

	/**
	 * @param frameBackgroundColor
	 *            the chartBackgroundColor to set
	 */
	public void setFrameBackgroundColor(Color frameBackgroundColor) {
		if (!frameBackgroundColor.equals(this.frameBackgroundColor)) {
			this.frameBackgroundColor = frameBackgroundColor;
			fireChartBackgroundChanged();
		}
	}

	private void fireRangeAxisMoved(int index, RangeAxisConfig rangeAxis) {
		if (!initializing) {
			firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this,
					PlotConfigurationChangeType.RANGE_AXIS_CONFIG_MOVED, rangeAxis, index));
		}
	}

	private void fireRangeAxisAdded(int index, RangeAxisConfig rangeAxis) {
		if (!initializing) {
			firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this,
					PlotConfigurationChangeType.RANGE_AXIS_CONFIG_ADDED, rangeAxis, index));
		}
	}

	private void fireRangeAxisRemoved(int index, RangeAxisConfig rangeAxis) {
		if (!initializing) {
			firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this,
					PlotConfigurationChangeType.RANGE_AXIS_CONFIG_REMOVED, rangeAxis, index));
		}
	}

	private void firePlotBackgroundColorChanged() {
		if (!initializing) {
			firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this,
					PlotConfigurationChangeType.PLOT_BACKGROUND_COLOR, plotBackgroundColor));
		}

	}

	private void fireLegendChanged(LegendConfigurationChangeEvent change) {
		if (!initializing) {
			firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this, change));
		}
	}

	private void fireChartBackgroundChanged() {
		if (!initializing) {
			firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this,
					PlotConfigurationChangeType.FRAME_BACKGROUND_COLOR, frameBackgroundColor));
		}
	}

	/**
	 * Sets the dimension config for a specific dimension.
	 *
	 * @param dimension
	 *            The dimension for which the config is specified. Must not be VALUE or DOMAIN.
	 * @param dimensionConfig
	 *            The new dimension config for the dimension. null means to remove the dimension
	 *            config.
	 */
	public void setDimensionConfig(PlotDimension dimension, DefaultDimensionConfig dimensionConfig) {
		DimensionConfig currentDimensionConfig = dimensionConfigMap.get(dimension);
		if (dimensionConfig != currentDimensionConfig) {
			if (dimensionConfig == null) {
				if (currentDimensionConfig != null) {
					currentDimensionConfig.removeDimensionConfigListener(this);
				}
				dimensionConfigMap.remove(dimension);
				fireDimensionConfigRemoved(dimension, currentDimensionConfig);
			} else {
				if (currentDimensionConfig != dimensionConfig) {
					if (currentDimensionConfig != null) {
						currentDimensionConfig.removeDimensionConfigListener(this);
					}

					dimensionConfigMap.put(dimension, dimensionConfig);
					dimensionConfig.addDimensionConfigListener(this);

					fireDimensionConfigAdded(dimension, dimensionConfig);
				}
			}
		}
	}

	private void informValueSourcesAboutDimensionChange(DimensionConfigChangeEvent e) {
		for (RangeAxisConfig rangeAxisConfig : rangeAxisConfigs) {
			for (ValueSource valueSource : rangeAxisConfig.getValueSources()) {
				valueSource.dimensionConfigChanged(e);
			}
		}
	}

	private void fireAxesFontChanged() {
		if (!initializing) {
			firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this, axesFont));
		}
	}

	/**
	 * Returns a list of all {@link ValueSource}s, no matter in which {@link RangeAxisConfig}
	 * they are located.
	 */
	public List<ValueSource> getAllValueSources() {
		List<ValueSource> resultList = new LinkedList<ValueSource>();
		for (RangeAxisConfig rangeAxisConfig : getRangeAxisConfigs()) {
			resultList.addAll(rangeAxisConfig.getValueSources());
		}
		return resultList;
	}

	/**
	 * Returns the dimension config for the specified dimension. May return <code>null</code> if not
	 * existing.
	 */
	public DimensionConfig getDimensionConfig(PlotDimension dimension) {
		if (dimension == PlotDimension.DOMAIN) {
			return domainConfigManager;
		}
		return dimensionConfigMap.get(dimension);
	}

	/**
	 * Returns all existing dimension configurations except the DOMAIN dimension config. This has to
	 * be fetched be getDomainConfigManager().
	 *
	 * @return
	 */
	public Map<PlotDimension, DefaultDimensionConfig> getDefaultDimensionConfigs() {
		return dimensionConfigMap;
	}

	public SeriesFormat getAutomaticSeriesFormatForNextValueSource(RangeAxisConfig rangeAxisConfig) {
		SeriesFormat seriesFormat = new SeriesFormat();

		List<ValueSource> valueSourceList = rangeAxisConfig.getValueSources();
		int size = valueSourceList.size();
		if (size > 0) {
			ValueSource lastValueSource = valueSourceList.get(valueSourceList.size() - 1);
			SeriesFormat lastFormat = lastValueSource.getSeriesFormat();
			// seriesFormat.setSeriesType(lastFormat.getSeriesType());
			if (lastFormat.getSeriesType() == VisualizationType.LINES_AND_SHAPES) {

				if (lastFormat.getItemShape() == ItemShape.NONE) {
					seriesFormat.setItemShape(ItemShape.NONE);
					LineStyle[] values = LineStyle.values();
					LineStyle nextItem = values[size % (values.length - 1) + 1];
					seriesFormat.setLineStyle(nextItem);
				} else {
					ItemShape[] values = SeriesFormat.ItemShape.values();
					ItemShape nextShape = values[size % (values.length - 1) + 1];
					seriesFormat.setItemShape(nextShape);
					seriesFormat.setLineStyle(lastFormat.getLineStyle());
				}

				seriesFormat.setLineWidth(lastFormat.getLineWidth());
				seriesFormat.setItemSize(lastFormat.getItemSize());
			}
			seriesFormat.setStackingMode(lastFormat.getStackingMode());
			seriesFormat.setOpacity(lastFormat.getOpacity());
			seriesFormat.setAreaFillStyle(lastFormat.getAreaFillStyle());
		}
		seriesFormat.setItemColor(getNextColor());

		return seriesFormat;
	}

	private void fireDimensionConfigAdded(PlotDimension dimension, DimensionConfig dimensionConfig) {
		if (!initializing) {
			// save old processing status
			boolean processEvents = isProcessingEvents();

			// set processing to false
			setProcessEvents(false);
			informValueSourcesAboutDimensionChange(
					new DimensionConfigChangeEvent(dimensionConfig, dimension, DimensionConfigChangeType.RESET));
			firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this,
					PlotConfigurationChangeType.DIMENSION_CONFIG_ADDED, dimension, dimensionConfig));
			setProcessEvents(processEvents); // Restore old state
		}
	}

	private void fireDimensionConfigRemoved(PlotDimension dimension, DimensionConfig dimensionConfig) {
		if (!initializing) {
			// save old processing status
			boolean processEvents = isProcessingEvents();

			// set processing to false
			setProcessEvents(false);
			informValueSourcesAboutDimensionChange(
					new DimensionConfigChangeEvent(dimensionConfig, dimension, DimensionConfigChangeType.RESET));
			firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this,
					PlotConfigurationChangeType.DIMENSION_CONFIG_REMOVED, dimension, dimensionConfig));
			setProcessEvents(processEvents); // Restore old state
		}
	}

	private void fireTitleChanged() {
		if (!initializing) {
			firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this, titleText));
		}
	}

	private Color getColorFromProperty(String propertyName, Color errorColor) {
		String propertyString = ParameterService.getParameterValue(propertyName);
		if (propertyString != null) {
			String[] colors = propertyString.split(",");
			if (colors.length != 3) {
				throw new IllegalArgumentException("Color '" + propertyString + "' defined as value for property '"
						+ propertyName + "' is not a vaild color. Colors must be of the form 'r,g,b'.");
			} else {
				Color color = new Color(Integer.parseInt(colors[0].trim()), Integer.parseInt(colors[1].trim()),
						Integer.parseInt(colors[2].trim()));
				return color;
			}
		} else {
			return errorColor;
		}
	}

	/**
	 * Tells the {@link PlotConfiguration} that the current event has been processed. If all
	 * Listeners have processed the last current event this triggers the next event to be processed
	 * if another is available.
	 */
	public synchronized void plotConfigurationChangeEventProcessed() {
		eventProcessed(false);
	}

	private synchronized void eventProcessed(boolean processedAtOnce) {
		listenersInformedCounter--;

		if (!processedAtOnce && listenersInformedCounter <= 0) {
			listenersInformedCounter = 0;
			resetCurrentEvent();
		}
	}

	private synchronized void resetCurrentEvent() {
		// no changes to the event list may be done while adding removing change
		// events
		informOfProcessingStatus(false);
		currentEvent = null;
		listenersInformedCounter = 0;
		processQueueEvent();
	}

	/**
	 * If this parameter is true, events that happen inside this PlotConfiguration, e.g. changes of
	 * RangeAxis, ValueSource etc. are process by the event queue. If this parameter is false, no
	 * events are processed.
	 *
	 * Best Practice to use it: boolean processing = isProcessingEvents(); setProcessEvents(false);
	 * OTHER CODE setProcessEvents(processing);
	 */
	public synchronized void setProcessEvents(Boolean process) {
		this.processEvents = process;

		if (processEvents) {
			processQueueEvent();
		}
	}

	public synchronized boolean isProcessingEvents() {
		return processEvents.booleanValue();
	}

	private void informOfProcessingStatus(boolean started) {
		// create a copy of listeners
		List<WeakReference<PlotConfigurationProcessingListener>> processingListenerCopy = new LinkedList<WeakReference<PlotConfigurationProcessingListener>>();

		synchronized (processingListeners) {
			processingListenerCopy.addAll(processingListeners);
		}

		// iterate over all listeners
		Iterator<WeakReference<PlotConfigurationProcessingListener>> defaultIt = processingListenerCopy.iterator();
		while (defaultIt.hasNext()) {
			WeakReference<PlotConfigurationProcessingListener> wrl = defaultIt.next();
			PlotConfigurationProcessingListener l = wrl.get();
			if (l != null) {
				if (started) {
					l.startProcessing();
				} else {
					l.endProcessing();
				}
			} else {
				defaultIt.remove();
			}
		}
	}

	private synchronized void processQueueEvent() {
		boolean booleanValue = false;
		// eventInformCounter has to be synchronize to prevent reaching 0 value
		// while informing listeners
		booleanValue = processEvents.booleanValue();
		if (booleanValue && currentEvent == null) {
			// if no current event is being processed

			// no changes to the event list may be done while processing a new
			// event

			// if there is an events that needs to be handled do so
			if (eventList.size() > 0) {
				// get new event
				currentEvent = eventList.get(0);
				currentEvent.setSource(this.clone());

				eventList.remove(0);

				informOfProcessingStatus(true);
			} else {

				// there are no recent events that have to be handled
				return;
			}

			// iterate over all listeners

			// first prioritizedListeners
			List<WeakReference<PlotConfigurationListener>> clonedPrioListeners = new LinkedList<WeakReference<PlotConfigurationListener>>();
			synchronized (prioritizedListeners) {
				clonedPrioListeners.addAll(prioritizedListeners);
			}

			Iterator<WeakReference<PlotConfigurationListener>> it = clonedPrioListeners.iterator();

			// // set counter to > 0, so that it will never drop to zero while
			// we are in the loop.
			// // This prevents the currentEvent becoming null while we are
			// still informing listeners.
			// // Will be decreased by 1 after we have informed all listeners.
			// listenersInformedCounter++;

			while (it.hasNext()) {
				WeakReference<PlotConfigurationListener> wrl = it.next();
				PlotConfigurationListener l = wrl.get();
				if (l != null) {
					// inform listener and increase informed counter
					listenersInformedCounter++;

					// if the event has been processed immediately decrease
					// informed counter
					if (l.plotConfigurationChanged(currentEvent)) {
						eventProcessed(true);
					}
				} else {
					// TODO this removes the element from the CLONED list, but should be removed
					// from original list
					it.remove();
				}
			}

			// then default listeners
			List<WeakReference<PlotConfigurationListener>> clonedDefaultListeners = new LinkedList<WeakReference<PlotConfigurationListener>>();
			synchronized (defaultListeners) {
				clonedDefaultListeners.addAll(defaultListeners);
			}
			Iterator<WeakReference<PlotConfigurationListener>> defaultIt = clonedDefaultListeners.iterator();
			while (defaultIt.hasNext()) {
				WeakReference<PlotConfigurationListener> wrl = defaultIt.next();
				PlotConfigurationListener l = wrl.get();
				if (l != null) {
					// inform listener and increase informed counter
					listenersInformedCounter++;

					// if the event has been processed immediately decrease
					// informed counter
					if (l.plotConfigurationChanged(currentEvent)) {
						eventProcessed(true);
					}
				} else {
					// TODO this removes the element from the CLONED list, but should be removed
					// from original list
					defaultIt.remove();
				}
			}

			// Decrease listener count, cause we increased it before (see
			// comment above)
			// listenersInformedCounter--;

			// if all event listeners have processed the event immediately
			if (listenersInformedCounter <= 0) {
				// remove event from list
				resetCurrentEvent();
			}

		}

		return;
	}

	/**
	 * Adds an {@link PlotConfigurationChangeEvent} to the event queue. Tries to process new event
	 * afterwards, if no other event is being processed.
	 *
	 * @param e
	 */
	private synchronized void addEventToQueue(PlotConfigurationChangeEvent e) {

		// no changes to the event list may be done while adding new change
		// events

		if (eventList.size() > 0) {
			PlotConfigurationChangeEvent plotConfigurationChangeEvent = eventList.get(0);
			if (plotConfigurationChangeEvent.getType() == PlotConfigurationChangeType.META_CHANGE) {
				plotConfigurationChangeEvent.addPlotConfigChangeEvent(this, e);
			} else {
				List<PlotConfigurationChangeEvent> events = new LinkedList<PlotConfigurationChangeEvent>();
				events.add(plotConfigurationChangeEvent);
				events.add(e);
				PlotConfigurationChangeEvent metaEvent = new PlotConfigurationChangeEvent(this, events);
				eventList.set(0, metaEvent);
			}
		} else {
			eventList.add(e);
		}

		processQueueEvent();
	}

	private void firePlotConfigurationChanged(PlotConfigurationChangeEvent e) {
		if (!initializing) {
			addEventToQueue(e);
		}
	}

	public DomainConfigManager getDomainConfigManager() {
		return domainConfigManager;
	}

	@Override
	public void dimensionConfigChanged(DimensionConfigChangeEvent change) {
		DimensionConfigChangeType type = change.getType();

		// save old processing status
		boolean processEvents = isProcessingEvents();

		// set processing to false
		setProcessEvents(false);

		switch (type) {
			case ABOUT_TO_CHANGE_GROUPING:
				changingGrouping = true;
				break;
			case GROUPING_CHANGED:
				ValueGroupingChangeEvent groupingChange = change.getGroupingChangeEvent();
				if (groupingChange.getType() == ValueGroupingChangeType.RESET) {
					if (changingGrouping) {
						changingGrouping = false;
					}
				}

				informValueSourcesAboutDimensionChange(change);
				linkAndBrushMaster.clearZooming(false);
				firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this, change));
				break;
			case RANGE:
				ValueRangeChangeEvent rangeChange = change.getValueRangeChangedEvent();
				if (rangeChange.getType() == ValueRangeChangeType.RESET) {
				}
				informValueSourcesAboutDimensionChange(change);
				linkAndBrushMaster.clearZooming(false);
				firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this, change));
				break;
			default:
				if (!changingGrouping) {
					informValueSourcesAboutDimensionChange(change);
					linkAndBrushMaster.clearZooming(false);
					firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this, change));
				}
				break;
		}
		setProcessEvents(processEvents);

	}

	@Override
	public void rangeAxisConfigChanged(RangeAxisConfigChangeEvent e) {
		switch (e.getType()) {
			case AUTO_NAMING:
			case LABEL:
				break;
			case VALUE_SOURCE_CHANGED:
				ValueSourceChangeEvent valueSourceChange = e.getValueSourceChange();
				boolean shouldBreak = false;
				switch (valueSourceChange.getType()) {
					case SERIES_FORMAT_CHANGED:
						shouldBreak = true;
						break;
					default:
						linkAndBrushMaster.clearZooming(false);
				}
				if (shouldBreak) {
					break;
				}
			default:
				linkAndBrushMaster.clearRangeAxisZooming(false);

		}
		firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this, e));
	}

	public List<PlotConfigurationError> getErrors() {
		List<PlotConfigurationError> errors = new LinkedList<PlotConfigurationError>();

		errors.addAll(domainConfigManager.getErrors());
		errors.addAll(linkAndBrushMaster.getErrors());

		for (DimensionConfig dimensionConfig : dimensionConfigMap.values()) {
			errors.addAll(dimensionConfig.getErrors());
		}

		for (RangeAxisConfig rangeAxisConfig : rangeAxisConfigs) {
			errors.addAll(rangeAxisConfig.getErrors());
		}

		return errors;
	}

	public List<PlotConfigurationError> getWarnings() {
		List<PlotConfigurationError> warnings = new LinkedList<PlotConfigurationError>();

		warnings.addAll(domainConfigManager.getWarnings());
		warnings.addAll(linkAndBrushMaster.getWarnings());

		for (DimensionConfig dimensionConfig : dimensionConfigMap.values()) {
			warnings.addAll(dimensionConfig.getWarnings());

			if (!isDimensionUsed(dimensionConfig.getDimension())) {
				warnings.add(
						new PlotConfigurationError("dimension_config_not_used", dimensionConfig.getDimension().getName()));
			}

		}

		boolean emptyAxis = false;
		for (RangeAxisConfig rangeAxisConfig : rangeAxisConfigs) {
			warnings.addAll(rangeAxisConfig.getWarnings());
			if (rangeAxisConfig.getSize() == 0) {
				emptyAxis = true;
			}
		}

		if (emptyAxis) {
			warnings.add(new PlotConfigurationError("no_data_configuration_assigned"));
		}

		return warnings;
	}

	public boolean isDimensionUsed(PlotDimension dimension) {
		if (dimension != PlotDimension.DOMAIN) {
			DimensionConfig dimensionConfig = getDimensionConfig(dimension);
			if (dimensionConfig == null) {
				return false;
			}
			if (!dimensionConfig.isGrouping()) {
				if (getDomainConfigManager().getGroupingState() == GroupingState.GROUPED) {
					return false;
				}
			}
			return true;
		} else {
			return true;
		}
	}

	public boolean isValid() {
		return getErrors().isEmpty();
	}

	public double getMinShapeSize() {
		return MIN_SHAPE_SCALING_FACTOR;
	}

	public double getMaxShapeSize() {
		return MAX_SHAPE_SCALING_FACTOR;
	}

	@Override
	public PlotConfiguration clone() {
		PlotConfiguration clone = new PlotConfiguration(domainConfigManager.clone(), legendConfiguration.clone());

		clone.initializing = true;

		clone.domainConfigManager.setPlotConfiguration(clone);

		clone.axesFont = axesFont;
		clone.changingGrouping = changingGrouping;
		clone.changingRange = changingRange;
		clone.frameBackgroundColor = frameBackgroundColor;

		// copy color schemes
		clone.colorSchemes = new HashMap<String, ColorScheme>();
		for (String key : colorSchemes.keySet()) {
			clone.colorSchemes.put(key, colorSchemes.get(key).clone());
		}
		clone.activeSchemeName = activeSchemeName;

		clone.axisLineColor = axisLineColor;
		clone.axisLineWidth = axisLineWidth;
		clone.orientation = orientation;
		clone.plotBackgroundColor = plotBackgroundColor;
		clone.titleFont = titleFont;
		clone.titleText = titleText;
		clone.titleColor = titleColor;

		clone.idCounter = idCounter;

		// clone dimension configs
		for (Map.Entry<PlotDimension, DefaultDimensionConfig> entry : dimensionConfigMap.entrySet()) {
			clone.setDimensionConfig(entry.getKey(), entry.getValue().clone());
		}

		// clone range axis configs
		for (RangeAxisConfig rangeAxisConfig : getRangeAxisConfigs()) {
			clone.addRangeAxisConfig(rangeAxisConfig.clone());
		}

		// update domain config manager on cloned value sources
		for (ValueSource clonedValueSource : clone.getAllValueSources()) {
			clonedValueSource.setDomainConfigManager(clone.getDomainConfigManager());
		}

		clone.linkAndBrushMaster = linkAndBrushMaster.clone(clone);

		clone.initializing = false;

		return clone;
	}

	public ColorScheme getDefaultColorScheme() {
		List<ColorRGB> listOfColors = new LinkedList<ColorRGB>();
		Color minColor = getColorFromProperty(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_MINCOLOR, Color.BLUE);
		ColorRGB minColorRGB = ColorRGB.convertColorToColorRGB(minColor);
		listOfColors.add(minColorRGB);
		Color maxColor = getColorFromProperty(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_MAXCOLOR, Color.RED);
		ColorRGB maxColorRGB = ColorRGB.convertColorToColorRGB(maxColor);
		listOfColors.add(maxColorRGB);

		Color third = ContinuousColorProvider.getColorForValue(3, 255, false, 0, 4, minColor, maxColor);
		listOfColors.add(ColorRGB.convertColorToColorRGB(third));

		Color second = ContinuousColorProvider.getColorForValue(2, 255, false, 0, 5, minColor, maxColor);
		listOfColors.add(ColorRGB.convertColorToColorRGB(second));

		Color first = ContinuousColorProvider.getColorForValue(1, 255, false, 0, 5, minColor, maxColor);
		listOfColors.add(ColorRGB.convertColorToColorRGB(first));

		return new ColorScheme(I18N.getGUILabel("plotter.default_color_scheme_name.label"), listOfColors, minColorRGB,
				maxColorRGB);
	}

	/**
	 * @param colorSchemes
	 *            the colorScheme to set
	 */
	public void setColorSchemes(Map<String, ColorScheme> colorSchemes, String activeColorSchemeName) {
		boolean changed = false;

		ColorScheme oldActiveScheme = getActiveColorScheme();
		if (!colorSchemes.equals(this.colorSchemes)) {
			this.colorSchemes = colorSchemes;
			changed = true;
		}

		if (!oldActiveScheme.equals(colorSchemes.get(activeColorSchemeName))) {
			setActiveScheme(activeColorSchemeName);
			changed = true;
		}
		if (changed) {
			firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this, getActiveColorScheme()));
		}
	}

	public Map<String, ColorScheme> getColorSchemes() {
		return colorSchemes;
	}

	private void setActiveScheme(String name) {
		this.activeSchemeName = name;

		// fetch new active color scheme
		ColorScheme colorScheme = colorSchemes.get(name);
		if (colorScheme == null) {
			colorSchemes.put(name, getDefaultColorScheme());
		}

		// save old processing status
		boolean processEvents = isProcessingEvents();

		// set processing to false
		setProcessEvents(false);

		for (PlotDimension dimension : PlotDimension.values()) {
			DimensionConfig dimConf = getDimensionConfig(dimension);
			if (dimConf != null) {
				dimConf.colorSchemeChanged();
			}
		}

		setProcessEvents(processEvents);
	}

	public void setActiveColorScheme(String name) {

		// check if colorscheme has changed
		if (!this.activeSchemeName.equals(name)) {
			setActiveScheme(name);

			firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this, getActiveColorScheme()));
		}
	}

	/**
	 * @return the colorScheme
	 */
	public ColorScheme getActiveColorScheme() {
		ColorScheme activeColorScheme = colorSchemes.get(activeSchemeName);
		if (activeColorScheme == null) {
			return getDefaultColorScheme();
		}
		return activeColorScheme;
	}

	public PlotConfigurationChangeEvent getCurrentEvent() {
		return currentEvent;
	}

	/**
	 * Adds a {@link ColorScheme} to the existing ones. If another {@link ColorScheme} with same
	 * name already exists it will be overwritten.
	 */
	public void addColorScheme(ColorScheme colorScheme) {
		colorSchemes.put(colorScheme.getName(), colorScheme);
	}

	/**
	 * Adds a {@link ColorScheme} to the existing ones. If another {@link ColorScheme} with same
	 * name already exists it will be overwritten. Afterwards it is set to be the active
	 * {@link ColorScheme}.
	 */
	public void addColorSchemeAndSetActive(ColorScheme colorScheme) {
		addColorScheme(colorScheme);
		setActiveScheme(colorScheme.getName());
		firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this, getActiveColorScheme()));
	}

	/**
	 * Removes {@link ColorScheme} with parameter name if present.
	 */
	public void removeColorScheme(String name) {
		colorSchemes.remove(name);
	}

	/**
	 * @return the link and brush master
	 */
	public LinkAndBrushMaster getLinkAndBrushMaster() {
		return linkAndBrushMaster;
	}

	/**
	 * Returns whether grouping is required or not. Grouping of new {@link ValueSource}s is required
	 * if a containing {@link ValueSource} is grouped and the selected grouping is categorical.
	 *
	 * Lives here and not in {@link RangeAxisConfig} because needs the domain config manager of this
	 * {@link PlotConfiguration}.
	 */
	public boolean isGroupingRequiredForNewValueSource(RangeAxisConfig rangeAxis) {
		List<ValueSource> valueSources = rangeAxis.getValueSources();
		for (ValueSource source : valueSources) {
			if (source.isUsingDomainGrouping() && getDomainConfigManager().isNominal()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void selectedLinkAndBrushRectangle(LinkAndBrushSelection e) {
		linkAndBrushMaster.selectedLinkAndBrushRectangle(e);
	}

	public static int getMaxAllowedValueCount() {
		String parameterValue = ParameterService.getParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_ROWS_MAXIMUM);
		if (parameterValue != null) {
			return Integer.parseInt(parameterValue);
		} else {
			return PlotConfiguration.GUI_PLOTTER_ROWS_MAXIMUM_IF_RAPIDMINER_PROPERTY_NOT_READABLE;
		}
	}

	@Override
	public void legendConfigurationChanged(LegendConfigurationChangeEvent change) {
		fireLegendChanged(change);
	}

	public LegendConfiguration getLegendConfiguration() {
		return legendConfiguration;
	}

	/**
	 * Removes all value sources, dimension configs and range axes, and resets all options to their
	 * default values.
	 */
	public void resetToDefaults() {
		boolean processing = isProcessingEvents();

		// do it this strange way to counter concurrent modification exception
		while (!getRangeAxisConfigs().isEmpty()) {
			removeRangeAxisConfig(0);
		}

		// do it this strange way to counter concurrent modification exception
		while (!getDefaultDimensionConfigs().isEmpty()) {
			setDimensionConfig(getDefaultDimensionConfigs().keySet().iterator().next(), null);
		}

		domainConfigManager.resetToDefaults();

		setAxesFont(DEFAULT_AXES_FONT);
		setFrameBackgroundColor(DEFAULT_FRAME_BACKGROUND_COLOR);
		setPlotBackgroundColor(DEFAULT_PLOT_BACKGROUND_COLOR);
		setOrientation(DEFAULT_PLOT_ORIENTATION);
		setTitleText(DEFAULT_TITLE_TEXT);
		setAxisLineWidth(DEFAULT_AXIS_LINE_WIDTH);
		setAxisLineColor(DEFAULT_AXIS_COLOR);

		legendConfiguration.resetToDefaults();

		addRangeAxisConfig(new RangeAxisConfig(null, this));

		setProcessEvents(processing);
	}

	public void addPlotConfigurationProcessingListener(PlotConfigurationProcessingListener l) {
		synchronized (processingListeners) {
			processingListeners.add(new WeakReference<PlotConfigurationProcessingListener>(l));
		}
	}

	public void removePlotConfigurationProcessingListener(PlotConfigurationProcessingListener l) {
		synchronized (processingListeners) {
			Iterator<WeakReference<PlotConfigurationProcessingListener>> it = processingListeners.iterator();
			while (it.hasNext()) {
				WeakReference<PlotConfigurationProcessingListener> listenerRef = it.next();
				if (l.equals(listenerRef.get())) {
					it.remove();
				}
			}
		}
	}

	public RangeAxisConfig getRangeAxisConfigById(int id) {
		for (RangeAxisConfig rangeAxisConfig : getRangeAxisConfigs()) {
			if (rangeAxisConfig.getId() == id) {
				return rangeAxisConfig;
			}
		}

		return null;
	}

	/**
	 * Returns the index of the {@link RangeAxisConfig} with a given id.
	 *
	 * @param id
	 * @return The index of the range axis, or -1 if no such range axis exists.
	 */
	public int getIndexOfRangeAxisConfigById(int id) {
		int idx = 0;
		for (RangeAxisConfig rangeAxisConfig : getRangeAxisConfigs()) {
			if (rangeAxisConfig.getId() == id) {
				return idx;
			}
			++idx;
		}

		return -1;
	}

	public DefaultDimensionConfig getDefaultDimensionConfigById(int dimensionConfigId) {
		if (domainConfigManager.getDomainConfig(true).getId() == dimensionConfigId) {
			return domainConfigManager.getDomainConfig(true);
		}
		if (domainConfigManager.getDomainConfig(false).getId() == dimensionConfigId) {
			return domainConfigManager.getDomainConfig(false);
		}

		for (DefaultDimensionConfig dimensionConfig : getDefaultDimensionConfigs().values()) {
			if (dimensionConfig.getId() == dimensionConfigId) {
				return dimensionConfig;
			}
		}
		return null;
	}

	/**
	 * @return
	 */
	public Color getTitleColor() {
		return titleColor;
	}

	public void setTitleColor(Color titleColor) {
		this.titleColor = titleColor;
		fireTitleChanged();
	}

	/**
	 * This function can be used to fire a TRIGGER_REPLOT event.
	 */
	public void triggerReplot() {
		firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this));
	}

	@Override
	public void linkAndBrushUpdate(LinkAndBrushSelection e) {
		firePlotConfigurationChanged(new PlotConfigurationChangeEvent(this, e));
	}
}
