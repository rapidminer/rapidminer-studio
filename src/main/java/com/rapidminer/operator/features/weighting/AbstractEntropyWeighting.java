/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.tree.NumericalSplitter;
import com.rapidminer.operator.learner.tree.criterions.Criterion;


/**
 * This operator calculates the relevance of a feature by computing the an entropy value of the
 * class distribution, if the given example set would have been splitted according to the feature.
 * 
 * @author Ingo Mierswa
 */
public abstract class AbstractEntropyWeighting extends AbstractWeighting {

	public AbstractEntropyWeighting(OperatorDescription description) {
		super(description, true);
	}

	public abstract Criterion getEntropyCriterion();

	@Override
	protected AttributeWeights calculateWeights(ExampleSet exampleSet) throws OperatorException {
		Attribute label = exampleSet.getAttributes().getLabel();
		if (!label.isNominal()) {
			throw new UserError(this, 101, getName(), label.getName());
		}

		// calculate the actual information gain values and assign them to weights
		Criterion criterion = getEntropyCriterion();
		NumericalSplitter splitter = new NumericalSplitter(criterion);
		AttributeWeights weights = new AttributeWeights(exampleSet);
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNominal()) {
				double weight = criterion.getNominalBenefit(exampleSet, attribute);
				weights.setWeight(attribute.getName(), weight);
			} else {
				double splitValue = splitter.getBestSplit(exampleSet, attribute);
				double weight = criterion.getNumericalBenefit(exampleSet, attribute, splitValue);
				weights.setWeight(attribute.getName(), weight);
			}
		}
		return weights;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case BINOMINAL_LABEL:
			case POLYNOMINAL_LABEL:
			case NUMERICAL_ATTRIBUTES:
			case BINOMINAL_ATTRIBUTES:
			case POLYNOMINAL_ATTRIBUTES:
				return true;
			default:
				return false;
		}
	}
}
