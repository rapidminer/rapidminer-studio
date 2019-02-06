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

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.rapidminer.core.io.gui.InvalidConfigurationException;
import com.rapidminer.core.io.gui.WizardStep;
import com.rapidminer.tools.LogService;


/**
 * The {@link AbstractWizardStep} implements the handling of the next/previous button states by
 * calling the {@link #validate()} method for the next button and enabling the previous button all
 * the time.
 *
 * @author Nils Woehler
 * @since 7.0.0
 */
public abstract class AbstractWizardStep implements WizardStep {

	private final List<ChangeListener> changeListeners = new LinkedList<>();

	@Override
	public void addChangeListener(ChangeListener listener) {
		this.changeListeners.add(listener);
	}

	@Override
	public void removeChangeListener(ChangeListener listener) {
		this.changeListeners.remove(listener);
	}

	/**
	 * Fires a {@link ChangeEvent} that informs the listeners of a changed state.
	 */
	protected void fireStateChanged() {
		ChangeEvent event = new ChangeEvent(this);
		for (ChangeListener listener : changeListeners) {
			try {
				listener.stateChanged(event);
			} catch (RuntimeException rte) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.io.dataimport.AbstractWizardStep.changelistener_failed", rte);
			}
		}
	}

	@Override
	public ButtonState getNextButtonState() {
		try {
			validate();
			return ButtonState.ENABLED;
		} catch (InvalidConfigurationException e) {
			return ButtonState.DISABLED;
		}
	}

	@Override
	public ButtonState getPreviousButtonState() {
		return ButtonState.ENABLED;
	}

}
