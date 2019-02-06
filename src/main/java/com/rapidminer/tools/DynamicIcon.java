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
package com.rapidminer.tools;

import java.awt.Graphics2D;


/**
 * This interface can be used to implement a dynamic icon, i.e. a progress bar. Such an icon may
 * then for example be used in an HTML document.
 * <p>
 * Usage: <br/>
 * If the identifier is <code>progress</code>, the HTML icon tag would look like this:
 * </p>
 * <p>
 * <code>&lt;img src=\"dynicon://progress/100/14/50&gt;</code>
 * </p>
 * <p>
 * where the first number after the identifier is the width of the icon in px, the second number is
 * the height in px and the third number is the percentage (0-100).
 * </p>
 * 
 * @author Marco Boeck
 * 
 */
public interface DynamicIcon {

	/**
	 * Draws the icon on the graphics context. To use the icon in an HTML document, see the
	 * following paragraph.
	 * <p>
	 * Usage: <br/>
	 * If the identifier is <code>progress</code>, the HTML icon tag would look like this:
	 * </p>
	 * <p>
	 * <code>&lt;img src=\"dynicon://progress/100/14/50&gt;</code>
	 * </p>
	 * <p>
	 * where the first number after the identifier is the width of the icon in px, the second number
	 * is the height in px and the third number is the percentage (0-100).
	 * </p>
	 * 
	 * @param g2
	 * @param width
	 *            the width of the icon to draw in px
	 * @param height
	 *            the height of the icon to draw in px
	 * @param percentage
	 *            the percentage which should be used to draw the appropriate icon. 0 <=
	 *            <code>value</code> <= 100
	 */
	public void drawIcon(Graphics2D g2, int width, int height, int percentage);
}
