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

import com.rapidminer.gui.new_plotter.ChartPlottimeException;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.LineFormat.LineStyle;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.ItemShape;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.data.ValueSourceData;
import com.rapidminer.gui.new_plotter.engine.jfreechart.renderer.FormattedAreaRenderer;
import com.rapidminer.gui.new_plotter.engine.jfreechart.renderer.FormattedBarRenderer;
import com.rapidminer.gui.new_plotter.engine.jfreechart.renderer.FormattedClusteredXYBarRenderer;
import com.rapidminer.gui.new_plotter.engine.jfreechart.renderer.FormattedDeviationRenderer;
import com.rapidminer.gui.new_plotter.engine.jfreechart.renderer.FormattedLineAndShapeRenderer;
import com.rapidminer.gui.new_plotter.engine.jfreechart.renderer.FormattedRenderer;
import com.rapidminer.gui.new_plotter.engine.jfreechart.renderer.FormattedScatterRenderer;
import com.rapidminer.gui.new_plotter.engine.jfreechart.renderer.FormattedStackedAreaRenderer;
import com.rapidminer.gui.new_plotter.engine.jfreechart.renderer.FormattedStackedBarRenderer;
import com.rapidminer.gui.new_plotter.engine.jfreechart.renderer.FormattedStackedXYAreaRenderer2;
import com.rapidminer.gui.new_plotter.engine.jfreechart.renderer.FormattedStackedXYBarRenderer;
import com.rapidminer.gui.new_plotter.engine.jfreechart.renderer.FormattedStatisticalBarRenderer;
import com.rapidminer.gui.new_plotter.engine.jfreechart.renderer.FormattedStatisticalLineAndShapeRenderer;
import com.rapidminer.gui.new_plotter.engine.jfreechart.renderer.FormattedXYAreaRenderer2;
import com.rapidminer.gui.new_plotter.engine.jfreechart.renderer.FormattedXYDifferenceRenderer;
import com.rapidminer.gui.new_plotter.engine.jfreechart.renderer.FormattedXYErrorRenderer;
import com.rapidminer.gui.new_plotter.engine.jfreechart.renderer.FormattedXYLineAndShapeRenderer;
import com.rapidminer.gui.new_plotter.utility.DataStructureUtils;
import com.rapidminer.tools.LogService;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.ScatterRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYAreaRenderer2;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;


