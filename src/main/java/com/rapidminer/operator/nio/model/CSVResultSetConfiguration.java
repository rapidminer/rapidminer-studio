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
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.table.TableModel;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.CSVExampleSource;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.io.Encoding;


/**
 * A class holding information about syntactical configuration for parsing CSV files
 *
 * @author Simon Fischer
 */
public class CSVResultSetConfiguration implements DataResultSetFactory {

	public static final String CSV_FILE_LOCATION = "csvFile";
	public static final String CSV_USE_QUOTES = "useQuotes";
	public static final String CSV_HAS_HEADER_ROW = "hasHeaderRow";
	public static final String CSV_HEADER_ROW = "headerRow";
	public static final String CSV_DECIMAL_CHARACTER = "decimalCharacter";
	public static final String CSV_STARTING_ROW = "startingRow";
	public static final String CSV_SKIP_COMMENTS = "skipComments";
	public static final String CSV_COLUMN_SEPARATORS = "columnSeparators";
	public static final String CSV_COMMENT_CHARACTERS = "commentCharacters";
	public static final String CSV_ESCAPE_CHARACTER = "escapeCharacter";
	public static final String CSV_QUOTE_CHARACTER = "quoteCharacter";
	public static final String CSV_TRIM_LINES = "trimLines";
	public static final String CSV_ENCODING = "encoding";
	public static final String CSV_SKIP_UTF_8_BOM = "skipUTF8BOM";
	public static final String CSV_TRIM_VALUES_PARSING = "trimValuesForParsing";
	private String csvFile;

	private boolean skipComments = true;
	private boolean useQuotes = true;
	private boolean skipUTF8BOM = false;
	private boolean trimLines = false;
	private boolean hasHeaderRow = true;
	private boolean trimValuesForParsing = true;

	private String columnSeparators = ";";

	private char quoteCharacter = '"';
	private char escapeCharacter = '\\';
	private char decimalCharacter = '.';
	private String commentCharacters = "#";
	private int startingRow = 0;
	private int headerRow = 0;

	private Charset encoding = Charset.defaultCharset();

	private List<ParsingError> errors;

	/**
	 * This will create a completely empty result set configuration
	 */
	public CSVResultSetConfiguration() {}

	/**
	 * This constructor reads all settings from the parameters of the given operator.
	 */
	public CSVResultSetConfiguration(CSVExampleSource csvExampleSource) throws OperatorException {
		if (csvExampleSource.isFileSpecified()) {
			setCsvFile(csvExampleSource.getSelectedFile().getAbsolutePath());
		}
		setSkipComments(csvExampleSource.getParameterAsBoolean(CSVExampleSource.PARAMETER_SKIP_COMMENTS));
		setUseQuotes(csvExampleSource.getParameterAsBoolean(CSVExampleSource.PARAMETER_USE_QUOTES));
		setStartingRow(csvExampleSource.getParameterAsInt(CSVExampleSource.PARAMETER_STARTING_ROW));
		setTrimLines(csvExampleSource.getParameterAsBoolean(CSVExampleSource.PARAMETER_TRIM_LINES));
		if (csvExampleSource.isParameterSet(CSVExampleSource.PARAMETER_COLUMN_SEPARATORS)) {
			setColumnSeparators(csvExampleSource.getParameterAsString(CSVExampleSource.PARAMETER_COLUMN_SEPARATORS));
		}
		if (csvExampleSource.isParameterSet(CSVExampleSource.PARAMETER_ESCAPE_CHARACTER)) {
			setEscapeCharacter(csvExampleSource.getParameterAsChar(CSVExampleSource.PARAMETER_ESCAPE_CHARACTER));
		}
		if (csvExampleSource.isParameterSet(CSVExampleSource.PARAMETER_COMMENT_CHARS)) {
			setCommentCharacters(csvExampleSource.getParameterAsString(CSVExampleSource.PARAMETER_COMMENT_CHARS));
		}
		if (csvExampleSource.isParameterSet(CSVExampleSource.PARAMETER_QUOTES_CHARACTER)) {
			setQuoteCharacter(csvExampleSource.getParameterAsChar(CSVExampleSource.PARAMETER_QUOTES_CHARACTER));
		}
		encoding = Encoding.getEncoding(csvExampleSource);
		trimValuesForParsing = csvExampleSource.trimForGuessing();
	}

	@Override
	public void setParameters(AbstractDataResultSetReader source) {
		source.setParameter(CSVExampleSource.PARAMETER_CSV_FILE, getCsvFile());
		source.setParameter(CSVExampleSource.PARAMETER_SKIP_COMMENTS, String.valueOf(isSkipComments()));
		source.setParameter(CSVExampleSource.PARAMETER_USE_QUOTES, String.valueOf(isUseQuotes()));
		source.setParameter(CSVExampleSource.PARAMETER_COLUMN_SEPARATORS, getColumnSeparators());
		source.setParameter(CSVExampleSource.PARAMETER_TRIM_LINES, String.valueOf(isTrimLines()));
		source.setParameter(CSVExampleSource.PARAMETER_QUOTES_CHARACTER, String.valueOf(getQuoteCharacter()));
		source.setParameter(CSVExampleSource.PARAMETER_ESCAPE_CHARACTER, String.valueOf(getEscapeCharacter()));
		source.setParameter(CSVExampleSource.PARAMETER_COMMENT_CHARS, getCommentCharacters());

		source.setParameter(Encoding.PARAMETER_ENCODING, encoding.name());
	}

	@Override
	public DataResultSet makeDataResultSet(Operator operator) throws OperatorException {
		return new CSVResultSet(this, operator);
	}

