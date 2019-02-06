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
package com.rapidminer.tools.expression.internal;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.tools.expression.Constant;
import com.rapidminer.tools.expression.ExpressionParserModule;
import com.rapidminer.tools.expression.Function;
import com.rapidminer.tools.expression.internal.function.bitwise.BitAnd;
import com.rapidminer.tools.expression.internal.function.bitwise.BitNot;
import com.rapidminer.tools.expression.internal.function.bitwise.BitOr;
import com.rapidminer.tools.expression.internal.function.bitwise.BitXor;
import com.rapidminer.tools.expression.internal.function.comparison.Finite;
import com.rapidminer.tools.expression.internal.function.comparison.Missing;
import com.rapidminer.tools.expression.internal.function.conversion.DateParse;
import com.rapidminer.tools.expression.internal.function.conversion.DateParseCustom;
import com.rapidminer.tools.expression.internal.function.conversion.DateParseWithLocale;
import com.rapidminer.tools.expression.internal.function.conversion.DateString;
import com.rapidminer.tools.expression.internal.function.conversion.DateStringCustom;
import com.rapidminer.tools.expression.internal.function.conversion.DateStringLocale;
import com.rapidminer.tools.expression.internal.function.conversion.NumericalToString;
import com.rapidminer.tools.expression.internal.function.conversion.StringToNumerical;
import com.rapidminer.tools.expression.internal.function.date.DateAdd;
import com.rapidminer.tools.expression.internal.function.date.DateAfter;
import com.rapidminer.tools.expression.internal.function.date.DateBefore;
import com.rapidminer.tools.expression.internal.function.date.DateDiff;
import com.rapidminer.tools.expression.internal.function.date.DateGet;
import com.rapidminer.tools.expression.internal.function.date.DateMillis;
import com.rapidminer.tools.expression.internal.function.date.DateNow;
import com.rapidminer.tools.expression.internal.function.date.DateSet;
import com.rapidminer.tools.expression.internal.function.logical.If;
import com.rapidminer.tools.expression.internal.function.mathematical.AbsoluteValue;
import com.rapidminer.tools.expression.internal.function.mathematical.BinaryLogarithm;
import com.rapidminer.tools.expression.internal.function.mathematical.CommonLogarithm;
import com.rapidminer.tools.expression.internal.function.mathematical.ExponentialFunction;
import com.rapidminer.tools.expression.internal.function.mathematical.ModulusAsFunction;
import com.rapidminer.tools.expression.internal.function.mathematical.NaturalLogarithm;
import com.rapidminer.tools.expression.internal.function.mathematical.PowerAsFunction;
import com.rapidminer.tools.expression.internal.function.mathematical.Signum;
import com.rapidminer.tools.expression.internal.function.mathematical.SquareRoot;
import com.rapidminer.tools.expression.internal.function.rounding.Ceil;
import com.rapidminer.tools.expression.internal.function.rounding.Floor;
import com.rapidminer.tools.expression.internal.function.rounding.Rint;
import com.rapidminer.tools.expression.internal.function.rounding.Round;
import com.rapidminer.tools.expression.internal.function.statistical.Average;
import com.rapidminer.tools.expression.internal.function.statistical.Binominal;
import com.rapidminer.tools.expression.internal.function.statistical.Maximum;
import com.rapidminer.tools.expression.internal.function.statistical.Minimum;
import com.rapidminer.tools.expression.internal.function.statistical.Sum;
import com.rapidminer.tools.expression.internal.function.text.CharAt;
import com.rapidminer.tools.expression.internal.function.text.Compare;
import com.rapidminer.tools.expression.internal.function.text.Concat;
import com.rapidminer.tools.expression.internal.function.text.Contains;
import com.rapidminer.tools.expression.internal.function.text.Cut;
import com.rapidminer.tools.expression.internal.function.text.Ends;
import com.rapidminer.tools.expression.internal.function.text.EscapeHTML;
import com.rapidminer.tools.expression.internal.function.text.Finds;
import com.rapidminer.tools.expression.internal.function.text.Index;
import com.rapidminer.tools.expression.internal.function.text.Length;
import com.rapidminer.tools.expression.internal.function.text.Lower;
import com.rapidminer.tools.expression.internal.function.text.Matches;
import com.rapidminer.tools.expression.internal.function.text.Prefix;
import com.rapidminer.tools.expression.internal.function.text.Replace;
import com.rapidminer.tools.expression.internal.function.text.ReplaceAll;
import com.rapidminer.tools.expression.internal.function.text.Starts;
import com.rapidminer.tools.expression.internal.function.text.Suffix;
import com.rapidminer.tools.expression.internal.function.text.TextEquals;
import com.rapidminer.tools.expression.internal.function.text.Trim;
import com.rapidminer.tools.expression.internal.function.text.Upper;
import com.rapidminer.tools.expression.internal.function.trigonometric.ArcCosine;
import com.rapidminer.tools.expression.internal.function.trigonometric.ArcHyperbolicCosine;
import com.rapidminer.tools.expression.internal.function.trigonometric.ArcHyperbolicSine;
import com.rapidminer.tools.expression.internal.function.trigonometric.ArcHyperbolicTangent;
import com.rapidminer.tools.expression.internal.function.trigonometric.ArcSine;
import com.rapidminer.tools.expression.internal.function.trigonometric.ArcTangent;
import com.rapidminer.tools.expression.internal.function.trigonometric.ArcTangent2;
import com.rapidminer.tools.expression.internal.function.trigonometric.Cosecant;
import com.rapidminer.tools.expression.internal.function.trigonometric.Cosine;
import com.rapidminer.tools.expression.internal.function.trigonometric.Cotangent;
import com.rapidminer.tools.expression.internal.function.trigonometric.HyperbolicCosine;
import com.rapidminer.tools.expression.internal.function.trigonometric.HyperbolicSine;
import com.rapidminer.tools.expression.internal.function.trigonometric.HyperbolicTangent;
import com.rapidminer.tools.expression.internal.function.trigonometric.Secant;
import com.rapidminer.tools.expression.internal.function.trigonometric.Sinus;
import com.rapidminer.tools.expression.internal.function.trigonometric.Tangent;


