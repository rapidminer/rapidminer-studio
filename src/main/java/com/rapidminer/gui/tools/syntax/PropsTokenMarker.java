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
 * PropsTokenMarker.java - Java props/DOS INI token marker Copyright (C) 1998, 1999 Slava Pestov
 * 
 * You may use and modify this package for any purpose. Redistribution is permitted, in both source
 * and binary form, provided that this notice remains intact in all source distributions of this
 * package.
 */

import javax.swing.text.Segment;


/**
 * Java properties/DOS INI token marker.
 * 
 * @author Slava Pestov, Ingo Mierswa
 */
public class PropsTokenMarker extends TokenMarker {

	public static final byte VALUE = Token.INTERNAL_FIRST;

	@Override
	public byte markTokensImpl(byte token, Segment line, int lineIndex) {
		char[] array = line.array;
		int offset = line.offset;
		int lastOffset = offset;
		int length = line.count + offset;
		loop: for (int i = offset; i < length; i++) {
			int i1 = (i + 1);

			switch (token) {
				case Token.NULL:
					switch (array[i]) {
						case '#':
						case ';':
							if (i == offset) {
								addToken(line.count, Token.COMMENT1);
								lastOffset = length;
								break loop;
							}
							break;
						case '[':
							if (i == offset) {
								addToken(i - lastOffset, token);
								token = Token.KEYWORD2;
								lastOffset = i;
							}
							break;
						case '=':
							addToken(i - lastOffset, Token.KEYWORD1);
							token = VALUE;
							lastOffset = i;
							break;
					}
					break;
				case Token.KEYWORD2:
					if (array[i] == ']') {
						addToken(i1 - lastOffset, token);
						token = Token.NULL;
						lastOffset = i1;
					}
					break;
				case VALUE:
					break;
				default:
					throw new InternalError("Invalid state: " + token);
			}
		}
		if (lastOffset != length) {
			addToken(length - lastOffset, Token.NULL);
		}
		return Token.NULL;
	}

	@Override
	public boolean supportsMultilineTokens() {
		return false;
	}
}
