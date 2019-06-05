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

import com.rapidminer.gui.ConditionalAction;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryTree;

import java.awt.event.ActionEvent;


/**
 * This action will show the current process in the repository tree
 * 
 * @author Marcin Skirzynski
 */
public class ShowProcessInRepositoryAction extends ResourceActionAdapter {

	private static final long serialVersionUID = -430582650605522700L;

	private final RepositoryTree tree;

	public ShowProcessInRepositoryAction(RepositoryTree tree) {
		super(true, "link");
		this.tree = tree;
		setCondition(PROCESS_HAS_REPOSITORY_LOCATION, ConditionalAction.MANDATORY);
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		if (RapidMinerGUI.getMainFrame().getProcess() != null) {
			RepositoryLocation repoLoc = RapidMinerGUI.getMainFrame().getProcess().getRepositoryLocation();
			if (repoLoc != null) {
				// scroll to location
				// twice because otherwise the repository browser selects the parent...
				tree.expandAndSelectIfExists(repoLoc);
				tree.expandAndSelectIfExists(repoLoc);
			}
		}
	}
}
