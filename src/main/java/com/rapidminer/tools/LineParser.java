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
package com.rapidminer.tools;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.model.CSVResultSetConfiguration;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 * @author Tobias Malbrecht, Marco Boeck, Nils Woehler
 */
public class LineParser {

	private enum SplitMachineState {
		NEW_SPLIT, START_WHITESPACE, WRITE_NOT_QUOTE, QUOTE_OPENED, QUOTE_CLOSED, WRITE_QUOTE, ERROR, END_OF_LINE
	}

	public static final String DEFAULT_COMMENT_CHARACTER_STRING = "#";
	public static final String DEFAULT_SPLIT_EXPRESSION = ",\\s*|;\\s*";
	public static final String SPLIT_BY_TAB_EXPRESSION = "\t";
	public static final String SPLIT_BY_SPACE_EXPRESSION = "\\s";
	public static final String SPLIT_BY_COMMA_EXPRESSION = ",";
	public static final String SPLIT_BY_SEMICOLON_EXPRESSION = ";";
	public static final char DEFAULT_QUOTE_CHARACTER = '"';
	public static final char DEFAULT_QUOTE_ESCAPE_CHARACTER = '\\';
	private static final char NONE = 0;

	private Charset encoding;
	private boolean skipComments = true;
	private String commentCharacterString = DEFAULT_COMMENT_CHARACTER_STRING;
	private Pattern splitPattern = Pattern.compile(DEFAULT_SPLIT_EXPRESSION);
	private boolean useQuotes = true;
	private char quoteCharacter = DEFAULT_QUOTE_CHARACTER;
	private char quoteEscapeCharacter = DEFAULT_QUOTE_ESCAPE_CHARACTER;
	private boolean trimLine = true;

	public LineParser() {}

	public LineParser(CSVResultSetConfiguration configuration) throws OperatorException {
		this();
		configure(configuration);
	}

	public void setEncoding(Charset encoding) {
		this.encoding = encoding;
	}

	public Charset getEncoding() {
		return encoding;
	}

	public boolean isTrimLine() {
		return trimLine;
	}

	public boolean isSkipComments() {
		return skipComments;
	}

	public String getSplitExpression() {
		return splitPattern.toString();
	}

	public String getCommentCharacters() {
		return commentCharacterString;
	}

	public boolean isUseQuotes() {
		return useQuotes;
	}

	public char getQuoteCharacter() {
		return quoteCharacter;
	}

	public char getQuoteEscapeCharacter() {
		return quoteEscapeCharacter;
	}

	public void setTrimLine(boolean trimLine) {
		this.trimLine = trimLine;
	}

	public void setSkipComments(boolean skipComments) {
		this.skipComments = skipComments;
	}

	public void setCommentCharacters(String commentCharacters) {
		this.commentCharacterString = commentCharacters;
	}

	public void setSplitExpression(String splitExpression) throws OperatorException {
		try {
			if (splitExpression == null) {
				splitExpression = "";
			}
			this.splitPattern = Pattern.compile(splitExpression);
		} catch (PatternSyntaxException e) {
			throw new OperatorException("Malformed split expression: " + splitExpression);
		}
	}

	public void setUseQuotes(boolean useQuotes) {
		this.useQuotes = useQuotes;
	}

	public void setQuoteCharacter(char quoteCharacter) {
		this.quoteCharacter = quoteCharacter;
	}

	public void setQuoteEscapeCharacter(char quoteEscapeCharacter) {
		this.quoteEscapeCharacter = quoteEscapeCharacter;
	}

	public String[] parse(String line) throws CSVParseException {
		line = removeComment(line);
		if (line == null || "".equals(line.trim())) {
			return null;
		}
		return split(line);
	}

	public String removeComment(String line) {
		String resultingLine = line;
		if (skipComments) {
			for (int i = 0; i < commentCharacterString.length(); i++) {
				int commentCharacterIndex = line.indexOf(commentCharacterString.charAt(i));
				if (commentCharacterIndex >= 0) {
					resultingLine = line.substring(0, commentCharacterIndex);
					if (line.trim().length() == 0) {
						return null;
					}
				}
			}
		}
		return resultingLine;
	}

