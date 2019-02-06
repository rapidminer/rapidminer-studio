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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport;

import java.awt.Window;

import javax.swing.JComponent;

import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryLocationChooser;


/**
 * This is a WizardStep for selecting a repository location.
 * 
 * @author Tobias Malbrecht, Sebastian Land
 */
public class RepositoryLocationSelectionWizardStep extends WizardStep {

	private final RepositoryLocationChooser locationChooser;
	private AbstractWizard owner;

	/**
	 * This constructor is only left for compatibility issues with the paren extension. Hence it
	 * doesn't make any sense please refer to the
	 * {@link #RepositoryLocationSelectionWizardStep(AbstractWizard, String)} Constructor instead.
	 */
	@Deprecated
	public RepositoryLocationSelectionWizardStep(String notUsed, AbstractWizard parent, RepositoryLocation notUsedToo,
			String initialValue) {
		this(parent, initialValue);
	}

	/**
	 * This constructor is only left for compatibility issues with the paren extension. Hence it
	 * doesn't make any sense please refer to the
	 * {@link #RepositoryLocationSelectionWizardStep(AbstractWizard, String, boolean)} Constructor
	 * instead.
	 */
	@Deprecated
	public RepositoryLocationSelectionWizardStep(AbstractWizard parent, String initialValue) {
		this(parent, initialValue, false);
	}

	/**
	 * Constructor for this wizard step. If storeWizard is set to <code>true</code>, will enforce
	 * valid repository entry names.
	 */
	public RepositoryLocationSelectionWizardStep(AbstractWizard parent, String initialValue, boolean storeWizard,
			boolean onlyWriteableRepositories) {
		super("select_repository_location");
		this.owner = parent;
		this.locationChooser = new RepositoryLocationChooser(parent, null, initialValue, true, false, false,
				onlyWriteableRepositories);
		this.locationChooser.addChangeListener(parent);
		if (storeWizard) {
			this.locationChooser.setEnforceValidRepositoryEntryName(true);
		}
	}

	public RepositoryLocationSelectionWizardStep(AbstractWizard parent, String initialValue, boolean storeWizard) {
		this(parent, initialValue, storeWizard, false);
	}

	@Override
	protected boolean canGoBack() {
		return true;
	}

	@Override
	protected boolean canProceed() {
		return locationChooser.isEntryValid();
	}

	@Override
	protected JComponent getComponent() {
		return locationChooser;
	}

	public Window getOwner() {
		return owner;
	}

	public String getRepositoryLocation() {
		try {
			return locationChooser.getRepositoryLocation();
		} catch (MalformedRepositoryLocationException e) {
			// Only queried if hasSelection returned true, so we will not have an exception.
			throw new RuntimeException(e);
		}
	}

	@Override
	protected boolean performEnteringAction(WizardStepDirection direction) {
		locationChooser.requestFocusInWindow();
		return true;
	}
}
