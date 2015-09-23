/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.operatortree.actions;

import com.rapidminer.gui.operatortree.OperatorTree;
import com.rapidminer.gui.tools.IconSize;
import com.rapidminer.gui.tools.SwingTools;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa
 */
public class LockTreeStructureAction extends AbstractAction {

	private static final long serialVersionUID = 1L;

	private static final String LOCKED_ICON_NAME = "lock.png";
	private static final String UNLOCKED_ICON_NAME = "lock_open.png";

	private static final Icon[] LOCKED_ICONS = new Icon[IconSize.values().length];
	private static final Icon[] UNLOCKED_ICONS = new Icon[IconSize.values().length];

	static {
		int counter = 0;
		for (IconSize size : IconSize.values()) {
			LOCKED_ICONS[counter] = SwingTools.createIcon(size.getSize() + "/" + LOCKED_ICON_NAME);
			UNLOCKED_ICONS[counter] = SwingTools.createIcon(size.getSize() + "/" + UNLOCKED_ICON_NAME);
			counter++;
		}
	}

	private OperatorTree operatorTree;

	private IconSize iconSize;

	public LockTreeStructureAction(OperatorTree operatorTree, IconSize size) {
		super("Lock Tree Structure", UNLOCKED_ICONS[size.ordinal()]);
		putValue(SHORT_DESCRIPTION, "Locks or unlocks the tree structure for drag and drop.");
		putValue(MNEMONIC_KEY, Integer.valueOf(KeyEvent.VK_S));
		this.operatorTree = operatorTree;
		this.iconSize = size;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		this.operatorTree.setStructureLocked(!this.operatorTree.isStructureLocked());
	}

	public void updateIcon() {
		if (this.operatorTree.isStructureLocked()) {
			putValue(SMALL_ICON, LOCKED_ICONS[iconSize.ordinal()]);
			putValue(NAME, "Unlock Tree Structure");
		} else {
			putValue(SMALL_ICON, UNLOCKED_ICONS[iconSize.ordinal()]);
			putValue(NAME, "Lock Tree Structure");
		}
	}
}
