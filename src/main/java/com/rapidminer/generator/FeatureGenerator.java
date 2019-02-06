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
package com.rapidminer.generator;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowReader;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.RandomGenerator;


/**
 * Abstract superclass of all attribute generators. Implementing classes have to implement the
 * <tt>generate(Example)</tt>, method and specify the input and output attributes by the appropriate
 * methods so that the using algorithms can use them correctly.
 * 
 * @author Simon Fischer, Ingo Mierswa Exp $
 */
public abstract class FeatureGenerator {

	private static final String[] FUNCTION_NAMES = { "+", "-", "*", "/", "1/", "sin", "cos", "tan", "atan", "exp", "log",
			"min", "max", "floor", "ceil", "round", "sqrt", "abs", "sgn", "pow" };

	/** The classes which corresponds to FUNCTION_NAMES. */
	private static final List<Class<? extends FeatureGenerator>> GENERATOR_CLASSES = Arrays.asList(
			BasicArithmeticOperationGenerator.class, BasicArithmeticOperationGenerator.class,
			BasicArithmeticOperationGenerator.class, BasicArithmeticOperationGenerator.class, ReciprocalValueGenerator.class,
			TrigonometricFunctionGenerator.class, TrigonometricFunctionGenerator.class, TrigonometricFunctionGenerator.class,
			TrigonometricFunctionGenerator.class, ExponentialFunctionGenerator.class, ExponentialFunctionGenerator.class,
			MinMaxGenerator.class, MinMaxGenerator.class, FloorCeilGenerator.class, FloorCeilGenerator.class,
			FloorCeilGenerator.class, SquareRootGenerator.class, AbsoluteValueGenerator.class, SignumGenerator.class,
			PowerGenerator.class);

	/** Maps function names to generators. */
	private static Map<String, Class<? extends FeatureGenerator>> generatorMap;

	static {
		generatorMap = new HashMap<>();
		for (int i = 0; i < FUNCTION_NAMES.length; i++) {
			generatorMap.put(FUNCTION_NAMES[i], GENERATOR_CLASSES.get(i));
		}
	}

	/** Indicates a non-restrictive generator selection mode. */
	public static final int SELECTION_MODE_ALL = 0;

	/** Indicates a restrictive generator selection mode. */
	public static final int SELECTION_MODE_RESTRICTIVE = 1;

	/**
	 * Indicates the selection mode. One of SELECTION_MODE_ALL and SELECTION_MODE_RESTRICTIVE.
	 */
	private static int selectionMode = SELECTION_MODE_ALL;

	/** The attributes of the function(s) calculated by this FeatureGenerator. */
	protected Attribute[] resultAttributes;

	/**
	 * The argument attributes on which to operate with respect to the example tables attribute
	 * array.
	 */
	private Attribute[] arguments = null;

	/** The example table to work on. */
	private ExampleTable exampleTable;

	// ------------------------------ The abstract methods
	// ------------------------------

	/**
	 * Generates the new attribute values for the example e and returns the new attribute values as
	 * doubles. <tt>e.getAttribute(getArgument(i))</tt> is the correct way to access argument
	 * <i>i</i>. If the according attribute's type is VALUE_SERIES, the end index can be determined
	 * by <tt>i_end = getExampleTable().getBlockEndIndex(getArgument(i))</tt>. Thus all values of
	 * the series can be accessed using indices <i>i</i> through <i>i_end</i>.
	 */
	public abstract void generate(DataRow data) throws GenerationException;

	/**
	 * Returns an array of Attributes where the length is the arity of the generator, <tt>[i]</tt>
	 * is the attribute type of the i-th argument.
	 */
	public abstract Attribute[] getInputAttributes();

	/** Returns the generated attributes types. */
	public abstract Attribute[] getOutputAttributes(ExampleTable input);

	/**
	 * Subclasses must implement this method so that a new instance of this generator class is
	 * returned. The arguments and the example table will not be cloned and thus be null. This kind
	 * of clone is needed as generating algorithms must be able to clone generators form their pool
	 * without changing the arguments already set for the others.
	 */
	public abstract FeatureGenerator newInstance();

	/**
	 * Sets the function name. This method is only useful if subclasses can generate more than one
	 * function. (like the BasicArithmeticOperationGenerator).
	 */
	public abstract void setFunction(String name);

