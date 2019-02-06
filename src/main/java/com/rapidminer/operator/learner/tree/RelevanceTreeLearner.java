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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.features.weighting.ChiSquaredWeighting;
import com.rapidminer.operator.features.weighting.InfoGainRatioWeighting;
import com.rapidminer.operator.learner.CapabilityCheck;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.learner.Learner;
import com.rapidminer.operator.learner.tree.criterions.GainRatioCriterion;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.LearnerPrecondition;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;

import java.util.LinkedList;
import java.util.List;


/**
 * Learns a pruned decision tree based on arbitrary feature relevance measurements defined by an
 * inner operator (use for example {@link InfoGainRatioWeighting} for C4.5 and
 * {@link ChiSquaredWeighting} for CHAID. Works only for nominal attributes.
 * 
 * @author Ingo Mierswa
 */
public class RelevanceTreeLearner extends OperatorChain implements Learner {

	protected final InputPort exampleSetInput = getInputPorts().createPort("training set");
	private final OutputPort innerExampleSource = getSubprocess(0).getInnerSources().createPort("training set");
	private final InputPort weightsInnerSink = getSubprocess(0).getInnerSinks().createPort("weights");
	private final OutputPort modelOutput = getOutputPorts().createPort("model");

	public RelevanceTreeLearner(OperatorDescription description) {
		super(description, "Weighting");
		exampleSetInput.addPrecondition(new LearnerPrecondition(this, exampleSetInput));
		getTransformer().addRule(new PassThroughRule(exampleSetInput, innerExampleSource, true));
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		weightsInnerSink.addPrecondition(new SimplePrecondition(weightsInnerSink, new MetaData(AttributeWeights.class),
				false));
		getTransformer().addRule(new GenerateNewMDRule(modelOutput, TreeModel.class));
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		// some checks
		if (exampleSet.getAttributes().getLabel() == null) {
			throw new UserError(this, 105, new Object[0]);
		}
		if (exampleSet.getAttributes().size() == 0) {
			throw new UserError(this, 106, new Object[0]);
		}
		// check if the label attribute contains any missing values
		Attribute labelAtt = exampleSet.getAttributes().getLabel();
		exampleSet.recalculateAttributeStatistics(labelAtt);
		if (exampleSet.getStatistics(labelAtt, Statistics.UNKNOWN) > 0) {
			throw new UserError(this, 162, labelAtt.getName());
		}

		// check capabilities and produce errors if they are not fulfilled
		CapabilityCheck check = new CapabilityCheck(this, Tools.booleanValue(
				ParameterService.getParameterValue(CapabilityProvider.PROPERTY_RAPIDMINER_GENERAL_CAPABILITIES_WARN), true));
		check.checkLearnerCapabilities(this, exampleSet);

		Model model = learn(exampleSet);
		modelOutput.deliver(model);
	}

	@SuppressWarnings("deprecation")
	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		TreeBuilder builder = new TreeBuilder(new GainRatioCriterion(0), getTerminationCriteria(exampleSet), getPruner(),
				null, new DecisionTreeLeafCreator(), getParameterAsBoolean(DecisionTreeLearner.PARAMETER_NO_PRE_PRUNING),
				getParameterAsInt(DecisionTreeLearner.PARAMETER_NUMBER_OF_PREPRUNING_ALTERNATIVES),
				getParameterAsInt(AbstractTreeLearner.PARAMETER_MINIMAL_SIZE_FOR_SPLIT),
				getParameterAsInt(AbstractTreeLearner.PARAMETER_MINIMAL_LEAF_SIZE)) {

			@Override
			public Benefit calculateBenefit(ExampleSet exampleSet, Attribute attribute) throws OperatorException {
				return RelevanceTreeLearner.this.calculateBenefit(exampleSet, attribute);
			}
		};

		// learn tree
		Tree root = builder.learnTree(exampleSet);

