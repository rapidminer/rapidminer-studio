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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Container for response data for the RapidMiner Vault entries
 *
 * @author Andreas Timm
 * @since 9.3
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteVaultEntry {

	/** Ask Server team for details. This Key contains the group. */
	public static class Key {
		private String group;

		public String getGroup() {
			return group;
		}

		public void setGroup(String group) {
			this.group = group;
		}
	}

	/** The Parameter contains injection info like the name, a Key and more */
	public static class Parameter {
		private String name;
		private boolean encrypted;
		private boolean injectable;
		private Key key;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isEncrypted() {
			return encrypted;
		}

		public void setEncrypted(boolean encrypted) {
			this.encrypted = encrypted;
		}

		public boolean isInjectable() {
			return injectable;
		}

		public void setInjectable(boolean injectable) {
			this.injectable = injectable;
		}

		public Key getKey() {
			return key;
		}

		public void setKey(Key key) {
			this.key = key;
		}
	}

	private String id;
	private Parameter parameter;
	private String value;
	private double updatedAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Parameter getParameter() {
		return parameter;
	}

	public void setParameter(Parameter parameter) {
		this.parameter = parameter;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public double getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(double updatedAt) {
		this.updatedAt = updatedAt;
	}
}