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
package com.rapidminer.tools;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.tools.OperatorService.OperatorServiceListener;
import com.rapidminer.tools.documentation.OperatorDocBundle;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ImageIcon;


/**
 * A group tree manages operator descriptions in a tree like manner. This is useful to present the
 * operators in groups and subgroups and eases operator selection in the GUI.
 * 
 * The group tree heavily depends on the associated OperatorService, since it reflects the
 * registered Operators of that Service. Each {@link OperatorService} can have multiple GroupTrees,
 * which register as listener to be able to update on new registration or unregistration events.
 * 
 * The listening is done by the {@link GroupTreeRoot} class implementing the
 * {@link OperatorServiceListener} interface.
 * 
 * @author Ingo Mierswa, Sebastian Land
 */
public abstract class GroupTree implements Comparable<GroupTree> {

	private static final ImageIcon[] NO_ICONS = new ImageIcon[3];

	/** The list of operators in this group. */
	private final List<OperatorDescription> operators = new LinkedList<OperatorDescription>();

	/** The subgroups of this group. */
	private final Map<String, GroupTreeNode> children = new LinkedHashMap<String, GroupTreeNode>();

	private String iconName;

	private ImageIcon[] icons;

	protected GroupTree() {

	}

	/**
	 * Clone constructor. This will keep the link to the {@link OperatorService}. Whenever a new
	 * Operator is added or removed, this will be reflected in the copy as well. For detecting such
	 * events, please register on
	 * {@link OperatorService#addOperatorServiceListener(OperatorServiceListener)}.
	 * */
	protected GroupTree(GroupTree other) {
		this.operators.addAll(other.operators);
		Iterator<? extends GroupTree> g = other.getSubGroups().iterator();
		while (g.hasNext()) {
			GroupTree child = g.next();
			addSubGroup((GroupTreeNode) child.clone());
		}
	}

	/** Returns the parent of this group. Returns null if no parent does exist. */
	public abstract GroupTree getParent();

	/** Returns or creates the subgroup with the given key. This is not the fully qualified key! */
	public GroupTree getSubGroup(String key) {
		return children.get(key);
	}

	/** Returns or creates the subgroup with the given name, creating it if not present. */
	public GroupTree getOrCreateSubGroup(String key, OperatorDocBundle bundle) {
		GroupTreeNode child = children.get(key);
		if (child == null) {
			child = new GroupTreeNode(this, key, bundle);
			addSubGroup(child);
		}
		return child;
	}

	/** Returns a set of all children group trees. */
	public Collection<? extends GroupTree> getSubGroups() {
		return children.values();
	}

	/** Returns the index of the given subgroup or -1 if the sub group is not a child of this node. */
	public int getIndexOfSubGroup(GroupTree child) {
		Iterator<? extends GroupTree> i = getSubGroups().iterator();
		int index = 0;
		while (i.hasNext()) {
			GroupTree current = i.next();
			if (current.equals(child)) {
				return index;
			}
			index++;
		}
		return -1;
	}

	/** Returns the i-th sub group. */
	public GroupTree getSubGroup(int index) {
		Collection<? extends GroupTree> allChildren = getSubGroups();
		if (index >= allChildren.size()) {
			return null;
		} else {
			Iterator<? extends GroupTree> i = allChildren.iterator();
			int counter = 0;
			while (i.hasNext()) {
				GroupTree current = i.next();
				if (counter == index) {
					return current;
				}
				counter++;
			}
			return null;
		}
	}

	/** Adds an operator to this group. */
	protected void addOperatorDescription(OperatorDescription description) {
		operators.add(description);
	}

	/**
	 * This removes the given {@link OperatorDescription} from this GroupTree
	 */
	protected void removeOperatorDescription(OperatorDescription description) {
		operators.remove(description);
	}

	/**
	 * Returns all operator descriptions in this group or an empty list if this group does not
	 * contain any operators.
	 */
	public List<OperatorDescription> getOperatorDescriptions() {
		return operators;
	}

