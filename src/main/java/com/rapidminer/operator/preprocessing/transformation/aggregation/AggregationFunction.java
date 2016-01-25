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
package com.rapidminer.operator.preprocessing.transformation.aggregation;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.tools.Ontology;


/**
 * This is an abstract class for all {@link AggregationFunction}s, that can be selected to aggregate
 * values of a certain group. Each {@link AggregationFunction} must be able to provide a certain
 * {@link Aggregator}, that will count the examples of one single group and compute the aggregated
 * value. So for example the {@link MeanAggregationFunction} provides an {@link MeanAggregator},
 * that will calculate the mean on all examples delivered to him.
 * 
 * The list of the names of all available functions can be queried from the static method
 * {@link #getAvailableAggregationFunctionNames()}. With a name one can call the static method
 * {@link #createAggregationFunction(String, Attribute)} to create a certain aggregator for the
 * actual counting.
 * 
 * Additional functions can be registered by calling
 * {@link #registerNewAggregationFunction(String, Class)} from extensions, preferable during their
 * initialization. Please notice that there will be no warning prior process execution if the
 * extension is missing but the usage of it's function is still configured.
 * 
 * @author Sebastian Land, Marius Helf
 */
public abstract class AggregationFunction {

	public static final String FUNCTION_SEPARATOR_OPEN = "(";
	public static final String FUNCTION_SEPARATOR_CLOSE = ")";

	// available functions
	public static final String FUNCTION_NAME_SUM = "sum";
	public static final String FUNCTION_NAME_SUM_FRACTIONAL = "sum (fractional)";
	public static final String FUNCTION_NAME_MEDIAN = "median";
	public static final String FUNCTION_NAME_AVERAGE = "average";
	public static final String FUNCTION_NAME_VARIANCE = "variance";
	public static final String FUNCTION_NAME_STANDARD_DEVIATION = "standard_deviation";
	public static final String FUNCTION_NAME_COUNT_IGNORE_MISSINGS = "count (ignoring missings)";
	public static final String FUNCTION_NAME_COUNT_INCLUDE_MISSINGS = "count (including missings)";
	public static final String FUNCTION_NAME_COUNT = "count";
	public static final String FUNCTION_NAME_COUNT_FRACTIONAL = "count (fractional)";
	public static final String FUNCTION_NAME_COUNT_PERCENTAGE = "count (percentage)";
	public static final String FUNCTION_NAME_MINIMUM = "minimum";
	public static final String FUNCTION_NAME_MAXIMUM = "maximum";
	public static final String FUNCTION_NAME_LOG_PRODUCT = "log product";
	public static final String FUNCTION_NAME_PRODOCT = "product";
	public static final String FUNCTION_NAME_MODE = "mode";
	public static final String FUNCTION_NAME_LEAST = "least";
	public static final String FUNCTION_NAME_LEAST_ONLY_OCCURRING = "least (only occurring)";
	public static final String FUNCTION_NAME_CONCATENATION = "concatenation";

