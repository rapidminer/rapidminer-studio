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
package com.rapidminer.operator.performance.cost;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.performance.PerformanceCriterion;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;


/**
 * This operator provides the ability to evaluate classification costs. Therefore a cost matrix
 * might be specified, denoting the costs for every possible classification outcome: predicted label
 * x real label. Costs will be minimized during optimization.
 * 
 * @author Sebastian Land
 */
public class RankingEvaluator extends Operator {

	private static final String PARAMETER_RANKING_COSTS = "ranking_costs";
	private static final String PARAMETER_RANK_START = "rank_interval_start";
	private static final String PARAMETER_RANK_COST = "costs";

	/** Up to and including version 9.0.2 the wrong fitness was returned by the RankingCriterion */
	static final OperatorVersion WRONG_FITNESS = new OperatorVersion(9, 0, 2);

	private InputPort exampleSetInput = getInputPorts().createPort("example set");

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort performanceOutput = getOutputPorts().createPort("performance");

	private PerformanceVector performance = null;

	public RankingEvaluator(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput, Ontology.ATTRIBUTE_VALUE,
				Attributes.LABEL_NAME));
		getTransformer().addGenerationRule(performanceOutput, PerformanceVector.class);
		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);

		addValue(new ValueDouble("ranking_cost", "blubb") {

			@Override
			public double getDoubleValue() {
				if (performance == null) {
					return Double.NaN;
				}
				PerformanceCriterion c = performance.getCriterion("RankingCosts");

				if (c != null) {
					return c.getAverage();
				} else {
					return Double.NaN;
				}
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		Tools.hasNominalLabels(exampleSet, getOperatorClassName());
		List<String[]> rankings = getParameterList(PARAMETER_RANKING_COSTS);
		// Sort rankings
		if (getCompatibilityLevel().isAbove(WRONG_FITNESS)) {
			rankings.sort(Comparator.comparingInt(o -> Integer.parseInt(o[0])));
		}
		int i = 0;
		double[] costs = new double[rankings.size()];
		int[] indices = new int[rankings.size()];
		for (String[] pair : rankings) {
			indices[i] = Integer.parseInt(pair[0]);
			costs[i] = Double.parseDouble(pair[1]);
			i++;
		}

		RankingCriterion criterion = new RankingCriterion(indices, costs, exampleSet);
		criterion.setVersion(getCompatibilityLevel());
		performance = new PerformanceVector();
		performance.addCriterion(criterion);
		// now measuring costs
		criterion.startCounting(exampleSet, false);
		for (Example example : exampleSet) {
			criterion.countExample(example);
		}

		exampleSetOutput.deliver(exampleSet);
		performanceOutput.deliver(performance);
	}

	@Override
	public int checkProperties() {
		int errorCount = super.checkProperties();
		if (isEnabled()) {
			try {
				List<String[]> rankings = getParameterList(PARAMETER_RANKING_COSTS);
				if (rankings.isEmpty()) {
					List<QuickFix> quickFixes = Collections.singletonList(new ParameterSettingQuickFix(this, PARAMETER_RANKING_COSTS));
					addError(new SimpleProcessSetupError(ProcessSetupError.Severity.ERROR, getPortOwner(), quickFixes,
							"parameter_list_undefined", PARAMETER_RANKING_COSTS.replace('_', ' ')));
					errorCount++;
				}
			} catch (UndefinedParameterError pe) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.operator.performance.cost.RankingEvaluator.parameter_undefined", PARAMETER_RANKING_COSTS);
			}
		}
		return errorCount;
	}	

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeList(
				PARAMETER_RANKING_COSTS,
				"This parameter defines the costs when the real call isn't the one with the highest confidence.",
				new ParameterTypeInt(
						PARAMETER_RANK_START,
						"This is the first rank of the interval between this and the nearest greater defined rank. Each of these ranks get assigned this value. Rank counting starts with 0.",
						0, Integer.MAX_VALUE), new ParameterTypeDouble(PARAMETER_RANK_COST,
				"This is the cost of all ranks within this range.", Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY), false);
		type.setPrimary(true);
		types.add(type);
		return types;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[]{WRONG_FITNESS});
	}
}
