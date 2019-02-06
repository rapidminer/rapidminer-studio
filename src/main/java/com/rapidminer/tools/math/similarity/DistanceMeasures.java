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
package com.rapidminer.tools.math.similarity;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.math.kernels.Kernel;
import com.rapidminer.tools.math.similarity.divergences.GeneralizedIDivergence;
import com.rapidminer.tools.math.similarity.divergences.ItakuraSaitoDistance;
import com.rapidminer.tools.math.similarity.divergences.KLDivergence;
import com.rapidminer.tools.math.similarity.divergences.LogarithmicLoss;
import com.rapidminer.tools.math.similarity.divergences.LogisticLoss;
import com.rapidminer.tools.math.similarity.divergences.MahalanobisDistance;
import com.rapidminer.tools.math.similarity.divergences.SquaredEuclideanDistance;
import com.rapidminer.tools.math.similarity.divergences.SquaredLoss;
import com.rapidminer.tools.math.similarity.mixed.MixedEuclideanDistance;
import com.rapidminer.tools.math.similarity.nominal.DiceNominalSimilarity;
import com.rapidminer.tools.math.similarity.nominal.JaccardNominalSimilarity;
import com.rapidminer.tools.math.similarity.nominal.KulczynskiNominalSimilarity;
import com.rapidminer.tools.math.similarity.nominal.NominalDistance;
import com.rapidminer.tools.math.similarity.nominal.RogersTanimotoNominalSimilarity;
import com.rapidminer.tools.math.similarity.nominal.RussellRaoNominalSimilarity;
import com.rapidminer.tools.math.similarity.nominal.SimpleMatchingNominalSimilarity;
import com.rapidminer.tools.math.similarity.numerical.CamberraNumericalDistance;
import com.rapidminer.tools.math.similarity.numerical.ChebychevNumericalDistance;
import com.rapidminer.tools.math.similarity.numerical.CorrelationSimilarity;
import com.rapidminer.tools.math.similarity.numerical.CosineSimilarity;
import com.rapidminer.tools.math.similarity.numerical.DTWDistance;
import com.rapidminer.tools.math.similarity.numerical.DiceNumericalSimilarity;
import com.rapidminer.tools.math.similarity.numerical.EuclideanDistance;
import com.rapidminer.tools.math.similarity.numerical.InnerProductSimilarity;
import com.rapidminer.tools.math.similarity.numerical.JaccardNumericalSimilarity;
import com.rapidminer.tools.math.similarity.numerical.KernelEuclideanDistance;
import com.rapidminer.tools.math.similarity.numerical.ManhattanDistance;
import com.rapidminer.tools.math.similarity.numerical.MaxProductSimilarity;
import com.rapidminer.tools.math.similarity.numerical.OverlapNumericalSimilarity;


/**
 * This is a convenient class for using the distanceMeasures. It offers methods for integrating the
 * measure classes into operators.
 * 
 * @author Sebastian Land
 */
public class DistanceMeasures {

	public static final String PARAMETER_MEASURE_TYPES = "measure_types";
	public static final String PARAMETER_NUMERICAL_MEASURE_TYPES = "numerical_measure_types";
	public static final String PARAMETER_NOMINAL_MEASURE = "nominal_measure";
	public static final String PARAMETER_NUMERICAL_MEASURE = "numerical_measure";
	public static final String PARAMETER_MIXED_MEASURE = "mixed_measure";
	public static final String PARAMETER_DIVERGENCE = "divergence";

	public static final String[] MEASURE_TYPES = new String[] { "MixedMeasures", "NominalMeasures", "NumericalMeasures",
			"BregmanDivergences" };

	public static final int MIXED_MEASURES_TYPE = 0;
	public static final int NOMINAL_MEASURES_TYPE = 1;
	public static final int NUMERICAL_MEASURES_TYPE = 2;
	public static final int DIVERGENCES_TYPE = 3;
	public static final int KERNEL_IN_NUMERICAL = 9;
	public static final String[] NUMERICAL_MEASURE_TYPES = Arrays.copyOfRange(MEASURE_TYPES, NUMERICAL_MEASURES_TYPE,
			DIVERGENCES_TYPE + 1);

	private static String[] NOMINAL_MEASURES = new String[] { "NominalDistance", "DiceSimilarity", "JaccardSimilarity",
			"KulczynskiSimilarity", "RogersTanimotoSimilarity", "RussellRaoSimilarity", "SimpleMatchingSimilarity" };
	@SuppressWarnings("unchecked")
	private static Class<? extends DistanceMeasure>[] NOMINAL_MEASURE_CLASSES = new Class[] { NominalDistance.class,
			DiceNominalSimilarity.class, JaccardNominalSimilarity.class, KulczynskiNominalSimilarity.class,
			RogersTanimotoNominalSimilarity.class, RussellRaoNominalSimilarity.class,
			SimpleMatchingNominalSimilarity.class };

