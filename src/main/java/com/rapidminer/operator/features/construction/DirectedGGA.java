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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.generator.FeatureGenerator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;


/**
 * DirectedGGA is an acronym for a Generating Genetic Algorithm which uses probability directed
 * search heuristics to select attributes for generation or removing. Its approach to generating new
 * attributes differs from the original one and is the same as the one of {@link YAGGA}. <br/>
 *
 * The (generating) mutation can do one of the following things with different probabilities:
 * <ul>
 * <li>Probability {@rapidminer.math p/4}: Add a newly generated attribute to the feature vector
 * </li>
 * <li>Probability {@rapidminer.math p/4}: Add a randomly chosen original attribute to the feature
 * vector</li>
 * <li>Probability {@rapidminer.math p/2}: Remove a randomly chosen attribute from the feature
 * vector</li>
 * </ul>
 * Thus it is guaranteed that the length of the feature vector can both grow and shrink. On average
 * it will keep its original length, unless longer or shorter individuals prove to have a better
 * fitness.<br/>
 *
 * In addition to these mutation heuristics probablilities based on the weights of the attributes
 * are calculated. It is more likely for attributes with a great weight to be selected for
 * generating new attributes. On the other hand the probability for removing an attribute from the
 * example set will decrease for attributes with great weights. This decreases the amount of needed
 * generations drastically. <br/>
 *
 * Another enhancement in comparison to the original GGA is the addition of several generators like
 * the ones for trigonometric or exponential functions. In this way a sinple linear working learning
 * scheme which can deliver weights can be used as inner operator. If this learner can also estimate
 * its performance it is not longer necessary to use a inner cross-validation which also decreases
 * learning time. Such a learner is for example the
 * {@link com.rapidminer.operator.learner.functions.kernel.JMySVMLearner} which delivers the
 * xi-alpha performance estimation at least for classification tasks. <br/>
 * .
 *
 * Summarized the advantages of this feature construction algorithm are smaller runtimes and smaller
 * attribute sets as result. These attribute sets increase performance and can be used to explain
 * the models of more complex learning schemes like SVMs. The additional generators allow the
 * construction of features which are not possible by the known kernel functions. <br/>
 *
 * Since this operator does not contain algorithms to extract features from value series, it is
 * restricted to example sets with only single attributes. For (automatic) feature extraction from
 * values series the value series plugin for RapidMiner written by Ingo Mierswa should be used. It
 * is available at <code>http://rapidminer.com</code>.
 *
 * @author Ingo Mierswa
 */
public class DirectedGGA extends YAGGA2 {

	/**
	 * The parameter name for &quot;The maximum number of generated attributes per generation.&quot;
	 */
	public static final String PARAMETER_MAX_GENERATED = "max_generated";

	/**
	 * The parameter name for &quot;The maximum number of original attributes added per
	 * generation.&quot;
	 */
	public static final String PARAMETER_MAX_ORIGINAL = "max_original";

	public DirectedGGA(OperatorDescription description) {
		super(description);
	}

	/** Returns the {@link DirectedGeneratingMutation}. */
	@Override
	protected ExampleSetBasedPopulationOperator getMutationPopulationOperator(ExampleSet eSet) throws OperatorException {
		List<FeatureGenerator> generators = getGenerators();
		if (generators.size() == 0) {
			logWarning("No FeatureGenerators specified for " + getName() + ".");
		}

		Attribute[] attributes = eSet.getAttributes().createRegularAttributeArray();

		return new DirectedGeneratingMutation(attributes, getParameterAsDouble(PARAMETER_P_MUTATION), generators,
				getParameterAsInt(PARAMETER_MAX_GENERATED), getParameterAsInt(PARAMETER_MAX_ORIGINAL),
				getParameterAsString(PARAMETER_UNUSED_FUNCTIONS).split(" "), getRandom());
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(PARAMETER_MAX_GENERATED, "The maximum number of generated attributes per generation.",
				1, Integer.MAX_VALUE, 2));
		types.add(new ParameterTypeInt(PARAMETER_MAX_ORIGINAL,
				"The maximum number of original attributes added per generation.", 1, Integer.MAX_VALUE, 2));
		types.add(new ParameterTypeString(PARAMETER_UNUSED_FUNCTIONS,
				"Space separated list of functions which are not allowed in arguments for attribute construction."));
		return types;
	}
}