	/**
	 * Returns all operator in this group and recursively the operators of all children.
	 */
	public Set<OperatorDescription> getAllOperatorDescriptions() {
		Set<OperatorDescription> result = new TreeSet<OperatorDescription>();
		addAllOperatorDescriptions(result);
		return result;
	}

	private void addAllOperatorDescriptions(Set<OperatorDescription> operators) {
		operators.addAll(this.operators);
		Iterator<GroupTreeNode> i = children.values().iterator();
		while (i.hasNext()) {
			GroupTree child = i.next();
			child.addAllOperatorDescriptions(operators);
		}
	}

	/**
	 * This returns a deep clone of this {@link GroupTree}.
	 */
	@Override
	public abstract GroupTree clone();

	/**
	 * This returns a group description depending on internationalization
	 */
	public abstract String getDescription();

	/**
	 * Gets the key of this group. This is used for internationalization and does not contain the
	 * parent groups. See {@link #getFullyQualifiedKey()} for this one.
	 */
	public abstract String getKey();

	/**
	 * Returns the fully qualified key where each group key is separated by dot. The qualified key
	 * starts with the highest parent group.
	 */
	public abstract String getFullyQualifiedKey();

	/**
	 * Deprecated method that returns the fully qualified key. See {@link #getFullyQualifiedKey()}
	 * for Details.
	 */
	@Deprecated
	public String getQName() {
		return getFullyQualifiedKey();
	}

	/** Returns the name of this group. This name depends on internationalization! */
	public abstract String getName();

	public void setIconName(String icon) {
		this.iconName = icon;
		loadIcons();
	}

	public String getIconName() {
		if (this.iconName != null) {
			return this.iconName;
		} else {
			return null;
		}
	}

	public ImageIcon[] getIcons() {
		if (icons != null) {
			return icons;
		} else {
			return NO_ICONS;
		}
	}

	protected final int countOperators() {
		int count = operators.size();
		for (GroupTree tree : children.values()) {
			count += tree.countOperators();
		}
		return count;

	}

	/**
	 * This method will sort this GroupTree according to the given comparator.
	 */
	public void sort(Comparator<OperatorDescription> comparator) {
		Collections.sort(operators, comparator);
		for (GroupTree child : children.values()) {
			child.sort(comparator);
		}
	}

	/** Adds a subgroup to this group. */
	private void addSubGroup(GroupTreeNode child) {
		children.put(child.getKey(), child);
		child.setParent(this);
	}

	/**
	 * Loads the icons if defined.
	 */
	private void loadIcons() {
		if (iconName == null) {
			icons = null;
			return;
		}
		icons = new ImageIcon[3];
		icons[0] = SwingTools.createIcon("16/" + iconName);
		icons[1] = SwingTools.createIcon("24/" + iconName);
		icons[2] = SwingTools.createIcon("48/" + iconName);
	}

	@Override
	public String toString() {
		String result = getName();
		if (getParent() == null) {
			result = "Root";
		}
		int size = countOperators();
		return result + (size > 0 ? " (" + size + ")" : "");
	}

	@Override
	public int compareTo(GroupTree o) {
		return this.getName().compareTo(o.getName());
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GroupTree)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return this.getKey().hashCode();
	}

	/**
	 * This method remains only for compatibility. If existing the group will be returned. Please
	 * notice, that it is not advised to interact with the GroupTree manually. Please use the
	 * {@link OperatorService#registerOperator(OperatorDescription)} method to add operators and
	 * modify the GroupTree accordingly.
	 */
	@Deprecated
	public static GroupTree findGroup(String fullyQualifiedGroupName, OperatorDocBundle object) {
		GroupTreeRoot root = (GroupTreeRoot) OperatorService.getGroups();
		return root.findGroup(fullyQualifiedGroupName);
	}
}