	public static final Map<String, Class<? extends AggregationFunction>> AGGREATION_FUNCTIONS = new TreeMap<>();
	static {
		// numerical/date
		AGGREATION_FUNCTIONS.put(FUNCTION_NAME_SUM, SumAggregationFunction.class);
		AGGREATION_FUNCTIONS.put(FUNCTION_NAME_SUM_FRACTIONAL, SumFractionalAggregationFunction.class);
		AGGREATION_FUNCTIONS.put(FUNCTION_NAME_MEDIAN, MedianAggregationFunction.class);
		AGGREATION_FUNCTIONS.put(FUNCTION_NAME_AVERAGE, MeanAggregationFunction.class);
		AGGREATION_FUNCTIONS.put(FUNCTION_NAME_VARIANCE, VarianceAggregationFunction.class);
		AGGREATION_FUNCTIONS.put(FUNCTION_NAME_STANDARD_DEVIATION, StandardDeviationAggregationFunction.class);
		AGGREATION_FUNCTIONS.put(FUNCTION_NAME_MINIMUM, MinAggregationFunction.class);
		AGGREATION_FUNCTIONS.put(FUNCTION_NAME_MAXIMUM, MaxAggregationFunction.class);

		AGGREATION_FUNCTIONS.put(FUNCTION_NAME_LOG_PRODUCT, LogProductAggregationFunction.class);
		AGGREATION_FUNCTIONS.put(FUNCTION_NAME_PRODOCT, ProductAggregationFunction.class);

		// numerical/date/nominal
		AGGREATION_FUNCTIONS.put(FUNCTION_NAME_COUNT_IGNORE_MISSINGS, CountIgnoringMissingsAggregationFunction.class);
		AGGREATION_FUNCTIONS.put(FUNCTION_NAME_COUNT_INCLUDE_MISSINGS, CountIncludingMissingsAggregationFunction.class);
		AGGREATION_FUNCTIONS.put(FUNCTION_NAME_COUNT, CountAggregationFunction.class);
		AGGREATION_FUNCTIONS.put(FUNCTION_NAME_COUNT_FRACTIONAL, CountFractionalAggregationFunction.class);
		AGGREATION_FUNCTIONS.put(FUNCTION_NAME_COUNT_PERCENTAGE, CountPercentageAggregationFunction.class);

		// Nominal Aggregations
		AGGREATION_FUNCTIONS.put(FUNCTION_NAME_MODE, ModeAggregationFunction.class);
		AGGREATION_FUNCTIONS.put(FUNCTION_NAME_LEAST, LeastAggregationFunction.class);
		AGGREATION_FUNCTIONS.put(FUNCTION_NAME_LEAST_ONLY_OCCURRING, LeastOccurringAggregationFunction.class);
		AGGREATION_FUNCTIONS.put(FUNCTION_NAME_CONCATENATION, ConcatAggregationFunction.class);
	}

