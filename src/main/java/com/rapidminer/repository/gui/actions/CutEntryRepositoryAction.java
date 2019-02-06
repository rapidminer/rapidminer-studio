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

import javax.swing.Action;

import com.rapidminer.repository.Entry;
import com.rapidminer.repository.gui.RepositoryTree;


/**
 * This action is the standard cut action.
 *
 * @author Adrian Wilke
 */
public class CutEntryRepositoryAction extends AbstractRepositoryAction<Entry> {

	private static final long serialVersionUID = 1L;

	/** Sets the i18n key and the action command key */
	public CutEntryRepositoryAction(RepositoryTree tree) {
		super(tree, Entry.class, true, "repository_cut");
		putValue(ACTION_COMMAND_KEY, "cut");
	}

	/** Fires action event */
	@Override
	public void loggedActionPerformed(ActionEvent e) {
		String actionCommand = e.getActionCommand();
		Action action = tree.getActionMap().get(actionCommand);
		if (action != null) {
			action.actionPerformed(new ActionEvent(tree, ActionEvent.ACTION_PERFORMED, actionCommand));
		}
	}

	/** Not in use. Use {@link #actionPerformed(Entry)} instead. */
	@Override
	public void actionPerformed(Entry cast) {
		// Like in CopyEntryRepositoryAction:
		// not needed because we override actionPerformed(ActionEvent e) which is the only caller
	}
}
