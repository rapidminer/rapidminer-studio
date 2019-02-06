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
package com.rapidminer.operator.preprocessing.filter;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * Converts all numerical attributes (especially integer attributes) to real valued attributes. Each
 * integer value is simply used as real value of the new attribute. If the value is missing, the new
 * value will be missing.
 * 
 * @author Ingo Mierswa
 */
public class Numerical2Real extends AbstractFilteredDataProcessing {

	public Numerical2Real(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSetMetaData applyOnFilteredMetaData(ExampleSetMetaData emd) {
		for (AttributeMetaData amd : emd.getAllAttributes()) {
			if (!amd.isSpecial()) {
				if ((Ontology.ATTRIBUTE_VALUE_TYPE.isA(amd.getValueType(), Ontology.NUMERICAL))
						&& (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(amd.getValueType(), Ontology.REAL))) {
					amd.setType(Ontology.REAL);
				}
			}
		}
		return emd;
	}

	@Override
	public ExampleSet applyOnFiltered(ExampleSet exampleSet) throws OperatorException {
		for (Attribute attribute : exampleSet.getAttributes()) {
			if ((Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.NUMERICAL))
					&& (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.REAL))) {
				Attribute newAttribute = AttributeFactory.changeValueType(attribute, Ontology.REAL);
				exampleSet.getAttributes().replace(attribute, newAttribute);
			}
		}
		return exampleSet;
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.NUMERICAL };
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler
				.getResourceConsumptionEstimator(getInputPort(), Numerical2Real.class, null);
	}
}
