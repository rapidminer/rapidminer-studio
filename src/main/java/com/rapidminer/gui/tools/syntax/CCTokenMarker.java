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
 * CCTokenMarker.java - C++ token marker Copyright (C) 1999 Slava Pestov
 * 
 * You may use and modify this package for any purpose. Redistribution is permitted, in both source
 * and binary form, provided that this notice remains intact in all source distributions of this
 * package.
 */

/**
 * C++ token marker.
 * 
 * @author Slava Pestov, Ingo Mierswa
 */
public class CCTokenMarker extends CTokenMarker {

	public CCTokenMarker() {
		super(true, getKeywords());
	}

	public static KeywordMap getKeywords() {
		if (ccKeywords == null) {
			KeywordMap ccKeywords = new KeywordMap(false);

			ccKeywords.add("and", Token.KEYWORD3);
			ccKeywords.add("and_eq", Token.KEYWORD3);
			ccKeywords.add("asm", Token.KEYWORD2); //
			ccKeywords.add("auto", Token.KEYWORD1); //
			ccKeywords.add("bitand", Token.KEYWORD3);
			ccKeywords.add("bitor", Token.KEYWORD3);
			ccKeywords.add("bool", Token.KEYWORD3);
			ccKeywords.add("break", Token.KEYWORD1); //
			ccKeywords.add("case", Token.KEYWORD1); //
			ccKeywords.add("catch", Token.KEYWORD1);
			ccKeywords.add("char", Token.KEYWORD3); //
			ccKeywords.add("class", Token.KEYWORD3);
			ccKeywords.add("compl", Token.KEYWORD3);
			ccKeywords.add("const", Token.KEYWORD1); //
			ccKeywords.add("const_cast", Token.KEYWORD3);
			ccKeywords.add("continue", Token.KEYWORD1); //
			ccKeywords.add("default", Token.KEYWORD1); //
			ccKeywords.add("delete", Token.KEYWORD1);
			ccKeywords.add("do", Token.KEYWORD1); //
			ccKeywords.add("double", Token.KEYWORD3); //
			ccKeywords.add("dynamic_cast", Token.KEYWORD3);
			ccKeywords.add("else", Token.KEYWORD1); //
			ccKeywords.add("enum", Token.KEYWORD3); //
			ccKeywords.add("explicit", Token.KEYWORD1);
			ccKeywords.add("export", Token.KEYWORD2);
			ccKeywords.add("extern", Token.KEYWORD2); //
			ccKeywords.add("false", Token.LITERAL2);
			ccKeywords.add("float", Token.KEYWORD3); //
			ccKeywords.add("for", Token.KEYWORD1); //
			ccKeywords.add("friend", Token.KEYWORD1);
			ccKeywords.add("goto", Token.KEYWORD1); //
			ccKeywords.add("if", Token.KEYWORD1); //
			ccKeywords.add("inline", Token.KEYWORD1);
			ccKeywords.add("int", Token.KEYWORD3); //
			ccKeywords.add("long", Token.KEYWORD3); //
			ccKeywords.add("mutable", Token.KEYWORD3);
			ccKeywords.add("namespace", Token.KEYWORD2);
			ccKeywords.add("new", Token.KEYWORD1);
			ccKeywords.add("not", Token.KEYWORD3);
			ccKeywords.add("not_eq", Token.KEYWORD3);
			ccKeywords.add("operator", Token.KEYWORD3);
			ccKeywords.add("or", Token.KEYWORD3);
			ccKeywords.add("or_eq", Token.KEYWORD3);
			ccKeywords.add("private", Token.KEYWORD1);
			ccKeywords.add("protected", Token.KEYWORD1);
			ccKeywords.add("public", Token.KEYWORD1);
			ccKeywords.add("register", Token.KEYWORD1);
			ccKeywords.add("reinterpret_cast", Token.KEYWORD3);
			ccKeywords.add("return", Token.KEYWORD1); //
			ccKeywords.add("short", Token.KEYWORD3); //
			ccKeywords.add("signed", Token.KEYWORD3); //
			ccKeywords.add("sizeof", Token.KEYWORD1); //
			ccKeywords.add("static", Token.KEYWORD1); //
			ccKeywords.add("static_cast", Token.KEYWORD3);
			ccKeywords.add("struct", Token.KEYWORD3); //
			ccKeywords.add("switch", Token.KEYWORD1); //
			ccKeywords.add("template", Token.KEYWORD3);
			ccKeywords.add("this", Token.LITERAL2);
			ccKeywords.add("throw", Token.KEYWORD1);
			ccKeywords.add("true", Token.LITERAL2);
			ccKeywords.add("try", Token.KEYWORD1);
			ccKeywords.add("typedef", Token.KEYWORD3); //
			ccKeywords.add("typeid", Token.KEYWORD3);
			ccKeywords.add("typename", Token.KEYWORD3);
			ccKeywords.add("union", Token.KEYWORD3); //
			ccKeywords.add("unsigned", Token.KEYWORD3); //
			ccKeywords.add("using", Token.KEYWORD2);
			ccKeywords.add("virtual", Token.KEYWORD1);
			ccKeywords.add("void", Token.KEYWORD1); //
			ccKeywords.add("volatile", Token.KEYWORD1); //
			ccKeywords.add("wchar_t", Token.KEYWORD3);
			ccKeywords.add("while", Token.KEYWORD1); //
			ccKeywords.add("xor", Token.KEYWORD3);
			ccKeywords.add("xor_eq", Token.KEYWORD3);

			// non ANSI keywords
			ccKeywords.add("NULL", Token.LITERAL2);

			CCTokenMarker.ccKeywords = ccKeywords;
		}
		return ccKeywords;
	}

	// private members
	private static KeywordMap ccKeywords;
}
