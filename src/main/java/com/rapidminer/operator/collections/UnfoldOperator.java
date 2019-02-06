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
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.CollectionMetaData;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;


/**
 * Turns a collection of collections into a flat collection.
 * 
 * @author Simon Fischer
 * 
 */
public class UnfoldOperator extends Operator {

	private final InputPort collectionInput = getInputPorts().createPort("collection", new CollectionMetaData());
	private final OutputPort flatOutput = getOutputPorts().createPort("flat");

	public UnfoldOperator(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new MDTransformationRule() {

			@Override
			public void transformMD() {
				MetaData md = collectionInput.getMetaData();
				if ((md != null) && (md instanceof CollectionMetaData)) {
					flatOutput.deliverMD(new CollectionMetaData(((CollectionMetaData) md).getElementMetaDataRecursive()));
				} else {
					flatOutput.deliverMD(null);
				}
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		IOObjectCollection<?> collection = collectionInput.getData(IOObjectCollection.class);
		flatOutput.deliver(new IOObjectCollection<IOObject>(collection.getObjectsRecursive()));
	}

}
