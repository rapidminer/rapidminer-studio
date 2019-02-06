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

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;


/**
 * This is the combined renderer for the check tree.
 * 
 * @author Santhosh Kumar, Ingo Mierswa 16:58:56 ingomierswa Exp $
 */
public class ExtendedCheckTreeCellRenderer extends JPanel implements TreeCellRenderer {

	private static final long serialVersionUID = -2532264318689978630L;

	private ExtendedCheckTreeSelectionModel selectionModel;

	private TreeCellRenderer delegate;

	private ExtendedTriStateCheckBox checkBox = new ExtendedTriStateCheckBox();

	public ExtendedCheckTreeCellRenderer(TreeCellRenderer delegate, ExtendedCheckTreeSelectionModel selectionModel) {
		this.delegate = delegate;
		this.selectionModel = selectionModel;
		setLayout(new BorderLayout());
		setOpaque(false);
		checkBox.setOpaque(false);
	}

	@Override
	public java.awt.Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus) {
		Component renderer = delegate.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		((DefaultTreeCellRenderer) renderer).setLeafIcon(null);
		((DefaultTreeCellRenderer) renderer).setIcon(null);
		((DefaultTreeCellRenderer) renderer).setOpenIcon(null);
		((DefaultTreeCellRenderer) renderer).setClosedIcon(null);

		TreePath path = tree.getPathForRow(row);
		if (path != null) {
			if (selectionModel.isPathSelected(path, true)) {
				checkBox.setState(Boolean.TRUE);
			} else {
				checkBox.setState(selectionModel.isPartiallySelected(path) ? null : Boolean.FALSE);
			}
		}
		removeAll();
		add(checkBox, BorderLayout.WEST);
		add(renderer, BorderLayout.CENTER);
		return this;
	}
}
