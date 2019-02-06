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
package com.rapidminer.gui.new_plotter.utility;

import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.listener.events.SeriesFormatChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.SeriesFormatChangeEvent.SeriesFormatChangeType;
import com.rapidminer.gui.new_plotter.templates.style.ColorRGB;

import java.awt.Color;


/**
 * Maps real values to a color.
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class ContinuousColorProvider implements ColorProvider {

	private double minValue;
	private double maxValue;
	private double originalMinValue;
	private double originalMaxValue;

	Color minColor;
	Color maxColor;
	int alpha;
	private boolean logarithmic;
	private boolean useGrayForOutliers;

	public ContinuousColorProvider(double minValue, double maxValue, Color minColor, Color maxColor, int alpha,
			boolean logarithmic) {
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.originalMinValue = minValue;
		this.originalMaxValue = maxValue;
		this.minColor = minColor;
		this.maxColor = maxColor;
		this.alpha = alpha;
		this.logarithmic = logarithmic;
		this.useGrayForOutliers = false;
	}

	/**
	 * Default constructor sets the minimum and maximum color to the specified in the preferences.
	 * 
	 */
	public ContinuousColorProvider(PlotInstance plotInstance, double minValue, double maxValue, int alpha,
			boolean logarithmic) {
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.originalMinValue = minValue;
		this.originalMaxValue = maxValue;
		PlotConfiguration currentPlotConfigurationClone = plotInstance.getCurrentPlotConfigurationClone();
		this.minColor = ColorRGB
				.convertToColor(currentPlotConfigurationClone.getActiveColorScheme().getGradientStartColor());
		this.maxColor = ColorRGB.convertToColor(currentPlotConfigurationClone.getActiveColorScheme().getGradientEndColor());
		this.alpha = alpha;
		this.logarithmic = logarithmic;
		this.useGrayForOutliers = false;
	}

	public int getAlpha() {
		return alpha;
	}

	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}

	public Color getMinColor() {
		return minColor;
	}

	public void setMinColor(Color minColor) {
		this.minColor = minColor;
	}

	public Color getMaxColor() {
		return maxColor;
	}

	public void setMaxColor(Color maxColor) {
		this.maxColor = maxColor;
	}

	public static Color getColorForValue(double value, int alpha, boolean logarithmic, double minValue, double maxValue,
			Color minColor, Color maxColor) {
		if (Double.isNaN(value)) {
			return Color.LIGHT_GRAY;
		}

		// map value to [0,1]
		if (minValue == maxValue) {
			value = 0.5;
		} else if (logarithmic) {
			value = (Math.log(value) - Math.log(minValue)) / (Math.log(maxValue) - Math.log(minValue));
		} else {
			value = (value - minValue) / (maxValue - minValue);
		}

		Color MIN_LEGEND_COLOR = minColor;
		Color MAX_LEGEND_COLOR = maxColor;
		float[] minCol = Color.RGBtoHSB(MIN_LEGEND_COLOR.getRed(), MIN_LEGEND_COLOR.getGreen(), MIN_LEGEND_COLOR.getBlue(),
				null);
		float[] maxCol = Color.RGBtoHSB(MAX_LEGEND_COLOR.getRed(), MAX_LEGEND_COLOR.getGreen(), MAX_LEGEND_COLOR.getBlue(),
				null);
		double hColorDiff = maxCol[0] - minCol[0];
		double sColorDiff = maxCol[1] - minCol[1];
		double bColorDiff = maxCol[2] - minCol[2];

		Color color = new Color(Color.HSBtoRGB((float) (minCol[0] + hColorDiff * value), (float) (minCol[1] + value
				* sColorDiff), (float) (minCol[2] + value * bColorDiff)));

		if (alpha < 255) {
			color = DataStructureUtils.setColorAlpha(color, alpha);
		}
		return color;
	}

	@Override
	public Color getColorForValue(double value) {
		if (Double.isNaN(value)) {
			return Color.LIGHT_GRAY;
		}

		// map value to [0,1]
		if (minValue == maxValue) {
			value = 0.5;
		} else if (value < minValue) {
			Color minColor;
			if (useGrayForOutliers) {
				minColor = Color.GRAY;
			} else {
				minColor = this.minColor;
			}
			if (alpha < 255) {
				minColor = DataStructureUtils.setColorAlpha(minColor, alpha);
			}
			return minColor;
		} else if (value > maxValue) {
			Color maxColor;
			if (useGrayForOutliers) {
				maxColor = Color.GRAY;
			} else {
				maxColor = this.maxColor;
			}
			if (alpha < 255) {
				maxColor = DataStructureUtils.setColorAlpha(maxColor, alpha);
			}
			return maxColor;
		} else if (logarithmic) {
			value = (Math.log(value) - Math.log(minValue)) / (Math.log(maxValue) - Math.log(minValue));
		} else {
			value = (value - minValue) / (maxValue - minValue);
		}

		Color MIN_LEGEND_COLOR = getMinColor();
		Color MAX_LEGEND_COLOR = getMaxColor();
		float[] minCol = Color.RGBtoHSB(MIN_LEGEND_COLOR.getRed(), MIN_LEGEND_COLOR.getGreen(), MIN_LEGEND_COLOR.getBlue(),
				null);
		float[] maxCol = Color.RGBtoHSB(MAX_LEGEND_COLOR.getRed(), MAX_LEGEND_COLOR.getGreen(), MAX_LEGEND_COLOR.getBlue(),
				null);
		double hColorDiff = maxCol[0] - minCol[0];
		double sColorDiff = maxCol[1] - minCol[1];
		double bColorDiff = maxCol[2] - minCol[2];

		Color color = new Color(Color.HSBtoRGB((float) (minCol[0] + hColorDiff * value), (float) (minCol[1] + value
				* sColorDiff), (float) (minCol[2] + value * bColorDiff)));

		if (alpha < 255) {
			color = DataStructureUtils.setColorAlpha(color, alpha);
		}
		return color;

	}

	@Override
	public boolean supportsCategoricalValues() {
		return false;
	}

	@Override
	public boolean supportsNumericalValues() {
		return true;
	}

	@Override
	public ColorProvider clone() {
		return new ContinuousColorProvider(minValue, maxValue, new Color(minColor.getRed(), minColor.getGreen(),
				minColor.getBlue()), new Color(maxColor.getRed(), maxColor.getGreen(), maxColor.getBlue()), alpha,
				logarithmic);
	}

	@Override
	public void seriesFormatChanged(SeriesFormatChangeEvent e) {
		if (e.getType() == SeriesFormatChangeType.OPACITY) {
			this.alpha = e.getOpacity();
		}
	}

	public void setLogarithmic(boolean logarithmic) {
		this.logarithmic = logarithmic;
	}

	public double getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(double maxValue) {
		this.maxValue = maxValue;
	}

	public double getMinValue() {
		return minValue;
	}

	public void setMinValue(double minValue) {
		this.minValue = minValue;
	}

	public void revertMinAndMaxValuesBackToOriginalValues() {
		this.minValue = originalMinValue;
		this.maxValue = originalMaxValue;
	}

	public boolean isUseGrayForOutliers() {
		return useGrayForOutliers;
	}

	public void setUseGrayForOutliers(boolean useGrayForOutliers) {
		this.useGrayForOutliers = useGrayForOutliers;
	}

	public boolean isColorMinMaxValueDifferentFromOriginal(double minValue, double maxValue) {
		return !(minValue == originalMinValue && maxValue == originalMaxValue);
	}
}
