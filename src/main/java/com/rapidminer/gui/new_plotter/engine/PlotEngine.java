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
package com.rapidminer.gui.new_plotter.engine;

import com.rapidminer.gui.new_plotter.PlotConfigurationError;
import com.rapidminer.gui.new_plotter.data.PlotInstance;

import java.util.List;


/**
 * Currently this interface is just an indicator interface.
 * 
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public interface PlotEngine {

	/**
	 * @return errors from the engine, e.g. PlotConfiguration settings which are not supported by
	 *         this engine and prevent the chart from being created.
	 */
	public List<PlotConfigurationError> getEngineErrors();

	/**
	 * @return errors from the engine, e.g. PlotConfiguration settings which are not supported by
	 *         this engine, but still allow the the chart to be created.
	 */
	public List<PlotConfigurationError> getEngineWarnings();

	/**
	 * @return the PlotInstance for which this engine creates a chart.
	 */
	public PlotInstance getPlotInstance();

}
