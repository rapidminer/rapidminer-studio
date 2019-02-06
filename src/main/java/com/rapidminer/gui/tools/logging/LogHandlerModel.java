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

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.Icon;


/**
 * Log Model that receives log entries from a {@link Logger} handler. This model is of type
 * {@link LogMode#PUSH}, i.e. the GUI automatically updates on each new entry.
 * <p>
 * Note that the name has to be unique between all log models to differentiate between them!
 * </p>
 * 
 * @author Sabrina Kirstein, Marco Boeck
 */
public class LogHandlerModel extends AbstractPushLogModel {

	private final Handler handler = new Handler() {

		@Override
		public void close() throws SecurityException {}

		@Override
		public void flush() {}

		@Override
		public void publish(final LogRecord record) {
			if (isLoggable(record)) {
				addLogEntry(record);
			}
		}
	};

	/**
	 * Creates a new {@link LogModel} with the specified settings. This model will automatically
	 * push all log records it receives from the specified {@link Logger}.
	 * 
	 * @param logger
	 *            the logger to which this model should register itself as a handler
	 * @param modelIcon
	 *            the icon to be displayed
	 * @param modelName
	 *            the name to be displayed
	 * @param isClosable
	 *            if <code>true</code>, the user can close this log in the view
	 */
	public LogHandlerModel(Logger logger, Icon modelIcon, String modelName, boolean isClosable) {
		super(modelIcon, modelName, isClosable);
		if (logger == null) {
			throw new IllegalArgumentException("logger must not be null!");
		}

		handler.setLevel(Level.ALL);
		logger.addHandler(handler);
	}

	/**
	 * Adds the specified {@link LogRecord} to this model and fires an update.
	 * 
	 * @param record
	 */
	private void addLogEntry(LogRecord record) {
		LogRecordEntry newEntry = new LogRecordEntry(record);
		List<LogEntry> newLogEntries = new LinkedList<>();
		newLogEntries.add(newEntry);
		addLogEntries(newLogEntries);
	}
}
