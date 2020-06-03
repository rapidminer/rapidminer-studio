/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.TimeUnit;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.DecisionRememberingConfirmDialog;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.repository.gui.RepositoryTree;
import com.rapidminer.repository.local.SimpleEntry;
import com.rapidminer.repository.local.SimpleFolder;
import com.rapidminer.repository.versioned.BasicDataEntry;
import com.rapidminer.repository.versioned.BasicFolder;
import com.rapidminer.repository.versioned.FilesystemRepositoryAdapter;
import com.rapidminer.tools.SystemInfoUtilities;


/**
 * This action tries to open the folder of the selected entry in the OS file browser.
 *
 * @author Marco Boeck
 */
public class OpenInFileBrowserAction extends AbstractRepositoryAction<Entry> {

	private static final long serialVersionUID = 1L;
	private static final int PROCESS_TIMEOUT = 10;


	public OpenInFileBrowserAction(RepositoryTree tree) {
		super(tree, Entry.class, false, "repository_open_in_filebrowser");
	}

	@Override
	public void actionPerformed(Entry entry) {
		if (entry == null || entry.getLocation() == null) {
			// should not happen
			return;
		}

		// try to open the entry
		if (entry instanceof BasicDataEntry) {
			BasicDataEntry<?> dataEntry = (BasicDataEntry<?>) entry;
			openFolderInFileBrowser(dataEntry.getRepositoryAdapter().getRealPath(dataEntry).getParent());
		} else if (entry instanceof BasicFolder) {
			BasicFolder folderEntry = (BasicFolder) entry;
			openFolderInFileBrowser(Paths.get(folderEntry.getRepositoryAdapter().getRoot().toAbsolutePath().toString(), folderEntry.getFsFolder().getPath()));
		} else if (entry instanceof FilesystemRepositoryAdapter) {
			FilesystemRepositoryAdapter repo = (FilesystemRepositoryAdapter) entry;
			openFolderInFileBrowser(repo.getRoot());
		} else if (entry instanceof SimpleEntry) {
			SimpleFolder folder = entry instanceof SimpleFolder ? (SimpleFolder) entry : (SimpleFolder) entry.getContainingFolder();
			openFolderInFileBrowser(Paths.get(folder.getFile().toString()));
		}
	}

	/**
	 * Opens the given folder file with the user-chosen file browser or the system default in an async fashion via a
	 * {@link ProgressThread}.
	 *
	 * @param path must be a directory and exist, otherwise nothing will happen
	 * @since 9.7
	 */
	public static void openFolderInFileBrowser(Path path) {
		if (!Files.isDirectory(path) || !Files.exists(path)) {
			return;
		}
		// check if user knows what he is doing
		if (!DecisionRememberingConfirmDialog.confirmAction("open_in_filebrowser",
				RapidMinerGUI.PROPERTY_OPEN_IN_FILEBROWSER)) {
			return;
		}
		String customFileBrowser = RepositoryTools.getCustomFileBrowser();
		if (customFileBrowser == null && (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN))) {
			SwingTools.showVerySimpleErrorMessage("open_in_filebrowser_unsupported_no_custom_browser");
			return;
		}

		new ProgressThread("open_in_filebrowser") {

			@Override
			public void run() {
				AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
					String customFileBrowser = RepositoryTools.getCustomFileBrowser();
					try {
						if (customFileBrowser == null) {
							try {
								Desktop.getDesktop().open(path.toFile());
							} catch (Exception e) {
								SwingTools.showSimpleErrorMessage("cannot_open_in_filebrowser", e);
							}
						} else {
							if (SystemInfoUtilities.getOperatingSystem() == SystemInfoUtilities.OperatingSystem.WINDOWS) {
								new ProcessBuilder(customFileBrowser, path.toAbsolutePath().toString()).inheritIO().start();
							} else if (SystemInfoUtilities.getOperatingSystem() == SystemInfoUtilities.OperatingSystem.OSX ||
									SystemInfoUtilities.getOperatingSystem() == SystemInfoUtilities.OperatingSystem.UNIX) {
								Process p = new ProcessBuilder("sh", "-c", customFileBrowser, path.toAbsolutePath().toString()).inheritIO().start();
								int exitCode = p.waitFor(PROCESS_TIMEOUT, TimeUnit.SECONDS) ? p.exitValue() : 0;
								switch (exitCode) {
									case 0:
										// all good
										// some programs don't return control and thus we don't know if it started or not.
										break;
									case 127:
										// unknown command/location
										SwingTools.showVerySimpleErrorMessage("cannot_open_in_filebrowser_127", exitCode);
										break;
									default:
										SwingTools.showVerySimpleErrorMessage("cannot_open_in_filebrowser_exitcode", exitCode);
								}
							}
						}
					} catch (Exception e) {
						SwingTools.showSimpleErrorMessage("cannot_open_in_filebrowser", e);
					}
					return null;
				});
			}
		}.start();
	}
}