	public static final Map<String, AggregationFunctionMetaDataProvider> AGGREGATION_FUNCTIONS_META_DATA_PROVIDER = new HashMap<>();
	static {
		HashMap<Integer, Integer> transformationRules = new HashMap<Integer, Integer>() {

			private static final long serialVersionUID = 8941596913239332241L;

			{
				put(Ontology.DATE_TIME, Ontology.DATE_TIME);
				put(Ontology.NUMERICAL, Ontology.REAL);
			}
		};
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(FUNCTION_NAME_SUM, new DefaultAggregationFunctionMetaDataProvider(
				FUNCTION_NAME_SUM, SumAggregationFunction.FUNCTION_SUM, FUNCTION_SEPARATOR_OPEN, FUNCTION_SEPARATOR_CLOSE,
				new int[] { Ontology.NUMERICAL }));
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(FUNCTION_NAME_SUM_FRACTIONAL,
				new DefaultAggregationFunctionMetaDataProvider("fractionalSum",
						SumFractionalAggregationFunction.FUNCTION_SUM_FRACTIONAL, FUNCTION_SEPARATOR_OPEN,
						FUNCTION_SEPARATOR_CLOSE, new int[] { Ontology.NUMERICAL }));
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(FUNCTION_NAME_MEDIAN, new MappingAggregationFunctionMetaDataProvider(
				FUNCTION_NAME_MEDIAN, MedianAggregationFunction.FUNCTION_MEDIAN, FUNCTION_SEPARATOR_OPEN,
				FUNCTION_SEPARATOR_CLOSE, transformationRules));
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(FUNCTION_NAME_AVERAGE, new MappingAggregationFunctionMetaDataProvider(
				FUNCTION_NAME_AVERAGE, MeanAggregationFunction.FUNCTION_AVERAGE, FUNCTION_SEPARATOR_OPEN,
				FUNCTION_SEPARATOR_CLOSE, transformationRules));
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(FUNCTION_NAME_VARIANCE, new DefaultAggregationFunctionMetaDataProvider(
				FUNCTION_NAME_VARIANCE, VarianceAggregationFunction.FUNCTION_VARIANCE, FUNCTION_SEPARATOR_OPEN,
				FUNCTION_SEPARATOR_CLOSE, new int[] { Ontology.NUMERICAL }, Ontology.REAL));
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(FUNCTION_NAME_STANDARD_DEVIATION,
				new DefaultAggregationFunctionMetaDataProvider(FUNCTION_NAME_STANDARD_DEVIATION,
						StandardDeviationAggregationFunction.FUNCTION_STANDARD_DEVIATION, FUNCTION_SEPARATOR_OPEN,
						FUNCTION_SEPARATOR_CLOSE, new int[] { Ontology.NUMERICAL }, Ontology.REAL));
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(FUNCTION_NAME_COUNT_IGNORE_MISSINGS,
				new DefaultAggregationFunctionMetaDataProvider(FUNCTION_NAME_COUNT_IGNORE_MISSINGS,
						CountIgnoringMissingsAggregationFunction.FUNCTION_COUNT, FUNCTION_SEPARATOR_OPEN,
						FUNCTION_SEPARATOR_CLOSE, new int[] { Ontology.ATTRIBUTE_VALUE }, Ontology.INTEGER));
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(FUNCTION_NAME_COUNT_INCLUDE_MISSINGS,
				new DefaultAggregationFunctionMetaDataProvider(FUNCTION_NAME_COUNT_INCLUDE_MISSINGS,
						CountIncludingMissingsAggregationFunction.FUNCTION_COUNT, FUNCTION_SEPARATOR_OPEN,
						FUNCTION_SEPARATOR_CLOSE, new int[] { Ontology.ATTRIBUTE_VALUE }, Ontology.INTEGER));
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(FUNCTION_NAME_COUNT, new DefaultAggregationFunctionMetaDataProvider(
				FUNCTION_NAME_COUNT, CountAggregationFunction.FUNCTION_COUNT, FUNCTION_SEPARATOR_OPEN,
				FUNCTION_SEPARATOR_CLOSE, new int[] { Ontology.ATTRIBUTE_VALUE }, Ontology.INTEGER));
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(FUNCTION_NAME_COUNT_FRACTIONAL,
				new DefaultAggregationFunctionMetaDataProvider("fractionalCount",
						CountFractionalAggregationFunction.FUNCTION_COUNT_FRACTIONAL, FUNCTION_SEPARATOR_OPEN,
						FUNCTION_SEPARATOR_CLOSE, new int[] { Ontology.ATTRIBUTE_VALUE }, Ontology.REAL));
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(FUNCTION_NAME_COUNT_PERCENTAGE,
				new DefaultAggregationFunctionMetaDataProvider("percentageCount",
						CountPercentageAggregationFunction.FUNCTION_COUNT_PERCENTAGE, FUNCTION_SEPARATOR_OPEN,
						FUNCTION_SEPARATOR_CLOSE, new int[] { Ontology.ATTRIBUTE_VALUE }, Ontology.REAL));
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(FUNCTION_NAME_MINIMUM, new MappingAggregationFunctionMetaDataProvider(
				FUNCTION_NAME_MINIMUM, MinAggregationFunction.FUNCTION_MIN, FUNCTION_SEPARATOR_OPEN,
				FUNCTION_SEPARATOR_CLOSE, transformationRules));
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(FUNCTION_NAME_MAXIMUM, new MappingAggregationFunctionMetaDataProvider(
				FUNCTION_NAME_MAXIMUM, MaxAggregationFunction.FUNCTION_MAX, FUNCTION_SEPARATOR_OPEN,
				FUNCTION_SEPARATOR_CLOSE, transformationRules));
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(FUNCTION_NAME_LOG_PRODUCT,
				new DefaultAggregationFunctionMetaDataProvider("log product",
						LogProductAggregationFunction.FUNCTION_LOG_PRODUCT, FUNCTION_SEPARATOR_OPEN,
						FUNCTION_SEPARATOR_CLOSE, new int[] { Ontology.NUMERICAL }, Ontology.REAL));
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(FUNCTION_NAME_PRODOCT, new DefaultAggregationFunctionMetaDataProvider(
				FUNCTION_NAME_PRODOCT, ProductAggregationFunction.FUNCTION_PRODUCT, FUNCTION_SEPARATOR_OPEN,
				FUNCTION_SEPARATOR_CLOSE, new int[] { Ontology.NUMERICAL }, Ontology.REAL));

		// Nominal Aggregations
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(FUNCTION_NAME_MODE, new DefaultAggregationFunctionMetaDataProvider(
				FUNCTION_NAME_MODE, ModeAggregationFunction.FUNCTION_MODE, FUNCTION_SEPARATOR_OPEN,
				FUNCTION_SEPARATOR_CLOSE, new int[] { Ontology.ATTRIBUTE_VALUE }));
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(FUNCTION_NAME_LEAST, new DefaultAggregationFunctionMetaDataProvider(
				FUNCTION_NAME_LEAST, LeastAggregationFunction.FUNCTION_LEAST, FUNCTION_SEPARATOR_OPEN,
				FUNCTION_SEPARATOR_CLOSE, new int[] { Ontology.NOMINAL }, Ontology.POLYNOMINAL));
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(FUNCTION_NAME_LEAST_ONLY_OCCURRING,
				new DefaultAggregationFunctionMetaDataProvider(FUNCTION_NAME_LEAST_ONLY_OCCURRING,
						LeastOccurringAggregationFunction.FUNCTION_LEAST_OCCURRING, FUNCTION_SEPARATOR_OPEN,
						FUNCTION_SEPARATOR_CLOSE, new int[] { Ontology.NOMINAL }, Ontology.POLYNOMINAL));
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(FUNCTION_NAME_CONCATENATION,
				new DefaultAggregationFunctionMetaDataProvider(FUNCTION_NAME_CONCATENATION,
						ConcatAggregationFunction.FUNCTION_CONCAT, FUNCTION_SEPARATOR_OPEN, FUNCTION_SEPARATOR_CLOSE,
						new int[] { Ontology.NOMINAL }, Ontology.POLYNOMINAL));
	}

