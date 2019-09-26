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
package com.rapidminer.gui.new_plotter.engine.jfreechart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.block.ColumnArrangement;
import org.jfree.chart.block.FlowArrangement;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.Dataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.VerticalAlignment;

import com.rapidminer.gui.new_plotter.ChartPlottimeException;
import com.rapidminer.gui.new_plotter.ConfigurationChangeResponse;
import com.rapidminer.gui.new_plotter.MasterOfDesaster;
import com.rapidminer.gui.new_plotter.PlotConfigurationError;
import com.rapidminer.gui.new_plotter.PlotConfigurationQuickFix;
import com.rapidminer.gui.new_plotter.configuration.AxisParallelLineConfiguration;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn.ValueType;
import com.rapidminer.gui.new_plotter.configuration.DefaultDimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.LegendConfiguration;
import com.rapidminer.gui.new_plotter.configuration.LegendConfiguration.LegendPosition;
import com.rapidminer.gui.new_plotter.configuration.LineFormat.LineStyle;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.IndicatorType;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.ItemShape;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.StackingMode;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.VisualizationType;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.data.DimensionConfigData;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.data.ValueSourceData;
import com.rapidminer.gui.new_plotter.engine.PlotEngine;
import com.rapidminer.gui.new_plotter.engine.jfreechart.actions.AddParallelLineAction;
import com.rapidminer.gui.new_plotter.engine.jfreechart.actions.ClearParallelLinesAction;
import com.rapidminer.gui.new_plotter.engine.jfreechart.actions.CopyChartAction;
import com.rapidminer.gui.new_plotter.engine.jfreechart.actions.ManageParallelLinesAction;
import com.rapidminer.gui.new_plotter.engine.jfreechart.actions.ManageZoomAction;
import com.rapidminer.gui.new_plotter.engine.jfreechart.dataset.ValueSourceToMultiValueCategoryDatasetAdapter;
import com.rapidminer.gui.new_plotter.engine.jfreechart.legend.ColoredBlockContainer;
import com.rapidminer.gui.new_plotter.engine.jfreechart.legend.SmartLegendTitle;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.LinkAndBrushChartPanel;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.plots.LinkAndBrushCategoryPlot;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.plots.LinkAndBrushXYPlot;
import com.rapidminer.gui.new_plotter.listener.JFreeChartPlotEngineListener;
import com.rapidminer.gui.new_plotter.listener.PlotConfigurationListener;
import com.rapidminer.gui.new_plotter.listener.PlotConfigurationProcessingListener;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent.DimensionConfigChangeType;
import com.rapidminer.gui.new_plotter.listener.events.LegendConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent.PlotConfigurationChangeType;
import com.rapidminer.gui.new_plotter.listener.events.RangeAxisConfigChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.RangeAxisConfigChangeEvent.RangeAxisConfigChangeType;
import com.rapidminer.gui.new_plotter.listener.events.SeriesFormatChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.ValueSourceChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.ValueSourceChangeEvent.ValueSourceChangeType;
import com.rapidminer.gui.plotter.CoordinateTransformation;
import com.rapidminer.gui.plotter.NullCoordinateTransformation;
import com.rapidminer.gui.tools.MultiSwingWorker;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;


