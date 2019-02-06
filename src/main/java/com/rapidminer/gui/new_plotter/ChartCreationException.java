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
 * An exception for errors which occur in the plotting framework. Don't use this class directly, but
 * use its subclasses to differentiate between plottime and configuration time errors.
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public abstract class ChartCreationException extends Exception {

	private static final long serialVersionUID = 1;
	private ConfigurationChangeResponse changeResponse;

	public ChartCreationException(ConfigurationChangeResponse changeResponse) {
		this.changeResponse = changeResponse;
	}

	public ChartCreationException(PlotConfigurationError error) {
		changeResponse = new ConfigurationChangeResponse();
		changeResponse.addError(error);
	}

	public ChartCreationException(String string, Object... params) {
		PlotConfigurationError error = new PlotConfigurationError(string, params);
		changeResponse = new ConfigurationChangeResponse();
		changeResponse.addError(error);
	}

	public ConfigurationChangeResponse getResponse() {
		return changeResponse;
	}
}
