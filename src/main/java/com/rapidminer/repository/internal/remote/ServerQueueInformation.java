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


/**
 * Container for the queue REST service GET requests.
 *
 * @author Marco Boeck
 * @since 9.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerQueueInformation {

	private String name;
	private String[] permittedGroups;
	private int totalPendingJobs;
	private int totalRunningJobs;
	private boolean permanent;


	/**
	 * The name of the queue.
	 *
	 * @return the name, never {@code null}
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * The permitted groups for this queue.
	 *
	 * @return the groups, may be empty but never {@code null}
	 */
	public String[] getPermittedGroups() {
		return permittedGroups;
	}

	public void setPermittedGroups(String[] permittedGroups) {
		this.permittedGroups = permittedGroups;
	}

	/**
	 * The number of jobs currently pending for this queue.
	 *
	 * @return the number of pending jobs
	 */
	public int getTotalPendingJobs() {
		return totalPendingJobs;
	}

	public void setTotalPendingJobs(int totalPendingJobs) {
		this.totalPendingJobs = totalPendingJobs;
	}

	/**
	 * The number of jobs currently running for this queue.
	 *
	 * @return the number of running jobs
	 */
	public int getTotalRunningJobs() {
		return totalRunningJobs;
	}

	public void setTotalRunningJobs(int totalRunningJobs) {
		this.totalRunningJobs = totalRunningJobs;
	}

	/**
	 * Whether this queue is permanent or not.
	 *
	 * @return {@code true} if it is permanent; {@code false} otherwise
	 */
	public boolean isPermanent() {
		return permanent;
	}

	public void setPermanent(boolean permanent) {
		this.permanent = permanent;
	}

}
