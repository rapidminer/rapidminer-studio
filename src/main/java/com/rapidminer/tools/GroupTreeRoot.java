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

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.tools.OperatorService.OperatorServiceListener;
import com.rapidminer.tools.documentation.OperatorDocBundle;


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
public class GroupTreeRoot extends GroupTree implements OperatorServiceListener {

	/**
	 * PackagePrivate Constructor for the OperatorService.
	 */
	GroupTreeRoot() {
		OperatorService.addOperatorServiceListener(this);
	}

	/**
	 * Clone constructor. This will keep the link to the {@link OperatorService}. Whenever a new
	 * Operator is added or removed, this will be reflected in the copy as well. For detecting such
	 * events, please register on
	 * {@link OperatorService#addOperatorServiceListener(OperatorServiceListener)}.
	 * */
	protected GroupTreeRoot(GroupTreeRoot other) {
		super(other);
		OperatorService.addOperatorServiceListener(this);
	}

	/** Returns a deep clone of this tree. */
	@Override
	public GroupTree clone() {
		return new GroupTreeRoot(this);
	}

	/** Returns the name of this group. */
	@Override
	public String getName() {
		return "Root";
	}

	/** Returns the parent of this group. Returns null if no parent does exist. */
	@Override
	public GroupTreeRoot getParent() {
		return null;
	}

	@Override
	public String getDescription() {
		return "";
	}

	@Override
	public String getKey() {
		return "";
	}

	@Override
	public String getFullyQualifiedKey() {
		return "";
	}

	/**
	 * This method will be called by the {@link OperatorService}, whenever a new {@link Operator}
	 * has been registered. The given bundle is used to derive subgroup names from.
	 */
	@Override
	public void operatorRegistered(OperatorDescription description, OperatorDocBundle bundle) {
		String groupKey = description.getGroup();
		String[] groupKeys = groupKey.split("\\.");
		GroupTree group = this;
		for (int i = 0; i < groupKeys.length; i++) {
			group = group.getOrCreateSubGroup(groupKeys[i], bundle);
		}
		group.addOperatorDescription(description);
	}

	/**
	 * This method will be called by the {@link OperatorService}, whenever an {@link Operator} has
	 * been unregistered.
	 */
	@Override
	public void operatorUnregistered(OperatorDescription description) {
		String groupKey = description.getGroup();
		String[] groupKeys = groupKey.split("\\.");
		GroupTree group = this;
		for (int i = 0; i < groupKeys.length && group != null; i++) {
			group = group.getSubGroup(groupKeys[i]);
		}
		if (group != null) {
			group.removeOperatorDescription(description);
		}
	}

	/**
	 * Finds the group for the given fully qualified name (dot separated) if existing, returning
	 * null otherwise.
	 */
	public GroupTree findGroup(String fullyQualifiedGroupName) {
		String[] groupKeys = fullyQualifiedGroupName.split("\\.");
		GroupTree group = this;
		for (int i = 0; i < groupKeys.length && group != null; i++) {
			group = group.getSubGroup(groupKeys[i]);
		}
		return group;
	}

	/**
	 * Finds or creates the group for the given fully qualified name (dot separated).
	 * 
	 * @param bundle
	 */
	public GroupTree findOrCreateGroup(String fullyQualifiedGroupName, OperatorDocBundle bundle) {
		String[] groupKeys = fullyQualifiedGroupName.split("\\.");
		GroupTree group = this;
		for (int i = 0; i < groupKeys.length && group != null; i++) {
			group = group.getOrCreateSubGroup(groupKeys[i], bundle);
		}
		return group;
	}
}
