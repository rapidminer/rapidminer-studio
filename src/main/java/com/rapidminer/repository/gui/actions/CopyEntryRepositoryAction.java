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

import com.rapidminer.repository.Entry;
import com.rapidminer.repository.gui.RepositoryTree;

import java.awt.event.ActionEvent;

import javax.swing.Action;


/**
 * This action is the standard copy action.
 * 
 * @author Simon Fischer
 */
public class CopyEntryRepositoryAction extends AbstractRepositoryAction<Entry> {

	private static final long serialVersionUID = 1L;

	public CopyEntryRepositoryAction(RepositoryTree tree) {
		super(tree, Entry.class, false, "repository_copy");
		putValue(ACTION_COMMAND_KEY, "copy");
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		String action = e.getActionCommand();
		Action a = tree.getActionMap().get(action);
		if (a != null) {
			a.actionPerformed(new ActionEvent(tree, ActionEvent.ACTION_PERFORMED, null));
		}
	}

	@Override
	public void actionPerformed(Entry cast) {
		// not needed because we override actionPerformed(ActionEvent e) which is the only caller
	}

}
