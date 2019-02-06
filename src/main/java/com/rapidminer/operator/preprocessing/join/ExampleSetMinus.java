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
package com.rapidminer.operator.preprocessing.join;

import java.util.ArrayList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.MappedExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * This operator performs a set minus on two example sets, i.e. the resulting example set contains
 * all the examples of the minuend example set whose IDs do not appear in the subtrahend example
 * set. Please note, that the subtrahend example set must be the first on the ioobject stack, i.e.
 * it must be the last example set which has been added. As compared to SQL, both example sets need
 * have neither the same number of columns nor the same data types. The operation does only depend
 * on the ID columns of the example sets.
 *
 * @author Tobias Malbrecht
 */
public class ExampleSetMinus extends AbstractDataProcessing {

	private InputPort subtrahendInput = getInputPorts().createPort("subtrahend");

	public ExampleSetMinus(OperatorDescription description) {
		super(description);
		subtrahendInput.addPrecondition(new ExampleSetPrecondition(subtrahendInput, Ontology.ATTRIBUTE_VALUE,
				Attributes.ID_NAME));
		getExampleSetInputPort().addPrecondition(
				new ExampleSetPrecondition(getExampleSetInputPort(), Ontology.ATTRIBUTE_VALUE, Attributes.ID_NAME));
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		metaData.getNumberOfExamples().reduceByUnknownAmount();
		// TODO: Could instead take a look at the values of ids if nominal.
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		// recall: difference = minuend - subtrahend
		// but the subtrahend is last on the ioobjects stack, so pop first
		ExampleSet subtrahendSet = subtrahendInput.getData(ExampleSet.class);
		ExampleSet minuendSet = exampleSet;

		subtrahendSet.remapIds();
		minuendSet.remapIds();

		Attribute minuendId = minuendSet.getAttributes().getId();
		Attribute subtrahendId = subtrahendSet.getAttributes().getId();

		// sanity checks
		if (minuendId == null || subtrahendId == null) {
			throw new UserError(this, 129);
		}
		if (minuendId.getValueType() != subtrahendId.getValueType()) {
			throw new UserError(this, 120,
					new Object[] { subtrahendId.getName(), Ontology.VALUE_TYPE_NAMES[subtrahendId.getValueType()],
							Ontology.VALUE_TYPE_NAMES[minuendId.getValueType()] });
		}

		List<Integer> indices = new ArrayList<>();
		{
			int i = 0;
			for (Example example : minuendSet) {
				double id = example.getValue(minuendId);
				Example subtrahendExample = null;
				if (minuendId.isNominal()) {
					subtrahendExample = subtrahendSet
							.getExampleFromId(subtrahendId.getMapping().getIndex(minuendId.getMapping().mapIndex((int) id)));
				} else {
					subtrahendExample = subtrahendSet.getExampleFromId(id);
				}
				if (subtrahendExample == null) {
					indices.add(i);
				}
				i++;
			}
		}

		int[] indexArray = new int[indices.size()];
		for (int i = 0; i < indices.size(); i++) {
			indexArray[i] = indices.get(i);
		}

		ExampleSet minusSet = new MappedExampleSet(minuendSet, indexArray);
		return minusSet;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), ExampleSetMinus.class,
				null);
	}

}
