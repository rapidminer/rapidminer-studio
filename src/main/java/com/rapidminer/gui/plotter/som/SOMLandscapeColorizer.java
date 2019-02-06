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

import java.awt.Color;


/**
 * This class provides a visualization for the SOMPlotter. Therefore it converts the value of a
 * point to a color of scheme, representing something like an landscape map.
 * 
 * @author Sebastian Land
 * @deprecated since 9.2.0
 */
@Deprecated
public class SOMLandscapeColorizer implements SOMMatrixColorizer {

	private Color[] colors = new Color[] { new Color(0, 0, 255), new Color(0, 255, 255), new Color(255, 255, 185),
			new Color(255, 255, 100), new Color(110, 255, 110), new Color(0, 170, 0), new Color(255, 195, 105),
			new Color(155, 95, 0), new Color(195, 195, 195), new Color(255, 255, 255) };

	private double[] intervalls = new double[] { 0, 0.2, 0.25, 0.6, 0.85, 1 };

	@Override
	public Color getPointColor(double value) {
		// finding fitting intervall
		int intervall;
		double intervallPosition = 0;
		for (intervall = 0; intervall < 5; intervall++) {
			double lowerBound = intervalls[intervall];
			double upperBound = intervalls[intervall + 1];
			if (value >= lowerBound && value <= upperBound) {
				intervallPosition = (value - lowerBound) / (upperBound - lowerBound);
				break;
			}
		}
		if (intervall >= 5) {
			return Color.BLACK;
		}
		// returning linear scaled Color of intervall
		intervall = intervall * 2;
		int redLow = colors[intervall].getRed();
		int redHigh = colors[intervall + 1].getRed();
		int red = (int) (redLow + (redHigh - redLow) * intervallPosition);
		int greenLow = colors[intervall].getGreen();
		int greenHigh = colors[intervall + 1].getGreen();
		int green = (int) (greenLow + (greenHigh - greenLow) * intervallPosition);
		int blueLow = colors[intervall].getBlue();
		int blueHigh = colors[intervall + 1].getBlue();
		int blue = (int) (blueLow + (blueHigh - blueLow) * intervallPosition);
		return new Color(red, green, blue);
	}

}
