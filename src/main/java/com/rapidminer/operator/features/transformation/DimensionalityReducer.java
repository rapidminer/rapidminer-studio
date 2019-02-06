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
package com.rapidminer.operator.features.transformation;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.CapabilityPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.preprocessing.PreprocessingOperator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;

import java.util.List;


/**
 * This class is completely unnecessary and is only kept for compatibility reasons. The class
 * hierarchy is complete nonsense and will be dropped with one of the next versions. So if you
 * implement using this class, please implement this little code fragment below again or build a
 * more fitting class hierarchy.
 * 
 * Abstract class representing some common functionality of dimensionality reduction methods.
 * 
 * @author Michael Wurst, Ingo Mierswa
 */
@Deprecated
public abstract class DimensionalityReducer extends Operator implements CapabilityProvider {

	/** The parameter name for &quot;the number of dimensions in the result representation&quot; */
	public static final String PARAMETER_DIMENSIONS = "dimensions";

	private InputPort exampleSetInput = getInputPorts().createPort("example set input");

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set output");
	private OutputPort originalOutput = getOutputPorts().createPort("original");
	private OutputPort modelOutput = getOutputPorts().createPort("preprocessing model");

	public DimensionalityReducer(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new CapabilityPrecondition(this, exampleSetInput));

		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.SUBSET) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
				metaData.clearRegular();
				int numberOfDimensinos = getParameterAsInt(PARAMETER_DIMENSIONS);
				for (int i = 0; i < numberOfDimensinos; i++) {
					metaData.addAttribute(new AttributeMetaData("d" + i, Ontology.REAL));
				}
				return metaData;
			}
		});
		getTransformer().addRule(new GenerateNewMDRule(modelOutput, Model.class));
		getTransformer().addRule(new PassThroughRule(exampleSetInput, originalOutput, false));
	}

	/**
	 * Perform the actual dimensionality reduction.
	 */
	protected abstract double[][] dimensionalityReduction(ExampleSet es, int dimensions);

	@Override
	public void doWork() throws OperatorException {
		ExampleSet es = exampleSetInput.getData(ExampleSet.class);
		int dimensions = getParameterAsInt(PARAMETER_DIMENSIONS);

		Tools.onlyNumericalAttributes(es, "dimensionality reduction");
		Tools.isNonEmpty(es);
		Tools.checkAndCreateIds(es);

		double[][] p = dimensionalityReduction(es, dimensions);

		DimensionalityReducerModel model = new DimensionalityReducerModel(es, p, dimensions);

		if (exampleSetOutput.isConnected()) {
			exampleSetOutput.deliver(model.apply((ExampleSet) es.clone()));
		}
		originalOutput.deliver(es);
		modelOutput.deliver(model);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PreprocessingOperator.PARAMETER_RETURN_PREPROCESSING_MODEL,
				"Indicates if the preprocessing model should also be returned", false));
		ParameterType type = new ParameterTypeInt(PARAMETER_DIMENSIONS,
				"the number of dimensions in the result representation", 1, Integer.MAX_VALUE, 2);
		type.setExpert(false);
		types.add(type);
		return types;
	}
}
