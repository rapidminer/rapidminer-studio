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
package com.rapidminer.tools.expression.internal.function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.rapidminer.MacroHandler;
import com.rapidminer.tools.expression.Expression;
import com.rapidminer.tools.expression.ExpressionContext;
import com.rapidminer.tools.expression.ExpressionEvaluator;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.Function;
import com.rapidminer.tools.expression.FunctionDescription;
import com.rapidminer.tools.expression.FunctionInput;
import com.rapidminer.tools.expression.internal.antlr.AntlrParser;
import com.rapidminer.tools.expression.internal.function.process.MacroValue;
import com.rapidminer.tools.expression.internal.function.process.ParameterValue;
import com.rapidminer.tools.expression.internal.function.statistical.Random;


/**
 * Tests the results of {@link AntlrParser#parse(String)} for functions that need a process.
 *
 * @author Gisa Schaefer
 *
 */
public class AntlrParserProcessFunctionTest extends AntlrParserTest {

	protected static final Map<String, Function> FUNCTION_MAP;
	static {
		FUNCTION_MAP = new HashMap<>();

		addFunction(new Random(null));
		addFunction(new ParameterValue(null));
		MacroHandler handler = new MacroHandler(null);
		handler.addMacro("my macro", "my value");
		addFunction(new MacroValue(handler));
	}

	protected static void addFunction(Function function) {
		FUNCTION_MAP.put(function.getFunctionName(), function);
	}

	protected static final ExpressionContext FUNCTION_CONTEXT = new ExpressionContext() {

		@Override
		public Function getFunction(String functionName) {
			return FUNCTION_MAP.get(functionName);
		}

		@Override
		public ExpressionEvaluator getVariable(String variableName) {
			return null;
		}

		@Override
		public ExpressionEvaluator getDynamicVariable(String variableName) {
			return null;
		}

		@Override
		public ExpressionEvaluator getScopeConstant(String scopeName) {
			return null;
		}

		@Override
		public String getScopeString(String scopeName) {
			return null;
		}

		@Override
		public List<FunctionDescription> getFunctionDescriptions() {
			return null;
		}

		@Override
		public List<FunctionInput> getFunctionInputs() {
			return null;
		}

		@Override
		public ExpressionEvaluator getConstant(String constantName) {
			return null;
		}
	};

	@Override
	protected Expression getExpressionWithFunctionContext(String expression) throws ExpressionException {
		AntlrParser parser = new AntlrParser(FUNCTION_CONTEXT);
		return parser.parse(expression);
	}

	@Test
	public void randWithArgument() {
		try {
			Expression expression = getExpressionWithFunctionContext("rand(2015)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(0.6224847827770777, expression.evaluateNumerical(), 1e-15);
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void randWithArgumentWrongType() {
		try {
			getExpressionWithFunctionContext("rand(\"bla\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void randWithWrongArgumentDouble() {
		try {
			getExpressionWithFunctionContext("rand(0.234)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void randWithWrongNumberOfArguments() {
		try {
			getExpressionWithFunctionContext("rand(2,3)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void paramWitArgumentWrongType() {
		try {
			getExpressionWithFunctionContext("param(5,\"bla\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void randWithNoArgument() {
		try {
			getExpressionWithFunctionContext("param()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void paramWithWrongNumberOfArguments() {
		try {
			getExpressionWithFunctionContext("param(\"operator\",\"parameter\",\"blup\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void macroWithWrongNumberOfArguments() {
		try {
			getExpressionWithFunctionContext("macro(\"operator\",\"parameter\",\"blup\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void macroWithNoArgument() {
		try {
			getExpressionWithFunctionContext("macro()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void macroWitArgumentWrongType() {
		try {
			getExpressionWithFunctionContext("macro(\"my macro\",5)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void macroExisting() {
		try {
			Expression expression = getExpressionWithFunctionContext("macro(\"my macro\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("my value", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void macroExistingWithDefault() {
		try {
			Expression expression = getExpressionWithFunctionContext("macro(\"my macro\", \"default\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("my value", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void macroNotExisting() {
		try {
			getExpressionWithFunctionContext("macro(\"no macro\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void macroNotExistingWithDefault() {
		try {
			Expression expression = getExpressionWithFunctionContext("macro(\"no macro\", \"default\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("default", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

}
