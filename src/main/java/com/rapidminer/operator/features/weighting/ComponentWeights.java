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
package com.rapidminer.operator.features.weighting;

import java.util.List;

import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.features.transformation.ComponentWeightsCreatable;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;


/**
 * For models creating components like <code>PCA</code>, <code>GHA</code> and <code>FastICA</code>
 * you can create the <code>AttributeWeights</code> from a component.
 * 
 * @author Daniel Hakenjos, Ingo Mierswa
 */
public class ComponentWeights extends AbstractWeighting {

	private InputPort modelInput = getInputPorts().createPort("model", Model.class);
	private OutputPort modelOutput = getOutputPorts().createPort("model");

	/** The parameter name for &quot;Create the weights of this component.&quot; */
	public static final String PARAMETER_COMPONENT_NUMBER = "component_number";

	public ComponentWeights(OperatorDescription description) {
		super(description, false);
		getTransformer().addPassThroughRule(modelInput, modelOutput);
	}

	/**
	 * Helper method for anonymous instantiations of this class.
	 */
	public AttributeWeights doWork(Model model, ExampleSet exampleSet) throws OperatorException {
		modelInput.receive(model);
		getExampleSetInputPort().receive(exampleSet);
		doWork();
		return getWeightsOutputPort().getData(AttributeWeights.class);
	}

	@Override
	protected AttributeWeights calculateWeights(ExampleSet exampleSet) throws OperatorException {
		Model model = modelInput.getData(Model.class);

		if (!(model instanceof ComponentWeightsCreatable)) {
			throw new OperatorException(getName()
					+ ": needs an input model wich implements the ComponentWeightsCreatable interface:"
					+ model.getClass().getName());
		}

		int component = getParameterAsInt(PARAMETER_COMPONENT_NUMBER);
		AttributeWeights weights = ((ComponentWeightsCreatable) model).getWeightsOfComponent(component);

		// normalize
		if (getParameterAsBoolean(PARAMETER_NORMALIZE_WEIGHTS)) {
			weights.normalize();
		}

		modelOutput.deliver(model);
		return weights;
	}

	@Override
	protected boolean isExampleSetMandatory() {
		return false;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		return true; // unimportant since exampleset is mandatory
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> list = super.getParameterTypes();
		list.addAll(super.getParameterTypes());
		list.add(new ParameterTypeInt(PARAMETER_COMPONENT_NUMBER, "Create the weights of this component.", 1,
				Integer.MAX_VALUE, 1));
		return list;
	}
}
