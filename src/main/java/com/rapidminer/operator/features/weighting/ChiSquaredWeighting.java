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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.preprocessing.discretization.BinDiscretization;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.math.ContingencyTableTools;


/**
 * This operator calculates the relevance of a feature by computing for each attribute of the input
 * example set the value of the chi-squared statistic with respect to the class attribute.
 *
 * @author Ingo Mierswa
 */
public class ChiSquaredWeighting extends AbstractWeighting {

	private static final int PROGRESS_UPDATE_STEPS = 1_000_000;

	public ChiSquaredWeighting(OperatorDescription description) {
		super(description, true);
	}

	@Override
	protected AttributeWeights calculateWeights(ExampleSet exampleSet) throws OperatorException {
		Tools.hasNominalLabels(exampleSet, getOperatorClassName());
		Attribute label = exampleSet.getAttributes().getLabel();

		BinDiscretization discretization = null;
		try {
			discretization = OperatorService.createOperator(BinDiscretization.class);
		} catch (OperatorCreationException e) {
			throw new UserError(this, 904, "Discretization", e.getMessage());
		}

		int numberOfBins = getParameterAsInt(BinDiscretization.PARAMETER_NUMBER_OF_BINS);
		discretization.setParameter(BinDiscretization.PARAMETER_NUMBER_OF_BINS, numberOfBins + "");
		exampleSet = discretization.doWork(exampleSet);

		int maximumNumberOfNominalValues = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNominal()) {
				maximumNumberOfNominalValues = Math.max(maximumNumberOfNominalValues, attribute.getMapping().size());
			}
		}

		if (numberOfBins < maximumNumberOfNominalValues) {
			getLogger().warning("Number of bins too small, was " + numberOfBins
					+ ". Set to maximum number of occurring nominal values (" + maximumNumberOfNominalValues + ")");
			numberOfBins = maximumNumberOfNominalValues;
		}

		// init
		double[][][] counters = new double[exampleSet.getAttributes().size()][numberOfBins][label.getMapping().size()];
		Attribute weightAttribute = exampleSet.getAttributes().getWeight();

		// count
		double[] temporaryCounters = new double[label.getMapping().size()];
		for (Example example : exampleSet) {
			double weight = 1.0d;
			if (weightAttribute != null) {
				weight = example.getValue(weightAttribute);
			}
			int labelIndex = (int) example.getLabel();
			temporaryCounters[labelIndex] += weight;
		}

		for (int k = 0; k < counters.length; k++) {
			for (int i = 0; i < temporaryCounters.length; i++) {
				counters[k][0][i] = temporaryCounters[i];
			}
		}

		// attribute counts
		getProgress().setTotal(100);
		long progressCounter = 0;
		double totalProgress = exampleSet.size() * exampleSet.getAttributes().size();

		int attributeCounter = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			for (Example example : exampleSet) {
				int labelIndex = (int) example.getLabel();
				double weight = 1.0d;
				if (weightAttribute != null) {
					weight = example.getValue(weightAttribute);
				}
				int attributeIndex = (int) example.getValue(attribute);
				counters[attributeCounter][attributeIndex][labelIndex] += weight;
				counters[attributeCounter][0][labelIndex] -= weight;
				if (++progressCounter % PROGRESS_UPDATE_STEPS == 0) {
					getProgress().setCompleted((int) (100 * (progressCounter / totalProgress)));
				}
			}
			attributeCounter++;
		}

		// calculate the actual chi-squared values and assign them to weights
		AttributeWeights weights = new AttributeWeights(exampleSet);
		attributeCounter = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			double weight = ContingencyTableTools
					.getChiSquaredStatistics(ContingencyTableTools.deleteEmpty(counters[attributeCounter]), false);
			weights.setWeight(attribute.getName(), weight);
			attributeCounter++;
		}

		return weights;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(BinDiscretization.PARAMETER_NUMBER_OF_BINS,
				"The number of bins used for discretization of numerical attributes before the chi squared test can be performed.",
				2, Integer.MAX_VALUE, 10));
		return types;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case BINOMINAL_ATTRIBUTES:
			case POLYNOMINAL_ATTRIBUTES:
			case NUMERICAL_ATTRIBUTES:
			case BINOMINAL_LABEL:
			case POLYNOMINAL_LABEL:
				return true;
			default:
				return false;
		}
	}
}
