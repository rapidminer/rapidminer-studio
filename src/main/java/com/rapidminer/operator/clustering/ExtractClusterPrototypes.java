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
package com.rapidminer.operator.clustering;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.ModelMetaData;
import com.rapidminer.tools.Ontology;


/**
 * This operator extracts the cluster prototypes from a flat clustermodel and builds an example set
 * containing them.
 *
 * @author Sebastian Land
 *
 */
public class ExtractClusterPrototypes extends Operator {

	private InputPort modelInput = getInputPorts().createPort("model", CentroidClusterModel.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort modelOutput = getOutputPorts().createPort("model");

	public ExtractClusterPrototypes(OperatorDescription description) {
		super(description);

		getTransformer().addPassThroughRule(modelInput, modelOutput);
		getTransformer().addRule(new GenerateNewMDRule(exampleSetOutput, ExampleSet.class) {

			@Override
			public MetaData modifyMetaData(MetaData unmodifiedMetaData) {
				if (modelInput.getMetaData() instanceof ModelMetaData) {
					ModelMetaData modelMetaData = (ModelMetaData) modelInput.getMetaData();
					ExampleSetMetaData emd = modelMetaData.getTrainingSetMetaData();
					emd.setNumberOfExamples(new MDInteger());
					return emd;
				}
				return super.modifyMetaData(unmodifiedMetaData);
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		CentroidClusterModel model = modelInput.getData(CentroidClusterModel.class);

		Attributes trainAttributes = model.getTrainingHeader().getAttributes();
		String[] attributeNames = model.getAttributeNames();
		Attribute[] attributes = new Attribute[attributeNames.length + 1];

		for (int i = 0; i < attributeNames.length; i++) {
			Attribute originalAttribute = trainAttributes.get(attributeNames[i]);
			attributes[i] = AttributeFactory.createAttribute(attributeNames[i], originalAttribute.getValueType());
			if (originalAttribute.isNominal()) {
				attributes[i].setMapping((NominalMapping) originalAttribute.getMapping().clone());
			}
		}
		Attribute clusterAttribute = AttributeFactory.createAttribute("cluster", Ontology.NOMINAL);
		attributes[attributes.length - 1] = clusterAttribute;

		ExampleSetBuilder builder = ExampleSets.from(attributes).withExpectedSize(model.getNumberOfClusters());
		for (int i = 0; i < model.getNumberOfClusters(); i++) {
			double[] data = new double[attributeNames.length + 1];
			System.arraycopy(model.getCentroidCoordinates(i), 0, data, 0, attributeNames.length);
			data[attributeNames.length] = clusterAttribute.getMapping().mapString("cluster_" + i);
			builder.addRow(data);
		}

		ExampleSet resultSet = builder.withRole(clusterAttribute, Attributes.CLUSTER_NAME).build();

		modelOutput.deliver(model);
		exampleSetOutput.deliver(resultSet);
	}

}
