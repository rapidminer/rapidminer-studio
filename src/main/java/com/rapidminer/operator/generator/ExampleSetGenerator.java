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
package com.rapidminer.operator.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.io.AbstractExampleSource;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.AboveOperatorVersionCondition;
import com.rapidminer.parameter.conditions.BelowOrEqualOperatorVersionCondition;
import com.rapidminer.parameter.conditions.EqualStringCondition;
import com.rapidminer.parameter.conditions.NonEqualStringCondition;
import com.rapidminer.parameter.conditions.OrParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.parameter.internal.DataManagementParameterHelper;


/**
 * Generates a random example set for testing purposes. Uses a subclass of {@link TargetFunction} to
 * create the examples from the attribute values. Possible target functions are: random, sum (of all
 * attributes), polynomial (of the first three attributes, degree 3), non linear, sinus, sinus
 * frequency (like sinus, but with frequencies in the argument), random classification, sum
 * classification (like sum, but positive for positive sum and negative for negative sum),
 * interaction classification (positive for negative x or positive y and negative z), sinus
 * classification (positive for positive sinus values).
 *
 * @author Ingo Mierswa, Marius Helf
 */
public class ExampleSetGenerator extends AbstractExampleSource {

	/** The parameter name for &quot;Specifies the target function of this example set&quot; */
	public static final String PARAMETER_TARGET_FUNCTION = "target_function";

	/** The parameter name for &quot;The number of generated examples.&quot; */
	public static final String PARAMETER_NUMBER_EXAMPLES = "number_examples";

	/** The parameter name for &quot;The number of attributes.&quot; */
	public static final String PARAMETER_NUMBER_OF_ATTRIBUTES = "number_of_attributes";

	/** The parameter name for &quot;The minimum value for the attributes.&quot; */
	public static final String PARAMETER_ATTRIBUTES_LOWER_BOUND = "attributes_lower_bound";

	/** The parameter name for &quot;The maximum value for the attributes.&quot; */
	public static final String PARAMETER_ATTRIBUTES_UPPER_BOUND = "attributes_upper_bound";

	/** The parameter name for &quot;Standard deviation of the Gaussian distribution used for generating attributes.&quot; */
	public static final String PARAMETER_ATTRIBUTES_GAUSSIAN_STDDEV = "gaussian_standard_deviation";

	/** The parameter name for &quot;The radius of the outermost ring cluster.&quot; */
	public static final String PARAMETER_ATTRIBUTES_LARGEST_RADIUS = "largest_radius";

	/** The parameter name for &quot;Determines, how the data is represented internally.&quot; */
	public static final String PARAMETER_DATAMANAGEMENT = "datamanagement";

	private static final String[] KNOWN_FUNCTION_NAMES = new String[] { "random", // regression
			"sum", "polynomial", "non linear", "one variable non linear", "complicated function", "complicated function2",
			"simple sinus", "sinus", "simple superposition", "sinus frequency", "sinus with trend", "sinc",
			"triangular function", "square pulse function", "random classification", // classification
			"one third classification", "sum classification", "quadratic classification", "simple non linear classification",
			"interaction classification", "simple polynomial classification", "polynomial classification",
			"checkerboard classification", "random dots classification", "global and local models classification",
			"sinus classification", "multi classification", "two gaussians classification", "transactions dataset", // transactions
			"grid function", // clusters
			"three ring clusters", "spiral cluster", "single gaussian cluster", "gaussian mixture clusters",
			"driller oscillation timeseries" // timeseries

	};

