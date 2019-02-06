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
package com.rapidminer.gui.processeditor;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.tools.CamelCaseFilter;
import com.rapidminer.gui.tools.CamelCaseTypoFilter;
import com.rapidminer.gui.tools.OperatorFilter;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.tools.GroupTree;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.OperatorService.OperatorServiceListener;
import com.rapidminer.tools.documentation.OperatorDocBundle;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * This is the model for the group selection tree in the new operator editor panel.
 *
 * @author Ingo Mierswa, Tobias Malbrecht, Sebastian Land
 */
public class NewOperatorGroupTreeModel implements TreeModel, OperatorServiceListener {

	/** Regular expression used as delimiter for search terms. */
	private static final String SEARCH_TERM_DELIMITER = "\\s+";

	/** Consider only the first {@value} search terms. */
	private static final int MAX_SEARCH_TERMS = 5;

	/** Compares operator descriptions based on their usage statistics. */
	private static final class UsageStatsComparator implements Comparator<OperatorDescription>, Serializable {

		private static final long serialVersionUID = 1L;

		@Override
		public int compare(OperatorDescription op1, OperatorDescription op2) {
			int usageCount1 = (int) ActionStatisticsCollector.getInstance().getData(ActionStatisticsCollector.TYPE_OPERATOR,
					op1.getKey(), ActionStatisticsCollector.OPERATOR_EVENT_EXECUTION);
			int usageCount2 = (int) ActionStatisticsCollector.getInstance().getData(ActionStatisticsCollector.TYPE_OPERATOR,
					op2.getKey(), ActionStatisticsCollector.OPERATOR_EVENT_EXECUTION);
			return usageCount2 - usageCount1;
		}
	}

	private static final class OperatorPriorityComparator implements Comparator<OperatorDescription> {

		@Override
		public int compare(OperatorDescription o1, OperatorDescription o2) {
			try {
				return Math.negateExact(Integer.compare(o1.getPriority(), o2.getPriority()));
			} catch (ArithmeticException e) {
				// no logging for idiotic error
				return 0;
			}
		}

	}

	private final GroupTree completeTree;

	private GroupTree displayedTree;

	private boolean filterDeprecated = true;

	private String filter = null;

	/** The list of all tree model listeners. */
	private final List<TreeModelListener> treeModelListeners = new LinkedList<>();

	private boolean sortByUsage = false;

	/** sorts by <priority> key of operators */
	private Comparator<OperatorDescription> priorityComparator = new OperatorPriorityComparator();
	/** sorts by local usage count of operators */
	private Comparator<OperatorDescription> usageComparator = new UsageStatsComparator();

	public NewOperatorGroupTreeModel() {
		this.completeTree = OperatorService.getGroups();
		OperatorService.addOperatorServiceListener(this);

		removeHidden(completeTree);

		this.displayedTree = this.completeTree;
		this.filterDeprecated = true;
		updateTree();
	}

	public void setFilterDeprecated(boolean filterDeprecated) {
		this.filterDeprecated = filterDeprecated;
		updateTree();
	}

	@Override
	public void addTreeModelListener(TreeModelListener l) {
		treeModelListeners.add(l);
	}

	public boolean contains(Object o) {
		return contains(this.getRoot(), o);
	}

