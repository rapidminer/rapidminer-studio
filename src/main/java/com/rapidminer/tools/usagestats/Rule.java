/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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
package com.rapidminer.tools.usagestats;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Auto generated Rule object for jackson
 *
 * @author Jonas Wilms-Pfau
 * @since 7.5.0
 *
 */
@JsonPropertyOrder({ "id", "queries", "message", "interval", "minStudioVersion", "maxStudioVersion" })
@JsonIgnoreProperties(ignoreUnknown = true)
class Rule {

	@JsonProperty("id")
	private String id;

	@JsonProperty("queries")
	private List<String> queries = null;

	@JsonProperty("message")
	private String message;

	@JsonProperty("interval")
	private Integer interval;

	@JsonProperty("minStudioVersion")
	private String minStudioVersion;

	@JsonProperty("maxStudioVersion")
	private String maxStudioVersion;


	@JsonProperty("id")
	public String getId() {
		return id;
	}

	@JsonProperty("id")
	public void setId(String id) {
		this.id = id;
	}

	@JsonProperty("queries")
	public List<String> getQueries() {
		return queries;
	}

	@JsonProperty("queries")
	public void setQueries(List<String> queries) {
		this.queries = queries;
	}

	@JsonProperty("message")
	public String getMessage() {
		return message;
	}

	@JsonProperty("message")
	public void setMessage(String message) {
		this.message = message;
	}

	@JsonProperty("minStudioVersion")
	public String getMinStudioVersion() {
		return minStudioVersion;
	}

	@JsonProperty("minStudioVersion")
	public void setMinStudioVersion(String minStudioVersion) {
		this.minStudioVersion = minStudioVersion;
	}

	@JsonProperty("maxStudioVersion")
	public String getMaxStudioVersion() {
		return maxStudioVersion;
	}

	@JsonProperty("maxStudioVersion")
	public void setMaxStudioVersion(String maxStudioVersion) {
		this.maxStudioVersion = maxStudioVersion;
	}


	/**
	 * The verification interval in seconds
	 *
	 * @return
	 */
	@JsonProperty("interval")
	public Integer getInterval() {
		return interval;
	}

	@JsonProperty("interval")
	public void setInterval(Integer interval) {
		this.interval = interval;
	}

	@Override
	public String toString() {
		return getId() + " with " + getQueries().size() + " SQL queries.";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (id == null ? 0 : id.hashCode());
		result = prime * result + (interval == null ? 0 : interval.hashCode());
		result = prime * result + (message == null ? 0 : message.hashCode());
		result = prime * result + (queries == null ? 0 : queries.hashCode());
		result = prime * result + (minStudioVersion == null ? 0 : minStudioVersion.hashCode());
		result = prime * result + (maxStudioVersion == null ? 0 : maxStudioVersion.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Rule other = (Rule) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (interval == null) {
			if (other.interval != null) {
				return false;
			}
		} else if (!interval.equals(other.interval)) {
			return false;
		}
		if (message == null) {
			if (other.message != null) {
				return false;
			}
		} else if (!message.equals(other.message)) {
			return false;
		}
		if (minStudioVersion == null) {
			if (other.minStudioVersion != null) {
				return false;
			}
		} else if (!minStudioVersion.equals(other.minStudioVersion)) {
			return false;
		}
		if (maxStudioVersion == null) {
			if (other.maxStudioVersion != null) {
				return false;
			}
		} else if (!maxStudioVersion.equals(other.maxStudioVersion)) {
			return false;
		}
		if (queries == null) {
			if (other.queries != null) {
				return false;
			}
		} else if (!queries.equals(other.queries)) {
			return false;
		}
		return true;
	}
}
