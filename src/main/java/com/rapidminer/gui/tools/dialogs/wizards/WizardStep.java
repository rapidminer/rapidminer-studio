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
package com.rapidminer.gui.tools.dialogs.wizards;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.tools.I18N;


/**
 *
 * @author Tobias Malbrecht
 */
public abstract class WizardStep {

	private LinkedList<ChangeListener> listeners = new LinkedList<ChangeListener>();

	private String i18nKey;

	public WizardStep(String i18nKey) {
		this.i18nKey = i18nKey;
	}

	private String getKey() {
		return "gui.dialog.step." + i18nKey;
	}

	protected String getTitle() {
		return I18N.getMessage(I18N.getGUIBundle(), getKey() + ".title");
	}

	protected String getInfoText() {
		return I18N.getMessage(I18N.getGUIBundle(), getKey() + ".message");
	}

	protected abstract JComponent getComponent();

	protected Collection<AbstractButton> getButtons() {
		return Collections.<AbstractButton> emptyList();
	}

	/**
	 * should be called whenever a field was updated
	 */
	public void fireStateChanged() {
		ChangeEvent e = new ChangeEvent(this);
		for (ChangeListener listener : listeners) {
			listener.stateChanged(e);
		}
	}

	protected void addChangeListener(ChangeListener listener) {
		listeners.add(listener);
	}

	protected void removeChangeListener(ChangeListener listener) {
		listeners.remove(listener);
	}

	/**
	 * is called whenever a field is updated
	 * 
	 * @return proceed possible (settings complete?)
	 */
	protected abstract boolean canProceed();

	/**
	 * is called whenever a field is updated
	 * 
	 * @return can go back to previous view?
	 */
	protected abstract boolean canGoBack();

	/**
	 * This method is called when the step is entered.
	 */
	protected boolean performEnteringAction(WizardStepDirection direction) {
		return performEnteringAction();
	}

	/**
	 * This method is called when the step is left.
	 */
	protected boolean performLeavingAction(WizardStepDirection direction) {
		return performLeavingAction();
	}

	/**
	 * This method is called when the step is entered. This method is deprecated since it does not
	 * give information about the direction from where the step came.
	 * 
	 * @param direction
	 *            TODO
	 */
	@Deprecated
	protected boolean performEnteringAction() {
		return true;
	}

	/**
	 * This method is called when the step is left. This method is deprecated since it does not give
	 * information about the direction in which the step goes.
	 * 
	 * @param direction
	 *            TODO
	 */
	@Deprecated
	protected boolean performLeavingAction() {
		return true;
	}
}
