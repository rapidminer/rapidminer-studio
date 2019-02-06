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
package com.rapidminer.example.table;

import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.regex.Pattern;


/**
 * A simple line converter for reading data from BufferedReaders. Each line is separated into
 * columns using a pattern matcher based on regular expressions. In addition, comments might be also
 * defined. Everything after a comment character is completely ignored.
 * 
 * Quotes might also be used. If a columns starts with a quote (&quot;) the end of the quoted region
 * is searched and the corresponding columns build a new column which replaces the old ones. Quoting
 * is added for compatibility reasons only. Since parsing is slower if quoting is used, quotes
 * should not be used at all. If possible please use and define a column separator which is not part
 * of your data.
 * 
 * @author Ingo Mierswa
 */
public class RapidMinerLineReader {

	/** A regular expression pattern which is used for splitting the columns. */
	private Pattern separatorPattern;

	/** The possible character for comment lines. */
	private String[] commentChars = null;

	/** Indicates if quotes should be regarded (slower!). */
	private boolean useQuotes = false;

	/** The character used for quoting. */
	private char quoteChar = '"';

	/** The character used for escaping the quotes. */
	private char escapeChar = '\\';

	/** Indicates if lines should be trimmed before they are splitted into columns. */
	private boolean trimLines = false;

	/** The current line number. */
	private int lineNumber = 1;

	/** Indicates if error lines should be skipped or if an error should occur. */
	private boolean skipErrorLines;

	/** Indicates if quoting (&quot;) can be used to form. */
	public RapidMinerLineReader(String separatorsRegExpr, char[] commentChars, boolean useQuotes, char quoteChar,
			char escapeChar, boolean trimLines, boolean skipErrorLines) {
		this.separatorPattern = Pattern.compile(separatorsRegExpr);
		if (commentChars != null) {
			this.commentChars = new String[commentChars.length];
			for (int i = 0; i < commentChars.length; i++) {
				this.commentChars[i] = Character.toString(commentChars[i]);
			}
		}
		this.useQuotes = useQuotes;
		this.quoteChar = quoteChar;
		this.escapeChar = escapeChar;
		this.trimLines = trimLines;
		this.skipErrorLines = skipErrorLines;
	}

	/**
	 * Ignores comment and empty lines and returns the first line not starting with a comment.
	 * Returns null if no such line exists. Throws an IOException if the line does not provide the
	 * given expected number of columns. This check will not be performed if the given parameter
	 * value is -1.
	 */
	public String[] readLine(BufferedReader in, int expectedNumberOfColumns) throws IOException {
		String line = null;
		while (line == null) {
			line = in.readLine();
			if (line == null) {
				break; // eof
			}

			if (trimLines) {
				line = line.trim();
			}

			// check for comments
			if (commentChars != null) {
				for (int c = 0; c < commentChars.length; c++) {
					if (line.indexOf(commentChars[c]) >= 0) {
						line = line.substring(0, line.indexOf(commentChars[c]));
					}
				}
			}
			// comment or empty line --> next line
			if (line.trim().length() == 0) {
				line = null;
			}
		}
		if (line == null) {
			return null;
		}

		String[] columns = null;
		if (useQuotes) {
			columns = Tools.quotedSplit(line, separatorPattern, quoteChar, escapeChar);
		} else {
			columns = separatorPattern.split(line, -1);
		}
		if (expectedNumberOfColumns != -1) {
			if (columns.length < expectedNumberOfColumns) {
				if (skipErrorLines) {
					// LogService.getGlobal().log("Possible data format error: line " + lineNumber +
					// " did not provide the expected number of columns (was: " + columns.length +
					// ", expected: " + expectedNumberOfColumns +
					// "), skip line...", LogService.WARNING);
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.example.table.RapidMinerLineReader.possible_data_format_error",
							new Object[] { lineNumber, columns.length, expectedNumberOfColumns });
					return readLine(in, expectedNumberOfColumns);
				} else {
					throw new IOException("Data format error in line " + lineNumber
							+ ": the line does not provide the expected number of columns (was: " + columns.length
							+ ", expected: " + expectedNumberOfColumns + ")! Stop reading...");
				}
			} else if (columns.length > expectedNumberOfColumns) {
				// only a warning since this might be desired if the data should
				// be loaded only partially
				// LogService.getGlobal().log("Possible data format error: line " + lineNumber +
				// " did not provide the expected number of columns (was: " + columns.length +
				// ", expected: " + expectedNumberOfColumns + ")!",
				// LogService.WARNING);
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.example.table.RapidMinerLineReader.possible_data_format_error",
						new Object[] { lineNumber, columns.length, expectedNumberOfColumns });
			}
		}
		lineNumber++;
		return columns;
	}
}
