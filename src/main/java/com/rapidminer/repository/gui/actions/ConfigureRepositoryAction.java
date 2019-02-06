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

import com.rapidminer.repository.Repository;
import com.rapidminer.repository.gui.RepositoryConfigurationDialog;
import com.rapidminer.repository.gui.RepositoryTree;


/**
 * This action configures the selected repository.
 *
 * @author Simon Fischer
 *
 */
public class ConfigureRepositoryAction extends AbstractRepositoryAction<Repository> {

	private static final long serialVersionUID = 1L;

	public ConfigureRepositoryAction(RepositoryTree tree) {
		super(tree, Repository.class, false, "configure_repository");
	}

	@Override
	public void actionPerformed(Repository repository) {
		new RepositoryConfigurationDialog(repository).setVisible(true);
	}

}
