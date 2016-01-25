/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import java.util.HashSet;

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

	/** Saved expanded repositories */
	private HashSet<String> expandedRepositories;

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

		saveSelectionPaths(tree.getSelectionPaths());

		expandedNodes = new HashSet<>();
		expandedRepositories = new HashSet<>();

		for (int i = 0; i < tree.getRowCount(); i++) {
			TreePath path = tree.getPathForRow(i);
			if (tree.isExpanded(path)) {
				Entry entry = (Entry) path.getLastPathComponent();
				String absoluteLocation = entry.getLocation().getAbsoluteLocation();
				if (entry instanceof Repository) {
					expandedRepositories.add(absoluteLocation);
				} else {
					expandedNodes.add(absoluteLocation);
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
		for (int i = 0; i < tree.getRowCount(); i++) {
			TreePath path = tree.getPathForRow(i);
			Object entryObject = path.getLastPathComponent();
			if (entryObject instanceof Entry) {
				Entry entry = (Entry) entryObject;
				String absoluteLocation = entry.getLocation().getAbsoluteLocation();
				if (expandedRepositories.contains(absoluteLocation) || expandedNodes.contains(absoluteLocation)) {
					tree.expandPath(path);
				}
			}
		}

		restoreSelectionPaths(tree);
	}

	/**
	 * Calls {@link RepositoryLocation###locateEntry()} on every node which was saved with ##
	 * {@link #saveExpansionState(JTree)}. This calls {@link RepositoryManager###locate(Repository,
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

}
