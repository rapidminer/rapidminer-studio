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
// Generated from FunctionExpressionLexer.g4 by ANTLR 4.5
package com.rapidminer.tools.expression.internal.antlr;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RuntimeMetaData;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.VocabularyImpl;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;


@SuppressWarnings({ "all", "warnings", "unchecked", "unused", "cast" })
public class FunctionExpressionLexer extends Lexer {

	static {
		RuntimeMetaData.checkVersion("4.5", RuntimeMetaData.VERSION);
	}

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache = new PredictionContextCache();
	public static final int PLUS = 1, MINUS = 2, MULTIPLY = 3, DIVIDE = 4, MODULO = 5, POWER = 6, LESS = 7, LEQ = 8,
			GREATER = 9, GEQ = 10, EQUALS = 11, NOT_EQUALS = 12, NOT = 13, OR = 14, AND = 15, LPARENTHESIS = 16,
			RPARENTHESIS = 17, COMMA = 18, NAME = 19, INTEGER = 20, REAL = 21, ATTRIBUTE = 22, STRING = 23,
			SCOPE_CONSTANT = 24, INDIRECT_SCOPE_CONSTANT = 25, LSQUARE_BRACKET = 26, OPENING_QOUTES = 27, SCOPE_OPEN = 28,
			INDIRECT_SCOPE_OPEN = 29, WHITESPACES = 30, SCOPE_CLOSE = 31, RSQUARE_BRACKET = 32, CLOSING_QUOTES = 33;
	public static final int INSIDE_SCOPE = 1;
	public static final int INSIDE_ATTRIBUTE = 2;
	public static final int INSIDE_STRING = 3;
	public static String[] modeNames = { "DEFAULT_MODE", "INSIDE_SCOPE", "INSIDE_ATTRIBUTE", "INSIDE_STRING" };

	public static final String[] ruleNames = { "PLUS", "MINUS", "MULTIPLY", "DIVIDE", "MODULO", "POWER", "LESS", "LEQ",
			"GREATER", "GEQ", "EQUALS", "NOT_EQUALS", "NOT", "OR", "AND", "LPARENTHESIS", "RPARENTHESIS", "COMMA", "NAME",
			"INTEGER", "REAL", "DIGITS", "EXPONENT", "ATTRIBUTE", "STRING", "SCOPE_CONSTANT", "INDIRECT_SCOPE_CONSTANT",
			"INSIDE_ATTRIBUTE", "INSIDE_SCOPE", "INSIDE_STRING", "UNICODE", "UNICODE_CHAR", "LSQUARE_BRACKET",
			"OPENING_QOUTES", "SCOPE_OPEN", "INDIRECT_SCOPE_OPEN", "WHITESPACES", "SCOPE_CLOSE", "RSQUARE_BRACKET",
			"CLOSING_QUOTES" };

