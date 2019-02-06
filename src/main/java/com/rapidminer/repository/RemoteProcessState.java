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
package com.rapidminer.repository;

/**
 * 
 * @author Simon Fischer
 * 
 */
public enum RemoteProcessState {
	PENDING(false, false, "clock_run.png"), RUNNING(false, false, "media_play.png"), COMPLETED(true, true, "check.png"), FAILED(
			true, false, "error.png"), STOPPED(true, false, "media_stop.png"), ZOMBIE(true, false, "skull.png");

	// STOP_REQUESTED(false, "media_stop.png");

	private String iconName;
	private boolean terminated;
	private boolean successful;

	private RemoteProcessState(boolean terminated, boolean successful, String iconName) {
		this.terminated = terminated;
		this.successful = successful;
		this.iconName = iconName;
	}

	public boolean isTerminated() {
		return terminated;
	}

	public String getIconName() {
		return iconName;
	}

	public boolean isSuccessful() {
		return successful;
	}
}
