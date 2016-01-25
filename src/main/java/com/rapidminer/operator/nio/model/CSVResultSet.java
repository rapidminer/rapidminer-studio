/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.operator.nio.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rapidminer.gui.tools.dialogs.wizards.dataimport.csv.LineReader;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.model.ParsingError.ErrorCode;
import com.rapidminer.tools.CSVParseException;
import com.rapidminer.tools.LineParser;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.WebServiceTools;


/**
 *
 * @author Simon Fischer
 *
 */
public class CSVResultSet implements DataResultSet {

	/**
	 * specifies how many rows should be read to guess the column separator, 1 headline + 10 further
	 * rows
	 */
	private static final int LINES_FOR_GUESSING = 11;
	private static final int MAX_LOG_COUNT = 100;
	private CSVResultSetConfiguration configuration;
	private LineReader reader;
	private LineParser parser;

	private String[] next;
	private String[] current;
	private int currentRow;
	private String[] columnNames;
	private int[] valueTypes;
	private int numColumns = 0;
	private Operator operator;
	private final List<ParsingError> errors = new LinkedList<ParsingError>();
	private int logCount = 0;

	public static enum ColumnSplitter {

		SEMI_COLON(";", Pattern.compile(";")),
		COMMA(",", Pattern.compile(",")),
		TAB("\t", Pattern.compile("\t")),
		TILDE("~", Pattern.compile("~")),
		PIPE("|", Pattern.compile("\\|"));

		private final Pattern pattern;
		private final String seperator;

		ColumnSplitter(String seperator, Pattern pattern) {
			this.seperator = seperator;
			this.pattern = pattern;
		}

		public Pattern getPattern() {
			return pattern;
		}

		public String getString() {
			return seperator;
		}

	}

	private static int getSeperatorCount(Pattern seperatorPattern, String content) {
		if (content == null) {
			return 0;
		}
		Matcher matcher = seperatorPattern.matcher(content);
		int count = 0;
		while (matcher.find()) {
			count++;
		}
		return count;
	}

	public CSVResultSet(CSVResultSetConfiguration configuration, Operator operator) throws OperatorException {
		this.configuration = configuration;
		this.operator = operator;
		open();
	}

	private void open() throws OperatorException {
		getErrors().clear();
		close();
		InputStream in = openStream();
		logCount = 0;

		// if encoding is UTF-8, we will have to check whether the stream starts with a BOM. If not
		// restart stream

		if (configuration.getEncoding().name().equals("UTF-8")) {
			try {
				if (in.read() != 239 || in.read() != 187 || in.read() != 191) {
					in.close();
					in = openStream();
				}
			} catch (IOException e) {
				try {
					in.close();
				} catch (IOException e1) {
				}
				throw new UserError(operator, e, 321, configuration.getCsvFile(), e.toString());
			}
		}

		reader = new LineReader(in, configuration.getEncoding());
		parser = new LineParser(configuration);

		try {
			readNext();
		} catch (IOException e) {
			try {
				in.close();
			} catch (IOException e1) {
			}
			throw new UserError(operator, e, 321, configuration.getCsvFile(), e.toString());
		}
		if (next == null) {
			errors.add(new ParsingError(1, -1, ErrorCode.FILE_SYNTAX_ERROR, "No valid line found."));
			// throw new UserError(operator, 321, configuration.getCsvFile(),
			// "No valid line found.");
			columnNames = new String[0];
			valueTypes = new int[0];
		} else {
			columnNames = new String[next.length];
			for (int i = 0; i < next.length; i++) {
				columnNames[i] = "att" + (i + 1);
			}
			valueTypes = new int[next.length];
			Arrays.fill(valueTypes, Ontology.NOMINAL);
			currentRow = -1;
		}
	}

	public static String guessColumnSeperator(String csvFile) {
		return guessColumnSplitter(csvFile).getString();
	}

