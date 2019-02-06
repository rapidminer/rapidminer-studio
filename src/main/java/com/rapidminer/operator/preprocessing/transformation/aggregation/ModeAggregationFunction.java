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
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.NominalMapping;


/**
 * This class implements the Mode Aggregation function. This will calculate the mode value of the
 * attribute of the examples within a group.
 * 
 * @author Sebastian Land
 */
public class ModeAggregationFunction extends AggregationFunction {

	public static final String FUNCTION_MODE = "mode";
	private Attribute targetAttribute;

	public ModeAggregationFunction(Attribute sourceAttribute, boolean ignoreMissings, boolean countOnlyDisctinct) {
		this(sourceAttribute, ignoreMissings, countOnlyDisctinct, FUNCTION_MODE, FUNCTION_SEPARATOR_OPEN,
				FUNCTION_SEPARATOR_CLOSE);
	}

	public ModeAggregationFunction(Attribute sourceAttribute, boolean ignoreMissings, boolean countOnlyDisctinct,
			String functionName, String separatorOpen, String separatorClose) {
		super(sourceAttribute, ignoreMissings, countOnlyDisctinct);
		this.targetAttribute = AttributeFactory.createAttribute(FUNCTION_MODE + FUNCTION_SEPARATOR_OPEN
				+ getSourceAttribute().getName() + FUNCTION_SEPARATOR_CLOSE, getSourceAttribute().getValueType());
		if (sourceAttribute.isNominal()) {
			this.targetAttribute.setMapping((NominalMapping) sourceAttribute.getMapping().clone());
		}

	}

	@Override
	public Attribute getTargetAttribute() {
		return targetAttribute;
	}

	@Override
	public boolean isCompatible() {
		return getSourceAttribute().isNominal() || getSourceAttribute().isNumerical(); // Both must
																						// be
																						// supported
																						// for
																						// backward
																						// compatibility
	}

	@Override
	public Aggregator createAggregator() {
		return new ModeAggregator(this);
	}
}
