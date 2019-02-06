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
package com.rapidminer.gui.tools;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 * Matches, e.g. DiBi to Discretize by Binning if first character is upper case and checks for case sensitive contains.
 * Otherwise does case insensitive contains. Accepts up to 1 mistakes.
 *
 * @author Andreas Timm
 * @Since 8.0
 */
public class CamelCaseTypoFilter implements OperatorFilter {

	private static final char ANY_CHAR_PLACEHOLDER = '?';
	private static final String OPEN_QUOTE = "\\Q";
	private static final String CLOSE_QUOTE = "\\E";
	/**
	 * There isn't a constant in Pattern
	 */
	private static final int NO_FLAG = 0x00;
	/** StringBuilder default initial space see {@link StringBuilder:new}*/
	private static final int REGEX_INITIAL_SIZE = 16;
	/** Minimal length to start the fuzzy search */
	private static final int MIN_SEARCH_LENGTH = 3;
	/** Limit maximal search length */
	private static final int MAX_SEARCH_LENGTH = 30;

	private final LinkedList<Pattern> patterns;
	private final boolean caseSensitive;

	/**
	 * This is the constructor. Only non-null values may be passed!
	 */
	public CamelCaseTypoFilter(String filterString) {
		String trimmedFilter = filterString == null ? "" : filterString.trim();
		//Disable case sensitivity
		caseSensitive = false;
		if (trimmedFilter.length() > 0) {
			patterns = generatePattern(trimmedFilter);
		} else {
			patterns = new LinkedList<>();
		}
	}

	/**
	 * Generate the patterns from the filterString
	 *
	 * @param filterString The filterString, not null or whitespace
	 */
	private LinkedList<Pattern> generatePattern(String filterString) {
		LinkedList<Pattern> generatedPatterns = new LinkedList<>();
		for (String s : createSpecializedStringTranspositions(filterString)) {
			Pattern p = toRegexp(s);
			if (p != null) {
				generatedPatterns.add(p);
			}
		}
		return generatedPatterns;
	}

	/**
	 * Creates transpositions where a single Character is misspelled. vrite -> ?rite
	 *
	 * @param filterString
	 * @return
	 */
	protected List<String> createSpecializedStringTranspositions(String filterString) {
		List<String> result = new ArrayList<>();
		result.add(filterString);
		if (filterString.length() >= MIN_SEARCH_LENGTH && filterString.length() <= MAX_SEARCH_LENGTH) {
			for (int i = 0; i < filterString.length(); i++) {
				char theChar = filterString.charAt(i);
				if (Character.isAlphabetic(theChar)) {
					String transpositionInsert = insert(filterString, i, ANY_CHAR_PLACEHOLDER);
					if (transpositionInsert != null) {
						result.add(transpositionInsert);
					}
				}
				if (Character.isLowerCase(theChar) || Character.isDigit(theChar)) {
					String transpositionReplace = replace(filterString, i, ANY_CHAR_PLACEHOLDER);
					if (transpositionReplace != null) {
						result.add(transpositionReplace);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Inserts the character at the given position of the String
	 *
	 * @param str
	 * 		The given String
	 * @param pos
	 * 		The position where insertChar should be added
	 * @param insertChar
	 * 		The Char to add to the str
	 * @return
	 */
	private String insert(String str, int pos, char insertChar) {
		if (pos >= 0 && str != null && pos <= str.length()) {
			if (pos == 0) {
				return insertChar + str;
			} else if (pos == str.length()) {
				return str + insertChar;
			} else {
				return str.substring(0, pos) + insertChar + str.substring(pos);
			}
		} else {
			return null;
		}
	}

	/**
	 * Replace the the character in str at the given pos with the replacementChar
	 *
	 * @param str
	 * 		The given str
	 * @param pos
	 * 		The position that should be replaced
	 * @param replacementChar
	 * 		The char to replace
	 * @return
	 */
	private String replace(String str, int pos, char replacementChar) {
		if (pos >= 0 && str != null && pos < str.length()) {
			if (pos == 0) {
				return replacementChar + str.substring(1);
			} else if (pos == str.length() - 1) {
				return str.substring(0, pos) + replacementChar;
			} else {
				return str.substring(0, pos) + replacementChar + str.substring(pos + 1);
			}
		} else {
			return null;
		}
	}

	/**
	 * Check if this Filter matches the string
	 *
	 * @param string
	 * @return
	 */
	public boolean matches(String string) {
		for (Pattern pattern : patterns) {
			if (pattern.matcher(string).find()) {
				patterns.remove(pattern);
				patterns.addFirst(pattern);
				return true;
			}
		}
		return false;
	}

	/**
	 * Converts the given filter String to a Regular expression
	 * <p>
	 * <ul> <li>Replaces ? with .?</li> <li>Quote non-regex expressions</li> <li>Allow Camel-Case search</li> </ul>
	 *
	 * @param filterString
	 * @return
	 */
	public Pattern toRegexp(String filterString) {
		StringBuilder regexp = new StringBuilder(filterString.length() + REGEX_INITIAL_SIZE);
		final AtomicBoolean quoteIsClosed = new AtomicBoolean(true);
		Supplier<String> openQuote = () -> quoteIsClosed.compareAndSet(true, false) ? OPEN_QUOTE : "";
		Supplier<String> closeQuote = () -> quoteIsClosed.compareAndSet(false, true) ? CLOSE_QUOTE : "";

		for (char c : filterString.toCharArray()) {
			if ((Character.isUpperCase(c) || Character.isDigit(c))) {
				regexp.append(closeQuote.get());
				regexp.append("[^A-Z]*"); // anything that is not upper case
				regexp.append(c);
			} else if (ANY_CHAR_PLACEHOLDER == c) {
				regexp.append(closeQuote.get());
				regexp.append(".?"); // 0..1 character
			} else {
				regexp.append(openQuote.get());
				regexp.append(c);
			}
		}
		regexp.append(closeQuote.get());

		try {
			return Pattern.compile(regexp.toString(), (caseSensitive) ? NO_FLAG : Pattern.CASE_INSENSITIVE);
		} catch (PatternSyntaxException e) {
			return null;
			// can happen only if regexp special chars in filter string that cannot be
			// escaped by \Q \E
		}
	}

}