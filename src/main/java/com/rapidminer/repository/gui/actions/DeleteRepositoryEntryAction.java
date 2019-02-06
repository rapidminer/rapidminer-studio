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
package com.rapidminer.repository.gui.actions;

import java.util.logging.Level;

import javax.swing.SwingUtilities;

import com.rapidminer.gui.tools.ProgressThreadDialog;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryTree;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.PasswordInputCanceledException;


/**
 * This action deletes the selected entry.
 *
 * @author Simon Fischer, Adrian Wilke
 */
public class DeleteRepositoryEntryAction extends AbstractRepositoryAction<Entry> {

	private static final long serialVersionUID = 1L;

	public static final String I18N_KEY = "repository_delete_entry";


	public DeleteRepositoryEntryAction(RepositoryTree tree) {
		super(tree, Entry.class, true, I18N_KEY);
	}

	@Override
	public void actionPerformed(Entry entry) {

		// Get location name for error dialog

		String locationName = "";
		RepositoryLocation location = entry.getLocation();
		if (location != null) {
			locationName = location.getName();
		}

		// Delete entry

		try {
			entry.delete();
		} catch (RepositoryException e) {

			// no extra dialog if login dialog was canceled
			if (!(e.getCause() instanceof PasswordInputCanceledException)) {

				// Retry-dialog on error
				ConfirmDialog dialog = null;
				String errorMessage = e.getMessage();
				errorMessage = errorMessage != null ? errorMessage.trim() : "";
				dialog = new ConfirmDialog(ProgressThreadDialog.getInstance(),
						"error_in_delete_entry" + (!errorMessage.isEmpty() ? "_with_cause" : ""),
						ConfirmDialog.YES_NO_OPTION, false, locationName, errorMessage);
				dialog.setVisible(true);
				if (dialog.getReturnOption() == ConfirmDialog.YES_OPTION) {
					actionPerformed(entry);
				} else {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.repository.RepositoryTree.error_during_deletion", e);
				}
			}
		}

		// We want to select our parent node afterwards
		// Firstly, select parent node (if possible)
		// Secondly, invoke later because a JTree will select another node as a result of a deletion
		// event
		final RepositoryLocation parentLocation;
		if (entry.getContainingFolder() != null) {
			parentLocation = entry.getContainingFolder().getLocation();
		} else {
			parentLocation = null;
		}
		if (parentLocation == null) {
			return;
		}
		SwingUtilities.invokeLater(() -> tree.expandAndSelectIfExists(parentLocation));
	}

}
