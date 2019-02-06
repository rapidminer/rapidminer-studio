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
package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;

import com.rapidminer.Process;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryLocationChooser;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa, Marco Boeck
 */
public class SaveAsAction extends ResourceAction {

	private static final long serialVersionUID = -6107588898380953147L;

	public SaveAsAction() {
		super("save_as");

		setCondition(EDIT_IN_PROGRESS, DONT_CARE);
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		saveAs(RapidMinerGUI.getMainFrame().getProcess());
	}

	/**
	 * Opens a location choser for the user to select where the save the process.
	 * 
	 * @param process
	 *            the process to be saved
	 * 
	 * 
	 */
	public static void saveAs(Process process) {
		saveAs(process, true);
	}

	/**
	 * Opens a location choser for the user to select where the save the process.
	 *
	 * @param process
	 * 		the process to be saved
	 * @param async
	 * 		if <code>true</code>, will save the process asynchronously after the user has
	 * 		selected a location; if <code>false</code> saves it synchronously.
	 * @return true on success, false on failure, and null if async=true
	 */
	public static Boolean saveAs(Process process, boolean async) {
		String initial = null;
		if (process.getRepositoryLocation() != null) {
			initial = process.getRepositoryLocation().toString();
		}
		String loc = RepositoryLocationChooser.selectLocation(null, initial, RapidMinerGUI.getMainFrame().getExtensionsMenu(), true, false,
				false, true, true);
		if (loc != null) {
			try {
				RepositoryLocation location = new RepositoryLocation(loc);

				Entry entry = location.locateEntry();
				if (entry instanceof ProcessEntry) {
					if (SwingTools.showConfirmDialog("overwrite", ConfirmDialog.YES_NO_OPTION, entry.getLocation()) == ConfirmDialog.NO_OPTION) {
						return false;
					}
				}
				RepositoryProcessLocation newProcessLocation = new RepositoryProcessLocation(location);
				if (newProcessLocation.getRepositoryLocation().getRepository().isReadOnly()) {
					SwingTools.showSimpleErrorMessage("save_to_read_only_repo", "", location.toString());
					// fall-through intended, SaveAction will go back to SaveAs due to read-only repository location
				} else {
					process.setProcessLocation(newProcessLocation);
				}
			} catch (MalformedRepositoryLocationException | RepositoryException e) {
				SwingTools.showSimpleErrorMessage("cannot_save_process", e, loc, e.getMessage());
				// cannot reasonably save here now, we had an unexpected error
				return false;
			}

			if (async) {
				SaveAction.saveAsync(process, true);
				return null;
			} else {
				return SaveAction.save(process, true);
			}
		}
		return false;
	}
}
