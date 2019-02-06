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

import com.rapidminer.gui.new_plotter.configuration.DefaultDimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.GroupCellKey;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat;
import com.rapidminer.gui.new_plotter.configuration.ValueSource.SeriesUsageType;
import com.rapidminer.gui.new_plotter.data.GroupCellData;
import com.rapidminer.gui.new_plotter.data.GroupCellKeyAndData;
import com.rapidminer.gui.new_plotter.data.GroupCellSeriesData;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.data.ValueSourceData;
import com.rapidminer.gui.new_plotter.listener.RenderFormatDelegateChangeListener;
import com.rapidminer.gui.new_plotter.listener.SeriesFormatListener;
import com.rapidminer.gui.new_plotter.listener.events.SeriesFormatChangeEvent;
import com.rapidminer.gui.new_plotter.utility.ColorProvider;
import com.rapidminer.gui.new_plotter.utility.DataStructureUtils;
import com.rapidminer.gui.new_plotter.utility.ShapeProvider;
import com.rapidminer.gui.new_plotter.utility.SizeProvider;
import com.rapidminer.gui.new_plotter.utility.ValueRange;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;


/**
 * Provides item formatting like color, shape etc. for rendering.
 * 
 * Colors: There are two possibilities: either, a series color can be set which is returned for all
 * items in a series. Alternatively a series ColorProvider can be set, which is then used to
 * retrieve colors for each value in each series. To retrieve colors for values, this class must be
 * able to access the values. Thus, if a ColorProvider for a series is set, also the series values
 * must be set via setSeriesValues().
 * 
 * If a series color is set, it has precedence over a ColorProvider for the same series.
 * 
 * The same applies for Shapes.
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class RenderFormatDelegate implements SeriesFormatListener {

	private List<RenderFormatDelegateChangeListener> listeners = new LinkedList<RenderFormatDelegateChangeListener>();

	private Vector<GroupCellKey> groupCellKeysBySeriesIdx;
	private SeriesFormat seriesFormat;
	private ColorProvider colorProvider;
	private ShapeProvider shapeProvider;
	private boolean individualColorForEachItem;
	private boolean seriesColorFromDimensionConfig;
	private Vector<double[]> colorValues;
	private boolean individualShapeForEachItem;
	private boolean seriesShapeFromDimensionConfig;
	private Vector<double[]> shapeValues;
	private boolean individualSizeForEachItem;
	private boolean seriesSizeFromDimensionConfig;
	private Vector<double[]> sizeValues;
	private Vector<double[]> selectedValues;
	private SizeProvider sizeProvider;

	public RenderFormatDelegate() {
		super();
		this.groupCellKeysBySeriesIdx = null;
	}

	public RenderFormatDelegate(ValueSourceData valueSourceData, PlotInstance plotInstance) {
		super();
		setConfiguration(valueSourceData, plotInstance);
	}

	/**
	 * If a series color for seriesIdx is set, returns that color. Otherwise returns null.
	 */
	public Paint getSeriesFillPaint(int seriesIdx) {
		if (!individualColorForEachItem) {
			if (colorProvider != null) {
				if (seriesColorFromDimensionConfig) {
					double colorIdx = getValueOfCurrentRange(seriesIdx, PlotDimension.COLOR);

					Color seriesColor = colorProvider.getColorForValue(colorIdx);
					seriesColor = DataStructureUtils.setColorAlpha(seriesColor,
							DataStructureUtils.multiplyOpacities256(seriesColor.getAlpha(), seriesFormat.getOpacity()));
					Paint seriesPaint = seriesFormat.getAreaFillPaint(seriesColor);
					return seriesPaint;
				}
			}
			return seriesFormat.getAreaFillPaint();
		} else {
			return null;
		}
	}

	/**
	 * Returns the paint to be used for drawing the item valueIdx in series seriesIdx.
	 */
	public Paint getItemPaint(int seriesIdx, int valueIdx) {
		boolean selected = isItemSelected(seriesIdx, valueIdx);
		if (!individualColorForEachItem) {
			if (colorProvider != null) {
				if (seriesColorFromDimensionConfig) {
					double colorIdx = getValueOfCurrentRange(seriesIdx, PlotDimension.COLOR);

					Color seriesColor = colorProvider.getColorForValue(colorIdx);
					seriesColor = DataStructureUtils.setColorAlpha(seriesColor,
							DataStructureUtils.multiplyOpacities256(seriesColor.getAlpha(), seriesFormat.getOpacity()));
					if (!selected) {
						// item not selected? Lower alpha
						seriesColor = DataStructureUtils.setColorAlpha(seriesColor, 20);
					}
					Paint seriesPaint = seriesFormat.getAreaFillPaint(seriesColor);
					return seriesPaint;
				}
			}
			Color color = seriesFormat.getItemColor();
			if (!selected) {
				// item not selected? Lower alpha
				color = DataStructureUtils.setColorAlpha(color, 20);
			}
			return seriesFormat.getAreaFillPaint(color);
		}

		if (colorProvider == null) {
			return null;
		} else {
			double value = getItemValue(seriesIdx, PlotDimension.COLOR, valueIdx);

			Color itemColor = colorProvider.getColorForValue(value);
			itemColor = DataStructureUtils.setColorAlpha(itemColor,
					DataStructureUtils.multiplyOpacities256(itemColor.getAlpha(), seriesFormat.getOpacity()));
			if (!selected) {
				// item not selected? Lower alpha
				itemColor = DataStructureUtils.setColorAlpha(itemColor, 20);
			}
			Paint paint = seriesFormat.getAreaFillPaint(itemColor);
			return paint;
		}
	}

	/**
	 * Returns if the given item is selected (aka value of 1 in the {@link PlotDimension#SELECTED}
	 * column)
	 * 
	 * @param seriesIdx
	 * @param valueIdx
	 * @return
	 */
	public boolean isItemSelected(int seriesIdx, int valueIdx) {
		double selectedValue = getItemValue(seriesIdx, PlotDimension.SELECTED, valueIdx);
		boolean selected = (selectedValue == 1) ? true : false;
		return selected;
	}

	private double getItemValue(int seriesIdx, PlotDimension dimension, int valueIdx) {
		switch (dimension) {
			case COLOR:
				return colorValues.get(seriesIdx)[valueIdx];
			case SHAPE:
				return shapeValues.get(seriesIdx)[valueIdx];
			case SIZE:
				return sizeValues.get(seriesIdx)[valueIdx];
			case SELECTED:
				return selectedValues.get(seriesIdx)[valueIdx];
			default:
				throw new IllegalArgumentException("getItemValue called for dimension " + dimension
						+ " which is unsupported by RenderFormatDelegate - this should not happen.");
		}
	}

	public Shape getItemShape(int seriesIdx, int valueIdx) {
		// get shape
		Shape shape;
		if (!individualShapeForEachItem) {
			Shape seriesShape;
			if (seriesShapeFromDimensionConfig) {
				double shapeIdx = getValueOfCurrentRange(seriesIdx, PlotDimension.SHAPE);
				seriesShape = shapeProvider.getShapeForCategory(shapeIdx);
			} else {
				seriesShape = seriesFormat.getItemShape().getShape();
			}
			shape = seriesShape;
		} else if (shapeProvider == null) {
			return null;
		} else {
			double shapeValue = getItemValue(seriesIdx, PlotDimension.SHAPE, valueIdx);

			// get shape
			Shape itemShape = shapeProvider.getShapeForCategory(shapeValue);

			shape = itemShape;

		}

		// scale shape
		double scalingFactor;
		if (!individualSizeForEachItem) {
			if (seriesSizeFromDimensionConfig) {
				// calculate series size
				double sizeIdx = getValueOfCurrentRange(seriesIdx, PlotDimension.SIZE);
				scalingFactor = sizeProvider.getScalingFactorForValue(sizeIdx);
			} else {
				scalingFactor = seriesFormat.getItemSize();
			}
		} else if (sizeProvider == null) {
			return null;
		} else {
			double sizeValue = getItemValue(seriesIdx, PlotDimension.SIZE, valueIdx);
			scalingFactor = sizeProvider.getScalingFactorForValue(sizeValue);
		}
		shape = scaleShape(shape, scalingFactor);
		return shape;
	}

	/**
	 * Scales a shape according to the scaling factor for value (given by the sizeProvider if one
	 * such exists). If no sizeProvider exists, the shape is returned unmodified.
	 */
	private Shape scaleShape(Shape shape, double scalingFactor) {
		// scale shape if necessary
		if (scalingFactor != 1) {
			AffineTransform t = new AffineTransform();
			t.scale(scalingFactor, scalingFactor);
			shape = t.createTransformedShape(shape);
		}
		return shape;
	}

	/**
	 * Sets the configuration of this {@link RenderFormatDelegate}. null arguments are not allowed.
	 * 
	 * @param valueSourceData
	 *            The {@link ValueSourceData} for which this {@link RenderFormatDelegate} provides
	 *            the format.
	 * @param plotInstance
	 *            The {@link PlotInstance}.
	 */
	public void setConfiguration(ValueSourceData valueSourceData, PlotInstance plotInstance) {

		// retrieve colorProvider
		DefaultDimensionConfig colorDimensionConfig = (DefaultDimensionConfig) plotInstance
				.getCurrentPlotConfigurationClone().getDimensionConfig(PlotDimension.COLOR);
		if (colorDimensionConfig != null) {
			colorProvider = plotInstance.getPlotData().getDimensionConfigData(colorDimensionConfig).getColorProvider();
		} else {
			colorProvider = null;
		}

		// retrieve shapeProvider
		DefaultDimensionConfig shapeDimensionConfig = (DefaultDimensionConfig) plotInstance
				.getCurrentPlotConfigurationClone().getDimensionConfig(PlotDimension.SHAPE);
		if (shapeDimensionConfig != null) {
			shapeProvider = plotInstance.getPlotData().getDimensionConfigData(shapeDimensionConfig).getShapeProvider();
		} else {
			shapeProvider = null;
		}

		// retrieve sizeProvider
		DefaultDimensionConfig sizeDimensionConfig = (DefaultDimensionConfig) plotInstance
				.getCurrentPlotConfigurationClone().getDimensionConfig(PlotDimension.SIZE);
		if (sizeDimensionConfig != null) {
			sizeProvider = plotInstance.getPlotData().getDimensionConfigData(sizeDimensionConfig).getSizeProvider();
		} else {
			sizeProvider = null;
		}

		// set format properties (for whole series or for individual items; from dimension config or
		// from value source config)
		individualColorForEachItem = SeriesFormat.calculateIndividualFormatForEachItem(valueSourceData.getValueSource()
				.getDomainConfig(), colorDimensionConfig);
		seriesColorFromDimensionConfig = SeriesFormat.useSeriesFormatFromDimensionConfig(valueSourceData.getValueSource()
				.getDomainConfig(), colorDimensionConfig);
		individualShapeForEachItem = SeriesFormat.calculateIndividualFormatForEachItem(valueSourceData.getValueSource()
				.getDomainConfig(), shapeDimensionConfig);
		seriesShapeFromDimensionConfig = SeriesFormat.useSeriesFormatFromDimensionConfig(valueSourceData.getValueSource()
				.getDomainConfig(), shapeDimensionConfig);
		individualSizeForEachItem = SeriesFormat.calculateIndividualFormatForEachItem(valueSourceData.getValueSource()
				.getDomainConfig(), sizeDimensionConfig);
		seriesSizeFromDimensionConfig = SeriesFormat.useSeriesFormatFromDimensionConfig(valueSourceData.getValueSource()
				.getDomainConfig(), sizeDimensionConfig);

		// retrieve series format
		seriesFormat = valueSourceData.getValueSource().getSeriesFormat();

		// copy series values if necessary
		if (individualColorForEachItem) {
			colorValues = copySeriesValues(valueSourceData, PlotDimension.COLOR);
		}
		if (individualShapeForEachItem) {
			shapeValues = copySeriesValues(valueSourceData, PlotDimension.SHAPE);
		}
		if (individualSizeForEachItem) {
			sizeValues = copySeriesValues(valueSourceData, PlotDimension.SIZE);
		}
		selectedValues = copySeriesValues(valueSourceData, PlotDimension.SELECTED);

		// copy group values if necessary
		if (seriesColorFromDimensionConfig || seriesShapeFromDimensionConfig || seriesSizeFromDimensionConfig) {
			GroupCellSeriesData seriesDataForAllGroupCells = valueSourceData.getSeriesDataForAllGroupCells();
			groupCellKeysBySeriesIdx = new Vector<GroupCellKey>(seriesDataForAllGroupCells.groupCellCount());
			for (GroupCellKeyAndData groupCellKeyAndData : seriesDataForAllGroupCells) {
				groupCellKeysBySeriesIdx.add(groupCellKeyAndData.getKey());
			}
		}

		fireChanged();
	}

	private Vector<double[]> copySeriesValues(ValueSourceData valueSourceData, PlotDimension dimension) {
		Vector<double[]> seriesValues = new Vector<double[]>(valueSourceData.getSeriesDataForAllGroupCells()
				.groupCellCount());
		for (GroupCellKeyAndData groupCellKeyAndData : valueSourceData.getSeriesDataForAllGroupCells()) {
			GroupCellData groupCellData = groupCellKeyAndData.getData();
			double[] seriesData = new double[groupCellData.getSize()];
			int i = 0;
			for (double d : groupCellData.getDataForUsageType(SeriesUsageType.MAIN_SERIES).get(dimension)) {
				seriesData[i] = d;
				++i;
			}
			seriesValues.add(seriesData);
		}
		return seriesValues;
	}

	public void addListener(RenderFormatDelegateChangeListener l) {
		listeners.add(l);
	}

	public void removeListener(RenderFormatDelegateChangeListener l) {
		listeners.remove(l);
	}

	private void fireChanged() {
		for (RenderFormatDelegateChangeListener l : listeners) {
			l.renderFormatDelegateChanged(this);
		}
	}

	@Override
	public void seriesFormatChanged(SeriesFormatChangeEvent e) {
		fireChanged();
	}

	public Color getSeriesColor(int seriesIdx) {
		if (!individualColorForEachItem) {
			if (seriesColorFromDimensionConfig) {
				double colorIdx = getValueOfCurrentRange(seriesIdx, PlotDimension.COLOR);

				int opacity = seriesFormat.getOpacity();
				Color color = colorProvider.getColorForValue(colorIdx);
				color = DataStructureUtils.setColorAlpha(color,
						DataStructureUtils.multiplyOpacities256(color.getAlpha(), opacity));
				return color;
			} else {
				return seriesFormat.getItemColor();
			}
		} else {
			return null;
		}
	}

	public SeriesFormat getSeriesFormat() {
		return seriesFormat;
	}

	private double getValueOfCurrentRange(int seriesIdx, PlotDimension dimension) {
		ValueRange currentValueRange = groupCellKeysBySeriesIdx.get(seriesIdx).getRangeForDimension(dimension);
		if (currentValueRange == null) {
			return Double.NaN;
		}
		double value = currentValueRange.getValue();
		return value;
	}

	public Paint getItemOutlinePaint(int seriesIdx, int valueIdx) {
		// TODO Auto-generated method stub
		return null;
	}
}
