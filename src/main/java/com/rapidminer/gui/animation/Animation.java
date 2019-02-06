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
package com.rapidminer.gui.animation;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;


/**
 * Interface for an animation that can draw itself and knows if it needs to be redrawn.
 *
 * @author Gisa Schaefer
 * @since 7.1.0
 */
public interface Animation {

	/**
	 * Checks if the animation needs to be redrawn.
	 *
	 * @return {@code true} if the animation needs to be redrawn
	 */
	boolean isRedrawRequired();

	/**
	 * Draws the animation with the given graphics. The {@link Color} and the
	 * {@link AffineTransform} of the graphics are not changed by this method.
	 *
	 * @param graphics
	 *            the graphics to use for drawing
	 */
	void draw(Graphics2D graphics);

	/**
	 * Returns the bounds of the animation.
	 *
	 * @return the bounding rectangle of the animation
	 */
	Rectangle getBounds();

}
