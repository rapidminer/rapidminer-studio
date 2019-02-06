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
package com.rapidminer.operator.preprocessing.weighting;

import Jama.Matrix;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.learner.local.LocalPolynomialRegressionModel.RegressionData;
import com.rapidminer.operator.learner.local.LocalPolynomialRegressionOperator;
import com.rapidminer.operator.learner.local.Neighborhood;
import com.rapidminer.operator.learner.local.Neighborhoods;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.container.Tupel;
import com.rapidminer.tools.math.LinearRegression;
import com.rapidminer.tools.math.VectorMath;
import com.rapidminer.tools.math.container.GeometricDataCollection;
import com.rapidminer.tools.math.container.LinearList;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasures;
import com.rapidminer.tools.math.smoothing.SmoothingKernel;
import com.rapidminer.tools.math.smoothing.SmoothingKernels;

import java.util.Collection;
import java.util.List;


/**
 * This operator performs a weighting of the examples and hence the resulting exampleset will
 * contain a new weight attribute. If a weight attribute was already included in the exampleSet, its
 * values will be used as initial values for this algorithm. If not, each example is assigned a
 * weight of 1.
 * 
 * For calculating the weights, this operator will perform a local polynomial regression for each
 * example. For more information about local polynomial regression, take a look at the operator
 * description of the local polynomial regression operator {@link LocalPolynomialRegressionOperator}
 * .
 * 
 * After the predicted result has been calculated, the residuals are computed and rescaled using
 * their median.
 * 
 * This result will be transformed by a smooth function, which cuts of values greater than a
 * threshold. This means, that examples without prediction error will gain a weight of 1, while
 * examples with an error greater than the threshold will be down weighted to 0.
 * 
 * This procedure is iterated as often as specified by the user and will result in weights, which
 * will penalize outliers heavily. This is especially useful for algorithms using the least squares
 * optimization such as Linear Regression, Polynomial Regression or Local Polynomial Regression,
 * since least square is very sensitive to outliers.
 * 
 * @author Sebastian Land
 */
public class LocalPolynomialExampleWeightingOperator extends Operator {

	public static final double ROOT_OF_SIX = Math.sqrt(6d);

	public static final String PARAMETER_NUMBER_OF_ITERATIONS = "iterations";

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	public LocalPolynomialExampleWeightingOperator(OperatorDescription description) {
		super(description);

		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
				if (metaData.containsSpecialAttribute(Attributes.WEIGHT_NAME) != MetaDataInfo.NO) {
					metaData.addAttribute(new AttributeMetaData(Attributes.WEIGHT_NAME, Ontology.REAL,
							Attributes.WEIGHT_NAME));
				}
				return metaData;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		// getting data
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		// doing the work
		exampleSet = doWork(exampleSet, this);

		exampleSetOutput.deliver(exampleSet);
	}