	private Attribute sourceAttribute;
	private boolean isIgnoringMissings;
	private boolean isCountingOnlyDistinct;

	public AggregationFunction(Attribute sourceAttribute, boolean ignoreMissings, boolean countOnlyDistinct) {
		this.sourceAttribute = sourceAttribute;
		this.isIgnoringMissings = ignoreMissings;
		this.isCountingOnlyDistinct = countOnlyDistinct;
	}

	/**
	 * This returns the attribute this aggregation function will derive the data from.
	 */
	public Attribute getSourceAttribute() {
		return sourceAttribute;
	}

	/**
	 * This returns the attribute that will be created in the resulting {@link ExampleSet} to get
	 * the aggregated values for each group.
	 */
	public abstract Attribute getTargetAttribute();

	/**
	 * This will return the {@link Aggregator} object that computes the value of this particular
	 * {@link AggregationFunction} for a specific group.
	 */
	public abstract Aggregator createAggregator();

	/**
	 * This determines, if any missing values will be just ignored or counted with the respective
	 * aggregation function. Some functions might cope with that, others will just turn to be NaN.
	 */
	public boolean isIgnoringMissings() {
		return isIgnoringMissings;
	}

	/**
	 * This determines, if values are counted only once, if occurring more than once. Please note
	 * that will increase the memory load drastically on numerical attributes.
	 */
	public boolean isCountingOnlyDistinct() {
		return isCountingOnlyDistinct;
	}

	/**
	 * This will return whether this {@link AggregationFunction} is compatible with the given
	 * sourceAttribute.
	 */
	public abstract boolean isCompatible();

	/**
	 * This method will fill in the default value of this aggregation function. It has to maintain
	 * the mapping, if the function is nominal. The default value will be a NaN. Every subclass that
	 * wants to change this, has to override this method.
	 */
	public void setDefault(Attribute attribute, DoubleArrayDataRow row) {
		row.set(attribute, Double.NaN);
	}

