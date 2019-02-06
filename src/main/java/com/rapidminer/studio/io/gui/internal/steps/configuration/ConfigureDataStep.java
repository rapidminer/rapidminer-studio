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
package com.rapidminer.studio.io.gui.internal.steps.configuration;

import java.text.DateFormat;
import java.util.logging.Level;
import javax.swing.JComponent;

import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.source.DataSource;
import com.rapidminer.core.io.gui.ImportWizard;
import com.rapidminer.core.io.gui.InvalidConfigurationException;
import com.rapidminer.core.io.gui.WizardDirection;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.nio.model.AbstractDataResultSetReader;
import com.rapidminer.studio.io.gui.internal.steps.AbstractWizardStep;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * A step that allows to define the column meta data for loaded data provided by the
 * {@link DataSource}.
 *
 * @author Nils Woehler, Marcel Seifert
 * @since 7.0.0
 */
public final class ConfigureDataStep extends AbstractWizardStep {

	private final ConfigureDataView view;

	private final ImportWizard wizard;
	private boolean storeData = true;

	private AbstractDataResultSetReader reader;


	/**
	 * Constructor for the {@link ConfigureDataStep} for data import purposes. It creates the
	 * {@link ConfigureDataView} instance and adds a change listener.
	 */
	public ConfigureDataStep(ImportWizard wizard) {
		this.wizard = wizard;
		this.view = new ConfigureDataView(wizard.getDialog());
		// A change listener that listens for changes to the {@link ConfigureDataView} and notifies change listeners for this step.
		this.view.addChangeListener(e -> fireStateChanged());
	}

	/**
	 * Constructor for the {@link ConfigureDataStep} as the last wizard step for operator configuration purposes. It
	 * creates the {@link ConfigureDataView} instance and adds a change listener.
	 *
	 * @param reader
	 * 		the reader for the data
	 * @param wizard
	 * 		the wizard for importing
	 * @param storeData
	 * 		whether data storing is allowed at the end of the wizard or not
	 * @since 9.0.0
	 */
	public ConfigureDataStep(ImportWizard wizard, AbstractDataResultSetReader reader, boolean storeData) {
		this(wizard);
		this.reader = reader;
		this.storeData = storeData;
	}

	@Override
	public String getI18NKey() {
		return ImportWizard.CONFIGURE_DATA_STEP_ID;
	}

	@Override
	public JComponent getView() {
		return view;
	}

	@Override
	public void viewWillBecomeVisible(WizardDirection direction) throws InvalidConfigurationException {
		wizard.setProgress(storeData ? 70 : 100);
		view.updatePreviewContent(wizard.getDataSource(DataSource.class));
	}

	@Override
	public void viewWillBecomeInvisible(WizardDirection direction) throws InvalidConfigurationException {
		view.cancelLoading();

		// update data source meta data with configured view meta data
		final DataSource dataSource = wizard.getDataSource(DataSource.class);
		DateFormat originalFormat;
		DateFormat chosenFormat;
		try {
			originalFormat = dataSource.getMetadata().getDateFormat();
			dataSource.getMetadata().configure(view.getMetaData());
			chosenFormat = dataSource.getMetadata().getDateFormat();
		} catch (DataSetException e) {
			SwingTools.showSimpleErrorMessage(wizard.getDialog(),
					"io.dataimport.step.data_column_configuration.error_configuring_metadata", e.getMessage());
			throw new InvalidConfigurationException();
		}

		// Log guessed and chosen format
		if (direction.equals(WizardDirection.NEXT) && !originalFormat.equals(chosenFormat)) {
			ActionStatisticsCollector.getInstance().logGuessedDateFormat(view.getGuessedDateFormat(), chosenFormat);
		}

		// we are in the last step of configuring an operator
		if (direction.equals(WizardDirection.NEXT) && reader != null) {
			try {
				reader.configure(dataSource);
			} catch (DataSetException | NumberFormatException e) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.io.gui.internal.steps.configuration.ConfigureDataStep.operator_configuration_error",
						reader.getName());
			}
		}

	}

	@Override
	public ButtonState getPreviousButtonState() {
		// Prevent a partial initialized state
		return view.isInitialized() ? ButtonState.ENABLED : ButtonState.DISABLED;
	}

	@Override
	public void validate() throws InvalidConfigurationException {
		view.validateConfiguration();
	}

	@Override
	public String getNextStepID() {
		return storeData ? ImportWizard.STORE_DATA_STEP_ID : null;
	}

}
