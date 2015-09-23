/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.operator.learner.tree;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.rapidminer.core.internal.Resources;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.preprocessing.sampling.BootstrappingOperator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.RandomGenerator;


/**
 * This operators learns a random forest. The resulting forest model contains several single random
 * tree models.
 *
 * @author Ingo Mierswa, Sebastian Land, Gisa Schaefer
 */
public class ParallelRandomForestLearner extends ParallelDecisionTreeLearner {

	public static final String PARAMETER_USE_HEURISTIC_SUBSET_RATION = "guess_subset_ratio";

	/** The parameter name for &quot;Ratio of randomly chosen attributes to test&quot; */
	public static final String PARAMETER_SUBSET_RATIO = "subset_ratio";

	/** The parameter name for the number of trees. */
	public static final String PARAMETER_NUMBER_OF_TREES = "number_of_trees";

	public ParallelRandomForestLearner(OperatorDescription description) {
		super(description);
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return RandomForestModel.class;
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		// check if the label attribute contains any missing values
		Attribute labelAtt = exampleSet.getAttributes().getLabel();
		exampleSet.recalculateAttributeStatistics(labelAtt);
		if (exampleSet.getStatistics(labelAtt, Statistics.UNKNOWN) > 0) {
			throw new UserError(this, 162, labelAtt.getName());
		}

		// learn base models
		List<TreeModel> baseModels = new LinkedList<TreeModel>();
		int numberOfTrees = getParameterAsInt(PARAMETER_NUMBER_OF_TREES);

		RandomGenerator random = RandomGenerator.getRandomGenerator(this);

		// create callables that build a random tree each
		List<Callable<TreeModel>> tasks = new ArrayList<>(numberOfTrees);
		for (int i = 0; i < numberOfTrees; i++) {
			tasks.add(new TreeCallable(exampleSet, random.nextInt()));
		}

		if (Resources.getConcurrencyContext(this).getParallelism() > 1 && tasks.size() > 1) {
			// execute in parallel
			List<TreeModel> results = null;
			try {
				results = Resources.getConcurrencyContext(this).call(tasks);
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof OperatorException) {
					throw (OperatorException) cause;
				} else if (cause instanceof RuntimeException) {
					throw (RuntimeException) cause;
				} else if (cause instanceof Error) {
					throw (Error) cause;
				} else {
					throw new OperatorException(cause.getMessage(), cause);
				}
			}
			for (TreeModel result : results) {
				baseModels.add(result);
			}
		} else {
			// execute sequential
			for (Callable<TreeModel> task : tasks) {
				try {
					baseModels.add(task.call());
				} catch (Exception e) {
					if (e instanceof OperatorException) {
						throw (OperatorException) e;
					} else if (e instanceof RuntimeException) {
						throw (RuntimeException) e;
					} else {
						throw new OperatorException(e.getMessage(), e);
					}
				}
			}
		}

