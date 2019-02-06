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
package com.rapidminer.operator.features.construction;

import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.generator.AbsoluteValueGenerator;
import com.rapidminer.generator.ExponentialFunctionGenerator;
import com.rapidminer.generator.FeatureGenerator;
import com.rapidminer.generator.FloorCeilGenerator;
import com.rapidminer.generator.MinMaxGenerator;
import com.rapidminer.generator.PowerGenerator;
import com.rapidminer.generator.SignumGenerator;
import com.rapidminer.generator.SinusFactory;
import com.rapidminer.generator.SquareRootGenerator;
import com.rapidminer.generator.TrigonometricFunctionGenerator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.expression.internal.ExpressionParserUtils;


/**
 * Basically the same operator as the
 * {@link com.rapidminer.operator.features.construction.GeneratingGeneticAlgorithm} operator. This
 * version adds additional generators and improves the simple GGA approach by providing some basic
 * intron prevention techniques. In general, this operator seems to work better than the original
 * approach but frequently deliver inferior results compared to the operator
 * {@link com.rapidminer.operator.features.construction.YAGGA2}.
 *
 * @author Ingo Mierswa
 */
public class AGA extends GeneratingGeneticAlgorithm {

	/** The parameter name for &quot;Generate square root values.&quot; */
	public static final String PARAMETER_USE_SQUARE_ROOTS = "use_square_roots";

	/** The parameter name for &quot;Generate the power of one attribute and another.&quot; */
	public static final String PARAMETER_USE_POWER_FUNCTIONS = "use_power_functions";

	/** The parameter name for &quot;Generate sinus.&quot; */
	public static final String PARAMETER_USE_SIN = "use_sin";

	/** The parameter name for &quot;Generate cosinus.&quot; */
	public static final String PARAMETER_USE_COS = "use_cos";

	/** The parameter name for &quot;Generate tangens.&quot; */
	public static final String PARAMETER_USE_TAN = "use_tan";

	/** The parameter name for &quot;Generate arc tangens.&quot; */
	public static final String PARAMETER_USE_ATAN = "use_atan";

	/** The parameter name for &quot;Generate exponential functions.&quot; */
	public static final String PARAMETER_USE_EXP = "use_exp";

	/** The parameter name for &quot;Generate logarithmic functions.&quot; */
	public static final String PARAMETER_USE_LOG = "use_log";

	/** The parameter name for &quot;Generate absolute values.&quot; */
	public static final String PARAMETER_USE_ABSOLUTE_VALUES = "use_absolute_values";

	/** The parameter name for &quot;Generate minimum values.&quot; */
	public static final String PARAMETER_USE_MIN = "use_min";

	/** The parameter name for &quot;Generate maximum values.&quot; */
	public static final String PARAMETER_USE_MAX = "use_max";

	/** The parameter name for &quot;Generate signum values.&quot; */
	public static final String PARAMETER_USE_SGN = "use_sgn";

	/** The parameter name for &quot;Generate floor, ceil, and rounded values.&quot; */
	public static final String PARAMETER_USE_FLOOR_CEIL_FUNCTIONS = "use_floor_ceil_functions";

	/** The parameter name for &quot;Use restrictive generator selection (faster).&quot; */
	public static final String PARAMETER_RESTRICTIVE_SELECTION = "restrictive_selection";

	/** The parameter name for &quot;Remove useless attributes.&quot; */
	public static final String PARAMETER_REMOVE_USELESS = "remove_useless";

	/** The parameter name for &quot;Remove equivalent attributes.&quot; */
	public static final String PARAMETER_REMOVE_EQUIVALENT = "remove_equivalent";

	/** The parameter name for &quot;Check this number of samples to prove equivalency.&quot; */
	public static final String PARAMETER_EQUIVALENCE_SAMPLES = "equivalence_samples";

	/**
	 * The parameter name for &quot;Consider two attributes equivalent if their difference is not
	 * bigger than epsilon.&quot;
	 */
	public static final String PARAMETER_EQUIVALENCE_EPSILON = "equivalence_epsilon";

