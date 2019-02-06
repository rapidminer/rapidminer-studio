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
 * SQLTokenMarker.java - Generic SQL token marker Copyright (C) 1999 mike dillon
 * 
 * You may use and modify this package for any purpose. Redistribution is permitted, in both source
 * and binary form, provided that this notice remains intact in all source distributions of this
 * package.
 */

import javax.swing.text.Segment;


/**
 * SQL token marker.
 * 
 * @author Mike Dillon, Ingo Mierswa
 */
public class SQLTokenMarker extends TokenMarker {

	private int offset, lastOffset, lastKeyword, lengthT;

	// public members
	public SQLTokenMarker(KeywordMap k) {
		this(k, false);
	}

	public SQLTokenMarker(KeywordMap k, boolean tsql) {
		keywords = k;
		isTSQL = tsql;
	}

	@Override
	public byte markTokensImpl(byte token, Segment line, int lineIndex) {
		offset = lastOffset = lastKeyword = line.offset;
		lengthT = line.count + offset;

		loop: for (int i = offset; i < lengthT; i++) {
			switch (line.array[i]) {
				case '*':
					if (token == Token.COMMENT1 && lengthT - i >= 1 && line.array[i + 1] == '/') {
						token = Token.NULL;
						i++;
						addToken((i + 1) - lastOffset, Token.COMMENT1);
						lastOffset = i + 1;
					} else if (token == Token.NULL) {
						searchBack(line, i);
						addToken(1, Token.OPERATOR);
						lastOffset = i + 1;
					}
					break;
				case '[':
					if (token == Token.NULL) {
						searchBack(line, i);
						token = Token.LITERAL1;
						literalChar = '[';
						lastOffset = i;
					}
					break;
				case ']':
					if (token == Token.LITERAL1 && literalChar == '[') {
						token = Token.NULL;
						literalChar = 0;
						addToken((i + 1) - lastOffset, Token.LITERAL1);
						lastOffset = i + 1;
					}
					break;
				case '.':
				case ',':
				case '(':
				case ')':
					if (token == Token.NULL) {
						searchBack(line, i);
						addToken(1, Token.NULL);
						lastOffset = i + 1;
					}
					break;
				case '+':
				case '%':
				case '&':
				case '|':
				case '^':
				case '~':
				case '<':
				case '>':
				case '=':
					if (token == Token.NULL) {
						searchBack(line, i);
						addToken(1, Token.OPERATOR);
						lastOffset = i + 1;
					}
					break;
				case ' ':
				case '\t':
					if (token == Token.NULL) {
						searchBack(line, i, false);
					}
					break;
				case ':':
					if (token == Token.NULL) {
						addToken((i + 1) - lastOffset, Token.LABEL);
						lastOffset = i + 1;
					}
					break;
				case '/':
					if (token == Token.NULL) {
						if (lengthT - i >= 2 && line.array[i + 1] == '*') {
							searchBack(line, i);
							token = Token.COMMENT1;
							lastOffset = i;
							i++;
						} else {
							searchBack(line, i);
							addToken(1, Token.OPERATOR);
							lastOffset = i + 1;
						}
					}
					break;
				case '-':
					if (token == Token.NULL) {
						if (lengthT - i >= 2 && line.array[i + 1] == '-') {
							searchBack(line, i);
							addToken(lengthT - i, Token.COMMENT1);
							lastOffset = lengthT;
							break loop;
						} else {
							searchBack(line, i);
							addToken(1, Token.OPERATOR);
							lastOffset = i + 1;
						}
					}
					break;
				case '!':
					if (isTSQL && token == Token.NULL && lengthT - i >= 2
							&& (line.array[i + 1] == '=' || line.array[i + 1] == '<' || line.array[i + 1] == '>')) {
						searchBack(line, i);
						addToken(1, Token.OPERATOR);
						lastOffset = i + 1;
					}
					break;
				case '"':
				case '\'':
					if (token == Token.NULL) {
						token = Token.LITERAL1;
						literalChar = line.array[i];
						addToken(i - lastOffset, Token.NULL);
						lastOffset = i;
					} else if (token == Token.LITERAL1 && literalChar == line.array[i]) {
						token = Token.NULL;
						literalChar = 0;
						addToken((i + 1) - lastOffset, Token.LITERAL1);
						lastOffset = i + 1;
					}
					break;
				default:
					break;
			}
		}
		if (token == Token.NULL) {
			searchBack(line, lengthT, false);
		}
		if (lastOffset != lengthT) {
			addToken(lengthT - lastOffset, token);
		}
		return token;
	}

	// protected members
	protected boolean isTSQL = false;

	// private members
	private KeywordMap keywords;

	private char literalChar = 0;

	private void searchBack(Segment line, int pos) {
		searchBack(line, pos, true);
	}

	private void searchBack(Segment line, int pos, boolean padNull) {
		int len = pos - lastKeyword;
		byte id = keywords.lookup(line, lastKeyword, len);
		if (id != Token.NULL) {
			if (lastKeyword != lastOffset) {
				addToken(lastKeyword - lastOffset, Token.NULL);
			}
			addToken(len, id);
			lastOffset = pos;
		}
		lastKeyword = pos + 1;
		if (padNull && lastOffset < pos) {
			addToken(pos - lastOffset, Token.NULL);
		}
	}
}
