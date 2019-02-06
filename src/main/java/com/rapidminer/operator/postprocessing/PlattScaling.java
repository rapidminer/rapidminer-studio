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
package com.rapidminer.operator.postprocessing;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ModelApplicationRule;
import com.rapidminer.tools.LogService;

import java.util.Iterator;
import java.util.logging.Level;


/**
 * A scaling operator, applying the original algorithm by Platt (1999) to turn confidence scores of
 * boolean classifiers into probability estimates.
 * 
 * Unlike the original version this operator assumes that the confidence scores are already in the
 * interval of [0,1], as e.g. given for the RapidMiner boosting operators. The crude estimates are
 * then transformed into log odds, and scaled by the original transformation of Platt.
 * 
 * The operator assumes a model and an example set for scaling. It outputs a PlattScalingModel, that
 * contains both, the supplied model and the scaling step. If the example set contains a weight
 * attribute, then this operator is able to fit a model to the weighted examples.
 * 
 * @author Martin Scholz
 */
public class PlattScaling extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private InputPort modelInput = getInputPorts().createPort("prediction model", Model.class);

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort modelOutput = getOutputPorts().createPort("model");

	public PlattScaling(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new ModelApplicationRule(exampleSetInput, exampleSetOutput, modelInput, false));
		getTransformer().addGenerationRule(modelOutput, PlattScalingModel.class);
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		Model model = modelInput.getData(Model.class);

		// some checks
		if (exampleSet.getAttributes().getLabel() == null) {
			throw new UserError(this, 105, new Object[0]);
		}
		if (exampleSet.getAttributes().size() == 0) {
			throw new UserError(this, 106, new Object[0]);
		}

		final Attribute label = this.extractLabel(model, exampleSet);

		PlattParameters plattParams;
		{
			ExampleSet calibrationSet = (ExampleSet) exampleSet.clone();
			calibrationSet = model.apply(calibrationSet);
			plattParams = computeParameters(calibrationSet, label);
			PredictionModel.removePredictedLabel(calibrationSet);
		}

		PlattScalingModel scalingModel = new PlattScalingModel(exampleSet, model, plattParams);

		exampleSetOutput.deliver(scalingModel.apply(exampleSet));
		modelOutput.deliver(scalingModel);
	}

	private Attribute extractLabel(Model model, ExampleSet exampleSet) {
		if (model instanceof PredictionModel) {
			return ((PredictionModel) model).getLabel();
		}
		logWarning("Could not find label in model for Platt's Scaling, using Label of provided ExampleSet instead.");
		return exampleSet.getAttributes().getLabel();
	}

	/**
	 * Implementation of Platt' scaling algorithm as found in [Platt, 1999].
	 * 
	 * @param exampleSet
	 *            the example set for finding the model parameters. It needs to contain a predicted
	 *            label and confidence scores. Please note, that the confidence values are expected
	 *            to range from 0 to 1, e.g. already take the form of coarse probability estimates.
	 * @return an object containing the parameters A and B of Platt's scaling
	 */
	public static PlattParameters computeParameters(ExampleSet exampleSet, Attribute label) {
		// The current label indices may be different from the expected ones
		// (label stored in model).
		// The current ones are used when accessing the true label,
		// the confidences are accessed via the Strings representations.
		final String posLabelS = label.getMapping().getPositiveString();
		final int posLabel = exampleSet.getAttributes().getLabel().getMapping().mapString(posLabelS);
		final String negLabelS = label.getMapping().getNegativeString();
		final int negLabel = exampleSet.getAttributes().getLabel().getMapping().mapString(negLabelS);

		// Prefetch the weight attribute of the example set, may be null.
		final Attribute weightAttr = exampleSet.getAttributes().getWeight();

		// compute priors
		double[] priors = new double[2];
		Iterator<Example> reader = exampleSet.iterator();
		while (reader.hasNext()) {
			Example example = reader.next();
			double weight = (weightAttr == null) ? 1.0d : example.getWeight();
			priors[(int) example.getLabel()] += weight;
		}

		// initialize values to be computed: A, B
		double A = 0;
		double B = Math.log((priors[negLabel] + 1.0d) / (priors[posLabel] + 1.0d));

		double hiTarget = ((priors[posLabel] + 1) / (priors[posLabel] + 2));
		double loTarget = 1.0d / (priors[negLabel] + 2);
		double lambda = 1E-3;
		double olderr = 1E300;

		// initialize temp array to store prob. estimates
		double[] pp = new double[exampleSet.size()];
		for (int i = 0; i < pp.length; i++) {
			pp[i] = (priors[posLabel] + 1.0d) / (priors[negLabel] + priors[posLabel] + 2.0d);
		}

		int count = 0;

		for (int it = 1; it <= 100; it++) {
			double a = 0;
			double b = 0;
			double c = 0;
			double d = 0;
			double e = 0;
			double t = 0;

			// compute Hessian & gradient of error function
			reader = exampleSet.iterator();
			int index = 0;
			while (reader.hasNext()) {
				Example example = reader.next();

				if (example.getLabel() == posLabel) {
					t = hiTarget;
				} else {
					t = loTarget;
				}

				// translate predictions (confidences) into expected log odds
				// format
				double predicted = getLogOddsPosConfidence(example.getConfidence(posLabelS));

				double weight = (weightAttr == null) ? 1.0d : example.getWeight();
				double d1 = weight * (pp[index] - t);
				double d2 = weight * (pp[index] * (1 - pp[index]));

				a += predicted * predicted * d2;
				b += d2;
				c += predicted * d2;
				d += predicted * d1;
				e += d1;

				index++;
			}

			// stop if gradient is tiny
			if (Math.abs(d) < 1E-9 && Math.abs(e) < 1E-9) {
				break;
			}

			double oldA = A;
			double oldB = B;
			double err = 0;

			// Loop until goodness of fit increases
			while (true) {
				double det = (a + lambda) * (b + lambda) - c * c;
				if (det == 0) {
					lambda *= 10;
					continue;
				}
				A = oldA + ((b + lambda) * d - c * e) / det;
				B = oldB + ((a + lambda) * e - c * d) / det;
				err = 0;

				index = 0;
				while (reader.hasNext()) {
					Example example = reader.next();
					double predicted = getLogOddsPosConfidence(example.getConfidence(posLabelS));
					double weight = (weightAttr == null) ? 1.0d : example.getWeight();

					// min and max avoids NaNs:
					double oddsVal = Math.min(1E30, Math.exp(predicted * A + B));
					double p = Math.min((1.0d - 1E-30), 1.0d / (1.0d + oddsVal));

					pp[index++] = p;
					err -= weight * (t * Math.log(p) + (t - 1) * Math.log(1.0d - p));
				}

				if (err < olderr * (1.0d + 1E-7)) {
					lambda *= 0.1;
					break;
				}

				lambda *= 10;
				if (lambda >= 1E6) {
					break;
				}
			}

			double diff = err - olderr;
			double scale = 0.5 * (err + olderr + 1);
			if ((diff > -1E-3 * scale) && (diff < 1E-7 * scale)) {
				count++;
			} else {
				count = 0;
			}

			olderr = err;
			if (count == 3) {
				break;
			}
		}

		if (Double.isNaN(A) || Double.isNaN(B)) {
			A = 1.0d;
			B = 0.0d;
			exampleSet.getLog().logWarning("Discarding invalid result of Platt's scaling, using identity instead.");
		}
		return new PlattParameters(A, B);
	}

	/**
	 * Translates confidence scores in [0, 1] to those originally expected by Platt's scaling, where
	 * positive values result in positive predictions, and where the absolute value indicates the
	 * confidence in the prediction.
	 */
	public static double getLogOddsPosConfidence(double originalConfidence) {
		// avoid infinite or meaningless results by not allowing arbitrarily
		// small or large values:
		double epsilon = 1E-30;
		double confidence = Math.min(Math.max(epsilon, originalConfidence), 1.0d - epsilon);
		if (Double.isNaN(confidence)) { // error, just try to continue
			confidence = 0.5;
			// LogService.getGlobal().log("Found a NaN confidence during Platt's Scaling.",
			// LogService.WARNING);
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.operator.postprocessing.PlattScaling.found_a_nan_confidence_during_platts_scaling");
		}

		double odds = (1.0d - confidence) / confidence;

		// All we need to do is compute the logarithm,
		// the choice of the base is implicitly left to the scaling part:
		return (Math.log(odds)); // an input of 0.5 results in a return value
		// of 0
	}
}