	/**
	 * The parameter name for &quot;Recalculates attribute statistics before equivalence
	 * check.&quot;
	 */
	public static final String PARAMETER_EQUIVALENCE_USE_STATISTICS = "equivalence_use_statistics";

	/**
	 * The parameter name for &quot;Use this number of highest frequency peaks for sinus
	 * generation.&quot;
	 */
	public static final String PARAMETER_SEARCH_FOURIER_PEAKS = "search_fourier_peaks";

	/** The parameter name for &quot;Use this number of additional peaks for each found peak.&quot; */
	public static final String PARAMETER_ATTRIBUTES_PER_PEAK = "attributes_per_peak";

	/** The parameter name for &quot;Use this range for additional peaks for each found peak.&quot; */
	public static final String PARAMETER_EPSILON = "epsilon";

	/** The parameter name for &quot;Use this adaption type for additional peaks.&quot; */
	public static final String PARAMETER_ADAPTION_TYPE = "adaption_type";

	public AGA(OperatorDescription description) {
		super(description);
	}

	@Override
	public void doWork() throws OperatorException {
		if (getParameterAsBoolean(PARAMETER_RESTRICTIVE_SELECTION)) {
			FeatureGenerator.setSelectionMode(FeatureGenerator.SELECTION_MODE_RESTRICTIVE);
		} else {
			FeatureGenerator.setSelectionMode(FeatureGenerator.SELECTION_MODE_ALL);
		}
		super.doWork();
	}

	@Override
	public List<FeatureGenerator> getGenerators() {
		List<FeatureGenerator> generators = super.getGenerators();
		if (getParameterAsBoolean(PARAMETER_USE_SQUARE_ROOTS)) {
			generators.add(new SquareRootGenerator());
		}
		if (getParameterAsBoolean(PARAMETER_USE_POWER_FUNCTIONS)) {
			generators.add(new PowerGenerator());
		}

		if (getParameterAsBoolean(PARAMETER_USE_SIN)) {
			generators.add(new TrigonometricFunctionGenerator(TrigonometricFunctionGenerator.SINUS));
		}
		if (getParameterAsBoolean(PARAMETER_USE_COS)) {
			generators.add(new TrigonometricFunctionGenerator(TrigonometricFunctionGenerator.COSINUS));
		}
		if (getParameterAsBoolean(PARAMETER_USE_TAN)) {
			generators.add(new TrigonometricFunctionGenerator(TrigonometricFunctionGenerator.TANGENS));
		}
		if (getParameterAsBoolean(PARAMETER_USE_ATAN)) {
			generators.add(new TrigonometricFunctionGenerator(TrigonometricFunctionGenerator.ARC_TANGENS));
		}

		if (getParameterAsBoolean(PARAMETER_USE_EXP)) {
			generators.add(new ExponentialFunctionGenerator(ExponentialFunctionGenerator.EXP));
		}
		if (getParameterAsBoolean(PARAMETER_USE_LOG)) {
			generators.add(new ExponentialFunctionGenerator(ExponentialFunctionGenerator.LOG));
		}

		if (getParameterAsBoolean(PARAMETER_USE_ABSOLUTE_VALUES)) {
			generators.add(new AbsoluteValueGenerator());
		}
		if (getParameterAsBoolean(PARAMETER_USE_MIN)) {
			generators.add(new MinMaxGenerator(MinMaxGenerator.MIN));
		}
		if (getParameterAsBoolean(PARAMETER_USE_MAX)) {
			generators.add(new MinMaxGenerator(MinMaxGenerator.MAX));
		}

		if (getParameterAsBoolean(PARAMETER_USE_SGN)) {
			generators.add(new SignumGenerator());
		}

		if (getParameterAsBoolean(PARAMETER_USE_FLOOR_CEIL_FUNCTIONS)) {
			generators.add(new FloorCeilGenerator(FloorCeilGenerator.FLOOR));
			generators.add(new FloorCeilGenerator(FloorCeilGenerator.CEIL));
			generators.add(new FloorCeilGenerator(FloorCeilGenerator.ROUND));
		}
		return generators;
	}

