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

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.GenerateModelTransformationRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.matrix.CovarianceMatrix;

import Jama.Matrix;


/**
 * Generalized Hebbian Algorithm (GHA) is an iterative method to compute principal components. From
 * a computational point of view, it can be advantageous to solve the eigenvalue problem by
 * iterative methods which do not need to compute the covariance matrix directly. This is useful
 * when the ExampleSet contains many Attributes (hundreds, thousands). The operator outputs a
 * <code>GHAModel</code>. With the <code>ModelApplier</code> you can transform the features.
 *
 * @author Daniel Hakenjos, Ingo Mierswa
 */
public class GHA extends Operator {

	/**
	 * The parameter name for &quot;Number of components to compute. If '-1' the number of
	 * attributes is taken.'&quot;
	 */
	public static final String PARAMETER_NUMBER_OF_COMPONENTS = "number_of_components";

	/** The parameter name for &quot;Number of Iterations to apply the update rule.&quot; */
	public static final String PARAMETER_NUMBER_OF_ITERATIONS = "number_of_iterations";

	/** The parameter name for &quot;The learning rate for GHA (small)&quot; */
	public static final String PARAMETER_LEARNING_RATE = "learning_rate";

	private InputPort exampleSetInput = getInputPorts().createPort("example set input");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set output");
	private OutputPort originalOutput = getOutputPorts().createPort("original");
	private OutputPort modelOutput = getOutputPorts().createPort("preprocessing model");

