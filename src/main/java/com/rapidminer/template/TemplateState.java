/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.template;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;


/**
 * Model class containing the state of user-made configuration of a {@link Template}. Also includes
 * results once the template is run.
 * 
 * @author Simon Fischer
 */
public class TemplateState extends Observable {

	public static final String OBSERVER_EVENT_ROLES = "roles";
	public static final String OBSERVER_EVENT_INPUT = "input";
	public static final String OBSERVER_EVENT_RESULTS = "results";
	public static final String OBSERVER_EVENT_TEMPLATE = "template";
	public static final String OBSERVER_EVENT_MACROS = "macros";

	private Date creationTimestamp, dataTimestamp;

	/** The selected template. */
	private Template template;

	/** The raw input data. */
	private ExampleSet inputData;

	/**
	 * Maps {@link RoleRequirement}s (identified by {@link RoleRequirement#getRoleName()} to
	 * attribute names in {@link #inputData}.
	 */
	private Map<String, RoleAssignment> roleAssignments = new HashMap<>();

	/** The results created by the template after {@link #run()}. */
	private IOObject[] results;

	/** Macro values at the end of the process execution. */
	private Map<String, String> macros = new HashMap<>();

	/** If true, the input will be downsampled to improve performance */
	private boolean downsamplingEnabled = true;

	/** True iff the process was opened in the design perspective. */
	private boolean processOpened = false;

	public Template getTemplate() {
		return template;
	}

	public void setTemplate(Template template) {
		if (template == this.template) {
			return;
		}
		creationTimestamp = new Date();
		this.template = template;
		setChanged();
		notifyObservers(OBSERVER_EVENT_TEMPLATE);
	}

	public void assignRole(String roleName, RoleAssignment roleAssignment) {
		roleAssignments.put(roleName, roleAssignment);
		dataTimestamp = new Date();
		setChanged();
		notifyObservers(OBSERVER_EVENT_ROLES);
	}

	public void clearRoleAssignments() {
		roleAssignments.clear();
		dataTimestamp = new Date();
		setChanged();
		notifyObservers(OBSERVER_EVENT_ROLES);
	}

	public RoleAssignment getRoleAssignment(String roleName) {
		return roleAssignments.get(roleName);
	}

	public ExampleSet getInputData() {
		return inputData;
	}

	public void setInputData(ExampleSet inputData) {
		if (inputData == this.inputData) {
			return;
		}
		this.inputData = inputData;
		dataTimestamp = new Date();
		setChanged();
		notifyObservers(OBSERVER_EVENT_INPUT);
	}

	public IOObject[] getResults() {
		return results;
	}

	public void setResults(IOObject[] results) {
		this.results = results;
		setChanged();
		notifyObservers(OBSERVER_EVENT_RESULTS);
	}

	public void clearMacros() {
		this.macros.clear();
		notifyObservers(OBSERVER_EVENT_MACROS);
	}

	public void setMacro(String key, String value) {
		this.macros.put(key, value);
		notifyObservers(OBSERVER_EVENT_MACROS);
	}

	public String getMacro(String key) {
		return this.macros.get(key);
	}

	public boolean isDownsamplingEnabled() {
		return downsamplingEnabled;
	}

	public void setDownsamplingEnabled(boolean downsamplingEnabled) {
		if (downsamplingEnabled == this.downsamplingEnabled) {
			return;
		}
		dataTimestamp = new Date();
		this.downsamplingEnabled = downsamplingEnabled;
	}

	/**
	 * Will be reset when {@link #template} is first assigned and every time it is re-assigned.
	 * Should be used to determine the folder in which the process is saved when opened in design
	 * perspective.
	 * 
	 * @see #getDataTimestamp()
	 */
	public Date getCreationTimestamp() {
		return creationTimestamp;
	}

	/**
	 * Re-assigned each time {@link #inputData} is assigned. Used to determine name of the input
	 * file when opening in design perspective.
	 * 
	 * @see #getCreationTimestamp()
	 */
	public Date getDataTimestamp() {
		return dataTimestamp;
	}

	public boolean isProcessOpened() {
		return processOpened;
	}

	public void setProcessOpened(boolean processOpened) {
		this.processOpened = processOpened;
	}
}
