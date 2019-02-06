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
package com.rapidminer.operator.collections;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.CollectionMetaData;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;

import java.util.List;


/**
 * Selects a single object from an {@link IOObjectCollection}.
 * 
 * @author Simon Fischer
 * 
 */
public class SelectionOperator extends Operator {

	public static final String PARAMETER_INDEX = "index";
	public static final String PARAMETER_UNFOLD = "unfold";

	private final InputPort collectionInput = getInputPorts().createPort("collection",
			new CollectionMetaData(new MetaData()));
	private final OutputPort selectedOutput = getOutputPorts().createPort("selected");

	public SelectionOperator(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new MDTransformationRule() {

			@Override
			public void transformMD() {
				MetaData md = collectionInput.getMetaData();
				if (md instanceof CollectionMetaData) {
					if (getParameterAsBoolean(PARAMETER_UNFOLD)) {
						selectedOutput.deliverMD(((CollectionMetaData) md).getElementMetaDataRecursive());
					} else {
						selectedOutput.deliverMD(((CollectionMetaData) md).getElementMetaData());
					}
				} else {
					selectedOutput.deliverMD(null);
				}
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		@SuppressWarnings("unchecked")
		IOObjectCollection<IOObject> collection = collectionInput.getData(IOObjectCollection.class);
		List<IOObject> elements;
		if (getParameterAsBoolean(PARAMETER_UNFOLD)) {
			elements = collection.getObjectsRecursive();
		} else {
			elements = collection.getObjects();
		}
		int index = getParameterAsInt(PARAMETER_INDEX);
		if ((index < 1) || (index > elements.size())) {
			throw new UserError(this, 159, index, elements.size());
		}
		selectedOutput.deliver(elements.get(index - 1));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(PARAMETER_INDEX, "Index within the collection of the object to return", 1,
				Integer.MAX_VALUE, 1, false));
		types.add(new ParameterTypeBoolean(
				PARAMETER_UNFOLD,
				"If checked, collections are unfolded, i.e., if the collection contains other collections, the children will be concatenated and then the element at the given index will be looked up.",
				false));
		return types;
	}
}