	/**
	 * This will create the {@link AggregationFunction} with the given name for the given source
	 * Attribute. This method might return
	 * 
	 * @param name
	 *            please use one of the FUNCTION_NAME_* constants to prevent unnecessary errors
	 */
	public static final AggregationFunction createAggregationFunction(String name, Attribute sourceAttribute,
			boolean ignoreMissings, boolean countOnlyDistinct) throws OperatorException {
		Class<? extends AggregationFunction> aggregationFunctionClass = AGGREATION_FUNCTIONS.get(name);
		if (aggregationFunctionClass == null) {
			throw new UserError(null, "aggregation.illegal_function_name", name);
		}
		try {
			Constructor<? extends AggregationFunction> constructor = aggregationFunctionClass.getConstructor(
					Attribute.class, boolean.class, boolean.class);
			return constructor.newInstance(sourceAttribute, ignoreMissings, countOnlyDistinct);
		} catch (Exception e) {
			throw new RuntimeException(
					"All implementations of AggregationFunction need to have a constructor accepting an Attribute and boolean. Other reasons for this error may be class loader problems.",
					e);
		}
	}

	/**
	 * This method can be called in order to get the target attribute meta data after the
	 * aggregation functions have been applied. This method can register errors on the given
	 * InputPort (if not null), if there's an illegal state. If the state makes applying an
	 * {@link AggregationFunction} impossible, this method will return null!
	 * 
	 * @param aggregationFunctionName
	 *            please use one of the FUNCTION_NAME_* constants to prevent unnecessary errors
	 */
	public static final AttributeMetaData getAttributeMetaData(String aggregationFunctionName,
			AttributeMetaData sourceAttributeMetaData, InputPort inputPort) {
		AggregationFunctionMetaDataProvider metaDataProvider = AGGREGATION_FUNCTIONS_META_DATA_PROVIDER
				.get(aggregationFunctionName);
		if (metaDataProvider != null) {
			return metaDataProvider.getTargetAttributeMetaData(sourceAttributeMetaData, inputPort);
		} else {
			// register error about unknown aggregation function
			if (inputPort != null) {
				inputPort.addError(new SimpleMetaDataError(Severity.ERROR, inputPort,
						"aggregation.unknown_aggregation_function", aggregationFunctionName));
			}
			return null;
		}
	}

	/**
	 * This method will return the array containing the names of all available aggregation
	 * functions. The names are sorted according to natural ordering.
	 */
	public static String[] getAvailableAggregationFunctionNames() {
		String[] names = new String[AGGREATION_FUNCTIONS.size()];
		int i = 0;
		for (String name : AGGREATION_FUNCTIONS.keySet()) {
			names[i] = name;
			i++;
		}

		return names;
	}

	/**
	 * This method will return a list of aggregate functions that are compatible with the provided
	 * valueType.
	 * 
	 * @param valueType
	 *            a valueType found in {@link Ontology}.
	 */
	public static List<String> getCompatibleAggregationFunctionNames(int valueType) {
		List<String> compatibleAggregationFunctions = new LinkedList<>();

		Attribute sampleAttribute = AttributeFactory.createAttribute(valueType);

		for (String name : getAvailableAggregationFunctionNames()) {
			try {
				if (createAggregationFunction(name, sampleAttribute, true, true).isCompatible()) {
					compatibleAggregationFunctions.add(name);
				}
			} catch (OperatorException e) {
				// do nothing
			}
		}

		return compatibleAggregationFunctions;
	}

	/**
	 * With this method extensions might register additional aggregation functions if needed.
	 */
	public static void registerNewAggregationFunction(String name, Class<? extends AggregationFunction> clazz,
			AggregationFunctionMetaDataProvider metaDataProvider) {
		AGGREATION_FUNCTIONS.put(name, clazz);
		AGGREGATION_FUNCTIONS_META_DATA_PROVIDER.put(name, metaDataProvider);
	}

	/**
	 * This function is called once during the aggregation process, when all {@link Aggregator}s are
	 * known. In this step post-processing like normalization etc. can be done.
	 * 
	 * The default implementation does nothing.
	 */
	public void postProcessing(List<Aggregator> allAggregators) {
		// do nothing
	}

}