	private static String[] MIXED_MEASURES = new String[] { "MixedEuclideanDistance" };
	@SuppressWarnings("unchecked")
	private static Class<? extends DistanceMeasure>[] MIXED_MEASURE_CLASSES = new Class[] { MixedEuclideanDistance.class };

	/* If this changes, the parameter dependencies might need to be updated */
	private static String[] NUMERICAL_MEASURES = new String[] { "EuclideanDistance", "CamberraDistance", "ChebychevDistance",
			"CorrelationSimilarity", "CosineSimilarity", "DiceSimilarity", "DynamicTimeWarpingDistance",
			"InnerProductSimilarity", "JaccardSimilarity", "KernelEuclideanDistance", "ManhattanDistance",
			"MaxProductSimilarity", "OverlapSimilarity" };
	@SuppressWarnings("unchecked")
	private static Class<? extends DistanceMeasure>[] NUMERICAL_MEASURE_CLASSES = new Class[] { EuclideanDistance.class,
			CamberraNumericalDistance.class, ChebychevNumericalDistance.class, CorrelationSimilarity.class,
			CosineSimilarity.class, DiceNumericalSimilarity.class, DTWDistance.class, InnerProductSimilarity.class,
			JaccardNumericalSimilarity.class, KernelEuclideanDistance.class, ManhattanDistance.class,
			MaxProductSimilarity.class, OverlapNumericalSimilarity.class };

	private static String[] DIVERGENCES = new String[] { "GeneralizedIDivergence", "ItakuraSaitoDistance", "KLDivergence",
			"LogarithmicLoss", "LogisticLoss", "MahalanobisDistance", "SquaredEuclideanDistance", "SquaredLoss", };
	@SuppressWarnings("unchecked")
	private static Class<? extends DistanceMeasure>[] DIVERGENCE_CLASSES = new Class[] { GeneralizedIDivergence.class,
			ItakuraSaitoDistance.class, KLDivergence.class, LogarithmicLoss.class, LogisticLoss.class,
			MahalanobisDistance.class, SquaredEuclideanDistance.class, SquaredLoss.class, };

	private static String[][] MEASURE_ARRAYS = new String[][] { MIXED_MEASURES, NOMINAL_MEASURES, NUMERICAL_MEASURES,
			DIVERGENCES };

	@SuppressWarnings("unchecked")
	private static Class<? extends DistanceMeasure>[][] MEASURE_CLASS_ARRAYS = new Class[][] { MIXED_MEASURE_CLASSES,
			NOMINAL_MEASURE_CLASSES, NUMERICAL_MEASURE_CLASSES, DIVERGENCE_CLASSES };

	/**
	 * This method allows registering distance or similarity measures defined in plugins. There are
	 * four different types of measures: Mixed Measures coping with examples containing nominal and
	 * numerical values. Numerical and Nominal Measures work only on their respective type of
	 * attribute. Divergences are a less restricted mathematical concept than distances but might be
	 * used for some algorithms not needing this restrictions. This type has to be specified using
	 * the first parameter.
	 * 
	 * @param measureType
	 *            The type is available as static property of class
	 * @param measureName
	 *            The name of the measure to register
	 * @param measureClass
	 *            The class of the measure, which needs to extend DistanceMeasure
	 */
	public static void registerMeasure(int measureType, String measureName, Class<? extends DistanceMeasure> measureClass) {
		int length = MEASURE_ARRAYS[measureType].length;
		MEASURE_ARRAYS[measureType] = Arrays.copyOf(MEASURE_ARRAYS[measureType], length + 1);
		MEASURE_ARRAYS[measureType][length] = measureName;

		MEASURE_CLASS_ARRAYS[measureType] = Arrays.copyOf(MEASURE_CLASS_ARRAYS[measureType], length + 1);
		MEASURE_CLASS_ARRAYS[measureType][length] = measureClass;
	}

