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
package com.rapidminer.studio.io.gui.internal.steps.configuration;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.source.DataSource;
import com.rapidminer.core.io.gui.ImportWizard;
import com.rapidminer.core.io.gui.InvalidConfigurationException;
import com.rapidminer.core.io.gui.WizardDirection;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.studio.io.gui.internal.steps.AbstractWizardStep;


/**
 * A step that allows to define the column meta data for loaded data provided by the
 * {@link DataSource}.
 *
 * @author Nils Woehler
 * @since 7.0.0
 */
public final class ConfigureDataStep extends AbstractWizardStep {

	private final ConfigureDataView view;

	/**
	 * A change listener that listens for changes to the {@link ConfigureDataView} and notifies
	 * change listeners for this step.
	 */
	private final ChangeListener changeListener = new ChangeListener() {

		@Override
		public void stateChanged(ChangeEvent e) {
			fireStateChanged();
		}
	};

	private final ImportWizard wizard;

	/**
	 * Constructor for the {@link ConfigureDataStep}. It creates the {@link ConfigureDataView}
	 * instance and adds a change listener.
	 */
	public ConfigureDataStep(ImportWizard wizard) {
		this.wizard = wizard;
		this.view = new ConfigureDataView(wizard.getDialog());
		this.view.addChangeListener(changeListener);
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
		wizard.setProgress(70);
		view.updatePreviewContent(wizard.getDataSource(DataSource.class));
	}

	@Override
	public void viewWillBecomeInvisible(WizardDirection direction) throws InvalidConfigurationException {

		// update data source meta data with configured view meta data
		final DataSource dataSource = wizard.getDataSource(DataSource.class);
		try {
			dataSource.getMetadata().configure(view.getMetaData());
		} catch (DataSetException e) {
			SwingTools.showSimpleErrorMessage("io.dataimport.step.data_column_configuration.error_configuring_metadata",
					e.getMessage());
			throw new InvalidConfigurationException();
		}
	}

	@Override
	public void validate() throws InvalidConfigurationException {
		view.validateConfiguration();
	}

	@Override
	public String getNextStepID() {
		return ImportWizard.STORE_DATA_STEP_ID;
	}

}