		// create and return model
		return new TreeModel(exampleSet, root);
	}

	protected void applyInnerLearner(ExampleSet exampleSet) throws OperatorException {
		innerExampleSource.deliver(exampleSet);
		executeInnerLearner();
	}

	protected void executeInnerLearner() throws OperatorException {
		getSubprocess(0).execute();
	}

	protected Benefit calculateBenefit(ExampleSet exampleSet, Attribute attribute) throws OperatorException {
		ExampleSet trainingSet = (ExampleSet) exampleSet.clone();
		double weight = Double.NaN;
		if (weightsInnerSink.isConnected()) {
			applyInnerLearner(trainingSet);
			AttributeWeights weights = weightsInnerSink.getData(AttributeWeights.class);
			weight = weights.getWeight(attribute.getName());
		} else {
			getLogger().info("Weight not connected. Skipping");
		}

		if (!Double.isNaN(weight)) {
			return new Benefit(weight, attribute);
		} else {
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	public Pruner getPruner() throws OperatorException {
		if (!getParameterAsBoolean(DecisionTreeLearner.PARAMETER_NO_PRUNING)) {
			return new PessimisticPruner(getParameterAsDouble(DecisionTreeLearner.PARAMETER_CONFIDENCE),
					new DecisionTreeLeafCreator());
		} else {
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	public List<Terminator> getTerminationCriteria(ExampleSet exampleSet) throws OperatorException {
		List<Terminator> result = new LinkedList<Terminator>();
		result.add(new SingleLabelTermination());
		result.add(new NoAttributeLeftTermination());
		result.add(new EmptyTermination());
		int maxDepth = getParameterAsInt(DecisionTreeLearner.PARAMETER_MAXIMAL_DEPTH);
		if (maxDepth <= 0) {
			maxDepth = exampleSet.size();
		}
		result.add(new MaxDepthTermination(maxDepth));
		return result;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		if (capability == com.rapidminer.operator.OperatorCapability.BINOMINAL_ATTRIBUTES) {
			return true;
		}
		if (capability == com.rapidminer.operator.OperatorCapability.POLYNOMINAL_ATTRIBUTES) {
			return true;
		}
		if (capability == com.rapidminer.operator.OperatorCapability.POLYNOMINAL_LABEL) {
			return true;
		}
		if (capability == com.rapidminer.operator.OperatorCapability.BINOMINAL_LABEL) {
			return true;
		}
		if (capability == com.rapidminer.operator.OperatorCapability.WEIGHTED_EXAMPLES) {
			return true;
		}
		return false;
	}

	@Override
	public PerformanceVector getEstimatedPerformance() throws OperatorException {
		throw new UserError(this, 912, getName(), "estimation of performance not supported.");
	}

	@Override
	public AttributeWeights getWeights(ExampleSet eSet) throws OperatorException {
		throw new UserError(this, 916, getName(), "calculation of weights not supported.");
	}

	@Override
	public boolean shouldCalculateWeights() {
		return false;
	}

	@Override
	public boolean shouldEstimatePerformance() {
		return false;
	}

	@SuppressWarnings("deprecation")
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeInt(AbstractTreeLearner.PARAMETER_MINIMAL_SIZE_FOR_SPLIT,
				"The minimal size of a node in order to allow a split.", 1, Integer.MAX_VALUE, 4);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(AbstractTreeLearner.PARAMETER_MINIMAL_LEAF_SIZE, "The minimal size of all leaves.", 1,
				Integer.MAX_VALUE, 2);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(DecisionTreeLearner.PARAMETER_MAXIMAL_DEPTH, "The maximum tree depth (-1: no bound)",
				-1, Integer.MAX_VALUE, 10);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(DecisionTreeLearner.PARAMETER_CONFIDENCE, "The confidence level used for pruning.",
				0.0000001, 0.5, 0.1);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeBoolean(DecisionTreeLearner.PARAMETER_NO_PRUNING,
				"Disables the pruning and delivers an unpruned tree.", false));
		types.add(new ParameterTypeInt(DecisionTreeLearner.PARAMETER_NUMBER_OF_PREPRUNING_ALTERNATIVES,
				"The number of alternative nodes tried when prepruning would prevent a split.", 0, Integer.MAX_VALUE, 3));
		return types;
	}

}