	/**
	 * Creates an uninitialized distance measure. Initialize the distance measure by calling
	 * {@link DistanceMeasure#init(ExampleSet, ParameterHandler)}.
	 */
	public static DistanceMeasure createMeasure(ParameterHandler parameterHandler)
			throws UndefinedParameterError, OperatorException {
		int measureType;
		if (parameterHandler.isParameterSet(PARAMETER_MEASURE_TYPES)) {
			measureType = parameterHandler.getParameterAsInt(PARAMETER_MEASURE_TYPES);
		} else if (parameterHandler.isParameterSet(PARAMETER_NUMERICAL_MEASURE_TYPES)) {
			measureType = parameterHandler.getParameterAsInt(PARAMETER_NUMERICAL_MEASURE_TYPES) + NUMERICAL_MEASURES_TYPE;
		} else {
			// if type is not set, then might be there is no type selection: Test if one definition
			// is present
			if (parameterHandler.isParameterSet(PARAMETER_MIXED_MEASURE)) {
				measureType = MIXED_MEASURES_TYPE;
			} else if (parameterHandler.isParameterSet(PARAMETER_NOMINAL_MEASURE)) {
				measureType = NOMINAL_MEASURES_TYPE;
			} else if (parameterHandler.isParameterSet(PARAMETER_NUMERICAL_MEASURE)) {
				measureType = NUMERICAL_MEASURES_TYPE;
			} else if (parameterHandler.isParameterSet(PARAMETER_DIVERGENCE)) {
				measureType = DIVERGENCES_TYPE;
			} else {
				// if nothing fits: Try to access to get a proper exception
				measureType = parameterHandler.getParameterAsInt(PARAMETER_MEASURE_TYPES);
			}
		}
		Class<? extends DistanceMeasure>[] classes = MEASURE_CLASS_ARRAYS[measureType];
		Class<? extends DistanceMeasure> measureClass = null;
		switch (measureType) {
			case MIXED_MEASURES_TYPE:
				measureClass = classes[parameterHandler.getParameterAsInt(PARAMETER_MIXED_MEASURE)];
				break;
			case NOMINAL_MEASURES_TYPE:
				measureClass = classes[parameterHandler.getParameterAsInt(PARAMETER_NOMINAL_MEASURE)];
				break;
			case NUMERICAL_MEASURES_TYPE:
				measureClass = classes[parameterHandler.getParameterAsInt(PARAMETER_NUMERICAL_MEASURE)];
				break;
			case DIVERGENCES_TYPE:
				measureClass = classes[parameterHandler.getParameterAsInt(PARAMETER_DIVERGENCE)];
				break;
		}
		if (measureClass != null) {
			DistanceMeasure measure;
			try {
				measure = measureClass.newInstance();
				return measure;
			} catch (InstantiationException e) {
				throw new OperatorException("Could not instanciate distance measure " + measureClass);
			} catch (IllegalAccessException e) {
				throw new OperatorException("Could not instanciate distance measure " + measureClass);
			}
		}
		return null;
	}

	/**
	 * @deprecated ioContainer is not used. Use a {@link DistanceMeasureHelper} to obtain distance
	 *             measures.
	 */
	@Deprecated
	public static DistanceMeasure createMeasure(ParameterHandler parameterHandler, ExampleSet exampleSet,
			IOContainer ioContainer) throws UndefinedParameterError, OperatorException {
		DistanceMeasure measure = createMeasure(parameterHandler);
		if (measure == null) {
			return null;
		}
		if (exampleSet != null) {
			measure.init(exampleSet, parameterHandler);
		}
		return measure;
	}

	public static int getSelectedMeasureType(ParameterHandler parameterHandler) throws UndefinedParameterError {
		return parameterHandler.getParameterAsInt(PARAMETER_MEASURE_TYPES);
	}

	public static boolean measureTypeSupportsNominalValues(int measureType) {
		return measureType == NOMINAL_MEASURES_TYPE || measureType == MIXED_MEASURES_TYPE;
	}

	public static boolean measureTypeSupportsNumericalValues(int measureType) {
		return measureType != NOMINAL_MEASURES_TYPE;
	}

	/**
	 * This method adds a parameter to chose a distance measure as parameter
	 */
	public static List<ParameterType> getParameterTypes(Operator parameterHandler) {
		List<ParameterType> list = new LinkedList<ParameterType>();
		list.add(new ParameterTypeCategory(PARAMETER_MEASURE_TYPES, "The measure type", MEASURE_TYPES, 0, false));
		ParameterType type = new ParameterTypeCategory(PARAMETER_MIXED_MEASURE, "Select measure",
				MEASURE_ARRAYS[MIXED_MEASURES_TYPE], 0, false);
		type.registerDependencyCondition(new EqualTypeCondition(parameterHandler, PARAMETER_MEASURE_TYPES, MEASURE_TYPES,
				false, MIXED_MEASURES_TYPE));
		list.add(type);
		type = new ParameterTypeCategory(PARAMETER_NOMINAL_MEASURE, "Select measure", MEASURE_ARRAYS[NOMINAL_MEASURES_TYPE],
				0, false);
		type.registerDependencyCondition(new EqualTypeCondition(parameterHandler, PARAMETER_MEASURE_TYPES, MEASURE_TYPES,
				false, NOMINAL_MEASURES_TYPE));
		list.add(type);
		type = new ParameterTypeCategory(PARAMETER_NUMERICAL_MEASURE, "Select measure",
				MEASURE_ARRAYS[NUMERICAL_MEASURES_TYPE], 0, false);
		type.registerDependencyCondition(new EqualTypeCondition(parameterHandler, PARAMETER_MEASURE_TYPES, MEASURE_TYPES,
				false, NUMERICAL_MEASURES_TYPE));
		list.add(type);
		type = new ParameterTypeCategory(PARAMETER_DIVERGENCE, "Select divergence", MEASURE_ARRAYS[DIVERGENCES_TYPE], 0,
				false);
		type.registerDependencyCondition(
				new EqualTypeCondition(parameterHandler, PARAMETER_MEASURE_TYPES, MEASURE_TYPES, false, DIVERGENCES_TYPE));
		list.add(type);
		list.addAll(registerDependency(Kernel.getParameters(parameterHandler), KERNEL_IN_NUMERICAL, parameterHandler));

		return list;
	}