	public GHA(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput, Ontology.NUMERICAL));
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.SUBSET) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {
				int numberOfAttributes = metaData.getNumberOfRegularAttributes();
				metaData.clearRegular();
				// adding newly generated components
				try {
					int numberOfGeneratedAttributes = getParameterAsInt(PARAMETER_NUMBER_OF_COMPONENTS);
					numberOfGeneratedAttributes = numberOfGeneratedAttributes == -1 ? numberOfAttributes
							: numberOfGeneratedAttributes;

					for (int i = 1; i <= numberOfGeneratedAttributes; i++) {
						AttributeMetaData generatedAttribute = new AttributeMetaData("pc_" + i, Ontology.NUMERICAL);
						metaData.addAttribute(generatedAttribute);
					}
					// knowing all remaining regular attributes
					metaData.attributesAreKnown();
					return metaData;
				} catch (UndefinedParameterError e) {
					return metaData;
				}
			}
		});
		getTransformer().addPassThroughRule(exampleSetInput, originalOutput);
		getTransformer().addRule(new GenerateModelTransformationRule(exampleSetInput, modelOutput, GHAModel.class));
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		RandomGenerator random = RandomGenerator.getRandomGenerator(this);
		exampleSet.recalculateAllAttributeStatistics();

		// 1) check whether all attributes are numerical
		Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		double[] means = new double[regularAttributes.length];
		int a = 0;
		for (Attribute attribute : regularAttributes) {
			if (!attribute.isNumerical()) {
				throw new UserError(this, 104, "GHA", attribute.getName());
			}
			means[a] = exampleSet.getStatistics(attribute, Statistics.AVERAGE);
			a++;
		}

		// 2) create data and subtract the mean
		log("Initialising the weight matrix...");
		double[][] data = new double[exampleSet.size()][exampleSet.getAttributes().size()];

		int d = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			Iterator<Example> reader = exampleSet.iterator();
			for (int sample = 0; sample < exampleSet.size(); sample++) {
				Example example = reader.next();
				data[sample][d] = example.getValue(attribute) - means[d];
			}
			checkForStop();
			d++;
		}

		// init
		double learningRate = getParameterAsDouble(PARAMETER_LEARNING_RATE);
		int numberOfComponents = getParameterAsInt(PARAMETER_NUMBER_OF_COMPONENTS);
		if (numberOfComponents < 0) {
			numberOfComponents = exampleSet.getAttributes().size();
		}
		int numberOfIterations = getParameterAsInt(PARAMETER_NUMBER_OF_ITERATIONS);

		double[][] randomMatrix = new double[numberOfComponents][exampleSet.getAttributes().size()];
		for (int i = 0; i < randomMatrix.length; i++) {
			for (int j = 0; j < randomMatrix[i].length; j++) {
				randomMatrix[i][j] = random.nextDouble();
			}
		}
		Matrix W = new Matrix(randomMatrix);
		W.timesEquals(0.1d);

		log("Training with learning rate: " + learningRate);
		train(data, W, numberOfIterations, learningRate, random);

		log("Creating the model...");
		// compute eigenvalues
		// --> create covariancematrix
		// --> multiply with eigenvector
		// --> calculate eigenvalue

		Matrix covarianceMatrix = CovarianceMatrix.getCovarianceMatrix(exampleSet, this);
		Matrix tmp = W.times(covarianceMatrix);

		double[][] weights = W.getArray();
		double[][] tmparray = tmp.getArray();

		double[] eigenvalues = new double[numberOfComponents];

		for (int i = 0; i < weights.length; i++) {
			double nr = 0;
			eigenvalues[i] = 0.0d;
			for (int j = 0; j < weights[0].length; j++) {
				tmparray[i][j] = tmparray[i][j] / weights[i][j];
				if (tmparray[i][j] > 0.0d) {
					nr += 1.0d;
					eigenvalues[i] += tmparray[i][j];
				}
			}
			nr = Math.max(nr, 1.0d);
			eigenvalues[i] = eigenvalues[i] / nr;
		}

		GHAModel model = new GHAModel(exampleSet, eigenvalues, W.getArray(), means);
		if (exampleSetOutput.isConnected()) {
			exampleSetOutput.deliver(model.apply(exampleSet));
		}
		originalOutput.deliver(exampleSet);
		modelOutput.deliver(model);
	}

	private void train(double[][] data, Matrix W, int numberOfIterations, double learningRate, Random random)
			throws OperatorException {
		int sample;
		Matrix x;
		Matrix y;

		int iterlog = 1;
		while (numberOfIterations / iterlog > 10 && numberOfIterations / (iterlog * 10) >= 3) {
			iterlog *= 10;
		}

		for (int iter = 1; iter <= numberOfIterations; iter++) {
			if (iter % iterlog == 0) {
				log("Iteration " + iter);
			}

			sample = (int) (random.nextDouble() * data.length);

			// sample as matrix
			x = new Matrix(data[sample], data[sample].length);

			// create output y
			// y = W'*x;
			y = W.times(x);

			// double[rows][columns]
			double[][] yyT = y.times(y.transpose()).getArray();
			// lower triangular

			for (int row = 0; row < yyT.length; row++) {
				for (int col = row + 1; col < yyT.length; col++) {
					yyT[row][col] = 0.0d;
				}
			}

			// the lower triangular matrix
			Matrix LT = new Matrix(yyT);

			// W = W + beta*(x*y' - W*tril(y*y'));
			// beta = options.rate*options.annealfunc(iter);
			Matrix tmp1 = y.times(x.transpose());
			Matrix tmp2 = LT.times(W);
			tmp1 = tmp1.minus(tmp2);
			tmp1.timesEquals(learningRate);

			W.plusEquals(tmp1);

			double[][] w = W.getArray();
			for (int i = 0; i < w.length; i++) {
				for (int j = 0; j < w[0].length; j++) {
					if (Double.isInfinite(w[i][j]) || Double.isNaN(w[i][j])) {
						throw new OperatorException("Lost convergence at iterator " + (iter + 1) + ". Lower learning rate?");
					}
				}
			}
			checkForStop();
			// all ok continue iteration
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_OF_COMPONENTS,
				"Number of components to compute. If \'-1\' nr of attributes is taken.'", -1, Integer.MAX_VALUE, -1);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_NUMBER_OF_ITERATIONS, "Number of Iterations to apply the update rule.", 0,
				Integer.MAX_VALUE, 10);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_LEARNING_RATE, "The learning rate for GHA (small)", 0.0d,
				Double.POSITIVE_INFINITY, 0.01d);
		types.add(type);

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
}
