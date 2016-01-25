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
package com.rapidminer.operator.features.construction;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;

import java.util.Iterator;
import java.util.List;


/**
 * <p>
 * This operator applies a linear combination for each vector of the input ExampleSet, i.e. it
 * creates a new feature containing the sum of all numerical values of each row.
 * </p>
 * 
 * @author Thomas Harzer, Ingo Mierswa, Sebastian Land
 */
public class LinearCombinationOperator extends AbstractFeatureConstruction {

	/** The parameter name for &quot;Indicates if the all old attributes should be kept.&quot; */
	public static final String PARAMETER_KEEP_ALL = "keep_all";

	public LinearCombinationOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		AttributeMetaData amd = new AttributeMetaData("linear_combination", Ontology.REAL);
		if (!getParameterAsBoolean(PARAMETER_KEEP_ALL)) {
			metaData.clearRegular();
		}
		metaData.addAttribute(amd);
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		// create linear combination attribute
		Attribute newAttribute = AttributeFactory.createAttribute("linear_combination", Ontology.REAL);
		exampleSet.getExampleTable().addAttribute(newAttribute);
		exampleSet.getAttributes().addRegular(newAttribute);

		Iterator i = exampleSet.iterator();
		// go through the object attributes and sum them up
		while (i.hasNext()) {
			Example example = (Example) i.next();
			double valueSum = 0.0d;
			for (Attribute attribute : example.getAttributes()) {
				if (!attribute.equals(newAttribute) && attribute.isNumerical()) {
					valueSum += example.getValue(attribute);
				}
			}
			example.setValue(newAttribute, valueSum);
		}

		// remove old attributes
		if (!getParameterAsBoolean(PARAMETER_KEEP_ALL)) {
			exampleSet.getAttributes().clearRegular();
			exampleSet.getAttributes().addRegular(newAttribute);
		}

		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_KEEP_ALL, "Indicates if all old attributes should be kept.", false));
		return types;
	}
}