	public String[] split(String line) throws CSVParseException {
		if (splitPattern == null) {
			return new String[] { line };
		}
		// if (useQuotes) {
		if (splitPattern.toString().length() > 1) {
			return split(line, splitPattern, trimLine, useQuotes ? quoteCharacter : NONE, quoteEscapeCharacter);
		} else if (splitPattern.toString().length() == 1) {
			return fastSplit(line, splitPattern.toString().charAt(0), trimLine, useQuotes ? quoteCharacter : NONE,
					quoteEscapeCharacter);
		} else {
			return new String[] { line };
		}
		// } else {
		// // return fastSplit(line, splitPattern.toString().charAt(0), trimLine, quoteCharacter,
		// quoteEscapeCharacter);
		// return split(line, splitPattern, trimLine);
		// }
	}

	public static String[] split(String line, Pattern splitPattern, boolean trimLine) throws CSVParseException {
		String[] splittedString = splitPattern.split(trimLine ? line.trim() : line);
		return splittedString;
	}

	public static String[] split(String line, Pattern splitPattern, boolean trimLine, char quoteCharacter,
			char quoteEscapeCharacter) throws CSVParseException {
		// String s = Tools.escapeQuoteCharsInQuotes(trimLine ? line.trim() : line, splitPattern,
		// quoteCharacter, quoteEscapeCharacter, true);
		String s = line;
		return Tools.quotedSplit(trimLine ? s.trim() : s, splitPattern, quoteCharacter, quoteEscapeCharacter);
	}

