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
package com.rapidminer.operator.preprocessing.filter;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.Tools;
import com.rapidminer.example.set.Condition;
import com.rapidminer.example.set.ConditionCreationException;
import com.rapidminer.example.set.ConditionedExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.features.weighting.InfoGainWeighting;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.operator.preprocessing.MaterializeDataInMemory;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.ProcessTools;
import com.rapidminer.tools.RandomGenerator;


/**
 * The operator MissingValueImpution imputes missing values by learning models for each attribute
 * (except the label) and applying those models to the data set. The learner which is to be applied
 * has to be given as inner operator. In order to specify a subset of the example set in which the
 * missing values should be imputed (e.g. to limit the imputation to only numerical attributes) the
 * corresponding attributes might be chosen by the filter parameters.
 *
 * Please be aware that depending on the ability of the inner operator to handle missing values this
 * operator might not be able to impute all missing values in some cases. This behavior leads to a
 * warning. It might hence be useful to combine this operator with a subsequent
 * MissingValueReplenishment.
 *
 * ATTENTION: This operator is currently under development and does not properly work in all cases.
 * We do not recommend the usage of this operator in production systems.
 *
 * @author Tobias Malbrecht
 */
public class MissingValueImputation extends OperatorChain {

	/**
	 * The parameter name for &quot;Order of attributes in which missing values are estimated.&quot;
	 */
	public static final String PARAMETER_ORDER = "order";

	/** The parameter name for &quot;Sort direction which is used in order strategy.&quot; */
	public static final String PARAMETER_SORT = "sort";

	/**
	 * The parameter name for &quot;Impute missing values immediately after having learned the
	 * corresponding concept and iterate.&quot;
	 */
	public static final String PARAMETER_ITERATE = "iterate";

	/**
	 * The parameter name for &quot;Learn concepts to impute missing values only on the basis of
	 * complete cases (should be used in case learning approach can not handle missing
	 * values).&quot;
	 */
	public static final String PARAMETER_LEARN_ON_COMPLETE_CASES = "learn_on_complete_cases";

	/** Chronological imputation order. */
	private static final int CHRONOLOGICAL = 0;

	/** Random imputation order. */
	private static final int RANDOM = 1;

	/** Imputation order based on the number of missing values. */
	private static final int NUMBER_OF_MISSING_VALUES = 2;

	/** Imputation order based on the information gain of the attributes. */
	private static final int INFORMATION_GAIN = 3;

	/** Order strategies names. */
	private static final String[] orderStrategies = { "chronological", "random", "number of missing values",
			"information gain" };

	/** Ascending sort order. */
	private static final int ASCENDING = 0;

	/** Sort strategies names. */
	private static final String[] sortStrategies = { "ascending", "descending" };

	/**
	 * Incompatible version, old version writes into the exampleset
	 */
	private static final OperatorVersion VERSION_MAY_WRITE_INTO_DATA = new OperatorVersion(7, 3, 1);

	private final InputPort exampleSetInput = getInputPorts().createPort("example set in", ExampleSet.class);
	private final OutputPort innerExampleSetSource = getSubprocess(0).getInnerSources().createPort("example set source");
	private final InputPort innerModelSink = getSubprocess(0).getInnerSinks().createPort("model sink");
	private final OutputPort exampleSetOutput = getOutputPorts().createPort("example set out");

	private final AttributeSubsetSelector attributeSelector = new AttributeSubsetSelector(this, exampleSetInput);

