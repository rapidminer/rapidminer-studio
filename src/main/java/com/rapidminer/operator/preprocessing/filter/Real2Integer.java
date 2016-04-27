/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.math.container.Range;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * Converts all real valued attributes to integer valued attributes. Each real value is simply
 * cutted (default) or rounded. If the value is missing, the new value will be missing.
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class Real2Integer extends AbstractFilteredDataProcessing {

	public static final String PARAMETER_ROUND = "round_values";

	public Real2Integer(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSetMetaData applyOnFilteredMetaData(ExampleSetMetaData emd) {
		boolean round = getParameterAsBoolean(PARAMETER_ROUND);

		for (AttributeMetaData amd : emd.getAllAttributes()) {
			if ((Ontology.ATTRIBUTE_VALUE_TYPE.isA(amd.getValueType(), Ontology.NUMERICAL))
					&& (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(amd.getValueType(), Ontology.INTEGER))) {
				amd.setType(Ontology.INTEGER);
			}
			if (round) {
				amd.setValueRange(
						new Range(Math.round(amd.getValueRange().getLower()), Math.round(amd.getValueRange().getUpper())),
						SetRelation.EQUAL);
			} else {
				amd.setValueRange(new Range((long) amd.getValueRange().getLower(), (long) amd.getValueRange().getUpper()),
						SetRelation.EQUAL);
			}
		}
		return emd;
	}

	@Override
	public ExampleSet applyOnFiltered(ExampleSet exampleSet) throws OperatorException {
		boolean round = getParameterAsBoolean(PARAMETER_ROUND);

		List<Attribute> newAttributes = new LinkedList<Attribute>();
		Iterator<Attribute> a = exampleSet.getAttributes().iterator();
		while (a.hasNext()) {
			Attribute attribute = a.next();
			if ((Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.NUMERICAL))
					&& (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.INTEGER))) {
				Attribute newAttribute = AttributeFactory.createAttribute(attribute.getName(), Ontology.INTEGER);
				newAttributes.add(newAttribute);
				exampleSet.getExampleTable().addAttribute(newAttribute);
				for (Example example : exampleSet) {
					double originalValue = example.getValue(attribute);
					if (Double.isNaN(originalValue)) {
						example.setValue(newAttribute, Double.NaN);
					} else {
						long newValue = round ? Math.round(originalValue) : (long) originalValue;
						example.setValue(newAttribute, newValue);
					}
				}
				a.remove();
			}
		}

		for (Attribute attribute : newAttributes) {
			exampleSet.getAttributes().addRegular(attribute);
		}

		return exampleSet;
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.REAL };
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_ROUND, "Indicates if the values should be rounded instead of cutted.",
				false));
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return true;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), Real2Integer.class, null);
	}

}
