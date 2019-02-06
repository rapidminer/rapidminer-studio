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
 * Converts all nominal attributes to string attributes. Each nominal value is simply used as string
 * value of the new attribute. If the value is missing, the new value will be missing.
 * 
 * @author Ingo Mierswa
 */
public class Nominal2String extends AbstractFilteredDataProcessing {

	public Nominal2String(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet applyOnFiltered(ExampleSet exampleSet) throws OperatorException {
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNominal()) {
				Attribute newAttribute = AttributeFactory.changeValueType(attribute, Ontology.STRING);
				exampleSet.getAttributes().replace(attribute, newAttribute);
			}
		}
		return exampleSet;
	}

	@Override
	public ExampleSetMetaData applyOnFilteredMetaData(ExampleSetMetaData emd) {
		for (AttributeMetaData amd : emd.getAllAttributes()) {
			if (amd.isNominal()) {
				amd.setType(Ontology.STRING);
			}
		}
		return emd;
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.NOMINAL };
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler
				.getResourceConsumptionEstimator(getInputPort(), Nominal2String.class, null);
	}
}
