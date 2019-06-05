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

import com.rapidminer.connection.gui.ConnectionCreationDialog;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.gui.RepositoryTree;


/**
 * Action to create a new {@link com.rapidminer.connection.ConnectionInformation Connection}.
 *
 * @author Marco Boeck
 * @since 9.3.0
 */
public class CreateConnectionAction extends AbstractRepositoryAction<Folder> {

	public CreateConnectionAction(RepositoryTree tree) {
		super(tree, Folder.class, true, "repository_create_connection");
	}


	@Override
	public void actionPerformed(Folder folder) {
		createConnection(folder);
	}

	/**
	 * Opens the connection creation dialog. If a folder is given and that repository does not support connections, will
	 * act as if no predefined location was passed at all.
	 *
	 * @param folder
	 * 		the optional predefined folder/repository where the new connection should be created. Can be {@code null}
	 */
	public static void createConnection(Folder folder) {
		Repository repo = null;
		try {
			repo = folder != null ? folder.getLocation().getRepository() : null;
			if (repo != null && !repo.supportsConnections()) {
				repo = null;
			}
		} catch (RepositoryException e) {
			// ignore, should not happen anyway
		}
		new ConnectionCreationDialog(ApplicationFrame.getApplicationFrame(), repo).setVisible(true);
	}
}
