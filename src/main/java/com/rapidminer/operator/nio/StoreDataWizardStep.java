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

import java.util.logging.Level;

import javax.swing.SwingUtilities;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.RepositoryLocationSelectionWizardStep;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.operator.nio.model.WizardState;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.local.SimpleIOObjectEntry;
import com.rapidminer.tools.LogService;


/**
 *
 * @author Simon Fischer
 *
 */
public final class StoreDataWizardStep extends RepositoryLocationSelectionWizardStep {

	private WizardState state;

	public StoreDataWizardStep(AbstractWizard parent, WizardState state, String preselectedLocation,
			boolean onlyWriteableRepositories) {
		super(parent, preselectedLocation, true, onlyWriteableRepositories);
		this.state = state;
	}

	public StoreDataWizardStep(AbstractWizard parent, WizardState state, String preselectedLocation) {
		this(parent, state, preselectedLocation, false);
	}

	@Override
	protected boolean performLeavingAction(WizardStepDirection direction) {
		if (direction == WizardStepDirection.FINISH) {
			String repositoryLocationPath = getRepositoryLocation();
			if (repositoryLocationPath == null) {
				return false;
			}
			final RepositoryLocation location;
			try {
				location = new RepositoryLocation(repositoryLocationPath);
				Entry entry = location.locateEntry();
				if (entry != null) {
					if (entry instanceof SimpleIOObjectEntry) {
						// could overwrite, ask for permission
						if (SwingTools.showConfirmDialog(getOwner(), "overwrite", ConfirmDialog.YES_NO_OPTION,
								entry.getLocation()) == ConfirmDialog.NO_OPTION) {
							return false;
						}
					} else {
						// cannot overwrite, inform user
						SwingTools.showSimpleErrorMessage(getOwner(), "cannot_save_data_no_dataentry", "", entry.getName());
						return false;
					}
				}
			} catch (Exception e) {
				SwingTools.showSimpleErrorMessage(getOwner(), "malformed_rep_location", e, repositoryLocationPath);
				return false;
			}
			state.setSelectedLocation(location);
			new ProgressThread("importing_data", true) {

				@Override
				public void run() {
					try (DataResultSet resultSet = state.getDataResultSetFactory().makeDataResultSet(null);) {
						if (state.getTranslator() != null) {
							state.getTranslator().close();
						}

						state.getTranslator().clearErrors();
						final ExampleSet exampleSet = state.readNow(resultSet, false, getProgressListener());

						try {
							RepositoryManager.getInstance(null).store(exampleSet, location, null);
							SwingUtilities.invokeLater(new Runnable() {

								@Override
								public void run() {
									// Select repository entry
									if (RapidMinerGUI.getMainFrame() != null) {
										RapidMinerGUI.getMainFrame().getRepositoryBrowser()
												.expandToRepositoryLocation(location);
										// Switch to result
										try {
											Entry entry = location.locateEntry();
											if (entry instanceof IOObjectEntry) {
												OpenAction.showAsResult((IOObjectEntry) entry);
											}
										} catch (RepositoryException e) {
											LogService.getRoot().log(Level.WARNING, "Can not open result", e);
										}
									}
								}
							});
						} catch (RepositoryException ex) {
							SwingTools.showSimpleErrorMessage(getOwner(), "cannot_store_obj_at_location", ex, location);
							return;
						}
					} catch (Exception e) {
						SwingTools.showSimpleErrorMessage(getOwner(), "cannot_store_obj_at_location", e, location);
					} finally {
						state.getDataResultSetFactory().close();
						getProgressListener().complete();
					}
				}
			}.start();
			return true;
		} else {
			return super.performLeavingAction(direction);
		}
	}
}
