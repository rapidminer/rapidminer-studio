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
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rapidminer.gui.tools.dialogs.wizards.dataimport.csv.LineReader;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.model.ParsingError.ErrorCode;
import com.rapidminer.tools.CSVParseException;
import com.rapidminer.tools.LineParser;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.WebServiceTools;


/**
 * @author Simon Fischer
 */
public class CSVResultSet implements DataResultSet {

	/**
	 * specifies how many rows should be read to guess the column separator, 1 headline + 99 further rows
	 */
	public static final int LINES_FOR_GUESSING = 100;
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
	private final List<ParsingError> errors = new LinkedList<>();
	private int logCount = 0;
	private long multiplier;
	private long lineCounter = 0;

	public enum ColumnSplitter {

		SEMI_COLON(";", Pattern.compile(";")), COMMA(",", Pattern.compile(",")), TAB("\t", Pattern.compile("\t")), TILDE("~",
				Pattern.compile("~")), PIPE("|", Pattern.compile("\\|"));

		private final Pattern pattern;
		private final String separator;

		ColumnSplitter(String separator, Pattern pattern) {
			this.separator = separator;
			this.pattern = pattern;
		}

		public Pattern getPattern() {
			return pattern;
		}

		public String getString() {
			return separator;
		}

	}

	public enum TextQualifier {
		DOUBLE_QUOTES("\"", Pattern.compile("\"")),
		SINGLE_QUOTES("'", Pattern.compile("'"));

		private final Pattern pattern;
		private final String qualifier;


		TextQualifier(String qualifier, Pattern pattern) {
			this.pattern = pattern;
			this.qualifier = qualifier;
		}

		public Pattern getPattern() {
			return pattern;
		}

		public String getString() {
			return qualifier;
		}
	}

	public enum DecimalCharacter {

		COMMA(",", Pattern.compile(",")),
		PERIOD(".", Pattern.compile("\\."));

		private final Pattern pattern;
		private final String character;

		public Pattern getPattern() {
			return pattern;
		}

		public String getString() {
			return character;
		}

		DecimalCharacter(String character, Pattern pattern) {
			this.character = character;
			this.pattern = pattern;
		}

	}

	private static int getTokenCount(Pattern tokenPattern, String content) {
		if (content == null) {
			return 0;
		}
		Matcher matcher = tokenPattern.matcher(content);
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

		if (operator == null) {
			init();
		}

		InputStream in = openStream();
		logCount = 0;

		in = cleanInputStream(in);

		reader = new LineReader(in, configuration.getEncoding());
		parser = new LineParser(configuration);

		try {
			if (operator != null && reader.getSize() > 0L) {
				multiplier = reader.getSize() / 100L;
				lineCounter = 0;
				operator.getProgress().setCheckForStop(false);
				operator.getProgress().setTotal(100);
			}
		} catch (IOException e) {
			// ignore and assume indeterminate progress
		}

		try {
			int startingRow = configuration.getStartingRow();

			do {
				// if we read via operator, skip until we reach the starting row
				// if starting row equals 1 (aka start from the first row), this just prepares the column meta data
				readNext();
			} while (operator != null && --startingRow > 0);
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
			numColumns = 0;
			currentRow = 0;
		} else if (operator != null) {
			initConfiguration(next);
		}
	}

	/**
	 * Initialize the settings for numColumns, columnNames and valueTypes from the configuration. Uses {@link CSVResultSet#openStream()}
	 * to get the {@link InputStream}.
	 *
	 * @throws OperatorException
	 */
	private void init() throws OperatorException {
		InputStream in = openStream();
		in = cleanInputStream(in);
		final LineReader lineReader = new LineReader(in, configuration.getEncoding());
		final LineParser lineParser = new LineParser(configuration);
		String[] strings;
		try {
			int initFromRow = configuration.hasHeaderRow() ? configuration.getHeaderRow() : configuration.getStartingRow();
			do {
				// skip until we reach the starting row
				strings = readNext(lineReader, lineParser);
			} while (initFromRow-- > 0);
		} catch (IOException e) {
			try {
				in.close();
			} catch (IOException e1) {
			}
			throw new UserError(operator, e, 321, configuration.getCsvFile(), e.toString());
		}
		if (strings != null) {
			initConfiguration(strings);
		} else {
			columnNames = new String[0];
			valueTypes = new int[0];
		}
	}

