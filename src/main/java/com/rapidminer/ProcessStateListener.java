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
package com.rapidminer;

/**
 * This listener can be used to register for process state changes like start/pause/stop initiated
 * by user.
 * 
 * @author Christian Pels, Nils Woehler
 * 
 */
public interface ProcessStateListener {

	/**
	 * Fired if a process is started.
	 * 
	 * @param process
	 *            the process that has been started
	 */
	public void started(Process process);

	/**
	 * Fired if a process was paused.
	 * 
	 * @param process
	 *            the process that was paused
	 */
	public void paused(Process process);

	/**
	 * Fired if a process was resumed.
	 * 
	 * @param process
	 *            the process that was resumed
	 */
	public void resumed(Process process);

	/**
	 * Fired if a process was stopped.
	 * 
	 * @param process
	 *            the process that was stopped
	 */
	public void stopped(Process process);

}
