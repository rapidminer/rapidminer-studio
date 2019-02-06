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

import org.junit.Test;

import com.rapidminer.tools.expression.Expression;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.internal.antlr.AntlrParser;


/**
 * Tests the results of {@link AntlrParser#parse(String)} for String transformation functions.
 *
 * @author David Arnu
 *
 */
public class AntlrParserStringTransformationTest extends AntlrParserTest {

	// concat
	@Test
	public void concatOne() {
		try {
			Expression expression = getExpressionWithFunctionContext("concat(\"abc\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("abc", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void concatTwo() {
		try {
			Expression expression = getExpressionWithFunctionContext("concat(\"abc\", \"def\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("abcdef", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void concatWrongFirstType() {
		try {
			getExpressionWithFunctionContext(" concat(5, \"abc\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void concatWrongSecondType() {
		try {
			getExpressionWithFunctionContext(" concat(\"abc\",5)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void concatEmptyArguments() {
		try {
			Expression expression = getExpressionWithFunctionContext("concat()");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void concatEmptyString() {
		try {
			Expression expression = getExpressionWithFunctionContext("concat(\"\", \"abc\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("abc", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void concatMissingValue() {
		try {
			Expression expression = getExpressionWithFunctionContext("concat(MISSING_NOMINAL, \"abc\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("abc", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	// replaceAll
	@Test
	public void replaceAllBasic() {
		try {
			Expression expression = getExpressionWithFunctionContext("replaceAll(\"abcd\", \"[ac]\", \"X\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("XbXd", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void replaceAllMissingText() {
		try {
			Expression expression = getExpressionWithFunctionContext("replaceAll(MISSING_NOMINAL, \"[ac]\", \"X\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void replaceAllMissingRegEx() {
		try {
			Expression expression = getExpressionWithFunctionContext("replaceAll(\"abcd\", MISSING_NOMINAL, \"X\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void replaceAllMissingReplace() {
		try {
			Expression expression = getExpressionWithFunctionContext("replaceAll(\"abcd\", \"[ac]\", MISSING_NOMINAL)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void replaceAllEmptyText() {
		try {
			Expression expression = getExpressionWithFunctionContext("replaceAll(\"\", \"[ac]\", \"X\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void replaceAllemptyReplacement() {
		try {
			Expression expression = getExpressionWithFunctionContext("replaceAll(\"abcd\", \"[ab]\", \"\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("cd", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void replaceAllMissingArgument() {
		try {
			getExpressionWithFunctionContext(" replaceAll( \"[ac]\", \"X\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void replaceAllTooManyArguments() {
		try {
			getExpressionWithFunctionContext(" replaceAll(\"abcd\", \".*\", \"X\", \"a\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void replaceAllEmptyRegEx() {
		try {
			getExpressionWithFunctionContext(" replaceAll(\"abcd\", \"\", \"X\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// cut

	@Test
	public void cutBasic() {
		try {
			Expression expression = getExpressionWithFunctionContext("cut(\"abcd\", 1, 2)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("bc", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void cutBasicDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("cut(\"abcdefg\", 1.345, 2.873)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("bc", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void cutMissingText() {
		try {
			Expression expression = getExpressionWithFunctionContext("cut(MISSING_NOMINAL, 1, 2)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void cutMissingIndex() {
		try {
			Expression expression = getExpressionWithFunctionContext("cut(\"abcd\", MISSING_NUMERIC, 2)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("ab", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void cutMissingLength() {
		try {
			Expression expression = getExpressionWithFunctionContext("cut(\"abcd\", 2, MISSING_NUMERIC)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void cutEmptyText() {
		try {
			Expression expression = getExpressionWithFunctionContext("cut(\"\", 2, 2)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			expression.evaluateNominal();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cutNegIndex() {
		try {
			Expression expression = getExpressionWithFunctionContext("cut(\"abcde\", -2, 2)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			expression.evaluateNominal();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cutNegLength() {
		try {
			Expression expression = getExpressionWithFunctionContext("cut(\"abcde\", 2, -2)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			expression.evaluateNominal();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cutIndexInf() {
		try {
			Expression expression = getExpressionWithFunctionContext("cut(\"abcde\", INFINITY, 2)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			expression.evaluateNominal();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cutIndexNegInf() {
		try {
			Expression expression = getExpressionWithFunctionContext("cut(\"abcde\", -INFINITY, 2)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			expression.evaluateNominal();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cutLengthInf() {
		try {
			Expression expression = getExpressionWithFunctionContext("cut(\"abcde\", 2, INFINITY)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			expression.evaluateNominal();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cutLengthNegnf() {
		try {
			Expression expression = getExpressionWithFunctionContext("cut(\"abcde\", 2, -INFINITY)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			expression.evaluateNominal();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cutLengthZero() {
		try {
			Expression expression = getExpressionWithFunctionContext("cut(\"abcde\", 2, 0)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void cutMissingArgument() {
		try {
			getExpressionWithFunctionContext(" cut( \"[ac]\", 5)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cutTooManyArguments() {
		try {
			getExpressionWithFunctionContext(" cut(\"abcd\",4, 4, 4)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void cutText() {
		try {
			Expression expression = getExpressionWithFunctionContext(" cut(\"text\",1, 3)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("ext", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void cutTextTooLong() {
		try {
			getExpressionWithFunctionContext(" cut(\"text\",1, 4)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// replace

	@Test
	public void replaceBasic() {
		try {
			Expression expression = getExpressionWithFunctionContext("replace(\"abcd\", \"a\", \"X\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("Xbcd", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void replaceMissingText() {
		try {
			Expression expression = getExpressionWithFunctionContext("replace(MISSING_NOMINAL, \"ac\", \"X\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void replaceMissingSearch() {
		try {
			Expression expression = getExpressionWithFunctionContext("replace(\"abcd\", MISSING_NOMINAL, \"X\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void replaceMissingReplace() {
		try {
			Expression expression = getExpressionWithFunctionContext("replace(\"abcd\", \"a\", MISSING_NOMINAL)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void replaceEmptyText() {
		try {
			Expression expression = getExpressionWithFunctionContext("replace(\"\", \"ac\", \"X\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void replaceemptyReplacement() {
		try {
			Expression expression = getExpressionWithFunctionContext("replace(\"abcd\", \"a\", \"\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("bcd", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void replaceMissingArgument() {
		try {
			getExpressionWithFunctionContext(" replace( \"ac\", \"X\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void replaceTooManyArguments() {
		try {
			getExpressionWithFunctionContext(" replace(\"abcd\", \".*\", \"X\", \"a\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void replaceEmptySearch() {
		try {
			getExpressionWithFunctionContext(" replace(\"abcd\", \"\", \"X\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// lower

	@Test
	public void lowerBasic() {
		try {
			Expression expression = getExpressionWithFunctionContext("lower(\"AbCd\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("abcd", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void lowerMissingText() {
		try {
			Expression expression = getExpressionWithFunctionContext("lower(MISSING_NOMINAL)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void lowerEmptyText() {
		try {
			Expression expression = getExpressionWithFunctionContext("lower(\"\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void lowerMissingArgument() {
		try {
			getExpressionWithFunctionContext(" lower()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void lowerTooManyArguments() {
		try {
			getExpressionWithFunctionContext(" lower(\"abcd\", \".*\", \"X\", \"a\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// upper

	@Test
	public void upperBasic() {
		try {
			Expression expression = getExpressionWithFunctionContext("upper(\"AbCd\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("ABCD", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void upperMissingText() {
		try {
			Expression expression = getExpressionWithFunctionContext("upper(MISSING_NOMINAL)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void upperEmptyText() {
		try {
			Expression expression = getExpressionWithFunctionContext("upper(\"\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void upperMissingArgument() {
		try {
			getExpressionWithFunctionContext(" upper()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void upperTooManyArguments() {
		try {
			getExpressionWithFunctionContext(" upper(\"abcd\", \".*\", \"X\", \"a\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// trim

	@Test
	public void trimBasic() {
		try {
			Expression expression = getExpressionWithFunctionContext("trim(\" abcd \")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("abcd", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	public void trimTab() {
		try {
			Expression expression = getExpressionWithFunctionContext("trim(\" abcd \t\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("abcd", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	public void trimNewLine() {
		try {
			Expression expression = getExpressionWithFunctionContext("trim(\" abcd \n\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("abcd", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void trimMissingText() {
		try {
			Expression expression = getExpressionWithFunctionContext("trim(MISSING_NOMINAL)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void trimEmptyText() {
		try {
			Expression expression = getExpressionWithFunctionContext("trim(\"\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void trimrMissingArgument() {
		try {
			getExpressionWithFunctionContext("trim()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void trimTooManyArguments() {
		try {
			getExpressionWithFunctionContext("trim(\"abcd\", \".*\", \"X\", \"a\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// escapeHTML

	@Test
	public void escapeBasic() {
		try {
			Expression expression = getExpressionWithFunctionContext("escape_html(\"<div>abcd</div>\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("&lt;div&gt;abcd&lt;/div&gt;", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void escapeMissingText() {
		try {
			Expression expression = getExpressionWithFunctionContext("escape_html(MISSING_NOMINAL)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void escapeEmptyText() {
		try {
			Expression expression = getExpressionWithFunctionContext("escape_html(\"\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void excapeMissingArgument() {
		try {
			getExpressionWithFunctionContext("escape_html()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void escapeTooManyArguments() {
		try {
			getExpressionWithFunctionContext("escape_html(\"abcd\", \".*\", \"X\", \"a\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// suffix

	@Test
	public void suffixBasicInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("suffix(\"abcd\", 2)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("cd", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void suffixBasicDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("suffix(\"abcd\", 2.54)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("cd", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void suffixTooBigLength() {
		try {
			Expression expression = getExpressionWithFunctionContext("suffix(\"abcd\", 10)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("abcd", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void suffixMissingText() {
		try {
			Expression expression = getExpressionWithFunctionContext("suffix(MISSING_NOMINAL, 2)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void suffixMissingLength() {
		try {
			Expression expression = getExpressionWithFunctionContext("suffix(\"abcd\", MISSING_NUMERIC)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void suffixEmptyText() {
		try {
			Expression expression = getExpressionWithFunctionContext("suffix(\"\", 2)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void suffixNegLength() {
		try {
			Expression expression = getExpressionWithFunctionContext("suffix(\"hallo\", -3)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("hallo", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void suffixLengthInf() {
		try {
			Expression expression = getExpressionWithFunctionContext("suffix(\"abcde\", INFINITY)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("abcde", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void suffixLengthNegInf() {
		try {
			Expression expression = getExpressionWithFunctionContext("suffix(\"abcde\", -INFINITY)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("abcde", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void suffixLengthZero() {
		try {
			Expression expression = getExpressionWithFunctionContext("suffix(\"abcde\", 0)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void suffixMissingArgument() {
		try {
			getExpressionWithFunctionContext("suffix( \"[ac]\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void suffixTooManyArguments() {
		try {
			getExpressionWithFunctionContext("suffix(\"abcd\",4, 4, 4)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void suffixNoArgument() {
		try {
			getExpressionWithFunctionContext("suffix()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// prefix

	@Test
	public void prefixBasicInt() {
		try {
			Expression expression = getExpressionWithFunctionContext("prefix(\"abcd\", 2)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("ab", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	public void prefixBasicDouble() {
		try {
			Expression expression = getExpressionWithFunctionContext("prefix(\"abcd\", 2.654)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("ab", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void prefixTooBigLength() {
		try {
			Expression expression = getExpressionWithFunctionContext("prefix(\"abcd\", 10)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("abcd", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void prefixMissingText() {
		try {
			Expression expression = getExpressionWithFunctionContext("prefix(MISSING_NOMINAL, 2)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void prefixMissingLength() {
		try {
			Expression expression = getExpressionWithFunctionContext("prefix(\"abcd\", MISSING_NUMERIC)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void prefixEmptyText() {
		try {
			Expression expression = getExpressionWithFunctionContext("prefix(\"\", 2)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void prefixNegLength() {
		try {
			Expression expression = getExpressionWithFunctionContext("prefix(\"hallo\", -3)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("hallo", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void prefixLengthInf() {
		try {
			Expression expression = getExpressionWithFunctionContext("prefix(\"abcde\", INFINITY)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("abcde", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void prefixLengthNegInf() {
		try {
			Expression expression = getExpressionWithFunctionContext("prefix(\"abcde\", -INFINITY)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("abcde", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void prefixLengthZero() {
		try {
			Expression expression = getExpressionWithFunctionContext("prefix(\"abcde\", 0)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void prefixMissingArgument() {
		try {
			getExpressionWithFunctionContext(" prefix( \"[ac]\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void prefixTooManyArguments() {
		try {
			getExpressionWithFunctionContext(" prefix(\"abcd\",4, 4, 4)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void prefixNoArgument() {
		try {
			getExpressionWithFunctionContext(" prefix()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	// char

	@Test
	public void charBasic() {
		try {
			Expression expression = getExpressionWithFunctionContext("char(\"abcd\", 2)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("c", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void charTooBigLength() {
		try {
			Expression expression = getExpressionWithFunctionContext("char(\"abcd\", 10)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void charMissingText() {
		try {
			Expression expression = getExpressionWithFunctionContext("char(MISSING_NOMINAL, 2)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void charMissingIndex() {
		try {
			Expression expression = getExpressionWithFunctionContext("char(\"abcd\", MISSING_NUMERIC)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("a", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void charEmptyText() {
		try {
			Expression expression = getExpressionWithFunctionContext("char(\"\", 2)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void CharNegIndex() {
		try {
			Expression expression = getExpressionWithFunctionContext("char(\"hallo\", -3)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void charIndexInf() {
		try {
			Expression expression = getExpressionWithFunctionContext("char(\"abcde\", INFINITY)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void charIndexNegInf() {
		try {
			Expression expression = getExpressionWithFunctionContext("char(\"abcde\", -INFINITY)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals(null, expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void charIndexZero() {
		try {
			Expression expression = getExpressionWithFunctionContext("char(\"abcde\", 0)");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("a", expression.evaluateNominal());
		} catch (ExpressionException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void charMissingArgument() {
		try {
			getExpressionWithFunctionContext(" char( \"[ac]\")");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void charTooManyArguments() {
		try {
			getExpressionWithFunctionContext(" char(\"abcd\",4, 4, 4)");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void charNoArgument() {
		try {
			getExpressionWithFunctionContext(" char()");
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void charDoubleIndex() {
		try {
			Expression expression = getExpressionWithFunctionContext(" char(\"FireHawk\"), 1.974");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			assertEquals("i", expression.evaluateNominal());
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void charBoolInput() {
		try {
			Expression expression = getExpressionWithFunctionContext(" char(\"FireHawk\"), TRUE");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			expression.evaluateNominal();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void charBoolInput2() {
		try {
			Expression expression = getExpressionWithFunctionContext(" char(TRUE), 1.974");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			expression.evaluateNominal();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	public void charWrongOrder() {
		try {
			Expression expression = getExpressionWithFunctionContext(" char(1.974, \"FireHawk\")");
			assertEquals(ExpressionType.STRING, expression.getExpressionType());
			expression.evaluateNominal();
			fail();
		} catch (ExpressionException e) {
			assertNotNull(e.getMessage());
		}
	}
}
