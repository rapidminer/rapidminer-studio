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
package com.rapidminer.operator.features.construction;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttributes;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.VectorMath;
import com.rapidminer.tools.math.container.Range;

import java.util.LinkedList;
import java.util.List;


/**
 * This operator creates all products of the specified attributes. The attribute names can be
 * specified by regular expressions.
 * 
 * @author Ingo Mierswa
 */
public class ProductGenerationOperator extends AbstractFeatureConstruction {

	public static final String PARAMETER_FIRST_ATTRIBUTE_NAME = "first_attribute_name";

	public static final String PARAMETER_SECOND_ATTRIBUTE_NAME = "second_attribute_name";

	public ProductGenerationOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		List<AttributeMetaData> newAttributes = new LinkedList<AttributeMetaData>();
		String firstAttributeName = getParameterAsString(PARAMETER_FIRST_ATTRIBUTE_NAME);
		String secondAttributeName = getParameterAsString(PARAMETER_SECOND_ATTRIBUTE_NAME);

		for (AttributeMetaData attribute : metaData.getAllAttributes()) {
			if (attribute.isNumerical()) {
				if (attribute.getName().matches(firstAttributeName)) {
					for (AttributeMetaData attribute2 : metaData.getAllAttributes()) {
						if (attribute2.isNumerical()) {
							if (attribute2.getName().matches(secondAttributeName)) {
								AttributeMetaData newAMD = new AttributeMetaData("(" + attribute.getName() + ") * ("
										+ attribute2.getName() + ")", Ontology.REAL);
								// range
								if (attribute.getValueSetRelation() == SetRelation.EQUAL
										&& attribute2.getValueSetRelation() == SetRelation.EQUAL) {
									Range range1 = attribute.getValueRange();
									Range range2 = attribute2.getValueRange();
									double[] values = { range1.getLower() * range2.getLower(),
											range1.getLower() * range2.getUpper(), range1.getUpper() * range2.getLower(),
											range1.getUpper() * range2.getUpper() };
									newAMD.setValueRange(
											new Range(VectorMath.minimalElement(values), VectorMath.maximalElement(values)),
											SetRelation.SUBSET);
								} else {
									newAMD.setValueRange(new Range(), SetRelation.UNKNOWN);
								}
								// unknown values
								if (attribute2.getNumberOfMissingValues().isKnown()
										&& attribute.getNumberOfMissingValues().isKnown()) {
									newAMD.setNumberOfMissingValues(new MDInteger(attribute.getNumberOfMissingValues()
											.getValue() + attribute2.getNumberOfMissingValues().getValue()));
								}
								newAttributes.add(newAMD);
							}
						}
					}
				}
			}
		}
		metaData.addAllAttributes(newAttributes);
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		List<Attribute> newAttributes = new LinkedList<Attribute>();
		String firstAttributeName = getParameterAsString(PARAMETER_FIRST_ATTRIBUTE_NAME);
		String secondAttributeName = getParameterAsString(PARAMETER_SECOND_ATTRIBUTE_NAME);
		Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		for (Attribute attribute : regularAttributes) {
			if (attribute.isNumerical()) {
				if (attribute.getName().matches(firstAttributeName)) {
					for (Attribute attribute2 : regularAttributes) {
						checkForStop();
						if (attribute2.isNumerical()) {
							if (attribute2.getName().matches(secondAttributeName)) {
								newAttributes.add(createAttribute(exampleSet, attribute, attribute2));
							}
						}
					}
				}
			}
		}

		for (Attribute attribute : newAttributes) {
			exampleSet.getAttributes().addRegular(attribute);
		}

		return exampleSet;
	}

	private Attribute createAttribute(ExampleSet exampleSet, Attribute attribute1, Attribute attribute2) {
		Attribute result = AttributeFactory.createAttribute("(" + attribute1.getName() + ") * (" + attribute2.getName()
				+ ")", Ontology.REAL);

		exampleSet.getExampleTable().addAttribute(result);

		for (Example example : exampleSet) {
			double value1 = example.getValue(attribute1);
			double value2 = example.getValue(attribute2);
			double resultValue = value1 * value2;
			example.setValue(result, resultValue);
		}

		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeAttributes(PARAMETER_FIRST_ATTRIBUTE_NAME,
				"The name(s) of the first attribute to be multiplied.", getExampleSetInputPort(), false, Ontology.NUMERICAL));
		types.add(new ParameterTypeAttributes(PARAMETER_SECOND_ATTRIBUTE_NAME,
				"The name(s) of the second attribute to be multiplied.", getExampleSetInputPort(), false, Ontology.NUMERICAL));
		return types;
	}
}
