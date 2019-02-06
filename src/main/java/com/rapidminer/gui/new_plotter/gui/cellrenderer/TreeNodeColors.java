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
package com.rapidminer.gui.new_plotter.gui.cellrenderer;

import java.awt.Color;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class TreeNodeColors {

	public static Color getNominalColor() {
		return new Color(203, 0, 204);
	}

	public static Color getNumericalColor() {
		return Color.blue;
	}

	public static Color getDateColor() {
		return new Color(1, 102, 0);
	}

	public static Color getInvalidColor() {
		return new Color(204, 0, 0);
	}

	public static Color getUnknownColor() {
		return Color.gray.brighter();
	}

	public static Color getWarningColor() {
		return new Color(204, 102, 0);
	}
}