/**
 * Helper class for created JFreeChart renderers for a given ValueSource.
 * 
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class ChartRendererFactory {

	/**
	 * 
	 */
	private static final double DEFAULT_PERCENTUAL_XY_BAR_GAP = 0.04;

	/**
	 * Private ctor, pure static class.
	 */
	private ChartRendererFactory() {}

	private static void configureXYLineAndShapeRenderer(XYLineAndShapeRenderer renderer, ValueSource valueSource,
			PlotInstance plotInstance) {
		renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
		SeriesFormat seriesFormat = valueSource.getSeriesFormat();
		DimensionConfig domainConfig = valueSource.getDomainConfig();
		DimensionConfig colorDimensionConfig = plotInstance.getCurrentPlotConfigurationClone().getDimensionConfig(
				PlotDimension.COLOR);
		DimensionConfig shapeDimensionConfig = plotInstance.getCurrentPlotConfigurationClone().getDimensionConfig(
				PlotDimension.SHAPE);
		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);

		int seriesCount = valueSourceData.getSeriesDataForAllGroupCells().groupCellCount();

		// Loop all series and set series format.
		// Format based on dimension configs will be set later on in initFormatDelegate().
		for (int seriesIdx = 0; seriesIdx < seriesCount; ++seriesIdx) {
			// configure linestyle
			if (seriesFormat.getLineStyle() == LineStyle.NONE) {
				renderer.setSeriesLinesVisible(seriesIdx, false);
			} else {
				renderer.setSeriesLinesVisible(seriesIdx, true);
				renderer.setSeriesStroke(seriesIdx, seriesFormat.getStroke(), false);
			}

			// configure series shape if necessary
			if (!SeriesFormat.calculateIndividualFormatForEachItem(domainConfig, shapeDimensionConfig)) {
				if (seriesFormat.getItemShape() != ItemShape.NONE) {
					renderer.setSeriesShapesVisible(seriesIdx, true);
					renderer.setSeriesShape(seriesIdx, seriesFormat.getItemShape().getShape());
				} else {
					renderer.setSeriesShapesVisible(seriesIdx, false);
				}
			}

			// configure series color if necessary
			if (!SeriesFormat.calculateIndividualFormatForEachItem(domainConfig, colorDimensionConfig)) {
				Color itemColor = seriesFormat.getItemColor();
				renderer.setSeriesPaint(seriesIdx, itemColor);
				renderer.setSeriesFillPaint(seriesIdx, itemColor);
			}
			renderer.setSeriesOutlinePaint(seriesIdx, PlotConfiguration.DEFAULT_SERIES_OUTLINE_PAINT);
			renderer.setUseOutlinePaint(true);
		}
	}

	private static void initFormatDelegate(ValueSource valueSource, FormattedRenderer formattedRenderer,
			PlotInstance plotInstance) {
		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
		List<Integer> seriesIndices = new LinkedList<Integer>();
		if (valueSourceData != null) {
			for (int i = 0; i < valueSourceData.getSeriesCount(); ++i) {
				seriesIndices.add(i);
			}
		} else {
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.gui.new_plotter.engine.jfreechart.ChartRendererFactory.null_value_source");
		}
		initFormatDelegate(valueSource, seriesIndices, formattedRenderer, plotInstance);
	}

	private static void initFormatDelegate(ValueSource valueSource, int seriesIdx, FormattedRenderer formattedRenderer,
			PlotInstance plotInstance) {
		List<Integer> seriesIndices = new LinkedList<Integer>();
		seriesIndices.add(seriesIdx);
		initFormatDelegate(valueSource, seriesIndices, formattedRenderer, plotInstance);
	}

	private static void initFormatDelegate(ValueSource valueSource, List<Integer> seriesIndices,
			FormattedRenderer formattedRenderer, PlotInstance plotInstance) {
		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
		RenderFormatDelegate formatDelegate = formattedRenderer.getFormatDelegate();
		formatDelegate.setConfiguration(valueSourceData, plotInstance);
	}

	public static XYItemRenderer[] createXYDifferenceRenderers(ValueSource valueSource, PlotInstance plotInstance) {
		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
		FormattedXYDifferenceRenderer[] renderers = new FormattedXYDifferenceRenderer[valueSourceData.getSeriesCount()];
		for (int seriesIdx = 0; seriesIdx < valueSourceData.getSeriesCount(); ++seriesIdx) {
			FormattedXYDifferenceRenderer renderer = new FormattedXYDifferenceRenderer(seriesIdx);
			configureXYDifferenceRenderer(renderer, valueSource, plotInstance.getCurrentPlotConfigurationClone());
			initFormatDelegate(valueSource, seriesIdx, renderer, plotInstance);
			renderers[seriesIdx] = renderer;
		}
		return renderers;
	}

	private static void configureXYDifferenceRenderer(FormattedXYDifferenceRenderer renderer, ValueSource valueSource,
			PlotConfiguration plotConfiguration) {
		renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
		SeriesFormat seriesFormat = valueSource.getSeriesFormat();
		DimensionConfig domainConfig = valueSource.getDomainConfig();
		DimensionConfig colorDimensionConfig = plotConfiguration.getDimensionConfig(PlotDimension.COLOR);
		DimensionConfig shapeDimensionConfig = plotConfiguration.getDimensionConfig(PlotDimension.SHAPE);

		int seriesCount = 1;// valueSource.getSeriesDataForAllGroupCells().groupCellCount();

		// Loop all series and set series format.
		// Format based on dimension configs will be set later on in initFormatDelegate().
		for (int seriesIdx = 0; seriesIdx < seriesCount; ++seriesIdx) {
			// configure linestyle
			if (seriesFormat.getLineStyle() == LineStyle.NONE) {
			} else {
				renderer.setSeriesStroke(seriesIdx, seriesFormat.getStroke(), false);
			}

			// configure series shape if necessary
			if (!SeriesFormat.calculateIndividualFormatForEachItem(domainConfig, shapeDimensionConfig)) {
				if (seriesFormat.getItemShape() != ItemShape.NONE) {
					renderer.setSeriesShape(seriesIdx, seriesFormat.getItemShape().getShape());
				} else {
				}
			}

			// configure series color if necessary
			if (!SeriesFormat.calculateIndividualFormatForEachItem(domainConfig, colorDimensionConfig)) {
				Color itemColor = seriesFormat.getItemColor();
				Color halfTransparentPaint = DataStructureUtils.setColorAlpha(itemColor, itemColor.getAlpha() / 2);

				renderer.setSeriesPaint(0, halfTransparentPaint);
				renderer.setSeriesFillPaint(0, halfTransparentPaint);
				renderer.setPositivePaint(halfTransparentPaint);
				renderer.setNegativePaint(new Color(255 - itemColor.getRed(), 255 - itemColor.getGreen(), 255 - itemColor
						.getBlue(), itemColor.getAlpha() / 2));
			}
			renderer.setSeriesOutlinePaint(seriesIdx, PlotConfiguration.DEFAULT_SERIES_OUTLINE_PAINT);
		}
	}

	public static XYItemRenderer createXYErrorRenderer(ValueSource valueSource, PlotInstance plotInstance, XYDataset dataset) {
		FormattedXYErrorRenderer renderer = new FormattedXYErrorRenderer();
		configureXYLineAndShapeRenderer(renderer, valueSource, plotInstance);
		initFormatDelegate(valueSource, renderer, plotInstance);
		renderer.setDrawXError(false);
		renderer.setDrawYError(true);
		return renderer;
	}

	public static XYItemRenderer createDeviationRenderer(ValueSource valueSource, PlotInstance plotInstance) {
		FormattedDeviationRenderer renderer = new FormattedDeviationRenderer();
		configureXYLineAndShapeRenderer(renderer, valueSource, plotInstance);
		renderer.setAlpha(0.5f * valueSource.getSeriesFormat().getOpacity() / 255f);
		initFormatDelegate(valueSource, renderer, plotInstance);
		return renderer;
	}

	public static XYItemRenderer createClusteredXYBarRenderer(ValueSource valueSource, PlotInstance plotInstance) {
		FormattedClusteredXYBarRenderer renderer = new FormattedClusteredXYBarRenderer(DEFAULT_PERCENTUAL_XY_BAR_GAP, false);
		configureXYBarRenderer(renderer, valueSource, plotInstance);
		initFormatDelegate(valueSource, renderer, plotInstance);
		return renderer;
	}

	private static void configureXYBarRenderer(XYBarRenderer renderer, ValueSource valueSource, PlotInstance plotInstance) {
		StandardXYBarPainter barPainter = new StandardXYBarPainter();
		renderer.setBarPainter(barPainter);
		renderer.setGradientPaintTransformer(null);
		SeriesFormat seriesFormat = valueSource.getSeriesFormat();
		DimensionConfig domainConfig = valueSource.getDomainConfig();
		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
		int seriesCount;
		if (valueSourceData != null) {
			seriesCount = valueSourceData.getSeriesCount();
		} else {
			seriesCount = 0;
		}
		DimensionConfig colorDimensionConfig = plotInstance.getCurrentPlotConfigurationClone().getDimensionConfig(
				PlotDimension.COLOR);
		// don't need shapeDimensionConfig, since the shape can't be represented for bars.

		// Loop all series and set series format.
		// Format based on dimension configs will be set later on in initFormatDelegate().
		for (int seriesIdx = 0; seriesIdx < seriesCount; ++seriesIdx) {
			// configure series paint if necessary
			if (!SeriesFormat.calculateIndividualFormatForEachItem(domainConfig, colorDimensionConfig)) {
				renderer.setSeriesPaint(seriesIdx, seriesFormat.getAreaFillPaint());
			}

			// configure general style of the bars
			renderer.setShadowVisible(false);
			renderer.setSeriesOutlinePaint(seriesIdx, PlotConfiguration.DEFAULT_SERIES_OUTLINE_PAINT);
		}
		renderer.setDrawBarOutline(true);
	}

	public static XYItemRenderer createStackedXYBarRenderer(ValueSource valueSource, PlotInstance plotInstance,
			boolean asPercentages) {
		FormattedStackedXYBarRenderer renderer = new FormattedStackedXYBarRenderer(0.1);
		renderer.setRenderAsPercentages(asPercentages);
		configureXYBarRenderer(renderer, valueSource, plotInstance);
		initFormatDelegate(valueSource, renderer, plotInstance);
		return renderer;
	}

	public static XYItemRenderer createXYAreaRenderer2(ValueSource valueSource, PlotInstance plotInstance) {
		FormattedXYAreaRenderer2 renderer = new FormattedXYAreaRenderer2();
		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);

		configureXYAreaRenderer2(valueSourceData, renderer);
		initFormatDelegate(valueSource, renderer, plotInstance);
		return renderer;
	}

	private static void configureXYAreaRenderer2(ValueSourceData valueSourceData, XYAreaRenderer2 renderer) {
		int seriesCount = valueSourceData.getSeriesCount();
		SeriesFormat seriesFormat = valueSourceData.getValueSource().getSeriesFormat();
		// Loop all series and set series format.
		// Format based on dimension configs will be set later on in initFormatDelegate().
		for (int seriesIdx = 0; seriesIdx < seriesCount; ++seriesIdx) {
			renderer.setSeriesPaint(seriesIdx, seriesFormat.getAreaFillPaint());
		}
		// don't use outline paint, otherwise the plot shows vertical lines
		renderer.setOutline(false);
	}

	public static XYItemRenderer createStackedXYAreaRenderer2(ValueSource valueSource, PlotInstance plotInstance, boolean b) {
		FormattedStackedXYAreaRenderer2 renderer = new FormattedStackedXYAreaRenderer2();

		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
		configureXYAreaRenderer2(valueSourceData, renderer);
		initFormatDelegate(valueSource, renderer, plotInstance);
		return renderer;
	}

	public static XYItemRenderer createXYLineAndShapeRenderer(ValueSource valueSource, PlotInstance plotInstance) {
		FormattedXYLineAndShapeRenderer renderer = new FormattedXYLineAndShapeRenderer();
		configureXYLineAndShapeRenderer(renderer, valueSource, plotInstance);
		initFormatDelegate(valueSource, renderer, plotInstance);
		return renderer;
	}

	public static FormattedStatisticalLineAndShapeRenderer createStatisticalLineAndShapeRenderer(ValueSource valueSource,
			PlotInstance plotInstance) {
		FormattedStatisticalLineAndShapeRenderer renderer = new FormattedStatisticalLineAndShapeRenderer();

		configureLineAndShapeRenderer(renderer, valueSource, plotInstance);
		initFormatDelegate(valueSource, renderer, plotInstance);
		return renderer;
	}

	public static FormattedLineAndShapeRenderer createLineAndShapeRenderer(ValueSource valueSource, PlotInstance plotInstance) {
		FormattedLineAndShapeRenderer renderer = new FormattedLineAndShapeRenderer();

		renderer.setDefaultEntityRadius(4);

		configureLineAndShapeRenderer(renderer, valueSource, plotInstance);
		initFormatDelegate(valueSource, renderer, plotInstance);
		return renderer;
	}

	private static void configureLineAndShapeRenderer(LineAndShapeRenderer renderer, ValueSource valueSource,
			PlotInstance plotInstance) {
		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
		int seriesCount = valueSourceData.getSeriesCount();
		SeriesFormat seriesFormat = valueSource.getSeriesFormat();
		DimensionConfig domainConfig = valueSource.getDomainConfig();
		DimensionConfig colorDimensionConfig = plotInstance.getCurrentPlotConfigurationClone().getDimensionConfig(
				PlotDimension.COLOR);
		DimensionConfig shapeDimensionConfig = plotInstance.getCurrentPlotConfigurationClone().getDimensionConfig(
				PlotDimension.SHAPE);

		renderer.setDefaultEntityRadius(4);

		// loop all series and set series format
		for (int seriesIdx = 0; seriesIdx < seriesCount; ++seriesIdx) {
			// configure linestyle
			if (seriesFormat.getLineStyle() != LineStyle.NONE) {
				renderer.setSeriesLinesVisible(seriesIdx, true);
				renderer.setSeriesStroke(seriesIdx, seriesFormat.getStroke(), false);
			} else {
				renderer.setSeriesLinesVisible(seriesIdx, false);
			}

			// configure series shape if necessary
			if (!SeriesFormat.calculateIndividualFormatForEachItem(domainConfig, shapeDimensionConfig)) {
				if (seriesFormat.getItemShape() != ItemShape.NONE) {
					renderer.setSeriesShapesVisible(seriesIdx, true);
					renderer.setSeriesShape(seriesIdx, seriesFormat.getItemShape().getShape());
				} else {
					renderer.setSeriesShapesVisible(seriesIdx, false);
				}
			}

			// configure series color if necessary
			if (!SeriesFormat.calculateIndividualFormatForEachItem(domainConfig, colorDimensionConfig)) {
				Color itemColor = seriesFormat.getItemColor();
				renderer.setSeriesPaint(seriesIdx, itemColor);
			}
			renderer.setSeriesOutlinePaint(seriesIdx, PlotConfiguration.DEFAULT_SERIES_OUTLINE_PAINT);
			renderer.setUseOutlinePaint(true);
		}
	}

	private static void configureScatterRenderer(ScatterRenderer renderer, ValueSource valueSource, PlotInstance plotInstance)
			throws ChartPlottimeException {
		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
		int seriesCount = valueSourceData.getSeriesCount();
		SeriesFormat seriesFormat = valueSource.getSeriesFormat();
		DimensionConfig domainConfig = valueSource.getDomainConfig();
		DimensionConfig colorDimensionConfig = plotInstance.getCurrentPlotConfigurationClone().getDimensionConfig(
				PlotDimension.COLOR);
		DimensionConfig shapeDimensionConfig = plotInstance.getCurrentPlotConfigurationClone().getDimensionConfig(
				PlotDimension.SHAPE);

		renderer.setDefaultEntityRadius(4);

		// loop all series and set series format
		for (int seriesIdx = 0; seriesIdx < seriesCount; ++seriesIdx) {
			// lines are not supported in a ScatterRenderer, but this is already checked in
			// JFreeChartPlotEngine.getWarnings().

			// configure series shape if necessary
			if (!SeriesFormat.calculateIndividualFormatForEachItem(domainConfig, shapeDimensionConfig)) {
				// if(seriesFormat.getItemShape() != ItemShape.NONE) {
				renderer.setSeriesShape(seriesIdx, seriesFormat.getItemShape().getShape());
				// } else {
				// throw new ChartPlottimeException("unsupported_item_shape",
				// valueSource.toString(), seriesFormat.getItemShape());
				// }
			}

			// configure series color if necessary
			if (!SeriesFormat.calculateIndividualFormatForEachItem(domainConfig, colorDimensionConfig)) {
				Color itemColor = seriesFormat.getItemColor();
				renderer.setSeriesPaint(seriesIdx, itemColor);
			}
			renderer.setSeriesOutlinePaint(seriesIdx, PlotConfiguration.DEFAULT_SERIES_OUTLINE_PAINT);
			renderer.setUseOutlinePaint(true);
			renderer.setDrawOutlines(true);
			renderer.setUseSeriesOffset(false);
		}
	}

	public static FormattedStatisticalBarRenderer createStatisticalBarRenderer(ValueSource valueSource,
			PlotInstance plotInstance) {
		FormattedStatisticalBarRenderer renderer = new FormattedStatisticalBarRenderer();
		configureBarRenderer(renderer, valueSource, plotInstance);
		initFormatDelegate(valueSource, renderer, plotInstance);
		return renderer;
	}

	private static void configureBarRenderer(BarRenderer renderer, ValueSource valueSource, PlotInstance plotInstance) {
		StandardBarPainter barPainter = new StandardBarPainter();
		renderer.setBarPainter(barPainter);
		renderer.setGradientPaintTransformer(null);

		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
		int seriesCount = valueSourceData.getSeriesCount();
		DimensionConfig domainConfig = valueSource.getDomainConfig();
		DimensionConfig colorDimensionConfig = plotInstance.getCurrentPlotConfigurationClone().getDimensionConfig(
				PlotDimension.COLOR);
		SeriesFormat seriesFormat = valueSource.getSeriesFormat();

		// Loop all series and set series format.
		// Format based on dimension configs will be set later on in initFormatDelegate().
		for (int seriesIdx = 0; seriesIdx < seriesCount; ++seriesIdx) {
			// configure series paint if necessary
			if (!SeriesFormat.calculateIndividualFormatForEachItem(domainConfig, colorDimensionConfig)) {
				renderer.setSeriesPaint(seriesIdx, seriesFormat.getAreaFillPaint());
			}

			// configure general style of the bars
			renderer.setShadowVisible(false);
			renderer.setSeriesOutlinePaint(seriesIdx, PlotConfiguration.DEFAULT_SERIES_OUTLINE_PAINT);
		}
		renderer.setDrawBarOutline(true);
	}

	public static FormattedBarRenderer createBarRenderer(ValueSource valueSource, PlotInstance plotInstance) {
		FormattedBarRenderer renderer = new FormattedBarRenderer();
		configureBarRenderer(renderer, valueSource, plotInstance);
		initFormatDelegate(valueSource, renderer, plotInstance);
		return renderer;
	}

	public static FormattedStackedBarRenderer createStackedBarRenderer(ValueSource valueSource, PlotInstance plotInstance,
			boolean asPercentages) {
		FormattedStackedBarRenderer renderer = new FormattedStackedBarRenderer();
		configureBarRenderer(renderer, valueSource, plotInstance);
		initFormatDelegate(valueSource, renderer, plotInstance);
		renderer.setRenderAsPercentages(asPercentages);
		return renderer;
	}

	public static FormattedAreaRenderer createAreaRenderer(ValueSource valueSource, PlotInstance plotInstance) {
		FormattedAreaRenderer renderer = new FormattedAreaRenderer();
		configureAreaRenderer(renderer, valueSource, plotInstance);
		initFormatDelegate(valueSource, renderer, plotInstance);
		return renderer;
	}

	private static void configureAreaRenderer(AreaRenderer renderer, ValueSource valueSource, PlotInstance plotInstance) {
		ValueSourceData valueSourceData = plotInstance.getPlotData().getValueSourceData(valueSource);
		int seriesCount = valueSourceData.getSeriesCount();
		SeriesFormat seriesFormat = valueSource.getSeriesFormat();

		// Loop all series and set series format.
		// Format based on dimension configs will be set later on in initFormatDelegate().
		for (int seriesIdx = 0; seriesIdx < seriesCount; ++seriesIdx) {
			renderer.setSeriesPaint(seriesIdx, seriesFormat.getAreaFillPaint());
		}
	}

	public static FormattedStackedAreaRenderer createStackedAreaRenderer(ValueSource valueSource, PlotInstance plotInstance,
			boolean asPercentages) {
		FormattedStackedAreaRenderer renderer = new FormattedStackedAreaRenderer();
		renderer.setRenderAsPercentages(asPercentages);
		configureAreaRenderer(renderer, valueSource, plotInstance);
		initFormatDelegate(valueSource, renderer, plotInstance);
		return renderer;
	}

	public static FormattedScatterRenderer createScatterRenderer(ValueSource valueSource, PlotInstance plotInstance)
			throws ChartPlottimeException {
		FormattedScatterRenderer renderer = new FormattedScatterRenderer();
		configureScatterRenderer(renderer, valueSource, plotInstance);
		initFormatDelegate(valueSource, renderer, plotInstance);
		return renderer;
	}
}
