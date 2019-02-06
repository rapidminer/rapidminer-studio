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
 * This is the interface for every visualization of a SOM map. It contains only one method, which
 * returns a color for a double value. The double value represents something like the height of the
 * SOM map at one point and should represent this in the returned color.
 * 
 * @author Sebastian Land
 * @deprecated since 9.2.0
 */
@Deprecated
public interface SOMMatrixColorizer {

	public Color getPointColor(double value);
}
