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
package com.rapidminer.operator.clustering.clusterer;

import java.util.HashMap;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.tools.metadata.MetaDataTools;


/**
 * This operator creates a flat cluster model using a nominal attribute and dividing the exampleset
 * by this attribute over the clusters. Every value is mapped onto a cluster, including the unkown
 * value. This operator will create a cluster attribute if not present yet.
 *
 * @author Sebastian Land
 */
public class ExampleSet2ClusterModel extends Operator {

	public static final String PARAMETER_ATTRIBUTE = "attribute";
	public static final String PARAMETER_REMOVE_UNLABELED = "remove_unlabeled";
	public static final String PARAMETER_ADD_AS_LABEL = "add_as_label";

	private InputPort exampleSetInput = getInputPorts().createPort("example set");

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort modelOutput = getOutputPorts().createPort("cluster model");

	public ExampleSet2ClusterModel(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput));
		exampleSetInput.addPrecondition(new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition
				.getAttributesByParameter(this, PARAMETER_ATTRIBUTE)));

		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {
				MetaDataTools.checkAndCreateIds(metaData);
				return metaData;
			}
		});
		getTransformer().addGenerationRule(modelOutput, ClusterModel.class);

	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		// checking and creating ids if necessary
		Tools.checkAndCreateIds(exampleSet);

		Attribute attribute = exampleSet.getAttributes().get(getParameterAsString(PARAMETER_ATTRIBUTE));
		if (attribute != null) {
			if (attribute.isNominal()) {
				// search all possible values
				HashMap<Double, Integer> valueMap = new HashMap<>();
				int[] clusterAssignments = new int[exampleSet.size()];
				int i = 0;
				for (Example example : exampleSet) {
					double value = example.getValue(attribute);
					if (valueMap.containsKey(value)) {
						clusterAssignments[i] = valueMap.get(value).intValue();
					} else {
						clusterAssignments[i] = valueMap.size();
						valueMap.put(value, valueMap.size());
					}
					i++;
				}
				ClusterModel model = new ClusterModel(exampleSet, valueMap.size(),
						getParameterAsBoolean(RMAbstractClusterer.PARAMETER_ADD_AS_LABEL),
						getParameterAsBoolean(RMAbstractClusterer.PARAMETER_REMOVE_UNLABELED));
				// assign examples to clusters
				model.setClusterAssignments(clusterAssignments, exampleSet);

				modelOutput.deliver(model);
				exampleSetOutput.deliver(exampleSet);
			} else {
				throw new UserError(this, 119, getParameterAsString(PARAMETER_ATTRIBUTE), "ExampleSet2ClusterModel");
			}
		} else {
			throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE, getParameterAsString(PARAMETER_ATTRIBUTE));
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_ATTRIBUTE,
				"Specifies the nominal attribute used to create the cluster", exampleSetInput, false));

		ParameterType type = new ParameterTypeBoolean(PARAMETER_ADD_AS_LABEL,
				"Should the cluster values be added as label.", false);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_REMOVE_UNLABELED, "Delete the unlabeled examples.", false);
		type.setExpert(false);
		types.add(type);

		return types;
	}

}
