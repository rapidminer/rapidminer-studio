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
package com.rapidminer.gui.new_plotter;

/**
 * An exception which is thrown if an error occurs during the configuration of a plot.
 * 
 * For errors on application of the configuration {@see ChartPlottimeException}.
 * 
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class ChartConfigurationException extends ChartCreationException {

	private static final long serialVersionUID = 1L;

	public ChartConfigurationException(ConfigurationChangeResponse changeResponse) {
		super(changeResponse);
	}

	public ChartConfigurationException(PlotConfigurationError error) {
		super(error);
	}

	public ChartConfigurationException(String string, Object... params) {
		super(string, params);
	}

}
