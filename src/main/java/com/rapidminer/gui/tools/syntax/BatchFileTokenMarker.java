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
 * BatchFileTokenMarker.java - Batch file token marker Copyright (C) 1998, 1999 Slava Pestov
 * 
 * You may use and modify this package for any purpose. Redistribution is permitted, in both source
 * and binary form, provided that this notice remains intact in all source distributions of this
 * package.
 */

import javax.swing.text.Segment;


/**
 * Batch file token marker.
 * 
 * @author Slava Pestov, Ingo Mierswa
 */
public class BatchFileTokenMarker extends TokenMarker {

	@Override
	public byte markTokensImpl(byte token, Segment line, int lineIndex) {
		char[] array = line.array;
		int offset = line.offset;
		int lastOffset = offset;
		int length = line.count + offset;

		if (SyntaxUtilities.regionMatches(true, line, offset, "rem")) {
			addToken(line.count, Token.COMMENT1);
			return Token.NULL;
		}

		loop: for (int i = offset; i < length; i++) {
			int i1 = (i + 1);

			switch (token) {
				case Token.NULL:
					switch (array[i]) {
						case '%':
							addToken(i - lastOffset, token);
							lastOffset = i;
							if (length - i <= 3 || array[i + 2] == ' ') {
								addToken(2, Token.KEYWORD2);
								i += 2;
								lastOffset = i;
							} else {
								token = Token.KEYWORD2;
							}
							break;
						case '"':
							addToken(i - lastOffset, token);
							token = Token.LITERAL1;
							lastOffset = i;
							break;
						case ':':
							if (i == offset) {
								addToken(line.count, Token.LABEL);
								lastOffset = length;
								break loop;
							}
							break;
						case ' ':
							if (lastOffset == offset) {
								addToken(i - lastOffset, Token.KEYWORD1);
								lastOffset = i;
							}
							break;
					}
					break;
				case Token.KEYWORD2:
					if (array[i] == '%') {
						addToken(i1 - lastOffset, token);
						token = Token.NULL;
						lastOffset = i1;
					}
					break;
				case Token.LITERAL1:
					if (array[i] == '"') {
						addToken(i1 - lastOffset, token);
						token = Token.NULL;
						lastOffset = i1;
					}
					break;
				default:
					throw new InternalError("Invalid state: " + token);
			}
		}

		if (lastOffset != length) {
			if (token != Token.NULL) {
				token = Token.INVALID;
			} else if (lastOffset == offset) {
				token = Token.KEYWORD1;
			}
			addToken(length - lastOffset, token);
		}
		return Token.NULL;
	}

	@Override
	public boolean supportsMultilineTokens() {
		return false;
	}
}
