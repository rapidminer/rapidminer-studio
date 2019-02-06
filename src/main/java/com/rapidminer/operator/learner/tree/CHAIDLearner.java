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
package com.rapidminer.operator.learner.tree;

import java.util.Iterator;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.features.weighting.ChiSquaredWeighting;
import com.rapidminer.operator.learner.tree.criterions.Criterion;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.OperatorService;


/**
 * The CHAID decision tree learner works like the
 * {@link com.rapidminer.operator.learner.tree.DecisionTreeLearner} with one exception: it used a
 * chi squared based criterion instead of the information gain or gain ratio criteria.
 *
 * @author Ingo Mierswa
 */
@SuppressWarnings("deprecation")
public class CHAIDLearner extends DecisionTreeLearner {

	public CHAIDLearner(OperatorDescription description) {
		super(description);
	}

	@Override
	protected Criterion createCriterion(double minimalGain) throws OperatorException {
		return new Criterion() {

			@Override
			public double getIncrementalBenefit() {
				throw new UnsupportedOperationException("Incremental calculation not supported.");
			}

			@Override
			public double getNominalBenefit(ExampleSet exampleSet, Attribute attribute) throws OperatorException {
				exampleSet = (ExampleSet) exampleSet.clone();
				exampleSet.getAttributes().clearRegular();
				exampleSet.getAttributes().addRegular(attribute);
				ChiSquaredWeighting weightOp = null;
				try {
					weightOp = OperatorService.createOperator(ChiSquaredWeighting.class);
				} catch (OperatorCreationException e) {
					throw new OperatorException("Cannot create chi squared calculation operator.", e);
				}
				AttributeWeights weights = weightOp.doWork(exampleSet);
				return weights.getWeight(attribute.getName());
			}

			@Override
			public double getNumericalBenefit(ExampleSet exampleSet, Attribute attribute, double splitValue) {
				throw new UnsupportedOperationException("Numerical attributes not supported.");
			}

			@Override
			public void startIncrementalCalculation(ExampleSet exampleSet) {
				throw new UnsupportedOperationException("Incremental calculation not supported.");
			}

			@Override
			public boolean supportsIncrementalCalculation() {
				return false;
			}

			@Override
			public void swapExample(Example example) {
				throw new UnsupportedOperationException("Incremental calculation not supported.");
			}

			@Override
			public double getBenefit(double[][] weightCounts) {
				throw new UnsupportedOperationException("Method not supported.");
			}
		};
	}

	/**
	 * This method calculates the benefit of the given attribute. This implementation utilizes the
	 * defined {@link Criterion}. Subclasses might want to override this method in order to
	 * calculate the benefit in other ways.
	 */
	protected Benefit calculateBenefit(ExampleSet trainingSet, Attribute attribute) throws OperatorException {
		ChiSquaredWeighting weightOp = null;
		try {
			weightOp = OperatorService.createOperator(ChiSquaredWeighting.class);
		} catch (OperatorCreationException e) {
			getLogger().warning("Cannot create chi squared calculation operator.");
			return null;
		}

		double weight = Double.NaN;
		if (weightOp != null) {
			AttributeWeights weights = weightOp.doWork(trainingSet);
			weight = weights.getWeight(attribute.getName());
		}

		if (!Double.isNaN(weight)) {
			return new Benefit(weight, attribute);
		} else {
			return null;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		// remove criterion selection
		Iterator<ParameterType> i = types.iterator();
		while (i.hasNext()) {
			ParameterType type = i.next();
			if (PARAMETER_CRITERION.equals(type.getKey())) {
				i.remove();
			} else if (PARAMETER_CONFIDENCE.equals(type.getKey())) {
				type.setDefaultValue(0.1d);
			} else if (PARAMETER_MINIMAL_GAIN.equals(type.getKey())) {
				type.setDefaultValue(0.01d);
			} else if (PARAMETER_MAXIMAL_DEPTH.equals(type.getKey())) {
				type.setDefaultValue(10);
			}
		}

		return types;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		if (capability == OperatorCapability.NUMERICAL_ATTRIBUTES) {
			return false;
		}
		return super.supportsCapability(capability);
	}
}
