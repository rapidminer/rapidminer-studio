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

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.DecisionRememberingConfirmDialog;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.gui.RepositoryTree;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.repository.local.SimpleFolder;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;


/**
 * This action tries to open the folder of the selected entry in the OS file browser.
 * 
 * @author Marco Boeck
 */
public class OpenInFileBrowserAction extends AbstractRepositoryAction<Entry> {

	private static final long serialVersionUID = 1L;

	public OpenInFileBrowserAction(RepositoryTree tree) {
		super(tree, Entry.class, false, "repository_open_in_filebrowser");
	}

	@Override
	public void actionPerformed(Entry entry) {
		if (entry == null || entry.getLocation() == null) {
			// should not happen
			return;
		}
		if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
			return;
		}

		// check if user knows what he is doing
		if (!DecisionRememberingConfirmDialog.confirmAction("open_in_filebrowser",
				RapidMinerGUI.PROPERTY_OPEN_IN_FILEBROWSER)) {
			return;
		}

		try {
			if (entry.getLocation().getRepository() instanceof LocalRepository) {
				File repoRoot = ((LocalRepository) entry.getLocation().getRepository()).getRoot();
				if (repoRoot != null) {
					StringBuilder pathBuilder = new StringBuilder();
					LinkedList<String> listOfFolders = new LinkedList<>();
					Folder folder = entry instanceof SimpleFolder ? (SimpleFolder) entry : entry.getContainingFolder();
					pathBuilder.append(repoRoot.getAbsolutePath());

					// collect all parent folders until we reach the LocalRepository
					while (folder != null && !(folder instanceof LocalRepository)) {
						listOfFolders.add(folder.getName());
						folder = folder.getContainingFolder();
					}

					// iterate backwards over the folders so we can create the real path
					for (int i = listOfFolders.size() - 1; i >= 0; i--) {
						pathBuilder.append(File.separatorChar);
						pathBuilder.append(listOfFolders.get(i));
					}

					// try to open it if it exists and is a directory
					File file = new File(pathBuilder.toString());
					if (file.isDirectory() && file.exists()) {
						Desktop.getDesktop().open(file);
					}
				}
			}
		} catch (IOException e) {
			// will appear on newer linux versions as the Desktop.open() call on a folder does not
			// work with current java versions
			SwingTools.showSimpleErrorMessage("cannot_open_in_filebrowser_io", "", entry.getLocation());
		} catch (Exception e) {
			SwingTools.showSimpleErrorMessage("cannot_open_in_filebrowser", e, entry.getLocation());
		}
	}

}
