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
package com.rapidminer.gui.operatortree.actions;

import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JComponent;

import com.rapidminer.gui.tools.ResourceAction;


/**
 * 
 * @author Simon Fischer, Marco Boeck
 */
public class CutCopyPasteDeleteAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	private JComponent focusOwner = null;

	public static final String DELETE_ACTION_COMMAND_KEY = "delete";

	public static final Action CUT_ACTION = new CutCopyPasteDeleteAction("cut", "cut");
	public static final Action COPY_ACTION = new CutCopyPasteDeleteAction("copy", "copy");
	public static final Action PASTE_ACTION = new CutCopyPasteDeleteAction("paste", "paste");
	public static final Action DELETE_ACTION = new CutCopyPasteDeleteAction("delete", DELETE_ACTION_COMMAND_KEY);

	private CutCopyPasteDeleteAction(String i18nKey, String action) {
		super(i18nKey);
		putValue(ACTION_COMMAND_KEY, action);
		KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		manager.addPropertyChangeListener("permanentFocusOwner", evt -> {
			Object o = evt.getNewValue();
			if (o instanceof JComponent) {
				focusOwner = (JComponent) o;
			} else {
				focusOwner = null;
			}
		});
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		if (focusOwner == null) {
			return;
		}
		String action = e.getActionCommand();
		Action a = focusOwner.getActionMap().get(action);
		if (a != null) {
			a.actionPerformed(new ActionEvent(focusOwner, ActionEvent.ACTION_PERFORMED, e.getActionCommand()));
		}
	}
}
