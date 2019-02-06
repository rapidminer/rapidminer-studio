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
package com.rapidminer.gui.tools.logging;

import javax.swing.Icon;


/**
 * This is the abstract model of a {@link LogMode#PULL} log in the {@link LogViewer}.
 *
 * @author Marco Boeck
 *
 */
public abstract class AbstractPullLogModel extends AbstractLogModel {

	/**
	 * Creates a new log model with max {@value AbstractLogModel#DEFAULT_MAX_LOG_ENTRIES} log
	 * entries. If the size is exceeded old entries will be overwritten.
	 *
	 * @param modelIcon
	 *            can be <code>null</code>. If not <code>null</code>, must be 16x16 pixel
	 * @param modelName
	 *            cannot be <code>null</code> or empty. Must not exceed
	 *            {@link LogModel#MAX_NAME_LENGTH} characters
	 * @param logMode
	 *            see {@link LogMode#PULL} and {@link LogMode#PUSH}
	 * @param isClosable
	 *            if <code>true</code>, the user can close the log via a button in the GUI
	 */
	public AbstractPullLogModel(Icon modelIcon, String modelName, boolean isClosable) {
		super(modelIcon, modelName, LogMode.PULL, isClosable);
	}

	/**
	 * Creates a new log model with the defined size of log entries. If the size is exceeded old
	 * entries will be overwritten.
	 *
	 * @param modelIcon
	 *            can be <code>null</code>. If not <code>null</code>, must be 16x16 pixel
	 * @param modelName
	 *            cannot be <code>null</code> or empty. Must not exceed
	 *            {@link LogModel#MAX_NAME_LENGTH} characters
	 * @param logMode
	 *            see {@link LogMode#PULL} and {@link LogMode#PUSH}
	 * @param isClosable
	 *            if <code>true</code>, the user can close the log via a button in the GUI
	 * @param maxLogEntries
	 *            the maximum size of log entries
	 */
	public AbstractPullLogModel(Icon modelIcon, String modelName, boolean isClosable, int maxLogEntries) {
		super(modelIcon, modelName, LogMode.PULL, isClosable, maxLogEntries);
	}

}