/**
 * This class creates a JFreeChart from a PlotConfiguration.
 *
 * Using the listener mechanism, the chart is updated or recreated each time the plot configuration
 * changes.
 *
 * Classes implementing the interface {@link JFreeChartPlotEngineListener} can register at the
 * Plotter2D via addListener() and will be informed when the chart has changed.
 *
 *
 * Dataset and Renderer Chart: Series type categorical grouped stacking error bars Renderer Dataset
 * Plot Constraints L - irrelevant n/a - XYLineAndShapeRenderer XYDataset, e.g. DefaultXYDataset
 * XYPlot L - irrelevant n/a bars XYErrorRenderer IntervalXYDataset XYPlot L - irrelevant n/a band
 * DeviationRenderer IntervalXYDataset XYPlot L - irrelevant n/a difference XYDifferenceRenderer
 * XYDataset with 2 series XYPlot distinct values only L X - n/a - ScatterRenderer
 * MultiValueCategoryDataset CategoryPlot no lines possible, color axis must be grouped (if present)
 * L X X n/a - LineAndShapeRenderer CategoryDataset CategoryPlot L X irrelevant n/a bars
 * StatisticalLineAndShapeRenderer StatisticalCategoryDataset CategoryPlot no duplicate values on
 * domain axis allowed, only symmetric error L X irrelevant n/a band not supported by JFreeChart n/a
 * n/a L X irrelevant n/a difference not supported by JFreeChart n/a n/a B - irrelevant none -
 * ClusteredXYBarRenderer IntervalXYDataset (XYSeriesCollection for fixed width binning/no binning,
 * DefaultIntervalXYDataset oXYPlot B - irrelevant absolute - StackedXYBarRenderer TableXYDataset
 * XYPlot B - irrelevant percentage - StackedXYBarRenderer TableXYDataset XYPlot B - irrelevant
 * irrelevant != none not supported by JFreeChart n/a n/a B X irrelevant none - BarRenderer
 * CategoryDataset CategoryPlot B X irrelevant absolute - StackedBarRenderer CategoryDataset
 * [(IntervalXYDataset && TableXYDataset), e.g. DefaultTableXYDataset] CategoryPlot B X irrelevant
 * percentage - StackedBarRenderer CategoryDataset [(IntervalXYDataset && TableXYDataset), e.g.
 * DefaultTableXYDataset] CategoryPlot B X irrelevant none bars StatisticalBarRenderer
 * StatisticalCategoryDataset CategoryPlot B X irrelevant none band, diff not supported by
 * JFreeChart n/a n/a B X irrelevant != none != none not supported by JFreeChart n/a n/a A -
 * irrelevant none - XYAreaRenderer2 XYDataset XYPlot A - irrelevant absolute -
 * StackedXYAreaRenderer2 TableXYDataset XYPlot A - irrelevant percentage - not supported by
 * JFreeChart n/a n/a A X irrelevant none - AreaRenderer CategoryDataset, e.g.
 * DefaultCategoryDataset CategoryPlot A X irrelevant absolute - StackedAreaRenderer
 * CategoryDataset, e.g. DefaultCategoryDataset CategoryPlot A X irrelevant percentage -
 * StackedAreaRenderer CategoryDataset, e.g. DefaultCategoryDataset CategoryPlot A irrelevant
 * irrelevant irrelevant != none not supported by JFreeChart n/a n/a
 *
 * Series types: L -> Lines and Shapes B -> Bar A -> Area
 *
 *
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class JFreeChartPlotEngine
		implements PlotEngine, PlotConfigurationListener, PlotConfigurationProcessingListener, LegendItemSource {

	private boolean initializing = false;

	private List<WeakReference<JFreeChartPlotEngineListener>> listeners = new LinkedList<WeakReference<JFreeChartPlotEngineListener>>();

	private PlotInstance plotInstance;
	private PlotInstance nextPlotInstance = null;

	private final PlotInstanceLegendCreator legendCreator = new PlotInstanceLegendCreator();

	private transient LegendItemCollection cachedLegendItems = null;

	private final LinkAndBrushChartPanel chartPanel;

	private AtomicBoolean updatingChart;

	private boolean currentChartIsValid = false;

	private MultiAxesCrosshairOverlay crosshairOverlay = new MultiAxesCrosshairOverlay();

	private Object nextPlotInstanceLock = new Object();

	/** the popup menu shown after a popup action on the chart */
	private JPopupMenu popupMenuChart;

	/** the mouse listener for the popup menu */
	private MouseListener popupMenuListener;

	/** the add crosshair action */
	private AddParallelLineAction addParallelLineAction;

	/**
	 * This is a transformation which transforms the components coordinates to screen coordinates.
	 * If is null, no transformation is needed.
	 */
	private transient CoordinateTransformation coordinateTransformation = new NullCoordinateTransformation();

	{
		// create popup menu for chart here
		popupMenuChart = new JPopupMenu();
		popupMenuChart.add(new CopyChartAction(this));
		popupMenuChart.addSeparator();
		popupMenuChart.add(new ManageZoomAction(this));
		popupMenuChart.addSeparator();
		addParallelLineAction = new AddParallelLineAction(this);
		popupMenuChart.add(addParallelLineAction);
		popupMenuChart.add(new ManageParallelLinesAction(this));
		popupMenuChart.addSeparator();
		popupMenuChart.add(new ClearParallelLinesAction(this));

		// popup listener for the chart panel
		popupMenuListener = new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}

			private void maybeShowPopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					addParallelLineAction.setPopupLocation(e.getPoint());
					coordinateTransformation.showPopupMenu(new Point(e.getX(), e.getY()), chartPanel, popupMenuChart);
				}
			}
		};
	}

	public JFreeChartPlotEngine(PlotInstance plotInstanceForEngine, boolean zoomInOnSelection) {

		this.plotInstance = plotInstanceForEngine;
		updatingChart = new AtomicBoolean(false);

		chartPanel = new LinkAndBrushChartPanel(new JFreeChart(new CategoryPlot()), 50, 50, 50, 50, zoomInOnSelection);
		chartPanel.setMinimumDrawWidth(50);
		chartPanel.setMinimumDrawHeight(50);
		chartPanel.setMaximumDrawWidth(10000);
		chartPanel.setMaximumDrawHeight(10000);
		chartPanel.addMouseListener(popupMenuListener);

		subscribeAtPlotInstance(plotInstance);
	}

	public JFreeChartPlotEngine(PlotInstance plotInstanceForEngine, boolean zoomInOnSelection, boolean useBuffer) {

		this.plotInstance = plotInstanceForEngine;
		updatingChart = new AtomicBoolean(false);

		chartPanel = new LinkAndBrushChartPanel(new JFreeChart(new CategoryPlot()), 50, 50, 50, 50, zoomInOnSelection,
				useBuffer);
		chartPanel.setMinimumDrawWidth(50);
		chartPanel.setMinimumDrawHeight(50);
		chartPanel.setMaximumDrawWidth(10000);
		chartPanel.setMaximumDrawHeight(10000);
		chartPanel.addMouseListener(popupMenuListener);

		subscribeAtPlotInstance(plotInstance);
	}

	private void subscribeAtPlotInstance(PlotInstance plotInstance) {
		initializing = true;
		// register as listener
		PlotConfiguration masterPlotConfiguration = plotInstance.getMasterPlotConfiguration();
		masterPlotConfiguration.addPlotConfigurationListener(this);
		masterPlotConfiguration.addPlotConfigurationProcessingListener(this);
		chartPanel.addLinkAndBrushSelectionListener(masterPlotConfiguration);
		initializing = false;
	}

	public boolean updatingChart() {
		return updatingChart.get();
	}

	/**
	 * Use to retrieve the current {@link JFreeChart} chart, stored and shown in the
	 * {@link ChartPanel}. The chart may not reflect the current {@link PlotConfiguration} and may
	 * be replaced by another chart shortly after it has been fetched.
	 */
	public JFreeChart getCurrentChart() {
		JFreeChart currentChart = chartPanel.getChart();
		if (currentChart == null) {
			return new JFreeChart(new CategoryPlot());
		}
		return currentChart;
	}

	/**
	 * Trigger an update of the {@link JFreeChart} that is stored in the {@link ChartPanel}. The
	 * update is performed by using a {@link MultiSwingWorker} thread. First the new Chart is created and
	 * afterwards the new chart is stored in the {@link ChartPanel}.
	 *
	 * @param informPlotConfigWhenDone
	 *            should inform the {@link PlotConfiguration} that the worker thread is done?
	 */
	private synchronized void updateChartPanelChart(final boolean informPlotConfigWhenDone) {
		updatingChart.getAndSet(true);

		MultiSwingWorker<JFreeChart, Void> updateChartWorker = new MultiSwingWorker<JFreeChart, Void>() {

			@Override
			public JFreeChart doInBackground() throws Exception {
				try {
					if (!isPlotInstanceValid()) {
						return null;
					}
					try {
						invalidateCache();
						JFreeChart createdChart = createChart();
						updateLegendItems();
						checkWarnings();
						currentChartIsValid = true;
						return createdChart;
					} catch (ChartPlottimeException e) {
						handlePlottimeException(e);
						return null;
					}
				} catch (Exception e) {
					e.printStackTrace();
					SwingTools.showFinalErrorMessage("generic_plotter_error", e, true, new Object[] {});
					handlePlottimeException(new ChartPlottimeException("generic_plotter_error"));
					return null;
				}
			}

			@Override
			public void done() {
				try {
					JFreeChart chart = null;
					try {
						chart = get(60, TimeUnit.SECONDS);
						updatingChartPanelChartDone();
					} catch (Exception e) {
						updatingChartPanelChartDone();
						e.printStackTrace();
						handlePlottimeException(new ChartPlottimeException("generic_plotter_error"));
						return;
					}
					if (chart == null) {
						currentChartIsValid = false;
						chart = new JFreeChart(new CategoryPlot());
					}
					updateChartPanel(chart);

					// informs plotConfig that the repaint event has been processed
					if (informPlotConfigWhenDone) {
						plotInstance.getMasterPlotConfiguration().plotConfigurationChangeEventProcessed();
					}
				} catch (Exception e) {
					e.printStackTrace();
					SwingTools.showFinalErrorMessage("generic_plotter_error", e);
				}

			}

		};

		updateChartWorker.start();
	}

	/**
	 * Updates the chart panel to show the provided {@link JFreeChart}. If the chart is
	 * <code>null</code> an empty Plot will be shown. If an overlay has been defined and the chart
	 * is a {@link XYPlot} the overlay is also drawn.
	 */
	private synchronized void updateChartPanel(final JFreeChart chart) {
		Runnable updateChartPanelRunnable = new Runnable() {

			@Override
			public void run() {
				if (chart != chartPanel.getChart()) {
					if (chart == null) {
						chartPanel.setChart(new JFreeChart(new CategoryPlot()));

						fireChartChanged(new JFreeChart(new CategoryPlot()));
					} else {
						RenderingHints renderingHints = chart.getRenderingHints();

						// enable antialiasing
						renderingHints
								.add(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));

						// disable normalization (normalization tries to draw the center of strokes
						// at whole pixels, which causes e.g.
						// scaled shapes to appear more like potatoes than like circles)
						renderingHints.add(
								new RenderingHints(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE));
						chart.setRenderingHints(renderingHints);

						chartPanel.setChart(chart);
						fireChartChanged(chart);
					}
				}
				if (chart != null) {
					chartPanel.removeOverlay(crosshairOverlay);
					crosshairOverlay = new MultiAxesCrosshairOverlay();

					if (chart.getPlot() instanceof XYPlot) {
						// add overlays for range axes
						int axisIdx = 0;
						for (RangeAxisConfig rangeAxisConfig : plotInstance.getCurrentPlotConfigurationClone()
								.getRangeAxisConfigs()) {
							for (AxisParallelLineConfiguration line : rangeAxisConfig.getCrossHairLines().getLines()) {
								Crosshair crosshair = new Crosshair(line.getValue(), line.getFormat().getColor(),
										line.getFormat().getStroke());
								crosshairOverlay.addRangeCrosshair(axisIdx, crosshair);
							}
							++axisIdx;
						}

						// add overlays for domain axis
						for (AxisParallelLineConfiguration line : plotInstance.getCurrentPlotConfigurationClone()
								.getDomainConfigManager().getCrosshairLines().getLines()) {
							Crosshair crosshair = new Crosshair(line.getValue(), line.getFormat().getColor(),
									line.getFormat().getStroke());
							crosshairOverlay.addDomainCrosshair(crosshair);
						}
						chartPanel.addOverlay(crosshairOverlay);
					}
				}
			}
		};

		if (SwingUtilities.isEventDispatchThread()) {
			updateChartPanelRunnable.run();
		} else {
			SwingUtilities.invokeLater(updateChartPanelRunnable);
		}

	}

	public void endInitializing() {
		if (initializing) {
			initializing = false;
			plotInstance.triggerReplot();
		}
	}

	public void startInitializing() {
		initializing = true;
	}

	/**
	 * Creates a new {@link JFreeChart} using the plotConfiguration of this Plotter2D.
	 */
	private JFreeChart createChart() throws ChartPlottimeException {
		Plot plot = createPlot();

		if (plot == null) {
			throw new ChartPlottimeException("The plot created was a NULL plot.");
		}

		JFreeChart chart = new JFreeChart(plot);
		formatChart(chart);
		return chart;
	}

	/**
	 * Sets all the format options on the given chart like fonts, chart title, legend, legend
	 * position etc.
	 */
	private void formatChart(JFreeChart chart) {
		Plot plot = chart.getPlot();
		PlotConfiguration currentPlotConfigurationClone = plotInstance.getCurrentPlotConfigurationClone();

		// set plot background color
		plot.setBackgroundPaint(currentPlotConfigurationClone.getPlotBackgroundColor());

		formatLegend(chart);

		// set chart background color
		chart.setBackgroundPaint(currentPlotConfigurationClone.getChartBackgroundColor());

		// add title to chart
		String text = currentPlotConfigurationClone.getTitleText();
		if (text == null) {
			chart.setTitle(text);
		} else {
			Font font = currentPlotConfigurationClone.getTitleFont();
			if (font == null) {
				font = FontTools.getFont(Font.DIALOG, Font.PLAIN, 10);
			}

			TextTitle textTitle = new TextTitle(text, font);
			textTitle.setPaint(currentPlotConfigurationClone.getTitleColor());

			chart.setTitle(textTitle);
		}
	}

	/**
	 * @param chart
	 */
	private void formatLegend(JFreeChart chart) {
		List<LegendTitle> legendTitles = createLegendTitles();

		while (chart.getLegend() != null) {
			chart.removeLegend();
		}
		for (LegendTitle legendTitle : legendTitles) {
			chart.addLegend(legendTitle);
		}

		// set legend font
		Font legendFont = plotInstance.getCurrentPlotConfigurationClone().getLegendConfiguration().getLegendFont();
		if (legendFont != null) {
			for (LegendTitle legendTitle : legendTitles) {
				legendTitle.setItemFont(legendFont);
			}
		}
	}

	private void invalidateCache() {
		cachedLegendItems = null;
	}

	/**
	 * Is called to clear the {@link MasterOfDesaster}, invalidate the {@link JFreeChartPlotEngine}
	 * cache and update the {@link ChartPanel}s chart. This should only be called if a
	 * {@link PlotConfigurationChangeEvent} is processed. If initializing it returns
	 * <code>true</code>, <code>false</code> otherwise.
	 */
	public boolean replot() {
		if (initializing) {
			return true;
		}
		plotInstance.getMasterOfDesaster().clearAll();
		invalidateCache();

		updateChartPanelChart(true);
		return false;
	}

	private boolean isPlotInstanceValid() {
		plotInstance.getMasterOfDesaster().clearAll();
		if (!plotInstance.isValid()) {

			if (!plotInstance.getCurrentPlotConfigurationClone().isValid()) {
				ConfigurationChangeResponse response = new ConfigurationChangeResponse();
				for (PlotConfigurationError error : plotInstance.getCurrentPlotConfigurationClone().getErrors()) {
					response.addError(error);
				}
				plotInstance.getMasterOfDesaster().registerConfigurationChangeResponse(response);
				return false;
			} else {
				ConfigurationChangeResponse response = new ConfigurationChangeResponse();
				for (PlotConfigurationError error : plotInstance.getPlotData().getErrors()) {
					response.addError(error);
				}
				plotInstance.getMasterOfDesaster().registerConfigurationChangeResponse(response);
				return false;
			}
		}
		return true;
	}

	private void setChartTitle() {
		JFreeChart chart = getCurrentChart();
		if (chart != null) {
			String text = plotInstance.getCurrentPlotConfigurationClone().getTitleText();
			if (text == null) {
				chart.setTitle(text);
				return;
			}

			Font font = plotInstance.getCurrentPlotConfigurationClone().getTitleFont();
			if (font == null) {
				font = FontTools.getFont(Font.DIALOG, Font.PLAIN, 10);
			}

			TextTitle textTitle = new TextTitle(text, font);
			textTitle.setPaint(plotInstance.getCurrentPlotConfigurationClone().getTitleColor());

			chart.setTitle(textTitle);

		}

	}

	private void setDomainAxisLabel(String name) {
		JFreeChart chart = getCurrentChart();
		if (chart != null) {
			Plot plot = chart.getPlot();
			if (plot instanceof CategoryPlot) {
				CategoryPlot categoryPlot = (CategoryPlot) plot;
				CategoryAxis domainAxis = categoryPlot.getDomainAxis();
				if (domainAxis != null) {
					domainAxis.setLabel(name);
				}
			} else {
				XYPlot xyPlot = (XYPlot) plot;
				ValueAxis domainAxis = xyPlot.getDomainAxis();
				if (domainAxis != null) {
					domainAxis.setLabel(name);
				}
			}
		}
	}

	private void setDomainAxisDateFormat(DateFormat dateFormat) {
		checkWarnings();
		JFreeChart chart = getCurrentChart();
		if (chart != null) {
			Plot plot = chart.getPlot();
			if (plot instanceof CategoryPlot) {
				CategoryPlot categoryPlot = (CategoryPlot) plot;
				CategoryAxis domainAxis = categoryPlot.getDomainAxis();
				if (domainAxis != null) {

				}
			} else {
				XYPlot xyPlot = (XYPlot) plot;
				ValueAxis domainAxis = xyPlot.getDomainAxis();
				if (domainAxis != null && domainAxis instanceof DateAxis) {
					DateAxis dateAxis = (DateAxis) domainAxis;
					if (getPlotInstance().getCurrentPlotConfigurationClone().getDomainConfigManager()
							.isUsingUserDefinedDateFormat()) {
						dateAxis.setDateFormatOverride(dateFormat);
						dateAxis.setTimeZone(TimeZone.getTimeZone("GMT"));
					} else {
						dateAxis.setDateFormatOverride(null);
					}
				}
			}
		}
	}

	private void setRangeAxisLabel(String name, RangeAxisConfig source) {
		JFreeChart chart = getCurrentChart();
		if (chart != null) {
			Plot plot = chart.getPlot();
			int rangeAxisIdx = plotInstance.getCurrentPlotConfigurationClone().getIndexOfRangeAxisConfigById(source.getId());
			if (rangeAxisIdx == -1) {
				return;
			}

			if (plot instanceof CategoryPlot) {
				CategoryPlot categoryPlot = (CategoryPlot) plot;
				ValueAxis valueAxis = categoryPlot.getRangeAxis(rangeAxisIdx);
				if (valueAxis != null) {
					valueAxis.setLabel(name);
				}
			} else {
				XYPlot xyPlot = (XYPlot) plot;
				ValueAxis valueAxis = xyPlot.getRangeAxis(rangeAxisIdx);
				if (valueAxis != null) {
					valueAxis.setLabel(name);
				}
			}
		}
	}

	private void setPlotBackgroundColor(Color backgroundColor) {
		JFreeChart chart = getCurrentChart();
		if (chart != null) {
			Plot plot = chart.getPlot();
			if (plot instanceof CategoryPlot) {
				CategoryPlot categoryPlot = (CategoryPlot) plot;
				categoryPlot.setBackgroundPaint(backgroundColor);
			} else {
				XYPlot xyPlot = (XYPlot) plot;
				xyPlot.setBackgroundPaint(backgroundColor);
			}
		}
	}

	private void setChartBackgroundColor(Color backgroundColor) {
		JFreeChart chart = getCurrentChart();
		if (chart != null) {
			chart.setBackgroundPaint(backgroundColor);
		}
	}

	private void setAxesFont(Font axesFont) {
		JFreeChart chart = getCurrentChart();
		if (chart != null) {
			Plot plot = chart.getPlot();

			if (plot instanceof CategoryPlot) {
				CategoryPlot categoryPlot = (CategoryPlot) plot;

				// first change range axes font
				int rangeAxisCount = categoryPlot.getRangeAxisCount();
				for (int i = 0; i < rangeAxisCount; ++i) {
					ValueAxis valueAxis = categoryPlot.getRangeAxis(i);
					if (valueAxis != null) {
						valueAxis.setLabelFont(axesFont);
						valueAxis.setTickLabelFont(axesFont);
					}
				}

				// then set domain axis font
				CategoryAxis domainAxis = categoryPlot.getDomainAxis();
				if (domainAxis != null) {
					domainAxis.setLabelFont(axesFont);
					domainAxis.setTickLabelFont(axesFont);
				}

			} else {
				XYPlot xyPlot = (XYPlot) plot;

				// first change range axes font
				int rangeAxisCount = xyPlot.getRangeAxisCount();
				for (int i = 0; i < rangeAxisCount; ++i) {
					ValueAxis rangeAxis = xyPlot.getRangeAxis(i);
					if (rangeAxis != null) {
						rangeAxis.setLabelFont(axesFont);
						rangeAxis.setTickLabelFont(axesFont);
					}
				}

				// then set domain axis font
				ValueAxis domainAxis = xyPlot.getDomainAxis();
				if (domainAxis != null) {
					domainAxis.setLabelFont(axesFont);
					domainAxis.setTickLabelFont(axesFont);
				}

			}
		}
	}

	/**
	 * Creates {@link LegendTitle}s for all dimensions from the PlotConfiguration of this Plotter2D.
	 * Expects that all {@link ValueSource} s in the provided PlotConfiguration use the same
	 * {@link DimensionConfig} s.
	 */
	private List<LegendTitle> createLegendTitles() {
		List<LegendTitle> legendTitles = new LinkedList<LegendTitle>();
		LegendConfiguration legendConfiguration = plotInstance.getCurrentPlotConfigurationClone().getLegendConfiguration();

		LegendTitle legendTitle = new SmartLegendTitle(this,
				new FlowArrangement(HorizontalAlignment.CENTER, VerticalAlignment.CENTER, 30, 2),
				new ColumnArrangement(HorizontalAlignment.LEFT, VerticalAlignment.CENTER, 0, 2));
		legendTitle.setItemPaint(legendConfiguration.getLegendFontColor());

		RectangleEdge position = legendConfiguration.getLegendPosition().getPosition();
		if (position == null) {
			return legendTitles;
		}
		legendTitle.setPosition(position);

		if (legendConfiguration.isShowLegendFrame()) {
			legendTitle.setFrame(new BlockBorder(legendConfiguration.getLegendFrameColor()));
		}
		ColoredBlockContainer wrapper = new ColoredBlockContainer(legendConfiguration.getLegendBackgroundColor());
		wrapper.add(legendTitle.getItemContainer());
		wrapper.setPadding(3, 3, 3, 3);
		legendTitle.setWrapper(wrapper);

		legendTitles.add(legendTitle);
		return legendTitles;
	}

	/**
	 * Creates a new JFreeChart {@link Plot} from {@link PlotConfiguration} of this Plotter2D.
	 */
	private Plot createPlot() throws ChartPlottimeException {

		PlotConfiguration currentPlotConfigurationClone = plotInstance.getCurrentPlotConfigurationClone();
		List<RangeAxisConfig> rangeAxes = currentPlotConfigurationClone.getRangeAxisConfigs();
		Plot plot = null;

		// Select item placement on domain axis
		ValueType domainAxisType = currentPlotConfigurationClone.getDomainConfigManager().getValueType();

		// create plot and domain axis
		if (domainAxisType == ValueType.NOMINAL) {
			CategoryPlot categoryPlot = new LinkAndBrushCategoryPlot();
			categoryPlot.setDomainAxis(ChartAxisFactory.createCategoryDomainAxis(currentPlotConfigurationClone));
			categoryPlot.setOrientation(currentPlotConfigurationClone.getOrientation());
			plot = categoryPlot;
		} else if (domainAxisType == ValueType.NUMERICAL) {
			LinkAndBrushXYPlot xyPlot = new LinkAndBrushXYPlot();
			xyPlot.setDomainAxis(ChartAxisFactory.createNumericalDomainAxis(plotInstance));
			xyPlot.setOrientation(currentPlotConfigurationClone.getOrientation());
			plot = xyPlot;
		} else if (domainAxisType == ValueType.DATE_TIME) {
			XYPlot xyPlot = new LinkAndBrushXYPlot();
			xyPlot.setDomainAxis(ChartAxisFactory.createDateDomainAxis(plotInstance));
			xyPlot.setOrientation(currentPlotConfigurationClone.getOrientation());
			plot = xyPlot;
		} else if (domainAxisType == ValueType.INVALID) {
			throw new ChartPlottimeException("illegal_domain_type", domainAxisType);
		} else if (domainAxisType == ValueType.UNKNOWN && currentPlotConfigurationClone.getAllValueSources().isEmpty()) {
			throw new ChartPlottimeException("no_value_source_defined");
		} else if (domainAxisType == ValueType.UNKNOWN) {
			throw new ChartPlottimeException("unknown_dimension_value_type");
		} else {
			throw new RuntimeException("Item placement is neither categorical, date or numerical. This cannot happen.");
		}

		int rangeAxisIdx = 0;
		for (RangeAxisConfig rangeAxisConfig : rangeAxes) {

			List<ValueSource> valueSources = rangeAxisConfig.getValueSources();

			for (ValueSource valueSource : valueSources) {

				// prepare call to recursivelyGetSeries
				Vector<PlotDimension> dimensionVector = new Vector<PlotDimension>();
				dimensionVector.addAll(currentPlotConfigurationClone.getDefaultDimensionConfigs().keySet());

				// create plot
				if (domainAxisType == ValueType.NOMINAL) {

					if (!valueSource.isUsingRelativeIndicator()) {
						throw new ChartPlottimeException("absolute_indicator_but_nominal_domain", valueSource.getLabel());
					}

					CategoryPlot categoryPlot = (CategoryPlot) plot;
					addDataAndRendererToCategoryPlot(valueSource, categoryPlot, rangeAxisIdx);
				} else if (domainAxisType == ValueType.NUMERICAL) {
					XYPlot xyPlot = (XYPlot) plot;
					addDataAndRendererToXYPlot(valueSource, xyPlot, rangeAxisIdx);
				} else if (domainAxisType == ValueType.DATE_TIME) {
					XYPlot xyPlot = (XYPlot) plot;
					addDataAndRendererToXYPlot(valueSource, xyPlot, rangeAxisIdx);
				} else {
					throw new RuntimeException("Item placement is neither categorical nor numerical. This cannot happen.");
				}
			}

			// set range axis
			ValueAxis rangeAxis = ChartAxisFactory.createRangeAxis(rangeAxisConfig, plotInstance);
			if (rangeAxis != null) {
				if (domainAxisType == ValueType.NUMERICAL || domainAxisType == ValueType.DATE_TIME) {
					try {
						XYPlot xyPlot = (XYPlot) plot;
						xyPlot.setRangeAxis(rangeAxisIdx, rangeAxis);
					} catch (RuntimeException e) {
						// probably this is because the domain axis contains
						// values less then zero and the scaling is logarithmic.
						// The shitty JFreeChart implementation does not throw a
						// proper exception stating what happened,
						// but just a RuntimeException with a string, so this is
						// our best guess:
						if (isProbablyZeroValuesOnLogScaleException(e)) {
							String label = rangeAxisConfig.getLabel();
							if (label == null) {
								label = I18N.getGUILabel("plotter.unnamed_value_label");
							}
							throw new ChartPlottimeException("gui.plotter.error.log_axis_contains_zero", label);
						} else {
							throw e;
						}
					}
				} else if (domainAxisType == ValueType.NOMINAL) {
					// TODO ensure that all values sources have the same range
					CategoryPlot categoryPlot = (CategoryPlot) plot;
					categoryPlot.setRangeAxis(rangeAxisIdx, rangeAxis);
				} else {
					throw new RuntimeException(
							"illegal value type on domain axis - this should not happen because of the checks above.");
				}
			}

			++rangeAxisIdx;
		}

		return plot;
	}

	/**
	 *
	 */
	private void checkWarnings() {
		plotInstance.getMasterOfDesaster().clearWarnings();
		if (plotInstance.hasWarnings()) {
			ConfigurationChangeResponse warningsResponse = new ConfigurationChangeResponse();
			for (PlotConfigurationError warning : plotInstance.getWarnings()) {
				warningsResponse.addWarning(warning);
			}
			plotInstance.getMasterOfDesaster().registerConfigurationChangeResponse(warningsResponse);
		}
		if (!this.getEngineWarnings().isEmpty()) {
			ConfigurationChangeResponse warningsResponse = new ConfigurationChangeResponse();
			for (PlotConfigurationError warning : this.getEngineWarnings()) {
				warningsResponse.addWarning(warning);
			}
			plotInstance.getMasterOfDesaster().registerConfigurationChangeResponse(warningsResponse);
		}
	}

	/**
	 * Creates an appropriate JFreeChart {@link Dataset} for valueSource and adds it to plot.
	 *
	 * @param rangeAxisIdx
	 *            The index of the range axis in the {@link XYPlot}
	 *
	 * @return The index of the newly added dataset in the plot.
	 */
	private void addDataAndRendererToXYPlot(ValueSource valueSource, XYPlot plot, int rangeAxisIdx)
			throws ChartPlottimeException {
		VisualizationType seriesType = valueSource.getSeriesFormat().getSeriesType();
		StackingMode stackingMode = valueSource.getSeriesFormat().getStackingMode();
		IndicatorType errorIndicator = valueSource.getSeriesFormat().getUtilityUsage();

		XYItemRenderer renderer;
		XYDataset dataset;

		DefaultDimensionConfig domainConfig = valueSource.getDomainConfig();
		DimensionConfigData domainConfigData = plotInstance.getPlotData().getDimensionConfigData(domainConfig);
		if (seriesType == VisualizationType.LINES_AND_SHAPES) {
			// stacking is ignored

			// grouping is irrelevant

			if (errorIndicator == IndicatorType.DIFFERENCE) {
				XYItemRenderer[] renderers = ChartRendererFactory.createXYDifferenceRenderers(valueSource, plotInstance);

				if (domainConfigData.hasDuplicateValues()) {
					throwDuplicateValuesNotSupported(valueSource, PlotDimension.DOMAIN);
				}

				for (int seriesIdx = 0; seriesIdx < renderers.length; ++seriesIdx) {
					dataset = ChartDatasetFactory.createDefaultXYDataset(valueSource, seriesIdx, plotInstance);
					renderer = renderers[seriesIdx];
					pushDataAndRendererIntoPlot(plot, rangeAxisIdx, renderer, dataset);
				}
				return;
			} else if (errorIndicator == IndicatorType.BARS) {
				dataset = ChartDatasetFactory.createDefaultIntervalXYDataset(valueSource, plotInstance, true);
				renderer = ChartRendererFactory.createXYErrorRenderer(valueSource, plotInstance, dataset);
			} else if (errorIndicator == IndicatorType.BAND) {
				dataset = ChartDatasetFactory.createDefaultIntervalXYDataset(valueSource, plotInstance, true);
				renderer = ChartRendererFactory.createDeviationRenderer(valueSource, plotInstance);
			} else if (errorIndicator == IndicatorType.NONE) {
				dataset = ChartDatasetFactory.createDefaultXYDataset(valueSource, plotInstance);
				renderer = ChartRendererFactory.createXYLineAndShapeRenderer(valueSource, plotInstance);
			} else {
				// unknown error indicator - this should not happen
				throw new IllegalArgumentException("unknown error indicator");
			}
		} else if (seriesType == VisualizationType.BARS) {
			// grouping is irrelevant

			if (errorIndicator != IndicatorType.NONE) {
				// not supported
				PlotConfigurationError error = new PlotConfigurationError("error_indicator_not_supported",
						valueSource.toString(), errorIndicator.getName());
				SeriesFormatChangeEvent change;
				// suggest to remove error indicators
				change = new SeriesFormatChangeEvent(valueSource.getSeriesFormat(), IndicatorType.NONE);
				error.addQuickFix(new PlotConfigurationQuickFix(change));
				// suggest to switch to lines and shapes
				change = new SeriesFormatChangeEvent(valueSource.getSeriesFormat(), VisualizationType.LINES_AND_SHAPES);
				error.addQuickFix(new PlotConfigurationQuickFix(change));
				throw new ChartPlottimeException(error);
			} else {
				if (stackingMode == StackingMode.NONE) {
					if (!valueSource.isUsingDomainGrouping() || !domainConfig.getGrouping().definesUpperLowerBounds()) {
						dataset = ChartDatasetFactory.createXYSeriesCollection(valueSource, plotInstance, 0.95, false, true);
					} else {
						dataset = ChartDatasetFactory.createDefaultIntervalXYDataset(valueSource, plotInstance, false);
					}
					renderer = ChartRendererFactory.createClusteredXYBarRenderer(valueSource, plotInstance);
				} else if (stackingMode == StackingMode.ABSOLUTE) {
					dataset = ChartDatasetFactory.createDefaultTableXYDataset(valueSource, plotInstance);
					renderer = ChartRendererFactory.createStackedXYBarRenderer(valueSource, plotInstance, false);
				} else if (stackingMode == StackingMode.RELATIVE) {
					// not supported
					dataset = ChartDatasetFactory.createDefaultTableXYDataset(valueSource, plotInstance);
					renderer = ChartRendererFactory.createStackedXYBarRenderer(valueSource, plotInstance, true);
				} else {
					// error - this should not happen
					throw new IllegalArgumentException("unknown stacking mode");
				}
			}
		} else if (seriesType == VisualizationType.AREA) {
			// grouping is irrelevant
			if (errorIndicator != IndicatorType.NONE) {
				// not supported
				PlotConfigurationError error = new PlotConfigurationError("error_indicator_not_supported",
						valueSource.toString(), errorIndicator.getName());
				SeriesFormatChangeEvent change;
				// suggest to remove error indicators
				change = new SeriesFormatChangeEvent(valueSource.getSeriesFormat(), IndicatorType.NONE);
				error.addQuickFix(new PlotConfigurationQuickFix(change));
				// suggest to switch to lines and shapes
				change = new SeriesFormatChangeEvent(valueSource.getSeriesFormat(), VisualizationType.LINES_AND_SHAPES);
				error.addQuickFix(new PlotConfigurationQuickFix(change));
				throw new ChartPlottimeException(error);
			} else {
				if (stackingMode == StackingMode.NONE) {
					dataset = ChartDatasetFactory.createXYSeriesCollection(valueSource, plotInstance, 0, false, true);
					renderer = ChartRendererFactory.createXYAreaRenderer2(valueSource, plotInstance);
				} else if (stackingMode == StackingMode.ABSOLUTE) {
					dataset = ChartDatasetFactory.createDefaultTableXYDataset(valueSource, plotInstance);
					renderer = ChartRendererFactory.createStackedXYAreaRenderer2(valueSource, plotInstance, false);
				} else if (stackingMode == StackingMode.RELATIVE) {
					// not supported
					PlotConfigurationError error = new PlotConfigurationError("stacking_mode_not_supported",
							valueSource.toString(), stackingMode.getName());
					SeriesFormatChangeEvent change;
					// suggest to change to absolute stacking
					change = new SeriesFormatChangeEvent(valueSource.getSeriesFormat(), StackingMode.ABSOLUTE);
					error.addQuickFix(new PlotConfigurationQuickFix(change));
					throw new ChartPlottimeException(error);

				} else {
					// error - this should not happen
					throw new IllegalArgumentException("unknown stacking mode: " + stackingMode);
				}
			}
		} else {
			// error - this should not happen
			throw new IllegalArgumentException("unknown series type: " + seriesType);
		}

		pushDataAndRendererIntoPlot(plot, rangeAxisIdx, renderer, dataset);
	}

	private void pushDataAndRendererIntoPlot(XYPlot plot, int rangeAxisIdx, XYItemRenderer renderer, XYDataset dataset)
			throws ChartPlottimeException {
		if (dataset != null && renderer != null) {
			int datasetIdx = plot.getDatasetCount();
			if (datasetIdx > 0 && plot.getDataset(datasetIdx - 1) == null) {
				datasetIdx -= 1;
			}
			// push dataset and renderer into plot
			try {
				plot.setDataset(datasetIdx, dataset); // if Eclipse states that
														 // dataset might not be
														 // initialized, you did
														 // not consider all
														 // possibilities in the
														 // condition block above
			} catch (RuntimeException e) {
				// probably this is because the domain axis contains values less
				// then zero and the scaling is logarithmic.
				// The shitty JFreeChart implementation does not throw a proper
				// exception stating what happened,
				// but just a RuntimeException with a string, so this is our
				// best guess:
				if (isProbablyZeroValuesOnLogScaleException(e)) {
					throw new ChartPlottimeException("gui.plotter.error.log_axis_contains_zero", "domain axis");
				} else {
					throw e;
				}
			}
			plot.mapDatasetToRangeAxis(datasetIdx, rangeAxisIdx);
			plot.setRenderer(datasetIdx, renderer);
		} else {
			ChartPlottimeException chartPlottimeException = new ChartPlottimeException(
					new PlotConfigurationError("generic_plotter_error"));
			throw chartPlottimeException;
		}
	}

	/**
	 *
	 * @param dataForAllGroupCells
	 * @param rangeAxisIdx
	 */
	private void addDataAndRendererToCategoryPlot(ValueSource valueSource, CategoryPlot plot, int rangeAxisIdx)
			throws ChartPlottimeException {
		VisualizationType seriesType = valueSource.getSeriesFormat().getSeriesType();
		StackingMode stackingMode = valueSource.getSeriesFormat().getStackingMode();
		IndicatorType errorIndicator = valueSource.getSeriesFormat().getUtilityUsage();
		DefaultDimensionConfig domainConfig = valueSource.getDomainConfig();
		DimensionConfigData domainConfigData = plotInstance.getPlotData().getDimensionConfigData(domainConfig);
		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);

		CategoryItemRenderer renderer;
		CategoryDataset dataset;

		if (seriesType == VisualizationType.LINES_AND_SHAPES) {
			// stacking is ignored

			if (errorIndicator == IndicatorType.DIFFERENCE || errorIndicator == IndicatorType.BAND) {
				// not supported
				throwErrorIndicatorNotSupported(valueSource, errorIndicator);
				return;
			} else if (errorIndicator == IndicatorType.BARS) {
				if (domainConfigData.hasDuplicateValues()) {
					throwDuplicateValuesNotSupported(valueSource, PlotDimension.DOMAIN);
					return;
				}
				dataset = ChartDatasetFactory.createDefaultStatisticalCategoryDataset(valueSource, plotInstance);
				renderer = ChartRendererFactory.createStatisticalLineAndShapeRenderer(valueSource, plotInstance);
			} else if (errorIndicator == IndicatorType.NONE) {
				if (valueSource.isUsingDomainGrouping()) {
					dataset = ChartDatasetFactory.createDefaultCategoryDataset(valueSource, plotInstance, false, true);
					renderer = ChartRendererFactory.createLineAndShapeRenderer(valueSource, plotInstance);
				} else {
					dataset = new ValueSourceToMultiValueCategoryDatasetAdapter(valueSourceData, plotInstance);
					renderer = ChartRendererFactory.createScatterRenderer(valueSource, plotInstance);
				}
			} else {
				// unknown error indicator - this should not happen
				throw new IllegalArgumentException("unknown error indicator: " + errorIndicator);
			}
		} else if (seriesType == VisualizationType.BARS) {
			// grouping is irrelevant

			// bars don't support duplicate values on domain dimension:
			if (domainConfigData.hasDuplicateValues()) {
				throwDuplicateValuesNotSupported(valueSource, PlotDimension.DOMAIN);
			}

			// don't support other error indicators than bars for unstacked bar
			// charts, and none at all for stacked bar charts
			if (errorIndicator != IndicatorType.NONE && errorIndicator != IndicatorType.BARS
					|| errorIndicator != IndicatorType.NONE && stackingMode != StackingMode.NONE) {
				throwErrorIndicatorNotSupported(valueSource, errorIndicator);
				return;
			} else {
				if (stackingMode == StackingMode.NONE) {
					if (errorIndicator == IndicatorType.BARS) {
						dataset = ChartDatasetFactory.createDefaultStatisticalCategoryDataset(valueSource, plotInstance);
						renderer = ChartRendererFactory.createStatisticalBarRenderer(valueSource, plotInstance);
					} else { // if (errorIndicator == ErrorIndicator.NONE) { //
							 // no if needed, because we made sure above that
							 // errorIndicator is one of NONE or BARS
						dataset = ChartDatasetFactory.createDefaultCategoryDataset(valueSource, plotInstance, false, true);
						renderer = ChartRendererFactory.createBarRenderer(valueSource, plotInstance);
					}
				} else if (stackingMode == StackingMode.ABSOLUTE) {
					dataset = ChartDatasetFactory.createDefaultCategoryDataset(valueSource, plotInstance, true, false);
					renderer = ChartRendererFactory.createStackedBarRenderer(valueSource, plotInstance, false);
				} else if (stackingMode == StackingMode.RELATIVE) {
					dataset = ChartDatasetFactory.createDefaultCategoryDataset(valueSource, plotInstance, true, false);
					renderer = ChartRendererFactory.createStackedBarRenderer(valueSource, plotInstance, true);
				} else {
					// error - this should not happen
					throw new IllegalArgumentException("unknown stacking mode");
				}
			}
		} else if (seriesType == VisualizationType.AREA) {
			// areas don't support duplicate values on domain dimension:
			if (domainConfigData.hasDuplicateValues()) {
				throwDuplicateValuesNotSupported(valueSource, PlotDimension.DOMAIN);
			}

			// grouping is irrelevant
			if (errorIndicator != IndicatorType.NONE) {
				throwErrorIndicatorNotSupported(valueSource, errorIndicator);
				return;
			} else {
				if (stackingMode == StackingMode.NONE) {
					dataset = ChartDatasetFactory.createDefaultCategoryDataset(valueSource, plotInstance, false, true);
					renderer = ChartRendererFactory.createAreaRenderer(valueSource, plotInstance);
				} else if (stackingMode == StackingMode.ABSOLUTE) {
					dataset = ChartDatasetFactory.createDefaultCategoryDataset(valueSource, plotInstance, true, false);
					renderer = ChartRendererFactory.createStackedAreaRenderer(valueSource, plotInstance, false);
				} else if (stackingMode == StackingMode.RELATIVE) {
					dataset = ChartDatasetFactory.createDefaultCategoryDataset(valueSource, plotInstance, true, false);
					renderer = ChartRendererFactory.createStackedAreaRenderer(valueSource, plotInstance, true);
				} else {
					// error - this should not happen
					throw new IllegalArgumentException("unknown stacking mode: " + stackingMode);
				}
			}
		} else {
			// error - this should not happen
			throw new IllegalArgumentException("unknown series type: " + seriesType);
		}

		if (dataset != null && renderer != null) {
			int datasetIdx = plot.getDatasetCount();
			if (datasetIdx > 0 && plot.getDataset(datasetIdx - 1) == null) {
				datasetIdx -= 1;
			}
			// push dataset and renderer into plot
			try {
				plot.setDataset(datasetIdx, dataset); // if Eclipse states that
														 // dataset might not be
														 // initialized, you did
														 // not consider all
														 // possibilities in the
														 // condition block above
			} catch (RuntimeException e) {
				// probably this is because the domain axis contains values less
				// then zero and the scaling is logarithmic.
				// The shitty JFreeChart implementation does not throw a proper
				// exception stating what happened,
				// but just a RuntimeException with a string, so this is our
				// best guess:
				if (isProbablyZeroValuesOnLogScaleException(e)) {
					throw new ChartPlottimeException("gui.plotter.error.log_axis_contains_zero", "domain axis");
				} else {
					throw e;
				}
			}
			plot.mapDatasetToRangeAxis(datasetIdx, rangeAxisIdx);
			plot.setRenderer(datasetIdx, renderer);
		} else {
			ChartPlottimeException chartPlottimeException = new ChartPlottimeException(
					new PlotConfigurationError("generic_plotter_error"));
			throw chartPlottimeException;
		}

		// int datasetIdx = plot.getDatasetCount();
		// // by default each CategoryPlot contains a null set. This one will be
		// overwritten by the following lines.
		// if (plot.getDataset(datasetIdx-1) == null) {
		// datasetIdx -= 1;
		// }
		//
		// CategoryDataset categoryDataset;
		//
		// categoryDataset =
		// ChartDatasetFactory.createDefaultCategoryDataset(valueSource,
		// plotConfiguration);
		// plot.setDataset(datasetIdx, categoryDataset);
		// plot.mapDatasetToRangeAxis(datasetIdx, rangeAxisIdx);
		// return datasetIdx;
	}

	private void throwDuplicateValuesNotSupported(ValueSource valueSource, PlotDimension dimension)
			throws ChartPlottimeException {
		throw new ChartPlottimeException("duplicate_value", valueSource.toString(), dimension.getName());
	}

	private void throwErrorIndicatorNotSupported(ValueSource valueSource, IndicatorType errorIndicator)
			throws ChartPlottimeException {
		PlotConfigurationError error = new PlotConfigurationError("error_indicator_not_supported", valueSource.toString(),
				errorIndicator.getName());
		SeriesFormatChangeEvent change;

		// suggest to remove error indicators
		change = new SeriesFormatChangeEvent(valueSource.getSeriesFormat(), IndicatorType.NONE);
		error.addQuickFix(new PlotConfigurationQuickFix(change));
		throw new ChartPlottimeException(error);
	}

	private void chartTitleChanged() {
		setChartTitle();
	}

	private void rangeAxisConfigAxisChanged(RangeAxisConfig rangeAxisConfig) {
		if (!isPlotInstanceValid()) {
			updateChartPanel(new JFreeChart(new CategoryPlot()));
			currentChartIsValid = false;
			return;
		}

		int axisIdx = plotInstance.getCurrentPlotConfigurationClone().getIndexOfRangeAxisConfigById(rangeAxisConfig.getId());

		if (axisIdx == -1) {
			return;
		}

		if (currentChartIsValid) {

			try {
				ValueAxis updatedAxis = ChartAxisFactory.createRangeAxis(rangeAxisConfig, plotInstance);
				if (updatedAxis != null) {
					JFreeChart chart = getCurrentChart();
					if (chart != null) {
						Plot plot = chart.getPlot();
						if (plot instanceof XYPlot) {
							((XYPlot) plot).setRangeAxis(axisIdx, updatedAxis);
						} else if (plot instanceof CategoryPlot) {
							((CategoryPlot) plot).setRangeAxis(axisIdx, updatedAxis);
						}
					}
				}

				checkWarnings();
			} catch (ChartPlottimeException e) {
				handlePlottimeException(e);
			} catch (RuntimeException e) {
				// probably this is because the domain axis contains values less
				// then zero and the scaling is logarithmic.
				// The shitty JFreeChart implementation does not throw a proper
				// exception stating what happened,
				// but just a RuntimeException with a string, so this is our best
				// guess:
				if (isProbablyZeroValuesOnLogScaleException(e)) {
					String label = rangeAxisConfig.getLabel();
					if (label == null) {
						label = I18N.getGUILabel("plotter.unnamed_value_label");
					}
					handlePlottimeException(new ChartPlottimeException("gui.plotter.error.log_axis_contains_zero", label));
				} else {
					throw e;
				}
			}
		} else {
			updateChartPanelChart(true);
		}
	}

	/**
	 * Guesses if the given exception is caused by trying to set negative values on a logarithmic
	 * axis in JFreeChart. The shitty JFreeChart implementation does not throw a proper exception
	 * stating what happened, but just a RuntimeException with a string, so this is our best guess:
	 */
	private boolean isProbablyZeroValuesOnLogScaleException(RuntimeException e) {
		return "Values less than or equal to zero not allowed with logarithmic axis".equals(e.getMessage());
	}

	private void legendPositionChanged(LegendPosition legendPosition) {
		JFreeChart chart = getCurrentChart();
		if (chart != null) {
			LegendTitle legend = chart.getLegend();
			RectangleEdge position = legendPosition.getPosition();
			if (legend != null) {
				if (position != null) {
					legend.setPosition(position);
				} else {
					while (chart.getLegend() != null) {
						chart.removeLegend();
					}
				}
			} else {
				if (position != null) {
					resetLegend();
				}
			}
		}
	}

	private void plotBackgroundColorChanged(Color backgroundColor) {
		setPlotBackgroundColor(backgroundColor);
	}

	private void axesFontChanged(Font axesFont) {
		setAxesFont(axesFont);
	}

	private void legendFontChanged(Font legendFont) {
		resetLegend();
	}

	private void chartBackgroundColorChanged(Color chartBackgroundColor) {
		setChartBackgroundColor(chartBackgroundColor);
	}

	/**
	 * Sets the plot configuration and adapts all subscriptions of this Plotter2D to event
	 * providers.
	 *
	 * @param plotInstance
	 *            The new PlotConfiguration. null not allowed.
	 */
	public void setPlotInstance(PlotInstance plotInstance) {
		if (plotInstance == null) {
			throw new IllegalArgumentException("null PlotConfiguration not allowed");
		}

		synchronized (nextPlotInstanceLock) {
			if (updatingChart.get()) {
				if (plotInstance != this.plotInstance) {
					this.nextPlotInstance = plotInstance;
				}
			} else {
				this.nextPlotInstance = plotInstance;
				privateSetPlotInstance();
			}
		}
	}

	private synchronized void updatingChartPanelChartDone() {
		updatingChart.getAndSet(false);
		synchronized (nextPlotInstanceLock) {
			if (nextPlotInstance != null) {
				privateSetPlotInstance();
			}
		}
	}

	private void privateSetPlotInstance() {
		unsubscribeFromPlotInstance(plotInstance);
		subscribeAtPlotInstance(nextPlotInstance);
		this.plotInstance = nextPlotInstance;
		this.nextPlotInstance = null;
		plotInstance.triggerReplot();
	}

	private void unsubscribeFromPlotInstance(PlotInstance plotInstance) {
		initializing = true;
		// unsubscribe from event sources
		PlotConfiguration masterPlotConfiguration = plotInstance.getMasterPlotConfiguration();
		masterPlotConfiguration.removePlotConfigurationListener(this);
		masterPlotConfiguration.removePlotConfigurationProcessingListener(this);
		endProcessing();
		chartPanel.removeLinkAndBrushSelectionListener(masterPlotConfiguration);
		initializing = false;
	}

	@Override
	public boolean plotConfigurationChanged(PlotConfigurationChangeEvent change) {
		PlotConfigurationChangeType type = change.getType();
		boolean processed;
		switch (type) {
			case DIMENSION_CONFIG_ADDED:
			case DIMENSION_CONFIG_REMOVED:
			case RANGE_AXIS_CONFIG_ADDED:
			case RANGE_AXIS_CONFIG_MOVED:
			case RANGE_AXIS_CONFIG_REMOVED:
			case COLOR_SCHEME:
			case DATA_TABLE_EXCHANGED:
			case TRIGGER_REPLOT:
			case META_CHANGE:
				processed = replot();
				break;
			case AXES_FONT:
				axesFontChanged(change.getAxesFont());
				processed = true;
				break;
			case FRAME_BACKGROUND_COLOR:
				chartBackgroundColorChanged(change.getFrameBackgroundColor());
				processed = true;
				break;
			case CHART_TITLE:
				chartTitleChanged();
				processed = true;
				break;
			case DIMENSION_CONFIG_CHANGED:
				processed = dimensionConfigChanged(change.getDimensionChange());
				break;
			case LEGEND_CHANGED:
				legendChanged(change.getLegendConfigurationChangeEvent());
				processed = true;
				break;
			case PLOT_BACKGROUND_COLOR:
				plotBackgroundColorChanged(change.getPlotBackgroundColor());
				processed = true;
				break;
			case RANGE_AXIS_CONFIG_CHANGED:
				processed = rangeAxisConfigChanged(change.getRangeAxisConfigChange());
				break;
			case PLOT_ORIENTATION:
				plotOrientationChanged(change.getOrientation());
				processed = true;
				break;
			case AXIS_LINE_COLOR:
				axisLineColorChanged(change.getDomainAxisLineColor());
				processed = true;
				break;
			case AXIS_LINE_WIDTH:
				axisLineWidthChanged(change.getDomainAxisLineWidth());
				processed = true;
				break;
			case LINK_AND_BRUSH_SELECTION:
				checkWarnings();
				processed = replot();
				break;
			default:
				// DONT FORGET TO RETURN TRUE OR FALSE
				throw new RuntimeException("Unknown event type " + type + ". This should not happen.");
		}
		return processed;
	}

	private void legendChanged(LegendConfigurationChangeEvent change) {
		switch (change.getType()) {
			case FONT:
				legendFontChanged(change.getLegendFont());
				break;
			case POSITON:
				legendPositionChanged(change.getLegendPosition());
				break;
			case SHOW_DIMENSION_TYPE:
				legendShowDimensionTypeChanged(change.isShowDimensionType());
				break;
			default:
				resetLegend();
		}
	}

	private void legendShowDimensionTypeChanged(boolean showDimensionType) {
		resetLegend();
	}

	private void resetLegend() {
		JFreeChart chart = getCurrentChart();
		if (chart != null) {
			updateLegendItems();
			formatLegend(chart);
			checkWarnings();
		}
	}

	private void axisLineColorChanged(Color lineColor) {
		JFreeChart chart = getCurrentChart();
		if (chart != null) {
			Plot plot = chart.getPlot();
			if (plot instanceof CategoryPlot) {
				CategoryPlot categoryPlot = (CategoryPlot) plot;
				ChartAxisFactory.formatAxis(plotInstance.getCurrentPlotConfigurationClone(), categoryPlot.getDomainAxis());
				int rangeAxisCount = categoryPlot.getRangeAxisCount();
				for (int i = 0; i < rangeAxisCount; ++i) {
					ValueAxis valueAxis = categoryPlot.getRangeAxis(i);
					if (valueAxis != null) {
						ChartAxisFactory.formatAxis(plotInstance.getCurrentPlotConfigurationClone(), valueAxis);
					}
				}
			} else if (plot instanceof XYPlot) {
				XYPlot xyPlot = (XYPlot) plot;
				ChartAxisFactory.formatAxis(plotInstance.getCurrentPlotConfigurationClone(), xyPlot.getDomainAxis());
				int rangeAxisCount = xyPlot.getRangeAxisCount();
				for (int i = 0; i < rangeAxisCount; ++i) {
					ValueAxis valueAxis = xyPlot.getRangeAxis(i);
					if (valueAxis != null) {
						valueAxis.setAxisLinePaint(lineColor);
						ChartAxisFactory.formatAxis(plotInstance.getCurrentPlotConfigurationClone(), valueAxis);
					}
				}
			}
		}

	}

	private void axisLineWidthChanged(Float lineWidth) {
		JFreeChart chart = getCurrentChart();
		if (chart != null) {
			Plot plot = chart.getPlot();
			BasicStroke stroke = new BasicStroke(lineWidth);
			if (plot instanceof CategoryPlot) {
				CategoryPlot categoryPlot = (CategoryPlot) plot;
				CategoryAxis domainAxis = categoryPlot.getDomainAxis();
				if (domainAxis != null) {
					domainAxis.setAxisLineStroke(stroke);
				}
				int rangeAxisCount = categoryPlot.getRangeAxisCount();
				for (int i = 0; i < rangeAxisCount; ++i) {
					ValueAxis valueAxis = categoryPlot.getRangeAxis(i);
					if (valueAxis != null) {
						valueAxis.setAxisLineStroke(stroke);
					}
				}
			} else if (plot instanceof XYPlot) {
				XYPlot xyPlot = (XYPlot) plot;
				ValueAxis domainAxis = xyPlot.getDomainAxis();
				if (domainAxis != null) {
					domainAxis.setAxisLineStroke(stroke);
				}
				int rangeAxisCount = xyPlot.getRangeAxisCount();
				for (int i = 0; i < rangeAxisCount; ++i) {
					ValueAxis valueAxis = xyPlot.getRangeAxis(i);
					if (valueAxis != null) {
						valueAxis.setAxisLineStroke(stroke);
					}
				}
			}
		}

	}

	private void plotOrientationChanged(PlotOrientation orientation) {
		JFreeChart chart = getCurrentChart();
		if (chart != null) {
			Plot plot = chart.getPlot();
			if (plot instanceof CategoryPlot) {
				CategoryPlot categoryPlot = (CategoryPlot) plot;
				categoryPlot.setOrientation(orientation);
			} else if (plot instanceof XYPlot) {
				XYPlot xyPlot = (XYPlot) plot;
				xyPlot.setOrientation(orientation);
			}
		}

	}

	private boolean rangeAxisConfigChanged(RangeAxisConfigChangeEvent change) {
		RangeAxisConfig source = change.getSource();
		RangeAxisConfigChangeType type = change.getType();
		boolean processed = true;
		switch (type) {
			case VALUE_SOURCE_ADDED:
			case VALUE_SOURCE_MOVED:
			case VALUE_SOURCE_REMOVED:
			case CLEARED:
				return replot();
			case LABEL:
				String label = change.getLabel();
				if (label == null) {
					label = I18N.getGUILabel("plotter.unnamed_value_label");
				}
				setRangeAxisLabel(label, source);
				break;
			case SCALING:
				rangeAxisConfigAxisChanged(source);
				break;
			case VALUE_SOURCE_CHANGED:
				processed = valueSouceChanged(change.getValueSourceChange());
				break;
			case AUTO_NAMING:
				break;
			case RANGE_CHANGED:
				rangeAxisConfigAxisChanged(source);
				break;
			case CROSSHAIR_LINES_CHANGED:
				updateChartPanel(getCurrentChart());
				break;
			default:
				throw new RuntimeException("Unknown event type " + type + " This should not happen.");
		}
		return processed;
	}

	private boolean valueSouceChanged(ValueSourceChangeEvent change) {
		ValueSourceChangeType type = change.getType();
		boolean processed = true;
		switch (type) {
			case USES_GROUPING:
			case AGGREGATION_WINDOWING_CHANGED:
			case USE_RELATIVE_UTILITIES:
			case AGGREGATION_FUNCTION_MAP:
			case DATATABLE_COLUMN_MAP:
			case SERIES_FORMAT_CHANGED:
				return replot();
			case UPDATED:
				// this is caused by other events that cause a replot - do nothing
				break;
			case LABEL:
				resetLegend();
				break;
			case AUTO_NAMING:
				break;
			default:
				throw new RuntimeException("Unknown event type " + type + " This should not happen.");
		}
		return processed;
	}

	/**
	 * Returns true iff processing is finished after this function has finished; false if a
	 * processing thread is started.
	 */
	private boolean dimensionConfigChanged(DimensionConfigChangeEvent change) {
		DimensionConfigChangeType type = change.getType();
		boolean processed;
		switch (type) {
			case LABEL:
				if (change.getDimension() == PlotDimension.DOMAIN) {
					String label = change.getLabel();
					if (label == null) {
						label = I18N.getGUILabel("plotter.unnamed_value_label");
					}
					setDomainAxisLabel(label);
				} else {
					resetLegend();
				}
				processed = true;
				break;
			case DATE_FORMAT_CHANGED:
				if (change.getDimension() == PlotDimension.DOMAIN) {
					setDomainAxisDateFormat(change.getDateFormat());
				} else {
					resetLegend();
				}
				processed = true;
				break;
			case COLOR_SCHEME:
				processed = true;
				break;
			case CROSSHAIR_LINES_CHANGED:
				updateChartPanel(getCurrentChart());
				processed = true;
				break;
			default:
				return replot();
		}

		return processed;

	}

	private void handlePlottimeException(ChartPlottimeException e) {
		if (plotInstance.getMasterOfDesaster() != null) {
			plotInstance.getMasterOfDesaster().registerConfigurationChangeResponse(e.getResponse());
		}
		invalidateCache();
		currentChartIsValid = false;
		updateChartPanel(new JFreeChart(new CategoryPlot()));
	}

	/**
	 * Creates the legend items for this {@link JFreeChartPlotEngine}.
	 *
	 * @see org.jfree.chart.LegendItemSource#getLegendItems()
	 */
	@Override
	public LegendItemCollection getLegendItems() {
		if (plotInstance.getCurrentPlotConfigurationClone().getLegendConfiguration()
				.getLegendPosition() == LegendPosition.NONE) {
			return null;
		}
		synchronized (this) {
			return cachedLegendItems;
		}
	}

	private void updateLegendItems() {
		synchronized (this) {
			cachedLegendItems = legendCreator.getLegendItems(plotInstance);
		}
	}

	/**
	 * Returns the {@link ChartPanel} that is controlled by this {@link JFreeChartPlotEngine}.
	 */
	public LinkAndBrushChartPanel getChartPanel() {
		return chartPanel;
	}

	public void addPlotEngineListener(JFreeChartPlotEngineListener l) {
		listeners.add(new WeakReference<JFreeChartPlotEngineListener>(l));
	}

	public void removePlotEngineListener(JFreeChartPlotEngineListener l) {
		Iterator<WeakReference<JFreeChartPlotEngineListener>> it = listeners.iterator();
		while (it.hasNext()) {
			JFreeChartPlotEngineListener listener = it.next().get();
			if (listener == null || listener == l) {
				it.remove();
			}
		}
	}

	/**
	 * This method sets the coordinate transformation for this component.
	 */
	public void setCoordinateTransformation(CoordinateTransformation transformation) {
		this.coordinateTransformation = transformation;
		this.chartPanel.setCoordinateTransformation(transformation);
	}

	private void fireChartChanged(JFreeChart chart) {
		Iterator<WeakReference<JFreeChartPlotEngineListener>> defaultIt = listeners.iterator();
		while (defaultIt.hasNext()) {
			WeakReference<JFreeChartPlotEngineListener> wrl = defaultIt.next();
			JFreeChartPlotEngineListener l = wrl.get();
			if (l != null) {
				l.chartChanged(this, chart);
			} else {
				defaultIt.remove();
			}
		}
	}

	@Override
	public List<PlotConfigurationError> getEngineErrors() {
		List<PlotConfigurationError> errors = new LinkedList<PlotConfigurationError>();
		return errors;
	}

	@Override
	public List<PlotConfigurationError> getEngineWarnings() {
		List<PlotConfigurationError> warnings = new LinkedList<PlotConfigurationError>();

		PlotConfiguration plotConfiguration = plotInstance.getCurrentPlotConfigurationClone();

		// check if a category plot contains value sources which request lines to be drawn
		if (plotConfiguration.getDomainConfigManager().isNominal()) {
			for (ValueSource valueSource : plotConfiguration.getAllValueSources()) {
				if (valueSource.getSeriesFormat().getSeriesType() == VisualizationType.LINES_AND_SHAPES
						&& valueSource.getSeriesFormat().getLineStyle() != LineStyle.NONE
						&& !valueSource.isUsingDomainGrouping()) {
					warnings.add(new PlotConfigurationError("plot_does_not_support_lines",
							"categorical scatter plot with ungrouped domain axis", valueSource.toString()));
				}
			}
		}

		// check if a value source requests a difference plot with items
		for (ValueSource valueSource : plotConfiguration.getAllValueSources()) {
			SeriesFormat format = valueSource.getSeriesFormat();
			if (format.getSeriesType() == VisualizationType.LINES_AND_SHAPES) {
				if (format.getUtilityUsage() == IndicatorType.DIFFERENCE) {
					if (format.getItemShape() != ItemShape.NONE) {
						warnings.add(new PlotConfigurationError("difference_plot_with_items_not_supported",
								valueSource.toString()));
					}
				}
			}
		}

		return warnings;
	}

	@Override
	public PlotInstance getPlotInstance() {
		return plotInstance;
	}

	@Override
	public void startProcessing() {
		plotInstance.getMasterOfDesaster().setCalculating(true);
	}

	@Override
	public void endProcessing() {
		plotInstance.getMasterOfDesaster().setCalculating(false);

	}
}
