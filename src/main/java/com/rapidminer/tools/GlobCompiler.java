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

import java.util.Stack;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 * This class provides the functionality to translate a GLOB expression into a standard regular
 * expression.
 * 
 * GLOB provides a subset of the functionality of regular expressions: '?' Matches one unknown
 * character '*' Matches any number of unknown characters '[abc]' Matches a, b or c, or more general
 * any inserted character '{abc,def}' Matches abc or def.
 * 
 * @author Sebastian Land
 * 
 */
public class GlobCompiler {

	private enum Commands {
		UNBRACED, SQUARE_BRACKETS, CURLY_BRACKETS
	}

	public static Pattern compileGlob(final String glob) throws PatternSyntaxException {
		// use stack to keep track if inside braces. Start unbraced...
		Stack<Commands> currentMode = new Stack<Commands>();
		currentMode.push(Commands.UNBRACED);

		int globLength = glob.length();
		int currentIndex = 0;

		/* equivalent REGEX expression to be compiled */
		StringBuffer buffer = new StringBuffer();

		while (currentIndex < globLength) {
			char c = glob.charAt(currentIndex++);
			if (c == '\\') {
				if (currentIndex == globLength) {
					// no characters left, so treat '\' as literal char
					buffer.append(Pattern.quote("\\"));
				} else {
					// read next character
					String s = glob.charAt(currentIndex) + "";
					if ((Commands.UNBRACED == currentMode.peek() && "\\[]{}?*".contains(s))
							|| (Commands.SQUARE_BRACKETS == currentMode.peek() && "\\[]{}?*!-".contains(s))
							|| (Commands.CURLY_BRACKETS == currentMode.peek()) && "\\[]{}?*,".contains(s)) {
						// escape the construct char
						currentIndex++;
						buffer.append(Pattern.quote(s));
					} else {
						// treat '\' as literal char
						buffer.append(Pattern.quote("\\"));
					}
				}
			} else if (c == '*') {
				// *
				buffer.append(".*");
			} else if (c == '?') {
				// .
				buffer.append('.');
				// [...]
			} else if (c == '[') {
				buffer.append('[');
				currentMode.push(Commands.SQUARE_BRACKETS);
				// check for negation character '!' immediately after the opening bracket '['
				if ((currentIndex < globLength) && (glob.charAt(currentIndex) == '!')) {
					currentIndex++;
					buffer.append('^');
				}
			} else if ((c == ']') && Commands.SQUARE_BRACKETS == (currentMode.peek())) {
				buffer.append(']');
				currentMode.pop();
			} else if ((c == '-') && Commands.SQUARE_BRACKETS == (currentMode.peek())) {
				// character range '-' in "[...]"
				buffer.append('-');
				// {...}
			} else if (c == '{') {
				buffer.append("(?:(?:");
				currentMode.push(Commands.CURLY_BRACKETS);
			} else if ((c == '}') && Commands.CURLY_BRACKETS == (currentMode.peek())) {
				buffer.append("))");
				currentMode.pop();
			} else if ((c == ',') && Commands.CURLY_BRACKETS == (currentMode.peek())) {
				// comma between strings in "{...}"
				buffer.append(")|(?:");
			} else {
				// simple literal
				buffer.append(Pattern.quote(c + ""));
			}
		}

		// finally check for mismatched [...] or {...}
		if ("[]".equals(currentMode.peek())) {
			throw new PatternSyntaxException("Cannot find matching closing square bracket ] in GLOB expression", glob, -1);
		}

		if ("{}".equals(currentMode.peek())) {
			throw new PatternSyntaxException("Cannot find matching closing curly brace } in GLOB expression", glob, -1);
		}
		return Pattern.compile(buffer.toString());
	}
}
