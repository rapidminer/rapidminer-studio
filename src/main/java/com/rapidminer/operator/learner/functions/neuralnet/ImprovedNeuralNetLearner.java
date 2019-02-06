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
package com.rapidminer.operator.learner.functions.neuralnet;

import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.RandomGenerator;


/**
 * <p>
 * This operator learns a model by means of a feed-forward neural network trained by a
 * backpropagation algorithm (multi-layer perceptron). The user can define the structure of the
 * neural network with the parameter list &quot;hidden_layers&quot;. Each list entry describes a new
 * hidden layer. The key of each entry must correspond to the layer name. The value of each entry
 * must be a number defining the size of the hidden layer. A size value of -1 indicates that the
 * layer size should be calculated from the number of attributes of the input example set. In this
 * case, the layer size will be set to (number of attributes + number of classes) / 2 + 1.
 * </p>
 *
 * <p>
 * If the user does not specify any hidden layers, a default hidden layer with sigmoid type and size
 * (number of attributes + number of classes) / 2 + 1 will be created and added to the net. If only
 * a single layer without nodes is specified, the input nodes are directly connected to the output
 * nodes and no hidden layer will be used.
 * </p>
 *
 * <p>
 * The used activation function is the usual sigmoid function. Therefore, the values ranges of the
 * attributes should be scaled to -1 and +1. This is also done by this operator if not specified
 * otherwise by the corresponding parameter setting. The type of the output node is sigmoid if the
 * learning data describes a classification task and linear for numerical regression tasks.
 * </p>
 *
 * @rapidminer.index Neural Net
 *
 * @author Ingo Mierswa
 */
public class ImprovedNeuralNetLearner extends AbstractLearner {

	/**
	 * The parameter name for &quot;The number of hidden layers. Only used if no layers are defined
	 * by the list hidden_layer_types.&quot;
	 */
	public static final String PARAMETER_HIDDEN_LAYERS = "hidden_layers";

	/**
	 * The parameter name for &quot;The number of training cycles used for the neural network
	 * training.&quot;
	 */
	public static final String PARAMETER_TRAINING_CYCLES = "training_cycles";

	/**
	 * The parameter name for &quot;The optimization is stopped if the training error gets below
	 * this epsilon value.&quot;
	 */
	public static final String PARAMETER_ERROR_EPSILON = "error_epsilon";

	/**
	 * The parameter name for &quot;The learning rate determines by how much we change the weights
	 * at each step.&quot;
	 */
	public static final String PARAMETER_LEARNING_RATE = "learning_rate";

	/**
	 * The parameter name for &quot;The momentum simply adds a fraction of the previous weight
	 * update to the current one (prevent local maxima and smoothes optimization directions).&quot;
	 */
	public static final String PARAMETER_MOMENTUM = "momentum";

	/** Indicates if the learning rate should be cooled down. */
	public static final String PARAMETER_DECAY = "decay";

	/** Indicates if the input data should be shuffled before learning. */
	public static final String PARAMETER_SHUFFLE = "shuffle";

	/** Indicates if the input data should be normalized between -1 and 1 before learning. */
	public static final String PARAMETER_NORMALIZE = "normalize";

	public ImprovedNeuralNetLearner(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		Tools.onlyNonMissingValues(exampleSet, getOperatorClassName(), this, new String[0]);

		ImprovedNeuralNetModel model = new ImprovedNeuralNetModel(exampleSet);

		List<String[]> hiddenLayers = getParameterList(PARAMETER_HIDDEN_LAYERS);
		int maxCycles = getParameterAsInt(PARAMETER_TRAINING_CYCLES);
		double maxError = getParameterAsDouble(PARAMETER_ERROR_EPSILON);
		double learningRate = getParameterAsDouble(PARAMETER_LEARNING_RATE);
		double momentum = getParameterAsDouble(PARAMETER_MOMENTUM);
		boolean decay = getParameterAsBoolean(PARAMETER_DECAY);
		boolean shuffle = getParameterAsBoolean(PARAMETER_SHUFFLE);
		boolean normalize = getParameterAsBoolean(PARAMETER_NORMALIZE);
		RandomGenerator randomGenerator = RandomGenerator.getRandomGenerator(this);

		model.train(exampleSet, hiddenLayers, maxCycles, maxError, learningRate, momentum, decay, shuffle, normalize,
				randomGenerator, this);
		return model;
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return ImprovedNeuralNetModel.class;
	}

	/**
	 * Returns true for all types of attributes and numerical and binominal labels.
	 */
	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		switch (lc) {
			case NUMERICAL_ATTRIBUTES:
			case POLYNOMINAL_LABEL:
			case BINOMINAL_LABEL:
			case NUMERICAL_LABEL:
			case WEIGHTED_EXAMPLES:
				return true;
				// $CASES-OMITTED$
			default:
				return false;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeList(
				PARAMETER_HIDDEN_LAYERS,
				"Describes the name and the size of all hidden layers.",
				new ParameterTypeString("hidden_layer_name", "The name of the hidden layer."),
				new ParameterTypeInt(
						"hidden_layer_sizes",
						"The size of the hidden layers. A size of < 0 leads to a layer size of (number_of_attributes + number of classes) / 2 + 1.",
						-1, Integer.MAX_VALUE, 2));
		type.setExpert(false);
		type.setPrimary(true);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_TRAINING_CYCLES,
				"The number of training cycles used for the neural network training.", 1, Integer.MAX_VALUE, 200);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_LEARNING_RATE,
				"The learning rate determines by how much we change the weights at each step. May not be 0.",
				Double.MIN_VALUE, 1.0d, 0.01d);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeDouble(
				PARAMETER_MOMENTUM,
				"The momentum simply adds a fraction of the previous weight update to the current one (prevent local maxima and smoothes optimization directions).",
				0.0d, 1.0d, 0.9d));

		types.add(new ParameterTypeBoolean(PARAMETER_DECAY,
				"Indicates if the learning rate should be decreased during learningh", false));

		types.add(new ParameterTypeBoolean(
				PARAMETER_SHUFFLE,
				"Indicates if the input data should be shuffled before learning (increases memory usage but is recommended if data is sorted before)",
				true));

		types.add(new ParameterTypeBoolean(
				PARAMETER_NORMALIZE,
				"Indicates if the input data should be normalized between -1 and +1 before learning (increases runtime but is in most cases necessary)",
				true));

		types.add(new ParameterTypeDouble(PARAMETER_ERROR_EPSILON,
				"The optimization is stopped if the training error gets below this epsilon value.", 0.0d,
				Double.POSITIVE_INFINITY, 0.0001d));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
}
