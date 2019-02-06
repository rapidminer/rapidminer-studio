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

import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;


/**
 * Interface for a log entry inside a {@link LogModel}. Each entry consists of styling information,
 * the log level and the formatted and unformatted log message.
 *
 * @author Sabrina Kirstein, Marco Boeck
 */
public interface LogEntry {

	/**
	 * The {@link SimpleAttributeSet} used by swing {@link Document}s to style a portion of the
	 * text. See {@link LogRecordEntry} for an example.
	 *
	 * @return the simple attribute set with formats or <code>null</code> if no styling is required
	 */
	public SimpleAttributeSet getSimpleAttributeSet();

	/**
	 * Return the formatted log string. This string is expected to be styled in the desired way and
	 * will be shown as-is in the GUI. See
	 * {@link java.util.logging.Formatter#format(java.util.logging.LogRecord)} for an example on how
	 * to do this.
	 *
	 * @return the formatted string
	 */
	public String getFormattedString();

	/**
	 * Return the unformatted plaintext string without any formatting.
	 *
	 * @return the initial unformatted string
	 */
	public String getInitialString();

	/**
	 * Return the log {@link Level} for this entry or <code>null</code>. The level controls if the
	 * entry is displayed in the GUI or not, depending on the selected log level for the
	 * {@link LogModel}. If the level is null, the entry is always displayed.
	 *
	 * @return a log level or null
	 */
	public Level getLogLevel();

	/**
	 * Returns whether this log entry makes use of formatting. When no formatting is used, the
	 * {@link LogViewer} can make use of performance optimizations.
	 *
	 * @return <code>true</code> if Swing text formatting is used; <code>false</code> otherwise.
	 */
	public boolean isFormatted();
}
