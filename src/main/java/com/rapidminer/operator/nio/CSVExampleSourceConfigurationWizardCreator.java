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
package com.rapidminer.operator.nio;

import javax.swing.JDialog;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.wizards.AbstractConfigurationWizardCreator;
import com.rapidminer.gui.wizards.ConfigurationListener;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.studio.io.data.internal.file.csv.CSVDataSourceFactory;
import com.rapidminer.studio.io.gui.internal.DataImportWizardBuilder;


/**
 * Creates a wizard for configuring the Read CSV operator. The wizard is the same as when using the "Import Data" button.
 *
 * @author Marcel Seifert
 *
 */
public class CSVExampleSourceConfigurationWizardCreator extends AbstractConfigurationWizardCreator {

	private static final long serialVersionUID = 1L;

	@Override
	public void createConfigurationWizard(ParameterType type, ConfigurationListener listener) {
		CSVExampleSource sourceOperator = (CSVExampleSource) listener;
		DataImportWizardBuilder builder = new DataImportWizardBuilder();
		JDialog wizard = builder.forOperator(sourceOperator, CSVDataSourceFactory.CSV_DATA_SOURCE_FACTORY_I18N_KEY).build(RapidMinerGUI.getMainFrame()).getDialog();
		wizard.setVisible(true);
	}

	@Override
	public String getI18NKey() {
		return "data_import_wizard";
	}
}