	public MissingValueImputation(OperatorDescription description) {
		super(description, "Replacement Learning");

		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, innerExampleSetSource, SetRelation.SUBSET) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {

				ExampleSetMetaData selectedSubset = attributeSelector.getMetaDataSubset(metaData, false);
				Iterator<AttributeMetaData> iterator = selectedSubset.getAllAttributes().iterator();
				// removing specials
				while (iterator.hasNext()) {
					if (iterator.next().isSpecial()) {
						iterator.remove();
					}
				}
				// setting missing values to null if according parameter is set
				if (getParameterAsBoolean(PARAMETER_LEARN_ON_COMPLETE_CASES)) {
					for (AttributeMetaData attribute : selectedSubset.getAllAttributes()) {
						attribute.setNumberOfMissingValues(new MDInteger(0));
					}
				}

				/**
				 * setting one of the regular attributes to label under the assumption that all
				 * attributes are from the same type. TODO: Set type to highest common type in
				 * ontology of all subset attributes.
				 */
				iterator = selectedSubset.getAllAttributes().iterator();
				if (iterator.hasNext()) {
					iterator.next().setRole(Attributes.LABEL_NAME);
				}

				return selectedSubset;
			}
		});
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {
				ExampleSetMetaData subset = attributeSelector.getMetaDataSubset(metaData, false);
				if (subset != null) {
					for (AttributeMetaData attribute : subset.getAllAttributes()) {
						metaData.getAttributeByName(attribute.getName()).setNumberOfMissingValues(new MDInteger(0));
					}
				}
				return metaData;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		boolean iterate = getParameterAsBoolean(PARAMETER_ITERATE);
		int order = getParameterAsInt(PARAMETER_ORDER);
		boolean ascending = getParameterAsInt(PARAMETER_SORT) == ASCENDING;
		boolean learnOnCompleteCases = getParameterAsBoolean(PARAMETER_LEARN_ON_COMPLETE_CASES);

		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		if (getCompatibilityLevel().isAbove(VERSION_MAY_WRITE_INTO_DATA)) {
			// materialize since imputed values are written into the data
			exampleSet = MaterializeDataInMemory.materializeExampleSet(exampleSet);
		}

		// delete original label which should not be learned from
		Attribute label = exampleSet.getAttributes().getLabel();
		if (label != null) {
			exampleSet.getAttributes().setLabel(null);
			exampleSet.getAttributes().remove(label);
		}

		ExampleSet imputationSet = (ExampleSet) exampleSet.clone();

		// filter example set in which missing values should be substituted
		imputationSet = attributeSelector.getSubset(exampleSet, false);

		int numberOfAttributes = imputationSet.getAttributes().size();
		Attribute[][] attributePairs = new Attribute[2][numberOfAttributes];

		imputationSet.getAttributes().setLabel(label);
		attributePairs[0] = getOrderedAttributes(imputationSet, order, ascending);
		imputationSet.getAttributes().setLabel(null);
		int imputationFailure = 0;

		ExampleSet workingSet = null;
		for (int i = 0; i < numberOfAttributes; i++) {
			// use either filtered set or original (full) set
			workingSet = (ExampleSet) imputationSet.clone();

			Attribute attribute = attributePairs[0][i];
			workingSet.getAttributes().setLabel(attribute);

			// sort out examples with missing labels
			Condition condition = null;
			try {
				condition = ConditionedExampleSet.createCondition("no_missing_labels", workingSet, "");
			} catch (ConditionCreationException e) {
				throw new UserError(this, 904, "no_missing_lables", e.getMessage());
			}
			ExampleSet learningSet = new ConditionedExampleSet(workingSet, condition);

			// if desired sort out cases with missing attribute values
			if (learnOnCompleteCases) {
				try {
					condition = ConditionedExampleSet.createCondition("no_missing_attributes", learningSet, "");
				} catch (ConditionCreationException e) {
					throw new UserError(this, 904, "no_missing_attributes", e.getMessage());
				}
				learningSet = new ConditionedExampleSet(learningSet, condition);
			}

			log("Learning imputation model for attribute " + attribute.getName() + " on " + learningSet.size()
					+ " examples.");

			// learn by applying inner subprocess
			innerExampleSetSource.deliver(learningSet);
			getSubprocess(0).execute();
			Model model = innerModelSink.getData(Model.class);

			// re-add current attribute
			workingSet = model.apply(workingSet);
			workingSet.getAttributes().setLabel(null);
			workingSet.getAttributes().addRegular(attribute);
			attributePairs[1][i] = workingSet.getAttributes().getPredictedLabel();

			// if strategy is iterative immediately impute missing values
			// after learning step
			if (iterate) {
				log("Imputating missing values in attribute " + attribute.getName() + ".");
				for (Example example : workingSet) {
					double value = example.getValue(attribute);
					if (Double.isNaN(value)) {
						example.setValue(attribute, example.getPredictedLabel());
						if (Double.isNaN(example.getPredictedLabel())) {
							imputationFailure++;

						}
					}
				}
			}
			if (imputationFailure > 0) {
				logWarning("Unable to impute " + imputationFailure + " missing values in attribute " + attribute.getName()
						+ ".");
				imputationFailure = 0;
			}
			workingSet.getAttributes().setPredictedLabel(null);
		}

		// if strategy is not iterative impute missing values not before having
		// learned all concepts
		if (!iterate) {
			for (int i = 0; i < numberOfAttributes; i++) {
				imputationFailure = 0;
				Attribute attribute = attributePairs[0][i];
				log("Imputating missing values in attribute " + attribute.getName() + ".");
				for (Example example : workingSet) {
					double value = example.getValue(attribute);
					if (Double.isNaN(value)) {
						example.setValue(attribute, example.getValue(attributePairs[1][i]));
						if (Double.isNaN(example.getValue(attributePairs[1][i]))) {
							imputationFailure++;
						}
					}
				}
				if (imputationFailure > 0) {
					logWarning("Unable to impute " + imputationFailure + " missing values in attribute "
							+ attribute.getName() + ".");
					imputationFailure = 0;
				}
			}
		}

		if (label != null) {
			exampleSet.getAttributes().addRegular(label);
			exampleSet.getAttributes().setLabel(label);
		}

		exampleSetOutput.deliver(exampleSet);
	}

	private Attribute[] getOrderedAttributes(ExampleSet exampleSet, int order, boolean ascending) throws OperatorException {
		Attribute[] sortedAttributes = new Attribute[exampleSet.getAttributes().size()];
		AttributeWeights weights = new AttributeWeights(exampleSet);

		switch (order) {
			case CHRONOLOGICAL:
				int index = 0;
				for (Attribute attribute : exampleSet.getAttributes()) {
					weights.setWeight(attribute.getName(), index);
					index++;
				}
				break;
			case RANDOM:
				RandomGenerator randomGenerator = RandomGenerator.getRandomGenerator(this);
				for (Attribute attribute : exampleSet.getAttributes()) {
					weights.setWeight(attribute.getName(), randomGenerator.nextDouble());
				}
				break;
			case NUMBER_OF_MISSING_VALUES:
				exampleSet.recalculateAllAttributeStatistics();
				for (Attribute attribute : exampleSet.getAttributes()) {
					weights.setWeight(attribute.getName(), exampleSet.getStatistics(attribute, Statistics.UNKNOWN));
				}
				break;
			case INFORMATION_GAIN:
				Tools.isLabelled(exampleSet);
				InfoGainWeighting infoGainWeightingOperator;
				try {
					infoGainWeightingOperator = OperatorService.createOperator(InfoGainWeighting.class);
				} catch (OperatorCreationException e) {
					throw new OperatorException(
							"Cannot create info gain weighting operator which is necessary for ordering the attributes.");
				}
				weights = infoGainWeightingOperator.doWork(exampleSet);
				break;
		}

		String[] attributeNames = new String[weights.size()];
		weights.getAttributeNames().toArray(attributeNames);
		int sortingOrder = (ascending ? AttributeWeights.INCREASING : AttributeWeights.DECREASING);
		weights.sortByWeight(attributeNames, sortingOrder, AttributeWeights.ABSOLUTE_WEIGHTS);
		for (int i = 0; i < attributeNames.length; i++) {
			sortedAttributes[i] = exampleSet.getAttributes().get(attributeNames[i]);
		}

		return sortedAttributes;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(ProcessTools.setSubsetSelectorPrimaryParameter(attributeSelector.getParameterTypes(), true));
		ParameterType type = new ParameterTypeBoolean(PARAMETER_ITERATE,
				"Impute missing values immediately after having learned the corresponding concept and iterate.", true);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeBoolean(
				PARAMETER_LEARN_ON_COMPLETE_CASES,
				"Learn concepts to impute missing values only on the basis of complete cases (should be used in case learning approach can not handle missing values).",
				true);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeCategory(PARAMETER_ORDER, "Order of attributes in which missing values are estimated.",
				orderStrategies, CHRONOLOGICAL));
		types.add(new ParameterTypeCategory(PARAMETER_SORT, "Sort direction which is used in order strategy.",
				sortStrategies, ASCENDING));
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPorts().getPortByIndex(0),
				MissingValueImputation.class, attributeSelector);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[] { VERSION_MAY_WRITE_INTO_DATA });
	}

}
