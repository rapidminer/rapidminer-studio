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
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryTree;

import javax.swing.SwingUtilities;


/**
 * This action creates a new folder.
 * 
 * @author Simon Fischer
 */
public class CreateFolderAction extends AbstractRepositoryAction<Folder> {

	private static final long serialVersionUID = 1L;

	public CreateFolderAction(RepositoryTree tree) {
		super(tree, Folder.class, true, "repository_create_folder");
	}

	@Override
	public void actionPerformed(final Folder folder) {
		ProgressThread openProgressThread = new ProgressThread("create_folder") {

			@Override
			public void run() {
				String name = SwingTools.showRepositoryEntryInputDialog("repository.new_folder", "");
				if (name != null) {
					try {
						folder.createFolder(name);
						final RepositoryLocation location = new RepositoryLocation(folder.getLocation(), name);
						SwingUtilities.invokeLater(() -> tree.expandAndSelectIfExists(location));
					} catch (RepositoryException e) {
						SwingTools.showSimpleErrorMessage("cannot_create_folder_with_reason", e, name, e.getMessage());
					} catch (Exception e) {
						SwingTools.showSimpleErrorMessage("cannot_create_folder", e, name);
					}
				}
			}
		};
		openProgressThread.start();
	}

}
