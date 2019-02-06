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
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ConditionedExampleSet;
import com.rapidminer.example.set.NoMissingAttributeValueCondition;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeAddingExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.tools.Ontology;

import java.util.List;


/**
 * This Operator clusters an exampleset given a cluster model. If an exampleSet does not contain an
 * id attribute it is probably not the same as the cluster model has been created on. Since cluster
 * models depend on a static nature of the id attributes, the outcome on another exampleset with
 * different values but same ids will be unpredictable and hence not automatically creation of ids
 * is performed. Only centroid based clusterings support assiging unseen examples to clusters. *
 * 
 * @author Sebastian Land
 */
public class ClusterModel2ExampleSet extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set", new ExampleSetMetaData());
	private InputPort modelInput = getInputPorts().createPort("model", ClusterModel.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort modelOutput = getOutputPorts().createPort("model");

	public static final String PARAMETER_KEEP_MODEL = "keep_model";
	public static final String PARAMETER_REMOVE_UNLABELED = "remove_unlabeled";
	public static final String PARAMETER_ADD_AS_LABEL = "add_as_label";

	public ClusterModel2ExampleSet(OperatorDescription description) {
		super(description);
		getTransformer().addRule(
				new AttributeAddingExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, new AttributeMetaData(
						Attributes.CLUSTER_NAME, Ontology.NOMINAL, Attributes.CLUSTER_NAME)));
		getTransformer().addRule(new PassThroughRule(modelInput, modelOutput, false));
	}

	public ExampleSet addClusterAttribute(ExampleSet exampleSet, ClusterModel model) throws OperatorException {
		Attributes attributes = exampleSet.getAttributes();

		// additional checks
		model.checkCapabilities(exampleSet);

		// creating attribute
		Attribute targetAttribute;
		if (!getParameterAsBoolean(PARAMETER_ADD_AS_LABEL)) {
			targetAttribute = AttributeFactory.createAttribute("cluster", Ontology.NOMINAL);
			exampleSet.getExampleTable().addAttribute(targetAttribute);
			attributes.setCluster(targetAttribute);
		} else {
			targetAttribute = AttributeFactory.createAttribute("label", Ontology.NOMINAL);
			exampleSet.getExampleTable().addAttribute(targetAttribute);
			attributes.setLabel(targetAttribute);
		}

		// setting values
		int[] clusterIndices = model.getClusterAssignments(exampleSet);
		int i = 0;
		for (Example example : exampleSet) {
			if (clusterIndices[i] != ClusterModel.UNASSIGNABLE) {
				example.setValue(targetAttribute, model.getCluster(clusterIndices[i]).toString());
			} else {
				example.setValue(targetAttribute, Double.NaN);
			}
			i++;
		}

		// removing unknown examples if desired
		if (getParameterAsBoolean(PARAMETER_REMOVE_UNLABELED)) {
			exampleSet = new ConditionedExampleSet(exampleSet, new NoMissingAttributeValueCondition(exampleSet,
					targetAttribute.getName()));
		}

		return exampleSet;
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		ClusterModel model = modelInput.getData(ClusterModel.class);
		exampleSet = addClusterAttribute(exampleSet, model);

		exampleSetOutput.deliver(exampleSet);
		modelOutput.deliver(model);
	}

	@Override
	public boolean shouldAutoConnect(OutputPort port) {
		if (port == modelOutput) {
			return getParameterAsBoolean(PARAMETER_KEEP_MODEL);
		} else {
			return super.shouldAutoConnect(port);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeBoolean(PARAMETER_ADD_AS_LABEL,
				"Should the cluster values be added as label.", false);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_REMOVE_UNLABELED, "Delete the unlabeled examples.", false);
		type.setExpert(false);
		types.add(type);

		// deprecated
		type = new ParameterTypeBoolean(PARAMETER_KEEP_MODEL, "Specifies if input model should be kept.", true);
		type.setDeprecated();
		types.add(type);

		return types;
	}

}
