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

import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * This operator discretizes all numeric attributes in the dataset into nominal attributes. This
 * discretization is performed by simple binning, i.e. the specified number of equally sized bins is
 * created and the numerical values are simply sorted into those bins. Skips all special attributes
 * including the label. In contrast to the usual simple binning performed by the
 * {@link BinDiscretization}, this operator bins the values into a predefined range (and not into
 * the range defined by the minimum and maximum values taken from the data).
 *
 * @author Ingo Mierswa
 */
public class MinMaxBinDiscretization extends AbstractDiscretizationOperator {

	static {
		registerDiscretizationOperator(MinMaxBinDiscretization.class);
	}

	/** Indicates the number of used bins. */
	public static final String PARAMETER_NUMBER_OF_BINS = "number_of_bins";

	public static final String PARAMETER_MIN_VALUE = "min_value";

	public static final String PARAMETER_MAX_VALUE = "max_value";

	/** Indicates if long range names should be used. */
	public static final String PARAMETER_RANGE_NAME_TYPE = "range_name_type";

	public static final String PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS = "automatic_number_of_digits";

	public static final String PARAMETER_NUMBER_OF_DIGITS = "number_of_digits";

	/**
	 * Incompatible version, old version writes into the exampleset, if original output port is not
	 * connected.
	 */
	private static final OperatorVersion VERSION_MAY_WRITE_INTO_DATA = new OperatorVersion(7, 1, 1);

	public MinMaxBinDiscretization(OperatorDescription description) {
		super(description);
	}

	@Override
	public PreprocessingModel createPreprocessingModel(ExampleSet exampleSet) throws OperatorException {
		DiscretizationModel model = new DiscretizationModel(exampleSet);

		exampleSet.recalculateAllAttributeStatistics();
		int numberOfBins = getParameterAsInt(PARAMETER_NUMBER_OF_BINS);
		HashMap<Attribute, double[]> ranges = new HashMap<Attribute, double[]>();

		double min = getParameterAsDouble(PARAMETER_MIN_VALUE);
		double max = getParameterAsDouble(PARAMETER_MAX_VALUE);
		if (min > max) {
			throw new UserError(this, 116, PARAMETER_MIN_VALUE + " and " + PARAMETER_MAX_VALUE,
					"minimum must be less than maximum");
		}
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNumerical()) { // skip nominal and date attributes
				double[] binRange = new double[numberOfBins + 2];
				binRange[0] = min;
				for (int b = 1; b < numberOfBins; b++) {
					binRange[b] = min + (((double) b / (double) numberOfBins) * (max - min));
				}
				binRange[numberOfBins] = max;
				binRange[numberOfBins + 1] = Double.POSITIVE_INFINITY;
				ranges.put(attribute, binRange);
			}
		}

		// determine number of digits
		int numberOfDigits = -1;
		if (getParameterAsBoolean(PARAMETER_AUTOMATIC_NUMBER_OF_DIGITS) == false) {
			numberOfDigits = getParameterAsInt(PARAMETER_NUMBER_OF_DIGITS);
		}

		model.setRanges(ranges, "range", getParameterAsInt(PARAMETER_RANGE_NAME_TYPE), numberOfDigits);
		return (model);
	}

	@Override
	public Class<? extends PreprocessingModel> getPreprocessingModelClass() {
		return DiscretizationModel.class;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_OF_BINS,
				"Defines the number of bins which should be used for each attribute.", 2, Integer.MAX_VALUE, 2);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeDouble(PARAMETER_MIN_VALUE, "The minimum value for the binning range.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false));
		types.add(new ParameterTypeDouble(PARAMETER_MAX_VALUE, "The maximum value for the binning range.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false));

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
			return super.writesIntoExistingData();
		} else {
			// old version: true only if original output port is connected
			return isOriginalOutputConnected() && super.writesIntoExistingData();
		}
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				MinMaxBinDiscretization.class, attributeSelector);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[] { VERSION_MAY_WRITE_INTO_DATA });
	}
}
