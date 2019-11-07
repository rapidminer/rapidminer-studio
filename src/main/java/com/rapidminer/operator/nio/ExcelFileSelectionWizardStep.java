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

import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.FileSelectionWizardStep;
import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;

import java.io.File;

import javax.swing.filechooser.FileFilter;


/**
 * This step allows to select an file. With this file the {@link ExcelResultSetConfiguration} will
 * be created.
 * 
 * @author Sebastian Land
 * @deprecated Replaced by the ExcelSheetSelectionWizardStep since 9.0.0
 */
@Deprecated
public class ExcelFileSelectionWizardStep extends FileSelectionWizardStep {

	private ExcelResultSetConfiguration configuration;

	/**
	 * There must be a configuration given, but might be empty.
	 */
	public ExcelFileSelectionWizardStep(AbstractWizard parent, ExcelResultSetConfiguration configuration) {
		super(parent, new FileFilter() {

			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith("xls") || f.getName().endsWith("xlsx");
			}

			@Override
			public String getDescription() {
				return "Excel Spreadsheets (.xls, .xlsx)";
			}
		});
		this.configuration = configuration;
	}

	@Override
	protected boolean performEnteringAction(WizardStepDirection direction) {
		if (configuration.getFile() != null) {
			this.fileChooser.setSelectedFile(configuration.getFile());
		}
		return true;
	}

	@Override
	protected boolean performLeavingAction(WizardStepDirection direction) {
		configuration.setWorkbookFile(getSelectedFile());
		return true;
	}
}