	private boolean contains(Object start, Object o) {
		if (o.equals(start)) {
			return true;
		} else {
			for (int i = 0; i < getChildCount(start); i++) {
				if (contains(getChild(start, i), o)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public Object getChild(Object parent, int index) {
		if (parent instanceof GroupTree) {
			GroupTree tree = (GroupTree) parent;
			int numSubGroups = tree.getSubGroups().size();
			if (index < numSubGroups) {
				return tree.getSubGroup(index);
			} else {
				return tree.getOperatorDescriptions().get(index - numSubGroups);
			}
		} else {
			return null;
		}

	}

	@Override
	public int getChildCount(Object parent) {
		if (parent instanceof GroupTree) {
			GroupTree tree = (GroupTree) parent;
			return tree.getSubGroups().size() + tree.getOperatorDescriptions().size();
		} else {
			return 0;
		}
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		GroupTree tree = (GroupTree) parent;
		if (child instanceof GroupTree) {
			return tree.getIndexOfSubGroup((GroupTree) child);
		} else {
			return tree.getOperatorDescriptions().indexOf(child) + tree.getSubGroups().size();
		}
	}

	@Override
	public Object getRoot() {
		return displayedTree;
	}

	@Override
	public boolean isLeaf(Object node) {
		return !(node instanceof GroupTree);
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		treeModelListeners.remove(l);
	}

	/** Will be invoked after editing changes of nodes. */
	@Override
	public void valueForPathChanged(TreePath path, Object node) {
		fireTreeChanged(node, path);
	}

	private void fireTreeChanged(Object source, TreePath path) {
		Iterator<TreeModelListener> i = treeModelListeners.iterator();
		while (i.hasNext()) {
			i.next().treeStructureChanged(new TreeModelEvent(source, path));
		}
	}

	private void fireCompleteTreeChanged(Object source) {
		Iterator<TreeModelListener> i = treeModelListeners.iterator();
		while (i.hasNext()) {
			i.next().treeStructureChanged(new TreeModelEvent(this, new TreePath(getRoot())));
		}
	}

	public List<TreePath> applyFilter(String filter) {
		this.filter = filter;
		return updateTree();
	}

	private GroupTree restoreTree(){
		GroupTree filteredTree = this.completeTree.clone();
		if (!"true".equals(System.getProperty(RapidMiner.PROPERTY_DEVELOPER_MODE))) {
			removeDeprecatedGroup(filteredTree);
		}
		return filteredTree;
	}

	public List<TreePath> updateTree() {
		GroupTree filteredTree = restoreTree();
		List<TreePath> expandedPaths = new LinkedList<>();
		if (filter != null && filter.trim().length() > 0) {
			int hits = 0;
			String[] terms = filter.trim().split(SEARCH_TERM_DELIMITER, MAX_SEARCH_TERMS);
			if (terms.length > 1) {
				Arrays.setAll(terms, i -> terms[i].toLowerCase());
				hits = removeFilteredInstances(terms, filteredTree, expandedPaths, new TreePath(getRoot()));
			}

			// If we couldn't find anything, restore the tree and try the CamelCase search
			if (hits == 0) {
				filteredTree = restoreTree();
				OperatorFilter ccFilter = new CamelCaseFilter(filter);
				hits = removeFilteredInstances(ccFilter, filteredTree, expandedPaths, new TreePath(getRoot()));
			}

			// If we still couldn't find anything, restore the tree and try the typo search
			if (hits == 0) {
				filteredTree = restoreTree();
				OperatorFilter cctFilter = new CamelCaseTypoFilter(filter);
				removeFilteredInstances(cctFilter, filteredTree, expandedPaths, new TreePath(getRoot()));
			}
		}

		if (filterDeprecated) {
			removeDeprecated(filteredTree);
		}
		this.displayedTree = filteredTree;
		filteredTree.sort(priorityComparator);

		if (sortByUsage) {
			filteredTree.sort(usageComparator);
		}

		fireCompleteTreeChanged(this);
		return expandedPaths;
	}

	public GroupTree getNonDeprecatedGroupTree(GroupTree tree) {
		GroupTree filteredTree = tree.clone();
		removeDeprecated(filteredTree);
		return filteredTree;
	}

	private void removeHidden(GroupTree tree) {
		Iterator<? extends GroupTree> g = tree.getSubGroups().iterator();
		while (g.hasNext()) {
			GroupTree child = g.next();
			removeHidden(child);
			if (child.getAllOperatorDescriptions().size() == 0) {
				g.remove();
			}
		}
		Iterator<OperatorDescription> o = tree.getOperatorDescriptions().iterator();
		while (o.hasNext()) {
			OperatorDescription description = o.next();
			if (description.getOperatorClass().equals(ProcessRootOperator.class)) {
				o.remove();
			}
		}
	}

	private void removeDeprecatedGroup(GroupTree tree) {
		Iterator<? extends GroupTree> g = tree.getSubGroups().iterator();
		while (g.hasNext()) {
			GroupTree child = g.next();
			if (child.getKey().equals("deprecated")) {
				g.remove();
			} else {
				removeDeprecatedGroup(child);
			}
		}
	}

	private int removeDeprecated(GroupTree tree) {
		int hits = 0;
		Iterator<? extends GroupTree> g = tree.getSubGroups().iterator();
		while (g.hasNext()) {
			GroupTree child = g.next();
			hits += removeDeprecated(child);
			if (child.getAllOperatorDescriptions().size() == 0) {
				g.remove();
			}
		}
		Iterator<OperatorDescription> o = tree.getOperatorDescriptions().iterator();
		while (o.hasNext()) {
			OperatorDescription description = o.next();
			if (description.isDeprecated()) {
				o.remove();
			} else {
				hits++;
			}
		}
		return hits;
	}

	/**
	 * Recursively deletes nodes from the filteredTree when neither the name of the node nor the
	 * name of one of its children or parents matches the filter. Stores paths to nodes that match
	 * the filter (not containing the node itself) in expandedPaths.
	 *
	 * @param filter
	 *            filter for which names to take
	 * @param filteredTree
	 *            the tree to filter
	 * @param expandedPaths
	 *            list of paths that should be expanded when the tree is displayed
	 * @param path
	 *            the current path
	 * @return number of hits below the current path
	 */
	private int removeFilteredInstances(OperatorFilter filter, GroupTree filteredTree, List<TreePath> expandedPaths,
			TreePath path) {
		int hits = 0;
		Iterator<? extends GroupTree> g = filteredTree.getSubGroups().iterator();
		while (g.hasNext()) {
			GroupTree child = g.next();
			boolean matches = filter.matches(child.getName());
			if (matches) {
				expandedPaths.add(path);
			}

			hits += removeFilteredInstances(filter, child, expandedPaths, path.pathByAddingChild(child));
			if (child.getAllOperatorDescriptions().size() == 0 && !matches) {
				g.remove();
			}

		}

		// remove non matching operator descriptions if the group does not match, keep all in
		// matching group, count hits even if matching
		boolean groupMatches = filter.matches(filteredTree.getName());
		Iterator<OperatorDescription> o = filteredTree.getOperatorDescriptions().iterator();
		while (o.hasNext()) {
			OperatorDescription description = o.next();
			boolean matches = filter.matches(description.getName()) || filter.matches(description.getShortName());
			if (!matches) {
				for (String tag : description.getTags()) {
					matches = filter.matches(tag);
					if (matches) {
						break;
					}
				}
			}
			if (!filterDeprecated) {
				for (String replaces : description.getReplacedKeys()) {
					matches |= filter.matches(replaces);
				}
			}
			if (!matches && !groupMatches) {
				o.remove();
			} else {
				hits++;
			}
		}

		if (hits > 0) {
			expandedPaths.add(path);
		}
		return hits;
	}

	public void setSortByUsage(boolean sort) {
		if (sort != this.sortByUsage) {
			this.sortByUsage = sort;
			updateTree();
		}
	}

	private int removeFilteredInstances(String[] terms, GroupTree filteredTree, List<TreePath> expandedPaths,
			TreePath path) {
		int hits = 0;

		Iterator<? extends GroupTree> g = filteredTree.getSubGroups().iterator();
		while (g.hasNext()) {
			GroupTree child = g.next();
			String lowerCaseName = child.getName().toLowerCase();
			boolean matches = true;
			for (String term : terms) {
				if (!lowerCaseName.contains(term)) {
					matches = false;
					break;
				}
			}

			if (matches) {
				expandedPaths.add(path);
			}

			hits += removeFilteredInstances(terms, child, expandedPaths, path.pathByAddingChild(child));
			if (child.getAllOperatorDescriptions().size() == 0 && !matches) {
				g.remove();
			}
		}

		// remove non matching operator descriptions if the group does not match, keep all in
		// matching group, count hits even if matching
		boolean groupMatches = true;
		String lowerCaseName = filteredTree.getName().toLowerCase();
		for (String term : terms) {
			if (!lowerCaseName.contains(term)) {
				groupMatches = false;
				break;
			}
		}

		Iterator<OperatorDescription> o = filteredTree.getOperatorDescriptions().iterator();
		while (o.hasNext()) {
			OperatorDescription description = o.next();

			boolean matches = true;
			for (String term : terms) {
				// check names
				if (description.getName().toLowerCase().contains(term)) {
					continue;
				}
				if (description.getShortName().toLowerCase().contains(term)) {
					continue;
				}
				// check tags
				boolean foundTag = false;
				for (String tag : description.getTags()) {
					if (tag.toLowerCase().contains(term)) {
						foundTag = true;
						break;
					}
				}
				if (foundTag) {
					continue;
				}
				// replaced keys
				boolean foundReplacedKey = false;
				if (!filterDeprecated) {
					for (String replaces : description.getReplacedKeys()) {
						replaces.toLowerCase().contains(term);
						foundReplacedKey = true;
						break;
					}
				}
				if (foundReplacedKey) {
					continue;
				}
				// term not found
				matches = false;
				break;
			}

			if (!matches && !groupMatches) {
				o.remove();
			} else {
				hits++;
			}
		}

		if (hits > 0) {
			expandedPaths.add(path);
		}

		return hits;
	}

	@Override
	public void operatorRegistered(OperatorDescription description, OperatorDocBundle bundle) {
		updateTree();
	}

	@Override
	public void operatorUnregistered(OperatorDescription description) {
		updateTree();
	}
}
