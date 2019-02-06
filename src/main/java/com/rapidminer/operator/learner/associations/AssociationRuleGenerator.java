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
package com.rapidminer.operator.learner.associations;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.associations.fpgrowth.FPGrowth;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.conditions.EqualTypeCondition;


/**
 * <p>
 * This operator generates association rules from frequent item sets. In RapidMiner, the process of
 * frequent item set mining is divided into two parts: first, the generation of frequent item sets
 * and second, the generation of association rules from these sets.
 * </p>
 *
 * <p>
 * For the generation of frequent item sets, you can use for example the operator {@link FPGrowth}.
 * The result will be a set of frequent item sets which could be used as input for this operator.
 * </p>
 *
 * @author Sebastian Land, Ingo Mierswa
 */
public class AssociationRuleGenerator extends Operator {

	private InputPort itemSetsInput = getInputPorts().createPort("item sets", FrequentItemSets.class);
	private OutputPort rulesOutput = getOutputPorts().createPort("rules");
	private OutputPort itemSetsOutput = getOutputPorts().createPort("item sets");

	public static final String PARAMETER_CRITERION = "criterion";

	public static final String PARAMETER_MIN_CONFIDENCE = "min_confidence";

	public static final String PARAMETER_MIN_CRITERION_VALUE = "min_criterion_value";

	public static final String PARAMETER_GAIN_THETA = "gain_theta";

	public static final String PARAMETER_LAPLACE_K = "laplace_k";

	public static final String[] CRITERIA = {"confidence", "lift", "conviction", "ps", "gain", "laplace"};

	public static final int CONFIDENCE = 0;
	public static final int LIFT = 1;
	public static final int CONVICTION = 2;
	public static final int PS = 3;
	public static final int GAIN = 4;
	public static final int LAPLACE = 5;

