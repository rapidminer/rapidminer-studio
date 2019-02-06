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

import com.rapidminer.gui.new_plotter.configuration.SeriesFormat;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.ItemShape;

import java.awt.Shape;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class ShapeProvider {

	private Map<Double, Shape> shapeMap;

	public ShapeProvider(Map<Double, Shape> shapeMap) {
		super();
		this.shapeMap = shapeMap;
	}

	public ShapeProvider(List<Double> categoryList) {
		super();
		this.shapeMap = createShapeMapping(categoryList);
	}

	public Shape getShapeForCategory(double category) {
		return shapeMap.get(category);
	}

	public static Map<Double, Shape> createShapeMapping(List<Double> categoryList) {
		List<ItemShape> itemShapeValues = Arrays.asList(SeriesFormat.ItemShape.values());
		Map<Double, Shape> shapeMapping = new HashMap<Double, Shape>();
		int idx = 1;
		for (Double category : categoryList) {
			if (idx < itemShapeValues.size()) {
				ItemShape itemShape = itemShapeValues.get(idx);
				if (itemShape != ItemShape.NONE) {
					shapeMapping.put(category, itemShape.getShape());
				}
			}
			++idx;
		}
		return shapeMapping;
	}

	public void setShapeForCategory(double category, Shape shape) {
		if (shape == null) {
			shapeMap.remove(category);
		} else {
			shapeMap.put(category, shape);
		}
	}

	public int maxCategoryCount() {
		return SeriesFormat.ItemShape.values().length - 1;
	}

	public boolean supportsNumericalValues() {
		return false;
	}
}