	/**
	 * Sets the function name. This method is only useful if subclasses can generate more than one
	 * function. (like the BasicArithmeticOperationGenerator).
	 */
	public abstract String getFunction();

	/**
	 * Returns all compatible input attribute arrays for this generator from the given example set
	 * as list. Features with a depth greater than maxDepth or which contains one of the given
	 * functions should not be used as input candidates. Subclasses must consider if the generator
	 * is self-applicable or commutative. A maxDepth of -1 means that no maximal depth should be
	 * considered.
	 */
	public abstract List<Attribute[]> getInputCandidates(ExampleSet exampleSet, String[] functions);

	// --------------------------------------------------------------------------------

	protected boolean checkCompatibility(Attribute attribute, Attribute compatible, String[] functions) {
		if (Tools.compatible(attribute, compatible)) {
			for (int f = 0; f < functions.length; f++) {
				if (attribute.getConstruction().indexOf(functions[f]) != -1) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	protected void setExampleTable(ExampleTable et) {
		this.exampleTable = et;
	}

	/** Gets the example table the examples are from. */
	protected ExampleTable getExampleTable() {
		return exampleTable;
	}

	/**
	 * Sets the arguments (indices) used in future <tt>generate(...)</tt> calls and has to be called
	 * prior to any <tt>generate(...)</tt> calls. The caller of this method has to take care that:
	 * <ul>
	 * <li><tt>args.length == getInputAttributes().length</tt>, i.e. that the arity is correct.
	 * <li>The types of the example attributes match the types specified by
	 * <tt>getInputAttributes()</tt>.
	 * <li>The true attribute indices are used (as used by the example set's example table)
	 * </ul>
	 */
	public void setArguments(Attribute[] args) {
		arguments = args;
	}

	/**
	 * returns <tt>true</tt>, if the arguments have already been set, and <tt>false</tt> otherwise.
	 */
	public boolean argumentsSet() {
		return getInputAttributes().length == 0 || arguments != null;
	}

	/**
	 * Returns the i-th selected argument (the true index as used in the example set's example
	 * table).
	 */
	public Attribute getArgument(int i) {
		return arguments[i];
	}

	/**
	 * Checks if the arguments are compatible with the attributes specified by getInputAttributes().
	 */
	private boolean argumentsOk(ExampleTable input) {
		Attribute[] inputA = getInputAttributes();
		for (int i = 0; i < inputA.length; i++) {
			if (!Tools.compatible(arguments[i], inputA[i])) {
				return false;
			}
		}
		return true;
	}

	// --------------------------------------------------------------------------------

	/** Creates a new FeatureGenerator for a given function name. */
	public static FeatureGenerator createGeneratorForFunction(String functionName) {
		if (functionName == null) {
			return null;
		}
		Class<? extends FeatureGenerator> genClass = generatorMap.get(functionName);
		if (genClass == null) {
			if (!functionName.startsWith(ConstantGenerator.FUNCTION_NAME)) {
				return null;
			}
			FeatureGenerator gen = new ConstantGenerator();
			gen.setFunction(functionName);
			return gen;
		}
		try {
			FeatureGenerator gen = genClass.newInstance();
			gen.setFunction(functionName);
			return gen;
		} catch (Exception e) {
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.generator.FeatureGenerator.instantiating_error",
					genClass.getName());
			return null;
		}

	}

	// --------------------------------------------------------------------------------

	/**
	 * Randomly selects a generator from the generator list. The probability of a generator to be
	 * selected is proportional to its number of attribute combinations as delivered by
	 * {@link #getInputCandidates(ExampleSet, String[])} method. Returns null if no generators are
	 * applicable.
	 *
	 * @param generators
	 *            List of {@link FeatureGenerator}s
	 */
	public static FeatureGenerator selectGenerator(ExampleSet exampleSet, List<? extends FeatureGenerator> generators,
			String[] functions, RandomGenerator random) {
		int combinationSum = 0;
		double[] probs = new double[generators.size()];
		int k = 0;
		for (FeatureGenerator generator : generators) {
			// probs[k] =
			// generator.getNumberOfApplicableGenerations(exampleSet);
			int candidates = generator.getInputCandidates(exampleSet, functions).size();
			probs[k] = candidates;
			combinationSum += candidates;
			k++;
		}
		if (combinationSum == 0) {
			return null;
		}
		for (k = 0; k < probs.length; k++) {
			probs[k] /= combinationSum;
		}

		return generators.get(random.randomIndex(probs));
	}

	// --------------------------------------------------------------------------------

	/**
	 * Generates all new attributes and updates the ExampleTable. Returns a list of Attributes for
	 * the newly generated attributes.
	 * 
	 * @param exampleTable
	 *            the source example table
	 * @param generatorList
	 *            List of FeatureGenerators
	 * @return A list of Attributes
	 */
	public static List<Attribute> generateAll(ExampleTable exampleTable, Collection<FeatureGenerator> generatorList)
			throws GenerationException {
		LogService.getRoot().log(Level.FINE, "com.rapidminer.generator.FeatureGenerator.starting_feature_generation",
				generatorList.size());

		Iterator<FeatureGenerator> gi = generatorList.iterator();
		while (gi.hasNext()) {
			gi.next().setExampleTable(exampleTable);
		}

		// for performance reasons convert the list to an array
		FeatureGenerator[] generators = new FeatureGenerator[generatorList.size()];
		generatorList.toArray(generators);

		List<Attribute> newAttributeList = newAttributes(generators, exampleTable);
		// add the attributes to the example table and ensure length of the
		// DataRows
		exampleTable.addAttributes(newAttributeList);
		LogService.getRoot().log(Level.FINE, "com.rapidminer.generator.FeatureGenerator.generator_list", generatorList);
		LogService.getRoot().log(Level.FINE,
				"com.rapidminer.generator.FeatureGenerator.input_has_feature_count_and_example_count",
				new Object[] { exampleTable.getAttributeCount(), exampleTable.size() });

		// generate the attribute values:
		DataRowReader reader = exampleTable.getDataRowReader();
		while (reader.hasNext()) {
			DataRow dataRow = reader.next();

			for (int j = 0; j < generators.length; j++) {
				generators[j].generate(dataRow);
			}
		}

		LogService.getRoot().log(Level.FINE, "com.rapidminer.generator.FeatureGenerator.finished_feature_generation");
		LogService.getRoot().log(Level.FINE,
				"com.rapidminer.generator.FeatureGenerator.generated_set_has_feature_count_and_example_count",
				new Object[] { exampleTable.getAttributeCount(), exampleTable.size() });

		return newAttributeList;
	}

	/**
	 * Returns a list of new Attributes that are generated by the given generators.
	 */
	private static List<Attribute> newAttributes(FeatureGenerator[] generators, ExampleTable exampleTable) {
		List<Attribute> newAttributeList = new LinkedList<>();

		// add the attributes to the example table
		for (int i = 0; i < generators.length; i++) {
			Attribute outputAttribute[] = generators[i].getOutputAttributes(exampleTable);
			generators[i].resultAttributes = new Attribute[outputAttribute.length];

			for (int j = 0; j < outputAttribute.length; j++) {
				newAttributeList.add(outputAttribute[j]);
				generators[i].resultAttributes[j] = outputAttribute[j];
			}

			// check the arguments
			if (!generators[i].argumentsSet()) {
				throw new RuntimeException("Catastrophic error: arguments not set for " + generators[i] + "!");
			}
			if (!generators[i].argumentsOk(exampleTable)) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.generator.FeatureGenerator.wrong_argument_types",
						generators[i]);
			}
		}
		return newAttributeList;
	}

	public static int getSelectionMode() {
		return selectionMode;
	}

	public static void setSelectionMode(int mode) {
		selectionMode = mode;
	}

	@Override
	public String toString() {
		return "FeatureGenerator (" + getClass().getName() + ")";
	}

	/**
	 * A FeatureGenerator equals another FeatureGenerator if its class is equal and its arguments
	 * are equal and its function names are equal.
	 */
	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (!this.getClass().equals(o.getClass())) {
			return false;
		}
		FeatureGenerator fg = (FeatureGenerator) o;
		if (!this.getFunction().equals(fg.getFunction())) {
			return false;
		}
		if (this.arguments.length != fg.arguments.length) {
			return false;
		}
		for (int i = 0; i < arguments.length; i++) {
			if (!this.arguments[i].equals(fg.arguments[i])) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hashCode = getFunction().hashCode();
		if (this.arguments != null) {
			hashCode ^= Arrays.hashCode(this.arguments);
		}
		return hashCode;
	}
}
