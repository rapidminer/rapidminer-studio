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
package com.rapidminer.gui.plotter.som;

import com.rapidminer.gui.plotter.ColorProvider;

import java.awt.Color;


/**
 * This class provides the color calculation for the classification areas of the SOMMOdelPlotter.
 * 
 * @author Sebastian Land
 * @deprecated since 9.2.0
 */
@Deprecated
public class SOMClassColorizer implements SOMMatrixColorizer {

	private int numberOfClasses;

	private ColorProvider colorProvider = new ColorProvider(true);

	public SOMClassColorizer(int numberOfClasses) {
		this.numberOfClasses = numberOfClasses;
	}

	@Override
	public Color getPointColor(double value) {
		double rest = value - Math.round(value);
		if (rest < 0.65 && rest > 0.35) {
			return Color.BLACK;
		}
		return colorProvider.getPointColor(((double) Math.round(value)) / (numberOfClasses - 1));
	}
}