	@SuppressWarnings("unchecked")
	private static final Class<? extends TargetFunction>[] KNOWN_FUNCTION_IMPLEMENTATIONS = new Class[] {
			RandomFunction.class, // regression
			SumFunction.class, PolynomialFunction.class, NonLinearFunction.class, OneVariableNonLinearFunction.class,
			ComplicatedFunction.class, ComplicatedFunction2.class, SimpleSinusFunction.class, SinusFunction.class,
			SimpleSuperpositionFunction.class, SinusFrequencyFunction.class, SinusWithTrendFunction.class,
			SincFunction.class, TriangularFunction.class, SquarePulseFunction.class, RandomClassificationFunction.class, // classification
			OneThirdClassification.class, SumClassificationFunction.class, QuadraticClassificationFunction.class,
			SimpleNonLinearClassificationFunction.class, InteractionClassificationFunction.class,
			SimplePolynomialClassificationFunction.class, PolynomialClassificationFunction.class,
			CheckerboardClassificationFunction.class, RandomDotsClassificationFunction.class,
			GlobalAndLocalPatternsFunction.class, SinusClassificationFunction.class, MultiClassificationFunction.class,
			TwoGaussiansClassificationFunction.class, TransactionDatasetFunction.class, // transactions
			GridFunction.class, // clusters
			RingClusteringFunction.class, SpiralClusteringFunction.class, GaussianFunction.class,
			GaussianMixtureFunction.class, DrillerOscillationFunction.class // timeseries
	};

	private static final String[] FUCTIONS_IGNORING_BOUND = new String[] { "transactions dataset", "spiral cluster",
			"driller oscillation timeseries" };

	private static final String[] FUNCTIONS_USING_GAUSSIAN_STDDEV = new String[] { "single gaussian cluster" };
	private static final String[] FUNCTIONS_USING_LARGEST_RADIUS = new String[] { "three ring clusters" };

	private static final String[] FUNCTIONS_USING_SINGLE_BOUND = (String[]) ArrayUtils.addAll(
			FUNCTIONS_USING_GAUSSIAN_STDDEV, FUNCTIONS_USING_LARGEST_RADIUS);

	protected static final double DEFAULT_SINGLE_BOUND = 10.0;

	public static final OperatorVersion VERSION_TARGET_PARAMETERS_CHANGED = new OperatorVersion(7, 1, 1);

	public ExampleSetGenerator(OperatorDescription description) {
		super(description);
	}

	@Override
	public MetaData getGeneratedMetaData() throws OperatorException {
		TargetFunction function = createTargetFunction();
		return function.getGeneratedMetaData();
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {

		// init
		int numberOfExamples = getParameterAsInt(PARAMETER_NUMBER_EXAMPLES);
		int numberOfAttributes = getParameterAsInt(PARAMETER_NUMBER_OF_ATTRIBUTES);
		TargetFunction function = createTargetFunction();

		// create table
		List<Attribute> attributes = new ArrayList<>();
		for (int m = 0; m < numberOfAttributes; m++) {
			attributes.add(AttributeFactory.createAttribute("att" + (m + 1), Ontology.REAL));
		}
		Attribute label = function.getLabel();
		if (label != null) {
			attributes.add(label);
		}
		ExampleSetBuilder builder = ExampleSets.from(attributes).withExpectedSize(numberOfExamples);

		// create data
		RandomGenerator random = RandomGenerator.getRandomGenerator(this);

		int datamanagement = getParameterAsInt(PARAMETER_DATAMANAGEMENT);
		if (!Boolean.parseBoolean(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT))) {
			datamanagement = DataRowFactory.TYPE_DOUBLE_ARRAY;
			builder.withOptimizationHint(DataManagementParameterHelper.getSelectedDataManagement(this));
		}

		DataRowFactory factory = new DataRowFactory(datamanagement, '.');
		try {
			function.init(random);
			getProgress().setTotal(numberOfExamples);
			int progressCounter = 0;
			for (int n = 0; n < numberOfExamples; n++) {

				double[] features = function.createArguments(numberOfAttributes, random);
				double[] example = features;
				if (label != null) {
					example = new double[numberOfAttributes + 1];
					System.arraycopy(features, 0, example, 0, features.length);
					example[example.length - 1] = function.calculate(features);
				}
				DataRow row = factory.create(example.length);
				for (int i = 0; i < example.length; i++) {
					row.set(attributes.get(i), example[i]);
				}
				row.trim();
				builder.addDataRow(row);

				// trigger operator progress every 100 examples
				++progressCounter;
				if (progressCounter % 100 == 0) {
					getProgress().step(100);
					progressCounter = 0;
				}

			}
		} catch (TargetFunction.FunctionException e) {
			throw new UserError(this, 918, e.getFunctionName(), e.getMessage());
		}

		if (label != null) {
			builder.withRole(label, Attributes.LABEL_NAME);
		}
		// create example set and return it
		ExampleSet result = builder.build();

		getProgress().complete();

		return result;
	}

