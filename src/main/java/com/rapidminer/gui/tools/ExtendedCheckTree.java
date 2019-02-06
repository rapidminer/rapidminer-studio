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
package com.rapidminer.gui.tools;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;


/**
 * The check tree, i.e. a JTree also displaying selection checkboxes with triple states and
 * automatic parent and child selections.
 * 
 * @author Ingo Mierswa
 */
public class ExtendedCheckTree extends JTree {

	private static final long serialVersionUID = -6788149857535452181L;

	private ExtendedCheckTreeMouseSelectionManager checkTreeManager;

	public ExtendedCheckTree(TreeModel model, boolean selectAll) {
		super(model);
		this.checkTreeManager = new ExtendedCheckTreeMouseSelectionManager(this, selectAll);
		setToggleClickCount(-1);
	}

	public void addCheckTreeSelectionListener(TreeSelectionListener l) {
		this.checkTreeManager.getSelectionModel().addTreeSelectionListener(l);
	}

	public TreePath[] getCheckedPaths() {
		return checkTreeManager.getSelectionModel().getSelectionPaths();
	}
}
