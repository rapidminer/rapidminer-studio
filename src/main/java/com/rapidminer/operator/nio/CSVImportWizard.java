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
import com.rapidminer.operator.nio.model.CSVResultSetConfiguration;
import com.rapidminer.operator.nio.model.DataResultSetFactory;
import com.rapidminer.repository.RepositoryLocation;


/**
 * This is the Wizard for Excel Import. It consists of several steps: - Selecting Excel file -
 * Selecting Sheet and possibly selection of sheet - Defining Annotations Step - Defining Meta Data
 *
 * @author Tobias Malbrecht, Sebastian Loh, Sebastian Land, Simon Fischer
 */
@SuppressWarnings("deprecation")
public class CSVImportWizard extends AbstractDataImportWizard {

	private static final long serialVersionUID = 1L;

	public CSVImportWizard() throws OperatorException {
		this(null, null, null);
	}

	/**
	 * Using this constructor you can skip the file selection step if already known.
	 * 
	 * @param file
	 * @throws OperatorException
	 */
	public CSVImportWizard(File file, RepositoryLocation preselectedLocation) throws OperatorException {
		this(file, preselectedLocation, true);
	}

	public CSVImportWizard(File file, RepositoryLocation preselectedLocation, boolean addStoreStep) throws OperatorException {
		super(null, preselectedLocation, "data_import_wizard");

		// setting available info
		CSVResultSetConfiguration config = (CSVResultSetConfiguration) getState().getDataResultSetFactory();
		config.setCsvFile(file.getAbsolutePath());

		// adding steps
		addStep(new CSVSyntaxConfigurationWizardStep(this, (CSVResultSetConfiguration) getState().getDataResultSetFactory()));
		addCommonSteps(addStoreStep);

		layoutDefault(HUGE);
	}

	public CSVImportWizard(final CSVExampleSource source, final ConfigurationListener listener,
			final RepositoryLocation preselectedLocation) throws OperatorException {
		super(source, preselectedLocation, "data_import_wizard");

		// adding steps
		addStep(new CSVFileSelectionWizardStep(this, (CSVResultSetConfiguration) getState().getDataResultSetFactory()));
		addStep(new CSVSyntaxConfigurationWizardStep(this, (CSVResultSetConfiguration) getState().getDataResultSetFactory()));
		addCommonSteps();
		// addStep(new AnnotationDeclarationWizardStep(state));
		// addStep(new MetaDataDeclarationWizardStep(state));
		// if (source == null) {
		// addStep(new StoreDataWizardStep(this, state, (preselectedLocation != null) ?
		// preselectedLocation.getAbsoluteLocation() : null));
		// }

		layoutDefault(HUGE);
	}

	@Override
	protected DataResultSetFactory makeFactory(final AbstractDataResultSetReader source) throws OperatorException {
		if (source != null) {
			return new CSVResultSetConfiguration((CSVExampleSource) source);
		} else {
			return new CSVResultSetConfiguration();
		}
	}
}
