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

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.tools.expression.Constant;
import com.rapidminer.tools.expression.ExpressionParser;
import com.rapidminer.tools.expression.ExpressionParserModule;
import com.rapidminer.tools.expression.Function;


/**
 * Singleton that stores the basic constants that can be used by the {@link ExpressionParser}.
 *
 * @author Gisa Schaefer
 *
 */
public enum BasicConstants implements ExpressionParserModule {

	INSTANCE;

	private List<Constant> constants;

	private BasicConstants() {
		constants = new LinkedList<>();
		constants.add(new SimpleConstant("true", true, null, true));
		constants.add(new SimpleConstant("false", false, null, true));
		constants.add(new SimpleConstant("TRUE", true));
		constants.add(new SimpleConstant("FALSE", false));
		constants.add(new SimpleConstant("e", Math.E));
		constants.add(new SimpleConstant("pi", Math.PI, null, true));
		constants.add(new SimpleConstant("PI", Math.PI));
		constants.add(new SimpleConstant("INFINITY", Double.POSITIVE_INFINITY));
		constants.add(new SimpleConstant("MISSING_NOMINAL", (String) null));
		constants.add(new SimpleConstant("MISSING_DATE", (Date) null));
		constants.add(new SimpleConstant("MISSING_NUMERIC", Double.NaN));
		constants.add(new SimpleConstant("MISSING", (String) null, null, true));
		constants.add(new SimpleConstant("NaN", Double.NaN, null, true));
		constants.add(new SimpleConstant("NAN", Double.NaN, null, true));
	}

	@Override
	public String getKey() {
		return "core.basic";
	}

	@Override
	public List<Constant> getConstants() {
		return constants;
	}

	@Override
	public List<Function> getFunctions() {
		return null;
	}

}
