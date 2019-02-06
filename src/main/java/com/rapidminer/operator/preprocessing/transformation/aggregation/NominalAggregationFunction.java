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
import com.rapidminer.tools.Ontology;


/**
 * This class implements an abstract superclass for all Nominal aggregation functions, the new
 * attribute will have an empty mapping. All subclasses must take care to modify the mapping
 * accordingly, if adding new nominal values.
 * 
 * @author Sebastian Land
 */
public abstract class NominalAggregationFunction extends AggregationFunction {

	private Attribute targetAttribute;

	public NominalAggregationFunction(Attribute sourceAttribute, boolean ignoreMissings, boolean countOnlyDisctinct,
			String functionName, String separatorOpen, String separatorClose) {
		super(sourceAttribute, ignoreMissings, countOnlyDisctinct);
		if (sourceAttribute.isNominal()) {
			this.targetAttribute = AttributeFactory.createAttribute(functionName + separatorOpen
					+ getSourceAttribute().getName() + separatorClose, Ontology.POLYNOMINAL);
		}
	}

	@Override
	public Attribute getTargetAttribute() {
		return targetAttribute;
	}

	@Override
	public final boolean isCompatible() {
		return getSourceAttribute().isNominal();
	}

}
