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
package com.rapidminer.operator.preprocessing.discretization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.quickfix.AttributeToNominalQuickFixProvider;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.math.MathFunctions;


/**
 * <p>
 * This operator discretizes all numeric attributes in the dataset into nominal attributes. The
 * discretization is performed by selecting a bin boundary minimizing the entropy in the induced
 * partitions. The method is then applied recursively for both new partitions until the stopping
 * criterion is reached. For Details see a) Multi-interval discretization of continued-values
 * attributes for classification learning (Fayyad,Irani) and b) Supervised and Unsupervised
 * Discretization (Dougherty,Kohavi,Sahami). Skips all special attributes including the label.
 * </p>
 *
 * <p>
 * Please note that this operator automatically removes all attributes with only one range (i.e.
 * those attributes which are not actually discretized since the entropy criterion is not
 * fulfilled). This behavior can be controlled by the remove_useless parameter.
 * </p>
 *
 * @author Sebastian Land, Dirk Dach
 */
public class MinimalEntropyDiscretization extends AbstractDiscretizationOperator {

	static {
		registerDiscretizationOperator(FrequencyDiscretization.class);
	}

	/** Indicates if long range names should be used. */
	public static final String PARAMETER_USE_LONG_RANGE_NAMES = "use_long_range_names";

	/**
	 * Indicates if useless discretized attributes, i.e. such attributes with only a single range
	 * after discretization should be removed.
	 */
	public static final String PARAMETER_REMOVE_USELESS = "remove_useless";

	/**
	 * Incompatible version, old version writes into the exampleset, if original output port is not
	 * connected.
	 */
	private static final OperatorVersion VERSION_MAY_WRITE_INTO_DATA = new OperatorVersion(7, 1, 1);

	public MinimalEntropyDiscretization(OperatorDescription description) {
		super(description);
	}

	@Override
	protected void checkSelectedSubsetMetaData(ExampleSetMetaData subsetMetaData) {
		switch (subsetMetaData.containsSpecialAttribute(Attributes.LABEL_NAME)) {
			case YES:
				AttributeMetaData labelMD = subsetMetaData.getLabelMetaData();
				if (!labelMD.isNominal()) {
					getExampleSetInputPort().addError(
							new SimpleMetaDataError(Severity.ERROR, getExampleSetInputPort(), AttributeToNominalQuickFixProvider.labelToNominal(getExampleSetInputPort(), labelMD), "attribute_has_wrong_type",
									labelMD.getName(), Ontology.VALUE_TYPE_NAMES[Ontology.NOMINAL]));
				}
				break;
			case NO:
				getExampleSetInputPort().addError(
						new SimpleMetaDataError(Severity.ERROR, getExampleSetInputPort(), "special_missing", "label"));
				break;
			case UNKNOWN:
			default:
				getExampleSetInputPort().addError(
						new SimpleMetaDataError(Severity.WARNING, getExampleSetInputPort(), "special_unknown", "label"));
				break;
		}
	}

