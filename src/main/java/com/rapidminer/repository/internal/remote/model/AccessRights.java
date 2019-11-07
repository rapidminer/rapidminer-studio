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
package com.rapidminer.repository.internal.remote.model;

import com.rapidminer.repository.AccessFlag;


/**
 * AccessRights to read, write, execute for a group
 *
 * @author Andreas Timm
 * @since 9.5.0
 */
public class AccessRights {

	protected String group;
	protected String execute;
	protected String read;
	protected String write;

	/**
	 * {@link com.rapidminer.repository.AccessFlag} String representation of permission to execute
	 *
	 * @return {@link AccessFlag#toString()}
	 */
	public String getExecute() {
		return execute;
	}

	public void setExecute(String execute) {
		this.execute = execute;
	}

	/**
	 * Name of the group that has these {@link AccessRights}
	 *
	 * @return name of the group
	 */
	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * {@link com.rapidminer.repository.AccessFlag} String representation of permission to read
	 *
	 * @return {@link AccessFlag#toString()}
	 */
	public String getRead() {
		return read;
	}

	public void setRead(String read) {
		this.read = read;
	}

	/**
	 * {@link com.rapidminer.repository.AccessFlag} String representation of permission to write
	 *
	 * @return {@link AccessFlag#toString()}
	 */
	public String getWrite() {
		return write;
	}

	public void setWrite(String write) {
		this.write = write;
	}
}
