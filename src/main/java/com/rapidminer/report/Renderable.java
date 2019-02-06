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
package com.rapidminer.report;

import java.awt.Graphics;


/**
 * This interface provides methods for exporting visual components.
 * 
 * @author Sebastian Land
 */
public interface Renderable extends Reportable {

	/** Will be invoked before rendering and even before render width and height retrieval. */
	public void prepareRendering();

	/**
	 * Will be invoked directly after rendering and gives the object the chance to perform some
	 * clean-up.
	 */
	public void finishRendering();

	/**
	 * This method paints the visual representation onto the given graphics
	 * 
	 * @param graphics
	 *            the graphics to render onto
	 */
	public void render(Graphics graphics, int width, int height);

	/**
	 * This method returns the pixel width the rendering needs
	 * 
	 * @param preferredWidth
	 *            tells the renderable of the size it should deliver best
	 * @return the pixel width
	 */
	public int getRenderWidth(int preferredWidth);

	/**
	 * This method return the pixel height the rendering needs
	 * 
	 * @param preferredHeight
	 *            tells the renderable of the size it should deliver best
	 * @return the pixel height
	 */
	public int getRenderHeight(int preferredHeight);

}
