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
package com.rapidminer.operator.visualization.dependencies;

import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.ExampleVisualizer;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.ObjectVisualizerService;


/**
 * <p>
 * This operator creates a transition graph from the given example set. The example set must have a
 * specific structure with (at least) two columns where one column specifies the source of the
 * transition and the second column specifies the target of the transition. Optionally, a third
 * column can be specified in order to define the strength of the transition (this column can for
 * example store the number of times this transition occurred after an aggregation).
 * </p>
 *
 * <p>
 * The parameter &quot;node_description&quot; will be used for displaying information about the
 * nodes if the information is made available via an example visualization operator. The string
 * might contain macros pointing to attribute names.
 * </p>
 *
 * @author Ingo Mierswa
 */
public class TransitionGraphOperator extends Operator {

	public static final String PARAMETER_SOURCE_ATTRIBUTE = "source_attribute";

	public static final String PARAMETER_TARGET_ATTRIBUTE = "target_attribute";

	public static final String PARAMETER_STRENGTH_ATTRIBUTE = "strength_attribute";

	public static final String PARAMETER_TYPE_ATTRIBUTE = "type_attribute";

	public static final String PARAMETER_NODE_DESCRIPTION = "node_description";

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort graphOutput = getOutputPorts().createPort("transition graph");

	public TransitionGraphOperator(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition
				.getAttributesByParameter(this, PARAMETER_SOURCE_ATTRIBUTE, PARAMETER_STRENGTH_ATTRIBUTE,
						PARAMETER_TARGET_ATTRIBUTE, PARAMETER_TYPE_ATTRIBUTE)));
		// exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput,
		// Attributes.ID_NAME, Ontology.ATTRIBUTE_VALUE));

		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addRule(new GenerateNewMDRule(graphOutput, TransitionGraph.class));
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		TransitionGraph transitionGraph = createTransitionGraph(exampleSet);
		ObjectVisualizerService.addObjectVisualizer(transitionGraph, new ExampleVisualizer(exampleSet));

		exampleSetOutput.deliver(exampleSet);
		graphOutput.deliver(transitionGraph);
	}

	public TransitionGraph createTransitionGraph(ExampleSet exampleSet) throws UndefinedParameterError, UserError {
		// Tools.checkIds(exampleSet);

		String sourceAttribute = getParameterAsString(PARAMETER_SOURCE_ATTRIBUTE);
		if (exampleSet.getAttributes().get(sourceAttribute) == null) {
			throw new AttributeNotFoundError(this, PARAMETER_SOURCE_ATTRIBUTE, sourceAttribute);
		}

		String targetAttribute = getParameterAsString(PARAMETER_TARGET_ATTRIBUTE);
		if (exampleSet.getAttributes().get(targetAttribute) == null) {
			throw new AttributeNotFoundError(this, PARAMETER_TARGET_ATTRIBUTE, targetAttribute);
		}

		String strengthAttribute = null;
		if (isParameterSet(PARAMETER_STRENGTH_ATTRIBUTE)) {
			strengthAttribute = getParameterAsString(PARAMETER_STRENGTH_ATTRIBUTE);
		}
		if (strengthAttribute != null && strengthAttribute.length() > 0) {
			if (exampleSet.getAttributes().get(strengthAttribute) == null) {
				throw new AttributeNotFoundError(this, PARAMETER_STRENGTH_ATTRIBUTE, strengthAttribute);
			}
			if (!exampleSet.getAttributes().get(strengthAttribute).isNumerical()) {
				throw new UserError(this, 144, strengthAttribute, getName());
			}
		}

		String typeAttribute = null;
		if (isParameterSet(PARAMETER_TYPE_ATTRIBUTE)) {
			typeAttribute = getParameterAsString(PARAMETER_TYPE_ATTRIBUTE);
		}
		if (typeAttribute != null && typeAttribute.length() > 0) {
			if (exampleSet.getAttributes().get(typeAttribute) == null) {
				throw new AttributeNotFoundError(this, PARAMETER_TYPE_ATTRIBUTE, typeAttribute);
			}
		}

		String nodeDescription = null;
		if (isParameterSet(PARAMETER_NODE_DESCRIPTION)) {
			nodeDescription = getParameterAsString(PARAMETER_NODE_DESCRIPTION);
		}

		TransitionGraph transitionGraph = new TransitionGraph(exampleSet, sourceAttribute, targetAttribute,
				strengthAttribute, typeAttribute, nodeDescription);
		return transitionGraph;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_SOURCE_ATTRIBUTE,
				"The name of the attribute defining the sources of the transitions.", exampleSetInput, false));
		types.add(new ParameterTypeAttribute(PARAMETER_TARGET_ATTRIBUTE,
				"The name of the attribute defining the targets of the transitions.", exampleSetInput, false));
		types.add(new ParameterTypeAttribute(PARAMETER_STRENGTH_ATTRIBUTE,
				"The name of the attribute defining the strength of the transitions.", exampleSetInput, true));
		types.add(new ParameterTypeAttribute(PARAMETER_TYPE_ATTRIBUTE,
				"The name of the attribute defining the type of the transitions.", exampleSetInput, true));
		types.add(new ParameterTypeString(PARAMETER_NODE_DESCRIPTION,
				"The description of each node where columns from the example data can be used by the macro form %{COLUMN_NAME}."));
		return types;
	}
}
