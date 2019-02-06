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

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;


/**
 * This is the selection model for the check tree.
 * 
 * @author Santhosh Kumar, Ingo Mierswa
 */
public class ExtendedCheckTreeSelectionModel extends DefaultTreeSelectionModel {

	private static final long serialVersionUID = -1617898630869822944L;

	private TreeModel model;

	public ExtendedCheckTreeSelectionModel(TreeModel model) {
		this.model = model;
		setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
	}

	// tests whether there is any unselected node in the subtree of given path
	public boolean isPartiallySelected(TreePath path) {
		if (isPathSelected(path, true)) {
			return false;
		}
		TreePath[] selectionPaths = getSelectionPaths();
		if (selectionPaths == null) {
			return false;
		}
		for (int j = 0; j < selectionPaths.length; j++) {
			if (isDescendant(selectionPaths[j], path)) {
				return true;
			}
		}
		return false;
	}

	// tells whether given path is selected.
	// if dig is true, then a path is assumed to be selected, if
	// one of its ancestor is selected.
	public boolean isPathSelected(TreePath path, boolean dig) {
		if (!dig) {
			return super.isPathSelected(path);
		}
		while (path != null && !super.isPathSelected(path)) {
			path = path.getParentPath();
		}
		return path != null;
	}

	// is path1 descendant of path2
	private boolean isDescendant(TreePath path1, TreePath path2) {
		Object obj1[] = path1.getPath();
		Object obj2[] = path2.getPath();
		if (obj1.length <= obj2.length) {
			return false;
		}
		for (int i = 0; i < obj2.length; i++) {
			if (obj1[i] != obj2[i]) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void setSelectionPaths(TreePath[] pPaths) {
		clearSelection();
		for (int i = 0; i < pPaths.length; i++) {
			addSelectionPath(pPaths[i]);
		}
	}

	@Override
	public void addSelectionPaths(TreePath[] paths) {
		// unselect all descendants of paths[]
		for (int i = 0; i < paths.length; i++) {
			TreePath path = paths[i];
			TreePath[] selectionPaths = getSelectionPaths();
			if (selectionPaths == null) {
				break;
			}
			List<TreePath> toBeRemoved = new ArrayList<TreePath>();
			for (int j = 0; j < selectionPaths.length; j++) {
				if (isDescendant(selectionPaths[j], path)) {
					toBeRemoved.add(selectionPaths[j]);
				}
			}
			super.removeSelectionPaths(toBeRemoved.toArray(new TreePath[0]));
		}

		// if all siblings are selected then unselect them and select parent recursively
		// otherwise just select that path.
		for (int i = 0; i < paths.length; i++) {
			TreePath path = paths[i];
			TreePath temp = null;
			while (areSiblingsSelected(path)) {
				temp = path;
				if (path.getParentPath() == null) {
					break;
				}
				path = path.getParentPath();
			}
			if (temp != null) {
				if (temp.getParentPath() != null) {
					addSelectionPath(temp.getParentPath());
				} else {
					if (!isSelectionEmpty()) {
						removeSelectionPaths(getSelectionPaths());
					}
					super.addSelectionPaths(new TreePath[] { temp });
				}
			} else {
				super.addSelectionPaths(new TreePath[] { path });
			}
		}
	}

	// tells whether all siblings of given path are selected.
	private boolean areSiblingsSelected(TreePath path) {
		TreePath parent = path.getParentPath();
		if (parent == null) {
			return true;
		}
		Object node = path.getLastPathComponent();
		Object parentNode = parent.getLastPathComponent();

		int childCount = model.getChildCount(parentNode);
		for (int i = 0; i < childCount; i++) {
			Object childNode = model.getChild(parentNode, i);
			if (childNode == node) {
				continue;
			}
			if (!isPathSelected(parent.pathByAddingChild(childNode))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void removeSelectionPaths(TreePath[] paths) {
		for (int i = 0; i < paths.length; i++) {
			TreePath path = paths[i];
			if (path.getPathCount() == 1) {
				super.removeSelectionPaths(new TreePath[] { path });
			} else {
				toggleRemoveSelection(path);
			}
		}
	}

	// if any ancestor node of given path is selected then unselect it
	// and selection all its descendants except given path and descendants.
	// otherwise just unselect the given path
	private void toggleRemoveSelection(TreePath path) {
		java.util.Stack<TreePath> stack = new Stack<TreePath>();
		TreePath parent = path.getParentPath();
		while (parent != null && !isPathSelected(parent)) {
			stack.push(parent);
			parent = parent.getParentPath();
		}
		if (parent != null) {
			stack.push(parent);
		} else {
			super.removeSelectionPaths(new TreePath[] { path });
			return;
		}

		while (!stack.isEmpty()) {
			TreePath temp = stack.pop();
			TreePath peekPath = stack.isEmpty() ? path : (TreePath) stack.peek();
			Object node = temp.getLastPathComponent();
			Object peekNode = peekPath.getLastPathComponent();
			int childCount = model.getChildCount(node);
			for (int i = 0; i < childCount; i++) {
				Object childNode = model.getChild(node, i);
				if (childNode != peekNode) {
					super.addSelectionPaths(new TreePath[] { temp.pathByAddingChild(childNode) });
				}
			}
		}
		super.removeSelectionPaths(new TreePath[] { parent });
	}
}
