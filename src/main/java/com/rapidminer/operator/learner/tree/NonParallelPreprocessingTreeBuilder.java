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

import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.learner.tree.criterions.ColumnCriterion;


/**
 * Builds a tree, not in parallel. The example set is preprocessed before starting the procedure. In each splitting step
 * (except for the first), the attribute selection is preprocessed again. The preprocessings must be non-null.
 *
 * @author Gisa Schaefer
 * @deprecated not used since 8.0
 */
@Deprecated
public class NonParallelPreprocessingTreeBuilder extends NonParallelTreeBuilder {

	private final SplitPreprocessing splitPreprocessing;

	/**
	 * Checks that the preprocessings are not null. Stores the splitPreprocessing and pipes the
	 * other parameters to the super constructor.
	 */
	public NonParallelPreprocessingTreeBuilder(Operator operator, ColumnCriterion criterion,
			List<ColumnTerminator> terminationCriteria, Pruner pruner, AttributePreprocessing preprocessing,
			boolean prePruning, int numberOfPrepruningAlternatives, int minSizeForSplit, int minLeafSize,
			SplitPreprocessing splitPreprocessing) {
		super(operator, criterion, terminationCriteria, pruner, preprocessing, prePruning, numberOfPrepruningAlternatives,
				minSizeForSplit, minLeafSize);
		// the two preprocessings must be non-zero
		if (preprocessing == null) {
			throw new IllegalArgumentException("preprocessing must not be null");
		}
		if (splitPreprocessing == null) {
			throw new IllegalArgumentException("splitPreprocessing must not be null");
		}
		this.splitPreprocessing = splitPreprocessing;
	}

	@Override
	protected ExampleSet preprocessExampleSet(ExampleSet exampleSet) {
		return splitPreprocessing.preprocess(exampleSet);
	}

}
