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
package com.rapidminer.studio.io.data;

import com.rapidminer.core.io.data.ColumnMetaData;


/**
 * A simple implementation of the {@link ColumnMetaData} interface.
 *
 * @author Gisa Schaefer
 * @since 7.0.0
 */
public class DefaultColumnMetaData implements ColumnMetaData {

	private String name;
	private ColumnType type;
	private String role;
	private boolean isRemoved;

	/**
	 * Creates a {@link ColumnMetaData} instance with the given data.
	 */
	public DefaultColumnMetaData(String name, ColumnType type) {
		this(name, type, null, false);
	}

	/**
	 * Creates a {@link ColumnMetaData} instance with the given data.
	 */
	public DefaultColumnMetaData(String name, ColumnType type, String role, boolean isRemoved) {
		this.name = name;
		this.type = type;
		this.role = role;
		this.isRemoved = isRemoved;
	}

	/**
	 * Copy constructor.
	 *
	 * @param other
	 *            the instance to copy.
	 */
	DefaultColumnMetaData(ColumnMetaData other) {
		this.name = other.getName();
		this.type = other.getType();
		this.role = other.getRole();
		this.isRemoved = other.isRemoved();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public ColumnType getType() {
		return type;
	}

	@Override
	public void setType(ColumnType type) {
		this.type = type;
	}

	@Override
	public String getRole() {
		return role;
	}

	@Override
	public void setRole(String role) {
		this.role = role;
	}

	@Override
	public boolean isRemoved() {
		return isRemoved;
	}

	@Override
	public void setRemoved(boolean removed) {
		this.isRemoved = removed;
	}

}
