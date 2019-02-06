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
package com.rapidminer.gui.plotter.charts;

import com.rapidminer.gui.plotter.charts.AbstractChartPanel.SelectionListener;


/**
 * This interface provides the hook for registering own selection listener.
 * 
 * @author Sebastian Land
 * @deprecated since 9.2.0
 */
@Deprecated
public interface SelectablePlotter {

	/**
	 * This method will add the given selection listener to the list of objects which will be
	 * notified as soon as a selection is made.
	 */
	public void registerSelectionListener(SelectionListener listener);

	/**
	 * This one clears the complete list of registered selection listeners. This might be useful if
	 * a default listener should be replaced.
	 */
	public void clearSelectionListener();

}
