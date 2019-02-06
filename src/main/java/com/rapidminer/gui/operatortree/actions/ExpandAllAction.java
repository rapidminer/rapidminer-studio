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

import com.rapidminer.gui.LoggedAbstractAction;
import com.rapidminer.gui.operatortree.OperatorTree;
import com.rapidminer.gui.tools.IconSize;
import com.rapidminer.gui.tools.SwingTools;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Icon;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa
 */
public class ExpandAllAction extends LoggedAbstractAction {

	private static final long serialVersionUID = -1779888083256884608L;

	private static final String ICON_NAME = "zoom_in.png";

	private static final Icon[] ICONS = new Icon[IconSize.values().length];

	static {
		int counter = 0;
		for (IconSize size : IconSize.values()) {
			ICONS[counter++] = SwingTools.createIcon(size.getSize() + "/" + ICON_NAME);
		}
	}

	private OperatorTree operatorTree;

	public ExpandAllAction(OperatorTree operatorTree, IconSize size) {
		super("Expand Tree", ICONS[size.ordinal()]);
		putValue(SHORT_DESCRIPTION, "Expands the complete operator tree");
		putValue(MNEMONIC_KEY, Integer.valueOf(KeyEvent.VK_X));
		this.operatorTree = operatorTree;
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		this.operatorTree.expandAll();
	}
}
