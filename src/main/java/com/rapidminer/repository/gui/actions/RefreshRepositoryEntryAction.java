/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
import com.rapidminer.repository.gui.RepositoryTree;


/**
 * This action refreshes the selected entry.
 *
 * @author Simon Fischer, Marco Boeck
 */
public class RefreshRepositoryEntryAction extends AbstractRepositoryAction<Entry> {

	private static final long serialVersionUID = 1L;

	public RefreshRepositoryEntryAction(RepositoryTree tree) {
		super(tree, Entry.class, false, "repository_refresh_folder");
	}

	@Override
	public void actionPerformed(final Entry entry) {
		ProgressThread openProgressThread = new ProgressThread("refreshing") {

			@Override
			public void run() {
				getProgressListener().setTotal(100);
				getProgressListener().setCompleted(0);
				try {
					getProgressListener().setCompleted(50);

					Folder folder = null;
					if (entry instanceof Folder) {
						folder = (Folder) entry;
					} else {
						folder = entry.getContainingFolder();
					}
					if (folder != null) {
						folder.refresh();
					}
				} catch (Exception e) {
					SwingTools.showSimpleErrorMessage("cannot_refresh_folder", e);
				} finally {
					getProgressListener().setCompleted(100);
				}
			}
		};
		openProgressThread.start();
	}

}