	/**
	 * This method provides the parameters to choose only from numerical and divergence distance
	 * measures.
	 *
	 * @since 7.6
	 */
	public static List<ParameterType> getParameterTypesForNumAndDiv(Operator parameterHandler) {
		List<ParameterType> list = new LinkedList<ParameterType>();
		list.add(new ParameterTypeCategory(PARAMETER_NUMERICAL_MEASURE_TYPES, "The measure type", NUMERICAL_MEASURE_TYPES, 0,
				false));
		ParameterType type = new ParameterTypeCategory(PARAMETER_NUMERICAL_MEASURE, "Select measure",
				MEASURE_ARRAYS[NUMERICAL_MEASURES_TYPE], 0, false);
		type.registerDependencyCondition(new EqualTypeCondition(parameterHandler, PARAMETER_NUMERICAL_MEASURE_TYPES,
				NUMERICAL_MEASURE_TYPES, false, NUMERICAL_MEASURES_TYPE - NUMERICAL_MEASURES_TYPE));
		list.add(type);
		type = new ParameterTypeCategory(PARAMETER_DIVERGENCE, "Select divergence", MEASURE_ARRAYS[DIVERGENCES_TYPE], 0,
				false);
		type.registerDependencyCondition(new EqualTypeCondition(parameterHandler, PARAMETER_NUMERICAL_MEASURE_TYPES,
				NUMERICAL_MEASURE_TYPES, false, DIVERGENCES_TYPE - NUMERICAL_MEASURES_TYPE));
		list.add(type);
		for (ParameterType kernelType : Kernel.getParameters(parameterHandler)) {
			kernelType.registerDependencyCondition(new EqualTypeCondition(parameterHandler, PARAMETER_NUMERICAL_MEASURE,
					MEASURE_ARRAYS[NUMERICAL_MEASURES_TYPE], false, KERNEL_IN_NUMERICAL));
			type.registerDependencyCondition(new EqualTypeCondition(parameterHandler, PARAMETER_NUMERICAL_MEASURE_TYPES,
					NUMERICAL_MEASURE_TYPES, false, NUMERICAL_MEASURES_TYPE - NUMERICAL_MEASURES_TYPE));
			kernelType.setExpert(true);
			list.add(kernelType);
		}

		return list;
	}

	/**
	 * This method provides the parameters to choose only from numerical measures.
	 */
	public static List<ParameterType> getParameterTypesForNumericals(ParameterHandler handler) {
		List<ParameterType> list = new LinkedList<ParameterType>();
		ParameterType type = new ParameterTypeCategory(PARAMETER_NUMERICAL_MEASURE, "Select measure",
				MEASURE_ARRAYS[NUMERICAL_MEASURES_TYPE], 0);
		list.add(type);
		if (!(handler instanceof Operator)) {
			return list;
		}
		Operator operator = (Operator) handler;
		for (ParameterType kernelType : Kernel.getParameters(operator)) {
			kernelType.registerDependencyCondition(new EqualTypeCondition(operator, PARAMETER_NUMERICAL_MEASURE,
					MEASURE_ARRAYS[NUMERICAL_MEASURES_TYPE], false, KERNEL_IN_NUMERICAL));
			kernelType.setExpert(true);
			list.add(kernelType);
		}
		return list;
	}

	private static Collection<ParameterType> registerDependency(Collection<ParameterType> sourceTypeList, int selectedValue,
			Operator handler) {
		for (ParameterType type : sourceTypeList) {
			type.registerDependencyCondition(new EqualTypeCondition(handler, PARAMETER_NUMERICAL_MEASURE,
					MEASURE_ARRAYS[NUMERICAL_MEASURES_TYPE], false, selectedValue));
			type.registerDependencyCondition(
					new EqualTypeCondition(handler, PARAMETER_MEASURE_TYPES, MEASURE_TYPES, false, NUMERICAL_MEASURES_TYPE));
		}
		return sourceTypeList;
	}
}
