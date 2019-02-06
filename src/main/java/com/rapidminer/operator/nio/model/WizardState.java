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
package com.rapidminer.operator.nio.model;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.ImportWizardUtils;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;

import java.util.logging.Level;


/**
 * The complete state of a data import wizard. Steps of the wizard communicate through this
 * interface.
 * 
 * @author Simon Fischer
 * 
 */
public class WizardState {

	private final DataResultSetTranslator translator;
	private final DataResultSetTranslationConfiguration config;
	private final DataResultSetFactory dataResultSetFactory;
	private final AbstractDataResultSetReader operator;
	private final int maxRows = ImportWizardUtils.getPreviewLength();

	private RepositoryLocation selectedLocation;

	public WizardState(AbstractDataResultSetReader operator, DataResultSetFactory dataResultSetFactory) {
		super();
		this.config = new DataResultSetTranslationConfiguration(operator);
		this.translator = new DataResultSetTranslator(operator);
		this.operator = operator;
		this.dataResultSetFactory = dataResultSetFactory;
	}

	public DataResultSetTranslator getTranslator() {
		return translator;
	}

	public DataResultSetTranslationConfiguration getTranslationConfiguration() {
		return config;
	}

	public DataResultSetFactory getDataResultSetFactory() {
		return dataResultSetFactory;
	}

	public ExampleSet readNow(DataResultSet dataResultSet, boolean previewOnly, ProgressListener progressListener)
			throws OperatorException {
		// LogService.getRoot().info("Reading example set...");
		LogService.getRoot().log(Level.INFO, "com.rapidminer.operator.nio.model.WizardState.reading_example_set");
		final DataResultSetTranslator translator = getTranslator();
		try {
			ExampleSet cachedExampleSet = translator.read(dataResultSet, getTranslationConfiguration(), previewOnly,
					progressListener);
			return cachedExampleSet;
		} finally {
			translator.close();
		}
	}

	public int getNumberOfPreviewRows() {
		return maxRows;
	}

	public AbstractDataResultSetReader getOperator() {
		return operator;
	}

	public void setSelectedLocation(RepositoryLocation selectedLocation) {
		this.selectedLocation = selectedLocation;
	}

	public RepositoryLocation getSelectedLocation() {
		return selectedLocation;
	}
}