		// create and return model
		return new RandomForestModel(exampleSet, baseModels);
	}

	/**
	 * Callable that applies bootstrapping to the example set and then learns a random tree. The
	 * callable has its own {@link Random} so that the randomness is independent of execution
	 * orders.
	 *
	 * @author Gisa Schaefer
	 *
	 */
	private class TreeCallable implements Callable<TreeModel> {

		private final Random callableRandom;
		private ExampleSet exampleSet;

		private TreeCallable(ExampleSet exampleSet, int seed) {
			this.callableRandom = new Random(seed);
			this.exampleSet = exampleSet;
		}

		@Override
		public TreeModel call() throws OperatorException {
			// apply bootstrapping
			BootstrappingOperator bootstrapping = getBootstrappingOperator(callableRandom.nextInt(Integer.MAX_VALUE));
			exampleSet = bootstrapping.apply(exampleSet);

			// learn random tree
			AbstractParallelTreeBuilder treeBuilder = getTreeBuilder(exampleSet, callableRandom);
			Tree tree = treeBuilder.learnTree(exampleSet);
			return generateModel(exampleSet, tree);
		}
	}

	/**
	 * Creates a BootstrappingOperator that has the given local random seed and uses a sample ration
	 * of <code>1.0</code>.
	 *
	 * @param seed
	 * @return
	 * @throws OperatorException
	 */
	private BootstrappingOperator getBootstrappingOperator(int seed) throws OperatorException {
		try {
			BootstrappingOperator bootstrapping = OperatorService.createOperator(BootstrappingOperator.class);
			bootstrapping.setParameter(BootstrappingOperator.PARAMETER_USE_WEIGHTS, "false");
			bootstrapping.setParameter(BootstrappingOperator.PARAMETER_SAMPLE_RATIO, "1.0");
			bootstrapping.setParameter(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED, "true");
			bootstrapping.setParameter(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED, "" + seed);
			return bootstrapping;
		} catch (OperatorCreationException e) {
			throw new OperatorException(getName() + ": cannot construct random forest learner: " + e.getMessage());
		}

	}

	/**
	 * Constructs a {@link NonParallelPreprocessingTreeBuilder} with termination criteria depending
	 * on the exampleSet and preprocessings depending on a random seed.
	 *
	 * @param exampleSet
	 * @param random
	 * @return
	 * @throws OperatorException
	 */
	protected AbstractParallelTreeBuilder getTreeBuilder(ExampleSet exampleSet, Random random) throws OperatorException {
		return new NonParallelPreprocessingTreeBuilder(this, createCriterion(), getTerminationCriteria(exampleSet),
				getPruner(), getSplitPreprocessing(random.nextInt(Integer.MAX_VALUE)),
				getParameterAsBoolean(PARAMETER_PRE_PRUNING),
				getParameterAsInt(PARAMETER_NUMBER_OF_PREPRUNING_ALTERNATIVES),
				getParameterAsInt(PARAMETER_MINIMAL_SIZE_FOR_SPLIT), getParameterAsInt(PARAMETER_MINIMAL_LEAF_SIZE),
				getExampleSetPreprocessing(random.nextInt(Integer.MAX_VALUE)));
	}

	/**
	 * Creates a {@link TreeModel} out of an exampleSet and a tree and names it after the operator.
	 *
	 * @param exampleSet
	 * @param tree
	 * @return
	 */
	private TreeModel generateModel(ExampleSet exampleSet, Tree tree) {
		TreeModel model = new TreeModel(exampleSet, tree);
		model.setSource(getName());
		return model;
	}

	/** Returns a random feature subset sampling. */
	@Override
	public AttributePreprocessing getSplitPreprocessing(int seed) {
		AttributePreprocessing preprocessing = null;
		try {
			preprocessing = new RandomAttributeSubsetPreprocessing(
					getParameterAsBoolean(PARAMETER_USE_HEURISTIC_SUBSET_RATION),
					getParameterAsDouble(PARAMETER_SUBSET_RATIO), RandomGenerator.getRandomGenerator(
							getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED), seed));
		} catch (UndefinedParameterError e) {
			// cannot happen
		}
		return preprocessing;
	}

	/**
	 * Returns a random feature subset sampling analogously as {@link #getSplitPreprocessing(int)}
	 * but the resulting preprocessing is applicable to example set instead of selection arrays.
	 *
	 * @param seed
	 * @return
	 */
	public SplitPreprocessing getExampleSetPreprocessing(int seed) {
		SplitPreprocessing preprocessing = null;
		try {
			preprocessing = new RandomSubsetPreprocessing(getParameterAsBoolean(PARAMETER_USE_HEURISTIC_SUBSET_RATION),
					getParameterAsDouble(PARAMETER_SUBSET_RATIO), RandomGenerator.getRandomGenerator(
							getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED), seed));
		} catch (UndefinedParameterError e) {
			// cannot happen
		}
		return preprocessing;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		if (capability == com.rapidminer.operator.OperatorCapability.BINOMINAL_ATTRIBUTES) {
			return true;
		}
		if (capability == com.rapidminer.operator.OperatorCapability.POLYNOMINAL_ATTRIBUTES) {
			return true;
		}
		if (capability == com.rapidminer.operator.OperatorCapability.NUMERICAL_ATTRIBUTES) {
			return true;
		}
		if (capability == com.rapidminer.operator.OperatorCapability.POLYNOMINAL_LABEL) {
			return true;
		}
		if (capability == com.rapidminer.operator.OperatorCapability.BINOMINAL_LABEL) {
			return true;
		}
		if (capability == com.rapidminer.operator.OperatorCapability.WEIGHTED_EXAMPLES) {
			return false;
		}
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {

		List<ParameterType> types = new LinkedList<ParameterType>();

		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_OF_TREES, "The number of learned random trees.", 1,
				Integer.MAX_VALUE, 10);
		type.setExpert(false);
		types.add(type);

		types.addAll(super.getParameterTypes());

		type = new ParameterTypeBoolean(PARAMETER_USE_HEURISTIC_SUBSET_RATION,
				"Indicates that log(m) + 1 features are used, otherwise a ratio has to be specified.", true);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_SUBSET_RATIO, "Ratio of randomly chosen attributes to test", 0.0d, 1.0d,
				0.2d);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_HEURISTIC_SUBSET_RATION, false,
				false));
		type.setExpert(false);
		types.add(type);

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}

}
