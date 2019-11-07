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
package com.rapidminer.repository.internal.remote.model;

import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * Container for remote executed processes
 *
 * @author Andreas Timm
 * @since 9.5.0
 */
public class ProcessResponse {
	protected XMLGregorianCalendar completionTime;
	protected String exception;
	protected int id;
	protected List<String> outputLocations;
	protected String processLocation;
	protected XMLGregorianCalendar startTime;
	protected String state;
	protected ProcessStackTrace trace;

	public XMLGregorianCalendar getCompletionTime() {
		return completionTime;
	}

	public void setCompletionTime(XMLGregorianCalendar completionTime) {
		this.completionTime = completionTime;
	}

	public String getException() {
		return exception;
	}

	public void setException(String exception) {
		this.exception = exception;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<String> getOutputLocations() {
		return outputLocations;
	}

	public void setOutputLocations(List<String> outputLocations) {
		this.outputLocations = outputLocations;
	}

	public String getProcessLocation() {
		return processLocation;
	}

	public void setProcessLocation(String processLocation) {
		this.processLocation = processLocation;
	}

	public XMLGregorianCalendar getStartTime() {
		return startTime;
	}

	public void setStartTime(XMLGregorianCalendar startTime) {
		this.startTime = startTime;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public ProcessStackTrace getTrace() {
		return trace;
	}

	public void setTrace(ProcessStackTrace trace) {
		this.trace = trace;
	}
}
