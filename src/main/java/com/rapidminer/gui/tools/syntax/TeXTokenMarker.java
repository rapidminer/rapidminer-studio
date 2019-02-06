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
 * TeXTokenMarker.java - TeX/LaTeX/AMS-TeX token marker Copyright (C) 1998 Slava Pestov
 * 
 * You may use and modify this package for any purpose. Redistribution is permitted, in both source
 * and binary form, provided that this notice remains intact in all source distributions of this
 * package.
 */

import javax.swing.text.Segment;


/**
 * TeX token marker.
 * 
 * @author Slava Pestov, Ingo Mierswa
 */
public class TeXTokenMarker extends TokenMarker {

	// public members
	public static final byte BDFORMULA = Token.INTERNAL_FIRST;

	public static final byte EDFORMULA = (byte) (Token.INTERNAL_FIRST + 1);

	@Override
	public byte markTokensImpl(byte token, Segment line, int lineIndex) {
		char[] array = line.array;
		int offset = line.offset;
		int lastOffset = offset;
		int length = line.count + offset;
		boolean backslash = false;
		loop: for (int i = offset; i < length; i++) {
			int i1 = (i + 1);

			char c = array[i];
			// if a backslash is followed immediately
			// by a non-alpha character, the command at
			// the non-alpha char. If we have a backslash,
			// some text, and then a non-alpha char,
			// the command ends before the non-alpha char.
			if (Character.isLetter(c)) {
				backslash = false;
			} else {
				if (backslash) {
					// \<non alpha>
					// we skip over this character,
					// hence the `continue'
					backslash = false;
					if (token == Token.KEYWORD2 || token == EDFORMULA) {
						token = Token.KEYWORD2;
					}
					addToken(i1 - lastOffset, token);
					lastOffset = i1;
					if (token == Token.KEYWORD1) {
						token = Token.NULL;
					}
					continue;
				} else {
					// \blah<non alpha>
					// we leave the character in
					// the stream, and it's not
					// part of the command token
					if (token == BDFORMULA || token == EDFORMULA) {
						token = Token.KEYWORD2;
					}
					addToken(i - lastOffset, token);
					if (token == Token.KEYWORD1) {
						token = Token.NULL;
					}
					lastOffset = i;
				}
			}
			switch (c) {
				case '%':
					if (backslash) {
						backslash = false;
						break;
					}
					addToken(i - lastOffset, token);
					addToken(length - i, Token.COMMENT1);
					lastOffset = length;
					break loop;
				case '\\':
					backslash = true;
					if (token == Token.NULL) {
						token = Token.KEYWORD1;
						addToken(i - lastOffset, Token.NULL);
						lastOffset = i;
					}
					break;
				case '$':
					backslash = false;
					if (token == Token.NULL) // singe $
					{
						token = Token.KEYWORD2;
						addToken(i - lastOffset, Token.NULL);
						lastOffset = i;
					} else if (token == Token.KEYWORD1) // \...$
					{
						token = Token.KEYWORD2;
						addToken(i - lastOffset, Token.KEYWORD1);
						lastOffset = i;
					} else if (token == Token.KEYWORD2) // $$aaa
					{
						if (i - lastOffset == 1 && array[i - 1] == '$') {
							token = BDFORMULA;
							break;
						}
						token = Token.NULL;
						addToken(i1 - lastOffset, Token.KEYWORD2);
						lastOffset = i1;
					} else if (token == BDFORMULA) // $$aaa$
					{
						token = EDFORMULA;
					} else if (token == EDFORMULA) // $$aaa$$
					{
						token = Token.NULL;
						addToken(i1 - lastOffset, Token.KEYWORD2);
						lastOffset = i1;
					}
					break;
			}
		}
		if (lastOffset != length) {
			addToken(length - lastOffset, token == BDFORMULA || token == EDFORMULA ? Token.KEYWORD2 : token);
		}
		return (token != Token.KEYWORD1 ? token : Token.NULL);
	}
}
