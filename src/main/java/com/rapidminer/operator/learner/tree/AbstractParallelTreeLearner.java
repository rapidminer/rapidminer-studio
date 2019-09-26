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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.tree.criterions.AbstractColumnCriterion;
import com.rapidminer.operator.learner.tree.criterions.AccuracyColumnCriterion;
import com.rapidminer.operator.learner.tree.criterions.ColumnCriterion;
import com.rapidminer.operator.learner.tree.criterions.GainRatioColumnCriterion;
import com.rapidminer.operator.learner.tree.criterions.GiniIndexColumnCriterion;
import com.rapidminer.operator.learner.tree.criterions.InfoGainColumnCriterion;
import com.rapidminer.operator.learner.tree.criterions.LeastSquareColumnCriterion;
import com.rapidminer.operator.learner.tree.criterions.LeastSquareDistributionColumnCriterion;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AbstractPrecondition;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.InputMissingMetaDataError;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.ParameterConditionedPrecondition;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.NonEqualStringCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;


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

	public static final String[] CRITERIA_NAMES = { "gain_ratio", "information_gain", "gini_index", "accuracy",
	"least_square" };

	public static final Class<?>[] CRITERIA_CLASSES = { GainRatioColumnCriterion.class, InfoGainColumnCriterion.class,
			GiniIndexColumnCriterion.class, AccuracyColumnCriterion.class, LeastSquareColumnCriterion.class };

	public static final Class<?>[] CRITERIA_CLASSES_NEW;

	static {
		Class<?>[] criteriaClassesCopy = Arrays.copyOf(CRITERIA_CLASSES, CRITERIA_CLASSES.length);
		criteriaClassesCopy[criteriaClassesCopy.length - 1] = LeastSquareDistributionColumnCriterion.class;
		CRITERIA_CLASSES_NEW = criteriaClassesCopy;
	}

	public static final int CRITERION_GAIN_RATIO = 0;

	public static final int CRITERION_INFO_GAIN = 1;

	public static final int CRITERION_GINI_INDEX = 2;

	public static final int CRITERION_ACCURACY = 3;

	public static final int CRITERION_LEAST_SQUARE = 4;

	/** The version before a faster least square criterion was introduced */
	public static final OperatorVersion FASTER_REGRESSION = new OperatorVersion(9, 4, 0);

	private static class CriterionLabelPrecondition extends AbstractPrecondition {

		private final int valueType;
		private final Operator operator;

		public CriterionLabelPrecondition(InputPort inputPort, int valueType, Operator operator) {
			super(inputPort);
			this.valueType = valueType;
			this.operator = operator;
		}

		@Override
		public boolean isCompatible(MetaData input, CompatibilityLevel level) {
			return null != input && ExampleSet.class.isAssignableFrom(input.getObjectClass());
		}

		@Override
		public MetaData getExpectedMetaData() {
			return new ExampleSetMetaData();
		}

		@Override
		public String getDescription() {
			return "<em>expects:</em> ExampleSet";
		}

		@Override
		public void check(MetaData metaData) {
			final InputPort inputPort = getInputPort();
			if (metaData == null) {
				inputPort.addError(new InputMissingMetaDataError(inputPort, ExampleSet.class, null));
			} else {
				if (metaData instanceof ExampleSetMetaData) {
					ExampleSetMetaData emd = (ExampleSetMetaData) metaData;
					// checking if criterion matches label type
					String name = Attributes.LABEL_NAME;
					if (emd.hasSpecial(name) == MetaDataInfo.YES) {
						AttributeMetaData amd = emd.getSpecial(name);
						if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(amd.getValueType(), valueType)) {
							makeError(valueType);
						}
					}
				}
			}
		}

		private void makeError(int valueType) {
			List<QuickFix> qfixes = new ArrayList<>();
			switch (valueType) {
				case Ontology.NOMINAL:
					qfixes.add(new ParameterSettingQuickFix(operator, PARAMETER_CRITERION,
							CRITERIA_NAMES[CRITERION_GAIN_RATIO]));
					qfixes.add(new ParameterSettingQuickFix(operator, PARAMETER_CRITERION,
							CRITERIA_NAMES[CRITERION_INFO_GAIN]));
					qfixes.add(new ParameterSettingQuickFix(operator, PARAMETER_CRITERION,
							CRITERIA_NAMES[CRITERION_GINI_INDEX]));
					qfixes.add(new ParameterSettingQuickFix(operator, PARAMETER_CRITERION,
							CRITERIA_NAMES[CRITERION_ACCURACY]));
					createError(Severity.WARNING, qfixes, "tree_input.least_square_nominal",
							CRITERIA_NAMES[CRITERION_LEAST_SQUARE]);
					break;
				case Ontology.NUMERICAL:
					qfixes.add(new ParameterSettingQuickFix(operator, PARAMETER_CRITERION,
							CRITERIA_NAMES[CRITERION_LEAST_SQUARE]));
					createError(Severity.WARNING, qfixes, "tree_input.least_square_numerical",
							CRITERIA_NAMES[CRITERION_LEAST_SQUARE]);
					break;
				default:
					break;
			}

		}

		@Override
		public void assumeSatisfied() {
			getInputPort().receiveMD(new ExampleSetMetaData());
		}
	}

	private OutputPort weightsOutput = getOutputPorts().createPort("weights");

	public AbstractParallelTreeLearner(OperatorDescription description) {
		super(description);
		getTransformer().addGenerationRule(weightsOutput, AttributeWeights.class);
		// add warnings and quickfixes when criterion does not fit to label type
		InputPort exampleInput = getExampleSetInputPort();
		Precondition numericalCondition = new CriterionLabelPrecondition(exampleInput, Ontology.NOMINAL, this);
		exampleInput
		.addPrecondition(new ParameterConditionedPrecondition(exampleInput, numericalCondition, this,
				PARAMETER_CRITERION, CRITERIA_NAMES[CRITERION_LEAST_SQUARE]));
		Precondition nominalCondition = new CriterionLabelPrecondition(exampleInput, Ontology.NUMERICAL, this);
		exampleInput
		.addPrecondition(new ParameterConditionedPrecondition(exampleInput, nominalCondition, this,
				PARAMETER_CRITERION, CRITERIA_NAMES[CRITERION_ACCURACY]));
		exampleInput
		.addPrecondition(new ParameterConditionedPrecondition(exampleInput, nominalCondition, this,
				PARAMETER_CRITERION, CRITERIA_NAMES[CRITERION_GAIN_RATIO]));
		exampleInput
		.addPrecondition(new ParameterConditionedPrecondition(exampleInput, nominalCondition, this,
				PARAMETER_CRITERION, CRITERIA_NAMES[CRITERION_GINI_INDEX]));
		exampleInput
		.addPrecondition(new ParameterConditionedPrecondition(exampleInput, nominalCondition, this,
				PARAMETER_CRITERION, CRITERIA_NAMES[CRITERION_INFO_GAIN]));
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
	public Pruner getPruner() throws OperatorException {
		if (getParameterAsBoolean(PARAMETER_PRUNING)
				&& !CRITERIA_NAMES[CRITERION_LEAST_SQUARE].equals(getParameterAsString(PARAMETER_CRITERION))) {
			return new TreebasedPessimisticPruner(getParameterAsDouble(PARAMETER_CONFIDENCE), null);
		} else {
			return null;
		}
	}

	/**
	 * The split preprocessing is applied before each new split. If this method returns <code>null</code> as in the
	 * default implementation the preprocessing step is skipped. Subclasses might want to override this in order to
	 * perform some data preprocessing like random subset selections. The default implementation of this method always
	 * returns <code>null</code> independent of the seed.
	 *
	 * @param seed
	 *            the seed for the {@link RandomGenerator} used for random subset selection. Not used in the default
	 *            implementation.
	 * @return the preprocessing to use before each split
	 */
	public AttributePreprocessing getSplitPreprocessing(int seed) {
		return null;
	}

	@Override
	public Model learn(ExampleSet eSet) throws OperatorException {
		checkLabelCriterionDependency(eSet);
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
		TreePredictionModel model;
		if (root instanceof RegressionTree) {
			model = new RegressionTreeModel(exampleSet, (RegressionTree) root);
		} else {
			model = new TreeModel(exampleSet, root);
		}
		checkCalculateWeights(Collections.singletonList(model));
		return model;
	}

	/**
	 * Calculates the weights if the weight port is connected.
	 */
	protected void checkCalculateWeights(List<TreePredictionModel> models) {
		if (weightsOutput.isConnected()) {
			weightsOutput.deliver(calculateWeights(models));
		}
	}

	/**
	 * Calculates the (normalized) attribute weights for the given tree-based models.
	 */
	private AttributeWeights calculateWeights(List<TreePredictionModel> models) {
		HashMap<String, Double> attributeBenefitMap = new HashMap<>();
		for (TreePredictionModel model : models) {
			extractWeights(attributeBenefitMap, model.getRoot());
		}
		AttributeWeights weights = new AttributeWeights();
		int numberOfModels = models.size();
		for (Entry<String, Double> entry : attributeBenefitMap.entrySet()) {
			weights.setWeight(entry.getKey(), entry.getValue() / numberOfModels);
		}
		weights.relativize();
		return weights;
	}

	/**
	 * Recursively extracts the benefits from the given tree.
	 */
	private void extractWeights(HashMap<String, Double> attributeBenefitMap, Tree node) {
		String attributeName = null;
		Iterator<Edge> childIterator = node.childIterator();
		while (childIterator.hasNext()) {
			Edge edge = childIterator.next();
			// retrieve attributeName: On each edge the same
			attributeName = edge.getCondition().getAttributeName();
			break;
		}
		double benefit = node.getBenefit();
		Double knownBenefit = attributeBenefitMap.get(attributeName);
		if (knownBenefit != null) {
			benefit += knownBenefit;
		}
		attributeBenefitMap.put(attributeName, benefit);
		childIterator = node.childIterator();
		while (childIterator.hasNext()) {
			Tree child = childIterator.next().getChild();
			extractWeights(attributeBenefitMap, child);
		}
	}

	/**
	 * Checks if the criterion is allowed for the nominal/numeric label.
	 *
	 * @param exampleSet
	 *            the input example set
	 * @throws UserError
	 *             if the chi-squared criterion is used for nominal label or something else than chi-squared for numeric
	 *             label
	 */
	protected void checkLabelCriterionDependency(ExampleSet exampleSet) throws UserError {
		Attribute label = exampleSet.getAttributes().getLabel();
		boolean isChiSquared = CRITERIA_NAMES[CRITERION_LEAST_SQUARE].equals(getParameterAsString(PARAMETER_CRITERION));
		if (label.isNominal() && isChiSquared) {
			throw new UserError(this, "tree_criterion.nominal_chi", CRITERIA_NAMES[CRITERION_LEAST_SQUARE]);
		}
		if (label.isNumerical() && !isChiSquared) {
			throw new UserError(this, "tree_criterion.numerical_not_chi", CRITERIA_NAMES[CRITERION_LEAST_SQUARE],
					getParameterAsString(PARAMETER_CRITERION));
		}

	}

	protected abstract AbstractParallelTreeBuilder getTreeBuilder(ExampleSet exampleSet) throws OperatorException;

	protected ColumnCriterion createCriterion() throws OperatorException {
		Class<?>[] criteriaClasses = CRITERIA_CLASSES;
		if (getCompatibilityLevel().isAbove(FASTER_REGRESSION)) {
			criteriaClasses = CRITERIA_CLASSES_NEW;
		}
		if (getParameterAsBoolean(PARAMETER_PRE_PRUNING)) {
			return AbstractColumnCriterion.createColumnCriterion(this, getParameterAsDouble(PARAMETER_MINIMAL_GAIN),
					criteriaClasses, CRITERIA_NAMES);
		} else {
			return AbstractColumnCriterion.createColumnCriterion(this, 0, criteriaClasses, CRITERIA_NAMES);
		}
	}


	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.add(super.getIncompatibleVersionChanges(),
				FASTER_REGRESSION);
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
		type.registerDependencyCondition(
				new NonEqualStringCondition(this, PARAMETER_CRITERION, false, CRITERIA_NAMES[CRITERION_LEAST_SQUARE]));
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_CONFIDENCE,
				"The confidence level used for the pessimistic error calculation of pruning.", 0.0000001, 0.5, 0.1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_PRUNING, false, true));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_PRE_PRUNING, "Activates the pre pruning and delivers a prepruned tree.",
				true);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_MINIMAL_GAIN,
				"The minimal gain which must be achieved in order to produce a split.", 0.0d, Double.POSITIVE_INFINITY, 0.01d);
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
