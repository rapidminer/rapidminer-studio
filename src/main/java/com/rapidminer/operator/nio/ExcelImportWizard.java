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

import java.io.File;

import com.rapidminer.gui.wizards.ConfigurationListener;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.model.AbstractDataResultSetReader;
import com.rapidminer.operator.nio.model.DataResultSetFactory;
import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;
import com.rapidminer.repository.RepositoryLocation;


/**
 * This is the Wizard for Excel Import. It consists of several steps: - Selecting Excel file -
 * Selecting Sheet and possibly selection of sheet - Defining Annotations Step - Defining Meta Data
 *
 * @author Tobias Malbrecht, Sebastian Loh, Sebastian Land, Simon Fischer
 * @deprecated Replaced by the {@link com.rapidminer.studio.io.gui.internal.DataImportWizardBuilder} since 9.0.0
 */
@Deprecated
public class ExcelImportWizard extends AbstractDataImportWizard {

	private static final long serialVersionUID = 1L;

	public ExcelImportWizard() throws OperatorException {
		this(null, null, null);
	}

	public ExcelImportWizard(File file, RepositoryLocation preselectedLocation) throws OperatorException {
		this(file, preselectedLocation, true);
	}

	public ExcelImportWizard(File file, RepositoryLocation preselectedLocation, boolean addStoreStep)
			throws OperatorException {
		super(null, preselectedLocation, "data_import_wizard");

		// adding steps
		ExcelResultSetConfiguration excelConfig = (ExcelResultSetConfiguration) getState().getDataResultSetFactory();
		excelConfig.setWorkbookFile(file);

		ExcelSheetSelectionWizardStep wizardStep = new ExcelSheetSelectionWizardStep(excelConfig);
		wizardStep.performEnteringAction(WizardStepDirection.FORWARD);
		addStep(wizardStep);
		addCommonSteps(addStoreStep);

		layoutDefault(HUGE);
	}

	public ExcelImportWizard(final ExcelExampleSource source, final ConfigurationListener listener,
			final RepositoryLocation preselectedLocation) throws OperatorException {
		super(source, preselectedLocation, "data_import_wizard");

		// adding steps
		addStep(new ExcelFileSelectionWizardStep(this, (ExcelResultSetConfiguration) getState().getDataResultSetFactory()));
		addStep(new ExcelSheetSelectionWizardStep((ExcelResultSetConfiguration) getState().getDataResultSetFactory()));
		addCommonSteps();

		layoutDefault(HUGE);
	}

	@Override
	protected DataResultSetFactory makeFactory(final AbstractDataResultSetReader reader) throws OperatorException {
		if (reader != null) {
			return new ExcelResultSetConfiguration((ExcelExampleSource) reader);
		} else {
			return new ExcelResultSetConfiguration();
		}
	}
}
