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
package com.rapidminer.gui;

/**
 * Listener to observe changes of the current Perspective.
 * 
 * @author Thilo Kamradt
 * 
 */
public interface PerspectiveChangeListener {

	/**
	 * Will be called if the current perspective changes.
	 * <p>
	 *  This method is <b>not</b> called on the EDT.
	 *  Use {@link com.rapidminer.gui.tools.SwingTools#invokeLater(Runnable) SwingTools.invokeLater} for any GUI manipulation.
	 * </p>
	 *
	 * @param perspective
	 * 		The new perspective
	 */
	public void perspectiveChangedTo(Perspective perspective);

}
