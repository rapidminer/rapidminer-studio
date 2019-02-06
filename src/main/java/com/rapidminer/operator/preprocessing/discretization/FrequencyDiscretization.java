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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.set.SortedExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * This operator discretizes all numeric attributes in the dataset into nominal attributes. This
 * discretization is performed by equal frequency binning, i.e. the thresholds of all bins is
 * selected in a way that all bins contain the same number of numerical values. The number of bins
 * is specified by a parameter, or, alternatively, is calculated as the square root of the number of
 * examples with non-missing values (calculated for every single attribute). Skips all special
 * attributes including the label. Note that it is possible to get bins with different numbers of
 * examples. This might occur, if the attributes's values are not unique, since the algorithm can
 * not split between examples with same value.
 *
 * @author Sebastian Land, Ingo Mierswa
 */
public class FrequencyDiscretization extends AbstractDiscretizationOperator {

	static {
		registerDiscretizationOperator(FrequencyDiscretization.class);
	}

	/**
	 * The parameter name for &quot;If true, the number of bins is instead determined by the square
	 * root of the number of non-missing values.&quot;
	 */
	public static final String PARAMETER_USE_SQRT_OF_EXAMPLES = "use_sqrt_of_examples";

	/** The parameter for the number of bins. */
	public static final String PARAMETER_NUMBER_OF_BINS = "number_of_bins";

	/** Indicates if long range names should be used. */
	public static final String PARAMETER_RANGE_NAME_TYPE = "range_name_type";

	public static final String PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS = "automatic_number_of_digits";

	public static final String PARAMETER_NUMBER_OF_DIGITS = "number_of_digits";

	/**
	 * Incompatible version, old version writes into the exampleset, if original output port is not
	 * connected.
	 */
	private static final OperatorVersion VERSION_MAY_WRITE_INTO_DATA = new OperatorVersion(7, 1, 1);

	public FrequencyDiscretization(OperatorDescription description) {
		super(description);
	}

	@Override
	protected Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd)
			throws UndefinedParameterError {
		AttributeMetaData newAMD = new AttributeMetaData(amd.getName(), Ontology.NOMINAL, amd.getRole());
		Set<String> valueSet = new TreeSet<String>();
		if (getParameterAsInt(PARAMETER_RANGE_NAME_TYPE) == DiscretizationModel.RANGE_NAME_SHORT) {
			for (int i = 0; i < getParameterAsInt(PARAMETER_NUMBER_OF_BINS); i++) {
				valueSet.add("range" + (i + 1));
			}
			newAMD.setValueSet(valueSet, SetRelation.EQUAL);
		} else {
			newAMD.setValueSet(valueSet, SetRelation.SUPERSET);
		}
		return Collections.singletonList(newAMD);
	}

	@Override
	public PreprocessingModel createPreprocessingModel(ExampleSet exampleSet) throws OperatorException {
		HashMap<Attribute, double[]> ranges = new HashMap<Attribute, double[]>();
		// Get and check parametervalues
		boolean useSqrt = getParameterAsBoolean(PARAMETER_USE_SQRT_OF_EXAMPLES);
		int numberOfBins = 0;
		if (!useSqrt) {
			// if not automatic sizing of bins, use parametervalue
			numberOfBins = getParameterAsInt(PARAMETER_NUMBER_OF_BINS);
			if (numberOfBins >= (exampleSet.size() - 1)) {
				throw new UserError(this, 116, PARAMETER_NUMBER_OF_BINS,
						"number of bins must be smaller than number of examples (here: " + exampleSet.size() + ")");
			}
		} else {
			exampleSet.recalculateAllAttributeStatistics();
		}

		for (Attribute currentAttribute : exampleSet.getAttributes()) {
			if (useSqrt) {
				numberOfBins = (int) Math.round(Math.sqrt(exampleSet.size()
						- (int) exampleSet.getStatistics(currentAttribute, Statistics.UNKNOWN)));
			}
			double[] attributeRanges = new double[numberOfBins];
			ExampleSet sortedSet = new SortedExampleSet(exampleSet, currentAttribute, SortedExampleSet.INCREASING);

			// finding ranges
			double examplesPerBin = exampleSet.size() / (double) numberOfBins;
			double currentBinSpace = examplesPerBin;
			double lastValue = Double.NaN;
			int currentBin = 0;

			for (Example example : sortedSet) {
				double value = example.getValue(currentAttribute);
				if (!Double.isNaN(value)) {
					// change bin if full and not last
					if (currentBinSpace < 1 && currentBin < numberOfBins && value != lastValue) {
						if (!Double.isNaN(lastValue)) {
							attributeRanges[currentBin] = (lastValue + value) / 2;
							currentBin++;
							currentBinSpace += examplesPerBin; // adding because same values might
																// cause binspace to be negative
							if (currentBinSpace < 1) {
								throw new UserError(this, 944, currentAttribute.getName());
							}
						}
					}
					currentBinSpace--;
					lastValue = value;
				}
			}
			attributeRanges[numberOfBins - 1] = Double.POSITIVE_INFINITY;
			ranges.put(currentAttribute, attributeRanges);
		}
		DiscretizationModel model = new DiscretizationModel(exampleSet);

		// determine number of digits
		int numberOfDigits = -1;
		if (getParameterAsBoolean(PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS) == false) {
			numberOfDigits = getParameterAsInt(PARAMETER_NUMBER_OF_DIGITS);
		}

		model.setRanges(ranges, "range", getParameterAsInt(PARAMETER_RANGE_NAME_TYPE), numberOfDigits);
		return model;
	}

	@Override
	public Class<? extends PreprocessingModel> getPreprocessingModelClass() {
		return DiscretizationModel.class;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeBoolean(PARAMETER_USE_SQRT_OF_EXAMPLES,
				"If true, the number of bins is instead determined by the square root of the number of non-missing values.",
				false));

		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_OF_BINS,
				"Defines the number of bins which should be used for each attribute.", 2, Integer.MAX_VALUE, 2);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_SQRT_OF_EXAMPLES, false, false));
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeCategory(PARAMETER_RANGE_NAME_TYPE,
				"Indicates if long range names including the limits should be used.", DiscretizationModel.RANGE_NAME_TYPES,
				DiscretizationModel.RANGE_NAME_LONG));

		type = new ParameterTypeBoolean(PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS,
				"Indicates if the number of digits should be automatically determined for the range names.", true);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_RANGE_NAME_TYPE,
				DiscretizationModel.RANGE_NAME_TYPES, false, DiscretizationModel.RANGE_NAME_INTERVAL));
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_NUMBER_OF_DIGITS,
				"The minimum number of digits used for the interval names (-1: determine minimal number automatically).",
				-1, Integer.MAX_VALUE, -1);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS, false,
				false));
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
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				FrequencyDiscretization.class, attributeSelector);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[] { VERSION_MAY_WRITE_INTO_DATA });
	}
}