	/**
	 * Set the configuration about columns using the given array of {@link String Strings}
	 *
	 * @param strings
	 * 		usually gets the parsed headerRow to set up internal data structures
	 * @since 9.1
	 */
	private void initConfiguration(String[] strings) {
		numColumns = strings.length;
		columnNames = new String[strings.length];
		for (int i = 0; i < strings.length; i++) {
			columnNames[i] = "att" + (i + 1);
		}
		valueTypes = new int[strings.length];
		Arrays.fill(valueTypes, Ontology.NOMINAL);
		currentRow = -1;
	}

	/**
	 * If the configuration's encoding is UTF-8, we will have to check whether the stream starts with a BOM. If not it
	 * will be restarted once.
	 *
	 * @param in
	 * 		the {@link InputStream} that needs to be checked
	 * @return the original or a restarted stream using openStream
	 * @throws UserError
	 * 		in case there was a problem reading or restarting the {@link InputStream}
	 * @since 9.1
	 */
	private InputStream cleanInputStream(InputStream in) throws UserError {
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
		return in;
	}

	/**
	 * Guesses the column separator of the csv file by counting which {@link ColumnSplitter} appears
	 * the most in the first rows.
	 *
	 * @param csvFile
	 * 		the csv file
	 * @return the most frequent column separator
	 */
	public static String guessColumnSeperator(File csvFile) {
		return guessColumnSplitter(csvFile).getString();
	}

	/**
	 * Guesses the column separator of the csv file by counting which {@link ColumnSplitter} appears
	 * the most in the first rows.
	 *
	 * @param csvFile
	 * 		the path to the file to analyze
	 * @return the most frequent column separator
	 */
	public static String guessColumnSeperator(String csvFile) {
		return guessColumnSplitter(csvFile).getString();
	}

	/**
	 * Guesses the column splitter of the csv file by counting which {@link ColumnSplitter} appears
	 * the most in the first rows.
	 *
	 * @param csvFile
	 * 		the path to the file to analyze
	 * @return the most frequent {@link ColumnSplitter}
	 */
	public static ColumnSplitter guessColumnSplitter(String csvFile) {
		return guessColumnSplitter(new File(csvFile));
	}

	/**
	 * Guesses the column splitter of the csv file by counting which {@link ColumnSplitter} appears
	 * the most in the first rows.
	 *
	 * @param csvFile
	 * 		the file to analyze
	 * @return the most frequent {@link ColumnSplitter}
	 */
	public static ColumnSplitter guessColumnSplitter(File csvFile) {
		try (LineReader tempReader = new LineReader(csvFile, StandardCharsets.UTF_8)) {

			/* could be default, apply heuristics to find the column splitter */
			Map<ColumnSplitter, Integer> splitterValues = new HashMap<>();
			for (ColumnSplitter splitter : ColumnSplitter.values()) {
				splitterValues.put(splitter, 0);
			}

			int lineCount = 0;

			while (lineCount < LINES_FOR_GUESSING) {
				String line = tempReader.readLine();

				for (ColumnSplitter splitter : ColumnSplitter.values()) {
					splitterValues.put(splitter, splitterValues.get(splitter)
							+ getTokenCount(splitter.getPattern(), line));
				}

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
			LogService.getRoot().log(Level.WARNING, "Problem reading the file.", e);
			return ColumnSplitter.SEMI_COLON;
		}
	}


	/**
	 * Guesses the Text Qualifier (quotes) being used by counting which one has the most pairs
	 *
	 * @param csvFile
	 * 		the path to the file to analyze
	 * @return the most frequent {@link ColumnSplitter}
	 * @since 9.0.0
	 */
	public static TextQualifier guessTextQualifier(String csvFile) {
		return guessTextQualifier(new File(csvFile));
	}

	/**
	 * Guesses the {@link TextQualifier} (quotes) being used by counting which one has the most pairs
	 *
	 * @param csvFile
	 * @return the most frequent {@link TextQualifier}
	 * @since 9.0.0
	 */
	public static TextQualifier guessTextQualifier(File csvFile) {

		try (LineReader tempReader = new LineReader(csvFile, StandardCharsets.UTF_8)) {
			//Set Default
			TextQualifier guessedQualifier = TextQualifier.DOUBLE_QUOTES;

			Map<TextQualifier, Integer> qualifierValues = new HashMap<>();
			for (TextQualifier splitter : TextQualifier.values()) {
				qualifierValues.put(splitter, 0);
			}

			int lineCount = 0;

			while (lineCount < LINES_FOR_GUESSING) {
				String line = tempReader.readLine();

				for (TextQualifier tq : TextQualifier.values()) {
					qualifierValues.put(tq, qualifierValues.get(tq) + getTokenCount(tq.getPattern(), line));
				}

				lineCount++;
			}


			int maxValue = 0;
			for (TextQualifier qualifier : TextQualifier.values()) {
				int numberOfOccurrences = qualifierValues.get(qualifier);
				if (numberOfOccurrences > maxValue && numberOfOccurrences % 2 == 0) {
					maxValue = numberOfOccurrences;
					guessedQualifier = qualifier;
				}
			}


			return guessedQualifier;
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "Problem reading the file.", e);
			return TextQualifier.DOUBLE_QUOTES;
		}
	}

