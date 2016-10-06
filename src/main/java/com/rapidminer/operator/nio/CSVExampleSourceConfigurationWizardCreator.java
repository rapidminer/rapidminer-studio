/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.operator.nio;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.wizards.AbstractConfigurationWizardCreator;
import com.rapidminer.gui.wizards.ConfigurationListener;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;


/**
 * Creates an {@link ExcelImportWizard}.
 * 
 * @author Sebastian Loh (06.05.2010)
 * 
 */
public class CSVExampleSourceConfigurationWizardCreator extends AbstractConfigurationWizardCreator {

	private static final long serialVersionUID = 1L;

	@Override
	public void createConfigurationWizard(ParameterType type, ConfigurationListener listener) {
		CSVExampleSource sourceOperator = (CSVExampleSource) listener;
		try {
			new CSVImportWizard(sourceOperator, listener, null).setVisible(true);
		} catch (OperatorException e) {
			SwingTools.showSimpleErrorMessage("importwizard.error_creating_wizard", e);
		}
	}

	@Override
	public String getI18NKey() {
		return "data_import_wizard";
	}
}
