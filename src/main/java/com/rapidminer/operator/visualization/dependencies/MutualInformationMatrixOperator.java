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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.preprocessing.PreprocessingOperator;
import com.rapidminer.operator.preprocessing.discretization.BinDiscretization;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.math.MathFunctions;

import java.util.List;


/**
 * <p>
 * This operator calculates the mutual information matrix between all attributes of the input
 * example set. This operator produces a dependency matrix which can be displayed to the user in the
 * result tab.
 * </p>
 * 
 * <p>
 * Please note that this simple implementation performs a data scan for each attribute combination
 * and might therefore take some time for non-memory example tables.
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class MutualInformationMatrixOperator extends AbstractPairwiseMatrixOperator {

	public MutualInformationMatrixOperator(OperatorDescription description) {
		super(description);
	}

	/** This preprocessing discretizes the input example set by a view. */
	@Override
	protected ExampleSet performPreprocessing(ExampleSet eSet) throws OperatorException {
		try {
			PreprocessingOperator discretizationOperator = OperatorService.createOperator(BinDiscretization.class);
			discretizationOperator.setParameter(BinDiscretization.PARAMETER_NUMBER_OF_BINS,
					getParameterAsInt(BinDiscretization.PARAMETER_NUMBER_OF_BINS) + "");
			discretizationOperator.setParameter(PreprocessingOperator.PARAMETER_CREATE_VIEW, "true");
			return discretizationOperator.doWork((ExampleSet) eSet.clone());
		} catch (OperatorCreationException e) {
			// should not happen
			throw new OperatorException(getName() + ": Cannot create discretization operator (" + e + ").");
		}
	}

	@Override
	public String getMatrixName() {
		return "Mutual Information";
	}

	/** Calculates the mutual information for both attributes. */
	@Override
	public double getMatrixValue(ExampleSet exampleSet, Attribute firstAttribute, Attribute secondAttribute) {
		// init
		double[] firstProbabilites = new double[firstAttribute.getMapping().size()];
		double[] secondProbabilites = new double[secondAttribute.getMapping().size()];
		double[][] jointProbabilities = new double[firstAttribute.getMapping().size()][secondAttribute.getMapping().size()];
		double firstCounter = 0.0d;
		double secondCounter = 0.0d;
		double firstSecondCounter = 0.0d;

		// count values
		for (Example example : exampleSet) {
			double firstValue = example.getValue(firstAttribute);
			if (!Double.isNaN(firstValue)) {
				firstProbabilites[(int) firstValue]++;
				firstCounter++;
			}
			double secondValue = example.getValue(secondAttribute);
			if (!Double.isNaN(secondValue)) {
				secondProbabilites[(int) secondValue]++;
				secondCounter++;
			}
			if (!Double.isNaN(firstValue) && !Double.isNaN(secondValue)) {
				jointProbabilities[(int) firstValue][(int) secondValue]++;
				firstSecondCounter++;
			}
		}

		// transform to probabilities
		for (int i = 0; i < firstProbabilites.length; i++) {
			firstProbabilites[i] /= firstCounter;
		}
		for (int i = 0; i < secondProbabilites.length; i++) {
			secondProbabilites[i] /= secondCounter;
		}
		for (int i = 0; i < jointProbabilities.length; i++) {
			for (int j = 0; j < jointProbabilities[i].length; j++) {
				jointProbabilities[i][j] /= firstSecondCounter;
			}
		}

		double firstEntropy = 0.0d;
		for (int i = 0; i < firstProbabilites.length; i++) {
			if (firstProbabilites[i] > 0.0d) {
				firstEntropy += firstProbabilites[i] * MathFunctions.ld(firstProbabilites[i]);
			}
		}
		firstEntropy *= -1;

		double secondEntropy = 0.0d;
		for (int i = 0; i < secondProbabilites.length; i++) {
			if (secondProbabilites[i] > 0.0d) {
				secondEntropy += secondProbabilites[i] * MathFunctions.ld(secondProbabilites[i]);
			}
		}
		secondEntropy *= -1;

		double jointEntropy = 0.0d;
		for (int i = 0; i < jointProbabilities.length; i++) {
			for (int j = 0; j < jointProbabilities[i].length; j++) {
				if (jointProbabilities[i][j] > 0.0d) {
					jointEntropy += jointProbabilities[i][j] * MathFunctions.ld(jointProbabilities[i][j]);
				}
			}
		}
		jointEntropy *= -1;

		return firstEntropy + secondEntropy - jointEntropy;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(BinDiscretization.PARAMETER_NUMBER_OF_BINS,
				"Indicates the number of bins used for numerical attributes.", 2, Integer.MAX_VALUE, 10));
		return types;
	}
}
