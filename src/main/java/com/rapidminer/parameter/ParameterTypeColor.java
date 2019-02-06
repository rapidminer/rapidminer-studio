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
package com.rapidminer.parameter;

import java.awt.Color;
import java.util.logging.Level;

import com.rapidminer.tools.LogService;


/**
 * A parameter type for colors. Operators ask for the selected file with
 * {@link com.rapidminer.operator.Operator#getParameterAsColor(String)}.
 *
 * @author Ingo Mierswa
 */
public class ParameterTypeColor extends ParameterTypeString {

	private static final long serialVersionUID = 2205857626001106753L;

	public ParameterTypeColor(String key, String description, Color defaultColor) {
		super(key, description, color2String(defaultColor));
	}

	public ParameterTypeColor(String key, String description, String defaultColor) {
		super(key, description, defaultColor);
	}

	public static String color2String(Color color) {
		return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
	}

	@Override
	public String getRange() {
		return "colors";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return always {@code false}
	 */
	@Override
	public boolean isSensitive() {
		return false;
	}

	public static Color string2Color(String colorString) {
		try {
			return Color.decode(colorString);
		} catch (Exception e) {
			String[] colors = colorString.split(",");
			if (colors.length == 3) {
				return new Color(Integer.parseInt(colors[0]), Integer.parseInt(colors[1]), Integer.parseInt(colors[2]));
			} else {
				// LogService.getRoot().warning("Cannot parse color: "+colorString);
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.parameter.ParameterTypeColor.parsing_color_error",
						colorString);
				return Color.BLACK;
			}
		}
	}
}
