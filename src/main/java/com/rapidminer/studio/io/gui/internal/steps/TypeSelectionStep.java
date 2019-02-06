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
package com.rapidminer.studio.io.gui.internal.steps;

import javax.swing.JComponent;

import com.rapidminer.core.io.gui.ImportWizard;
import com.rapidminer.core.io.gui.InvalidConfigurationException;
import com.rapidminer.core.io.gui.WizardDirection;


/**
 * The first step of the {@link ImportWizard} which allows to select the data source type.
 *
 * @author Nils Woehler
 * @since 7.0.0
 *
 */
public final class TypeSelectionStep extends AbstractWizardStep {

	private static final String TYPE_SELECTION_STEP_ID = "type_selection";

	private final TypeSelectionView view;
	private final ImportWizard wizard;

	public TypeSelectionStep(ImportWizard wizard) {
		this.wizard = wizard;
		this.view = new TypeSelectionView(wizard);
	}

	@Override
	public String getI18NKey() {
		return TYPE_SELECTION_STEP_ID;
	}

	@Override
	public JComponent getView() {
		return view;
	}

	@Override
	public ButtonState getNextButtonState() {
		// No next button is shown.
		// Next step is triggered by buttons within the type selection step.
		return ButtonState.HIDDEN;
	}

	@Override
	public ButtonState getPreviousButtonState() {
		// No previous button is shown.
		return ButtonState.HIDDEN;
	}

	@Override
	public void viewWillBecomeVisible(WizardDirection direction) {
		wizard.setProgress(0);
		view.enableButtons();
	}

	@Override
	public void viewWillBecomeInvisible(WizardDirection direction) throws InvalidConfigurationException {
		// ignore
	}

	@Override
	public void validate() throws InvalidConfigurationException {
		// ignore
	}

	@Override
	public String getNextStepID() {
		return LocationSelectionStep.LOCATION_SELECTION_STEP_ID;
	}

}
