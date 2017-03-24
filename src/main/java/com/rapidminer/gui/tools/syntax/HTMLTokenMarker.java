/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.gui.tools.syntax;

/*
 * HTMLTokenMarker.java - HTML token marker Copyright (C) 1998, 1999 Slava Pestov
 * 
 * You may use and modify this package for any purpose. Redistribution is permitted, in both source
 * and binary form, provided that this notice remains intact in all source distributions of this
 * package.
 */

import javax.swing.text.Segment;


/**
 * HTML token marker.
 * 
 * @author Slava Pestov, Ingo Mierswa
 */
public class HTMLTokenMarker extends TokenMarker {

	public static final byte JAVASCRIPT = Token.INTERNAL_FIRST;

	public HTMLTokenMarker() {
		this(true);
	}

	public HTMLTokenMarker(boolean js) {
		this.js = js;
		keywords = JavaScriptTokenMarker.getKeywords();
	}

	@Override
	public byte markTokensImpl(byte token, Segment line, int lineIndex) {
		char[] array = line.array;
		int offset = line.offset;
		lastOffset = offset;
		lastKeyword = offset;
		int length = line.count + offset;
		boolean backslash = false;

		loop: for (int i = offset; i < length; i++) {
			int i1 = (i + 1);

			char c = array[i];
			if (c == '\\') {
				backslash = !backslash;
				continue;
			}

			switch (token) {
				case Token.NULL: // HTML text
					backslash = false;
					switch (c) {
						case '<':
							addToken(i - lastOffset, token);
							lastOffset = lastKeyword = i;
							if (SyntaxUtilities.regionMatches(false, line, i1, "!--")) {
								i += 3;
								token = Token.COMMENT1;
							} else if (js && SyntaxUtilities.regionMatches(true, line, i1, "script>")) {
								addToken(8, Token.KEYWORD1);
								lastOffset = lastKeyword = (i += 8);
								token = JAVASCRIPT;
							} else {
								token = Token.KEYWORD1;
							}
							break;
						case '&':
							addToken(i - lastOffset, token);
							lastOffset = lastKeyword = i;
							token = Token.KEYWORD2;
							break;
					}
					break;
				case Token.KEYWORD1: // Inside a tag
					backslash = false;
					if (c == '>') {
						addToken(i1 - lastOffset, token);
						lastOffset = lastKeyword = i1;
						token = Token.NULL;
					}
					break;
				case Token.KEYWORD2: // Inside an entity
					backslash = false;
					if (c == ';') {
						addToken(i1 - lastOffset, token);
						lastOffset = lastKeyword = i1;
						token = Token.NULL;
						break;
					}
					break;
				case Token.COMMENT1: // Inside a comment
					backslash = false;
					if (SyntaxUtilities.regionMatches(false, line, i, "-->")) {
						addToken((i + 3) - lastOffset, token);
						lastOffset = lastKeyword = i + 3;
						token = Token.NULL;
					}
					break;
				case JAVASCRIPT: // Inside a JavaScript
					switch (c) {
						case '<':
							backslash = false;
							doKeyword(line, i, c);
							if (SyntaxUtilities.regionMatches(true, line, i1, "/script>")) {
								addToken(i - lastOffset, Token.NULL);
								addToken(9, Token.KEYWORD1);
								lastOffset = lastKeyword = (i += 9);
								token = Token.NULL;
							}
							break;
						case '"':
							if (backslash) {
								backslash = false;
							} else {
								doKeyword(line, i, c);
								addToken(i - lastOffset, Token.NULL);
								lastOffset = lastKeyword = i;
								token = Token.LITERAL1;
							}
							break;
						case '\'':
							if (backslash) {
								backslash = false;
							} else {
								doKeyword(line, i, c);
								addToken(i - lastOffset, Token.NULL);
								lastOffset = lastKeyword = i;
								token = Token.LITERAL2;
							}
							break;
						case '/':
							backslash = false;
							doKeyword(line, i, c);
							if (length - i > 1) {
								addToken(i - lastOffset, Token.NULL);
								lastOffset = lastKeyword = i;
								if (array[i1] == '/') {
									addToken(length - i, Token.COMMENT2);
									lastOffset = lastKeyword = length;
									break loop;
								} else if (array[i1] == '*') {
									token = Token.COMMENT2;
								}
							}
							break;
						default:
							backslash = false;
							if (!Character.isLetterOrDigit(c) && c != '_') {
								doKeyword(line, i, c);
							}
							break;
					}
					break;
				case Token.LITERAL1: // JavaScript "..."
					if (backslash) {
						backslash = false;
					} else if (c == '"') {
						addToken(i1 - lastOffset, Token.LITERAL1);
						lastOffset = lastKeyword = i1;
						token = JAVASCRIPT;
					}
					break;
				case Token.LITERAL2: // JavaScript '...'
					if (backslash) {
						backslash = false;
					} else if (c == '\'') {
						addToken(i1 - lastOffset, Token.LITERAL1);
						lastOffset = lastKeyword = i1;
						token = JAVASCRIPT;
					}
					break;
				case Token.COMMENT2: // Inside a JavaScript comment
					backslash = false;
					if (c == '*' && length - i > 1 && array[i1] == '/') {
						addToken((i += 2) - lastOffset, Token.COMMENT2);
						lastOffset = lastKeyword = i;
						token = JAVASCRIPT;
					}
					break;
				default:
					throw new InternalError("Invalid state: " + token);
			}
		}

		switch (token) {
			case Token.LITERAL1:
			case Token.LITERAL2:
				addToken(length - lastOffset, Token.INVALID);
				token = JAVASCRIPT;
				break;
			case Token.KEYWORD2:
				addToken(length - lastOffset, Token.INVALID);
				token = Token.NULL;
				break;
			case JAVASCRIPT:
				doKeyword(line, length, '\0');
				addToken(length - lastOffset, Token.NULL);
				break;
			default:
				addToken(length - lastOffset, token);
				break;
		}

		return token;
	}

	// private members
	private KeywordMap keywords;

	private boolean js;

	private int lastOffset;

	private int lastKeyword;

	private boolean doKeyword(Segment line, int i, char c) {
		int i1 = i + 1;

		int len = i - lastKeyword;
		byte id = keywords.lookup(line, lastKeyword, len);
		if (id != Token.NULL) {
			if (lastKeyword != lastOffset) {
				addToken(lastKeyword - lastOffset, Token.NULL);
			}
			addToken(len, id);
			lastOffset = i;
		}
		lastKeyword = i1;
		return false;
	}
}
