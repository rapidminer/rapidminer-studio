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
package com.rapidminer.gui.new_plotter.data;

import com.rapidminer.gui.new_plotter.configuration.DimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.configuration.ValueSource.SeriesUsageType;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class GroupCellData {

	Map<SeriesUsageType, Map<PlotDimension, double[]>> dimensionToDataMap = new HashMap<ValueSource.SeriesUsageType, Map<PlotDimension, double[]>>();

	public Map<PlotDimension, double[]> getDataForUsageType(SeriesUsageType usageType) {
		return dimensionToDataMap.get(usageType);
	}

	public void setDataForUsageType(SeriesUsageType usageType, Map<PlotDimension, double[]> data) {
		if (data != null) {
			dimensionToDataMap.put(usageType, data);
		} else {
			dimensionToDataMap.remove(usageType);
		}
	}

	/**
	 * Returns the number of data points in the DOMAIN dimension of the MAIN_SERIES series of this
	 * GroupCellData. All other usage types should have the same number of values.
	 */
	public int getSize() {
		Map<PlotDimension, double[]> mainData = getDataForUsageType(SeriesUsageType.MAIN_SERIES);
		if (mainData == null) {
			return 0;
		}

		double[] domainData = mainData.get(PlotDimension.DOMAIN);
		if (domainData == null) {
			return 0;
		}

		return domainData.length;
	}

	/**
	 * Initializes a map from Dimension to double[] for the given SeriesUsageType. Each double array
	 * is initialized with size valueCount.
	 * 
	 * The created map is added to this GroupCellData and then returned to the caller.
	 */
	public Map<PlotDimension, double[]> initDataForUsageType(SeriesUsageType usageType, Iterable<PlotDimension> dimensions,
			int valueCount) {
		Map<PlotDimension, double[]> data = new HashMap<DimensionConfig.PlotDimension, double[]>();
		for (PlotDimension dimension : dimensions) {
			data.put(dimension, new double[valueCount]);
		}
		setDataForUsageType(usageType, data);
		return data;
	}
}
