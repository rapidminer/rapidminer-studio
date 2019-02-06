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
 * JavaScriptTokenMarker.java - JavaScript token marker Copyright (C) 1999 Slava Pestov
 * 
 * You may use and modify this package for any purpose. Redistribution is permitted, in both source
 * and binary form, provided that this notice remains intact in all source distributions of this
 * package.
 */

/**
 * JavaScript token marker.
 * 
 * @author Slava Pestov, Ingo Mierswa
 */
public class JavaScriptTokenMarker extends CTokenMarker {

	public JavaScriptTokenMarker() {
		super(false, getKeywords());
	}

	public static KeywordMap getKeywords() {
		if (javaScriptKeywords == null) {
			KeywordMap javaScriptKeywords = new KeywordMap(false);
			javaScriptKeywords.add("function", Token.KEYWORD3);
			javaScriptKeywords.add("var", Token.KEYWORD3);
			javaScriptKeywords.add("else", Token.KEYWORD1);
			javaScriptKeywords.add("for", Token.KEYWORD1);
			javaScriptKeywords.add("if", Token.KEYWORD1);
			javaScriptKeywords.add("in", Token.KEYWORD1);
			javaScriptKeywords.add("new", Token.KEYWORD1);
			javaScriptKeywords.add("return", Token.KEYWORD1);
			javaScriptKeywords.add("while", Token.KEYWORD1);
			javaScriptKeywords.add("with", Token.KEYWORD1);
			javaScriptKeywords.add("break", Token.KEYWORD1);
			javaScriptKeywords.add("case", Token.KEYWORD1);
			javaScriptKeywords.add("continue", Token.KEYWORD1);
			javaScriptKeywords.add("default", Token.KEYWORD1);
			javaScriptKeywords.add("false", Token.LABEL);
			javaScriptKeywords.add("this", Token.LABEL);
			javaScriptKeywords.add("true", Token.LABEL);

			JavaScriptTokenMarker.javaScriptKeywords = javaScriptKeywords;
		}
		return javaScriptKeywords;
	}

	// private members
	private static KeywordMap javaScriptKeywords;
}
