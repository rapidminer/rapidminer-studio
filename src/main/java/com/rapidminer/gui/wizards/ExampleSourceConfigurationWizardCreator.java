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

import com.rapidminer.parameter.ParameterType;


/**
 * This creator can be used to create wizards for the
 * {@link com.rapidminer.operator.io.ExampleSource} operator.
 * 
 * @author Ingo Mierswa
 */
public class ExampleSourceConfigurationWizardCreator extends AbstractConfigurationWizardCreator {

	private static final long serialVersionUID = 2690870634059674565L;

	/** Necessary for construction by reflection. */
	public ExampleSourceConfigurationWizardCreator() {}

	@Override
	public String getI18NKey() {
		return "dataloading";
	}

	@Override
	public void createConfigurationWizard(ParameterType type, ConfigurationListener listener) {
		(new ExampleSourceConfigurationWizard(listener)).setVisible(true);
	}
}
