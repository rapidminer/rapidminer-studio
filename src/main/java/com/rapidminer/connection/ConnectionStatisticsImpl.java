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
package com.rapidminer.connection;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;


/**
 * Implementation of {@link ConnectionStatistics}
 *
 * @author Jan Czogalla
 * @since 9.3
 */
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class ConnectionStatisticsImpl implements ConnectionStatistics {

	private ZonedDateTime lastSuccess;
	private ZonedDateTime lastChange;
	private String lastError;

	/** Minimal constructor for Json; no change, no statistics */
	ConnectionStatisticsImpl() {
		lastChange = ZonedDateTime.now();
	}

	/** Constructor with successful start. Also updates time of change with the same time as the success */
	public ConnectionStatisticsImpl(ZonedDateTime lastSuccess) {
		this.lastSuccess = lastSuccess;
		this.lastChange = lastSuccess;
	}

	/** Constructor with failed start. Also updates time of change with now */
	public ConnectionStatisticsImpl(String lastError) {
		this();
		this.lastError = lastError;
	}

	@Override
	public ZonedDateTime getLastSuccess() {
		return lastSuccess;
	}

	@Override
	public ZonedDateTime getLastChange() {
		return lastChange;
	}

	@Override
	public String getLastError() {
		return lastError;
	}

	@Override
	public ConnectionStatistics updateSuccess() {
		this.lastSuccess = ZonedDateTime.now();
		return this;
	}

	@Override
	public ConnectionStatistics updateChange() {
		// lets start and keep track of all changes here if possible (enterprise feature audit) // comment by AnTi
		this.lastChange = ZonedDateTime.now();
		return this;
	}

	@Override
	public ConnectionStatistics updateError(String error) {
		this.lastError = error;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ConnectionStatisticsImpl that = (ConnectionStatisticsImpl) o;
		return Objects.equals(lastSuccess, that.lastSuccess) &&
				Objects.equals(lastChange, that.lastChange) &&
				Objects.equals(lastError, that.lastError);
	}

	@Override
	public int hashCode() {
		return Objects.hash(lastSuccess, lastChange, lastError);
	}

	/** Json specific getter to convert dates to strings */
	@JsonAnyGetter
	private Map<String, String> getJsonValues() {
		Map<String, String> values = new HashMap<>();
		values.put("lastSuccess", lastSuccess != null ? lastSuccess.toString() : null);
		values.put("lastChange", lastChange != null ? lastChange.toString() : null);
		values.put("lastError", lastError);
		return values;
	}

	/** Json specific setter to convert strings to dates */
	@JsonAnySetter
	private void setJsonValue(String name, String value) {
		switch (name) {
			case "lastChange":
				this.lastChange = ZonedDateTime.parse(Objects.requireNonNull(StringUtils.trimToNull(value)));
				return;
			case "lastSuccess":
				this.lastSuccess = StringUtils.trimToNull(value) == null ? null : ZonedDateTime.parse(value);
				return;
			case "lastError":
				this.lastError = StringUtils.trimToNull(value);
				return;
			default:
		}
	}

}
