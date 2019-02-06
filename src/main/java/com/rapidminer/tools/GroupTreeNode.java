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

import com.rapidminer.tools.documentation.GroupDocumentation;
import com.rapidminer.tools.documentation.OperatorDocBundle;

import java.util.logging.Level;


/**
 * A group tree manages operator descriptions in a tree like manner. This is useful to present the
 * operators in groups and subgroups and eases operator selection in the GUI.
 * 
 * The group tree heavily depends on the associated OperatorService, since it reflects the
 * registered Operators of that Service. Each {@link OperatorService} can have multiple GroupTrees,
 * which register as listener to be able to update on new registration or unregistration events.
 * 
 * @author Ingo Mierswa, Sebastian Land
 */
public class GroupTreeNode extends GroupTree {

	/** The key used for mapping I18N support. */
	private String key = null;

	/** The parent of this group. This is the root node if parent is null. */
	private GroupTree parent = null;

	private final GroupDocumentation documentation;

	/** Creates a new group tree with no operators and children. */
	GroupTreeNode(GroupTree parent, String key, OperatorDocBundle bundle) {
		this.parent = parent;
		this.key = key;
		if (bundle != null) {
			this.documentation = ((GroupDocumentation) bundle.getObject("group." + getFullyQualifiedKey()));
		} else {
			// LogService.getRoot().fine("No documentation bundle associated with group " +
			// getFullyQualifiedKey());
			LogService.getRoot().log(Level.FINE, "com.rapidminer.tools.GroupTreeNode.no_documentation_bundle_associated",
					getFullyQualifiedKey());
			this.documentation = new GroupDocumentation(key);
		}
	}

	/** Clone constructor. */
	GroupTreeNode(GroupTreeNode other) {
		super(other);
		this.key = other.key;
		this.documentation = other.documentation;
	}

	@Override
	public GroupTree clone() {
		return new GroupTreeNode(this);
	}

	/** Returns the name of this group. */
	@Override
	public String getName() {
		return getDocumentation().getName();
	}

	private GroupDocumentation getDocumentation() {
		return documentation;
	}

	/** Sets the parent of this group. */
	public void setParent(GroupTree parent) {
		this.parent = parent;
	}

	/** Returns the parent of this group. Returns null if no parent does exist. */
	@Override
	public GroupTree getParent() {
		return parent;
	}

	@Override
	public String getDescription() {
		return documentation.getHelp();
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public String getIconName() {
		String groupIcon = super.getIconName();
		if (groupIcon != null) {
			return groupIcon;
		} else {
			return parent.getIconName();
		}
	}

	@Override
	public int compareTo(GroupTree o) {
		return this.getName().compareTo(o.getName());
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GroupTreeNode)) {
			return false;
		}
		GroupTreeNode a = (GroupTreeNode) o;
		if (!this.key.equals(a.key)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return this.key.hashCode();
	}

	@Override
	public String getFullyQualifiedKey() {
		String parentKey = parent.getFullyQualifiedKey();
		if (parentKey.length() > 0) {
			return parentKey + "." + key;
		} else {
			return key;
		}
	}
}
