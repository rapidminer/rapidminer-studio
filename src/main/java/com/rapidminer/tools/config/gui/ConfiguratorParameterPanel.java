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
package com.rapidminer.tools.config.gui;

import javax.swing.JDialog;

import com.rapidminer.gui.properties.GenericParameterPanel;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.tools.parameter.Parameter;


/**
 * This panel can display the {@link Parameter}s of {@link Configurator}s and supports live
 * dependency changes.
 *
 * @author Marco Boeck
 *
 */
public class ConfiguratorParameterPanel extends GenericParameterPanel {

	private static final long serialVersionUID = 1L;
	private final ConfigurableDialog configurableDialog;

	/**
	 * Creates a new {@link ConfiguratorParameterPanel} for the specified {@link Parameters}.
	 *
	 * @param configurableDialog
	 *
	 * @param parameters
	 */
	public ConfiguratorParameterPanel(ConfigurableDialog configurableDialog, Parameters parameters) {
		super(parameters);
		this.configurableDialog = configurableDialog;
	}

	@Override
	protected void setValue(Operator operator, ParameterType type, String value, boolean updateComponents) {
		// always update components (otherwise live dependency changes won't work)
		setValue(operator, type, value);
	}

	@Override
	protected JDialog getDialogOwner() {
		return configurableDialog;
	}

}
