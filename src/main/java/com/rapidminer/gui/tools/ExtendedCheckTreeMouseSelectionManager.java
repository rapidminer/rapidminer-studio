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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;


/**
 * This is a mouse listener for check trees in order to update the selection events.
 * 
 * @author Santhosh Kumar, Ingo Mierswa
 */
public class ExtendedCheckTreeMouseSelectionManager extends MouseAdapter implements TreeSelectionListener {

	private ExtendedCheckTreeSelectionModel selectionModel;

	private JTree tree = new JTree();

	int hotspot = new JCheckBox().getPreferredSize().width;

	public ExtendedCheckTreeMouseSelectionManager(JTree tree, boolean selectAll) {
		this.tree = tree;
		selectionModel = new ExtendedCheckTreeSelectionModel(tree.getModel());

		if (selectAll) {
			selectionModel.addSelectionPath(tree.getPathForRow(0));
		}

		tree.setCellRenderer(new ExtendedCheckTreeCellRenderer(new DefaultTreeCellRenderer(), selectionModel));
		tree.addMouseListener(this);
		selectionModel.addTreeSelectionListener(this);
	}

	@Override
	public void mouseClicked(MouseEvent me) {
		TreePath path = tree.getPathForLocation(me.getX(), me.getY());
		if (path == null) {
			return;
		}
		if (me.getX() > tree.getPathBounds(path).x + hotspot) {
			return;
		}

		boolean selected = selectionModel.isPathSelected(path, true);
		selectionModel.removeTreeSelectionListener(this);

		try {
			if (selected) {
				selectionModel.removeSelectionPath(path);
			} else {
				selectionModel.addSelectionPath(path);
			}
		} finally {
			selectionModel.addTreeSelectionListener(this);
			tree.treeDidChange();
		}
	}

	public ExtendedCheckTreeSelectionModel getSelectionModel() {
		return selectionModel;
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		tree.treeDidChange();
	}
}
