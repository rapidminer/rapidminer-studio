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
package com.rapidminer.repository.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import com.rapidminer.repository.Entry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.LogService;


/**
 * A utility class to save and restore expansion states and selection paths of the repository tree.
 *
 * @author Nils Woehler, Venkatesh Umaashankar, Adrian Wilke
 *
 */
public class RepositoryTreeUtil {

	/** Saved selected paths */
	private TreePath[] selectedPaths;

	/** Saved expanded nodes */
	private HashSet<String> expandedNodes;

	/** Saved selected nodes */
	private HashSet<String> selectedNodes;

	/** Saves single path */
	public void saveSelectionPath(TreePath treePath) {
		TreePath[] treePaths = new TreePath[1];
		treePaths[0] = treePath;
		selectedPaths = treePaths;
	}

	/** Saves multiple paths */
	public void saveSelectionPaths(TreePath[] treePaths) {
		selectedPaths = treePaths;
	}

	/**
	 * Sets selected paths to the previous path(s) saved by {@link #saveSelectionPath(TreePath)} or
	 * ##{@link #saveSelectionPaths(TreePath[])}. Scrolls to the first selected path.
	 *
	 * @param tree
	 *            The related tree, containing the path(s)
	 */
	public void restoreSelectionPaths(JTree tree) {
		if (selectedPaths != null) {
			tree.setSelectionPaths(selectedPaths);
			tree.scrollPathToVisible(tree.getSelectionPath());
		}
	}

	/**
	 * Saves the currently selected paths and saves all expanded repositories and nodes.
	 *
	 * @param tree
	 *            The related tree, containing the path(s)
	 */
	public void saveExpansionState(JTree tree) {


		expandedNodes = new LinkedHashSet<>();
		selectedNodes = new LinkedHashSet<>();

		for (int i = 0; i < tree.getRowCount(); i++) {
			TreePath path = tree.getPathForRow(i);
			boolean isExpanded = tree.isExpanded(path);
			boolean isSelected = tree.isPathSelected(path);
			Object entryObject = path.getLastPathComponent();
			if ((isExpanded || isSelected) && entryObject instanceof Entry) {
				Entry entry = (Entry) entryObject;
				String absoluteLocation = entry.getLocation().getAbsoluteLocation();
				if (isExpanded) {
					expandedNodes.add(absoluteLocation);
				}
				if (isSelected) {
					selectedNodes.add(absoluteLocation);
				}
			}
		}

	}

	/**
	 * Expands all repositories and nodes, which have been saved before. Restores selected paths,
	 * which have been saved proviously.
	 *
	 * @param tree
	 *            The related tree, containing the path(s)
	 */
	public void restoreExpansionState(JTree tree) {
		List<TreePath> selectedPathList = new ArrayList<>();
		for (int i = 0; i < tree.getRowCount(); i++) {
			TreePath path = tree.getPathForRow(i);
			// sanity check for concurrent refreshes
			if (path != null) {
				Object entryObject = path.getLastPathComponent();
				if (entryObject instanceof Entry) {
					Entry entry = (Entry) entryObject;
					String absoluteLocation = entry.getLocation().getAbsoluteLocation();
					if (expandedNodes.contains(absoluteLocation)) {
						tree.expandPath(path);
					}
					if (selectedNodes.contains(absoluteLocation)) {
						selectedPathList.add(path);
					}
				}
			}
		}
		if (!selectedPathList.isEmpty()) {
			tree.setSelectionPaths(selectedPathList.toArray(new TreePath[0]));
		}
	}

	/**
	 * Calls {@link RepositoryLocation###locateEntry()} on every node which was saved with ##
	 * {@link #saveExpansionState(JTree)}. This calls {@link com.rapidminer.repository.RepositoryManager#locate(Repository,
	 * String, boolean)} which refreshes parent folders of missed entries.
	 */
	public void locateExpandedEntries() {
		for (String absoluteLocation : expandedNodes) {
			try {
				RepositoryLocation repositoryLocation = new RepositoryLocation(absoluteLocation);
				repositoryLocation.locateEntry();
			} catch (MalformedRepositoryLocationException | RepositoryException e) {
				LogService.getRoot().warning(
						"com.rapidminer.repository.RepositoryTreeUtil.error_expansion" + absoluteLocation);
			}
		}
	}

	/**
	 * Retain only the root selectedPaths
	 *
	 * @param tree
	 *           The related tree, containing the path(s)
	 */
	public void retainRootSelections(JTree tree) {
		if (selectedPaths == null) {
			selectedPaths = tree.getSelectionPaths();
		}
		if (selectedPaths != null) {
			List<TreePath> parents = Arrays.asList(selectedPaths);
			List<TreePath> newSelection = new ArrayList<>(parents);
			for (TreePath entry : parents) {
				for (TreePath parent = entry.getParentPath(); parent != null; parent = parent.getParentPath()) {
					if (parents.contains(parent)) {
						newSelection.remove(entry);
						break;
					}
				}
			}
			selectedPaths = newSelection.toArray(new TreePath[0]);
		}
	}
}
