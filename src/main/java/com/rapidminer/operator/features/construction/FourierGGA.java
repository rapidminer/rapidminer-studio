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

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.generator.FeatureGenerator;
import com.rapidminer.generator.SinusFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;


/**
 * FourierGGA has all functions of YAGGA2. Additionally for each added attribute a fourier
 * transformation is performed and the sinus function corresponding to the highest peaks are
 * additionally added.
 * 
 * YAGGA is an acronym for Yet Another Generating Genetic Algorithm. Its approach to generating new
 * attributes differs from the original one. The (generating) mutation can do one of the following
 * things with different probabilities:
 * <ul>
 * <li>Probability {@rapidminer.math p/4}: Add a newly generated attribute to the feature vector</li>
 * <li>Probability {@rapidminer.math p/4}: Add a randomly chosen original attribute to the feature
 * vector</li>
 * <li>Probability {@rapidminer.math p/2}: Remove a randomly chosen attribute from the feature
 * vector</li>
 * </ul>
 * Thus it is guaranteed that the length of the feature vector can both grow and shrink. On average
 * it will keep its original length, unless longer or shorter individuals prove to have a better
 * fitness.
 * 
 * Since this operator does not contain algorithms to extract features from value series, it is
 * restricted to example sets with only single attributes. For (automatic) feature extraction from
 * values series the value series plugin for RapidMiner written by Ingo Mierswa should be used. It
 * is available at <a href="http://rapidminer.com">http://rapidminer.com</a>.
 * 
 * @author Ingo Mierswa
 */
public class FourierGGA extends YAGGA2 {

	/**
	 * The parameter name for &quot;The maximum of original attributes added in each
	 * generation.&quot;
	 */
	public static final String PARAMETER_NUMBER_ORIGINAL = "number_original";

	/**
	 * The parameter name for &quot;The maximum number of attributes constructed in each
	 * generation.&quot;
	 */
	public static final String PARAMETER_NUMBER_CONSTRUCTED = "number_constructed";

	/** The parameter name for &quot;Uses a fourier generation in this first generations&quot; */
	public static final String PARAMETER_START_SINUS_BOOST = "start_sinus_boost";

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

	public FourierGGA(OperatorDescription description) {
		super(description);
	}

	/** Returns the generating mutation <code>PopulationOperator</code>. */
	@Override
	protected ExampleSetBasedPopulationOperator getMutationPopulationOperator(ExampleSet eSet) throws OperatorException {
		List<FeatureGenerator> generators = getGenerators();
		if (generators.size() == 0) {
			logWarning("No FeatureGenerators specified for " + getName() + ".");
		}
		List<Attribute> attributes = new LinkedList<Attribute>();
		for (Attribute attribute : eSet.getAttributes()) {
			attributes.add(attribute);
		}
		double pMutation = getParameterAsDouble(PARAMETER_P_MUTATION);
		return new FourierGeneratingMutation(attributes, pMutation, generators,
				getParameterAsInt(PARAMETER_NUMBER_CONSTRUCTED), getParameterAsInt(PARAMETER_NUMBER_ORIGINAL),
				getParameterAsInt(PARAMETER_SEARCH_FOURIER_PEAKS), getParameterAsInt(PARAMETER_ADAPTION_TYPE),
				getParameterAsInt(PARAMETER_ATTRIBUTES_PER_PEAK), getParameterAsDouble(PARAMETER_EPSILON),
				getParameterAsString(PARAMETER_UNUSED_FUNCTIONS).split(" "), getRandom());
	}

	@Override
	protected List<ExampleSetBasedPopulationOperator> getPreProcessingPopulationOperators(ExampleSet eSet)
			throws OperatorException {
		List<ExampleSetBasedPopulationOperator> popOps = super.getPreProcessingPopulationOperators(eSet);
		int startSinus = getParameterAsInt(PARAMETER_START_SINUS_BOOST);
		if (startSinus > 0) {
			FourierGenerator fourierGen = new FourierGenerator(getParameterAsInt(PARAMETER_SEARCH_FOURIER_PEAKS),
					getParameterAsInt(PARAMETER_ADAPTION_TYPE), getParameterAsInt(PARAMETER_ATTRIBUTES_PER_PEAK),
					getParameterAsDouble(PARAMETER_EPSILON), getRandom());
			fourierGen.setStartGenerations(startSinus);
			fourierGen.setApplyInGeneration(0);
			popOps.add(fourierGen);
		}
		return popOps;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(PARAMETER_NUMBER_ORIGINAL,
				"The maximum of original attributes added in each generation.", 0, Integer.MAX_VALUE, 2));
		types.add(new ParameterTypeInt(PARAMETER_NUMBER_CONSTRUCTED,
				"The maximum number of attributes constructed in each generation.", 0, Integer.MAX_VALUE, 2));
		types.add(new ParameterTypeString(PARAMETER_UNUSED_FUNCTIONS,
				"Space separated list of functions which are not allowed in arguments for attribute construction."));
		types.add(new ParameterTypeInt(PARAMETER_START_SINUS_BOOST, "Uses a fourier generation in this first generations",
				0, Integer.MAX_VALUE, 0));
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
}
