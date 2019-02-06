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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.tools.math.MathFunctions;


/**
 * <p>
 * This operator calculates the correlation matrix between all attributes of the input example set.
 * Furthermore, attribute weights based on the correlations can be returned. This allows the
 * de-selection of highly correlated attributes with the help of an
 * {@link com.rapidminer.operator.features.selection.AttributeWeightSelection} operator. If no
 * weights should be created, this operator produces simply a correlation matrix which up to now
 * cannot be used by other operators but can be displayed to the user in the result tab.
 * </p>
 *
 * <p>
 * Please note that this simple implementation performs a data scan for each attribute combination
 * and might therefore take some time for non-memory example tables.
 * </p>
 *
 * @author Ingo Mierswa
 * @deprecated since 8.1, replaced by the BeltCorrelationMatrix in the Concurrency extension
 */
@Deprecated
public class CorrelationMatrixOperator extends Operator {

	public static final String PARAMETER_CREATE_WEIGHTS = "create_weights";

	public static final String PARAMETER_NORMALIZE_WEIGHTS = "normalize_weights";

	public static final String PARAMETER_SQUARED_CORRELATION = "squared_correlation";

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort matrixOutput = getOutputPorts().createPort("matrix");
	private OutputPort weightsOutput = getOutputPorts().createPort("weights");

	public CorrelationMatrixOperator(OperatorDescription description) {
		super(description);
		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addRule(new GenerateNewMDRule(matrixOutput, NumericalMatrix.class));
		getTransformer().addRule(new GenerateNewMDRule(weightsOutput, AttributeWeights.class));
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		NumericalMatrix matrix = new NumericalMatrix("Correlation", exampleSet, true);
		int numberOfAttributes = exampleSet.getAttributes().size();
		boolean squared = getParameterAsBoolean(PARAMETER_SQUARED_CORRELATION);
		boolean createWeights = getParameterAsBoolean(PARAMETER_CREATE_WEIGHTS);
		boolean normalizeWeights = getParameterAsBoolean(PARAMETER_NORMALIZE_WEIGHTS);
		int k = 0;
		long progressCounter = 0;
		getProgress().setTotal(100);
		long batch = Math.max(1L, exampleSet.getAttributes().size() * (long) exampleSet.getAttributes().size() / 100);
		Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		for (Attribute firstAttribute : regularAttributes) {
			int l = 0;
			for (Attribute secondAttribute : regularAttributes) {
				matrix.setValue(k, l,
						MathFunctions.correlation(exampleSet, firstAttribute, secondAttribute, squared || createWeights));
				l++;
				if (++progressCounter % batch == 0 || progressCounter % 1000 == 0) {
					getProgress().setCompleted((int) (progressCounter * 100
							/ (exampleSet.getAttributes().size() * (long) exampleSet.getAttributes().size())));
				}
			}
			k++;
		}

		AttributeWeights weights = new AttributeWeights();
		// use squared correlations for weights --> learning schemes should
		// be able to use both positively and negatively high correlated
		// values
		int i = 0;
		for (Attribute attribute : regularAttributes) {
			double sum = 0.0d;
			for (int j = 0; j < numberOfAttributes; j++) {
				sum += 1.0d - matrix.getValue(i, j); // actually the
				// squared value
			}
			weights.setWeight(attribute.getName(), sum / numberOfAttributes);
			i++;
		}
		if (normalizeWeights) {
			weights.normalize();
		}
		exampleSetOutput.deliver(exampleSet);
		weightsOutput.deliver(weights);
		matrixOutput.deliver(matrix);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeBoolean(PARAMETER_CREATE_WEIGHTS,
				"Indicates if attribute weights based on correlation should be calculated or if the complete matrix should be returned.",
				false);
		type.setExpert(false);
		type.setHidden(true);
		types.add(type);
		types.add(new ParameterTypeBoolean(PARAMETER_NORMALIZE_WEIGHTS,
				"Indicates if the attributes weights should be normalized.", true, false));
		types.add(new ParameterTypeBoolean(PARAMETER_SQUARED_CORRELATION,
				"Indicates if the squared correlation should be calculated.", false, false));
		return types;
	}
}
