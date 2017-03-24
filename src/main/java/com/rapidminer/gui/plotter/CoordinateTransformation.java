/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.gui.plotter;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;


/**
 * This interface provides methods for transforming coordinates of the components (user) space to
 * the screen position.
 * 
 * @author Sebastian Land
 */
public interface CoordinateTransformation {

	/**
	 * This method transforms the given point of the components space to screen coordinates.
	 * 
	 * @param source
	 *            TODO
	 */

	public void showPopupMenu(Point point, JComponent source, JPopupMenu menu);

	public Graphics2D getTransformedGraphics(JComponent source);

	public Point transformCoordinate(Point point, JComponent source);

	public Rectangle2D transformRectangle(Rectangle2D zoomRectangle, JComponent source);

}
