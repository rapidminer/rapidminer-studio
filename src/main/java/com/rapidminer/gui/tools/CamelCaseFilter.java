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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 * Matches, e.g. BiDi to BinDiscretization if first character is upper case and checks for case
 * sensitive contains. Otherwise does case insensitive contains.
 *
 *
 * @author Simon Fischer, Gisa Schaefer
 *
 */
public class CamelCaseFilter implements OperatorFilter {

	private final String filterString;
	private Pattern pattern = null;
	private boolean caseSensitive;

	/**
	 * This is the constructor. Only non-null values might be passed!
	 */
	public CamelCaseFilter(String filterString) {
		if (filterString != null && filterString.trim().length() > 0) {

			caseSensitive = Character.isUpperCase(filterString.charAt(0)) ? true : false;
			if (caseSensitive) {
				this.filterString = filterString.trim();
			} else {
				this.filterString = filterString.trim().toLowerCase();
			}
			StringBuilder regexp = new StringBuilder();
			// regexp.append(".*"); //use if camel case should not start with first word
			boolean first = true;
			for (char c : filterString.toCharArray()) {
				if ((Character.isUpperCase(c) || Character.isDigit(c)) && !first) {
					regexp.append("\\E[^A-Z]*"); // end of literal sequence plus anything
					// that is not upper case
				}
				if (first && Character.isLowerCase(c)) {
					regexp.append(Character.toUpperCase(c));
					regexp.append("\\Q");
				} else {
					regexp.append(c);
				}
				first = false;

				if (Character.isUpperCase(c) || Character.isDigit(c)) {
					regexp.append("\\Q");
				}
			}
			regexp.append("\\E.*");
			try {
				this.pattern = Pattern.compile(regexp.toString());
			} catch (PatternSyntaxException e) {
				this.pattern = null;
				// can happen only if regexp special chars in filter string that cannot be
				// escaped by \Q \E
			}

		} else {
			this.filterString = "";
		}

	}

	public boolean matches(String string) {
		if (string == null) {
			return false;
		} else {
			if (caseSensitive) {
				return string.contains(filterString) || pattern != null && pattern.matcher(string).matches();
			} else {
				return string.toLowerCase().contains(filterString) || pattern != null && pattern.matcher(string).matches();
			}

		}
	}

	@Override
	public String toString() {
		if (pattern != null) {
			return pattern.toString();
		} else {
			return filterString;
		}
	}
}
