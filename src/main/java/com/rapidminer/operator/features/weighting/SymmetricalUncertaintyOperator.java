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
 * <p>
 * This operator calculates the relevance of an attribute by measuring the symmetrical uncertainty
 * with respect to the class. The formulaization for this is:
 * </p>
 *
 * <code>relevance = 2 * (P(Class) - P(Class | Attribute)) / P(Class) + P(Attribute)</code>
 *
 * @author Ingo Mierswa
 */
public class SymmetricalUncertaintyOperator extends AbstractWeighting {

	private static final int PROGRESS_UPDATE_STEPS = 1_000_000;

	public SymmetricalUncertaintyOperator(OperatorDescription description) {
		super(description, true);
	}

	@Override
	protected AttributeWeights calculateWeights(ExampleSet exampleSet) throws OperatorException {
		Tools.hasNominalLabels(exampleSet, getOperatorClassName());
		Attribute label = exampleSet.getAttributes().getLabel();

		// discretize numerical data
		BinDiscretization discretization = null;
		try {
			discretization = OperatorService.createOperator(BinDiscretization.class);
		} catch (OperatorCreationException e) {
			throw new UserError(this, 904, "Discretization", e.getMessage());
		}

		int numberOfBins = getParameterAsInt(BinDiscretization.PARAMETER_NUMBER_OF_BINS);
		discretization.setParameter(BinDiscretization.PARAMETER_NUMBER_OF_BINS, numberOfBins + "");
		exampleSet = discretization.doWork(exampleSet);

		// create and deliver weights
		double totalProgress = exampleSet.getAttributes().size() * exampleSet.size();
		long progressCounter = 0;
		getProgress().setTotal(100);
		AttributeWeights weights = new AttributeWeights(exampleSet);
		for (Attribute attribute : exampleSet.getAttributes()) {
			double[][] counters = new double[attribute.getMapping().size()][label.getMapping().size()];
			for (Example example : exampleSet) {
				counters[(int) example.getValue(attribute)][(int) example.getLabel()]++;
				if (++progressCounter % PROGRESS_UPDATE_STEPS == 0) {
					getProgress().setCompleted((int) (100 * (progressCounter / totalProgress)));
				}
			}
			double weight = ContingencyTableTools.symmetricalUncertainty(counters);
			weights.setWeight(attribute.getName(), weight);
		}

		return weights;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case BINOMINAL_LABEL:
			case POLYNOMINAL_LABEL:
			case BINOMINAL_ATTRIBUTES:
			case POLYNOMINAL_ATTRIBUTES:
			case NUMERICAL_ATTRIBUTES:
				return true;
			default:
				return false;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(BinDiscretization.PARAMETER_NUMBER_OF_BINS,
				"The number of bins used for discretization of numerical attributes before the chi squared test can be performed.",
				2, Integer.MAX_VALUE, 10));
		return types;
	}
}
