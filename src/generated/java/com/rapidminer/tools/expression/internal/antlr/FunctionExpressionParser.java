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
// Generated from FunctionExpressionParser.g4 by ANTLR 4.5
package com.rapidminer.tools.expression.internal.antlr;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class FunctionExpressionParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.5", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		PLUS=1, MINUS=2, MULTIPLY=3, DIVIDE=4, MODULO=5, POWER=6, LESS=7, LEQ=8, 
		GREATER=9, GEQ=10, EQUALS=11, NOT_EQUALS=12, NOT=13, OR=14, AND=15, LPARENTHESIS=16, 
		RPARENTHESIS=17, COMMA=18, NAME=19, INTEGER=20, REAL=21, ATTRIBUTE=22, 
		STRING=23, SCOPE_CONSTANT=24, INDIRECT_SCOPE_CONSTANT=25, LSQUARE_BRACKET=26, 
		OPENING_QOUTES=27, SCOPE_OPEN=28, INDIRECT_SCOPE_OPEN=29, WHITESPACES=30, 
		SCOPE_CLOSE=31, RSQUARE_BRACKET=32, CLOSING_QUOTES=33;
	public static final int
		RULE_operationExp = 0, RULE_atomExp = 1, RULE_lowerExp = 2, RULE_function = 3, 
		RULE_attribute = 4, RULE_scopeConstant = 5, RULE_indirectScopeConstant = 6, 
		RULE_string = 7, RULE_variable = 8, RULE_real = 9, RULE_integer = 10;
	public static final String[] ruleNames = {
		"operationExp", "atomExp", "lowerExp", "function", "attribute", "scopeConstant", 
		"indirectScopeConstant", "string", "variable", "real", "integer"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'+'", "'-'", "'*'", "'/'", "'%'", "'^'", "'<'", "'<='", "'>'", 
		"'>='", "'=='", "'!='", "'!'", "'||'", "'&&'", "'('", "')'", "','", null, 
		null, null, null, null, null, null, "'['", null, "'%{'", "'#{'", null, 
		"'}'", "']'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "PLUS", "MINUS", "MULTIPLY", "DIVIDE", "MODULO", "POWER", "LESS", 
		"LEQ", "GREATER", "GEQ", "EQUALS", "NOT_EQUALS", "NOT", "OR", "AND", "LPARENTHESIS", 
		"RPARENTHESIS", "COMMA", "NAME", "INTEGER", "REAL", "ATTRIBUTE", "STRING", 
		"SCOPE_CONSTANT", "INDIRECT_SCOPE_CONSTANT", "LSQUARE_BRACKET", "OPENING_QOUTES", 
		"SCOPE_OPEN", "INDIRECT_SCOPE_OPEN", "WHITESPACES", "SCOPE_CLOSE", "RSQUARE_BRACKET", 
		"CLOSING_QUOTES"
	};
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

	@Override
	public String getGrammarFileName() { return "FunctionExpressionParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public FunctionExpressionParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class OperationExpContext extends ParserRuleContext {
		public Token op;
		public List<OperationExpContext> operationExp() {
			return getRuleContexts(OperationExpContext.class);
		}
		public OperationExpContext operationExp(int i) {
			return getRuleContext(OperationExpContext.class,i);
		}
		public TerminalNode NOT() { return getToken(FunctionExpressionParser.NOT, 0); }
		public TerminalNode PLUS() { return getToken(FunctionExpressionParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(FunctionExpressionParser.MINUS, 0); }
		public AtomExpContext atomExp() {
			return getRuleContext(AtomExpContext.class,0);
		}
		public TerminalNode POWER() { return getToken(FunctionExpressionParser.POWER, 0); }
		public TerminalNode MULTIPLY() { return getToken(FunctionExpressionParser.MULTIPLY, 0); }
		public TerminalNode DIVIDE() { return getToken(FunctionExpressionParser.DIVIDE, 0); }
		public TerminalNode MODULO() { return getToken(FunctionExpressionParser.MODULO, 0); }
		public TerminalNode LESS() { return getToken(FunctionExpressionParser.LESS, 0); }
		public TerminalNode LEQ() { return getToken(FunctionExpressionParser.LEQ, 0); }
		public TerminalNode GREATER() { return getToken(FunctionExpressionParser.GREATER, 0); }
		public TerminalNode GEQ() { return getToken(FunctionExpressionParser.GEQ, 0); }
		public TerminalNode EQUALS() { return getToken(FunctionExpressionParser.EQUALS, 0); }
		public TerminalNode NOT_EQUALS() { return getToken(FunctionExpressionParser.NOT_EQUALS, 0); }
		public TerminalNode AND() { return getToken(FunctionExpressionParser.AND, 0); }
		public TerminalNode OR() { return getToken(FunctionExpressionParser.OR, 0); }
		public OperationExpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operationExp; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).enterOperationExp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).exitOperationExp(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FunctionExpressionParserVisitor ) return ((FunctionExpressionParserVisitor<? extends T>)visitor).visitOperationExp(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OperationExpContext operationExp() throws RecognitionException {
		return operationExp(0);
	}

	private OperationExpContext operationExp(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		OperationExpContext _localctx = new OperationExpContext(_ctx, _parentState);
		OperationExpContext _prevctx = _localctx;
		int _startState = 0;
		enterRecursionRule(_localctx, 0, RULE_operationExp, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(28);
			switch (_input.LA(1)) {
			case NOT:
				{
				setState(23);
				((OperationExpContext)_localctx).op = match(NOT);
				setState(24);
				operationExp(10);
				}
				break;
			case PLUS:
			case MINUS:
				{
				setState(25);
				((OperationExpContext)_localctx).op = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==PLUS || _la==MINUS) ) {
					((OperationExpContext)_localctx).op = (Token)_errHandler.recoverInline(this);
				} else {
					consume();
				}
				setState(26);
				operationExp(9);
				}
				break;
			case LPARENTHESIS:
			case NAME:
			case INTEGER:
			case REAL:
			case ATTRIBUTE:
			case STRING:
			case SCOPE_CONSTANT:
			case INDIRECT_SCOPE_CONSTANT:
				{
				setState(27);
				atomExp();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(53);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(51);
					switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
					case 1:
						{
						_localctx = new OperationExpContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_operationExp);
						setState(30);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(31);
						((OperationExpContext)_localctx).op = match(POWER);
						setState(32);
						operationExp(8);
						}
						break;
					case 2:
						{
						_localctx = new OperationExpContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_operationExp);
						setState(33);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(34);
						((OperationExpContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << MULTIPLY) | (1L << DIVIDE) | (1L << MODULO))) != 0)) ) {
							((OperationExpContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(35);
						operationExp(8);
						}
						break;
					case 3:
						{
						_localctx = new OperationExpContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_operationExp);
						setState(36);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(37);
						((OperationExpContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==PLUS || _la==MINUS) ) {
							((OperationExpContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(38);
						operationExp(7);
						}
						break;
					case 4:
						{
						_localctx = new OperationExpContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_operationExp);
						setState(39);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(40);
						((OperationExpContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LESS) | (1L << LEQ) | (1L << GREATER) | (1L << GEQ))) != 0)) ) {
							((OperationExpContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(41);
						operationExp(6);
						}
						break;
					case 5:
						{
						_localctx = new OperationExpContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_operationExp);
						setState(42);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(43);
						((OperationExpContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==EQUALS || _la==NOT_EQUALS) ) {
							((OperationExpContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(44);
						operationExp(5);
						}
						break;
					case 6:
						{
						_localctx = new OperationExpContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_operationExp);
						setState(45);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(46);
						((OperationExpContext)_localctx).op = match(AND);
						setState(47);
						operationExp(4);
						}
						break;
					case 7:
						{
						_localctx = new OperationExpContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_operationExp);
						setState(48);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(49);
						((OperationExpContext)_localctx).op = match(OR);
						setState(50);
						operationExp(3);
						}
						break;
					}
					} 
				}
				setState(55);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class AtomExpContext extends ParserRuleContext {
		public FunctionContext function() {
			return getRuleContext(FunctionContext.class,0);
		}
		public AttributeContext attribute() {
			return getRuleContext(AttributeContext.class,0);
		}
		public ScopeConstantContext scopeConstant() {
			return getRuleContext(ScopeConstantContext.class,0);
		}
		public IndirectScopeConstantContext indirectScopeConstant() {
			return getRuleContext(IndirectScopeConstantContext.class,0);
		}
		public StringContext string() {
			return getRuleContext(StringContext.class,0);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public RealContext real() {
			return getRuleContext(RealContext.class,0);
		}
		public IntegerContext integer() {
			return getRuleContext(IntegerContext.class,0);
		}
		public LowerExpContext lowerExp() {
			return getRuleContext(LowerExpContext.class,0);
		}
		public AtomExpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_atomExp; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).enterAtomExp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).exitAtomExp(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FunctionExpressionParserVisitor ) return ((FunctionExpressionParserVisitor<? extends T>)visitor).visitAtomExp(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AtomExpContext atomExp() throws RecognitionException {
		AtomExpContext _localctx = new AtomExpContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_atomExp);
		try {
			setState(65);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(56);
				function();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(57);
				attribute();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(58);
				scopeConstant();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(59);
				indirectScopeConstant();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(60);
				string();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(61);
				variable();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(62);
				real();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(63);
				integer();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(64);
				lowerExp();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LowerExpContext extends ParserRuleContext {
		public TerminalNode LPARENTHESIS() { return getToken(FunctionExpressionParser.LPARENTHESIS, 0); }
		public OperationExpContext operationExp() {
			return getRuleContext(OperationExpContext.class,0);
		}
		public TerminalNode RPARENTHESIS() { return getToken(FunctionExpressionParser.RPARENTHESIS, 0); }
		public LowerExpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lowerExp; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).enterLowerExp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).exitLowerExp(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FunctionExpressionParserVisitor ) return ((FunctionExpressionParserVisitor<? extends T>)visitor).visitLowerExp(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LowerExpContext lowerExp() throws RecognitionException {
		LowerExpContext _localctx = new LowerExpContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_lowerExp);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(67);
			match(LPARENTHESIS);
			setState(68);
			operationExp(0);
			setState(69);
			match(RPARENTHESIS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(FunctionExpressionParser.NAME, 0); }
		public TerminalNode LPARENTHESIS() { return getToken(FunctionExpressionParser.LPARENTHESIS, 0); }
		public TerminalNode RPARENTHESIS() { return getToken(FunctionExpressionParser.RPARENTHESIS, 0); }
		public List<OperationExpContext> operationExp() {
			return getRuleContexts(OperationExpContext.class);
		}
		public OperationExpContext operationExp(int i) {
			return getRuleContext(OperationExpContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(FunctionExpressionParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(FunctionExpressionParser.COMMA, i);
		}
		public FunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_function; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).enterFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).exitFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FunctionExpressionParserVisitor ) return ((FunctionExpressionParserVisitor<? extends T>)visitor).visitFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionContext function() throws RecognitionException {
		FunctionContext _localctx = new FunctionContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_function);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(71);
			match(NAME);
			setState(72);
			match(LPARENTHESIS);
			setState(82);
			switch (_input.LA(1)) {
			case RPARENTHESIS:
				{
				}
				break;
			case PLUS:
			case MINUS:
			case NOT:
			case LPARENTHESIS:
			case NAME:
			case INTEGER:
			case REAL:
			case ATTRIBUTE:
			case STRING:
			case SCOPE_CONSTANT:
			case INDIRECT_SCOPE_CONSTANT:
				{
				setState(74);
				operationExp(0);
				setState(79);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(75);
					match(COMMA);
					setState(76);
					operationExp(0);
					}
					}
					setState(81);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(84);
			match(RPARENTHESIS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AttributeContext extends ParserRuleContext {
		public TerminalNode ATTRIBUTE() { return getToken(FunctionExpressionParser.ATTRIBUTE, 0); }
		public AttributeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attribute; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).enterAttribute(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).exitAttribute(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FunctionExpressionParserVisitor ) return ((FunctionExpressionParserVisitor<? extends T>)visitor).visitAttribute(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AttributeContext attribute() throws RecognitionException {
		AttributeContext _localctx = new AttributeContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_attribute);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(86);
			match(ATTRIBUTE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ScopeConstantContext extends ParserRuleContext {
		public TerminalNode SCOPE_CONSTANT() { return getToken(FunctionExpressionParser.SCOPE_CONSTANT, 0); }
		public ScopeConstantContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_scopeConstant; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).enterScopeConstant(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).exitScopeConstant(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FunctionExpressionParserVisitor ) return ((FunctionExpressionParserVisitor<? extends T>)visitor).visitScopeConstant(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ScopeConstantContext scopeConstant() throws RecognitionException {
		ScopeConstantContext _localctx = new ScopeConstantContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_scopeConstant);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(88);
			match(SCOPE_CONSTANT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IndirectScopeConstantContext extends ParserRuleContext {
		public TerminalNode INDIRECT_SCOPE_CONSTANT() { return getToken(FunctionExpressionParser.INDIRECT_SCOPE_CONSTANT, 0); }
		public IndirectScopeConstantContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_indirectScopeConstant; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).enterIndirectScopeConstant(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).exitIndirectScopeConstant(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FunctionExpressionParserVisitor ) return ((FunctionExpressionParserVisitor<? extends T>)visitor).visitIndirectScopeConstant(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IndirectScopeConstantContext indirectScopeConstant() throws RecognitionException {
		IndirectScopeConstantContext _localctx = new IndirectScopeConstantContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_indirectScopeConstant);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(90);
			match(INDIRECT_SCOPE_CONSTANT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StringContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(FunctionExpressionParser.STRING, 0); }
		public StringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_string; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).enterString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).exitString(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FunctionExpressionParserVisitor ) return ((FunctionExpressionParserVisitor<? extends T>)visitor).visitString(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StringContext string() throws RecognitionException {
		StringContext _localctx = new StringContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_string);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(92);
			match(STRING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(FunctionExpressionParser.NAME, 0); }
		public VariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).enterVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).exitVariable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FunctionExpressionParserVisitor ) return ((FunctionExpressionParserVisitor<? extends T>)visitor).visitVariable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableContext variable() throws RecognitionException {
		VariableContext _localctx = new VariableContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_variable);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(94);
			match(NAME);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RealContext extends ParserRuleContext {
		public TerminalNode REAL() { return getToken(FunctionExpressionParser.REAL, 0); }
		public RealContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_real; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).enterReal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).exitReal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FunctionExpressionParserVisitor ) return ((FunctionExpressionParserVisitor<? extends T>)visitor).visitReal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RealContext real() throws RecognitionException {
		RealContext _localctx = new RealContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_real);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(96);
			match(REAL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IntegerContext extends ParserRuleContext {
		public TerminalNode INTEGER() { return getToken(FunctionExpressionParser.INTEGER, 0); }
		public IntegerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_integer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).enterInteger(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FunctionExpressionParserListener ) ((FunctionExpressionParserListener)listener).exitInteger(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FunctionExpressionParserVisitor ) return ((FunctionExpressionParserVisitor<? extends T>)visitor).visitInteger(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntegerContext integer() throws RecognitionException {
		IntegerContext _localctx = new IntegerContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_integer);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(98);
			match(INTEGER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 0:
			return operationExp_sempred((OperationExpContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean operationExp_sempred(OperationExpContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 8);
		case 1:
			return precpred(_ctx, 7);
		case 2:
			return precpred(_ctx, 6);
		case 3:
			return precpred(_ctx, 5);
		case 4:
			return precpred(_ctx, 4);
		case 5:
			return precpred(_ctx, 3);
		case 6:
			return precpred(_ctx, 2);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3#g\4\2\t\2\4\3\t\3"+
		"\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t\13\4\f"+
		"\t\f\3\2\3\2\3\2\3\2\3\2\3\2\5\2\37\n\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3"+
		"\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\7\2\66\n\2\f\2"+
		"\16\29\13\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\5\3D\n\3\3\4\3\4\3\4\3"+
		"\4\3\5\3\5\3\5\3\5\3\5\3\5\7\5P\n\5\f\5\16\5S\13\5\5\5U\n\5\3\5\3\5\3"+
		"\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\f\2\3\2\r\2"+
		"\4\6\b\n\f\16\20\22\24\26\2\6\3\2\3\4\3\2\5\7\3\2\t\f\3\2\r\16n\2\36\3"+
		"\2\2\2\4C\3\2\2\2\6E\3\2\2\2\bI\3\2\2\2\nX\3\2\2\2\fZ\3\2\2\2\16\\\3\2"+
		"\2\2\20^\3\2\2\2\22`\3\2\2\2\24b\3\2\2\2\26d\3\2\2\2\30\31\b\2\1\2\31"+
		"\32\7\17\2\2\32\37\5\2\2\f\33\34\t\2\2\2\34\37\5\2\2\13\35\37\5\4\3\2"+
		"\36\30\3\2\2\2\36\33\3\2\2\2\36\35\3\2\2\2\37\67\3\2\2\2 !\f\n\2\2!\""+
		"\7\b\2\2\"\66\5\2\2\n#$\f\t\2\2$%\t\3\2\2%\66\5\2\2\n&\'\f\b\2\2\'(\t"+
		"\2\2\2(\66\5\2\2\t)*\f\7\2\2*+\t\4\2\2+\66\5\2\2\b,-\f\6\2\2-.\t\5\2\2"+
		".\66\5\2\2\7/\60\f\5\2\2\60\61\7\21\2\2\61\66\5\2\2\6\62\63\f\4\2\2\63"+
		"\64\7\20\2\2\64\66\5\2\2\5\65 \3\2\2\2\65#\3\2\2\2\65&\3\2\2\2\65)\3\2"+
		"\2\2\65,\3\2\2\2\65/\3\2\2\2\65\62\3\2\2\2\669\3\2\2\2\67\65\3\2\2\2\67"+
		"8\3\2\2\28\3\3\2\2\29\67\3\2\2\2:D\5\b\5\2;D\5\n\6\2<D\5\f\7\2=D\5\16"+
		"\b\2>D\5\20\t\2?D\5\22\n\2@D\5\24\13\2AD\5\26\f\2BD\5\6\4\2C:\3\2\2\2"+
		"C;\3\2\2\2C<\3\2\2\2C=\3\2\2\2C>\3\2\2\2C?\3\2\2\2C@\3\2\2\2CA\3\2\2\2"+
		"CB\3\2\2\2D\5\3\2\2\2EF\7\22\2\2FG\5\2\2\2GH\7\23\2\2H\7\3\2\2\2IJ\7\25"+
		"\2\2JT\7\22\2\2KU\3\2\2\2LQ\5\2\2\2MN\7\24\2\2NP\5\2\2\2OM\3\2\2\2PS\3"+
		"\2\2\2QO\3\2\2\2QR\3\2\2\2RU\3\2\2\2SQ\3\2\2\2TK\3\2\2\2TL\3\2\2\2UV\3"+
		"\2\2\2VW\7\23\2\2W\t\3\2\2\2XY\7\30\2\2Y\13\3\2\2\2Z[\7\32\2\2[\r\3\2"+
		"\2\2\\]\7\33\2\2]\17\3\2\2\2^_\7\31\2\2_\21\3\2\2\2`a\7\25\2\2a\23\3\2"+
		"\2\2bc\7\27\2\2c\25\3\2\2\2de\7\26\2\2e\27\3\2\2\2\b\36\65\67CQT";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
