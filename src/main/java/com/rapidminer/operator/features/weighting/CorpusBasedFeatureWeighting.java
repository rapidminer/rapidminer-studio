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
package com.rapidminer.operator.features.weighting;

import java.util.Iterator;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;


/**
 * This operator uses a corpus of examples to characterize a single class by setting feature
 * weights. Characteristic features receive higher weights than less characteristic features. The
 * weight for a feature is determined by calculating the average value of this feature for all
 * examples of the target class. This operator assumes that the feature values characterize the
 * importance of this feature for an example (e.g. TFIDF or others). Therefore, this operator is
 * mainly used on textual data based on TFIDF weighting schemes. To extract such feature values from
 * text collections you can use the Text plugin.
 * 
 * @author Michael Wurst, Ingo Mierswa
 */
public class CorpusBasedFeatureWeighting extends AbstractWeighting {

	/**
	 * The parameter name for &quot;The target class for which to find characteristic feature
	 * weights.&quot;
	 */
	private static final String PARAMETER_CLASS_TO_CHARACTERIZE = "class_to_characterize";

	public CorpusBasedFeatureWeighting(OperatorDescription description) {
		super(description, true);
		// TODO: Add Dictionary Quickfix for parameter.
	}

	@Override
	protected AttributeWeights calculateWeights(ExampleSet es) throws OperatorException {
		String targetValue = getParameterAsString(PARAMETER_CLASS_TO_CHARACTERIZE);

		double[] weights = generateWeightsForClass(es, targetValue);

		double maxWeight = Double.NEGATIVE_INFINITY;
		for (double w : weights) {
			maxWeight = Math.max(maxWeight, w);
		}

		AttributeWeights attWeights = new AttributeWeights();

		int i = 0;
		for (Attribute attribute : es.getAttributes()) {
			if (maxWeight > 0.0d) {
				attWeights.setWeight(attribute.getName(), weights[i++] / maxWeight);
			} else {
				attWeights.setWeight(attribute.getName(), 0.0d);
			}
		}

		return attWeights;
	}

	private double[] generateWeightsForClass(ExampleSet es, String value) {
		double[] result = new double[es.getAttributes().size()];
		for (int i = 0; i < es.getAttributes().size(); i++) {
			result[i] = 0.0;
		}
		Iterator<Example> er = es.iterator();
		Attribute labelAttribute = es.getAttributes().getLabel();
		while (er.hasNext()) {
			Example e = er.next();
			if (e.getValueAsString(labelAttribute).equalsIgnoreCase(value)) {
				int index = 0;
				for (Attribute attribute : es.getAttributes()) {
					result[index] += e.getValue(attribute);
					index++;
				}
			}
		}
		return result;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case BINOMINAL_LABEL:
			case POLYNOMINAL_LABEL:
			case NUMERICAL_ATTRIBUTES:
				return true;
			default:
				return false;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeString(PARAMETER_CLASS_TO_CHARACTERIZE,
				"The target class for which to find characteristic feature weights.", false, false));
		return types;
	}
}
