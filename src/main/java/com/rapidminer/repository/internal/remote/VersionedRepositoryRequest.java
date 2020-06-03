/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Container for the versioned repositories REST service POST/PATCH requests.
 *
 * @author Marco Boeck
 * @since 9.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VersionedRepositoryRequest {

	@JsonPropertyOrder({ "primaryKeyId", "key" })
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class RepositorySecret {

		@JsonPropertyOrder({ "keyData", "outputPrefixType", "keyId", "status" })
		@JsonIgnoreProperties(ignoreUnknown = true)
		private static class TinkKey {

			@JsonPropertyOrder({ "typeUrl", "keyMaterialType", "value" })
			@JsonIgnoreProperties(ignoreUnknown = true)
			private static class TinkKeyData {

				@JsonProperty
				private String typeUrl;
				@JsonProperty
				private String keyMaterialType;
				@JsonProperty
				private String value;


				private String getTypeUrl() {
					return typeUrl;
				}

				private String getKeyMaterialType() {
					return keyMaterialType;
				}

				private String getValue() {
					return value;
				}
			}

			@JsonProperty
			private TinkKeyData keyData;
			@JsonProperty
			private String outputPrefixType;
			@JsonProperty
			private Long keyId;
			@JsonProperty
			private String status;


			private TinkKeyData getKeyData() {
				return keyData;
			}

			private String getOutputPrefixType() {
				return outputPrefixType;
			}

			private Long getKeyId() {
				return keyId;
			}

			private String getStatus() {
				return status;
			}
		}

		@JsonProperty
		private Long primaryKeyId;
		@JsonProperty
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private List<TinkKey> key;


		private Long getPrimaryKeyId() {
			return primaryKeyId;
		}

		private List<TinkKey> getKey() {
			return key;
		}
	}

	/**
	 * Single privilege a group can have in {@link RepositoryPermission}.
	 *
	 * @author Jan Czogalla
	 * @since 9.7
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public enum RepositoryPrivilege {
		READ, WRITE, OWNER;

		@JsonCreator
		public static RepositoryPrivilege fromName(String name) {
			if (name == null) {
				return null;
			}
			try {
				return valueOf(name);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
	}

	/**
	 * A permission specifying the group and its {@link RepositoryPrivilege}.
	 *
	 * @author Jan Czogalla
	 * @since 9.7
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class RepositoryPermission {

		@JsonProperty
		private String groupName;

		@JsonProperty
		private RepositoryPrivilege privilege;

		public String getGroupName() {
			return groupName;
		}

		public void setGroupName(String groupName) {
			this.groupName = groupName;
		}

		public RepositoryPrivilege getPrivilege() {
			return privilege;
		}

		public void setPrivilege(RepositoryPrivilege privilege) {
			this.privilege = privilege;
		}
	}

	@JsonProperty
	private String name;
	@JsonProperty
	private String displayName;
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String description;
	@JsonProperty
	private RepositorySecret secret;
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private List<RepositoryPermission> permissions;


	/**
	 * Get the name of the repository.
	 *
	 * @return the name of the repository. Never {@code null} or empty
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the repository.
	 *
	 * @param name the name of the repository. Must not be {@code null} or empty and must be in a compatible format with
	 *             no whitespaces and only contain symbols 'A-Z', 'a-z', '0-9', and '-'.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the display name of the repository. Shown in the AI Hub browser.
	 *
	 * @return the display name, never {@code null} or empty
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Set the display name of the repository. Shown in the AI Hub browser.
	 *
	 * @param displayName the display name, must not be {@code null} or empty
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Get the description of the repository. Shown in the AI Hub browser.
	 *
	 * @return the description, can be {@code null}
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the description of the repository. Shown in the AI Hub browser.
	 *
	 * @param description the description, can be {@code null}
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Gets the list of {@link RepositoryPermission RepositoryPermissions} for this repository.
	 *
	 * @return the permitted groups, can be {@code null} or empty if nobody has access to it
	 */
	public List<RepositoryPermission> getPermissions() {
		return permissions;
	}

	/**
	 * Set the permitted group names that have access to this repository.
	 *
	 * @param permissions the permitted groups, can be {@code null} or empty if nobody has access to it
	 */
	public void setPermissions(List<RepositoryPermission> permissions) {
		this.permissions = permissions;
	}

	/**
	 * Get the encryption keyset handle used for encryption of this repository.
	 *
	 * @return the object, never {@code null}
	 */
	@JsonIgnore
	public RepositorySecret getSecret() {
		return this.secret;
	}

	/**
	 * Sets the encryption keyset handle that should be used for encryption of this repository.
	 *
	 * @param secret optional. the secret, can be {@code null} if AI Hub should create a new one
	 */
	@JsonIgnore
	public void setSecret(RepositorySecret secret) {
		this.secret = secret;
	}
}