	public static DecimalCharacter guessDecimalSeparator(String csvFile) {
		return guessDecimalSeparator(new File(csvFile));
	}

	public static DecimalCharacter guessDecimalSeparator(File csvFile) {
		try (LineReader tempReader = new LineReader(csvFile, StandardCharsets.UTF_8)) {
			//Set Default
			DecimalCharacter guessedCharacter = DecimalCharacter.PERIOD;

			Map<DecimalCharacter, Integer> decimalCharacterValues = new HashMap<>();
			for (DecimalCharacter splitter : DecimalCharacter.values()) {
				decimalCharacterValues.put(splitter, 0);
			}

			int lineCount = 0;

			while (lineCount < LINES_FOR_GUESSING) {
				String line = tempReader.readLine();

				for (DecimalCharacter sep : DecimalCharacter.values()) {
					decimalCharacterValues.put(sep, decimalCharacterValues.get(sep) + getTokenCount(sep.getPattern(), line));
				}

				lineCount++;
			}

			int maxValue = 0;
			for (DecimalCharacter character : DecimalCharacter.values()) {
				int numberOfOccurrences = decimalCharacterValues.get(character);
				if (numberOfOccurrences > maxValue) {
					maxValue = numberOfOccurrences;
					guessedCharacter = character;
				}
			}

			return guessedCharacter;
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "Problem reading the file.", e);
			return DecimalCharacter.PERIOD;
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
			String line = reader == null ? null : reader.readLine();
			if (line == null) {
				next = null;
				return;
			}
			try {
				next = parser.parse(line);
				if (operator != null && ++lineCounter % 1000 == 0) {
					long position = reader == null ? -1L : reader.getPosition();
					if (position > 0) {
						int currentProgress = (int) (position / multiplier);
						if (currentProgress != operator.getProgress().getCompleted()) {
							try {
								operator.getProgress().setCompleted(currentProgress);
							} catch (ProcessStoppedException e) {
								// Will not happen, because check for stop is deactivated.
							}
						}
					}
				}
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
				next = new String[]{line};
			}
		} while (true);
	}

	/**
	 * Gets the next rows parts using the given {@link LineParser}
	 *
	 * @param lineReader
	 * 		to be used for reading the next row
	 * @param lineParser
	 * 		to be used to parse the row
	 * @return the output of the parser or an array with one entry being the whole rows content. Can be {@code null} if the lineReader returned {@code null}.
	 * @throws IOException
	 * 		in case the lineReader got an error
	 * @since 9.1
	 */
	private static String[] readNext(LineReader lineReader, LineParser lineParser) throws IOException {
		String[] myNext;
		do {
			String line = lineReader == null ? null : lineReader.readLine();
			if (line == null) {
				return null;
			}
			try {
				myNext = lineParser.parse(line);
				if (myNext != null) { // no comment read
					break;
				}
			} catch (CSVParseException e) {
				continue;
			}
		} while (true);
		return myNext;
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
		return numColumns;
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