	private TargetFunction createTargetFunction() throws UndefinedParameterError, UserError {
		String functionName = getParameterAsString(PARAMETER_TARGET_FUNCTION);
		if (functionName == null) {
			throw new UndefinedParameterError(PARAMETER_TARGET_FUNCTION, this);
		}

		TargetFunction function = null;
		try {
			function = getFunctionForName(functionName);
		} catch (Exception e) {
			throw new UserError(this, e, 904, new Object[] { functionName, e });
		}

		int numberOfExamples = getParameterAsInt(PARAMETER_NUMBER_EXAMPLES);
		int numberOfAttributes = getParameterAsInt(PARAMETER_NUMBER_OF_ATTRIBUTES);

		if (Arrays.asList(FUNCTIONS_USING_GAUSSIAN_STDDEV).contains(functionName)) {
			setSingleArgumentBound(function, PARAMETER_ATTRIBUTES_GAUSSIAN_STDDEV);
		} else if (Arrays.asList(FUNCTIONS_USING_LARGEST_RADIUS).contains(functionName)) {
			setSingleArgumentBound(function, PARAMETER_ATTRIBUTES_LARGEST_RADIUS);
		} else if (Arrays.asList(FUCTIONS_IGNORING_BOUND).contains(functionName)) {
			// no bound
		} else {
			double lower = getParameterAsDouble(PARAMETER_ATTRIBUTES_LOWER_BOUND);
			double upper = getParameterAsDouble(PARAMETER_ATTRIBUTES_UPPER_BOUND);
			if (upper < lower) {
				throw new UserError(this, 226, lower, upper);
			}
			function.setLowerArgumentBound(lower);
			function.setUpperArgumentBound(upper);
		}

		function.setTotalNumberOfExamples(numberOfExamples);
		function.setTotalNumberOfAttributes(numberOfAttributes);

		return function;
	}

	/**
	 * This function sets the single argument bound of the target function, using the parameter(s)
	 * that is/are compatible with the current version.
	 *
	 * @param function
	 *            The target function.
	 * @param targetParameter
	 *            The new parameter substituting lower & upper bound parameters.
	 * @throws UndefinedParameterError
	 */
	private void setSingleArgumentBound(TargetFunction function, String targetParameter) throws UndefinedParameterError {
		if (getCompatibilityLevel().isAtMost(VERSION_TARGET_PARAMETERS_CHANGED)) {
			double lower = getParameterAsDouble(PARAMETER_ATTRIBUTES_LOWER_BOUND);
			double upper = getParameterAsDouble(PARAMETER_ATTRIBUTES_UPPER_BOUND);
			double newParam = Math.max(Math.max(Math.abs(lower), Math.abs(upper)), DEFAULT_SINGLE_BOUND);
			function.setLowerArgumentBound(newParam);
		} else {
			function.setLowerArgumentBound(getParameterAsDouble(targetParameter));
		}
	}

	// ================================================================================

	@SuppressWarnings("unchecked")
	public static TargetFunction getFunctionForName(String functionName)
			throws IllegalAccessException, InstantiationException, ClassNotFoundException {
		for (int i = 0; i < KNOWN_FUNCTION_NAMES.length; i++) {
			if (KNOWN_FUNCTION_NAMES[i].equals(functionName)) {
				return KNOWN_FUNCTION_IMPLEMENTATIONS[i].newInstance();
			}
		}
		Class<? extends TargetFunction> clazz = (Class<? extends TargetFunction>) Tools.classForName(functionName);
		return clazz.newInstance();
	}

