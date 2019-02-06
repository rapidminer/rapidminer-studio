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
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.tools.Ontology;


/**
 * This class implements the Log Product Aggregation function. This will calculate the logarithm of
 * a product of a source attribute for each group. This can help in situations, where the normal
 * product would exceed the numerical range and should be used as an intermediate result.
 * 
 * This obviously only works on numbers being all greater 0.
 * 
 * @author Sebastian Land
 */
public class LogProductAggregationFunction extends NumericalAggregationFunction {

	public static final String FUNCTION_LOG_PRODUCT = "logProduct";

	public LogProductAggregationFunction(Attribute sourceAttribute, boolean ignoreMissings, boolean countOnlyDisctinct) {
		super(sourceAttribute, ignoreMissings, countOnlyDisctinct, FUNCTION_LOG_PRODUCT, FUNCTION_SEPARATOR_OPEN,
				FUNCTION_SEPARATOR_CLOSE);
	}

	public LogProductAggregationFunction(Attribute sourceAttribute, boolean ignoreMissings, boolean countOnlyDisctinct,
			String functionName, String separatorOpen, String separatorClose) {
		super(sourceAttribute, ignoreMissings, countOnlyDisctinct, functionName, separatorOpen, separatorClose);
	}

	@Override
	public Aggregator createAggregator() {
		return new LogProductAggregator(this);
	}

	@Override
	public void setDefault(Attribute attribute, DoubleArrayDataRow row) {
		row.set(attribute, 0);
	}

	@Override
	protected int getTargetValueType(int sourceValueType) {
		return Ontology.REAL;
	}

	@Override
	public boolean isCompatible() {
		return getSourceAttribute().isNumerical();
	}
}
