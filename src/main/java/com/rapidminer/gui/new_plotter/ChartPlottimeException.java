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
 * An exception which is thrown if an error occurs during plotting. Think of this as a
 * RuntimeException in the context of the plotter framework.
 * 
 * For errors during chart configuration {@see ChartConfigurationException}.
 * 
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class ChartPlottimeException extends ChartCreationException {

	private static final long serialVersionUID = 1L;

	public ChartPlottimeException(ConfigurationChangeResponse changeResponse) {
		super(changeResponse);
	}

	public ChartPlottimeException(PlotConfigurationError error) {
		super(error);
	}

	public ChartPlottimeException(String string, Object... params) {
		super(string, params);
	}
}