	public ExampleSet doWork(ExampleSet exampleSet, ParameterHandler handler) throws OperatorException {
		DistanceMeasure measure = DistanceMeasures.createMeasure(handler);
		measure.init(exampleSet, this);

		Attributes attributes = exampleSet.getAttributes();
		Attribute label = attributes.getLabel();

		// reading parameter
		Neighborhood neighborhood = Neighborhoods.createNeighborhood(handler);
		SmoothingKernel smoother = SmoothingKernels.createKernel(handler);
		int degree = handler.getParameterAsInt(LocalPolynomialRegressionOperator.PARAMETER_DEGREE);
		double ridge = handler.getParameterAsDouble(LocalPolynomialRegressionOperator.PARAMETER_RIDGE);
		int numberOfSteps = handler.getParameterAsInt(PARAMETER_NUMBER_OF_ITERATIONS);

		// creating weight attribute
		Attribute weightAttribute;
		weightAttribute = attributes.getWeight();
		if (weightAttribute == null) {
			weightAttribute = AttributeFactory.createAttribute("weight", Ontology.REAL);
			exampleSet.getExampleTable().addAttribute(weightAttribute);
			attributes.addRegular(weightAttribute);
			attributes.setSpecialAttribute(weightAttribute, Attributes.WEIGHT_NAME);
		}

		// init weight attribute with 1
		for (Example example : exampleSet) {
			example.setValue(weightAttribute, 1d);
		}

		// start iterating
		for (int step = 0; step < numberOfSteps; step++) {
			// building geometric data collection
			GeometricDataCollection<RegressionData> data = new LinearList<RegressionData>(measure);
			for (Example example : exampleSet) {
				double[] values = new double[attributes.size()];
				double labelValue = example.getValue(label);

				// copying example values
				int i = 0;
				for (Attribute attribute : attributes) {
					values[i] = example.getValue(attribute);
					i++;
				}

				// inserting into geometric data collection
				data.add(values, new RegressionData(values, labelValue, example.getValue(weightAttribute)));
			}

			// now perform regression for every data point to get residuals
			double[] residuals = new double[exampleSet.size()];
			double[] probe = new double[attributes.size()];
			int exampleIndex = 0;
			for (Example example : exampleSet) {
				// copying example values
				int i = 0;
				for (Attribute attribute : attributes) {
					probe[i] = example.getValue(attribute);
					i++;
				}

				// determining neighborhood
				Collection<Tupel<Double, RegressionData>> localExamples = neighborhood.getNeighbourhood(data, probe);
				if (localExamples.size() > 1) {
					// building matrixes
					double[][] x = new double[localExamples.size()][];
					double[][] y = new double[localExamples.size()][1];
					double[] distance = new double[localExamples.size()];
					double[] weight = new double[localExamples.size()];
					int j = 0;
					for (Tupel<Double, RegressionData> tupel : localExamples) {
						distance[j] = tupel.getFirst(); // distance
						x[j] = VectorMath.polynomialExpansion(tupel.getSecond().getExampleValues(), degree); // data
																												// itself
						y[j][0] = tupel.getSecond().getExampleLabel(); // the label
						weight[j] = tupel.getSecond().getExampleWeight();
						j++;
					}

					// finding greatest distance
					double maxDistance = Double.NEGATIVE_INFINITY;
					for (j = 0; j < distance.length; j++) {
						maxDistance = (maxDistance < distance[j]) ? distance[j] : maxDistance;
					}

					// using kernel smoother for locality weight calculation and multiply by example
					// weight
					for (j = 0; j < distance.length; j++) {
						weight[j] = weight[j] * smoother.getWeight(distance[j], maxDistance);
					}

					double[] coefficients = LinearRegression.performRegression(new Matrix(x), new Matrix(y), weight, ridge);
					double[] probeExpaneded = VectorMath.polynomialExpansion(probe, degree);

					double prediction = VectorMath.vectorMultiplication(probeExpaneded, coefficients);
					residuals[exampleIndex] = Math.abs(example.getValue(label) - prediction);

				} else {
					residuals[exampleIndex] = 0d;
				}
				exampleIndex++;
			}

			// determining mean of residuals
			double median = VectorMath.getMedian(residuals);

			// calculating and setting weight value
			exampleIndex = 0;
			for (Example example : exampleSet) {
				example.setValue(weightAttribute, calculateRobustnessWeight(residuals[exampleIndex] / median));
				exampleIndex++;
			}
		}
		return exampleSet;
	}

	private double calculateRobustnessWeight(double residual) {
		if (residual > ROOT_OF_SIX) {
			return 0d;
		}
		double toQuad = 1d - (residual * residual / 6);
		return toQuad * toQuad;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(
				LocalPolynomialRegressionOperator.PARAMETER_DEGREE,
				"Specifies the degree of the local fitted polynomial. Please keep in mind, that a higher degree than 2 will increase calculation time extremely and probably suffer from overfitting.",
				0, Integer.MAX_VALUE, 2);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(
				LocalPolynomialRegressionOperator.PARAMETER_RIDGE,
				"Specifies the ridge factor. This factor is used to penalize high coefficients. In order to aviod overfitting this might be increased.",
				0, Double.POSITIVE_INFINITY, 0.000000001);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_NUMBER_OF_ITERATIONS,
				"The number of iterations performed for weight calculation. See operator description for details.", 1,
				Integer.MAX_VALUE, 20);
		types.add(type);

		types.addAll(DistanceMeasures.getParameterTypesForNumericals(this));
		types.addAll(Neighborhoods.getParameterTypes(this));
		types.addAll(SmoothingKernels.getParameterTypes(this));

		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPorts().getPortByIndex(0),
				LocalPolynomialExampleWeightingOperator.class, null);
	}
}
