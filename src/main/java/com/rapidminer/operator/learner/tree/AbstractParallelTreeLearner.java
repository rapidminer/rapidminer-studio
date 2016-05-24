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
package com.rapidminer.operator.learner.tree;

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.tree.criterions.AbstractColumnCriterion;
import com.rapidminer.operator.learner.tree.criterions.AccuracyColumnCriterion;
import com.rapidminer.operator.learner.tree.criterions.ColumnCriterion;
import com.rapidminer.operator.learner.tree.criterions.GainRatioColumnCriterion;
import com.rapidminer.operator.learner.tree.criterions.GiniIndexColumnCriterion;
import com.rapidminer.operator.learner.tree.criterions.InfoGainColumnCriterion;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;


/**
 * This is the abstract super class for all decision tree learners that can learn in parallel. The
 * actual type of the tree is determined by the criterion, e.g. using gain_ratio or Gini for CART /
 * C4.5 and chi_squared for CHAID.
 *
 * @author Sebastian Land, Ingo Mierswa
 */
public abstract class AbstractParallelTreeLearner extends AbstractLearner {

	/**
	 * The parameter name for &quot;Specifies the used criterion for selecting attributes and
	 * numerical splits.&quot;
	 */
	public static final String PARAMETER_CRITERION = "criterion";

	/** The parameter name for &quot;The minimal size of all leaves.&quot; */
	public static final String PARAMETER_MINIMAL_SIZE_FOR_SPLIT = "minimal_size_for_split";

	/** The parameter name for &quot;The minimal size of all leaves.&quot; */
	public static final String PARAMETER_MINIMAL_LEAF_SIZE = "minimal_leaf_size";

	/** The parameter name for the minimal gain. */
	public static final String PARAMETER_MINIMAL_GAIN = "minimal_gain";

	/** The parameter name for the maximum tree depth. */
	public static final String PARAMETER_MAXIMAL_DEPTH = "maximal_depth";

	/** The parameter name for &quot;The confidence level used for pruning.&quot; */
	public static final String PARAMETER_CONFIDENCE = "confidence";

	/** The parameter name for &quot;Enables the pruning and delivers a pruned tree.&quot; */
	public static final String PARAMETER_PRUNING = "apply_pruning";

	public static final String PARAMETER_PRE_PRUNING = "apply_prepruning";

	public static final String PARAMETER_NUMBER_OF_PREPRUNING_ALTERNATIVES = "number_of_prepruning_alternatives";

	public static final String[] CRITERIA_NAMES = { "gain_ratio", "information_gain", "gini_index", "accuracy" };

	public static final Class<?>[] CRITERIA_CLASSES = { GainRatioColumnCriterion.class, InfoGainColumnCriterion.class,
			GiniIndexColumnCriterion.class, AccuracyColumnCriterion.class };

	public static final int CRITERION_GAIN_RATIO = 0;

	public static final int CRITERION_INFO_GAIN = 1;

	public static final int CRITERION_GINI_INDEX = 2;

	public static final int CRITERION_ACCURACY = 3;

	public AbstractParallelTreeLearner(OperatorDescription description) {
		super(description);
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return TreeModel.class;
	}

	/** Returns all termination criteria. */
	public abstract List<ColumnTerminator> getTerminationCriteria(ExampleSet exampleSet) throws OperatorException;

	/**
	 * Returns the pruner for this tree learner. If this method returns null, pruning will be
	 * disabled.
	 */
	public abstract Pruner getPruner() throws OperatorException;

	/**
	 * The split preprocessing is applied before each new split. If this method returns
	 * <code>null</code> as in the default implementation the preprocessing step is skipped.
	 * Subclasses might want to override this in order to perform some data preprocessing like
	 * random subset selections. The default implementation of this method always returns
	 * <code>null</code> independent of the seed.
	 *
	 * @param seed
	 *            the seed for the {@link RandomGenerator} used for random subset selection. Not
	 *            used in the default implementation.
	 * @return
	 */
	public AttributePreprocessing getSplitPreprocessing(int seed) {
		return null;
	}

	@Override
	public Model learn(ExampleSet eSet) throws OperatorException {
		ExampleSet exampleSet = (ExampleSet) eSet.clone();

		// check if the label attribute contains any missing values
		Attribute labelAtt = exampleSet.getAttributes().getLabel();
		exampleSet.recalculateAttributeStatistics(labelAtt);
		if (exampleSet.getStatistics(labelAtt, Statistics.UNKNOWN) > 0) {
			throw new UserError(this, 162, labelAtt.getName());
		}

		// create tree builder
		AbstractParallelTreeBuilder builder = getTreeBuilder(exampleSet);
		// learn tree
		Tree root = builder.learnTree(exampleSet);

		// create and return model
		return new TreeModel(exampleSet, root);
	}

	protected abstract AbstractParallelTreeBuilder getTreeBuilder(ExampleSet exampleSet) throws OperatorException;

	protected ColumnCriterion createCriterion() throws OperatorException {
		if (getParameterAsBoolean(PARAMETER_PRE_PRUNING)) {
			return AbstractColumnCriterion.createColumnCriterion(this, getParameterAsDouble(PARAMETER_MINIMAL_GAIN));
		} else {
			return AbstractColumnCriterion.createColumnCriterion(this, 0);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeStringCategory(PARAMETER_CRITERION,
				"Specifies the used criterion for selecting attributes and numerical splits.", CRITERIA_NAMES,
				CRITERIA_NAMES[CRITERION_GAIN_RATIO], false);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_MAXIMAL_DEPTH, "The maximum tree depth (-1: no bound)", -1, Integer.MAX_VALUE,
				20);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_PRUNING, "Activates the pruning of the tree.", true);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_CONFIDENCE,
				"The confidence level used for the pessimistic error calculation of pruning.", 0.0000001, 0.5, 0.25);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_PRUNING, false, true));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_PRE_PRUNING, "Activates the pre pruning and delivers a prepruned tree.",
				true);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_MINIMAL_GAIN,
				"The minimal gain which must be achieved in order to produce a split.", 0.0d, Double.POSITIVE_INFINITY, 0.1d);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_PRE_PRUNING, false, true));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_MINIMAL_LEAF_SIZE, "The minimal size of all leaves.", 1, Integer.MAX_VALUE, 2);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_PRE_PRUNING, false, true));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_MINIMAL_SIZE_FOR_SPLIT,
				"The minimal size of a node in order to allow a split.", 1, Integer.MAX_VALUE, 4);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_PRE_PRUNING, false, true));
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_NUMBER_OF_PREPRUNING_ALTERNATIVES,
				"The number of alternative nodes tried when prepruning would prevent a split.", 0, Integer.MAX_VALUE, 3);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_PRE_PRUNING, false, true));
		types.add(type);

		return types;
	}
}
