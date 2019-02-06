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
package com.rapidminer.gui.tools;

import java.awt.Color;
import java.util.Set;


/**
 * Delivers a color based on the given value scaled.
 * 
 * @author Ingo Mierswa
 */
public class CellColorProviderScaled implements CellColorProvider {

	private double min;

	private double max;

	private boolean absolute;

	private ExtendedJTable table;

	private Set<Integer> notColorizedIndices;

	public CellColorProviderScaled(ExtendedJTable table, boolean absolute, double min, double max,
			Set<Integer> notColorizedColumnIndices) {
		this.table = table;
		this.absolute = absolute;
		this.min = min;
		this.max = max;
		this.notColorizedIndices = notColorizedColumnIndices;
	}

	@Override
	public Color getCellColor(int row, int column) {
		if (notColorizedIndices.contains(column)) {
			return Color.WHITE;
		}
		Object valueObject = table.getValueAt(row, column);
		try {
			double value = Double.parseDouble(valueObject.toString());
			if (!Double.isNaN(value)) {
				if (absolute) {
					value = Math.abs(value);
				}
				float scaled = (float) ((value - min) / (max - min));
				float r = 1.0f - scaled * 0.2f;
				if (r < 0) {
					r = 0.0f;
				}
				Color color = new Color(r, r, 1.0f);
				return color;
			} else {
				return Color.WHITE;
			}
		} catch (NumberFormatException e) {
			return Color.WHITE;
		}
	}
}
