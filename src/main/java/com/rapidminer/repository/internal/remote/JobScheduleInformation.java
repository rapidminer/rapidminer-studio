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
 * Container for the job scheduler REST service responses.
 *
 * @author Marco Boeck
 * @since 9.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobScheduleInformation extends JobScheduleRequest {

	@JsonProperty
	private String id;
	@JsonProperty
	private String state;
	@JsonProperty
	private Long nextFireTime;
	@JsonProperty
	private Long previousFireTime;


	/**
	 * The id of this job.
	 *
	 * @return the id, never {@code null}
	 */
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/**
	 * The state of this job.
	 *
	 * @return the state, never {@code null}
	 */
	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	/**
	 * The timestamp (ms since epoch in GMT) of the next time this job will run.
	 *
	 * @return the timestamp or {@code null} if the job will not run again
	 */
	public Long getNextFireTime() {
		return nextFireTime;
	}

	public void setNextFireTime(Long nextFireTime) {
		this.nextFireTime = nextFireTime;
	}

	/**
	 * The timestamp (ms since epoch in GMT) of the last time this job ran.
	 *
	 * @return the timestamp or {@code null} if the job has never run
	 */
	public Long getPreviousFireTime() {
		return previousFireTime;
	}

	public void setPreviousFireTime(Long previousFireTime) {
		this.previousFireTime = previousFireTime;
	}
}
