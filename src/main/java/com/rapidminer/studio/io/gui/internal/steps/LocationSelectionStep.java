/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.studio.io.gui.internal.steps;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.rapidminer.core.io.data.source.DataSource;
import com.rapidminer.core.io.data.source.DataSourceFactory;
import com.rapidminer.core.io.data.source.DataSourceFactoryRegistry;
import com.rapidminer.core.io.gui.ImportWizard;
import com.rapidminer.core.io.gui.InvalidConfigurationException;
import com.rapidminer.core.io.gui.WizardDirection;
import com.rapidminer.core.io.gui.WizardStep;
import com.rapidminer.gui.tools.SwingTools;


/**
 * The second step of the {@link ImportWizard} which allows to select the data location of the
 * selected {@link DataSource}. It uses the {@link DataSourceFactory} to create a new location
 * selection view for the current {@link DataSource} instance which is shown to the user.
 *
 * @author Nils Woehler
 * @since 7.0.0
 *
 */
public final class LocationSelectionStep extends AbstractWizardStep {

	public static final String LOCATION_SELECTION_STEP_ID = "location_selection";

	private JPanel viewWrapper = new JPanel(new BorderLayout());

	private final ChangeListener changeListener = new ChangeListener() {

		@Override
		public void stateChanged(ChangeEvent e) {
			fireStateChanged();
		}
	};

	private WizardStep locationStep;

	private final ImportWizard wizard;

	public LocationSelectionStep(ImportWizard wizard) {
		this.wizard = wizard;
	}

	@Override
	public String getI18NKey() {
		return LOCATION_SELECTION_STEP_ID;
	}

	@Override
	public JComponent getView() {
		return viewWrapper;
	}

	@Override
	public void viewWillBecomeVisible(WizardDirection direction) throws InvalidConfigurationException {
		final DataSource dataSource = wizard.getDataSource(DataSource.class);

		// update view content
		viewWrapper.removeAll();
		if (dataSource != null) {

			SwingTools.invokeAndWait(new Runnable() {

				@Override
				public void run() {
					@SuppressWarnings("rawtypes")
					DataSourceFactory factory = DataSourceFactoryRegistry.INSTANCE.lookUp(dataSource.getClass());

					locationStep = factory.createLocationStep(wizard);

					// create and add view component
					JComponent viewComponent = locationStep.getView();
					viewWrapper.add(viewComponent, BorderLayout.CENTER);
				}

			});

			// register for location change events
			locationStep.addChangeListener(changeListener);
			locationStep.viewWillBecomeVisible(direction);
		}
	}

	@Override
	public void viewWillBecomeInvisible(WizardDirection direction) throws InvalidConfigurationException {
		locationStep.viewWillBecomeInvisible(direction);
		locationStep.removeChangeListener(changeListener);
	}

	@Override
	public void validate() throws InvalidConfigurationException {
		if (locationStep != null) {
			locationStep.validate();
		}
	}

	@Override
	public String getNextStepID() {
		if (locationStep != null) {
			return locationStep.getNextStepID();
		}
		return null;
	}

	@Override
	public ButtonState getNextButtonState() {
		if (locationStep != null) {
			return locationStep.getNextButtonState();
		} else {
			return super.getNextButtonState();
		}
	}

}
