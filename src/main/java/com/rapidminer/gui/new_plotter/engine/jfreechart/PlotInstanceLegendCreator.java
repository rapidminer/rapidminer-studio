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
import com.rapidminer.gui.new_plotter.configuration.DefaultDimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.DistinctValueGrouping;
import com.rapidminer.gui.new_plotter.configuration.LineFormat.LineStyle;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.FillStyle;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.VisualizationType;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.data.DimensionConfigData;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.engine.jfreechart.legend.CustomLegendItem;
import com.rapidminer.gui.new_plotter.engine.jfreechart.legend.FlankedShapeLegendItem;
import com.rapidminer.gui.new_plotter.utility.ColorProvider;
import com.rapidminer.gui.new_plotter.utility.ContinuousColorProvider;
import com.rapidminer.gui.new_plotter.utility.ContinuousSizeProvider;
import com.rapidminer.gui.new_plotter.utility.DataStructureUtils;
import com.rapidminer.gui.new_plotter.utility.ShapeProvider;
import com.rapidminer.gui.new_plotter.utility.SizeProvider;
import com.rapidminer.tools.I18N;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.sql.Date;
import java.text.DateFormat;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;


/**
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class PlotInstanceLegendCreator {

	// get this string by creating a closed bezier path in e.g. inkscape, save as svg and copy the
	// path from "c" to "z".
	private static final String UNDEFINED_SHAPE_PATH_STRING = "61.60505,-17.6627 4.43287,24.66755 80.8122,70.71067 32.26478,19.44992 94.06958,-36.71539 86.87313,11.11169 -11.06983,73.56922 -54.23403,-3.79434 -74.09398,31.56769 -15.44503,27.50097 110.35408,39.30287 35.87633,109.92879 -26.99372,15.97748 -24.31258,-53.77902 -48.62472,-38.78282 -18.89542,11.65506 12.73492,55.53523 -57.74657,36.48531 -30.46568,-8.23435 18.26797,-39.15956 -3.71038,-55.83427 -28.37109,-21.5248 -41.8619,91.13915 -70.90379,2.49826 -15.22703,-46.47553 37.08276,-11.0608 44.75129,-69.85281 3.03046,-23.23351 -47.77036,-82.19632 6.76649,-97.83251";
	private static final String UNDEFINED_SHAPE_AND_COLOR_PATH_STRING = "-17.05687,19.94964 -48.00093,21.59475 -70.38775,35.29712 -21.74096,13.30706 -36.83506,39.34171 -61.62405,45.27914 -25.5254,6.11382 -51.52664,-10.74373 -77.69201,-12.81692 -25.4105,-2.01338 -52.92466,10.177 -76.46931,0.40989 -24.24409,-10.05727 -35.37093,-38.97845 -55.32056,-56.03532 -19.37409,-16.56478 -48.79884,-22.87497 -62.1059,-44.61593 -13.702365,-22.38682 -5.70472,-52.32474 -11.818535,-77.85014 -5.93744,-24.78899 -26.03352,-47.18948 -24.02014,-72.59998 2.07319,-26.16537 26.14049,-45.68475 36.197755,-69.92884 9.76712,-23.54465 6.67572,-53.47921 23.24049,-72.8533 17.05687,-19.949641 48.00093,-21.594751 70.38775,-35.297121 21.74096,-13.30706 36.83506,-39.341712 61.62405,-45.279142 25.5254,-6.11382 51.52664,10.74373 77.69201,12.81692 25.4105,2.01338 52.92466,-10.177 76.46931,-0.40989 24.24409,10.057272 35.37093,38.978452 55.32056,56.035323 19.37409,16.56478 48.79884,22.87497 62.1059,44.61593 13.70237,22.38682 5.70472,52.32474 11.81854,77.85014 5.93744,24.78899 26.03352,47.18948 24.02014,72.59998 -2.07319,26.16537 -26.14049,45.68475 -36.19776,69.92884 -9.76712,23.54465 -6.67572,53.47921 -23.24049,72.8533";
	private static final Shape UNDEFINED_SHAPE = shapeFromSvgRelativeBezierPath(UNDEFINED_SHAPE_PATH_STRING, 1.0f / 20);
	private static final Shape UNDEFINED_SHAPE_AND_COLOR = shapeFromSvgRelativeBezierPath(
			UNDEFINED_SHAPE_AND_COLOR_PATH_STRING, 1.0f / 45f * 0.9f);
	private static final Color UNDEFINED_COLOR = Color.DARK_GRAY;
	private static final Paint UNDEFINED_COLOR_PAINT = createTransparentCheckeredPaint(UNDEFINED_COLOR, 1);
	private static final Paint UNDEFINED_LINE_COLOR = Color.DARK_GRAY;
	private static final Shape BAR_SHAPE = createBarShape();
	private static final Shape AREA_SHAPE = createAreaShape();
	private static final BasicStroke DEFAULT_OUTLINE_STROKE = new BasicStroke(1);
	private static final double MIN_LEGEND_ITEM_SCALING_FACTOR = 0.2;
	private static final double MAX_LEGEND_ITEM_SCALING_FACTOR = 1.5;

	public LegendItemCollection getLegendItems(PlotInstance plotInstance) {
		PlotConfiguration plotConfiguration = plotInstance.getCurrentPlotConfigurationClone();
		LegendItemCollection legendItemCollection = new LegendItemCollection();

		// Get list of ValueSources which get an own legend entry.
		// These are those ValueSources, for which useSeriesFormatForDimension() returns true
		// for at least one dimension (not considering X and VALUE).

		// get all values sources
		List<ValueSource> allValueSources = plotConfiguration.getAllValueSources();

		// add the plots heading (if we have any value sources)
		if (!allValueSources.isEmpty()) {
			legendItemCollection.add(createTitleLegendItem(I18N.getGUILabel("plotter.legend.plots_heading.label") + ":",
					plotConfiguration));
		}

		// now find those value sources for which we need a legend entry,
		// and also remember which dimensions are shown in the legend entry (color, shape, ...)
		for (ValueSource valueSource : allValueSources) {
			CustomLegendItem legendItem = createValueSourceLegendItem(plotConfiguration, valueSource);
			if (legendItem != null) {
				legendItemCollection.add(legendItem);
			}
		}

		List<Set<PlotDimension>> dimensionsWithLegend = findCompatibleDimensions(plotConfiguration, allValueSources);

		// create legend items for DimensionConfigs
		for (Set<PlotDimension> dimensionSet : dimensionsWithLegend) {
			PlotDimension aDimension = dimensionSet.iterator().next();
			DimensionConfig dimensionConfig = plotConfiguration.getDimensionConfig(aDimension);

			createDimensionConfigLegendItem(plotInstance, (DefaultDimensionConfig) dimensionConfig, dimensionSet,
					legendItemCollection);
		}
		return legendItemCollection;
	}

	private List<Set<PlotDimension>> findCompatibleDimensions(PlotConfiguration plotConfiguration,
			List<ValueSource> allValueSources) {
		Map<PlotDimension, DefaultDimensionConfig> dimensionConfigMap = plotConfiguration.getDefaultDimensionConfigs();

		// find all Dimensions for which we create a legend item
		List<Set<PlotDimension>> dimensionsWithLegend = new LinkedList<Set<PlotDimension>>();
		for (Entry<PlotDimension, DefaultDimensionConfig> dimensionEntry : dimensionConfigMap.entrySet()) {
			PlotDimension dimension = dimensionEntry.getKey();
			DefaultDimensionConfig dimensionConfig = dimensionEntry.getValue();

			boolean createLegend = false;
			if (dimensionConfig.isGrouping()) {
				createLegend = true;
			} else {
				for (ValueSource valueSource : allValueSources) {
					if (!valueSource.isUsingDomainGrouping()) {
						createLegend = true;
						break;
					}
				}
			}

			if (createLegend) {
				if (!dimensionConfig.isNominal()) {
					Set<PlotDimension> newSet = new HashSet<DimensionConfig.PlotDimension>();
					newSet.add(dimension);
					dimensionsWithLegend.add(newSet);
				} else {
					// iterate over list and find dimensions with compatible properties
					boolean compatibleToSomething = false;
					for (Set<PlotDimension> dimensionSet : dimensionsWithLegend) {
						boolean compatible = true;
						for (PlotDimension comparedDimension : dimensionSet) {
							DefaultDimensionConfig comparedDimensionConfig = (DefaultDimensionConfig) plotConfiguration
									.getDimensionConfig(comparedDimension);
							if (!comparedDimensionConfig.isNominal()) {
								compatible = false;
								break;
							}
							if (!dimensionConfig.getDataTableColumn().equals(comparedDimensionConfig.getDataTableColumn())) {
								compatible = false;
								break;
							} else if (comparedDimensionConfig.isGrouping()
									&& comparedDimensionConfig.getGrouping() instanceof DistinctValueGrouping
									&& !dimensionConfig.isGrouping() && dimensionConfig.isNominal()) {
								compatible = true;
							} else if (dimensionConfig.isGrouping()
									&& dimensionConfig.getGrouping() instanceof DistinctValueGrouping
									&& !comparedDimensionConfig.isGrouping() && comparedDimensionConfig.isNominal()) {
								compatible = true;
							} else if (dimensionConfig.isGrouping() != comparedDimensionConfig.isGrouping()) {
								compatible = false;
								break;
							} else if (!dimensionConfig.isGrouping()) {
								compatible = true;
							} else if (dimensionConfig.getGrouping().equals(comparedDimensionConfig.getGrouping())) {
								compatible = true;
							} else {
								compatible = false;
								break;
							}
						}
						if (compatible) {
							dimensionSet.add(dimension);
							compatibleToSomething = true;
							break;
						}
					}
					if (!compatibleToSomething) {
						Set<PlotDimension> newSet = new HashSet<DimensionConfig.PlotDimension>();
						newSet.add(dimension);
						dimensionsWithLegend.add(newSet);
					}
				}
			}
		}
		return dimensionsWithLegend;
	}

	private CustomLegendItem createValueSourceLegendItem(PlotConfiguration plotConfig, ValueSource valueSource) {

		Set<PlotDimension> dimensions = new HashSet<PlotDimension>();
		for (PlotDimension dimension : PlotDimension.values()) {
			switch (dimension) {
				case DOMAIN:
				case VALUE:
					break;
				default:
					if (valueSource.useSeriesFormatForDimension(plotConfig, dimension)) {
						dimensions.add(dimension);
					}
			}
		}
		if (dimensions.isEmpty()) {
			return null;
		}

		SeriesFormat format = valueSource.getSeriesFormat();
		String description = "";
		String toolTipText = "";
		String urlText = "";
		boolean shapeVisible = true;
		Shape shape;
		boolean shapeFilled = true;
		Paint fillPaint = UNDEFINED_COLOR_PAINT;
		boolean shapeOutlineVisible = true;
		Paint outlinePaint = PlotConfiguration.DEFAULT_OUTLINE_COLOR;
		Stroke outlineStroke = DEFAULT_OUTLINE_STROKE;
		boolean lineVisible = format.getLineStyle() != LineStyle.NONE
				&& format.getSeriesType() == SeriesFormat.VisualizationType.LINES_AND_SHAPES;

		// configure fill paint and line paint
		Paint linePaint;
		String label = valueSource.toString();
		if (label == null) {
			label = "";
		}
		if (dimensions.contains(PlotDimension.COLOR)) {
			Color color = format.getItemColor();
			fillPaint = format.getAreaFillPaint(color);
			linePaint = fillPaint;
		} else {
			if (format.getAreaFillStyle() == FillStyle.NONE) {
				fillPaint = new Color(0, 0, 0, 0);
				linePaint = fillPaint;
			} else if (format.getAreaFillStyle() == FillStyle.SOLID) {
				fillPaint = UNDEFINED_COLOR_PAINT;
				linePaint = UNDEFINED_LINE_COLOR;
			} else {
				fillPaint = format.getAreaFillPaint(UNDEFINED_COLOR);
				linePaint = fillPaint;
			}

		}

		VisualizationType seriesType = valueSource.getSeriesFormat().getSeriesType();
		if (seriesType == VisualizationType.LINES_AND_SHAPES) {
			if (dimensions.contains(PlotDimension.SHAPE)) {
				shape = format.getItemShape().getShape();
			} else if (dimensions.contains(PlotDimension.COLOR)) {
				shape = UNDEFINED_SHAPE;
			} else {
				shape = UNDEFINED_SHAPE_AND_COLOR;
			}

			if (dimensions.contains(PlotDimension.SIZE)) {
				AffineTransform transformation = new AffineTransform();
				double scalingFactor = format.getItemSize();
				transformation.scale(scalingFactor, scalingFactor);
				shape = transformation.createTransformedShape(shape);
			}
		} else if (seriesType == VisualizationType.BARS) {
			shape = BAR_SHAPE;
		} else if (seriesType == VisualizationType.AREA) {
			shape = AREA_SHAPE;
		} else {
			throw new RuntimeException("Unknown SeriesType. This should not happen.");
		}

		// configure line shape
		float lineLength = 0;
		if (lineVisible) {
			lineLength = format.getLineWidth();
			if (lineLength < 1) {
				lineLength = 1;
			}
			if (lineLength > 1) {
				lineLength = 1 + (float) Math.log(lineLength) / 2;
			}

			// line at least 30 pixels long, and show at least 2 iterations of stroke
			lineLength = Math.max(lineLength * 30, format.getStrokeLength() * 2);

			// line at least 2x longer than shape width
			if (shape != null) {
				lineLength = Math.max(lineLength, (float) shape.getBounds().getWidth() * 2f);
			}
		}

		// now create line shape and stroke
		Shape line = new Line2D.Float(0, 0, lineLength, 0);
		BasicStroke lineStroke = format.getStroke();
		if (lineStroke == null) {
			lineStroke = new BasicStroke();
		}

		// unset line ending decoration to prevent drawing errors in legend
		{
			BasicStroke s = lineStroke;
			lineStroke = new BasicStroke(s.getLineWidth(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, s.getMiterLimit(),
					s.getDashArray(), s.getDashPhase());
		}

		return new CustomLegendItem(label, description, toolTipText, urlText, shapeVisible, shape, shapeFilled, fillPaint,
				shapeOutlineVisible, outlinePaint, outlineStroke, lineVisible, line, lineStroke, linePaint);
	}

	private static GeneralPath createAreaShape() {
		GeneralPath areaShape = new GeneralPath();
		areaShape.moveTo(0, 0);
		areaShape.lineTo(0, -5);
		areaShape.lineTo(5, -10);
		areaShape.lineTo(10, -5);
		areaShape.lineTo(15, -7);
		areaShape.lineTo(15, 0);

		areaShape.closePath();
		return areaShape;
	}

	private static GeneralPath createBarShape() {
		GeneralPath barShape = new GeneralPath();
		barShape.moveTo(0, 0);
		barShape.lineTo(0, -5);
		barShape.lineTo(5, -5);
		barShape.lineTo(5, 0);
		barShape.lineTo(5, -15);
		barShape.lineTo(10, -15);
		barShape.lineTo(10, 0);
		barShape.lineTo(10, -10);
		barShape.lineTo(15, -10);
		barShape.lineTo(15, 0);
		barShape.closePath();
		return barShape;
	}

	private void createDimensionConfigLegendItem(PlotInstance plotInstance, DefaultDimensionConfig dimensionConfig,
			Set<PlotDimension> dimensionSet, LegendItemCollection legendItemCollection) {
		PlotConfiguration plotConfiguration = plotInstance.getCurrentPlotConfigurationClone();
		DimensionConfigData dimensionConfigData = plotInstance.getPlotData().getDimensionConfigData(dimensionConfig);
		if (dimensionConfig.isGrouping()) {
			// create legend entry based on the grouping
			if (dimensionConfig.isNominal()) {
				// create categorical legend --> one item for each category
				createCategoricalLegendItems(plotInstance, dimensionSet, legendItemCollection,
						dimensionConfigData.getDistinctValues());
			} else if (dimensionConfig.isNumerical() || dimensionConfig.isDate()) {
				createDimensionTitleLegendItem(plotInstance, dimensionSet, legendItemCollection);

				// create one continuous legend item
				double minValue = dimensionConfigData.getMinValue();
				double maxValue = dimensionConfigData.getMaxValue();

				LegendItem legendItem = createContinuousLegendItem(plotInstance, dimensionSet, minValue, maxValue,
						dimensionConfig.isDate() ? dimensionConfig.getDateFormat() : null);
				if (legendItem != null) {
					legendItemCollection.add(legendItem);
				}
			} else {
				throw new RuntimeException("unknown data type during legend creation - this should not happen");
			}
		} else {
			// dimension config not grouping --> create legend item only, if there exists
			// at least one non-aggregated value source (otherwise the dimension config is
			// not used at all in the plot and thus we also don't need a legend item for it).
			boolean createLegend = false;
			for (ValueSource valueSource : plotConfiguration.getAllValueSources()) {
				if (!valueSource.isUsingDomainGrouping()) {
					createLegend = true;
					break;
				}
			}
			if (createLegend) {
				// create legend based on the attribute values on the dimension config
				if (dimensionConfig.isNominal()) {
					// create one legend item for each nominal value
					List<Double> values = dimensionConfigData.getDistinctValues();
					createCategoricalLegendItems(plotInstance, dimensionSet, legendItemCollection, values);
				} else if (dimensionConfig.isNumerical() || dimensionConfig.isDate()) {
					createDimensionTitleLegendItem(plotInstance, dimensionSet, legendItemCollection);

					// create one continuous legend item for the value range
					double minValue = dimensionConfigData.getMinValue();
					double maxValue = dimensionConfigData.getMaxValue();

					LegendItem legendItem = createContinuousLegendItem(plotInstance, dimensionSet, minValue, maxValue,
							dimensionConfig.isDate() ? dimensionConfig.getDateFormat() : null);
					if (legendItem != null) {
						legendItemCollection.add(legendItem);
					}
				} else {
					throw new RuntimeException("unknown data type during legend creation - this should not happen");
				}
			}
		}
	}

	/**
	 * Creates a continuous legend item for one item in dimensionSet, i.e. dimensionSet must be a
	 * set containing exactly one value.
	 * 
	 * @param dateFormat
	 *            format used to format minValue and maxValue as dates, or null if they should be
	 *            displayed numerically instead of as dates
	 * @throws ChartPlottimeException
	 */
	private LegendItem createContinuousLegendItem(PlotInstance plotInstance, Set<PlotDimension> dimensionSet,
			double minValue, double maxValue, DateFormat dateFormat) {
		PlotConfiguration plotConfiguration = plotInstance.getCurrentPlotConfigurationClone();
		PlotDimension dimension = dimensionSet.iterator().next();
		DefaultDimensionConfig dimensionConfig = (DefaultDimensionConfig) plotConfiguration.getDimensionConfig(dimension);
		DimensionConfigData dimensionConfigData = plotInstance.getPlotData().getDimensionConfigData(dimensionConfig);
		// String label = dimensionConfig.getLabel();
		// if(label == null) {
		// label = I18N.getGUILabel("plotter.unnamed_value_label");
		// }
		String label = "";

		if (dimension == PlotDimension.COLOR) {
			ColorProvider colorProvider = dimensionConfigData.getColorProvider();
			if (!colorProvider.supportsNumericalValues()) {
				throw new RuntimeException("Color provider for continuous legend item does not support numerical values.");
			}

			// shape dimensions
			final int width = 50;
			final int height = 10;

			// create item paint

			// first disable logarithmic scale on color provider ( -> linear gradient in legend)
			// ContinuousColorProvider continuousColorProvider = null;
			// if (dimensionConfig.isLogarithmic() && colorProvider instanceof
			// ContinuousColorProvider) {
			// continuousColorProvider = (ContinuousColorProvider)colorProvider;
			// continuousColorProvider.setLogarithmic(false);
			// }

			// calculate gradient
			float fractions[] = new float[width];
			Color colors[] = new Color[width];
			for (int i = 0; i < width; ++i) {

				float fraction = i / (width - 1.0f);
				double fractionValue;
				if (colorProvider instanceof ContinuousColorProvider
						&& ((ContinuousColorProvider) colorProvider).isColorMinMaxValueDifferentFromOriginal(
								((ContinuousColorProvider) colorProvider).getMinValue(),
								((ContinuousColorProvider) colorProvider).getMaxValue())) {
					fractionValue = ((ContinuousColorProvider) colorProvider).getMinValue()
							+ fraction
							* (((ContinuousColorProvider) colorProvider).getMaxValue() - ((ContinuousColorProvider) colorProvider)
									.getMinValue());
				} else {
					fractionValue = minValue + fraction * (maxValue - minValue);
				}
				colors[i] = colorProvider.getColorForValue(fractionValue);
				fractions[i] = fraction;
			}
			LinearGradientPaint shapeFillPaint = new LinearGradientPaint(new Point(0, 0), new Point(width, 0), fractions,
					colors, CycleMethod.REPEAT);

			// reset color provider to logarithmic if necessary
			// if (continuousColorProvider != null && dimensionConfig.isLogarithmic()) {
			// continuousColorProvider.setLogarithmic(true);
			// }

			// create item shape
			Rectangle itemShape = new Rectangle(width, height);

			if (colorProvider instanceof ContinuousColorProvider) {
				return createFlankedShapeLegendItem(label, ((ContinuousColorProvider) colorProvider).getMinValue(),
						((ContinuousColorProvider) colorProvider).getMaxValue(), itemShape, shapeFillPaint, true, dateFormat);
			} else {
				return createFlankedShapeLegendItem(label, minValue, maxValue, itemShape, shapeFillPaint, true, dateFormat);
			}
		} else if (dimension == PlotDimension.SHAPE) {
			// shape provider probably never supports numerical values
			return null;
		} else if (dimension == PlotDimension.SIZE) {
			SizeProvider sizeProvider = dimensionConfigData.getSizeProvider();

			if (!sizeProvider.supportsNumericalValues()) {
				throw new RuntimeException("Size provider for continuous legend item does not support numerical values.");
			}

			double minScalingFactor = sizeProvider.getMinScalingFactor();
			double maxScalingFactor = sizeProvider.getMaxScalingFactor();
			ContinuousSizeProvider legendSizeProvider = new ContinuousSizeProvider(minScalingFactor, maxScalingFactor,
					MIN_LEGEND_ITEM_SCALING_FACTOR, MAX_LEGEND_ITEM_SCALING_FACTOR, false);

			int legendItemCount = 4;
			Area composedShape = new Area();
			Shape originalShape = UNDEFINED_SHAPE;
			if (dimensionSet.contains(PlotDimension.SIZE) && dimensionSet.size() == 1) {
				originalShape = UNDEFINED_SHAPE_AND_COLOR;
			}
			double maxHeight = originalShape.getBounds().getHeight() * MAX_LEGEND_ITEM_SCALING_FACTOR;
			for (int i = 0; i < legendItemCount; ++i) {
				double fraction = minScalingFactor + ((double) i / legendItemCount * (maxScalingFactor - minScalingFactor));
				double legendScalingFactor = legendSizeProvider.getScalingFactorForValue(fraction);

				double composedWidth = composedShape.getBounds().getWidth();

				AffineTransform t = new AffineTransform();
				t.scale(legendScalingFactor, legendScalingFactor);
				Shape shape = t.createTransformedShape(originalShape);

				t = new AffineTransform();
				double shapeWidth = shape.getBounds().getWidth();
				double shapeHeight = shape.getBounds().getHeight();
				t.translate(composedWidth + shapeWidth * .1, (maxHeight - shapeHeight) / 2.0);
				t.translate(-shape.getBounds().getMinX(), -shape.getBounds().getMinY());
				shape = t.createTransformedShape(shape);
				composedShape.add(new Area(shape));
			}

			return createFlankedShapeLegendItem(label, minValue, maxValue, composedShape, UNDEFINED_COLOR_PAINT, false,
					dateFormat);

		} else {
			throw new RuntimeException("Unsupported dimension. Execution path should never reach this line.");
		}
	}

	private LegendItem createFlankedShapeLegendItem(String label, double minValue, double maxValue, Shape itemShape,
			Paint shapeFillPaint, boolean shapeOutlineVisible, DateFormat dateFormat) {
		// configure legend item
		String description = "";
		String toolTipText = "";
		String urlText = "";
		boolean shapeVisible = true;
		boolean shapeFilled = true;
		Paint outlinePaint = Color.BLACK;
		Stroke outlineStroke = DEFAULT_OUTLINE_STROKE;
		boolean lineVisible = false;
		Shape line = new Line2D.Float();
		Stroke lineStroke = new BasicStroke();	// basic stroke is fine here, since continuous legend
												// item does not show a line
		Paint linePaint = Color.BLACK;

		// create legend item
		FlankedShapeLegendItem legendItem = new FlankedShapeLegendItem(label, description, toolTipText, urlText,
				shapeVisible, itemShape, shapeFilled, shapeFillPaint, shapeOutlineVisible, outlinePaint, outlineStroke,
				lineVisible, line, lineStroke, linePaint);

		if (dateFormat != null) {
			legendItem.setLeftShapeLabel(dateFormat.format(new Date((long) minValue)));
			legendItem.setRightShapeLabel(dateFormat.format(new Date((long) maxValue)));
		} else {
			// set intelligently rounded strings as labels
			int powerOf10 = DataStructureUtils.getOptimalPrecision(minValue, maxValue);
			legendItem.setLeftShapeLabel(DataStructureUtils.getRoundedString(minValue, powerOf10 - 1));
			legendItem.setRightShapeLabel(DataStructureUtils.getRoundedString(maxValue, powerOf10 - 1));
		}

		return legendItem;
	}

	private void createCategoricalLegendItems(PlotInstance plotInstance, Set<PlotDimension> dimensionSet,
			LegendItemCollection legendItemCollection, Iterable<Double> values) {
		createDimensionTitleLegendItem(plotInstance, dimensionSet, legendItemCollection);

		PlotConfiguration plotConfig = plotInstance.getCurrentPlotConfigurationClone();

		Shape defaultShape = new Ellipse2D.Float(-5f, -5f, 10f, 10f);
		Color defaultOutlineColor = PlotConfiguration.DEFAULT_OUTLINE_COLOR;
		ColorProvider colorProvider = null;
		ShapeProvider shapeProvider = null;
		SizeProvider sizeProvider = null;

		DefaultDimensionConfig dimensionConfig = (DefaultDimensionConfig) plotConfig.getDimensionConfig(dimensionSet
				.iterator().next());
		DimensionConfigData dimensionConfigData = plotInstance.getPlotData().getDimensionConfigData(dimensionConfig);
		for (PlotDimension dimension : dimensionSet) {
			if (dimension == PlotDimension.COLOR) {
				colorProvider = dimensionConfigData.getColorProvider();
			} else if (dimension == PlotDimension.SHAPE) {
				shapeProvider = dimensionConfigData.getShapeProvider();
			} else if (dimension == PlotDimension.SIZE) {
				sizeProvider = dimensionConfigData.getSizeProvider();
			}
		}

		// initialize size scale for legend
		ContinuousSizeProvider legendSizeProvider = null;
		if (sizeProvider != null) {
			double minScalingFactor = sizeProvider.getMinScalingFactor();
			double maxScalingFactor = sizeProvider.getMaxScalingFactor();
			double minLegendScalingFactor = MIN_LEGEND_ITEM_SCALING_FACTOR;
			double maxLegendScalingFactor = MAX_LEGEND_ITEM_SCALING_FACTOR;
			if (minScalingFactor > maxScalingFactor) {
				double tmp = minScalingFactor;
				minScalingFactor = maxScalingFactor;
				maxScalingFactor = tmp;
				minLegendScalingFactor = MAX_LEGEND_ITEM_SCALING_FACTOR;
				maxLegendScalingFactor = MIN_LEGEND_ITEM_SCALING_FACTOR;
			}
			legendSizeProvider = new ContinuousSizeProvider(minScalingFactor, maxScalingFactor, minLegendScalingFactor,
					maxLegendScalingFactor, false);
		}

		for (Double value : values) {
			// configure shape and stroke
			Shape shape = defaultShape;
			BasicStroke outlineStroke;
			Color outlineColor = new Color(0, 0, 0, 0);
			if (shapeProvider != null) {
				shape = shapeProvider.getShapeForCategory(value);
				outlineStroke = DEFAULT_OUTLINE_STROKE;
				outlineColor = defaultOutlineColor;
			} else {
				outlineStroke = new BasicStroke();
				if (colorProvider != null) {
					shape = UNDEFINED_SHAPE;
				} else {
					shape = UNDEFINED_SHAPE_AND_COLOR;
				}
			}

			// configure fill paint
			Paint paint = UNDEFINED_COLOR_PAINT;
			if (colorProvider != null) {
				paint = colorProvider.getColorForValue(value);
			}

			double scalingFactor = 1;
			if (sizeProvider != null) {
				// scale shape according to sizeProvider
				scalingFactor = sizeProvider.getScalingFactorForValue(value);
				// scale shape to fit into legend
				scalingFactor = legendSizeProvider.getScalingFactorForValue(scalingFactor);
				AffineTransform transformation = new AffineTransform();
				transformation.scale(scalingFactor, scalingFactor);
				shape = transformation.createTransformedShape(shape);
			}

			String label = dimensionConfigData.getStringForValue(value);
			if (label == null) {
				label = "";
			}

			CustomLegendItem legendItem = new CustomLegendItem(label, null, null, null, shape, paint, outlineStroke,
					outlineColor);
			legendItemCollection.add(legendItem);
		}
	}

	/**
	 * Creates the headings for the dimension config items ( like "Color (attribute X)" ) and adds
	 * it to legendItemCollection.
	 */
	private void createDimensionTitleLegendItem(PlotInstance plotInstance, Set<PlotDimension> dimensionSet,
			LegendItemCollection legendItemCollection) {
		PlotConfiguration plotConfig = plotInstance.getCurrentPlotConfigurationClone();
		StringBuilder titleBuilder = new StringBuilder();
		boolean first = true;
		boolean showDimensionType = plotConfig.getLegendConfiguration().isShowDimensionType();
		if (showDimensionType) {
			for (PlotDimension dimension : dimensionSet) {
				if (!first) {
					titleBuilder.append(", ");
				}
				titleBuilder.append(dimension.getShortName());
				first = false;
			}
		}

		if (showDimensionType) {
			titleBuilder.append(" (");
		}

		// get unique dimension labels:
		Set<String> uniqueDimensionLabels = new HashSet<String>();
		first = true;
		for (PlotDimension dimension : dimensionSet) {
			DefaultDimensionConfig dimensionConfig = (DefaultDimensionConfig) plotConfig.getDimensionConfig(dimension);
			String label = dimensionConfig.getLabel();
			if (label == null) {
				label = I18N.getGUILabel("plotter.unnamed_value_label");
			}

			if (!uniqueDimensionLabels.contains(label)) {
				if (!first) {
					titleBuilder.append(", ");
					first = false;
				}
				titleBuilder.append(label);
				uniqueDimensionLabels.add(label);
			}
		}
		if (showDimensionType) {
			titleBuilder.append(")");
		}
		titleBuilder.append(": ");

		legendItemCollection.add(createTitleLegendItem(titleBuilder.toString(), plotConfig));
	}

	/**
	 * Creates a title item (i.e. bold font etc.) with the given string. Simply gets the default
	 * font from the plotConfig and sets it style to bold.
	 * 
	 * @return The created legend item.
	 */
	private LegendItem createTitleLegendItem(String titleString, PlotConfiguration plotConfiguration) {
		LegendItem titleItem = new LegendItem(titleString, "", "", "", false, new Rectangle(), false, Color.WHITE, false,
				Color.WHITE, new BasicStroke(), false, new Rectangle(), new BasicStroke(), Color.WHITE);
		Font titleFont = titleItem.getLabelFont();

		if (titleFont == null) {
			titleFont = plotConfiguration.getLegendConfiguration().getLegendFont();
		}
		titleItem.setLabelFont(titleFont.deriveFont(Font.BOLD));
		return titleItem;
	}

	private static Paint createTransparentCheckeredPaint(Color color, int checkerSize) {
		int s = checkerSize;
		BufferedImage bufferedImage = new BufferedImage(2 * s, 2 * s, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2 = bufferedImage.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, // Anti-alias!
				RenderingHints.VALUE_ANTIALIAS_ON);

		Color c1 = DataStructureUtils.setColorAlpha(color, (int) (color.getAlpha() * .8));
		Color c2 = DataStructureUtils.setColorAlpha(color, (int) (color.getAlpha() * .2));
		g2.setStroke(new BasicStroke(0));
		g2.setPaint(c2);
		g2.setColor(c2);
		g2.fillRect(0, 0, s, s);
		g2.fillRect(s, s, s, s);
		g2.setPaint(c1);
		g2.setColor(c1);
		g2.fillRect(0, s, s, s);
		g2.fillRect(s, 0, s, s);

		// paint with the texturing brush
		Rectangle2D rect = new Rectangle2D.Double(0, 0, 2 * s, 2 * s);
		return new TexturePaint(bufferedImage, rect);
	}

	public static Shape shapeFromSvgRelativeBezierPath(String pathString, float scalingFactor) {
		String[] points = pathString.split(" ");
		float x1, y1, x2, y2, cx1, cy1, cx2, cy2;
		x2 = y2 = 0;

		float s = scalingFactor;

		Path2D.Float path = new Path2D.Float();
		for (int i = 0; i < points.length / 3; ++i) {
			String c1String = points[i * 3 + 0];
			String c2String = points[i * 3 + 1];
			String targetString = points[i * 3 + 2];

			String[] c1Split = c1String.split(",");
			String[] c2Split = c2String.split(",");
			String[] targetSplit = targetString.split(",");
			x1 = x2;
			y1 = y2;
			x2 = s * Float.parseFloat(targetSplit[0]) + x1;
			y2 = s * Float.parseFloat(targetSplit[1]) + y1;
			cx1 = s * Float.parseFloat(c1Split[0]) + x1;
			cy1 = s * Float.parseFloat(c1Split[1]) + y1;
			cx2 = s * Float.parseFloat(c2Split[0]) + x1;
			cy2 = s * Float.parseFloat(c2Split[1]) + y1;

			CubicCurve2D.Float curve = new CubicCurve2D.Float(x1, y1, cx1, cy1, cx2, cy2, x2, y2);
			path.append(curve, true);
		}
		return path;
	}
}