	private static final String[] _LITERAL_NAMES = { null, "'+'", "'-'", "'*'", "'/'", "'%'", "'^'", "'<'", "'<='", "'>'",
			"'>='", "'=='", "'!='", "'!'", "'||'", "'&&'", "'('", "')'", "','", null, null, null, null, null, null, null,
			"'['", null, "'%{'", "'#{'", null, "'}'", "']'" };
	private static final String[] _SYMBOLIC_NAMES = { null, "PLUS", "MINUS", "MULTIPLY", "DIVIDE", "MODULO", "POWER",
			"LESS", "LEQ", "GREATER", "GEQ", "EQUALS", "NOT_EQUALS", "NOT", "OR", "AND", "LPARENTHESIS", "RPARENTHESIS",
			"COMMA", "NAME", "INTEGER", "REAL", "ATTRIBUTE", "STRING", "SCOPE_CONSTANT", "INDIRECT_SCOPE_CONSTANT",
			"LSQUARE_BRACKET", "OPENING_QOUTES", "SCOPE_OPEN", "INDIRECT_SCOPE_OPEN", "WHITESPACES", "SCOPE_CLOSE",
			"RSQUARE_BRACKET", "CLOSING_QUOTES" };
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override
	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	public FunctionExpressionLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
	}

	@Override
	public String getGrammarFileName() {
		return "FunctionExpressionLexer.g4";
	}

	@Override
	public String[] getRuleNames() {
		return ruleNames;
	}

	@Override
	public String getSerializedATN() {
		return _serializedATN;
	}

	@Override
	public String[] getModeNames() {
		return modeNames;
	}

	@Override
	public ATN getATN() {
		return _ATN;
	}

	public static final String _serializedATN = "\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2#\u00fa\b\1\b\1\b"
			+ "\1\b\1\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t"
			+ "\4\n\t\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21"
			+ "\t\21\4\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30"
			+ "\t\30\4\31\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37"
			+ "\t\37\4 \t \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)"
			+ "\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\t\3"
			+ "\n\3\n\3\13\3\13\3\13\3\f\3\f\3\f\3\r\3\r\3\r\3\16\3\16\3\17\3\17\3\17"
			+ "\3\20\3\20\3\20\3\21\3\21\3\22\3\22\3\23\3\23\3\24\6\24\u0082\n\24\r\24"
			+ "\16\24\u0083\3\24\7\24\u0087\n\24\f\24\16\24\u008a\13\24\3\25\3\25\3\26"
			+ "\3\26\3\26\5\26\u0091\n\26\3\26\3\26\5\26\u0095\n\26\3\26\5\26\u0098\n"
			+ "\26\3\27\6\27\u009b\n\27\r\27\16\27\u009c\3\30\3\30\5\30\u00a1\n\30\3"
			+ "\30\3\30\3\31\3\31\3\31\3\31\3\32\3\32\3\32\3\32\3\33\3\33\3\33\3\33\3"
			+ "\34\3\34\3\34\3\34\3\35\3\35\3\35\6\35\u00b8\n\35\r\35\16\35\u00b9\3\36"
			+ "\3\36\3\36\6\36\u00bf\n\36\r\36\16\36\u00c0\3\37\3\37\3\37\3\37\3\37\7"
			+ "\37\u00c8\n\37\f\37\16\37\u00cb\13\37\3 \3 \3 \3 \3 \3 \3 \3!\3!\3\"\3"
			+ "\"\3\"\3\"\3#\3#\3#\3#\3$\3$\3$\3$\3$\3%\3%\3%\3%\3%\3&\6&\u00e9\n&\r"
			+ "&\16&\u00ea\3&\3&\3\'\3\'\3\'\3\'\3(\3(\3(\3(\3)\3)\3)\3)\2\2*\6\3\b\4"
			+ "\n\5\f\6\16\7\20\b\22\t\24\n\26\13\30\f\32\r\34\16\36\17 \20\"\21$\22"
			+ "&\23(\24*\25,\26.\27\60\2\62\2\64\30\66\318\32:\33<\2>\2@\2B\2D\2F\34"
			+ "H\35J\36L\37N P!R\"T#\6\2\3\4\5\r\4\2C\\c|\6\2\62;C\\aac|\4\2GGgg\4\2"
			+ "--//\4\2\13\f]_\6\2\13\f^^}}\177\177\5\2^^}}\177\177\4\2$$^^\4\2\13\f"
			+ "\17\17\5\2\62;CHch\5\2\13\f\17\17\"\"\u00ff\2\6\3\2\2\2\2\b\3\2\2\2\2"
			+ "\n\3\2\2\2\2\f\3\2\2\2\2\16\3\2\2\2\2\20\3\2\2\2\2\22\3\2\2\2\2\24\3\2"
			+ "\2\2\2\26\3\2\2\2\2\30\3\2\2\2\2\32\3\2\2\2\2\34\3\2\2\2\2\36\3\2\2\2"
			+ "\2 \3\2\2\2\2\"\3\2\2\2\2$\3\2\2\2\2&\3\2\2\2\2(\3\2\2\2\2*\3\2\2\2\2"
			+ ",\3\2\2\2\2.\3\2\2\2\2\64\3\2\2\2\2\66\3\2\2\2\28\3\2\2\2\2:\3\2\2\2\2"
			+ "F\3\2\2\2\2H\3\2\2\2\2J\3\2\2\2\2L\3\2\2\2\2N\3\2\2\2\3P\3\2\2\2\4R\3"
			+ "\2\2\2\5T\3\2\2\2\6V\3\2\2\2\bX\3\2\2\2\nZ\3\2\2\2\f\\\3\2\2\2\16^\3\2"
			+ "\2\2\20`\3\2\2\2\22b\3\2\2\2\24d\3\2\2\2\26g\3\2\2\2\30i\3\2\2\2\32l\3"
			+ "\2\2\2\34o\3\2\2\2\36r\3\2\2\2 t\3\2\2\2\"w\3\2\2\2$z\3\2\2\2&|\3\2\2"
			+ "\2(~\3\2\2\2*\u0081\3\2\2\2,\u008b\3\2\2\2.\u0094\3\2\2\2\60\u009a\3\2"
			+ "\2\2\62\u009e\3\2\2\2\64\u00a4\3\2\2\2\66\u00a8\3\2\2\28\u00ac\3\2\2\2"
			+ ":\u00b0\3\2\2\2<\u00b7\3\2\2\2>\u00be\3\2\2\2@\u00c9\3\2\2\2B\u00cc\3"
			+ "\2\2\2D\u00d3\3\2\2\2F\u00d5\3\2\2\2H\u00d9\3\2\2\2J\u00dd\3\2\2\2L\u00e2"
			+ "\3\2\2\2N\u00e8\3\2\2\2P\u00ee\3\2\2\2R\u00f2\3\2\2\2T\u00f6\3\2\2\2V"
			+ "W\7-\2\2W\7\3\2\2\2XY\7/\2\2Y\t\3\2\2\2Z[\7,\2\2[\13\3\2\2\2\\]\7\61\2"
			+ "\2]\r\3\2\2\2^_\7\'\2\2_\17\3\2\2\2`a\7`\2\2a\21\3\2\2\2bc\7>\2\2c\23"
			+ "\3\2\2\2de\7>\2\2ef\7?\2\2f\25\3\2\2\2gh\7@\2\2h\27\3\2\2\2ij\7@\2\2j"
			+ "k\7?\2\2k\31\3\2\2\2lm\7?\2\2mn\7?\2\2n\33\3\2\2\2op\7#\2\2pq\7?\2\2q"
			+ "\35\3\2\2\2rs\7#\2\2s\37\3\2\2\2tu\7~\2\2uv\7~\2\2v!\3\2\2\2wx\7(\2\2"
			+ "xy\7(\2\2y#\3\2\2\2z{\7*\2\2{%\3\2\2\2|}\7+\2\2}\'\3\2\2\2~\177\7.\2\2"
			+ "\177)\3\2\2\2\u0080\u0082\t\2\2\2\u0081\u0080\3\2\2\2\u0082\u0083\3\2"
			+ "\2\2\u0083\u0081\3\2\2\2\u0083\u0084\3\2\2\2\u0084\u0088\3\2\2\2\u0085"
			+ "\u0087\t\3\2\2\u0086\u0085\3\2\2\2\u0087\u008a\3\2\2\2\u0088\u0086\3\2"
			+ "\2\2\u0088\u0089\3\2\2\2\u0089+\3\2\2\2\u008a\u0088\3\2\2\2\u008b\u008c"
			+ "\5\60\27\2\u008c-\3\2\2\2\u008d\u0090\5\60\27\2\u008e\u008f\7\60\2\2\u008f"
			+ "\u0091\5\60\27\2\u0090\u008e\3\2\2\2\u0090\u0091\3\2\2\2\u0091\u0095\3"
			+ "\2\2\2\u0092\u0093\7\60\2\2\u0093\u0095\5\60\27\2\u0094\u008d\3\2\2\2"
			+ "\u0094\u0092\3\2\2\2\u0095\u0097\3\2\2\2\u0096\u0098\5\62\30\2\u0097\u0096"
			+ "\3\2\2\2\u0097\u0098\3\2\2\2\u0098/\3\2\2\2\u0099\u009b\4\62;\2\u009a"
			+ "\u0099\3\2\2\2\u009b\u009c\3\2\2\2\u009c\u009a\3\2\2\2\u009c\u009d\3\2"
			+ "\2\2\u009d\61\3\2\2\2\u009e\u00a0\t\4\2\2\u009f\u00a1\t\5\2\2\u00a0\u009f"
			+ "\3\2\2\2\u00a0\u00a1\3\2\2\2\u00a1\u00a2\3\2\2\2\u00a2\u00a3\5\60\27\2"
			+ "\u00a3\63\3\2\2\2\u00a4\u00a5\5F\"\2\u00a5\u00a6\5<\35\2\u00a6\u00a7\5"
			+ "R(\2\u00a7\65\3\2\2\2\u00a8\u00a9\5H#\2\u00a9\u00aa\5@\37\2\u00aa\u00ab"
			+ "\5T)\2\u00ab\67\3\2\2\2\u00ac\u00ad\5J$\2\u00ad\u00ae\5>\36\2\u00ae\u00af"
			+ "\5P\'\2\u00af9\3\2\2\2\u00b0\u00b1\5L%\2\u00b1\u00b2\5>\36\2\u00b2\u00b3"
			+ "\5P\'\2\u00b3;\3\2\2\2\u00b4\u00b8\n\6\2\2\u00b5\u00b6\7^\2\2\u00b6\u00b8"
			+ "\4]_\2\u00b7\u00b4\3\2\2\2\u00b7\u00b5\3\2\2\2\u00b8\u00b9\3\2\2\2\u00b9"
			+ "\u00b7\3\2\2\2\u00b9\u00ba\3\2\2\2\u00ba=\3\2\2\2\u00bb\u00bf\n\7\2\2"
			+ "\u00bc\u00bd\7^\2\2\u00bd\u00bf\t\b\2\2\u00be\u00bb\3\2\2\2\u00be\u00bc"
			+ "\3\2\2\2\u00bf\u00c0\3\2\2\2\u00c0\u00be\3\2\2\2\u00c0\u00c1\3\2\2\2\u00c1"
			+ "?\3\2\2\2\u00c2\u00c8\n\t\2\2\u00c3\u00c8\t\n\2\2\u00c4\u00c5\7^\2\2\u00c5"
			+ "\u00c8\t\t\2\2\u00c6\u00c8\5B \2\u00c7\u00c2\3\2\2\2\u00c7\u00c3\3\2\2"
			+ "\2\u00c7\u00c4\3\2\2\2\u00c7\u00c6\3\2\2\2\u00c8\u00cb\3\2\2\2\u00c9\u00c7"
			+ "\3\2\2\2\u00c9\u00ca\3\2\2\2\u00caA\3\2\2\2\u00cb\u00c9\3\2\2\2\u00cc"
			+ "\u00cd\7^\2\2\u00cd\u00ce\7w\2\2\u00ce\u00cf\5D!\2\u00cf\u00d0\5D!\2\u00d0"
			+ "\u00d1\5D!\2\u00d1\u00d2\5D!\2\u00d2C\3\2\2\2\u00d3\u00d4\t\13\2\2\u00d4"
			+ "E\3\2\2\2\u00d5\u00d6\7]\2\2\u00d6\u00d7\3\2\2\2\u00d7\u00d8\b\"\2\2\u00d8"
			+ "G\3\2\2\2\u00d9\u00da\7$\2\2\u00da\u00db\3\2\2\2\u00db\u00dc\b#\3\2\u00dc"
			+ "I\3\2\2\2\u00dd\u00de\7\'\2\2\u00de\u00df\7}\2\2\u00df\u00e0\3\2\2\2\u00e0"
			+ "\u00e1\b$\4\2\u00e1K\3\2\2\2\u00e2\u00e3\7%\2\2\u00e3\u00e4\7}\2\2\u00e4"
			+ "\u00e5\3\2\2\2\u00e5\u00e6\b%\4\2\u00e6M\3\2\2\2\u00e7\u00e9\t\f\2\2\u00e8"
			+ "\u00e7\3\2\2\2\u00e9\u00ea\3\2\2\2\u00ea\u00e8\3\2\2\2\u00ea\u00eb\3\2"
			+ "\2\2\u00eb\u00ec\3\2\2\2\u00ec\u00ed\b&\5\2\u00edO\3\2\2\2\u00ee\u00ef"
			+ "\7\177\2\2\u00ef\u00f0\3\2\2\2\u00f0\u00f1\b\'\6\2\u00f1Q\3\2\2\2\u00f2"
			+ "\u00f3\7_\2\2\u00f3\u00f4\3\2\2\2\u00f4\u00f5\b(\6\2\u00f5S\3\2\2\2\u00f6"
			+ "\u00f7\7$\2\2\u00f7\u00f8\3\2\2\2\u00f8\u00f9\b)\6\2\u00f9U\3\2\2\2\24"
			+ "\2\3\4\5\u0083\u0088\u0090\u0094\u0097\u009c\u00a0\u00b7\u00b9\u00be\u00c0"
			+ "\u00c7\u00c9\u00ea\7\4\4\2\4\5\2\4\3\2\b\2\2\4\2\2";
	public static final ATN _ATN = new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