	// ================================================================================

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeStringCategory(PARAMETER_TARGET_FUNCTION,
				"Specifies the target function of this example set", KNOWN_FUNCTION_NAMES, KNOWN_FUNCTION_NAMES[0]);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_NUMBER_EXAMPLES, "The number of generated examples.", 1, Integer.MAX_VALUE,
				100);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_NUMBER_OF_ATTRIBUTES, "The number of attributes.", 1, Integer.MAX_VALUE, 5);
		type.setExpert(false);
		types.add(type);

		NonEqualStringCondition useTwoBounds = new NonEqualStringCondition(this, PARAMETER_TARGET_FUNCTION, false,
				(String[]) ArrayUtils.addAll(FUCTIONS_IGNORING_BOUND, FUNCTIONS_USING_SINGLE_BOUND));

		type = new ParameterTypeDouble(PARAMETER_ATTRIBUTES_LOWER_BOUND,
				"The minimum value for the attributes. In case of target functions using Gaussian distribution, the attribute values may exceed this value.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, -10);
		type.registerDependencyCondition(new OrParameterCondition(this, false,
				new BelowOrEqualOperatorVersionCondition(this, VERSION_TARGET_PARAMETERS_CHANGED), useTwoBounds));
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_ATTRIBUTES_UPPER_BOUND,
				"The maximum value for the attributes. In case of target functions using Gaussian distribution, the attribute values may exceed this value.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 10);
		type.registerDependencyCondition(new OrParameterCondition(this, false,
				new BelowOrEqualOperatorVersionCondition(this, VERSION_TARGET_PARAMETERS_CHANGED), useTwoBounds));
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_ATTRIBUTES_GAUSSIAN_STDDEV,
				"Standard deviation of the Gaussian distribution used for generating attributes.", Double.MIN_VALUE,
				Double.POSITIVE_INFINITY, 10);
		type.registerDependencyCondition(new AboveOperatorVersionCondition(this, VERSION_TARGET_PARAMETERS_CHANGED));
		type.registerDependencyCondition(
				new EqualStringCondition(this, PARAMETER_TARGET_FUNCTION, false, FUNCTIONS_USING_GAUSSIAN_STDDEV));
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_ATTRIBUTES_LARGEST_RADIUS, "The radius of the outermost ring cluster.",
				10.0, Double.POSITIVE_INFINITY, 10);
		type.registerDependencyCondition(new AboveOperatorVersionCondition(this, VERSION_TARGET_PARAMETERS_CHANGED));
		type.registerDependencyCondition(
				new EqualStringCondition(this, PARAMETER_TARGET_FUNCTION, false, FUNCTIONS_USING_LARGEST_RADIUS));
		types.add(type);

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		DataManagementParameterHelper.addParameterTypes(types, this);
		return types;
	}

	@Override
	public int checkProperties() {
		int errorCount = super.checkProperties();

		String functionName;
		try {
			functionName = getParameterAsString(PARAMETER_TARGET_FUNCTION);
			TargetFunction function = getFunctionForName(functionName);
			int functionMinAttr = function.getMinNumberOfAttributes();
			int functionMaxAttr = function.getMaxNumberOfAttributes();
			int setAttr = getParameterAsInt(PARAMETER_NUMBER_OF_ATTRIBUTES);
			List<QuickFix> quickFixes = new LinkedList<>();
			if (functionMinAttr == functionMaxAttr && setAttr != functionMinAttr) {
				quickFixes.add(
						new ParameterSettingQuickFix(this, PARAMETER_NUMBER_OF_ATTRIBUTES, String.valueOf(functionMaxAttr)));
				addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(), quickFixes,
						"example_set_generator.wrong_number_of_attributes", functionName, functionMaxAttr));
			} else if (setAttr > functionMaxAttr) {
				quickFixes.add(
						new ParameterSettingQuickFix(this, PARAMETER_NUMBER_OF_ATTRIBUTES, String.valueOf(functionMaxAttr)));
				addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(), quickFixes,
						"example_set_generator.too_many_attributes", functionName, functionMaxAttr));
			} else if (setAttr < functionMinAttr) {
				quickFixes.add(
						new ParameterSettingQuickFix(this, PARAMETER_NUMBER_OF_ATTRIBUTES, String.valueOf(functionMinAttr)));
				addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(), quickFixes,
						"example_set_generator.too_few_attributes", functionName, functionMinAttr));
			}
		} catch (UndefinedParameterError e) {
			// this should not happen
		} catch (IllegalAccessException e) {
			// this should not happen
		} catch (InstantiationException e) {
			// this should not happen
		} catch (ClassNotFoundException e) {
			// this should not happen
		}

		return errorCount;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return new OperatorVersion[] { VERSION_TARGET_PARAMETERS_CHANGED };
	}
}
