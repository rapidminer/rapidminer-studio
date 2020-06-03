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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Container for the versioned repositories REST service POST/PATCH responses and the GET response.
 *
 * @author Marco Boeck
 * @since 9.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VersionedRepositoryInformation extends VersionedRepositoryRequest {

	@JsonProperty
	private Long createdAt;
	@JsonProperty
	private String cloneUrl;
	@JsonProperty
	private String uid;


	/**
	 * The timestamp (ms since epoch in GMT) the repository was created.
	 *
	 * @return the timestamp, never {@code null}
	 */
	public Long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Long createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * The git URL which can be used for direct cloning of the Git repository.
	 *
	 * @return the url string ending in .git, never {@code null}
	 */
	public String getCloneUrl() {
		return cloneUrl;
	}

	public void setCloneUrl(String cloneUrl) {
		this.cloneUrl = cloneUrl;
	}

	/**
	 * The repository's UID.
	 *
	 * @return the UID of the repository
	 */
	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

}
