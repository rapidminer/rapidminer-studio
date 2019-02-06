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
package com.rapidminer.gui.wizards;

import java.util.Map;


/**
 * An abstract super class for the creators of configuration handling providing some basic parameter
 * handling.
 * 
 * @author Ingo Mierswa
 */
public abstract class AbstractConfigurationWizardCreator implements ConfigurationWizardCreator {

	private static final long serialVersionUID = 3622980797331677255L;

	private Map<String, String> parameters;

	@Override
	public Map<String, String> getParameters() {
		return this.parameters;
	}

	@Override
	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}
}
