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
package com.rapidminer.operator;

import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.CollectionMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.ModelMetaData;


/**
 * <p>
 * This operator ungroups a previously grouped model ({@link ModelGrouper}) and delivers the grouped
 * input models.
 * </p>
 * 
 * <p>
 * This operator replaces the automatic model grouping known from previous versions of RapidMiner.
 * The explicit usage of this ungrouping operator gives the user more control about the ungrouping
 * procedure. Single models can be grouped with the {@link ModelGrouper} operator.
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class ModelUngrouper extends Operator {

	private InputPort groupedModelInput = getInputPorts().createPort("grouped model", GroupedModel.class);
	private OutputPort modelOutput = getOutputPorts().createPort("models");

	public ModelUngrouper(OperatorDescription description) {
		super(description);
		getTransformer().addRule(
				new GenerateNewMDRule(modelOutput, new CollectionMetaData(new ModelMetaData(Model.class,
						new ExampleSetMetaData()))));
	}

	@Override
	public void doWork() throws OperatorException {
		GroupedModel groupedModel = groupedModelInput.getData(GroupedModel.class);
		Model[] result = new Model[groupedModel.getNumberOfModels()];
		int index = 0;
		for (Model inner : groupedModel) {
			result[index++] = inner;
		}
		modelOutput.deliver(new IOObjectCollection<Model>(result));
	}
}