	@Override
	public TableModel makePreviewTableModel(ProgressListener listener) throws OperatorException, ParseException {
		DefaultPreview preview = null;
		try (DataResultSet resultSet = makeDataResultSet(null)) {
			this.errors = ((CSVResultSet) resultSet).getErrors();
			preview = new DefaultPreview(resultSet, listener);
		}
		return preview;
	}

	public void setCsvFile(String csvFile) {
		this.csvFile = csvFile;
	}

	public String getCsvFile() {
		return csvFile;
	}

	public File getCsvFileAsFile() {
		return csvFile == null ? null : new File(csvFile);
	}

	public void setUseQuotes(boolean useQuotes) {
		this.useQuotes = useQuotes;
	}

	public boolean isUseQuotes() {
		return useQuotes;
	}

	public boolean hasHeaderRow() {
		return hasHeaderRow;
	}

	public void setHasHeaderRow(boolean hasHeaderRow) {
		this.hasHeaderRow = hasHeaderRow;
	}

	public void setSkipComments(boolean skipComments) {
		this.skipComments = skipComments;
	}

	public boolean isSkipComments() {
		return skipComments;
	}

	public void setColumnSeparators(String columnSeparators) {
		this.columnSeparators = columnSeparators;
	}

	public String getColumnSeparators() {
		return columnSeparators;
	}

	public void setCommentCharacters(String commentCharacters) {
		this.commentCharacters = commentCharacters;
	}

	public String getCommentCharacters() {
		return commentCharacters;
	}

	public void setEscapeCharacter(char escapeCharacter) {
		this.escapeCharacter = escapeCharacter;
	}

	public char getEscapeCharacter() {
		return escapeCharacter;
	}

	public void setQuoteCharacter(char quoteCharacter) {
		this.quoteCharacter = quoteCharacter;
	}

	public char getQuoteCharacter() {
		return quoteCharacter;
	}

	public void setDecimalCharacter(char decimalCharacter) {
		this.decimalCharacter = decimalCharacter;
	}

	public char getDecimalCharacter() {
		return decimalCharacter;
	}

	public void setTrimLines(boolean trimLines) {
		this.trimLines = trimLines;
	}

	public boolean isTrimLines() {
		return trimLines;
	}

	public void setEncoding(Charset encoding) {
		this.encoding = encoding;
	}

	public Charset getEncoding() {
		return encoding;
	}

	public boolean isSkippingUTF8BOM() {
		return skipUTF8BOM;
	}

	public void setSkipUTF8BOM(boolean skipUTF8BOM) {
		this.skipUTF8BOM = skipUTF8BOM;
	}

	public int getStartingRow() {
		return startingRow;
	}

	public void setStartingRow(int startingRow) {
		this.startingRow = startingRow;
	}

	public int getHeaderRow() {
		return headerRow;
	}

	public void setHeaderRow(int headerRow) {
		this.headerRow = headerRow;
	}

	/**
	 * Whether values should be trimmed for parsing
	 *
	 * @return {@code true} if they should be trimmed
	 * @since 9.2.0
	 */
	public boolean trimValuesForParsing() {
		return trimValuesForParsing;
	}

	/**
	 * Set whether values should be trimmed for parsing
	 *
	 * @param trimValuesForParsing
	 * 		{@code true} if they should be trimmed
	 * @since 9.2.0
	 */
	public void setTrimValuesForParsing(boolean trimValuesForParsing) {
		this.trimValuesForParsing = trimValuesForParsing;
	}

	@Override
	public String getResourceName() {
		return getCsvFile();
	}

	@Override
	public ExampleSetMetaData makeMetaData() {
		return new ExampleSetMetaData();
	}

	public List<ParsingError> getErrors() {
		return errors;
	}

	@Override
	public void close() {}

	/**
	 * @return a map containing all fieldNames and their values
	 */
	public Map<String, String> getParameterMap() {
		Map<String, String> parameterMap = new HashMap<>();
		parameterMap.put(CSV_FILE_LOCATION, getCsvFile());
		parameterMap.put(CSV_USE_QUOTES, String.valueOf(isUseQuotes()));
		parameterMap.put(CSV_HAS_HEADER_ROW, String.valueOf(hasHeaderRow()));
		parameterMap.put(CSV_HEADER_ROW, String.valueOf(getHeaderRow()));
		parameterMap.put(CSV_DECIMAL_CHARACTER, String.valueOf(getDecimalCharacter()));
		parameterMap.put(CSV_STARTING_ROW, String.valueOf(getStartingRow()));
		parameterMap.put(CSV_SKIP_COMMENTS, String.valueOf(isSkipComments()));
		parameterMap.put(CSV_COLUMN_SEPARATORS, getColumnSeparators());
		parameterMap.put(CSV_COMMENT_CHARACTERS, getCommentCharacters());
		parameterMap.put(CSV_ESCAPE_CHARACTER, String.valueOf(getEscapeCharacter()));
		parameterMap.put(CSV_QUOTE_CHARACTER, String.valueOf(getQuoteCharacter()));
		parameterMap.put(CSV_TRIM_LINES, String.valueOf(isTrimLines()));
		parameterMap.put(CSV_ENCODING, String.valueOf(getEncoding()));
		parameterMap.put(CSV_SKIP_UTF_8_BOM, String.valueOf(isSkippingUTF8BOM()));
		parameterMap.put(CSV_TRIM_VALUES_PARSING, String.valueOf(trimValuesForParsing));
		return parameterMap;
	}
}
