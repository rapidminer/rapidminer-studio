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
package com.rapidminer.operator.preprocessing;

import java.util.Collection;
import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.container.Range;


/**
 * This operator adds random attributes and white noise to the data. New random attributes are
 * simply filled with random data which is not correlated to the label at all. Additionally, this
 * operator might add noise to the label attribute or to the regular attributes. In case of a
 * numerical label the given <code>label_noise</code> is the percentage of the label range which
 * defines the standard deviation of normal distributed noise which is added to the label attribute.
 * For nominal labels the parameter <code>label_noise</code> defines the probability to randomly
 * change the nominal label value. In case of adding noise to regular attributes the parameter
 * <code>default_attribute_noise</code> simply defines the standard deviation of normal distributed
 * noise without using the attribute value range. Using the parameter list it is possible to set
 * different noise levels for different attributes. However, it is not possible to add noise to
 * nominal attributes.
 *
 * @author Ingo Mierswa
 */
public class NoiseOperator extends PreprocessingOperator {

	/** The parameter name for &quot;Adds this number of random attributes.&quot; */
	public static final String PARAMETER_RANDOM_ATTRIBUTES = "random_attributes";

	/**
	 * The parameter name for &quot;Add this percentage of a numerical label range as a normal
	 * distributed noise or probability for a nominal label change.&quot;
	 */
	public static final String PARAMETER_LABEL_NOISE = "label_noise";

	/** The parameter name for &quot;The standard deviation of the default attribute noise.&quot; */
	public static final String PARAMETER_DEFAULT_ATTRIBUTE_NOISE = "default_attribute_noise";

	/** The parameter name for &quot;List of noises for each attributes.&quot; */
	public static final String PARAMETER_NOISE = "noise";

	/** The parameter name for &quot;Offset added to the values of each random attribute&quot; */
	public static final String PARAMETER_OFFSET = "offset";

	/**
	 * The parameter name for &quot;Linear factor multiplicated with the values of each random
	 * attribute&quot;
	 */
	public static final String PARAMETER_LINEAR_FACTOR = "linear_factor";

	public NoiseOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	public PreprocessingModel createPreprocessingModel(ExampleSet exampleSet) throws OperatorException {
		exampleSet.recalculateAllAttributeStatistics();
		int randomAttributes = getParameterAsInt(PARAMETER_RANDOM_ATTRIBUTES);
		String[] attributeNames = new String[randomAttributes];
		for (int i = 0; i < randomAttributes; i++) {
			attributeNames[i] = AttributeFactory.createName("random");
		}
		return new NoiseModel(exampleSet, RandomGenerator.getRandomGenerator(this), getParameterList(PARAMETER_NOISE),
				getParameterAsDouble(PARAMETER_DEFAULT_ATTRIBUTE_NOISE), getParameterAsDouble(PARAMETER_LABEL_NOISE),
				getParameterAsDouble(PARAMETER_OFFSET), getParameterAsDouble(PARAMETER_LINEAR_FACTOR), attributeNames);
	}

	@Override
	protected ExampleSetMetaData modifyMetaData(ExampleSetMetaData exampleSetMetaData) throws UndefinedParameterError {
		AttributeMetaData label = exampleSetMetaData.getLabelMetaData();
		if (label != null) {
			if (label.isNumerical() && getParameterAsDouble(PARAMETER_LABEL_NOISE) > 0) {
				label.setValueSetRelation(SetRelation.SUPERSET);
			}
		}
		double defaultNoise = getParameterAsDouble(PARAMETER_DEFAULT_ATTRIBUTE_NOISE);
		if (defaultNoise > 0) {
			for (AttributeMetaData amd : exampleSetMetaData.getAllAttributes()) {
				if (!amd.isSpecial()) {
					if (amd.isNumerical()) {
						amd.setValueSetRelation(SetRelation.SUPERSET);
					}
				}
			}
		}
		int numberOfRandomAttributes = getParameterAsInt(PARAMETER_RANDOM_ATTRIBUTES);
		for (int i = 0; i < numberOfRandomAttributes; i++) {
			AttributeMetaData amd = new AttributeMetaData("random" + ((i == 0) ? "" : i + ""), Ontology.REAL);
			amd.setValueRange(new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), SetRelation.SUBSET);
			exampleSetMetaData.addAttribute(amd);
		}
		return exampleSetMetaData;
	}

	/**
	 * This method isn't used anymore, since the calling super function is overridden.
	 */
	@Override
	protected Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd) {
		return null;
	}

	@Override
	public boolean isSupportingAttributeRoles() {
		return true;
	}

	/** This operator does not support view creation proper. Hence hide the parameter */
	@Override
	public boolean isSupportingView() {
		return false;
	}

	@Override
	public Class<? extends PreprocessingModel> getPreprocessingModelClass() {
		return NoiseModel.class;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeInt(PARAMETER_RANDOM_ATTRIBUTES, "Adds this number of random attributes.", 0,
				Integer.MAX_VALUE, 0);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(
				PARAMETER_LABEL_NOISE,
				"Add this percentage of a numerical label range as a normal distributed noise or probability for a nominal label change.",
				0.0d, Double.POSITIVE_INFINITY, 0.05d);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeDouble(PARAMETER_DEFAULT_ATTRIBUTE_NOISE,
				"The standard deviation of the default attribute noise.", 0.0d, Double.POSITIVE_INFINITY, 0.0d));
		types.add(new ParameterTypeList(PARAMETER_NOISE, "List of noises for each attributes.", new ParameterTypeAttribute(
				"attribute", "To this attribute noise is added.", getExampleSetInputPort()), new ParameterTypeDouble(
				PARAMETER_NOISE, "The strength of gaussian noise, which is added to this attribute.", 0.0d,
				Double.POSITIVE_INFINITY, 0.05d)));
		type = new ParameterTypeDouble(PARAMETER_OFFSET, "Offset added to the values of each random attribute",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0d);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_LINEAR_FACTOR,
				"Linear factor multiplicated with the values of each random attribute", 0.0d, Double.POSITIVE_INFINITY, 1.0d);
		types.add(type);
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.ATTRIBUTE_VALUE };
	}

	@Override
	public boolean writesIntoExistingData() {
		// model takes care of materialization
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), NoiseOperator.class,
				attributeSelector);
	}

}