	public AssociationRuleGenerator(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new GenerateNewMDRule(rulesOutput, AssociationRules.class));
		getTransformer().addPassThroughRule(itemSetsInput, itemSetsOutput);
	}

	@Override
	public void doWork() throws OperatorException {
		double minValue = getParameterAsDouble(PARAMETER_MIN_CONFIDENCE);
		if (getParameterAsInt(PARAMETER_CRITERION) != CONFIDENCE) {
			minValue = getParameterAsDouble(PARAMETER_MIN_CRITERION_VALUE);
		}
		double theta = getParameterAsDouble(PARAMETER_GAIN_THETA);
		double laplaceK = getParameterAsDouble(PARAMETER_LAPLACE_K);
		int criterion = getParameterAsInt(PARAMETER_CRITERION);

		FrequentItemSets sets = itemSetsInput.getData(FrequentItemSets.class);
		AssociationRules rules = new AssociationRules();
		HashMap<Collection<Item>, Integer> setFrequencyMap = new HashMap<>();
		int numberOfTransactions = sets.getNumberOfTransactions();

		// iterating sorted over every frequent Set, generating every possible rule and building
		// frequency map
		sets.sortSets();
		int progressCounter = 0;
		getProgress().setTotal(sets.size());
		for (FrequentItemSet set : sets) {
			setFrequencyMap.put(set.getItems(), set.getFrequency());
			// generating rule by splitting set in every two parts for head and body of rule
			if (set.getItems().size() > 1) {
				PowerSet<Item> powerSet = new PowerSet<>(set.getItems());
				for (Collection<Item> premises : powerSet) {
					if (!premises.isEmpty() && premises.size() < set.getItems().size()) {
						Collection<Item> conclusion = powerSet.getComplement(premises);
						int totalFrequency = set.getFrequency();
						int preconditionFrequency = setFrequencyMap.getOrDefault(premises, 0);
						int conclusionFrequency = setFrequencyMap.getOrDefault(conclusion, 0);

						double value = getCriterionValue(totalFrequency, preconditionFrequency, conclusionFrequency,
								numberOfTransactions, theta, laplaceK, criterion);
						if (value >= minValue) {
							AssociationRule rule = new AssociationRule(premises, conclusion,
									getSupport(totalFrequency, numberOfTransactions));
							rule.setConfidence(getConfidence(totalFrequency, preconditionFrequency));
							rule.setLift(getLift(totalFrequency, preconditionFrequency, conclusionFrequency,
									numberOfTransactions));
							rule.setConviction(getConviction(totalFrequency, preconditionFrequency, conclusionFrequency,
									numberOfTransactions));
							rule.setPs(
									getPs(totalFrequency, preconditionFrequency, conclusionFrequency, numberOfTransactions));
							rule.setGain(getGain(theta, totalFrequency, preconditionFrequency,
									numberOfTransactions));
							rule.setLaplace(getLaPlace(laplaceK, totalFrequency, preconditionFrequency,
									numberOfTransactions));
							rules.addItemRule(rule);
						}
					}
				}
			}
			if (++progressCounter % 100 == 0) {
				getProgress().step(100);
			}
		}
		rules.sort();
		rulesOutput.deliver(rules);
		itemSetsOutput.deliver(sets);
	}

	private double getCriterionValue(int totalFrequency, int preconditionFrequency, int conclusionFrequency,
									 int numberOfTransactions, double theta, double laplaceK, int criterion) {
		switch (criterion) {
			case LIFT:
				return getLift(totalFrequency, preconditionFrequency, conclusionFrequency, numberOfTransactions);
			case CONVICTION:
				return getConviction(totalFrequency, preconditionFrequency, conclusionFrequency, numberOfTransactions);
			case PS:
				return getPs(totalFrequency, preconditionFrequency, conclusionFrequency, numberOfTransactions);
			case GAIN:
				return getGain(theta, totalFrequency, preconditionFrequency, numberOfTransactions);
			case LAPLACE:
				return getLaPlace(laplaceK, totalFrequency, preconditionFrequency,
						numberOfTransactions);
			case CONFIDENCE:
			default:
				return getConfidence(totalFrequency, preconditionFrequency);
		}
	}

	private double getGain(double theta, int totalFrequency, int preconditionFrequency,
						   int numberOfTransactions) {
		return getSupport(totalFrequency, numberOfTransactions)
				- theta * getSupport(preconditionFrequency, numberOfTransactions);
	}

	private double getLift(int totalFrequency, int preconditionFrequency, int conclusionFrequency,
						   int numberOfTransactions) {
		return (double) totalFrequency * (double) numberOfTransactions
				/ ((double) preconditionFrequency * conclusionFrequency);
	}

	private double getPs(int totalFrequency, int preconditionFrequency, int conclusionFrequency, int numberOfTransactions) {
		return getSupport(totalFrequency, numberOfTransactions) - getSupport(preconditionFrequency, numberOfTransactions)
				* getSupport(conclusionFrequency, numberOfTransactions);
	}

	private double getLaPlace(double k, int totalFrequency, int preconditionFrequency,
							  int numberOfTransactions) {
		return (getSupport(totalFrequency, numberOfTransactions) + 1d)
				/ (getSupport(preconditionFrequency, numberOfTransactions) + k);
	}

	private double getConviction(int totalFrequency, int preconditionFrequency, int conclusionFrequency,
								 int numberOfTransactions) {
		double numerator = (double) preconditionFrequency * (numberOfTransactions - conclusionFrequency);
		double denumerator = (double) numberOfTransactions * (preconditionFrequency - totalFrequency);
		return numerator / denumerator;
	}

	private double getConfidence(int totalFrequency, int preconditionFrequency) {
		return (double) totalFrequency / (double) preconditionFrequency;
	}

	private double getSupport(int frequency, int completeSize) {
		return (double) frequency / (double) completeSize;
	}

	@Override
	public boolean shouldAutoConnect(OutputPort port) {
		if (port == itemSetsOutput) {
			return getParameterAsBoolean("keep_frequent_item_sets");
		} else {
			return super.shouldAutoConnect(port);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeCategory(PARAMETER_CRITERION,
				"The criterion which is used for the selection of rules", CRITERIA, 0);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_MIN_CONFIDENCE, "The minimum confidence of the rules", 0.0d, 1.0d, 0.8d);
		type.setExpert(false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_CRITERION, CRITERIA, true, CONFIDENCE));
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_MIN_CRITERION_VALUE,
				"The minimum value of the rules for the selected criterion", Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY, 0.8d);
		type.setExpert(false);
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_CRITERION, CRITERIA, true, LIFT, CONVICTION, PS, GAIN, LAPLACE));
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_GAIN_THETA, "The Parameter Theta in Gain calculation",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 2d);
		type.setExpert(true);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_LAPLACE_K, "The Parameter k in LaPlace function calculation", 1,
				Double.POSITIVE_INFINITY, 1d);
		type.setExpert(true);
		types.add(type);

		return types;
	}
}