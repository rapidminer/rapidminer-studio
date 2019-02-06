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
package com.rapidminer.studio.concurrency.internal.util;

import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.util.logging.Level;

import com.rapidminer.Process;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;


/**
 * This class contains information about a process that has been put into the background execution
 * queue. It is used to have multiple versions of the same process entry in the queue.
 * <p>
 * Note that this part of the API is only temporary and might be removed in future versions again.
 * </p>
 *
 * @author Sebastian Land
 * @since 7.4
 */
public class ProcessBackgroundExecution implements BackgroundExecution {

	private Process executedProcess;
	private Process originalProcess;
	private ProcessBackgroundExecutionState state;
	private long scheduledTime;

	/**
	 * Creates a new ProcessBackgroundExcecution object
	 *
	 * @param originalProcess
	 *            The original process
	 * @param executedProcess
	 *            The executed process
	 * @param processState
	 *            The current state of the executed process
	 */
	public ProcessBackgroundExecution(Process originalProcess, Process executedProcess,
			ProcessBackgroundExecutionState processState) {
		this.originalProcess = originalProcess;
		this.executedProcess = executedProcess;
		this.state = processState;
		this.scheduledTime = System.currentTimeMillis();
	}

	public RepositoryLocation getLocation() {
		return originalProcess.getRepositoryLocation();
	}

	public long getScheduledTime() {
		return scheduledTime;
	}

	public ProcessBackgroundExecutionState getBackgroundExecutionState() {
		return state;
	}

	/**
	 * Returns the executed Process
	 *
	 * <p>
	 * Warning: This Process might not be equal to the original Process, use
	 * {@code getOriginalProcess()} or {@code getProcessWithoutLocation()} to display the Process to
	 * the user.
	 * </p>
	 *
	 * @return
	 */
	public Process getProcess() {
		return executedProcess;
	}

	/**
	 * Returns the original process
	 *
	 * @return
	 */
	public Process getOriginalProcess() {
		return originalProcess;
	}

	/**
	 * Returns the process as a "New Process"
	 *
	 * @return
	 */
	public Process getProcessWithoutLocation() {
		try {
			return new Process(originalProcess.getRootOperator().getXML(false));
		} catch (IOException | XMLException e) {
			LogService.getRoot().log(Level.WARNING, "Failed to read process XML, fall back to provided process.", e);
			return originalProcess;
		}
	}

	@Override
	public String getName() {
		StringBuilder builder = new StringBuilder();
		builder.append(getLocation() != null ? getLocation().getAbsoluteLocation() : "Unsaved process");
		builder.append(" [");
		builder.append(DateFormat.getDateTimeInstance().format(new Date(scheduledTime)));
		builder.append("]");

		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (getLocation() == null ? 0 : getLocation().hashCode());
		result = prime * result + (int) (scheduledTime ^ scheduledTime >>> 32);
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
		ProcessBackgroundExecution other = (ProcessBackgroundExecution) obj;
		if (getLocation() == null) {
			if (other.getLocation() != null) {
				return false;
			}
		} else if (!getLocation().equals(other.getLocation())) {
			return false;
		}
		if (scheduledTime != other.scheduledTime) {
			return false;
		}
		return true;
	}

}
