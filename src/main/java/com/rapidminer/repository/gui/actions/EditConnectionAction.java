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

import java.io.IOException;

import javax.swing.SwingUtilities;

import com.rapidminer.connection.gui.ConnectionEditDialog;
import com.rapidminer.connection.gui.actions.SaveConnectionAction;
import com.rapidminer.connection.gui.dto.ConnectionInformationHolder;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ProgressThreadDialog;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.gui.RepositoryTree;
import com.rapidminer.repository.internal.remote.RemoteConnectionEntry;
import com.rapidminer.tools.PasswordInputCanceledException;


/**
 * Edit Connection Action
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3.0
 */
public class EditConnectionAction extends AbstractRepositoryAction<ConnectionEntry> {

	private static final String PROGRESS_THREAD_KEY = "download_connection_from_repository";

	public EditConnectionAction(RepositoryTree tree) {
		super(tree, ConnectionEntry.class, true, "repository_edit_connection");
	}

	@Override
	public void actionPerformed(ConnectionEntry entry) {
		editConnection(entry, true);
	}

	/**
	 * Opens the connection edit dialog. Note that the retrieval happens async, so this may take a while before the
	 * dialog will appear. A {@link com.rapidminer.gui.tools.ProgressThread} will be visible while the retrieval is in
	 * progress.
	 *
	 * @param connection
	 * 		the connection entry
	 * @param openInEditMode
	 *        {@code true} if the connection should be opened in edit mode
	 */
	public static void editConnection(ConnectionEntry connection, boolean openInEditMode) {
		if (containsId(ProgressThread.getQueuedThreads(), PROGRESS_THREAD_KEY) ||
				containsId(ProgressThread.getCurrentThreads(), PROGRESS_THREAD_KEY)) {
			// Must happen after the frequent dispose calls in ProgressThreadDialog#updateUI
			SwingUtilities.invokeLater(() -> ProgressThreadDialog.getInstance().setVisible(true));
			return;
		}

		final ProgressThread downloadProgressThread = new ProgressThread(PROGRESS_THREAD_KEY, false, connection.getLocation().toString()) {

			@Override
			public void run() {
				try {
					ConnectionInformationHolder ci = ConnectionInformationHolder.from(connection);
					SwingTools.invokeLater(() -> new ConnectionEditDialog(ci, openInEditMode).setVisible(true));
				} catch (RepositoryException e) {
					if (connection instanceof RemoteConnectionEntry) {
						SwingTools.showSimpleErrorMessage("error_contacting_repository", e, e.getMessage());
					} else {
						SwingTools.showSimpleErrorMessage("connection_read_error", e, e.getMessage());
					}
				} catch (IOException e) {
					SwingTools.showSimpleErrorMessage("connection_read_error", e, e.getMessage());
				} catch (PasswordInputCanceledException e) {
					SwingTools.showSimpleErrorMessage("error_access_rights", e, e.getMessage());
				}
			}

			@Override
			public String getID() {
				return super.getID() + connection.getLocation();
			}
		};
		downloadProgressThread.setStartDialogShowTimer(true);
		downloadProgressThread.setShowDialogTimerDelay(1500);
		downloadProgressThread.setIndeterminate(true);
		downloadProgressThread.addDependency(downloadProgressThread.getID());
		String saveConnectionID = SaveConnectionAction.PROGRESS_THREAD_ID_PREFIX + connection.getLocation();
		downloadProgressThread.addDependency(saveConnectionID);
		downloadProgressThread.start();
		// Show progress thread dialog to indicate that editing is blocked by saving
		if (containsId(ProgressThread.getQueuedThreads(), saveConnectionID) ||
				containsId(ProgressThread.getCurrentThreads(), saveConnectionID)) {
			// Must happen after the frequent dispose calls in ProgressThreadDialog#updateUI
			SwingUtilities.invokeLater(() -> ProgressThreadDialog.getInstance().setVisible(true));
		}
	}

	/**
	 * Checks if the given list contains a progress thread with the id
	 *
	 * @param progressThreads
	 * 		a list of progress threads
	 * @param id the {@link ProgressThread#getID()}
	 * @return {@code true} if the list contains a progress thread with the id {@value #PROGRESS_THREAD_KEY}
	 */
	private static boolean containsId(Iterable<ProgressThread> progressThreads, String id) {
		for (ProgressThread pt : progressThreads) {
			if (id.equals(pt.getID())) {
				return true;
			}
		}
		return false;
	}
}
