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
import com.rapidminer.tools.Ontology;


/**
 * This class implements the Median Aggregation function. This will calculate the median of a source
 * attribute for each group by remembering all values.
 *
 * @author Sebastian Land
 */
public class MedianAggregationFunctionLegacy extends NumericalAggregationFunction {

	public static final String FUNCTION_MEDIAN = "median";

	public MedianAggregationFunctionLegacy(Attribute sourceAttribute, boolean ignoreMissings, boolean countOnlyDisctinct) {
		super(sourceAttribute, ignoreMissings, countOnlyDisctinct, FUNCTION_MEDIAN, FUNCTION_SEPARATOR_OPEN,
				FUNCTION_SEPARATOR_CLOSE);
	}

	public MedianAggregationFunctionLegacy(Attribute sourceAttribute, boolean ignoreMissings, boolean countOnlyDisctinct,
			String functionName, String separatorOpen, String separatorClose) {
		super(sourceAttribute, ignoreMissings, countOnlyDisctinct, functionName, separatorOpen, separatorClose);
	}

	@Override
	public Aggregator createAggregator() {
		return new MedianAggregatorLegacy(this);
	}

	@Override
	protected int getTargetValueType(int sourceValueType) {
		return sourceValueType;
	}

	@Override
	public boolean isCompatible() {
		return getSourceAttribute().isNumerical()
				|| Ontology.ATTRIBUTE_VALUE_TYPE.isA(getSourceAttribute().getValueType(), Ontology.DATE_TIME);
	}

}
