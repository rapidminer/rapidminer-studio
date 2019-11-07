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
package com.rapidminer.operator.learner.meta;

import java.util.Iterator;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Tools;


/**
 * Model for TransformedRegression. Applies the inverse transformation on the predictions of the
 * inner model.
 *
 * @author Stefan Rueping
 */
public class TransformedRegressionModel extends PredictionModel implements DelegationModel {

	private static final long serialVersionUID = -1273082758742436998L;

	public static final String[] METHODS = { "log", "logistic link", "exp", "rank", "none" };

	public static final int LOG = 0;

	public static final int LOG_LINK = 1;

	public static final int EXP = 2;

	public static final int RANK = 3;

	public static final int NONE = 4;

	private int method;

	private double[] rank;

	private double mean;

	private double stddev;

	private Model model;

	private boolean interpolate;

	private boolean zscale;

	public TransformedRegressionModel(ExampleSet exampleSet, int method, double[] rank, Model model, boolean zscale,
			double mean, double stddev, boolean interpolate) {
		super(exampleSet, null, null);
		this.method = method;
		this.rank = rank;
		this.model = model;
		this.zscale = zscale;
		this.mean = mean;
		this.stddev = stddev;
		this.interpolate = interpolate;
	}

	/** Iterates over all examples and applies this model. */
	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabelAttribute) throws OperatorException {

		ExampleSet eSet = (ExampleSet) exampleSet.clone();
		eSet = model.apply(eSet);
		Iterator<Example> reader = eSet.iterator();

		Iterator<Example> originalReader = exampleSet.iterator();
		switch (method) {
			case LOG:
				while (originalReader.hasNext()) {
					double functionValue = reader.next().getPredictedLabel();
					if (zscale) {
						// if(zscale) is quicker and has less chance of
						// numerical errors
						functionValue = functionValue * stddev + mean;
					}
					Example example = originalReader.next();
					example.setPredictedLabel(Math.exp(functionValue) - rank[0]);
				}
				break;
			case LOG_LINK:
				while (originalReader.hasNext()) {
					double functionValue = reader.next().getPredictedLabel();
					if (zscale) {
						// if(zscale) is quicker and has less chance of
						// numerical errors
						functionValue = functionValue * stddev + mean;
					}
					Example example = originalReader.next();
					double powered = Math.exp(functionValue);
					example.setPredictedLabel(powered / (1 + powered));
				}
				break;
			case EXP:
				while (originalReader.hasNext()) {
					double functionValue = reader.next().getPredictedLabel();
					if (zscale) {
						functionValue = functionValue * stddev + mean;
					}
					Example example = originalReader.next();
					example.setPredictedLabel(Math.log(functionValue));
				}
				break;
			case RANK:
				while (originalReader.hasNext()) {
					double predictedRank = reader.next().getPredictedLabel();
					if (zscale) {
						predictedRank = predictedRank * stddev + mean;
					}
					Example example = originalReader.next();
					if (interpolate) {
						int lower = (int) Math.round(Math.floor(predictedRank));
						int upper = (int) Math.round(Math.ceil(predictedRank));
						if (lower < 0) {
							lower = 0;
						}
						if (lower >= rank.length) {
							lower = rank.length - 1;
						}
						if (upper < 0) {
							upper = 0;
						}
						if (upper >= rank.length) {
							upper = rank.length - 1;
						}
						if (!(upper == lower)) {
							predictedRank = (upper - predictedRank) * rank[lower] + (predictedRank - lower) * rank[upper];
						} else {
							predictedRank = rank[lower];
						}
					} else {
						int thisRank = (int) Math.round(predictedRank);
						if (thisRank < 0) {
							thisRank = 0;
						}
						if (thisRank >= rank.length) {
							thisRank = rank.length - 1;
						}
						predictedRank = rank[thisRank];
					}
					example.setPredictedLabel(predictedRank);
				}
				break;
			case NONE:
				while (originalReader.hasNext()) {
					double functionValue = reader.next().getPredictedLabel();
					if (zscale) {
						functionValue = functionValue * stddev + mean;
					}
					Example example = originalReader.next();
					example.setPredictedLabel(functionValue);
				}
				break;
			default:
				// cannot happen
				break;
		}

		return exampleSet;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder(super.toString() + Tools.getLineSeparator());
		result.append("Method: " + METHODS[method] + Tools.getLineSeparator());
		result.append(model.toString());
		return result.toString();
	}

	@Override
	public Model getBaseModel() {
		return model;
	}

	@Override
	public String getShortInfo() {
		return "Method used: " + METHODS[method] + Tools.getLineSeparator();
	}

}
