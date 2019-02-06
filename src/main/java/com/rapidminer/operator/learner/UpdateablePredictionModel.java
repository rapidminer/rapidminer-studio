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
package com.rapidminer.operator.learner;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities.SetsCompareOption;
import com.rapidminer.example.set.ExampleSetUtilities.TypesCompareOption;
import com.rapidminer.example.set.RemappedExampleSet;
import com.rapidminer.operator.OperatorException;


/**
 * This is an abstract class for all updateable prediction models. It already implements the needed
 * functionality to ensure that the value mappings of the given example set of the updateModel
 * method are remapped to the original ones used during construction time of the model.
 *
 * @author Sebastian Land
 */
public abstract class UpdateablePredictionModel extends PredictionModel {

	private static final long serialVersionUID = -4204522134594981103L;

	protected UpdateablePredictionModel(ExampleSet trainingExampleSet) {
		super(trainingExampleSet, null, null);
	}

	protected UpdateablePredictionModel(ExampleSet trainingExampleSet, SetsCompareOption setsCompareOption,
			TypesCompareOption typesCompareOption) {
		super(trainingExampleSet, setsCompareOption, typesCompareOption);
	}

	/**
	 * This implementation returns true. Note that subclasses must provide the functionality for
	 * updating their models.
	 */
	@Override
	public final boolean isUpdatable() {
		return true;
	}

	/**
	 * This implementation remaps a given exampleSet to the header set and then calls a method of
	 * the subclass to update its model.
	 */
	@Override
	public final void updateModel(ExampleSet updateExampleSet) throws OperatorException {
		ExampleSet mappedExampleSet = RemappedExampleSet.create(updateExampleSet, getTrainingHeader(), true, true);
		checkCompatibility(mappedExampleSet);
		update(mappedExampleSet);
	}

	public abstract void update(ExampleSet updateExampleSet) throws OperatorException;
}
