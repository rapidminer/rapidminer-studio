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
 * JavaTokenMarker.java - Java token marker Copyright (C) 1999 Slava Pestov
 * 
 * You may use and modify this package for any purpose. Redistribution is permitted, in both source
 * and binary form, provided that this notice remains intact in all source distributions of this
 * package.
 */

/**
 * Java token marker.
 * 
 * @author Slava Pestov, Ingo Mierswa
 */
public class JavaTokenMarker extends CTokenMarker {

	public JavaTokenMarker() {
		super(false, getKeywords());
	}

	public static KeywordMap getKeywords() {
		if (javaKeywords == null) {
			KeywordMap javaKeywords = new KeywordMap(false);
			javaKeywords.add("package", Token.KEYWORD2);
			javaKeywords.add("import", Token.KEYWORD2);
			javaKeywords.add("byte", Token.KEYWORD3);
			javaKeywords.add("char", Token.KEYWORD3);
			javaKeywords.add("short", Token.KEYWORD3);
			javaKeywords.add("int", Token.KEYWORD3);
			javaKeywords.add("long", Token.KEYWORD3);
			javaKeywords.add("float", Token.KEYWORD3);
			javaKeywords.add("double", Token.KEYWORD3);
			javaKeywords.add("boolean", Token.KEYWORD3);
			javaKeywords.add("void", Token.KEYWORD3);
			javaKeywords.add("class", Token.KEYWORD3);
			javaKeywords.add("interface", Token.KEYWORD3);
			javaKeywords.add("abstract", Token.KEYWORD1);
			javaKeywords.add("final", Token.KEYWORD1);
			javaKeywords.add("private", Token.KEYWORD1);
			javaKeywords.add("protected", Token.KEYWORD1);
			javaKeywords.add("public", Token.KEYWORD1);
			javaKeywords.add("static", Token.KEYWORD1);
			javaKeywords.add("synchronized", Token.KEYWORD1);
			javaKeywords.add("native", Token.KEYWORD1);
			javaKeywords.add("volatile", Token.KEYWORD1);
			javaKeywords.add("transient", Token.KEYWORD1);
			javaKeywords.add("break", Token.KEYWORD1);
			javaKeywords.add("case", Token.KEYWORD1);
			javaKeywords.add("continue", Token.KEYWORD1);
			javaKeywords.add("default", Token.KEYWORD1);
			javaKeywords.add("do", Token.KEYWORD1);
			javaKeywords.add("else", Token.KEYWORD1);
			javaKeywords.add("for", Token.KEYWORD1);
			javaKeywords.add("if", Token.KEYWORD1);
			javaKeywords.add("instanceof", Token.KEYWORD1);
			javaKeywords.add("new", Token.KEYWORD1);
			javaKeywords.add("return", Token.KEYWORD1);
			javaKeywords.add("switch", Token.KEYWORD1);
			javaKeywords.add("while", Token.KEYWORD1);
			javaKeywords.add("throw", Token.KEYWORD1);
			javaKeywords.add("try", Token.KEYWORD1);
			javaKeywords.add("catch", Token.KEYWORD1);
			javaKeywords.add("extends", Token.KEYWORD1);
			javaKeywords.add("finally", Token.KEYWORD1);
			javaKeywords.add("implements", Token.KEYWORD1);
			javaKeywords.add("throws", Token.KEYWORD1);
			javaKeywords.add("this", Token.LITERAL2);
			javaKeywords.add("null", Token.LITERAL2);
			javaKeywords.add("super", Token.LITERAL2);
			javaKeywords.add("true", Token.LITERAL2);
			javaKeywords.add("false", Token.LITERAL2);

			JavaTokenMarker.javaKeywords = javaKeywords;
		}
		return javaKeywords;
	}

	// private members
	private static KeywordMap javaKeywords;
}