	/**
	 * Guesses the column splitter of the csv file by counting which {@link ColumnSplitter} appears
	 * the most in the first rows.
	 *
	 * @param csvFile
	 *            the file to analyze
	 * @return the most frequent {@link ColumnSplitter}
	 */
	public static ColumnSplitter guessColumnSplitter(String csvFile) {
		try (LineReader tempReader = new LineReader(new File(csvFile), StandardCharsets.UTF_8)) {

			/* could be default, apply heuristics to find the column splitter */
			HashMap<ColumnSplitter, Integer> splitterValues = new HashMap<>();
			for (ColumnSplitter splitter : ColumnSplitter.values()) {
				splitterValues.put(splitter, 0);
			}

			int lineCount = 0;

			while (lineCount < LINES_FOR_GUESSING) {
				String line = tempReader.readLine();
				// SEMI_COLON,
				splitterValues.put(ColumnSplitter.SEMI_COLON, splitterValues.get(ColumnSplitter.SEMI_COLON)
						+ getSeperatorCount(ColumnSplitter.SEMI_COLON.getPattern(), line));
				// COMMA,
				splitterValues.put(ColumnSplitter.COMMA, splitterValues.get(ColumnSplitter.COMMA)
						+ getSeperatorCount(ColumnSplitter.COMMA.getPattern(), line));
				// TAB,
				splitterValues.put(ColumnSplitter.TAB,
						splitterValues.get(ColumnSplitter.TAB) + getSeperatorCount(ColumnSplitter.TAB.getPattern(), line));
				// TILDE,
				splitterValues.put(ColumnSplitter.TILDE, splitterValues.get(ColumnSplitter.TILDE)
						+ getSeperatorCount(ColumnSplitter.TILDE.getPattern(), line));
				// PIPE
				splitterValues.put(ColumnSplitter.PIPE,
						splitterValues.get(ColumnSplitter.PIPE) + getSeperatorCount(ColumnSplitter.PIPE.getPattern(), line));

				lineCount++;
			}

			int maxValue = 0;
			ColumnSplitter guessedSplitter = ColumnSplitter.SEMI_COLON;

			for (ColumnSplitter splitter : ColumnSplitter.values()) {
				if (splitterValues.get(splitter) > maxValue) {
					maxValue = splitterValues.get(splitter);
					guessedSplitter = splitter;
				}
			}

			return guessedSplitter;

		} catch (IOException e) {
			return ColumnSplitter.SEMI_COLON;
		}
	}

	protected InputStream openStream() throws UserError {
		try {
			URL url = new URL(configuration.getCsvFile());
			try {
				return WebServiceTools.openStreamFromURL(url);
			} catch (IOException e) {
				throw new UserError(operator, 301, e, configuration.getCsvFile());
			}
		} catch (MalformedURLException e) {
			// URL did not work? Try as file...
			try {
				String csvFile = configuration.getCsvFile();
				if (csvFile == null) {
					throw new UserError(this.operator, "file_consumer.no_file_defined");
				}

				return new FileInputStream(csvFile);
			} catch (FileNotFoundException e1) {
				throw new UserError(operator, 301, e1, configuration.getCsvFile());
			}
		}
	}

	private void readNext() throws IOException {
		do {
			String line = reader.readLine();
			if (line == null) {
				next = null;
				return;
			}
			try {
				next = parser.parse(line);
				if (next != null) { // no comment read
					break;
				}
			} catch (CSVParseException e) {
				ParsingError parsingError = new ParsingError(currentRow, -1, ErrorCode.FILE_SYNTAX_ERROR, line, e);
				getErrors().add(parsingError);
				String warning = "Could not parse line " + currentRow + " in input: " + e.toString();
				if (logCount < MAX_LOG_COUNT) {
					if (operator != null) {
						operator.logWarning(warning);
					} else {
						LogService.getRoot().warning(warning);
					}
				} else {
					if (logCount == MAX_LOG_COUNT) {
						if (operator != null) {
							operator.logWarning("Maximum number of warnings exceeded. Will display no further warnings.");
						} else {
							LogService.getRoot()
									.warning("Maximum number of warnings exceeded. Will display no further warnings.");
						}
					}
				}
				logCount++;
				next = new String[] { line };
			}
		} while (true);
		numColumns = Math.max(numColumns, next.length);
	}

	@Override
	public boolean hasNext() {
		return next != null;
	}

	@Override
	public void next(ProgressListener listener) throws OperatorException {
		current = next;
		currentRow++;
		try {
			readNext();
		} catch (IOException e) {
			throw new UserError(operator, e, 321, configuration.getCsvFile(), e.toString());
		}
	}

	@Override
	public int getNumberOfColumns() {
		if (current != null) {
			return current.length;
		} else {
			return numColumns;
		}
	}

	@Override
	public String[] getColumnNames() {
		return columnNames;
	}

	@Override
	public boolean isMissing(int columnIndex) {
		return columnIndex >= current.length || current[columnIndex] == null || current[columnIndex].isEmpty();
	}

	@Override
	public Number getNumber(int columnIndex) throws ParseException {
		throw new ParseException(
				new ParsingError(currentRow, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_REAL, current[columnIndex]));
	}

	@Override
	public String getString(int columnIndex) throws ParseException {
		if (columnIndex < current.length) {
			return current[columnIndex];
		} else {
			return null;
		}
	}

	@Override
	public Date getDate(int columnIndex) throws ParseException {
		throw new ParseException(
				new ParsingError(currentRow, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_DATE, current[columnIndex]));
	}

	@Override
	public ValueType getNativeValueType(int columnIndex) throws ParseException {
		return ValueType.STRING;
	}

	@Override
	public void close() throws OperatorException {
		if (reader == null) {
			return;
		}
		try {
			reader.close();
		} catch (IOException e) {
			throw new UserError(operator, 321, e, configuration.getCsvFile(), e.toString());
		} finally {
			reader = null;
		}
	}

	@Override
	public void reset(ProgressListener listener) throws OperatorException {
		open();
	}

	@Override
	public int[] getValueTypes() {
		return valueTypes;
	}

	@Override
	public int getCurrentRow() {
		return currentRow;
	}

	public List<ParsingError> getErrors() {
		return errors;
	}
}
