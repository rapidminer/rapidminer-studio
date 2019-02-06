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

import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.listener.events.SeriesFormatChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.SeriesFormatChangeEvent.SeriesFormatChangeType;
import com.rapidminer.gui.new_plotter.templates.style.ColorRGB;
import com.rapidminer.gui.new_plotter.templates.style.ColorScheme;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class CategoricalColorProvider implements ColorProvider {

	private Map<Double, Color> colorMap;
	private int alpha;

	public CategoricalColorProvider(Map<Double, Color> colorMap, int alpha) {
		this.colorMap = colorMap;
		this.alpha = alpha;
		updateColorMapAlpha();
	}

	public CategoricalColorProvider(PlotInstance plotInstance, List<Double> categoryList, int alpha) {
		this.alpha = alpha;
		this.colorMap = createColorMapping(plotInstance, categoryList, alpha);
	}

	private Map<Double, Color> createColorMapping(PlotInstance plotInstance, List<Double> categoryList, int alpha) {

		// get color scheme
		ColorScheme colorScheme = plotInstance.getCurrentPlotConfigurationClone().getActiveColorScheme();

		// fill color mapping
		Map<Double, Color> colorMapping = new HashMap<Double, Color>();
		int idx = 0;
		for (Double category : categoryList) {
			colorMapping.put(category, getColorForCategoryIdx(idx, colorScheme));
			++idx;
		}
		return colorMapping;
	}

	static public Color getColorForCategoryIdx(int idx, ColorScheme colorScheme) {
		List<ColorRGB> colors = colorScheme.getColors();
		int colorListSize = colors.size();
		int darken = idx / colorListSize;

		Color categoryColor = ColorRGB.convertToColor(colors.get(idx % colorListSize));
		for (int i = 0; i < darken; ++i) {
			categoryColor = categoryColor.darker();
		}
		return categoryColor;
	}

	public void setCategoryColor(double category, Color color) {
		colorMap.put(category, DataStructureUtils.setColorAlpha(color, alpha));
	}

	@Override
	public Color getColorForValue(double value) {
		return colorMap.get(value);
	}

	private void updateColorMapAlpha() {
		Map<Double, Color> newColorMap = new HashMap<Double, Color>();
		for (Entry<Double, Color> colorEntry : colorMap.entrySet()) {
			newColorMap.put(colorEntry.getKey(), DataStructureUtils.setColorAlpha(colorEntry.getValue(), alpha));
		}
		colorMap = newColorMap;
	}

	@Override
	public boolean supportsCategoricalValues() {
		return true;
	}

	@Override
	public boolean supportsNumericalValues() {
		return false;
	}

	@Override
	public ColorProvider clone() {
		Map<Double, Color> clonedColorMap = new HashMap<Double, Color>();
		clonedColorMap.putAll(colorMap);
		return new CategoricalColorProvider(clonedColorMap, alpha);
	}

	@Override
	public void seriesFormatChanged(SeriesFormatChangeEvent e) {
		if (e.getType() == SeriesFormatChangeType.OPACITY) {
			this.alpha = e.getOpacity();
			updateColorMapAlpha();
		}
	}

	public Map<Double, Color> getColorMap() {
		return colorMap;
	}
}
