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

import java.util.logging.Level;

import javax.swing.text.SimpleAttributeSet;


/**
 * Simple log entry containing an unformatted String and a log level.
 *
 * @author Sabrina Kirstein, Marco Boeck
 */
public class SimpleLogEntry implements LogEntry {

	private static final String NEWLINE = "\n";

	private SimpleAttributeSet attributeSet = new SimpleAttributeSet();
	private String simpleString;
	private Level level;

	/**
	 * Creates a log entry with {@link Level#INFO}.
	 *
	 * @param logEntry
	 */
	public SimpleLogEntry(String logEntry) {
		this(logEntry, Level.INFO);
	}

	/**
	 * Creates a log entry with the specified {@link Level}.
	 *
	 * @param logEntry
	 * @param level
	 */
	public SimpleLogEntry(String logEntry, Level level) {
		if (logEntry == null || "".equals(logEntry.trim())) {
			throw new IllegalArgumentException("logEntry must not be null or empty!");
		}

		this.simpleString = logEntry.endsWith(NEWLINE) ? logEntry : logEntry + NEWLINE;
		this.level = level;
	}

	@Override
	public SimpleAttributeSet getSimpleAttributeSet() {
		return attributeSet;
	}

	@Override
	public String getFormattedString() {
		return simpleString;
	}

	@Override
	public String getInitialString() {
		return simpleString;
	}

	@Override
	public Level getLogLevel() {
		return level;
	}

	@Override
	public boolean isFormatted() {
		return false;
	}

}
