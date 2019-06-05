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
package com.rapidminer.repository.internal.remote;

import com.rapidminer.tools.ValidationUtil;


/**
 * Container object with the necessary information to create a new vault entry using the {@link RemoteRepository}
 *
 * @author Andreas Timm
 * @since 9.3
 */
public class RemoteCreateVaultInformation {
	private String group;
	private String name;
	private String value;

	/**
	 * Data to create in the vault
	 *
	 * @param group
	 * 		the group the info belongs to
	 * @param name
	 * 		the name to be used when injecting this value
	 * @param value
	 * 		the value to be injected
	 */
	public RemoteCreateVaultInformation(String group, String name, String value) {
		this.group = ValidationUtil.requireNonNull(group);
		this.name = ValidationUtil.requireNonNull(name);
		this.value = value;
	}

	/**
	 * The group the info belongs to
	 *
	 * @return name of the group
	 */
	public String getGroup() {
		return group;
	}

	private void setGroup(String group) {
		this.group = ValidationUtil.requireNonNull(group);
	}

	/**
	 * The name for this info
	 *
	 * @return name of this info
	 */
	public String getName() {
		return name;
	}

	private void setName(String name) {
		this.name = ValidationUtil.requireNonNull(name);
	}

	/**
	 * The value to be injected
	 *
	 * @return value for injection
	 */
	public String getValue() {
		return value;
	}

	private void setValue(String value) {
		this.value = value;
	}
}
