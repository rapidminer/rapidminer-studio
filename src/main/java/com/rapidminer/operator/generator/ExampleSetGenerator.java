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
package com.rapidminer.operator.generator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.ListDataRowReader;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.io.AbstractExampleSource;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.Tools;


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

	/** The parameter name for &quot;Determines, how the data is represented internally.&quot; */
	public static final String PARAMETER_DATAMANAGEMENT = "datamanagement";

	private static final String[] KNOWN_FUNCTION_NAMES = new String[] {
			"random", // regression
			"sum", "polynomial", "non linear", "one variable non linear", "complicated function", "complicated function2",
			"simple sinus", "sinus", "simple superposition",
			"sinus frequency",
			"sinus with trend",
			"sinc",
			"triangular function",
			"square pulse function",
			"random classification", // classification
			"one third classification", "sum classification", "quadratic classification",
			"simple non linear classification", "interaction classification", "simple polynomial classification",
			"polynomial classification", "checkerboard classification", "random dots classification",
			"global and local models classification", "sinus classification", "multi classification",
			"two gaussians classification",
			"transactions dataset", // transactions
			"grid function", // clusters
			"three ring clusters", "spiral cluster", "single gaussian cluster", "gaussian mixture clusters",
			"driller oscillation timeseries" // timeseries

	};

	private static final Class[] KNOWN_FUNCTION_IMPLEMENTATIONS = new Class[] {
			RandomFunction.class, // regression
			SumFunction.class, PolynomialFunction.class, NonLinearFunction.class, OneVariableNonLinearFunction.class,
			ComplicatedFunction.class, ComplicatedFunction2.class, SimpleSinusFunction.class,
			SinusFunction.class,
			SimpleSuperpositionFunction.class,
			SinusFrequencyFunction.class,
			SinusWithTrendFunction.class,
			SincFunction.class,
			TriangularFunction.class,
			SquarePulseFunction.class,
			RandomClassificationFunction.class, // classification
			OneThirdClassification.class, SumClassificationFunction.class, QuadraticClassificationFunction.class,
			SimpleNonLinearClassificationFunction.class, InteractionClassificationFunction.class,
			SimplePolynomialClassificationFunction.class, PolynomialClassificationFunction.class,
			CheckerboardClassificationFunction.class, RandomDotsClassificationFunction.class,
			GlobalAndLocalPatternsFunction.class, SinusClassificationFunction.class, MultiClassificationFunction.class,
			TwoGaussiansClassificationFunction.class,
			TransactionDatasetFunction.class, // transactions
			GridFunction.class, // clusters
			RingClusteringFunction.class, SpiralClusteringFunction.class, GaussianFunction.class,
			GaussianMixtureFunction.class, DrillerOscillationFunction.class // timeseries
	};

	public ExampleSetGenerator(OperatorDescription description) {
		super(description);
	}

	@Override
	public MetaData getGeneratedMetaData() throws OperatorException {
		TargetFunction function = createTargetFunction();
		ExampleSetMetaData generatedMD = function.getGeneratedMetaData();
		return generatedMD;
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
		MemoryExampleTable table = new MemoryExampleTable(attributes);

		// create data
		RandomGenerator random = RandomGenerator.getRandomGenerator(this);
		List<DataRow> data = new LinkedList<>();
		DataRowFactory factory = new DataRowFactory(getParameterAsInt(PARAMETER_DATAMANAGEMENT), '.');
		try {
			function.init(random);
			getProgress().setTotal(numberOfExamples);
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
				data.add(row);

				getProgress().step();
			}
		} catch (TargetFunction.FunctionException e) {
			throw new UserError(this, 918, e.getFunctionName(), e.getMessage());
		}

		// fill table with data
		table.readExamples(new ListDataRowReader(data.iterator()));

		// create example set and return it
		ExampleSet result = table.createExampleSet(label);

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
		double lower = getParameterAsDouble(PARAMETER_ATTRIBUTES_LOWER_BOUND);
		double upper = getParameterAsDouble(PARAMETER_ATTRIBUTES_UPPER_BOUND);
		if (upper < lower) {
			throw new UserError(this, 226, lower, upper);
		}
		function.setLowerArgumentBound(lower);
		function.setUpperArgumentBound(upper);
		function.setTotalNumberOfExamples(numberOfExamples);
		function.setTotalNumberOfAttributes(numberOfAttributes);

		return function;
	}

	// ================================================================================

	public static TargetFunction getFunctionForName(String functionName) throws IllegalAccessException,
			InstantiationException, ClassNotFoundException {
		for (int i = 0; i < KNOWN_FUNCTION_NAMES.length; i++) {
			if (KNOWN_FUNCTION_NAMES[i].equals(functionName)) {
				return (TargetFunction) KNOWN_FUNCTION_IMPLEMENTATIONS[i].newInstance();
			}
		}
		Class clazz = Tools.classForName(functionName);
		return (TargetFunction) clazz.newInstance();
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

		types.add(new ParameterTypeDouble(PARAMETER_ATTRIBUTES_LOWER_BOUND, "The minimum value for the attributes.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, -10));
		types.add(new ParameterTypeDouble(PARAMETER_ATTRIBUTES_UPPER_BOUND, "The maximum value for the attributes.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 10));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		types.add(new ParameterTypeCategory(PARAMETER_DATAMANAGEMENT, "Determines, how the data is represented internally.",
				DataRowFactory.TYPE_NAMES, DataRowFactory.TYPE_DOUBLE_ARRAY));
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
				quickFixes.add(new ParameterSettingQuickFix(this, PARAMETER_NUMBER_OF_ATTRIBUTES, String
						.valueOf(functionMaxAttr)));
				addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(), quickFixes,
						"example_set_generator.wrong_number_of_attributes", functionName, functionMaxAttr));
			} else if (setAttr > functionMaxAttr) {
				quickFixes.add(new ParameterSettingQuickFix(this, PARAMETER_NUMBER_OF_ATTRIBUTES, String
						.valueOf(functionMaxAttr)));
				addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(), quickFixes,
						"example_set_generator.too_many_attributes", functionName, functionMaxAttr));
			} else if (setAttr < functionMinAttr) {
				quickFixes.add(new ParameterSettingQuickFix(this, PARAMETER_NUMBER_OF_ATTRIBUTES, String
						.valueOf(functionMinAttr)));
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

}