	@Override
	public PreprocessingModel createPreprocessingModel(ExampleSet exampleSet) throws OperatorException {
		HashMap<Attribute, double[]> rangesMap = new HashMap<Attribute, double[]>();
		double[][] ranges = getRanges(exampleSet);
		int attributeIndex = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNumerical()) {
				ranges[attributeIndex][ranges[attributeIndex].length - 1] = Double.POSITIVE_INFINITY;
				rangesMap.put(attribute, ranges[attributeIndex]);
				attributeIndex++;
			}
		}

		DiscretizationModel model = new DiscretizationModel(exampleSet, getParameterAsBoolean(PARAMETER_REMOVE_USELESS));

		// determine number of digits
		int numberOfDigits = -1;
		if (getParameterAsBoolean(FrequencyDiscretization.PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS) == false) {
			numberOfDigits = getParameterAsInt(FrequencyDiscretization.PARAMETER_NUMBER_OF_DIGITS);
		}

		model.setRanges(rangesMap, "range", getParameterAsInt(FrequencyDiscretization.PARAMETER_RANGE_NAME_TYPE),
				numberOfDigits);
		return model;
	}

	private Double getMinEntropySplitpoint(LinkedList<double[]> truncatedExamples, Attribute label) {
		HashSet<Double> candidateSplitpoints = new HashSet<Double>();
		Iterator<double[]> it = truncatedExamples.iterator();
		int[] totalLabelDistribution = new int[label.getMapping().size()]; // Label distribution for
																			// all examples.
		while (it.hasNext()) { // Get splitpoint candidates and total label distribution.
			double[] attributeLabelPair = it.next();
			candidateSplitpoints.add(attributeLabelPair[0]);
			int labelIndex = (int) attributeLabelPair[1];
			totalLabelDistribution[labelIndex]++;
		}
		double[] totalFrequencies = new double[label.getMapping().size()];
		for (int i = 0; i < label.getMapping().size(); i++) {
			totalFrequencies[i] = (double) totalLabelDistribution[i] / (double) truncatedExamples.size();
		}
		double totalEntropy = 0.0d;
		for (int i = 0; i < label.getMapping().size(); i++) {
			totalEntropy -= totalFrequencies[i] * MathFunctions.ld(totalFrequencies[i]);
		}

		double minClassInformationEntropy = totalEntropy;
		double bestSplitpoint = Double.NaN;
		double bestSplitpointEntropy1 = Double.POSITIVE_INFINITY;
		double bestSplitpointEntropy2 = Double.POSITIVE_INFINITY;
		int k1 = 0; // Number of different class labels in class 1.
		int k2 = 0; // Number of different class labels in class 2.

		for (double currentSplitpoint : candidateSplitpoints) {
			// Test every value as splitpoint
			// Initialize.
			int s1 = 0; // Instances in partition 1.
			int s2 = 0; // Instances in partition 2.
			k1 = 0;
			k2 = 0;
			int[] labelDistribution1 = new int[label.getMapping().size()]; // Label distribution in
																			// class 1.
			int[] labelDistribution2 = new int[label.getMapping().size()]; // Label distribution in
																			// class 2.

			// Determine the class of each instance and the corresponding label distribution.
			for (double[] attributeLabelPair : truncatedExamples) {
				double valueToCompare = attributeLabelPair[0];
				int labelIndex = (int) attributeLabelPair[1];
				if (valueToCompare <= currentSplitpoint) {
					// Partition 1 gets all instances with values less or equal to the current
					// splitpoint.
					s1++;
					labelDistribution1[labelIndex]++;
				} else { // Partition 2 gets all instances with values
					// greater than the current split point.
					s2++;
					labelDistribution2[labelIndex]++;
				}
			}

			// Calculate frequencies and number of different labels for this
			// splitpoint each class.
			double[] frequencies1 = new double[label.getMapping().size()];
			double[] frequencies2 = new double[label.getMapping().size()];
			for (int i = 0; i < label.getMapping().size(); i++) {
				frequencies1[i] = (double) labelDistribution1[i] / (double) s1;
				frequencies2[i] = (double) labelDistribution2[i] / (double) s2;
				if (labelDistribution1[i] > 0) { // Label value i exists in
					// class 1.
					k1++;
				}
				if (labelDistribution2[i] > 0) { // Label value i exists in
					// class 2.
					k2++;
				}
			}

			/*
			 * Calculate entropies.
			 * 
			 * In the case of p(x_i) = 0 for some i, the value of the corresponding summand 0 *
			 * ld(0) is taken to be 0, which is consistent with the well-known limit:
			 * 
			 * lim_(p -> 0+) p*log(p) = 0
			 */
			double entropy1 = 0.0d;
			for (int i = 0; i < label.getMapping().size(); i++) {
				double frequency = frequencies1[i];
				// if frequency is zero, skip label
				if (frequency != 0.0d) {
					entropy1 -= frequency * MathFunctions.ld(frequency);
				}
			}
			double entropy2 = 0.0d;
			for (int i = 0; i < label.getMapping().size(); i++) {
				double frequency = frequencies2[i];
				// if frequency is zero, skip label
				if (frequency != 0.0d) {
					entropy2 -= frequency * MathFunctions.ld(frequency);
				}
			}

			double classInformationEntropy = ((double) s1 / (double) truncatedExamples.size()) * entropy1
					+ ((double) s2 / (double) truncatedExamples.size()) * entropy2;
			if (classInformationEntropy < minClassInformationEntropy) {
				minClassInformationEntropy = classInformationEntropy;
				bestSplitpoint = currentSplitpoint;
				bestSplitpointEntropy1 = entropy1;
				bestSplitpointEntropy2 = entropy2;
			}
		}

		// Calculate the termination criterion. Return null if termination
		// criterion is met.
		double gain = totalEntropy - minClassInformationEntropy;
		double delta = MathFunctions.ld(Math.pow(3.0, label.getMapping().size()) - 2)
				- (label.getMapping().size() * totalEntropy - k1 * bestSplitpointEntropy1 - k2 * bestSplitpointEntropy2);
		if (gain >= MathFunctions.ld(truncatedExamples.size() - 1) / truncatedExamples.size() + delta
				/ truncatedExamples.size()) {
			return Double.valueOf(bestSplitpoint);
		} else {
			return null;
		}
	}

	/*
	 * LinkedList partition consist of double arrays of size 2. array[0]=value of the current
	 * attribute, array[1]=corresponding label value.
	 */
	private ArrayList<Double> getSplitpoints(LinkedList<double[]> startPartition, Attribute label) {
		LinkedList<LinkedList<double[]>> border = new LinkedList<LinkedList<double[]>>();
		ArrayList<Double> result = new ArrayList<Double>();
		border.addLast(startPartition);
		while (!border.isEmpty()) {
			LinkedList<double[]> currentPartition = border.removeFirst();
			Double splitpoint = this.getMinEntropySplitpoint(currentPartition, label);
			if (splitpoint != null) {
				result.add(splitpoint);
				double splitValue = splitpoint.doubleValue();
				LinkedList<double[]> newPartition1 = new LinkedList<double[]>();
				LinkedList<double[]> newPartition2 = new LinkedList<double[]>();
				Iterator<double[]> it = currentPartition.iterator();
				while (it.hasNext()) { // Create new partitions.
					double[] attributeLabelPair = it.next();
					if (attributeLabelPair[0] <= splitValue) {
						newPartition1.addLast(attributeLabelPair);
					} else {
						newPartition2.addLast(attributeLabelPair);
					}
				}
				border.addLast(newPartition1);
				border.addLast(newPartition2);
			}
		}
		return result; // Empty ArrayList if no Splitpoint could be found.

	}

	/**
	 * Delivers the maximum range thresholds for all attributes, i.e. the value getRanges()[a][b] is
	 * the b-th threshold for the a-th attribute.
	 *
	 * @throws UserError
	 *             is label is missing
	 */
	private double[][] getRanges(ExampleSet exampleSet) throws UserError {
		Tools.isLabelled(exampleSet);
		Attributes attributes = exampleSet.getAttributes();
		double[][] ranges = new double[attributes.size()][];
		Attribute label = attributes.getLabel();
		if (!label.isNominal()) {
			OperatorDescription[] descriptions = OperatorService.getOperatorDescriptions(getClass());
			String name = getClass().getSimpleName();
			if (descriptions != null && descriptions.length > 0 && descriptions[0] != null) {
				name = descriptions[0].getName();
			}
			throw new UserError(this, 101, name, label.getName());
		}

		int a = 0;
		for (Attribute attribute : attributes) {
			if (attribute.isNumerical()) { // skip nominal and date attributes
				Iterator<Example> reader = exampleSet.iterator();
				LinkedList<double[]> startPartition = new LinkedList<double[]>();
				while (reader.hasNext()) { // Create start partition.
					Example example = reader.next();
					double[] attributeLabelPair = new double[2];
					attributeLabelPair[0] = example.getValue(attribute);
					attributeLabelPair[1] = example.getValue(label);
					startPartition.addLast(attributeLabelPair);
				}
				ArrayList<Double> splitpointsOfAttribute = getSplitpoints(startPartition, label);
				Iterator<Double> it = splitpointsOfAttribute.iterator();
				ranges[a] = new double[splitpointsOfAttribute.size() + 1];
				for (int i = 0; it.hasNext(); i++) {
					ranges[a][i] = it.next();
				}
				ranges[a][ranges[a].length - 1] = exampleSet.getStatistics(attribute, Statistics.MAXIMUM);
				Arrays.sort(ranges[a]);
			}
			a++;
		}
		return ranges;
	}

	@Override
	public boolean isSupportingAttributeRoles() {
		return true;
	}

	@Override
	public Class<? extends PreprocessingModel> getPreprocessingModelClass() {
		return DiscretizationModel.class;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_REMOVE_USELESS,
				"Indicates if useless attributes, i.e. those containing only one single range, should be removed.", true));
		types.add(new ParameterTypeCategory(FrequencyDiscretization.PARAMETER_RANGE_NAME_TYPE,
				"Indicates if long range names including the limits should be used.", DiscretizationModel.RANGE_NAME_TYPES,
				DiscretizationModel.RANGE_NAME_LONG));

		ParameterType type = new ParameterTypeBoolean(FrequencyDiscretization.PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS,
				"Indicates if the number of digits should be automatically determined for the range names.", true);
		type.registerDependencyCondition(new EqualTypeCondition(this, FrequencyDiscretization.PARAMETER_RANGE_NAME_TYPE,
				DiscretizationModel.RANGE_NAME_TYPES, false, DiscretizationModel.RANGE_NAME_INTERVAL));
		types.add(type);

		type = new ParameterTypeInt(FrequencyDiscretization.PARAMETER_NUMBER_OF_DIGITS,
				"The minimum number of digits used for the interval names (-1: determine minimal number automatically).",
				-1, Integer.MAX_VALUE, -1);
		type.registerDependencyCondition(new BooleanParameterCondition(this,
				FrequencyDiscretization.PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS, false, false));
		types.add(type);

		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		if (getCompatibilityLevel().isAbove(VERSION_MAY_WRITE_INTO_DATA)) {
			return false;
		} else {
			// old version: true only if original output port is connected
			return isOriginalOutputConnected() && super.writesIntoExistingData();
		}
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getExampleSetInputPort(),
				MinimalEntropyDiscretization.class, attributeSelector);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[] { VERSION_MAY_WRITE_INTO_DATA });
	}
}
