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
package com.rapidminer.operator.learner.local;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.local.LocalPolynomialRegressionModel.RegressionData;
import com.rapidminer.operator.preprocessing.weighting.LocalPolynomialExampleWeightingOperator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.math.container.GeometricDataCollection;
import com.rapidminer.tools.math.container.LinearList;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasures;
import com.rapidminer.tools.math.smoothing.SmoothingKernels;

import java.util.List;


/**
 * This operator provides functionality to perform a local regression. That means, that if the label
 * value for a point in the data space is requested, the local neighborhood of this point is
 * searched. For this search the distance measure specified in the distance measure parameter is
 * used. After the neighborhood has been determined, its datapoints are used for fitting a
 * polynomial of the specified degree using the weighted least squares optimization. The value of
 * this polynom at the requested point in data space is then returned as result. During the fitting
 * of the polynom, the neighborhoods data points are weighted by their distance to the requested
 * point. Here again the distance function specified in the parameters is used. The weight is
 * calculated from the distance using the kernel smoother, specified in the parameters. The
 * resulting weight is then included into the least squares optimization. If the training example
 * set contains a weight attribute, the distance based weight is multiplied by the example's weight.
 * If the parameter use_robust_estimation is checked, a LocalPolynomialExampleWeighting is performed
 * with the same parameters as the following LocalPolynomialRegression. For different settings the
 * operator LocalPolynomialExampleWeighting might be used as a preprocessing step instead of
 * checking the parameter. The effect is, that outlier will be downweighted so that the least
 * squares fitting will not be affected by them anymore.
 * 
 * Since it is a local method, the computational need for training is minimal: In fact, each example
 * is only stored in a way which provides a fast neighborhood search during application time. Since
 * all calculations are performed during application time, it is slower than for example SVM,
 * LinearRegression or NaiveBayes. In fact it really much depends on the number of training examples
 * and the number of attributes. If a higher degree than 1 is used, the calculations take much
 * longer, because implicitly the polynomial expansion must be calculated.
 * 
 * @author Sebastian Land
 * 
 */
public class LocalPolynomialRegressionOperator extends AbstractLearner {

	public static final String PARAMETER_DEGREE = "degree";
	public static final String PARAMETER_RIDGE = "ridge_factor";
	public static final String PARAMETER_USE_EXAMPLE_WEIGHTS = "use_weights";
	public static final String PARAMETER_USE_ROBUST_ESTIMATION = "use_robust_estimation";

	public LocalPolynomialRegressionOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		DistanceMeasure measure = DistanceMeasures.createMeasure(this);
		measure.init(exampleSet, this);
		GeometricDataCollection<RegressionData> data = new LinearList<RegressionData>(measure);

		// check if weights should be used
		boolean useWeights = getParameterAsBoolean(PARAMETER_USE_EXAMPLE_WEIGHTS);
		// check if robust estimate should be performed: Then calculate weights and use it anyway
		if (getParameterAsBoolean(PARAMETER_USE_ROBUST_ESTIMATION)) {
			useWeights = true;
			LocalPolynomialExampleWeightingOperator weightingOperator;
			try {
				weightingOperator = OperatorService.createOperator(LocalPolynomialExampleWeightingOperator.class);
				exampleSet = weightingOperator.doWork((ExampleSet) exampleSet.clone(), this);
			} catch (OperatorCreationException e) {
				throw new UserError(this, 904, "LocalPolynomialExampleWeighting", e.getMessage());
			}
		}

		Attributes attributes = exampleSet.getAttributes();
		Attribute label = attributes.getLabel();
		Attribute weightAttribute = attributes.getWeight();
		for (Example example : exampleSet) {
			double[] values = new double[attributes.size()];
			double labelValue = example.getValue(label);
			double weight = 1d;
			if (weightAttribute != null && useWeights) {
				weight = example.getValue(weightAttribute);
			}

			// filter out examples without influence
			if (weight > 0d) {
				// copying example values
				int i = 0;
				for (Attribute attribute : attributes) {
					values[i] = example.getValue(attribute);
					i++;
				}

				// inserting into geometric data collection
				data.add(values, new RegressionData(values, labelValue, weight));
			}
		}
		return new LocalPolynomialRegressionModel(exampleSet, data, Neighborhoods.createNeighborhood(this),
				SmoothingKernels.createKernel(this), getParameterAsInt(PARAMETER_DEGREE),
				getParameterAsDouble(PARAMETER_RIDGE));
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case NUMERICAL_ATTRIBUTES:
			case NUMERICAL_LABEL:
				return true;
			default:
				return false;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(
				PARAMETER_DEGREE,
				"Specifies the degree of the local fitted polynomial. Please keep in mind, that a higher degree than 2 will increase calculation time extremely and probably suffer from overfitting.",
				0, Integer.MAX_VALUE, 2);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(
				PARAMETER_RIDGE,
				"Specifies the ridge factor. This factor is used to penalize high coefficients. In order to aviod overfitting this might be increased.",
				0, Double.POSITIVE_INFINITY, 0.000000001);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_USE_ROBUST_ESTIMATION,
				"If checked, a reweighting of the examples is performed in order to downweight outliers", false);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_USE_EXAMPLE_WEIGHTS,
				"Indicates if example weights should be used if present in the given example set.", true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_ROBUST_ESTIMATION, false, false));
		types.add(type);

		type = new ParameterTypeInt(LocalPolynomialExampleWeightingOperator.PARAMETER_NUMBER_OF_ITERATIONS,
				"The number of iterations performed for weight calculation.", 1, Integer.MAX_VALUE, 20);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_ROBUST_ESTIMATION, false, true));
		types.add(type);

		types.addAll(DistanceMeasures.getParameterTypesForNumericals(this));
		types.addAll(Neighborhoods.getParameterTypes(this));
		types.addAll(SmoothingKernels.getParameterTypes(this));

		return types;
	}
}
