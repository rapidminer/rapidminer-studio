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
package com.rapidminer.repository.search;

import com.rapidminer.repository.Entry;


/**
 * POJO for a repository Global Search item, describing one {@link com.rapidminer.repository.Entry}.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public class RepositoryGlobalSearchItem {

	private String name;
	private String type;
	private String parent;
	private String owner;
	private String location;
	private String modified;
	private String[] attributes;
	private String connectionName;
	private String connectionType;
	private String[] connectionTags;

	/**
	 * The name of this item.
	 *
	 * @return the name, never {@code null}.
	 */
	public String getName() {
		return name;
	}

	/**
	 * The name of the parent folder in the repository of this item.
	 *
	 * @return the name, never {@code null} but may be empty if it's in the root folder.
	 */
	public String getParent() {
		return parent;
	}

	/**
	 * The name of the user who owns this item.
	 *
	 * @return the name, may be {@code null}.
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * The type of this entry. See {@link Entry#getType()} .
	 *
	 * @return the type, never {@code null}.
	 */
	public String getType() {
		return type;
	}

	/**
	 * The full repository location of this item.
	 *
	 * @return the full location, never {@code null}.
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * The last modified time.
	 *
	 * @return the timestamp in ms since epoch, may be {@code null} or empty.
	 */
	public String getModified() {
		return modified;
	}

	/**
	 * The attribute names associated with this entry.
	 *
	 * @return the attribute names, may be {@code null} or empty.
	 */
	public String[] getAttributes() {
		return attributes;
	}

	/**
	 * The connection name associated with this entry
	 *
	 * @return the connection name, may be {@code null} or empty.
	 * @since 9.3
	 */
	public String getConnectionName() {
		return connectionName;
	}

	/**
	 * The connection type associated with this entry
	 *
	 * @return the connection type, may be {@code null} or empty
	 * @since 9.3
	 */
	public String getConnectionType() {
		return connectionType;
	}

	/**
	 * The connection tags associated with this entry
	 *
	 * @return the connection tags, may be {@code null} or empty
	 * @since 9.3
	 */
	public String[] getConnectionTags() {
		return connectionTags;
	}

	public RepositoryGlobalSearchItem setName(String name) {
		this.name = name;
		return this;
	}

	public RepositoryGlobalSearchItem setParent(String parent) {
		this.parent = parent;
		return this;
	}

	public RepositoryGlobalSearchItem setOwner(String owner) {
		this.owner = owner;
		return this;
	}

	public RepositoryGlobalSearchItem setType(String type) {
		this.type = type;
		return this;
	}

	public RepositoryGlobalSearchItem setLocation(String location) {
		this.location = location;
		return this;
	}

	public RepositoryGlobalSearchItem setModified(String modified) {
		this.modified = modified;
		return this;
	}

	public RepositoryGlobalSearchItem setAttributes(String[] attributes) {
		this.attributes = attributes;
		return this;
	}

	public void setConnectionName(String connectionName) {
		this.connectionName = connectionName;
	}

	public void setConnectionType(String connectionType) {
		this.connectionType = connectionType;
	}

	public void setConnectionTags(String[] connectionTags) {
		this.connectionTags = connectionTags;
	}

	@Override
	public String toString() {
		return getLocation();
	}
}
