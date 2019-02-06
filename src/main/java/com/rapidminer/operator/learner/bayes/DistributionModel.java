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
package com.rapidminer.operator.learner.bayes;

import java.util.Collection;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.learner.UpdateablePredictionModel;
import com.rapidminer.tools.math.distribution.Distribution;


/**
 * DistributionModel is a model for learners which estimate distributions of attribute values from
 * example sets like NaiveBayes.
 *
 * Predictions are calculated as product of the conditional probabilities for all attributes times
 * the class probability.
 *
 * The basic learning concept is to simply count occurances of classes and attribute values. This
 * means no propabilities are calculated during the learning step. This is only done before output.
 * Optionally, this calculation can apply a Laplace correction which means in particular that zero
 * probabilities are avoided which would hide information in distributions of other attributes.
 *
 * @author Tobias Malbrecht
 */
public abstract class DistributionModel extends UpdateablePredictionModel {

	private static final long serialVersionUID = -402827845291958569L;

	public DistributionModel(ExampleSet exampleSet, ExampleSetUtilities.SetsCompareOption setsCompareOption,
			ExampleSetUtilities.TypesCompareOption typesCompareOption) {
		super(exampleSet, setsCompareOption, typesCompareOption);
	}

	public abstract String[] getAttributeNames();

	public abstract int getNumberOfAttributes();

	public abstract double getLowerBound(int attributeIndex);

	public abstract double getUpperBound(int attributeIndex);

	public abstract boolean isDiscrete(int attributeIndex);

	public abstract Collection<Integer> getClassIndices();

	public abstract int getNumberOfClasses();

	public abstract String getClassName(int index);

	public abstract Distribution getDistribution(int classIndex, int attributeIndex);

	@Override
	public abstract ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws ProcessStoppedException;

}