/**
 * Singleton that holds the standard functions and the associated constants for the date functions.
 * The standard functions do not include the operations (+,-,*...).
 *
 * @author Gisa Schaefer
 *
 */
public enum StandardFunctionsWithConstants implements ExpressionParserModule {

	INSTANCE;

	private List<Function> standardFunctions = new LinkedList<>();
	private List<Constant> functionConstants = new LinkedList<>();

	private StandardFunctionsWithConstants() {

		// logical functions
		standardFunctions.add(new If());

		// comparison functions
		standardFunctions.add(new Missing());
		standardFunctions.add(new Finite());

		// text information functions
		standardFunctions.add(new Length());
		standardFunctions.add(new Index());
		standardFunctions.add(new Compare());
		standardFunctions.add(new TextEquals());
		standardFunctions.add(new Contains());
		standardFunctions.add(new Starts());
		standardFunctions.add(new Ends());
		standardFunctions.add(new Matches());
		standardFunctions.add(new Finds());

		// text transformation functions
		standardFunctions.add(new Cut());
		standardFunctions.add(new Concat());
		standardFunctions.add(new Replace());
		standardFunctions.add(new ReplaceAll());
		standardFunctions.add(new Lower());
		standardFunctions.add(new Upper());
		standardFunctions.add(new Prefix());
		standardFunctions.add(new Suffix());
		standardFunctions.add(new CharAt());
		standardFunctions.add(new Trim());
		standardFunctions.add(new EscapeHTML());

		// mathematical functions
		standardFunctions.add(new SquareRoot());
		standardFunctions.add(new PowerAsFunction());
		standardFunctions.add(new ExponentialFunction());
		standardFunctions.add(new NaturalLogarithm());
		standardFunctions.add(new CommonLogarithm());
		standardFunctions.add(new BinaryLogarithm());
		standardFunctions.add(new Signum());
		standardFunctions.add(new AbsoluteValue());
		standardFunctions.add(new ModulusAsFunction());

		// statistical functions
		standardFunctions.add(new Average());
		standardFunctions.add(new Minimum());
		standardFunctions.add(new Maximum());
		standardFunctions.add(new Binominal());
		standardFunctions.add(new Sum());

		// trigonometric functions
		standardFunctions.add(new Sinus());
		standardFunctions.add(new Cosine());
		standardFunctions.add(new Tangent());
		standardFunctions.add(new Cotangent());
		standardFunctions.add(new Secant());
		standardFunctions.add(new Cosecant());
		standardFunctions.add(new ArcSine());
		standardFunctions.add(new ArcCosine());
		standardFunctions.add(new ArcTangent());
		standardFunctions.add(new ArcTangent2());
		standardFunctions.add(new HyperbolicSine());
		standardFunctions.add(new HyperbolicCosine());
		standardFunctions.add(new HyperbolicTangent());
		standardFunctions.add(new ArcHyperbolicSine());
		standardFunctions.add(new ArcHyperbolicCosine());
		standardFunctions.add(new ArcHyperbolicTangent());

		// rounding functions
		standardFunctions.add(new Round());
		standardFunctions.add(new Floor());
		standardFunctions.add(new Ceil());
		standardFunctions.add(new Rint());

		// conversion functions
		standardFunctions.add(new NumericalToString());
		standardFunctions.add(new StringToNumerical());
		standardFunctions.add(new DateParse());
		standardFunctions.add(new DateParseWithLocale());
		standardFunctions.add(new DateParseCustom());
		standardFunctions.add(new DateString());
		standardFunctions.add(new DateStringLocale());
		standardFunctions.add(new DateStringCustom());

		// date functions
		standardFunctions.add(new DateBefore());
		standardFunctions.add(new DateAfter());
		standardFunctions.add(new DateNow());
		standardFunctions.add(new DateDiff());
		standardFunctions.add(new DateAdd());
		standardFunctions.add(new DateSet());
		standardFunctions.add(new DateGet());
		standardFunctions.add(new DateMillis());

		// bitwise functions
		standardFunctions.add(new BitOr());
		standardFunctions.add(new BitAnd());
		standardFunctions.add(new BitXor());
		standardFunctions.add(new BitNot());

		// Date constants:
		functionConstants.add(new SimpleConstant("DATE_SHORT", ExpressionParserConstants.DATE_FORMAT_SHORT,
				"used in date_str and date_str_loc"));
		functionConstants.add(new SimpleConstant("DATE_MEDIUM", ExpressionParserConstants.DATE_FORMAT_MEDIUM,
				"used in date_str and date_str_loc"));
		functionConstants.add(new SimpleConstant("DATE_LONG", ExpressionParserConstants.DATE_FORMAT_LONG,
				"used in date_str and date_str_loc"));
		functionConstants.add(new SimpleConstant("DATE_FULL", ExpressionParserConstants.DATE_FORMAT_FULL,
				"used in date_str and date_str_loc"));
		functionConstants.add(new SimpleConstant("DATE_SHOW_DATE_ONLY", ExpressionParserConstants.DATE_SHOW_DATE_ONLY,
				"used in date_str and date_str_loc"));
		functionConstants.add(new SimpleConstant("DATE_SHOW_TIME_ONLY", ExpressionParserConstants.DATE_SHOW_TIME_ONLY,
				"used in date_str and date_str_loc"));
		functionConstants.add(new SimpleConstant("DATE_SHOW_DATE_AND_TIME",
				ExpressionParserConstants.DATE_SHOW_DATE_AND_TIME, "used in date_add, date_set and date_get"));
		functionConstants.add(new SimpleConstant("DATE_UNIT_YEAR", ExpressionParserConstants.DATE_UNIT_YEAR,
				"used in date_add, date_set and date_get"));
		functionConstants.add(new SimpleConstant("DATE_UNIT_MONTH", ExpressionParserConstants.DATE_UNIT_MONTH,
				"used in date_add, date_set and date_get"));
		functionConstants.add(new SimpleConstant("DATE_UNIT_WEEK", ExpressionParserConstants.DATE_UNIT_WEEK,
				"used in date_add, date_set and date_get"));
		functionConstants.add(new SimpleConstant("DATE_UNIT_DAY", ExpressionParserConstants.DATE_UNIT_DAY,
				"used in date_add, date_set and date_get"));
		functionConstants.add(new SimpleConstant("DATE_UNIT_HOUR", ExpressionParserConstants.DATE_UNIT_HOUR,
				"used in date_add, date_set and date_get"));
		functionConstants.add(new SimpleConstant("DATE_UNIT_MINUTE", ExpressionParserConstants.DATE_UNIT_MINUTE,
				"used in date_add, date_set and date_get"));
		functionConstants.add(new SimpleConstant("DATE_UNIT_SECOND", ExpressionParserConstants.DATE_UNIT_SECOND,
				"used in date_add, date_set and date_get"));
		functionConstants.add(new SimpleConstant("DATE_UNIT_MILLISECOND", ExpressionParserConstants.DATE_UNIT_MILLISECOND,
				"used in date_add, date_set and date_get"));
	}

	@Override
	public String getKey() {
		return "core.function_constants";
	}

	@Override
	public List<Constant> getConstants() {
		return functionConstants;
	}

	@Override
	public List<Function> getFunctions() {
		return standardFunctions;
	}

}