	/**
	 * Splits the given line at each split character which is not in quotes and not following an
	 * escape character.
	 * 
	 * @param line
	 *            the string to be splitted
	 * @param splitChar
	 *            the character which seperates the values, e.g. for the line
	 *            {@code value1;"30.545";value3} it would be ';'
	 * @param trimLine
	 *            true if preceding and appending whitespaces of the line should be removed; false
	 *            otherwise
	 * @param quoteChar
	 *            the character used for value quotes, e.g. for the line
	 *            {@code value1;"30.545";value3} it would be '"'
	 * @param escapeChar
	 *            the character used to escape the following character. The character following an
	 *            escape character will always be part of the result and will not be used as a quote
	 *            or split character. For example, if set to '\', the line
	 *            {@code val\;ue1;"30.545";value3} would result in [val;ue1],[30.545],[value3]
	 * @return an array with the splitted strings
	 */
	public static String[] fastSplit(String line, char splitChar, boolean trimLine, char quoteChar, char escapeChar)
			throws CSVParseException {
		List<String> resultList = new ArrayList<String>();
		/** holding the temporary split string */
		StringBuilder tempString = new StringBuilder();
		/** character read in iteration i */
		// Character currentChar = null;
		/** character which would be read in iteration i+1 */
		// Character nextChar = null;
		/** error message to display */
		String errorMessage = "";
		/** column index where the error occurred */
		int errorColumnIndex = 0;
		/** string with the last 10 characters read before the error occurred */
		String errorLastFewReadChars = "";
		/** current state of the SplitMachine */
		SplitMachineState machineState = SplitMachineState.NEW_SPLIT;

		// trim wanted?
		line = trimLine ? line.trim() : line;
		// go through the line
		for (int i = 0; i < line.length(); i++) {
			// read current character and next character (if applicable)
			char currentChar = line.charAt(i);
			char nextChar;
			if (i + 1 < line.length()) {
				nextChar = line.charAt(i + 1);
			} else {
				nextChar = NONE;
			}
			// run through our split machine
			switch (machineState) {
				case NEW_SPLIT:
					tempString.setLength(0); // faster??
					if (currentChar == splitChar) {
						resultList.add("");
						if (nextChar == NONE) {
							resultList.add("");
							tempString = new StringBuilder();
							machineState = SplitMachineState.END_OF_LINE;
						}
						continue;
					}
					if (currentChar == ' ' || currentChar == '\t') {
						machineState = SplitMachineState.START_WHITESPACE;
						continue;
					}
					if (currentChar == quoteChar) {
						tempString.append(currentChar);

						machineState = SplitMachineState.QUOTE_OPENED;
						continue;
					}
					if (currentChar == escapeChar) {
						if (nextChar == NONE) {
							// special case: escape char followed by EndOfLine -> empty value
							resultList.add("");
							machineState = SplitMachineState.NEW_SPLIT;
							continue;
						}
						// next character was escaped, therefore add it to the string and bypass it
						// in loop
						tempString.append(nextChar);
						i++;
						machineState = SplitMachineState.WRITE_NOT_QUOTE;
						continue;
					}
					tempString.append(currentChar);
					machineState = SplitMachineState.WRITE_NOT_QUOTE;
					continue;
				case START_WHITESPACE:
					if (currentChar == splitChar) {
						resultList.add("");
						machineState = SplitMachineState.NEW_SPLIT;
						continue;
					}
					if (currentChar == ' ' || currentChar == '\t') {
						continue;
					}
					if (currentChar == quoteChar) {
						tempString.append(currentChar);
						machineState = SplitMachineState.QUOTE_OPENED;
						continue;
					}
					if (currentChar == escapeChar) {
						if (nextChar == NONE) {
							// special case: escape char followed by EndOfLine -> empty value
							resultList.add(null);
							machineState = SplitMachineState.END_OF_LINE;
							continue;
						}
						// next character was escaped, therefore add it to the string and bypass it
						// in loop
						tempString.append(nextChar);
						i++;
						machineState = SplitMachineState.WRITE_NOT_QUOTE;
						continue;
					}
					tempString.append(currentChar);
					machineState = SplitMachineState.WRITE_NOT_QUOTE;
					continue;
				case WRITE_NOT_QUOTE:
					if (currentChar == splitChar) {
						resultList.add(tempString.toString());
						// splitChar at end of line handling
						if (nextChar == NONE) {
							resultList.add("");
							tempString = new StringBuilder();
							machineState = SplitMachineState.END_OF_LINE;
							continue;
						}
						machineState = SplitMachineState.NEW_SPLIT;
						continue;
					}
					if (currentChar == escapeChar) {
						if (nextChar == NONE) {
							// special case: escape char followed by EndOfLine -> string read so far
							// w/o escape char
							resultList.add(tempString.toString().trim());
							machineState = SplitMachineState.END_OF_LINE;
							continue;
						}
						// next character was escaped, therefore add it to the string and bypass it
						// in loop
						tempString.append(nextChar);
						i++;
						continue;
					}
					if (currentChar == quoteChar) {
						// error handling
						errorMessage = "Value quote misplaced";
						errorColumnIndex = i;
						if (tempString.length() < 10) {
							StringBuilder errorCharBuf = new StringBuilder();
							errorCharBuf.append(tempString);
							if (errorCharBuf.length() > 0) {
								errorCharBuf.insert(0, splitChar);
							}
							for (int j = errorCharBuf.length(), k = resultList.size() - 1; j < 20; j++, k--) {
								if (k < 0) {
									break;
								}
								errorCharBuf.insert(0, resultList.get(k));
								if (errorCharBuf.length() < 18) {
									errorCharBuf.insert(0, splitChar);
								}
								j = errorCharBuf.length();
							}
							errorCharBuf.reverse();
							errorCharBuf.setLength(19);
							errorCharBuf.reverse();
							errorCharBuf.append(currentChar);
							errorLastFewReadChars = errorCharBuf.toString();
						} else {
							errorLastFewReadChars = tempString.substring(tempString.length() - 9).toString() + currentChar;
						}
						machineState = SplitMachineState.ERROR;
						continue;
					}
					tempString.append(currentChar);
					continue;
				case END_OF_LINE:
					// nothing to do here
					break;
				case QUOTE_OPENED:
					if (currentChar == quoteChar) {
						tempString.append(currentChar);
						machineState = SplitMachineState.QUOTE_CLOSED;
						continue;
					}
					if (currentChar == escapeChar) {
						if (nextChar == NONE) {
							// special case: quote char followed by escape char followed by
							// EndOfLine -> error
							errorMessage = "Value quotes malformed";
							errorColumnIndex = i;
							if (tempString.length() < 10) {
								StringBuilder errorCharBuf = new StringBuilder();
								errorCharBuf.append(tempString);
								if (errorCharBuf.length() > 0) {
									errorCharBuf.insert(0, splitChar);
								}
								for (int j = errorCharBuf.length(), k = resultList.size() - 1; j < 20; j++, k--) {
									if (k < 0) {
										break;
									}
									errorCharBuf.insert(0, resultList.get(k));
									if (errorCharBuf.length() < 18) {
										errorCharBuf.insert(0, splitChar);
									}
									j = errorCharBuf.length();
								}
								errorCharBuf.reverse();
								errorCharBuf.setLength(19);
								errorCharBuf.reverse();
								errorCharBuf.append(currentChar);
								errorLastFewReadChars = errorCharBuf.toString();
							} else {
								errorLastFewReadChars = tempString.substring(tempString.length() - 9).toString()
										+ currentChar;
							}
							machineState = SplitMachineState.ERROR;
							continue;
						}
						// next character was escaped, therefore add it to the string and bypass it
						// in loop
						tempString.append(nextChar);
						i++;
						machineState = SplitMachineState.WRITE_QUOTE;
						continue;
					}
					tempString.append(currentChar);
					machineState = SplitMachineState.WRITE_QUOTE;
					continue;
				case WRITE_QUOTE:
					// special case: double quotes (eg. "") are used to escape the quote character.
					// Excel exports it so...
					if (nextChar != NONE && nextChar == quoteChar && currentChar == quoteChar) {
						tempString.append(nextChar);
						i++;
						continue;
					}
					if (currentChar == quoteChar) {
						tempString.append(currentChar);
						machineState = SplitMachineState.QUOTE_CLOSED;
						continue;
					}
					if (currentChar == escapeChar) {
						if (nextChar == NONE) {
							// special case: quote char followed by char* followed by escape char
							// followed by EndOfLine -> error
							errorMessage = "Value quotes malformed";
							errorColumnIndex = i;
							if (tempString.length() < 10) {
								StringBuilder errorCharBuf = new StringBuilder();
								errorCharBuf.append(tempString);
								if (errorCharBuf.length() > 0) {
									errorCharBuf.insert(0, splitChar);
								}
								for (int j = errorCharBuf.length(), k = resultList.size() - 1; j < 20; j++, k--) {
									if (k < 0) {
										break;
									}
									errorCharBuf.insert(0, resultList.get(k));
									if (errorCharBuf.length() < 18) {
										errorCharBuf.insert(0, splitChar);
									}
									j = errorCharBuf.length();
								}
								errorCharBuf.reverse();
								errorCharBuf.setLength(19);
								errorCharBuf.reverse();
								errorCharBuf.append(currentChar);
								errorLastFewReadChars = errorCharBuf.toString();
							} else {
								errorLastFewReadChars = tempString.substring(tempString.length() - 9).toString()
										+ currentChar;
							}
							machineState = SplitMachineState.ERROR;
							// needs to be thrown here as the loop exists after this pass
							throw new CSVParseException(errorMessage + " at position " + i + ". Last characters read: "
									+ errorLastFewReadChars);
						}
						// next character was escaped, therefore add it to the string and bypass it
						// in loop
						tempString.append(nextChar);
						i++;
						continue;
					}

					tempString.append(currentChar);
					continue;
				case QUOTE_CLOSED:
					if (currentChar == splitChar) {
						// remove quotes
						if (tempString.charAt(0) == quoteChar && tempString.charAt(tempString.length() - 1) == quoteChar) {
							// IF YOU DO NOT WANT "abc   " to become "abc", (w/o the quotes) remove
							// .trim() ! Check the last string handling for
							// similiar case!
							resultList.add(tempString.substring(1, tempString.length() - 1).trim());
						} else {
							// this should not occur, malformed quotes should be caught earlier
							resultList.add(tempString.toString());
						}
						// splitChar at end of line handling
						if (nextChar == NONE) {
							resultList.add("");
							tempString = new StringBuilder();
							machineState = SplitMachineState.END_OF_LINE;
							continue;
						}
						machineState = SplitMachineState.NEW_SPLIT;
						continue;
					}
					if (currentChar == ' ' || (currentChar == '\t')) {
						// delete whitespaces after closing quotes
						continue;
					}
					// error handling
					errorMessage = "Unexpected character after closed value quote";
					errorColumnIndex = i;
					if (tempString.length() < 10) {
						StringBuilder errorCharBuf = new StringBuilder();
						errorCharBuf.append(tempString);
						if (errorCharBuf.length() > 0) {
							errorCharBuf.insert(0, splitChar);
						}
						for (int j = errorCharBuf.length(), k = resultList.size() - 1; j < 20; j++, k--) {
							if (k < 0) {
								break;
							}
							errorCharBuf.insert(0, resultList.get(k));
							if (errorCharBuf.length() < 18) {
								errorCharBuf.insert(0, splitChar);
							}
							j = errorCharBuf.length();
						}
						errorCharBuf.reverse();
						errorCharBuf.setLength(19);
						errorCharBuf.reverse();
						errorCharBuf.append(currentChar);
						errorLastFewReadChars = errorCharBuf.toString();
					} else {
						errorLastFewReadChars = tempString.substring(tempString.length() - 9).toString() + currentChar;
					}
					// needs to be thrown here as the loop exists after this pass
					throw new CSVParseException(errorMessage + " at position " + i + ". Last characters read: "
							+ errorLastFewReadChars);
				case ERROR:
					throw new CSVParseException(errorMessage + " at position " + i + ". Last characters read: "
							+ errorLastFewReadChars);
			}
		}

		// last string handling
		// error state, malformed quote
		if (machineState == SplitMachineState.QUOTE_OPENED || machineState == SplitMachineState.WRITE_QUOTE) {
			errorMessage = "Value quotes not closed";
			errorColumnIndex = line.length() - 1;
			if (tempString.length() < 10) {
				StringBuilder errorCharBuf = new StringBuilder();
				errorCharBuf.append(tempString);
				if (errorCharBuf.length() > 0) {
					errorCharBuf.insert(0, splitChar);
				}
				for (int j = errorCharBuf.length(), k = resultList.size() - 1; j < 20; j++, k--) {
					if (k < 0) {
						break;
					}
					errorCharBuf.insert(0, resultList.get(k));
					if (errorCharBuf.length() < 18) {
						errorCharBuf.insert(0, splitChar);
					}
					j = errorCharBuf.length();
				}
				errorCharBuf.reverse();
				errorCharBuf.setLength(20);
				errorCharBuf.reverse();
				errorLastFewReadChars = errorCharBuf.toString();
			} else {
				errorLastFewReadChars = tempString.substring(tempString.length() - 10).toString();
			}
			throw new CSVParseException(errorMessage + " at position " + errorColumnIndex + ". Last characters read: "
					+ errorLastFewReadChars);
		} else {
			// add the last string to the list
			if (tempString.length() > 0) {
				// remove quotes if state QUOTE_CLOSED was reached
				if (machineState == SplitMachineState.QUOTE_CLOSED && tempString.charAt(0) == quoteChar
						&& tempString.charAt(tempString.length() - 1) == quoteChar) {
					// IF YOU DO NOT WANT "abc   " to become "abc", (w/o the quotes) remove .trim()
					// !
					resultList.add(tempString.substring(1, tempString.length() - 1).trim());
					tempString = new StringBuilder();
				} else {
					resultList.add(tempString.toString());
					tempString = new StringBuilder();
				}
			}
		}

		String[] resultArray = new String[resultList.size()];
		resultList.toArray(resultArray);
		return resultArray;
	}

	public void configure(CSVResultSetConfiguration configuration) throws OperatorException {
		setTrimLine(configuration.isTrimLines());
		setSkipComments(configuration.isSkipComments());
		setSplitExpression(configuration.getColumnSeparators());
		setUseQuotes(configuration.isUseQuotes());
		setQuoteCharacter(configuration.getQuoteCharacter());
		setQuoteEscapeCharacter(configuration.getEscapeCharacter());
		setCommentCharacters(configuration.getCommentCharacters());
		setEncoding(configuration.getEncoding());
	}
}
