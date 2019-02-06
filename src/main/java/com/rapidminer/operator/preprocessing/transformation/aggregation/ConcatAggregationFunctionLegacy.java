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
package com.rapidminer.operator.preprocessing.transformation.aggregation;

import com.rapidminer.example.Attribute;


/**
 * This class emulates the 8.2.0 and earlier concat behavior,
 * which ignored the {@code ignoreMissing} and {@code countOnlyDistinct} settings
 *
 * @author Jonas Wilms-Pfau
 * @since 8.2.1
 */
public class ConcatAggregationFunctionLegacy extends ConcatAggregationFunction {

	/** in version 8.2.0 and earlier, missing values were always ignored */
	private static final boolean IGNORE_MISSING = true;

	/** in version 8.2.0 and earlier, distinct was not working */
	private static final boolean COUNT_ONLY_DISTINCT = false;

	/**
	 * Constructs an 8.2.0 and earlier {@link ConcatAggregationFunction}
	 *
	 * @param sourceAttribute
	 * 		attribute to aggregate
	 * @param ignoreMissings
	 * 		IGNORED always {@value IGNORE_MISSING}
	 * @param countOnlyDisctinct
	 * 		IGNORED always {@value COUNT_ONLY_DISTINCT}
	 */
	public ConcatAggregationFunctionLegacy(Attribute sourceAttribute, boolean ignoreMissings, boolean countOnlyDisctinct) {
		super(sourceAttribute, IGNORE_MISSING, COUNT_ONLY_DISTINCT);
	}

	/**
	 * Constructs an 8.2.0 and earlier {@link ConcatAggregationFunction}
	 *
	 * @param sourceAttribute
	 * 		attribute to aggregate
	 * @param ignoreMissings
	 * 		IGNORED always {@value IGNORE_MISSING}
	 * @param countOnlyDisctinct
	 * 		IGNORED always {@value COUNT_ONLY_DISTINCT}
	 * @param functionName
	 * 		The function name of this aggregation function, default {@link #FUNCTION_CONCAT}
	 * @param separatorOpen
	 * 		The open separator, default {@link #FUNCTION_SEPARATOR_OPEN}
	 * @param separatorClose
	 * 		The close separator, default {@link #FUNCTION_SEPARATOR_CLOSE}
	 */
	public ConcatAggregationFunctionLegacy(Attribute sourceAttribute, boolean ignoreMissings, boolean countOnlyDisctinct,
										   String functionName, String separatorOpen, String separatorClose) {
		super(sourceAttribute, IGNORE_MISSING, COUNT_ONLY_DISTINCT, functionName, separatorOpen, separatorClose);
	}
}
