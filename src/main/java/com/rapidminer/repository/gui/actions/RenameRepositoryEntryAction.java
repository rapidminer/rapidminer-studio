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

import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.gui.RepositoryTree;


/**
 * This action renames the selected entry.
 * 
 * @author Simon Fischer
 */
public class RenameRepositoryEntryAction extends AbstractRepositoryAction<Entry> {

	private static final long serialVersionUID = 1L;

	public RenameRepositoryEntryAction(RepositoryTree tree) {
		super(tree, Entry.class, true, "repository_rename_entry");
	}

	@Override
	public void actionPerformed(Entry entry) {
		// no renaming of repositories allowed, RepositoryConfigurationDialog is responsible for that
		if (entry instanceof Repository) {
			return;
		}
		if (entry instanceof Folder) {
			Folder f = (Folder) entry;
			// if this is the connections folder AND it is named properly as "Connections"
			if (f.isSpecialConnectionsFolder() && Folder.isConnectionsFolderName(f.getName(), true)) {
				return;
			}
		}

		String name = SwingTools.showRepositoryEntryInputDialog("file_chooser.rename", entry.getName(), entry.getName());
		if ((name != null) && !name.equals(entry.getName())) {
			// don't rename in EDT, RemoteRepository could block entire UI
			new ProgressThread("repository_rename") {
				@Override
				public void run() {
					boolean success;
					try {
						success = entry.rename(name);
					} catch (Exception e) {
						SwingTools.showSimpleErrorMessage("cannot_rename_entry", e, entry.getName(), name, e.getMessage());
						return;
					}
					if (!success) {
						SwingTools.showVerySimpleErrorMessage("cannot_rename_entry", entry.getName(), name);
					}
				}
			}.start();
		}
	}

}
