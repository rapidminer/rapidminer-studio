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

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.rapidminer.gui.operatortree.actions.CutCopyPasteDeleteAction;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.repository.gui.RepositoryTree;


/**
 * Abstract superclass of actions that are executed on subclasses of {@link Entry}. Automatically
 * enables/disables itself.
 *
 * @author Simon Fischer, Adrian Wilke
 */
public abstract class AbstractRepositoryAction<T extends Entry> extends ResourceAction {

	/** the tree to which the action belongs to */
	protected final transient RepositoryTree tree;

	/** the required selection type for the action to show/enable */
	private final Class<T> requiredSelectionType;

	/** if write access to the repository is needed for this action */
	private final boolean needsWriteAccess;

	private static final long serialVersionUID = -6415235351430454776L;

	public AbstractRepositoryAction(RepositoryTree tree, Class<T> requiredSelectionType, boolean needsWriteAccess,
			String i18nKey) {
		super(true, i18nKey);
		this.tree = tree;
		this.requiredSelectionType = requiredSelectionType;
		this.needsWriteAccess = needsWriteAccess;
		setEnabled(false);
	}

	@Override
	protected void update(boolean[] conditions) {
		// we have our own mechanism to enable/disable actions,
		// so ignore ConditionalAction mechanism
	}

	/**
	 * Enables action, if every entry exists and can be written if needed.
	 */
	public void enable() {

		// Do not treat entries, whose are already included in selected folders
		List<Entry> entries = removeIntersectedEntries(tree.getSelectedEntries());

		boolean enable = true;
		for (Entry entry : entries) {
			if (entry == null) {
				enable = false;
				break;
			}
			if (!requiredSelectionType.isInstance(entry)) {
				enable = false;
				break;
			}
			if (needsWriteAccess && entry.isReadOnly()) {
				enable = false;
				break;
			}
		}
		if (entries.isEmpty()) {
			enable = false;
		}
		setEnabled(enable);
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {

		List<Entry> entries = tree.getSelectedEntries();

		// Deletion of elements
		if (e.getActionCommand().equals(DeleteRepositoryEntryAction.I18N_KEY)
				|| e.getActionCommand().equals(CutCopyPasteDeleteAction.DELETE_ACTION_COMMAND_KEY)) {
			if (entries.size() == 1) {
				try {
					Folder connectionFolder = RepositoryTools.getConnectionFolder(entries.get(0).getLocation().getRepository());
					if (connectionFolder != null && connectionFolder.getLocation().getAbsoluteLocation().equals(entries.get(0).getLocation().getAbsoluteLocation())) {
						return;
					}
				} catch (RepositoryException e1) {
					// ignore, should not happen anyway and is irrelevant here. Worst case, you get the delete dialog if you should not, but backend blocks delete anyway
				}
				if (SwingTools.showConfirmDialog("file_chooser.delete", ConfirmDialog.YES_NO_OPTION,
						entries.get(0).getName()) != ConfirmDialog.YES_OPTION) {
					return;
				}
			} else {
				if (SwingTools.showConfirmDialog("file_chooser.delete_multiple", ConfirmDialog.YES_NO_OPTION,
						entries.size()) != ConfirmDialog.YES_OPTION) {
					return;
				}
			}

			final List<Entry> remainingEntries = removeIntersectedEntries(tree.getSelectedEntries());
			ProgressThread progressThread = new ProgressThread(DeleteRepositoryEntryAction.I18N_KEY) {

				/** Total progress of progress listener bar */
				private int progressListenerCompleted = 0;

				/** Step size for single entry operation */
				private final int PROGRESS_LISTENER_SINGLE_STEP_SIZE = 1;

				@Override
				public void run() {

					// Initialize progress listener
					getProgressListener().setTotal(remainingEntries.size() * PROGRESS_LISTENER_SINGLE_STEP_SIZE);
					getProgressListener().setCompleted(progressListenerCompleted);
					for (Entry entry : remainingEntries) {
						actionPerformed(requiredSelectionType.cast(entry));
						progressListenerCompleted += PROGRESS_LISTENER_SINGLE_STEP_SIZE;
						getProgressListener().setCompleted(progressListenerCompleted);
					}
					getProgressListener().complete();
				}
			};

			progressThread.setShowDialogTimerDelay(200);
			progressThread.setStartDialogShowTimer(true);
			progressThread.start();

		} else {
			// Do not treat entries, whose are already included in selected folders
			entries = removeIntersectedEntries(tree.getSelectedEntries());
			for (Entry entry : entries) {
				actionPerformed(requiredSelectionType.cast(entry));
			}
		}
	}

	public abstract void actionPerformed(T cast);

	/**
	 * Removes entries from list, which are already included in parent entries
	 *
	 * Example: [/1/2/3, /1, /1/2] becomes [/1]
	 */
	protected List<Entry> removeIntersectedEntries(List<Entry> entries) {

		// Get locations of entries
		Map<RepositoryLocation, Entry> locations = entries.stream().collect(Collectors.toMap(Entry::getLocation, Function.identity()));

		// Remove intersected locations
		List<RepositoryLocation> filteredLocations = RepositoryLocation.removeIntersectedLocations(locations.keySet());

		// return entries
		return filteredLocations.stream().map(locations::get).collect(Collectors.toList());
	}
}
