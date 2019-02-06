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
package com.rapidminer.gui.tools.syntax;

/*
 * ShellScriptTokenMarker.java - Shell script token marker Copyright (C) 1998, 1999 Slava Pestov
 * 
 * You may use and modify this package for any purpose. Redistribution is permitted, in both source
 * and binary form, provided that this notice remains intact in all source distributions of this
 * package.
 */

import javax.swing.text.Segment;


/**
 * Shell script token marker.
 * 
 * @author Slava Pestov, Ingo Mierswa
 */
public class ShellScriptTokenMarker extends TokenMarker {

	// public members
	public static final byte LVARIABLE = Token.INTERNAL_FIRST;

	@Override
	public byte markTokensImpl(byte token, Segment line, int lineIndex) {
		char[] array = line.array;
		byte cmdState = 0; // 0 = space before command, 1 = inside
		// command, 2 = after command
		int offset = line.offset;
		int lastOffset = offset;
		int length = line.count + offset;

		if (token == Token.LITERAL1 && lineIndex != 0 && lineInfo[lineIndex - 1].obj != null) {
			String str = (String) lineInfo[lineIndex - 1].obj;
			if (str != null && str.length() == line.count && SyntaxUtilities.regionMatches(false, line, offset, str)) {
				addToken(line.count, Token.LITERAL1);
				return Token.NULL;
			} else {
				addToken(line.count, Token.LITERAL1);
				lineInfo[lineIndex].obj = str;
				return Token.LITERAL1;
			}
		}

		boolean backslash = false;
		loop: for (int i = offset; i < length; i++) {
			int i1 = (i + 1);

			char c = array[i];

			if (c == '\\') {
				backslash = !backslash;
				continue;
			}

			switch (token) {
				case Token.NULL:
					switch (c) {
						case ' ':
						case '\t':
						case '(':
						case ')':
							backslash = false;
							if (cmdState == 1/* insideCmd */) {
								addToken(i - lastOffset, Token.KEYWORD1);
								lastOffset = i;
								cmdState = 2; /* afterCmd */
							}
							break;
						case '=':
							backslash = false;
							if (cmdState == 1/* insideCmd */) {
								addToken(i - lastOffset, token);
								lastOffset = i;
								cmdState = 2; /* afterCmd */
							}
							break;
						case '&':
						case '|':
						case ';':
							if (backslash) {
								backslash = false;
							} else {
								cmdState = 0; /* beforeCmd */
							}
							break;
						case '#':
							if (backslash) {
								backslash = false;
							} else {
								addToken(i - lastOffset, token);
								addToken(length - i, Token.COMMENT1);
								lastOffset = length;
								break loop;
							}
							break;
						case '$':
							if (backslash) {
								backslash = false;
							} else {
								addToken(i - lastOffset, token);
								cmdState = 2; /* afterCmd */
								lastOffset = i;
								if (length - i >= 2) {
									switch (array[i1]) {
										case '(':
											continue;
										case '{':
											token = LVARIABLE;
											break;
										default:
											token = Token.KEYWORD2;
											break;
									}
								} else {
									token = Token.KEYWORD2;
								}
							}
							break;
						case '"':
							if (backslash) {
								backslash = false;
							} else {
								addToken(i - lastOffset, token);
								token = Token.LITERAL1;
								lineInfo[lineIndex].obj = null;
								cmdState = 2; /* afterCmd */
								lastOffset = i;
							}
							break;
						case '\'':
							if (backslash) {
								backslash = false;
							} else {
								addToken(i - lastOffset, token);
								token = Token.LITERAL2;
								cmdState = 2; /* afterCmd */
								lastOffset = i;
							}
							break;
						case '<':
							if (backslash) {
								backslash = false;
							} else {
								if (length - i > 1 && array[i1] == '<') {
									addToken(i - lastOffset, token);
									token = Token.LITERAL1;
									lastOffset = i;
									lineInfo[lineIndex].obj = new String(array, i + 2, length - (i + 2));
								}
							}
							break;
						default:
							backslash = false;
							if (Character.isLetter(c)) {
								if (cmdState == 0 /* beforeCmd */) {
									addToken(i - lastOffset, token);
									lastOffset = i;
									cmdState++; /* insideCmd */
								}
							}
							break;
					}
					break;
				case Token.KEYWORD2:
					backslash = false;
					if (!Character.isLetterOrDigit(c) && c != '_') {
						if (i != offset && array[i - 1] == '$') {
							addToken(i1 - lastOffset, token);
							lastOffset = i1;
							token = Token.NULL;
							continue;
						} else {
							addToken(i - lastOffset, token);
							lastOffset = i;
							token = Token.NULL;
						}
					}
					break;
				case Token.LITERAL1:
					if (backslash) {
						backslash = false;
					} else if (c == '"') {
						addToken(i1 - lastOffset, token);
						cmdState = 2; /* afterCmd */
						lastOffset = i1;
						token = Token.NULL;
					} else {
						backslash = false;
					}
					break;
				case Token.LITERAL2:
					if (backslash) {
						backslash = false;
					} else if (c == '\'') {
						addToken(i1 - lastOffset, Token.LITERAL1);
						cmdState = 2; /* afterCmd */
						lastOffset = i1;
						token = Token.NULL;
					} else {
						backslash = false;
					}
					break;
				case LVARIABLE:
					backslash = false;
					if (c == '}') {
						addToken(i1 - lastOffset, Token.KEYWORD2);
						lastOffset = i1;
						token = Token.NULL;
					}
					break;
				default:
					throw new InternalError("Invalid state: " + token);
			}
		}

		switch (token) {
			case Token.NULL:
				if (cmdState == 1) {
					addToken(length - lastOffset, Token.KEYWORD1);
				} else {
					addToken(length - lastOffset, token);
				}
				break;
			case Token.LITERAL2:
				addToken(length - lastOffset, Token.LITERAL1);
				break;
			case Token.KEYWORD2:
				addToken(length - lastOffset, token);
				token = Token.NULL;
				break;
			case LVARIABLE:
				addToken(length - lastOffset, Token.INVALID);
				token = Token.NULL;
				break;
			default:
				addToken(length - lastOffset, token);
				break;
		}
		return token;
	}
}