	@Override
	protected List<ExampleSetBasedPopulationOperator> getPreProcessingPopulationOperators(ExampleSet input)
			throws OperatorException {
		List<ExampleSetBasedPopulationOperator> popOps = super.getPreProcessingPopulationOperators(input);
		if (getParameterAsBoolean(PARAMETER_REMOVE_USELESS)) {
			popOps.add(new RemoveUselessAttributes());
		}
		if (getParameterAsBoolean(PARAMETER_REMOVE_EQUIVALENT)) {
			popOps.add(new EquivalentAttributeRemoval(getParameterAsInt(PARAMETER_EQUIVALENCE_SAMPLES),
					getParameterAsDouble(PARAMETER_EQUIVALENCE_EPSILON),
					getParameterAsBoolean(PARAMETER_EQUIVALENCE_USE_STATISTICS), getRandom(), this));
		}
		int maxPeaks = getParameterAsInt(PARAMETER_SEARCH_FOURIER_PEAKS);
		if (maxPeaks > 0) {
			popOps.add(new FourierGenerator(maxPeaks, getParameterAsInt(PARAMETER_ADAPTION_TYPE),
					getParameterAsInt(PARAMETER_ATTRIBUTES_PER_PEAK), getParameterAsDouble(PARAMETER_EPSILON), getRandom()));
		}
		return popOps;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_USE_SQUARE_ROOTS, "Generate square root values.", false));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_POWER_FUNCTIONS,
				"Generate the power of one attribute and another.", false));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_SIN, "Generate sinus.", false));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_COS, "Generate cosinus.", false));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_TAN, "Generate tangens.", false));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_ATAN, "Generate arc tangens.", false));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_EXP, "Generate exponential functions.", false));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_LOG, "Generate logarithmic functions.", false));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_ABSOLUTE_VALUES, "Generate absolute values.", false));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_MIN, "Generate minimum values.", false));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_MAX, "Generate maximum values.", false));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_SGN, "Generate signum values.", false));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_FLOOR_CEIL_FUNCTIONS, "Generate floor, ceil, and rounded values.",
				false));
		types.add(new ParameterTypeBoolean(PARAMETER_RESTRICTIVE_SELECTION, "Use restrictive generator selection (faster).",
				true));
		types.add(new ParameterTypeBoolean(PARAMETER_REMOVE_USELESS, "Remove useless attributes.", true));
		types.add(new ParameterTypeBoolean(PARAMETER_REMOVE_EQUIVALENT, "Remove equivalent attributes.", true));
		types.add(new ParameterTypeInt(PARAMETER_EQUIVALENCE_SAMPLES, "Check this number of samples to prove equivalency.",
				1, Integer.MAX_VALUE, 10));
		types.add(new ParameterTypeDouble(PARAMETER_EQUIVALENCE_EPSILON,
				"Consider two attributes equivalent if their difference is not bigger than epsilon.", 0.0d,
				Double.POSITIVE_INFINITY, 0.0000005d));
		types.add(new ParameterTypeBoolean(PARAMETER_EQUIVALENCE_USE_STATISTICS,
				"Recalculates attribute statistics before equivalence check.", true));
		types.add(new ParameterTypeInt(PARAMETER_SEARCH_FOURIER_PEAKS,
				"Use this number of highest frequency peaks for sinus generation.", 0, Integer.MAX_VALUE, 0));
		types.add(new ParameterTypeInt(PARAMETER_ATTRIBUTES_PER_PEAK,
				"Use this number of additional peaks for each found peak.", 1, Integer.MAX_VALUE, 1));
		types.add(new ParameterTypeDouble(PARAMETER_EPSILON, "Use this range for additional peaks for each found peak.", 0,
				Double.POSITIVE_INFINITY, 0.1));
		types.add(new ParameterTypeCategory(PARAMETER_ADAPTION_TYPE, "Use this adaption type for additional peaks.",
				SinusFactory.ADAPTION_TYPES, SinusFactory.GAUSSIAN));
		return types;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		// add expression parser version change
		// will be used by the ExpressionParserBuilder in EquivalentAttributeRemoval
		return ExpressionParserUtils.addIncompatibleExpressionParserChange(super.getIncompatibleVersionChanges());
	}

}
