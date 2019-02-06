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

import java.awt.Color;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import com.rapidminer.gui.tools.SwingTools;


/**
 * Represents a {@link LogEntry} for a {@link LogModel} which is created from a {@link LogRecord}.
 *
 *
 * @author Sabrina Kirstein, Marco Boeck
 *
 */
public class LogRecordEntry implements LogEntry {

	/** log level of FINE and lower */
	private static final Color COLOR_DEFAULT;

	/** log level of WARNING and higher */
	private static final Color COLOR_WARNING;

	/** log level of SEVERE */
	private static final Color COLOR_ERROR;

	/** log level of INFO and higher */
	private static final Color COLOR_INFO;

	static {
		COLOR_DEFAULT = Color.GRAY;
		COLOR_INFO = Color.BLACK;
		COLOR_WARNING = SwingTools.RAPIDMINER_LIGHT_ORANGE;
		COLOR_ERROR = Color.RED;
	}

	private SimpleAttributeSet simpleAttributeSet;
	private String formattedString;
	private String initialString;
	private Level logLevel;

	private static final Formatter formatter = new SimpleFormatter() {

		@Override
		public String format(LogRecord record) {
			StringBuilder b = new StringBuilder();
			b.append(DateFormat.getDateTimeInstance().format(new Date(record.getMillis())));
			b.append(" ");
			b.append(record.getLevel().getLocalizedName());
			b.append(": ");
			b.append(formatMessage(record));
			b.append("\n");
			return b.toString();
		}

	};

	/**
	 * Creates a new {@link LogRecordEntry} which automatically formats the given {@link LogRecord}
	 * with the RapidMiner Studio log styling and default logging format.
	 *
	 * @param logRecord
	 */
	public LogRecordEntry(LogRecord logRecord) {

		logLevel = logRecord.getLevel();
		initialString = logRecord.getMessage();

		simpleAttributeSet = new SimpleAttributeSet();
		if (logRecord.getLevel().intValue() >= Level.SEVERE.intValue()) {
			StyleConstants.setForeground(simpleAttributeSet, COLOR_ERROR);
			StyleConstants.setBold(simpleAttributeSet, true);
		} else if (logRecord.getLevel().intValue() >= Level.WARNING.intValue()) {
			StyleConstants.setForeground(simpleAttributeSet, COLOR_WARNING);
			StyleConstants.setBold(simpleAttributeSet, true);
		} else if (logRecord.getLevel().intValue() >= Level.INFO.intValue()) {
			StyleConstants.setForeground(simpleAttributeSet, COLOR_INFO);
			StyleConstants.setBold(simpleAttributeSet, false);
		} else {
			StyleConstants.setForeground(simpleAttributeSet, COLOR_DEFAULT);
			StyleConstants.setBold(simpleAttributeSet, false);
		}

		formattedString = formatter.format(logRecord);
	}

	@Override
	public SimpleAttributeSet getSimpleAttributeSet() {
		return simpleAttributeSet;
	}

	@Override
	public String getFormattedString() {
		return formattedString;
	}

	@Override
	public String getInitialString() {
		return initialString;
	}

	@Override
	public Level getLogLevel() {
		return logLevel;
	}

	@Override
	public boolean isFormatted() {
		return true;
	}
}
